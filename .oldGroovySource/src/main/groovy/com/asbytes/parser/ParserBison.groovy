package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import groovy.transform.InheritConstructors

/**
 * Parsing Account.csv
 */
@InheritConstructors
class ParserBison extends OrderParser {
//Typ;Datum/Zeit;Symbol;Rate;Anzahl;Betrag;Plattform
	int fieldCount = 7
	int idxType = 0
	int idxDate = 1
	int idxName = 2
	int idxIsin = 2
	int idxRate = 3
	int idxAmount = 4
	int idxTotal = 5
	int idxPlatform = 6

	final String separator = ';'

	@Override
	List<Transaction> parseCsv(File file) {
		def csvLines = file.readLines()
		csvLines.remove(0);
		return parseCsvLines(csvLines.findAll { it.trim().size() > 0 && it.split(this.separator).size() == fieldCount })
	}

	List<Transaction> parseCsvLines(List<String> csvLines) {
		def transactions = [] as List<Transaction>

		csvLines.each { line ->
			def transaction = new Transaction(OrderParserType.BISON)
			parseType(transaction, line);
			parseAmount(transaction, line);
			parseRate(transaction, line)
			parseCurrency(transaction, line)
			parseId(transaction, line)
			parseName(transaction, line)
			parseIsin(transaction, line)
			parseDate(transaction, line)
			parseTotal(transaction, line)
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
	boolean isPreEmptiveRights(String content) {
		return false
	}

	@Override
	boolean isDividend(String content) {
		return false
	}

	boolean isBuy(String content) {
		return content.split(separator)[idxType].trim() == 'Kauf'
	}

	boolean isSell(String content) {
		return content.split(separator)[idxType].trim() == 'Verkauf'
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
		Exceptions.assertTrue(transaction.currency != Transaction.Currency.UNKOWN, "Could not parse currency\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		transaction.date = content.split(this.separator)[idxDate]
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
		transaction.amount = content.split(separator)[idxAmount]
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
