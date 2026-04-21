package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import groovy.transform.InheritConstructors

/**
 * Parsing Account.csv
 */
@InheritConstructors
class ParserBitpanda extends OrderParser {
//Typ;Datum/Zeit;Symbol;Rate;Anzahl;Betrag;Plattform
	int fieldCount = 16
	int idxOrderId = 0
	int idxDate = 1
	int idxType = 2
	int idxTotal = 4
	int idxAmount = 6
	int idxName = 7
	int idxIsin = 7
	int idxRate = 8
	int idxCategory = 10

	final String separator = ','

	@Override
	List<Transaction> parseCsv(File file, int firstLine) {
		def csvLines = file.readLines()
		def datalines = csvLines.findAll { it.trim().size() > 0 && it.split(this.separator).size() == fieldCount }
		datalines.remove(0);
		return parseCsvLines(datalines);
	}

	List<Transaction> parseCsvLines(List<String> csvLines) {
		// TODO throw new Exception('OpenInvest2021 nur 5 €?!?!?!')
		def transactions = [] as List<Transaction>

		csvLines.each { line ->
			if (line.split(separator)[idxCategory].contains('Stock')) {
				def transaction = new Transaction(OrderParserType.BITPANDA)
				parseType(transaction, line);
				parseAmount(transaction, line);
				parseRate(transaction, line)
				parseId(transaction, line)
				parseName(transaction, line)
				parseIsin(transaction, line)
				parseDate(transaction, line)
				parseTotal(transaction, line)
				transactions << transaction
			}
		}
		return transactions
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
		def type = content.split(separator)[idxType].trim();
		return  type == 'buy' || type == 'transfer'
	}

	boolean isSell(String content) {
		return content.split(separator)[idxType].trim() == 'sell'
	}

	boolean isKnockout(String content) {
		return false
	}

	@Override
	boolean isIsinSwitch(String content) {
		return false
	}

	void parseId(Transaction transaction, String content) {
		transaction.id = content.split(this.separator)[idxOrderId]
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String content) {
		transaction.currency = Transaction.Currency.EUR
		Exceptions.assertTrue(transaction.currency != Transaction.Currency.UNKOWN, "Could not parse currency\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		transaction.date = content.split(this.separator)[idxDate]
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
		transaction.amount = Double.parseDouble(content.split(separator)[idxAmount])
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String content) {
		transaction.setTotal(content.split(separator)[idxTotal], false);
	}

	@Override
	void parseFee(Transaction transaction, String content) {
		def fee = 0.0d;
		transaction.addFee(fee, false)
	}

	@Override
	void parseTax(Transaction transaction, String content) {
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
		transaction.isin = content.split(separator)[idxIsin]
	}

	@Override
	void parseName(Transaction transaction, String content) {
		transaction.name = content.split(separator)[idxName]
	}

	@Override
	void parseRate(Transaction transaction, String content) {
		transaction.rate = content.split(separator)[idxRate]
		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
