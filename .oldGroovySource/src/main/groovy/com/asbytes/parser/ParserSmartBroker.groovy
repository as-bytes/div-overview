package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserSmartBroker extends OrderParser {

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
		def type = content.split(';')[2]
		return type?.equalsIgnoreCase("Dividenden, Erträge") || type?.equalsIgnoreCase("Ausschüttung")
	}

	boolean isBuy(String content) {
		def type = content.split(';')[2]
		return type?.equalsIgnoreCase("KAUF")
	}

	boolean isSell(String content) {
		def type = content.split(';')[2]
		return type?.equalsIgnoreCase("VERKAUF")
	}

	boolean isKnockout(String content) {
		def type = content.split(';')[2]
		return type?.equalsIgnoreCase("AUSBUCHUNG")
	}

	boolean isTax(String content) {
		return false
	}

	@Override
	boolean isIsinSwitch(String content) {
		return false
	}

	void parseId(Transaction transaction, String content) {
		transaction.id = content.split(';')[4]
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	void parseStatus(Transaction transaction, String content) {
		def status = content.split(';')[15]
		transaction.setSmartBrokerStatus(status)
		Exceptions.assertNotNull(transaction.smartBrokerStatus, "Could not parse status \"$status\"\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String content) {
		transaction.currency = Transaction.Currency.EUR
		def currencyConversion = content.split(';')[14]
		if (currencyConversion) {
			transaction.currency = Transaction.Currency.fromString(currencyConversion)
		}

		// TODO handle CHF

		Exceptions.assertTrue(transaction.currency != Transaction.Currency.UNKOWN, "Could not parse currency\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		def fields = content.split(';')
		assert fields.length > 18: "Missing field 19 in $content"
		if (transaction.isDiv()) {
			transaction.date = fields[3]
		} else {
			transaction.date = fields[19]
		}
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
		transaction.amount = content.split(';')[5].replace('Stück', '')
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	public static double getUsd2EurRate() {
		def url = new URL('https://api.exchangeratesapi.io/latest')
		def json = new groovy.json.JsonSlurper().parse(url)
		return json['rates']['USD'].toString().toDouble()
	}

	@Override
	void parseTotal(Transaction transaction, String content) {
		// Smartbroker nutzt Dollar-Konto, daher aktuellen USD-Kurs nehmen
		transaction.setTotal(content.split(';')[18].replace('-', ''))
		if (!transaction.isEUR() && transaction.isDiv()) {
			def rate = ParserDeGiro.getExchangeRateByDate(transaction.getDateAsStr("yyyy-MM-dd"))
			def inEur = transaction.total * rate;
			transaction.setTotal(inEur)
		}
	}

	@Override
	void parseFee(Transaction transaction, String content) {
		// def pattern = /(?m)^Fremdkostenzuschlag ([-\d,. ]+) EUR$/
		// TODO nur im PDF -.-

		def totalFromRate = Math.abs(transaction.getTotalFromRate())
		def totalFromFile = Math.abs(transaction.total)

		if (transaction.isSell() && totalFromRate == totalFromFile && !transaction.parserType.isSmartBroker()) {
			throw new IllegalStateException('Rate of total is total, no fee?')
		}

		def fee = Math.abs(totalFromRate - totalFromFile)
		transaction.addFee(fee.round(3), true)
	}

	@Override
	void parseTax(Transaction transaction, String content) {
		// TODO nur im PDF -.-
		transaction.addTax("0")
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
		transaction.isin = content.split(';')[7]
	}

	@Override
	void parseName(Transaction transaction, String content) {
		transaction.name = Utils.fixIsinName(content.split(';')[6])
	}

	@Override
	void parseRate(Transaction transaction, String content) {
		transaction.setRate(content.split(';')[16])

		if (!transaction.isEUR() || transaction.isKnockout()) {
			def rate = transaction.getRateFromTotal(false) ?: 0
			transaction.setRate(rate)
		} else if (transaction.isTaxes()) {
			transaction.setRate(0)
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
