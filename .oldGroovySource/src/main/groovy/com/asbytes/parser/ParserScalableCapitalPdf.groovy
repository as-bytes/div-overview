package com.asbytes.parser

import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
/** also Baader Bank just as Finanzen.NET Zero **/
class ParserScalableCapitalPdf extends OrderParser {
	@Override
	Map<String, Closure> getOverwrites() {
		return [:]
	}

	@Override
	boolean isPreEmptiveRights(String content) {
		return false
	}

	@Override
	boolean isDividend(String content) {
		def patterns = [/(?m)^Dividendenabrechnung/, /(?m)^Fondsausschüttung/, /(?m)^Dividende KOPIE/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isBuy(String content) {
		def patterns = [/(?m)^Wertpapierabrechnung: Kauf/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isSell(String content) {
		def patterns = [/(?m)^Wertpapierabrechnung: Verkauf/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isKnockout(String content) {
//		def patterns = [/(?m)^TILGUNG/, /(?m)^Ausbuchung/]
//		return patterns.any { Utils.isMatch(content, it) }
		return false
	}

	boolean isTax(String content) {
		def patterns = [/(?m)^Steuerausgleichsrechnung/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	@Override
	boolean isIsinSwitch(String content) {
		return false
	}

	void parseId(Transaction transaction, String content) {
		transaction.id = Utils.find1stGroup(content, /(?m)^Vorgangs-Nr.: (.+)$/)
		transaction.id += "#" + (Utils.find1stGroup(content, /(?m)^Referenz-Nr.:  (.+)$/) ?: "")
		Exceptions.assertTrue(transaction.id != "#", "Could not parse id\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		def patternBuySell = /(?m)^Details zur Ausführung:\r\n.+(\d\d\.\d\d\.\d{4} \d\d:\d\d:\d\d)/
		def patternOrderTime = /(?m)^Auftragszeit:[\r\n]+(\d\d\.\d\d\.\d{4})/
		def patternDiv = /(?m)^Zahltag.+(\d\d\.\d\d\.\d{4})/
		def patternSteuerausgleich = /(?m)^(\d\d\.\d\d\.\d{4})/
		def date = [patternBuySell, patternOrderTime, patternDiv, patternSteuerausgleich].findResult { Utils.find1stGroup(content, it) }
		if (date == null) {
			throw new Exception("Date is null for:\n${transaction.toString()}\n$content")
		}
		transaction.date = date
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
		transaction.amount = Utils.find1stGroup(content, /(?m)^STK ([,.\d]+)/)
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String content) {
		def pattern1 = /(?m)^Valuta:.+EUR ([,.\d]+)$/
		def pattern2 = /(?m)([,.\d]+)EURValuta: (?:\d\d\.\d\d\.\d{4})/
		transaction.total = [pattern1, pattern2].findResult { Utils.find1stGroup(content, it) }
		Exceptions.assertNotNull(transaction.total, "Total was not found\n$transaction")
	}

	@Override
	void parseFee(Transaction transaction, String content) {
		def patterns = []
		patterns << /(?m)Mindermengenzuschlag Finanzen.net ([\d,. ]+)-?EUR$/
		if (transaction.isBuy()) {
			patterns.each { transaction.addFee(Utils.find1stGroup(content, it), true) }
		} else {
			patterns.each { transaction.addFee(Utils.find1stGroup(content, it), true) }
		}
	}

	@Override
	void parseTax(Transaction transaction, String content) {
		def patterns = []
//		patterns << /(?m)Kapitalertragsteuer auf Aktiengewinne .+ ([-\d,. ]+)EUR$/
//		patterns << /(?m)bezahlte Kapitalertragsteuer .+ ([-\d,. ]+)EUR/

		// Steuerausgleichsrechnung
		if (transaction.isTaxes()) {
			patterns << /(?m)^([\d,. ]+)EURErstattung$/
			patterns.each { transaction.addTax(Utils.find1stGroup(content, it), false) }
		} else {
			patterns << /(?m)Fra\. Finanztransaktionssteuer ([\d,. ]+)-?EUR$/
			patterns << /(?m)Solidaritätszuschlag ([\d,. ]+)-?EUR$/
			patterns << /(?m)Kapitalertragsteuer ([\d,. ]+)-?EUR$/
			patterns << /(?m)US-Quellensteuer ([\d,. ]+)-?EUR$/
			patterns.each { transaction.addTax(Utils.find1stGroup(content, it), true) }
		}
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
		def pattern1 = /(?m)ISIN: ([A-Z]{2}[0-9A-Z]{10})$/
		def pattern2 = /(?m)ISIN: ([A-Z]{2}[0-9A-Z]{10})/
		transaction.isin = [pattern1, pattern2].findResult { Utils.find1stGroup(content, it) }
	}

	@Override
	void parseName(Transaction transaction, String content) {
		def groupPatterns = []
		groupPatterns << /(?m)^STK [\d,.]+ (.+)$/
		groupPatterns << /(?m)p.STK(.+)$/
		transaction.name = groupPatterns.findResult { Utils.find1stGroup(content, it) }
		if (transaction.name) {
			transaction.name = Utils.fixIsinName(transaction.name)
		}
	}

	@Override
	void parseRate(Transaction transaction, String content) {
		def rate;
		if (transaction.isDiv()) {
			rate = transaction.getRateFromTotal(false);
		}

		if (rate == null) {
			def pattern = /(?m)^Details zur Ausführung:[\r\n]+.+STK\s+[,.\d]+\s+EUR ([.,\d]+)$/
			rate = Utils.find1stGroup(content, /${pattern}/)
		}

		if (rate == null) {
//			Auftragsdatum:
//			EUR
//			Ausführungsplatz: GETTEX - MM Munich
//			9,211
//			Auftragszeit:
			def pattern = /(?m)Auftragsdatum:[\r\n]+EUR[\r\n]+Ausführungsplatz.+[\r\n]+^([,.\d]+)$[\r\n]+Auftragszeit:/
			rate = Utils.find1stGroup(content, /${pattern}/)
		}

		transaction.setRate(rate)

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
