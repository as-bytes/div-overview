package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserScalableCapitalWur extends OrderParser {
	def separator = ';'
	def idxId = 117
	def idxName = 11
	def idxIsin = 15
//	def idxStatus =
	def idxType = 9
	def idxTotal = 31 // 31|37|43
	def idxDatum = 5
	def formatDate = "YYYYMMDD"
	def idxUhrzeit = 6
	def formatTime = "HHMMSSMM"
	def idxKurs = 30
	def idxAnzahl = 29 // 29|52

	def Transaction validate(Transaction transaction, String line = "") {
		if (this.isExecuted(line)) {
			OrderParser.validate(transaction, line)
		}
	}

	@Override
	Map<String, Closure> getOverwrites() {
		return [:]
	}

	@Override
	boolean isPreEmptiveRights(String line) {
		return false
	}

	@Override
	boolean isDividend(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.equalsIgnoreCase("DVIDENDE")
	}

	@Override
	boolean isBuy(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.equalsIgnoreCase("KAUF")
	}

	@Override
	boolean isSell(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.equalsIgnoreCase("VERKAUF")
	}

	@Override
	boolean isKnockout(String line) {
		return false
	}

	@Override
	boolean isIsinSwitch(String line) {
		return false
	}

	@Override
	void parseId(Transaction transaction, String line) {
		def fields = line.split(this.separator)
		def orderid = "${fields[idxId]}"
		transaction.id = orderid
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String line) {
		transaction.currency = Transaction.Currency.EUR
	}

	@Override
	void parseDate(Transaction transaction, String line) {
		def fields = line.split(this.separator)
		transaction.date = fields[idxDatum]
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String line) {
		transaction.amount = line.split(this.separator)[idxAnzahl]
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String line) {
		transaction.setTotal(line.split(this.separator)[idxTotal].replace('-', ''))
	}

	@Override
	void parseFee(Transaction transaction, String line) {
		// TODO bisher nix im CSV
		transaction.addFee(0, false)
	}

	@Override
	void parseTax(Transaction transaction, String line) {
		// TODO bisher nix im CSV
		transaction.addTax("0")
	}


	@Override
	void parseIsin(Transaction transaction, String line) {
		transaction.isin = line.split(separator)[idxIsin]
	}

	@Override
	void parseName(Transaction transaction, String line) {
		transaction.name = Utils.fixIsinName(line.split(this.separator)[idxName])
	}

	@Override
	void parseRate(Transaction transaction, String line) {
		transaction.setRate(line.split(this.separator)[idxKurs])

		if (!transaction.isEUR() || transaction.isKnockout()) {
			def rate = transaction.getRateFromTotal(false) ?: 0
			transaction.setRate(rate)
		} else if (transaction.isTaxes()) {
			transaction.setRate(0)
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}

}
