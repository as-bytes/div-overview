package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@Deprecated
@InheritConstructors
class ParserFinanzenZeroCsv extends OrderParser {
	def separator = ';'
	def idxDatum = 0
	def idxTotal = 2
	def idxStatus = 4

	def idxName = 5
	def idxIsin = 5
	def idxType = 5

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
	public boolean isUnsupported(Transaction transaction, String content) {
		def type = content.split(this.separator)[idxType]
		def isAccountMovement = type?.startsWith("Gutschrift:")
		if (isAccountMovement) {
			return true
		}

		def isAccountWithdrawal = type?.contains("Auszahlung auf Referenzkonto")
		if (isAccountWithdrawal) {
			return true
		}

		def isExecuted = content.split(this.separator)[idxStatus].equalsIgnoreCase('gebucht')
		if (isExecuted) {
			return false
		}

		return true
	}

	@Override
	boolean isDividend(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.startsWith("Coupons/Dividende:")
	}

	@Override
	boolean isBuy(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.containsIgnoreCase("- Kauf")
	}

	@Override
	boolean isSell(String line) {
		def type = line.split(this.separator)[idxType]
		return type?.containsIgnoreCase("- Verkauf") || type?.containsIgnoreCase("Abrechnung Verkauf")
	}

	@Override
	boolean isKnockout(String line) {
		return false
	}

	@Override
	boolean isTax(String line) {
		return line.split(this.separator)[idxType].contains('Steuerausgleich')
	}

	@Override
	boolean isFeeType(String line) {
		return line.split(this.separator)[idxType].contains('KKT-Abschluss')
	}

	@Override
	boolean isIsinSwitch(String line) {
		return false
	}

	boolean isCrypto(Transaction transaction) {
		return (transaction.isin) ? transaction.isin.startsWithAny('XX', 'XC') : false
	}

	@Override
	void parseId(Transaction transaction, String line) {
		// todo why these have no id?
		def unknownId = line.containsIgnoreCase("WP-Abrechnung")


		if (!transaction.isDiv() && !transaction.isTaxes() && !isCrypto(transaction)) {
			def content = line.split(this.separator)[idxName]
			transaction.id = Utils.find1stGroup(content, /(?m)Order Nr (\d+) ISIN/)

			// todo why these have no id?
			if (!transaction.id) {
				transaction.setId("n/a#" + transaction.isin + "#" + transaction.dateAsStr)
			}

			Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
		} else if (transaction.isDiv()) {
			transaction.setId("div#" + transaction.isin + "#" + transaction.dateAsStr)
		} else if (transaction.isTaxes()) {
			transaction.setId("tax#" + transaction.dateAsStr)
		} else if (isCrypto(transaction)) {
			transaction.setId("crypto#" + transaction.isin + "#" + transaction.dateAsStr)
		}

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
		def content = line.split(this.separator)[idxName]
		transaction.amount = Utils.find1stGroup(content, /(?m)STK\s+([,\.\d]+)/)
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String line) {
		transaction.setTotal(line.split(this.separator)[idxTotal].replace('-', ''))
	}

	@Override
	void parseFee(Transaction transaction, String line) {
		transaction.addFee(0, false)
	}

	@Override
	void parseTax(Transaction transaction, String line) {
		// TODO gibt's nur im PDF :( muss nachträglich nach WinLossFifo gemacht werden
		// bei kontoumsaetze*.csv ist die Steuer abgezogen
		// bei orders*.csv ist die Steuer nicht abgezogen

		// is actual PostTax
		if (transaction.isTaxes()) {
			transaction.addTax(transaction.total)
		}
	}

	@Override
	void parseIsin(Transaction transaction, String line) {
		def pattern1 = /(?m)ISIN ([A-Z]{2}[0-9A-Z]{10})/
		def pattern2 = /(?m)ISIN ([A-Z]{2}[0-9A-Z]{10})/
		transaction.isin = [pattern1, pattern2].findResult { Utils.find1stGroup(line.split(separator)[idxIsin], it) }
	}

	@Override
	void parseName(Transaction transaction, String line) {
		if (!transaction.isDiv() && !transaction.isTaxes()) {
			def pattern = /(?m)\((.+) ISIN/
			def name = [pattern].findResult { Utils.find1stGroup(line.split(separator)[idxName], it) }
			try {
				transaction.name = Utils.fixIsinName(name)
			} catch(ignore) {
				throw ignore
			}
		} else if (transaction.isTaxes()) {
			transaction.name = line.split(separator)[idxName]
		}
	}

	@Override
	void parseRate(Transaction transaction, String line) {
		/* bei CSV kontoumsaetze ist die Steuer in der Rate!!! */
		def rate = transaction.getRateFromTotal(false) ?: 0
		transaction.setRate(rate)
		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}

}
