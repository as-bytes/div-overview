package com.asbytes.parser

/*
	Todo:
	Corporate Action für CNR... bisher nicht umgesetzt, keine Ahnung was das sollte hat wohl keinen Effekt auf bisherigen Bestand

	Interest (KKT-Abschluss) noch nicht im HTML / XSL implementiert?
 */

import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserScalableCapitalCsv extends OrderParser {
	def separator = ';'

	def idxDate = 0
	def idxTime = 1
	def idxStatus = 2
	def idxId = 3
	def idxName = 4
//	def idxAssetType = 5
	def idxType = 6
	def idxIsin = 7
	def idxAmount = 8
	def idxRate = 9
	def idxTotal = 10
	def idxFee = 11
	def idxTax = 12
	def idxCurrency = 13

	@Override
	Map<String, Closure> getOverwrites() {
		return [:]
	}

	Closure<String> getFieldOfLine = { String line, Integer idxField -> return line.split(this.separator)[idxField] ?: '' };

	@Override
	public boolean isUnsupported(Transaction transaction, String line) {
		String type = this.getFieldOfLine(line, idxType)?.toLowerCase() ?: ''
		return type in ['withdrawal', 'deposit', 'corporate action']
	}

	@Override
	public boolean isExecuted(String line) {
		return this.getFieldOfLine(line, idxStatus).equalsIgnoreCase('Executed');
	}

	@Override
	boolean isTax(String line) {
		return this.getFieldOfLine(line, idxType).equalsIgnoreCase('Taxes');
//		return type?.equalsIgnoreCase("Taxes") || type?.equalsIgnoreCase("TaxRefund")
	}

	@Override
	boolean isPreEmptiveRights(String line) {
		return false
	}

	@Override
	boolean isInterest(String line) {
		return this.getFieldOfLine(line, idxType).equalsIgnoreCase("Interest")
	}

	@Override
	boolean isFeeType(String line) {
		return this.getFieldOfLine(line, idxType).equalsIgnoreCase("Fee")
	}

	@Override
	boolean isDividend(String line) {
		return this.getFieldOfLine(line, idxType).equalsIgnoreCase("Distribution")
	}

	@Override
	boolean isBuy(String line) {
		def type = this.getFieldOfLine(line, idxType);
		return type.equalsIgnoreCase("Buy") || type.equalsIgnoreCase("Savings plan")
	}

	@Override
	boolean isSell(String line) {
		return this.getFieldOfLine(line, idxType).equalsIgnoreCase("Sell")
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
		transaction.id = this.getFieldOfLine(line, idxId)
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseCurrency(Transaction transaction, String line) {
		Exceptions.throwOnFalse(this.getFieldOfLine(line, idxCurrency).equalsIgnoreCase('EUR'), "Non-EUR-handling not implemented for Scalable:\n$transaction")
		transaction.currency = Transaction.Currency.EUR
	}

	@Override
	void parseDate(Transaction transaction, String line) {
		transaction.date = "${this.getFieldOfLine(line, idxDate)} ${this.getFieldOfLine(line, idxTime)}"
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String line) {
		transaction.setAmount(this.getFieldOfLine(line, idxAmount))
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String line) {
		transaction.setTotal(this.getFieldOfLine(line, idxTotal), false)

		if (transaction.isFeeType()) {
			return;
		}

		// total in csv is exclusive taxes and fee's
		if (!transaction.isTaxes()) {
			transaction.addTotal(transaction.tax + transaction.fee, false)
		} else {
			transaction.addTotal(transaction.fee, false)
		}
	}

	@Override
	void parseFee(Transaction transaction, String line) {
		if (transaction.isFeeType()) {
			transaction.addFee(this.getFieldOfLine(line, idxTotal), false)
		} else {
			transaction.addFee(this.getFieldOfLine(line, idxFee), true)
		}
	}

	@Override
	void parseInterest(Transaction transaction, String line) {
		transaction.addInterest(this.getFieldOfLine(line, idxTotal), false)
	}

	@Override
	void parseTax(Transaction transaction, String line) {
		if (transaction.isTaxes()) {
			transaction.addTax(this.getFieldOfLine(line, idxTotal), false)
		} else {
			transaction.addTax(this.getFieldOfLine(line, idxTax), true)
		}
	}

	@Override
	void parseIsin(Transaction transaction, String line) {
		transaction.isin = this.getFieldOfLine(line, idxIsin)
	}

	@Override
	void parseName(Transaction transaction, String line) {
		transaction.name = Utils.fixIsinName(this.getFieldOfLine(line, idxName))
	}

	@Override
	void parseRate(Transaction transaction, String line) {
		if (transaction.isTaxes()) {
			transaction.setRate(0)
		} else if (transaction.isDiv()) {
			transaction.setRate(this.getFieldOfLine(line, idxTotal))
		} else  {
			transaction.setRate(this.getFieldOfLine(line, idxRate))
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
