package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors
import org.junit.Test

import java.util.zip.CRC32

/**
 * Parsing Account.csv
 */
@InheritConstructors
class ParserQuirion extends OrderParser {
	def separator = '\t'
	def idxType = 0
	def idxAmountAndName = 1
	def idxDate = 2
	def idxValuta = 3
	def idxTotal = 4


	@Test
	public void testFile() {
		new ParserQuirion(OrderParserType.QUIRION).parseCsv(new File('O:\\OneDrive\\Documents\\Finance\\Quirion\\8317352620.tsv'))
	}

	@Override
	List<Transaction> parseCsv(File file, int firstLine) {
		def tsvLines = file.readLines()
		tsvLines.remove(0)
		return parseLines(tsvLines, file)
	}

	private List<Transaction> parseLines(List<String> lines, File file) {
		def transactions = [] as List<Transaction>

		def transCounter = 1


		List<String> buys = []
		List<String> sells = []
		List<String> divs = []
		List<String> taxes = []
		List<String> fees = []

		lines.each { line ->
			def fields = line.split(separator)
			def fieldName = fields[idxAmountAndName]
			if (isDividend(line)) {
				divs << fields
			} else if (isTax(line)) {
				taxes << fields
			} else if (isFeeType(line)) {
				fees << fields
			} else {
				def amount = fieldName.matches(/^(\d+,\d+)\s.+/)
				if (amount) {
					if (isBuy(line)) {
						buys << fields
					} else if (isSell(line)) {
						sells << fields
					} else {
						if (fields[idxTotal].startsWith('-')) {
							buys << fields
						} else {
							sells << fields
						}
					}
				} else {
					def fieldType = fields[idxType]
					def investTypes = fieldType == 'Umbuchung'
							|| fieldType == 'Lastschrifteingang' || fieldType == 'Lastschriftausgang'
							|| fieldType == 'Überweisungseingang'|| fieldType == 'Überweisungsausgang'
					def abschluss = fields.length == 3 || fieldName.trim().length() == 0
					if (!investTypes && !abschluss) {
						if (fieldName.startsWith('Storno')) {
							// find sell and buy with same amount
							def stornoBuys = buys.findAll { Utils.toDouble(it[idxTotal]).abs() == Utils.toDouble(fields[idxTotal]).abs() && fieldName.contains(it[idxAmountAndName])}
							def stornoSells =  sells.findAll { Utils.toDouble(it[idxTotal]).abs() == Utils.toDouble(fields[idxTotal]).abs() && fieldName.contains(it[idxAmountAndName])}
							if (stornoBuys.size() + stornoSells.size() == 0) {
								throw new IllegalStateException("Cannot find match for Storno: $line")
							} else if (stornoBuys.size() + stornoSells.size() > 1) {
								throw new IllegalStateException("Cannot find unique match for Storno: $line\nBuys: ${stornoBuys}\nSell: ${stornoSells}")
							} else {
								if (stornoBuys.size() == 1) {
									buys.remove(stornoBuys.first())
								}
								if (stornoSells.size() == 1) {
									sells.remove(stornoSells.first())
								}
							}
						} else {
							throw new IllegalStateException("Unkown transaction type for line: $fieldName")
						}
					}
				}
			}
		}

		fees.each { fields ->
			def transaction = new Transaction(OrderParserType.QUIRION, file)
			transaction.setType(Transaction.TransTypes.INTEREST)
			transaction.setAmount(1d)
			transaction.setName('')

			transaction.setDate(fields[idxValuta])
			transaction.setId("fee#${transCounter++}")
			parseTotal(transaction, fields[idxTotal])
			transaction.setRate(transaction.getRateFromTotal(false).round(2))

			transactions << transaction
		}

		taxes.each { fields ->
			def fieldName = fields[idxAmountAndName]

			def transaction = new Transaction(OrderParserType.QUIRION, file)
			transaction.setType(Transaction.TransTypes.TAX)
			transaction.setAmount(1d)

			if (fieldName.startsWith('Steuerereignisse') || fieldName.startsWith('Taxation correction')) {
				transaction.setName(fieldName.replace('Steuerereignisse', '').replace('Taxation correction', '').trim())
				def crc = new CRC32()
				crc.update(transaction.name.getBytes())
				transaction.setIsin("Quirion" + Long.toHexString(crc.getValue()))
			}

			transaction.setDate(fields[idxValuta])
			transaction.setId("tax#${transCounter++}")
			parseTotal(transaction, fields[idxTotal])
			transaction.setRate(transaction.getRateFromTotal(false).round(2))

			transactions << transaction
		}

		divs.each { fields ->
			def fieldName = fields[idxAmountAndName]

			def transaction = new Transaction(OrderParserType.QUIRION, file)
			transaction.setType(Transaction.TransTypes.DIV)
			transaction.setAmount(1d)
			transaction.setName(Utils.fixIsinName(fieldName))

			def crc = new CRC32()
			crc.update(transaction.name.getBytes())
			transaction.setIsin("Quirion" + Long.toHexString(crc.getValue()))
			transaction.setDate(fields[idxValuta])
			transaction.setId("div#${transCounter++}")
			parseTotal(transaction, fields[idxTotal])
			transaction.setRate(transaction.getRateFromTotal(false).round(2))

			transactions << transaction
		}

		(buys + sells).each { fields ->
			def fieldName = fields[idxAmountAndName]

			def transaction = new Transaction(OrderParserType.QUIRION, file)
			transaction.setName(Utils.fixIsinName(fieldName.split(' ').drop(1).join(' ')))
			transaction.setAmount(Utils.find1stGroup(fieldName, /(?m)^(\d+,\d+)\s.+/))
			parseTotal(transaction, fields[idxTotal])

			if (transaction.total > 0) {
				transaction.setType(Transaction.TransTypes.SELL)
			} else if (transaction.total < 0) {
				transaction.setType(Transaction.TransTypes.BUY)
			} else {
				throw new IllegalStateException("Could not determine buy or sell with amount being ${amount}")
			}

			def crc = new CRC32()
			crc.update(transaction.name.getBytes())
			transaction.setIsin("Quirion" + Long.toHexString(crc.getValue()))
			transaction.setDate(fields[idxValuta])
			transaction.setId("${transaction.type.name().toLowerCase()}#${transCounter++}")
			transaction.setRate(transaction.getRateFromTotal(false).round(2))

			transactions << transaction
		}

		return transactions
	}

	@Override
	Map<String, Closure> getOverwrites() {
		def overwrites = [:]
		return overwrites
	}

	@Override
	boolean isPreEmptiveRights(String line) {
		return false
	}

	@Override
	boolean isDividend(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("Ertragsgutschrift")
	}

	boolean isBuy(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("Wertpapierkauf")
	}

	boolean isSell(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("Wertpapierverkauf")
	}

	boolean isKnockout(String line) {
		return false
	}

	boolean isFeeType(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("Gebühreneinzug")
	}

	boolean isTax(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("Steuerbuchung")
	}

	@Override
	boolean isIsinSwitch(String line) {
		return false
	}

	void parseId(Transaction transaction, String content) {
		transaction.id = content
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String content) {
		transaction.currency = Transaction.Currency.EUR
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

		if (transaction.isTaxes()) {
			transaction.setRate(0)
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
