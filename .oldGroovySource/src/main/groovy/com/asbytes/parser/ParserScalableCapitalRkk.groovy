package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserScalableCapitalRkk extends OrderParser {
	def separator = ';'
	// def idxId = 117
	def idxName = 9
	def idxIsin = 10
//	def idxStatus =
	def idxType = 8
	def idxTotal = 13 // 31|37|43
	def idxDatum = 5
	def formatDate = "YYYYMMDD"
//	def idxUhrzeit = 6
//	def formatTime = "HHMMSSMM"
//	def idxKurs = 30
	def idxAnzahl = 11 // 29|52


//	public isExecuted(String line) {
//		return true
//		//line.split(this.separator)[idxStatus].equalsIgnoreCase('ausgeführt')
//	}

//	@Override
//	def Transaction validate(Transaction transaction, String line = "") {
//		if (this.isExecuted(line)) {
//			OrderParser.validate(transaction, line)
//		}
//	}

	@Override
	Map<String, Closure> getOverwrites() {
		return [:]
	}

	@Override
	boolean isPreEmptiveRights(String line) {
		return false
	}

	@Override
	public boolean isUnsupported(Transaction transaction, String line) {
		String type = line.split(this.separator)[idxType]?.trim()?.toLowerCase() ?: ''
		return type in ['saldovortrag', 'saldo', 'gutschrift', 'lastschrift aktiv', 'sepa-ueberweisung']
	}

	@Override
	boolean isDividend(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.trim()?.startsWithIgnoreCase("Coupons/Dividende")
	}

	@Override
	boolean isBuy(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("WP-Abrechnung Kauf")
	}

	@Override
	boolean isSell(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("WP-Abrechnung Verkauf")
	}

	@Override
	boolean isTax(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWithIgnoreCase("Steuerausgleich")
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
//		def fields = line.split(this.separator)
//		def orderid = "${fields[idxId]}"
		transaction.id = System.nanoTime()
//		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String line) {
		transaction.currency = Transaction.Currency.EUR
	}

	@Override
	void parseDate(Transaction transaction, String line) {
		def fields = line.split(this.separator)
		transaction.date = fields[idxDatum].trim()
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String line) {
		if (transaction.taxes) {
			// there is no value in CSV -.-
			transaction.amount = '0'
		} else {
			transaction.amount = line.split(this.separator)[idxAnzahl].trim().replace("STK ", "").toString()
		}
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String line) {
		if (transaction.taxes) {
			// there is no value in CSV -.-
			transaction.setTotal(0)
		} else {
			transaction.setTotal(line.split(this.separator)[idxTotal].replace('-', ''))
		}
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
		transaction.isin = line.split(separator)[idxIsin].trim().replace('ISIN ', '')
	}

	@Override
	void parseName(Transaction transaction, String line) {
		transaction.name = Utils.fixIsinName(line.split(this.separator)[idxName])
	}

	@Override
	void parseRate(Transaction transaction, String line) {
		def rate = transaction.getRateFromTotal(false) ?: 0
		transaction.setRate(rate)
		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
