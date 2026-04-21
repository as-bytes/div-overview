package com.asbytes.parser

import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils

abstract class OrderParser {
	OrderParserType parserType

	OrderParser(OrderParserType parserType) {
		this.parserType = parserType
	}

	public abstract boolean isPreEmptiveRights(String content)
	public abstract boolean isDividend(String content)
	public abstract boolean isBuy(String content)
	public abstract boolean isSell(String content)
	public abstract boolean isKnockout(String content)
	public abstract boolean isIsinSwitch(String content)

	public boolean isInterest(String content) {
		return false
	}

	public boolean isFeeType(String content) {
		return false
	}

	public boolean isSpinOff(String content) {
		return false
	}

	public boolean isUnsupported(Transaction transaction, String content) {
		return false
	}

	public boolean isExecuted(String line) {
		return true
	}

	public boolean isTax(String content) {
		return false
	}

	public abstract Map<String, Closure> getOverwrites()

	/**
	 * must occur after total
	 */
	public abstract void parseRate(Transaction transaction, String content)

	/**
	 * must occur after total to generate id for div/kno...
	 */
	public abstract void parseId(Transaction transaction, String content)

	public abstract void parseDate(Transaction transaction, String content)

	public abstract void parseAmount(Transaction transaction, String content)

	public abstract void parseTotal(Transaction transaction, String content)

	public abstract void parseFee(Transaction transaction, String content)

	public abstract void parseTax(Transaction transaction, String content)

	public abstract void parseIsin(Transaction transaction, String content)

	public abstract void parseName(Transaction transaction, String content)

	public void parseInterest(Transaction transaction, String content) {
	}

	public List<Transaction> parseCsv(File csv, int firstLine = 0) {
		OrderParser parserCsv = this;
		def transactions = []
		csv.eachLine(firstLine) { String line, int number ->
			if (number <= firstLine || line.trim().length() == 0) {
				return
			}

			def transaction = new Transaction(this.parserType, csv)
			try {
				parserCsv.parseType(transaction, line)
			} catch(ignored) {
				throw ignored
			}

			if (transaction.type != Transaction.TransTypes.UNSUPPORTED && this.isExecuted(line)) {
				if (this.parserType == OrderParserType.SMARTBROKER) {
					(this as ParserSmartBroker).parseStatus(transaction, line)
				}

				parserCsv.parseDate(transaction, line)
				parserCsv.parseIsin(transaction, line)

				if (!transaction.isIsinSwitch()) {
					parserCsv.parseCurrency(transaction, line)
					parserCsv.parseName(transaction, line)
					parserCsv.parseAmount(transaction, line)
					parserCsv.parseTax(transaction, line)

					if (transaction.isInterest()) {
						parserCsv.parseInterest(transaction, line)
					}

					// total does not contain fee/taxes, so both have to appear before for scalable
					if (transaction.isScalable()) {
						parserCsv.parseRate(transaction, line)
						parserCsv.parseFee(transaction, line)
						parserCsv.parseTotal(transaction, line)
					} else {
						parserCsv.parseTotal(transaction, line)
						// must occur after total to use getRateFromTotal
						parserCsv.parseRate(transaction, line)
						parserCsv.parseFee(transaction, line)
					}

					// must occur after total to generate id for div/kno...
					if (line == null || line == 'null') {
						throw new Exception('is null for line: ' + line)
					}

					this.parseId(transaction, line)

					def overwrite = this.getOverwrites().getOrDefault(transaction.id, null)
					if (overwrite) {
						overwrite(transaction)
						transaction.overridden = true
					}

					transactions.push(OrderParser.validate(transaction, line))
				}
			}
			return
		}
		return transactions
	}

	static Transaction validate(Transaction transaction, String line = "") {
		assert transaction.date: transaction
		assert transaction.type != Transaction.TransTypes.UNKOWN: transaction
		/*if (!isDiv()) {
			assert id && !id.contains("null") && id.split("#").size() == 2: this
		}
		*/
		if (!transaction.isKnockout() && !transaction.isTaxes() && !transaction.isDiv() && !transaction.isOverridden() && !transaction.isCapital() && !transaction.spinOff) {
			assert transaction.name: transaction
			assert transaction.amount && transaction.amount > 0: transaction
		}

		// todo this is very specific scalable has fees not associated with transactions, this should be checked in the parser itself like (assertIsin)
		if (!transaction.isTaxes() && !transaction.isInterest() && !transaction.crypto && !transaction.isFeeType()) {
			assert transaction.isin && transaction.isin.size() == 12: transaction
		}

		assert transaction.rate != null && !Double.isNaN(transaction.rate): transaction
		if ((transaction.isBuy() || transaction.isSell()) && !transaction.spinOff) {
			assert transaction.rate > 0: transaction
		}
		assert transaction.total != null && transaction.total != "": transaction

		if (!transaction.isKnockout() && !transaction.isDiv() && !transaction.isPreEmptive() && !transaction.isCapital() && !transaction.spinOff) {
			assert transaction.total != 0: transaction
		}

		Double calcedTotal = transaction.getTotalFromRate()
		// TODO hack for rounding issue with stupid Trade Republic -.-
		if (calcedTotal && calcedTotal != transaction.total) {
			def diff = (transaction.amount == 0) ? 0 : Math.abs(calcedTotal-transaction.total) / transaction.amount
			if (transaction.crypto) {
				calcedTotal = transaction.getTotalFromRate(3)
				diff = (transaction.amount == 0) ? 0 : Math.abs(calcedTotal-transaction.total)
			}
			assert diff < 0.01d: "$calcedTotal != ${transaction.total} rate-diff too high (${diff}):\n$transaction"
		} else {
			try {
				def total = transaction.total + 0.0d
				assert calcedTotal == total: "$calcedTotal != ${total} (${total-calcedTotal}: $transaction"
			} catch(err) {
				println err
			}
		}

		return transaction
	}

	def handleIsinSwitch(String content) {
		throw new IllegalAccessError('isin switch not implemented')
	}

	void parseCurrency(Transaction transaction, String content) {
		transaction.currency = Transaction.Currency.EUR
		def patterns = []
		patterns << /(?m)^Zwischensumme (?:[\d,.]+) EUR\/([A-Z]{3,4}) (?:[\d,.]+) EUR$/
		patterns << /(?m)Kurswert (?:[\d,. ]+) (?!EUR)([A-Z]+)/
		patterns << /(?m)Kurswert (?!EUR)([A-Z]+)/
		patterns << /(?m)Kurswert (?:[\d,.]+) (?!EUR)/
		patterns << /(?m);EUR;/

		def currencyConversion = patterns.findResult { Utils.find1stGroup(content, it) }
		if (currencyConversion) {
			transaction.currency = Transaction.Currency.fromString(currencyConversion)
		}

		Exceptions.assertTrue(transaction.currency != Transaction.Currency.UNKOWN, "Could not parse currency\n$transaction")
	}

	def isKapitalErhoehung(String content) {
		return content.contains('KAPITALERHÖHUNG GEGEN BAR') && content.contains('Einbuchung Bezugsrechte')
	}

	def boolean isSplit(String content) {
		return false
	}

	void parseType(Transaction transaction, final String content) {
//		println "content:"
//		println content
		if (isUnsupported(transaction, content)) {
			transaction.type = Transaction.TransTypes.UNSUPPORTED
		} else if (isBuy(content)) {
			transaction.type = Transaction.TransTypes.BUY
		} else if (isSpinOff(content)) {
			transaction.type = Transaction.TransTypes.BUY
			transaction.spinOff = true
		} else if (isSell(content)) {
			transaction.type = Transaction.TransTypes.SELL
		} else if (isDividend(content)) {
			transaction.type = Transaction.TransTypes.DIV
		} else if (isInterest(content)) {
			transaction.type = Transaction.TransTypes.INTEREST
		} else if (isFeeType(content)) {
			transaction.type = Transaction.TransTypes.FEE
		} else if (isKnockout(content)) {
			transaction.type = Transaction.TransTypes.KNO
		} else if (isTax(content)) {
			transaction.type = Transaction.TransTypes.TAX
		} else if (isKapitalErhoehung(content)) {
			transaction.type = Transaction.TransTypes.CAPITAL
		} else if (isSplit(content)) {
			// either create a buy and sell with 0€ total or change the amount into negative???
			transaction.type = Transaction.TransTypes.SPLIT
		} else  if (isIsinSwitch(content)) {
			// ignore for now, handled using overrwites (rewrite isin)
			transaction.type = Transaction.TransTypes.ISINSWITCH
//			def type = (content.contains('Umtausch Eingang')) ? Transaction.TransTypes.ISINSWITCHIN : null
//			type = type ?: (content.contains('Umtausch Ausgang')) ? Transaction.TransTypes.ISINSWITCHOUT : null
//			assert type != null: 'Ein-/Ausgang konnte nicht ermittelt werden'
//			transaction.type = type
		} else if (isPreEmptiveRights(content)) {
				transaction.type = Transaction.TransTypes.PREEMPTIVE
		} else {
			Exceptions.assertNotNull(transaction.type, "Type undefined\n${transaction}\n${content}\n\n")
		}
	}
}
