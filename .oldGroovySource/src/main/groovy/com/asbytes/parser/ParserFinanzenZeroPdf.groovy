package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
/** also Baader Bank just as Scalable Capital **/
class ParserFinanzenZeroPdf extends OrderParser {
	@Override
	/**
	 * [id : closure]
	 * id = VorgangsNr#ReferenzNr
	 */
	Map<String, Closure> getOverwrites() {
		def overwrites = [:]
		// Reverse-Split: 15x US37733W1053 nach 12x US37733W2044
		overwrites.put('60623643#161785921', {
			it.isin = 'US37733W2044';
			it.rate = Math.abs(it.total / 12);
			it.amount = 12;
		})
		overwrites.put('6939553#196443981', { it.isin = 'US37733W2044' })
		overwrites.put('7907383#234041976', { it.isin = 'US37733W2044' })
		overwrites.put('9559870#274232002', { it.isin = 'US37733W2044' })

		// Obligatorischer Umtausch: AU000000WPL2 nach AU0000224040
		overwrites.put('8603757#251030797', { it.isin = 'AU0000224040' })

		// Obligatorischer Umtausch: GB00BH0P3Z91 nach AU000000BHP4
		overwrites.put('61755978#165727626', { it.isin = 'AU000000BHP4' })
		overwrites.put('60975953#164613870', { it.isin = 'AU000000BHP4' })
		overwrites.put('72192795#188908575', { it.isin = 'AU000000BHP4' })

		return overwrites
	}

	@Override
	public boolean isUnsupported(Transaction transaction, String content) {
		def patterns = [
				/(?m)^Abrechnung über den Kauf von Kryptowerten/,
				/(?m)^Abrechnung über den Verkauf von Kryptowerten/,
				// todo support? Gladstone clusterfuck, easiert to overwrite
				/(?m)^Die Dividende wurde reklassifiziert/,
		]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isTax(String content) {
		def patterns = [
				/(?m)^Steuerausgleichsrechnung/,
				/(?m)^Spitzenregulierung/,
				/(?m)^Return of Capital/,
				/(?m)^Steuerkorrektur/,
		]
		return patterns.any { Utils.isMatch(content, it) }
	}

	@Override
	void parseTax(Transaction transaction, String content) {
		def patterns = []
//		patterns << /(?m)Kapitalertragsteuer auf Aktiengewinne .+ ([-\d,. ]+)EUR$/
//		patterns << /(?m)bezahlte Kapitalertragsteuer .+ ([-\d,. ]+)EUR/

		if (transaction.isTaxes()) {
			[
					/(?m)^([\d,. ]+)EURErstattung$/,
					// Valuta: 22.01.2024 EUR 10,36Zu Gunsten Konto 1165616002
					/(?m)^Valuta: \d\d\.\d\d\.\d\d\d\d EUR ([\d,.]+)Zu Gunsten.+$/,
					/(?m)^([\d,.]+)EURValuta: \d\d\.\d\d\.\d\d\d\dZu Gunsten.+$/,
			].each { transaction.addTax(Utils.find1stGroup(content, it), false) }

			[
					/(?m)^([\d,. ]+)-EURNachbelastung$/
			].each { transaction.addTax(Utils.find1stGroup(content, it), true) }

			Exceptions.assertTrue(transaction.tax != 0, "Tax was not found\n$content")
		} else {
			patterns << /(?m)Fra\. Finanztransaktionssteuer ([\d,. ]+)-?EUR$/
			patterns << /(?m)Solidaritätszuschlag ([\d,. ]+)-?EUR$/
			patterns << /(?m)Kapitalertragsteuer ([\d,. ]+)-?EUR$/
			patterns << /(?m)US-Quellensteuer ([\d,. ]+)-?EUR$/
			patterns.each { transaction.addTax(Utils.find1stGroup(content, it), true) }
		}
	}

	@Override
	boolean isPreEmptiveRights(String content) {
		return false
	}

	@Override
	public boolean isSpinOff(String content) {
		def pattern1 = /(?m)^(Spin Off)$/
		def type = [pattern1].findResult { Utils.find1stGroup(content, it) }
		return type?.equalsIgnoreCase("Spin Off")
	}

	@Override
	boolean isDividend(String content) {
		// todo scheiß Bader bank mit ihrem verfickten Storno, über 10 PDF's mit 2 Seiten -.- erstmal ignorieren, dumme Husos
		def patterns = [/(?m)^Dividendenabrechnung/, /(?m)^Fondsausschüttung/, /(?m)^Dividende KOPIE$/, /(?m)^Dividende$/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isBuy(String content) {
		def patterns = [
				/(?m)^Wertpapierabrechnung: Kauf/,
//				/(?m)^Abrechnung über den Kauf von Kryptowerten/
		]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isSell(String content) {
		def patterns = [
				/(?m)^Wertpapierabrechnung: Verkauf/,
//				/(?m)^Abrechnung über den Verkauf von Kryptowerten/
		]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isKnockout(String content) {
//		def patterns = [/(?m)^TILGUNG/, /(?m)^Ausbuchung/]
//		return patterns.any { Utils.isMatch(content, it) }
		return false
	}

	@Override
	boolean isIsinSwitch(String content) {
		return false
	}

	@Override
	void parseId(Transaction transaction, String content) {
		def patterns = [
			Utils.find1stGroup(content, /(?m)^Vorgangs-Nr.: (.+)$/),
			Utils.find1stGroup(content, /(?m)^Referenz-Nr.: (.+)$/) ?: ""
		]

		transaction.id = patterns.join('#')
		Exceptions.assertTrue(transaction.id != "#", "Could not parse id\n$transaction")
		Exceptions.assertTrue(transaction.id != "", "Could not parse id\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		def patternBuySell = /(?m)^Details zur Ausführung:\r\n.+(\d\d\.\d\d\.\d{4} \d\d:\d\d:\d\d)/
		def patternDiv = /(?m)^Zahltag.+(\d\d\.\d\d\.\d{4})/
		def patternSteuerausgleich = /(?m)^(\d\d\.\d\d\.\d{4})/
		def date = [patternBuySell, patternDiv, patternSteuerausgleich].findResult { Utils.find1stGroup(content, it) }
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

		if (transaction.isTaxes()) {
			transaction.setTotal(transaction.tax, false);
		}

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
	void parseIsin(Transaction transaction, String content) {
		def pattern1 = /(?m)ISIN: ([A-Z]{2}[0-9A-Z]{10})$/
		def pattern2 = /(?m)ISIN: ([A-Z]{2}[0-9A-Z]{10})/
		transaction.isin = [pattern1, pattern2].findResult { Utils.find1stGroup(content, it) }

		if (!transaction.isin && transaction.isTaxes()) {
			transaction.isin = 'DE-JUSTTAXES'
		}
	}

	@Override
	void parseName(Transaction transaction, String content) {
		def groupPatterns = []
		groupPatterns << /(?m)^STK [\d,.]+ (.+)$/
		transaction.name = groupPatterns.findResult { Utils.find1stGroup(content, it) }

		if (transaction.name) {
			transaction.name = Utils.fixIsinName(transaction.name)
		}
	}

	@Override
	void parseRate(Transaction transaction, String content) {
		if (transaction.isDiv()) {
			transaction.setRate(transaction.getRateFromTotal(false))
		} else {
			def pattern = /(?m)^Details zur Ausführung:[\r\n]+.+STK\s+[,.\d]+\s+EUR ([.,\d]+)$/
			def rate = Utils.find1stGroup(content, /${pattern}/)
			if (!rate) {
				def pattern2 = /(?m)^Details zur Ausführung:[\r\n]+.+STK\s+[,.\d]+\s+EUR ([.,\d]+)/
				rate = Utils.find1stGroup(content, /${pattern2}/)
			}
			if (!rate) {
				def pattern3 = /(?m)^Ausführungsplatz:.+[\r\n]+([.,\d]+)[\r\n]+Auftragszeit:/
				rate = Utils.find1stGroup(content, /${pattern3}/)
			}

			transaction.setRate(rate)

		}
		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}
}
