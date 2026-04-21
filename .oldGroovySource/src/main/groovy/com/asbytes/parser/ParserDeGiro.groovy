package com.asbytes.parser

import com.asbytes.Exchange
import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.json.JsonSlurper
import groovy.transform.InheritConstructors
import org.junit.Test

/**
 * Parsing Account.csv
 */
@InheritConstructors
class ParserDeGiro extends OrderParser {

	int idxDate = 0
	int idxTime = 1
	int idxValuta = 2
	int idxName = 3
	int idxIsin = 4
	int idxDesc = 5
	int idxFx = 6
	int idxCurrency = 7
	int idxTotal = 8
	int idxSaldo = 10
	int idxOrderId = 11

	final String separator = ','

	@Test
	void testStrToDouble() {
		def examples = [:]
		examples << ['1.00': 1d]
		examples << ['1,00': 1d]
		examples << ['1,000': 1d]
		examples << ['1000': 1000d]
		examples << ['1.000': 1000d]
		examples << ['1.000,00': 1000d]
		examples << ['1,000.00': 1000d]

		examples.each { str, num ->
			print "$str".padLeft(5).padRight(15)
			print " => "
			print "$num".padRight(15)
			def result = Exchange.currencyToDouble(str)
			println result
			assert result == num
		}
	}

	@Test
	void testParser() {
		def exxonDivTaxIssue = []
		exxonDivTaxIssue << unquoteLineWithComma('11-09-2020,07:54,10-09-2020,EXXON MOBIL CORPORATIO,US30231G1022,Dividende,,USD,"20,01",USD,"17,01",').split(separator)
		exxonDivTaxIssue << unquoteLineWithComma('11-09-2020,07:54,10-09-2020,EXXON MOBIL CORPORATIO,US30231G1022,Dividendensteuer,,USD,"-3,00",USD,"-3,00",').split(separator)

		def transactions = this.parseCsvLines(exxonDivTaxIssue)
		assert transactions.size() == 1

		def transaction = transactions.first()
		assert transaction.isDiv()
		assert transaction.rate.round(2) == 20.01d
		assert transaction.tax.round(2) == -2.53d
		println transaction
	}

	String unquoteLineWithComma(String line) {
		def quotedLine = line.contains(',"')
		if (quotedLine) {
			def idxBegin = line.indexOf(',"', 0) + 1
			def idxLast = line.indexOf('",', idxBegin) + 1
			def quotedField = line.substring(idxBegin, idxLast)
			line = line.replace(quotedField, quotedField.replace(['"': '', ',': '.']))
			return unquoteLineWithComma(line)
		} else {
			return line
		}
	}

	@Override
	List<Transaction> parseCsv(File file, int firstLine) {
		def csvLines = file.readLines().collect { String line ->
			return unquoteLineWithComma(line).split(separator)
		}
		csvLines.remove(0);
		return parseCsvLines(csvLines, file)
	}

	List<Transaction> parseCsvLines(List<String[]> csvLines, File file) {
		def transactions = [] as List<Transaction>

		parseForZinsen(transactions, csvLines)
		parseForADRs(transactions, csvLines)

		def isins = csvLines.collect {it[idxIsin] }.unique()

		isins.remove('LU1959429272') // don't check GMF: MORGAN STANLEY EUR LIQUIDITY FUND

		def transCounter = 1
		isins.findAll { it != "" }.each { isin ->
			def isinLines = csvLines.findAll {line -> line[idxIsin] == isin }
			def buys = isinLines.findAll { line ->
				def descLine = line[idxDesc].stripMargin('"')
				return descLine.startsWith('Kauf') || descLine.startsWith('AKTIENSPLIT: Kauf') || descLine.startsWith('SPIN-OFF: Kauf')
			}
			def sells = isinLines.findAll { line ->
				def descLine = line[idxDesc].stripMargin('"')
				return descLine.startsWith('Verkauf') || descLine.startsWith('AKTIENSPLIT: Verkauf')
			}
			def fees = isinLines.findAll { line -> line[idxDesc].startsWith('Transaktionsgebühr') }
			def divs = isinLines.findAll { line -> line[idxDesc].equals('Dividende') }
			def divtaxs = isinLines.findAll { line -> line[idxDesc].equals('Dividendensteuer') }
			def fxs = isinLines.findAll { line -> line[idxDesc].startsWith('Währungswechsel') }


			divs.each {div ->
				def transaction = new Transaction(OrderParserType.DEGIRO, file)
				transaction.setType(Transaction.TransTypes.DIV)
				transaction.setAmount(1d)
				parseCurrency(transaction, div[idxCurrency])
				transaction.setName(div[idxName])
				transaction.setIsin(div[idxIsin])
				transaction.setDate("${div[idxValuta]} ${div[idxTime]}")
				transaction.setId("div#${transCounter++}")
				parseTotal(transaction, div[idxTotal])
				transaction.setRate(transaction.getRateFromTotal(false).round(2))

				def divTax = divtaxs.find {divtax -> divtax[idxIsin] == div[idxIsin] && "${divtax[idxDate]} ${divtax[idxTime]}" == "${div[idxDate]} ${div[idxTime]}" }
				if (divTax) {
					transaction.addTax(divTax[idxTotal])
					transaction.setTotal(transaction.total + transaction.tax)
					if (divTax[idxCurrency] != 'EUR' || div[idxCurrency] != 'EUR') {
						// TODO how to handle non-converted USD :(
						// current solution => Degiro is always USD, fxs is not evaluated!
					}
					divtaxs.remove(divTax)
				}

				transactions << transaction
			}

			def buyPattern = /(?<typ>Verkauf|Kauf) (?<amt>[\d.]+) zu je (?<prc>[0-9,.]+) (?<cur>[A-Z]{1,3}).+/
			(buys + sells).each { buy ->
				def match = buy[idxDesc] =~ buyPattern;
				if (match.find()) {
					def transaction = new Transaction(OrderParserType.DEGIRO, file)
					def transType = (match.group('typ') == 'Verkauf') ? Transaction.TransTypes.SELL : Transaction.TransTypes.BUY
					transaction.setType(transType)
					transaction.setName(buy[idxName])
					transaction.setAmount(match.group('amt'))
					parseRate(transaction, match.group('prc'))
					parseCurrency(transaction, buy[idxCurrency])
					parseDate(transaction, buy[idxValuta])

					if (buy.size() <= idxOrderId) {
						if (!buy[idxDesc].startsWith('AKTIENSPLIT') && !buy[idxDesc].startsWith('SPIN-OFF')) {
							throw new Exception("Line fields are less than expected: $buy")
						} else {
							transaction.id = "Split${transaction.amount}#${transaction.getDate()}"
						}
					} else {
						parseId(transaction, buy[idxOrderId])
					}
					parseName(transaction, buy[idxName])
					parseIsin(transaction, buy[idxIsin])
					parseTotal(transaction, buy[idxTotal])
					parseSaldo(transaction, buy[idxSaldo])

					def transFees = fees.findAll {fee ->
						return fee[idxIsin] == buy[idxIsin] && fee[idxOrderId] == buy[idxOrderId]
					}
					transFees.each { transFee ->  transaction.addTax(transFee[idxTotal]) }
					fees.removeAll(transFees)
					transactions << transaction
				}
			}
		}

		// fix currency
		transactions.findAll { it.currency != Transaction.Currency.EUR }.each { Transaction transaction ->
			def rate = getExchangeRateByDate(transaction.getDateAsStr("yyyy-MM-dd"))
			// println "${transaction.currency} $rate"
			transaction.setTotal(transaction.total * rate, false)
			transaction.setTax(transaction.getTax() * rate)
		}

		return transactions
	}

	static Double getExchangeRateByDate(String date) {
		def fromCurrency = "EUR"
		def toCurrency = "USD"
		if (fromCurrency != 'EUR') {
			throw new IllegalStateException('Current API does only support EUR base currency')
		}

		def key = "${date}-${fromCurrency}-${toCurrency}"
		def properties = new Properties()
		def propertiesFile = new File("exchangerates.properties")
		if (propertiesFile.exists()) {
			properties.load(propertiesFile.newInputStream());
			def value = properties.get(key.toString())
			if (value) {
				return value.toString().toDouble();
			}
		}

		def url = new URL("http://api.exchangeratesapi.io/v1/${date}?access_key=6151a8a197574d13bf1c58875acbb360&base=${fromCurrency}&symbols=${toCurrency}")
		def json = new JsonSlurper().parse(url)
		def rate = 1/json['rates'][toCurrency].toString().toDouble()
		if (rate) {
			properties.setProperty(key, rate.toString())
			properties.store(propertiesFile.newOutputStream(), "")
		}

		return rate
	}

	@Override
	Map<String, Closure> getOverwrites() {
		def overwrites = [:]
		return overwrites
	}

	@Override
	boolean isPreEmptiveRights(String content) {
		return false
	}

	@Override
	boolean isDividend(String content) {
		return false
	}

	boolean isBuy(String content) {
		throw new IllegalAccessError('Use parseCSV')
	}

	boolean isSell(String content) {
		throw new IllegalAccessError('Use parseCSV')
	}

	boolean isKnockout(String content) {
		return false
	}

	@Override
	boolean isIsinSwitch(String content) {
		return false
	}

	void parseId(Transaction transaction, String content) {
		transaction.id = content
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String content) {
		transaction.currency = Transaction.Currency.EUR
		if (content != transaction.currency.name()) {
			transaction.currency = Transaction.Currency.fromString(content)
		}

		Exceptions.assertTrue(transaction.currency != Transaction.Currency.UNKOWN, "Could not parse currency\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		transaction.date = content
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
	}

	@Override
	void parseTotal(Transaction transaction, String field) {
		transaction.setTotal(field, false)
	}

	void parseSaldo(Transaction transaction, String field) {
		def saldo = Utils.toDouble(field)
		transaction.setDeGiroSaldo(saldo)
	}

	@Override
	void parseFee(Transaction transaction, String content) {
		transaction.addFee(content.split(separator)[14], false)
	}

	@Override
	void parseTax(Transaction transaction, String content) {
		transaction.addTax("0")
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
		transaction.isin = content
	}

	@Override
	void parseName(Transaction transaction, String content) {
		transaction.name = Utils.fixIsinName(content)
	}

	@Override
	void parseRate(Transaction transaction, String field) {
		def rate = field
		if (rate.contains('.') && !rate.contains(',')) {
			rate = rate.replace('.', ',')
		}
		transaction.setRate(rate)

//		if (!transaction.isEUR() || transaction.isKnockout()) {
//			def exchangeRate = Utils.toDouble(field)
// 			def exrate = transaction.rate / exchangeRate // transaction.getRateFromTotal(false) ?: 0
//			transaction.setRate(exrate)
//		} else if (transaction.isTax()) {
		if (transaction.isTaxes()) {
			transaction.setRate(0)
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}

	void parseForZinsen(List<Transaction> transactions, List<String[]> csvLines) {
		csvLines.findAll { it[idxDesc].startsWith('Zinsen') || it[idxDesc].startsWith('Flatex Interest') }.each { line ->
			def transaction = new Transaction(OrderParserType.DEGIRO)
			transaction.setType(Transaction.TransTypes.INTEREST)
			transaction.setAmount(1d)
			parseCurrency(transaction, line[idxCurrency])
			transaction.setName('Zinsen')
			transaction.setDate(line[idxValuta])
			transaction.setId('ZI-' + transaction.dateAsStr)
			transaction.setIsin('XX0000000000')
			parseTotal(transaction, line[idxTotal])
			parseRate(transaction, line[idxTotal])
			transactions << transaction
		}
	}

	void parseForADRs(List<Transaction> transactions, List<String[]> csvLines) {
		csvLines.findAll { it[idxDesc].startsWith('ADR/GDR Weitergabegebühr') }.each { line ->
			def transaction = new Transaction(OrderParserType.DEGIRO)
			transaction.setType(Transaction.TransTypes.FEE)
			transaction.setAmount(1d)
			parseCurrency(transaction, line[idxCurrency])
			transaction.setName('ADR/GDR Gebühr')
			transaction.setDate(line[idxValuta])
			transaction.setId('FEE-' + transaction.dateAsStr)
			transaction.setIsin(line[idxIsin])
			parseTotal(transaction, line[idxTotal])
			parseRate(transaction, line[idxTotal])
			transactions << transaction
		}
	}
}
