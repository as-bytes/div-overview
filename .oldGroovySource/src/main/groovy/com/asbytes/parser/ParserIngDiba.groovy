package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserIngDiba extends OrderParser {
	@Override
	Map<String, Closure> getOverwrites() {
		def overwrites = [:]
		/** bmp Holding AG
		 * 09/05/17 Umtausch 3:1 DE0003304200 (1020x 330420) ->  DE000A2E3772 (340x A2E377)
		 */
		def bmpClosure = {
			it.isin = 'DE000A2E3772'
			it.amount = it.amount/3
			it.rate = it.rate*3
		}
		overwrites.put('60321377#001', bmpClosure)
		overwrites.put('111252705#001', bmpClosure)

		/** MIL Turkey von FR zu LU
		 * Umtausch 1:1 FR0010326256 (LYX0AK) -> LU1900067601 (LYX02F)
		 **/
		overwrites.put('131795521#001', { it.isin = 'LU1900067601' })
		overwrites.put('131021232#001', { it.isin = 'LU1900067601' })
		overwrites.put('129228431#001', { it.isin = 'LU1900067601' })
		overwrites.put('128843845#001', { it.isin = 'LU1900067601' })

		/** PainThera. zu Cassava Science US14817C1071 **/
		//    A2DRLU US69562K5065 x420
		// => A2PGL8 US69562K5065 x464,28571  (464,28571x 938335) -> (3250x A2DRLU)
		def painClosure = {
			it.isin = 'US14817C1071'
			it.amount = it.amount/7
			it.rate = it.rate*7
		}

		// 26.09.2016 106834719.001 EK +500
		// 26.09.2016 106838743.001 VK -500
		overwrites.put('106834719.001', { it.isin = "US14817C1071" })
		overwrites.put('106838743.001', { it.isin = "US14817C1071" })

		// 29.09.2016 106922649.001 EK +600 => 85,71428
		// 30.09.2016 106964493.001 EK +650 => 92,85714
		// 10.11.2016 108293627.001 EK +2000 => 285,71428
		// 18.05.2017 -3250 Umtausch Ausgang US69562K1007
		// 18.05.2017 +464,28571 Umtausch Eingang US69562K5065
		overwrites.put('106922649.001', { it.isin = "US14817C1071"; it.rate = 85.71428 })
		overwrites.put('106964493.001', { it.isin = "US14817C1071"; it.rate = 92.85714 })
		overwrites.put('108293627.001', { it.isin = "US14817C1071"; it.rate = 285.71428 })

		// 18.05.2017 114789450.001 VK -0,28571
		overwrites.put('114789450.001', { it.isin = "US14817C1071"; it.rate = 0.2857 })

		// 13.07.2018 130706937.001 EK +120
		// 09.01.2018 123197192.001 VK -232
		// 09.01.2018 123204572.001 VK -232
		// 04.10.2018 133833521.001 EK +300
		overwrites.put('130706937.001', { it.isin = 'US14817C1071' })
		overwrites.put('123197192.001', { it.isin = 'US14817C1071' })
		overwrites.put('123204572.001', { it.isin = 'US14817C1071' })
		overwrites.put('133833521#001', { it.isin = 'US14817C1071' })

		// 29.03.2019 +420 Umtausch Eingang US14817C1071
		// 29.03.2019 -420 Umtausch Ausgang US69562K5065
		// 02.04.2019 Switch A2PGL8 zu A2DRLU 1:1
		// 03.05.2019 143848353.001 EK +200
		// 27.12.2019 154936629.001 VK -120
		// 27.12.2019 154853886.001 VK -500
		overwrites.put('143848353.001', { it.isin = 'US14817C1071' })
		overwrites.put('154936629.001', { it.isin = 'US14817C1071' })
		overwrites.put('154853886.001', { it.isin = 'US14817C1071' })

		/** 18/06/18 Pacific Potash -> Pacific Silk Road
		 * Umtausch 10:1 in andere ISIN CA6947811056 (A1JMQD) -> CA6947812047 (A140BY)
		 * Umtausch 1:1 in andere ISIN CA6947812047 (A140BY) -> CA69481U1066 (A2JNSZ)
		 */
		def closurePacific = {
			it.isin = 'CA69481U1066'
			it.amount = it.amount/10
			it.rate = it.rate*10
		}
		overwrites.put('72802469#001', closurePacific)
		overwrites.put('72802469#001', closurePacific)
		overwrites.put('74727246#001', closurePacific)
		overwrites.put('222866061#001', { it.amount = 3600/10; it.rate = 0.014*10 })

		/** 14/05/20 Aurora Cannabis - Bestandsveränderung */
		def closureAurora = {
			it.isin = 'CA05156X8843'
			it.amount = 29.16667
			it.rate = Math.abs((it.total + Math.abs(it.fee)) / it.amount);
		}
		overwrites.put('144879722#001', closureAurora)
		overwrites.put('169873557#001', {  it.isin = 'CA05156X8843' })

		/** Isin switch L&G US Energy Infra from DE000A1XE2Q3
		 */
		def closureLG = {
			it.isin = 'IE00BHZKHS06'
		}
		overwrites.put('166399458#001', closureLG)

		/** Protores - Wertlos from AU000000PRW9 */
		// Direkt_Depot_8004195622_Bestandsveraenderung_AU000000PRW9_Order_0013379263_20180821
		def closureProtoRes1 = { Transaction trans ->
			trans.isin = 'AU0000PRWDA1'
			trans.amount = 3750;
			trans.rate = 150/3750;
		}
		overwrites.put('69343361#001', closureProtoRes1)
		def closureProtoRes2 = {
			it.isin = 'AU0000PRWDA1'
		}
		overwrites.put('KNO-AU000000PRW9#2018-08-21x0', closureProtoRes2)

		return overwrites
	}

	@Override
	boolean isBuy(String content) {
		def type = Utils.find1stGroup(content, /(?m)^Wertpapierabrechnung (\w+)/)
		return type?.equalsIgnoreCase("KAUF")
	}

	@Override
	boolean isSell(String content) {
		def type = Utils.find1stGroup(content, /(?m)^Wertpapierabrechnung (\w+)/)
		return type?.equalsIgnoreCase("VERKAUF")
	}

	@Override
	boolean isKnockout(String content) {
		def pattern = ['Rückzahlung[\r\n]+ISIN', 'Einlösung[\r\n]+ISIN']
		def tilg = pattern.any { Utils.isMatch(content, it) }
		if (tilg) {
			return true
		} else {
			// rate = 0
			return content.startsWith('Wertpapier Ausgang')
		}
	}

	@Override
	boolean isTax(String content) {
		def patterns = [/(?m)^Vorabpauschale$/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	@Override
	boolean isPreEmptiveRights(String content) {
		return content.contains('Wertpapier Eingang') && !content.contains('Umtausch')
	}

	@Override
	boolean isIsinSwitch(String content) {
		def patterns = []
		//patterns << 'zum steuerrelevanten Umtausch'
		// patterns << 'Wertpapier Ausgang'
		patterns << 'Umtausch Eingang'
		patterns << 'Umtausch Ausgang'
		return patterns.any { content.contains(it) }
	}

	// todo
	//file:///O:/OneDrive/Documents/Finance/ING%20DiBa/Depot/2020/Direkt_Depot_8004195622_Bestandsveraenderung_CA05156X8843_Order_0015011704_20200514.pdf

	@Override
	def handleIsinSwitch(String content) {
		throw new IllegalAccessError('isin switch not implemented')
//		def start = content.indexOf('Ausbuchung')
//		def ende = content.indexOf('Einbuchung')
//		assert start > 0 && ende > 0 && start < ende
//
//		def haystack = content.substring(start)
//		def isins = Utils.findGroups(haystack, )
//
//		def isinOut = new Transaction(this, pdf)
//
//		def isinIn = new Transaction(this, pdf)
//
//		// works only for TradeRepublic for now
//		this.parseId(transaction, docContent)
//		def lines = docContent.split('\n')
//		def outward = lines.findIndexOf {it.startsWith('1 Ausbuchung')}
//		def inward = lines.findIndexOf {it.startsWith('2 Einbuchung')}
//
//		def outAmount = lines[outward..inward].find {it.trim().endsWith(' Stk.')}
//		def outwardSplit = new Transaction(this, pdf)
//		outwardSplit.with {
//			it.id = transaction.id + "-out"
//			it.rate = 0d
//			it.total = 0d
//			it.currency = Transaction.Currency.EUR
//			it.date = transaction.date
//			it.isin = transaction.isin
//			it.amount = outAmount.split(' ').first()
//			it.type = Transaction.TransTypes.SPLITSELL
//		};
//
//		def inAmount = lines[inward..inward+5].find {it.trim().endsWith(' Stk.')}
//		def inwardSplit = new Transaction(this, pdf)
//		inwardSplit.with {
//			it.id = transaction.id + "-in"
//			it.rate = 0d
//			it.total = 0d
//			it.currency = Transaction.Currency.EUR
//			it.date = transaction.date
//			it.isin = transaction.isin
//			it.amount = inAmount.split(' ').first()
//			it.type = Transaction.TransTypes.SPLITBUY
//		};
//
//		return [outwardSplit, inwardSplit]
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		def patterns = [/(?m)^Valuta ([.0-9]+)/, /(?m)^Zahltag ([.0-9]+)/, /(?m)^Datum: ([.0-9]+)/]
		transaction.date = patterns.findResult { Utils.find1stGroup(content, it) }
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String content) {
		// Endbetrag zu Ihren Lasten EUR 2.744,53
		// Gesamtbetrag zu Ihren Gunsten EUR 4,93
		// Gesamtbetrag 0,00
		// Endbetrag EUR 252,00
		def patterns = []
		patterns << /(?m)^\w+ zu Ihren \w+ EUR ([-\d,. ]+)$/
		patterns << /(?m)^Gesamtbetrag (?:EUR)*([-\d,. ]+)$/
		patterns << /(?m)^Endbetrag EUR ([-\d,. ]+)$/
		def total = patterns.findResult { Utils.find1stGroup(content, it) }
		transaction.setTotal(total)

/*
		if (!isHardKnockout(text)) {
			assert total != null
			this.setTotal(total, (isBuy()))
		}
*/
		Exceptions.assertNotNull(transaction.total, "Total was not found\n$transaction")

	}

	@Override
	void parseFee(Transaction transaction, String content) {
		def patterns = []
		patterns << /(?m)^Provision EUR ([\d,.]+)$/
		patterns << /(?m)^Handelsentgelt EUR ([\d,.]+)$/
		patterns << /(?m)^Börsenentgelt EUR ([\d,.]+)$/
		patterns << /(?m)^Courtage EUR ([\d,.]+)$/
		patterns << /(?m)^Handelsplatzgebühr EUR ([\d,.]+)$/
		patterns << /(?m)^Variables Transaktionsentgelt EUR ([\d,.]+)$/
		patterns.each {
			transaction.addFee(Utils.find1stGroup(content, it), true)
		}

		def discounts = []
		discounts << /(?m)^Rabatt EUR ([\d,. -]+)$/
		discounts.each {
			transaction.addFee(Utils.find1stGroup(content, it), true)
		}
	}


	@Override
	void parseTax(Transaction transaction, String content) {
		def patterns = []
		patterns << /(?m)^Kapitalertragsteuer \d+,\d+ % EUR ([-\d,. ]+)$/
		patterns << /(?m)^Solidaritätszuschlag \d+,\d+ % EUR ([-\d,. ]+)$/
		patterns << /(?m)^Franz\. Transaktionssteuer \d+,\d+% EUR ([-\d,. ]+)$/
		patterns.each { transaction.addTax(Utils.find1stGroup(content, it), true) }
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
		def (isin, wkn) = Utils.findGroups(content, /(?m)^ISIN \(WKN\)(?:\:)* ([A-Z]{2}[0-9A-Z]{10}) \(([0-9A-Z]{6})\)/)
		transaction.isin = isin
		transaction.wkn = wkn
		Exceptions.assertNotNull(transaction.isin, "ISIN was not found\n$transaction")
		Exceptions.assertNotNull(transaction.wkn, "WKN was not found\n$transaction")
	}

	@Override
	void parseName(Transaction transaction, String content) {
		transaction.name = Utils.find1stGroup(content, /(?m)^Wertpapierbezeichnung (.+)$/)
		if (!transaction.name && (transaction.isPreEmptive() || transaction.isKnockout())) {
			transaction.name = Utils.find1stGroup(content, /(?m)Stück \d{2}.\d{2}.\d{4} \d+(.+)$/)
		}

		if (transaction.name) {
			def lines = content.split(/\n/)
			def nameIndex = lines.findIndexOf { it.contains(transaction.name) }
			transaction.name += " " + lines[nameIndex + 1].trim()

			def ignoreStr = Utils.getIgnoreIsinName()
			ignoreStr.each {
				def index = transaction.name.indexOf(it)
				if (index > 0) {
					transaction.name = transaction.name.substring(0, index - 1)
				}
			}
		}
		Exceptions.assertNotNull(transaction.name, "Name was not found\n$transaction")

		transaction.name = Utils.fixIsinName(transaction.name)
	}

	@Override
	void parseRate(Transaction transaction, String content) {
		if (transaction.isDiv()) {
			transaction.rate = transaction.getRateFromTotal(true)
		} else if (!transaction.isEUR()) {
			transaction.rate = transaction.getRateFromTotal(false)
		} else if (transaction.isPreEmptive()) {
			transaction.rate = 0
		} else {
			def patterns = []
			patterns << /(?m)^Kurs \(Festpreisgeschäft\) EUR ([,.0-9]+)/
			patterns << /(?m)^Kurs(?: EUR)? ([,.0-9]+)/
			patterns << /(?m)^Einlösung zum Kurs von ([,.0-9]+) EUR/
			patterns << /(?m)^Kurswert EUR ([,.0-9]+)/
			transaction.rate = patterns.findResult { Utils.find1stGroup(content, it) }
		}

		if (transaction.isKnockout() && transaction.rate == null) {
			transaction.rate = 0.0d
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}

	@Override
	boolean isDividend(String content) {
		def patterns = [
				/(?m)^(?:Ertragsgutschrift)$/,
				/(?m)^(?:Ertragsthesaurierung)$/,
				/(?m)^(?:Dividendengutschrift)$/
		]
		return patterns.any { Utils.isMatch(content, it) }
	}

	void parseId(Transaction transaction, String content) {
		if (transaction.isBuy() || transaction.isSell()) {
			transaction.id = Utils.find1stGroup(content, /(?m)^Ordernummer (.+)$/)
		} else {
			def total = (transaction.total * 100).toInteger()
			def suffix = "${transaction.isin}#${transaction.date}x$total"

			if (transaction.isDiv()) {
				transaction.id = "DIV-$suffix"
			} else if (transaction.isPreEmptive()) {
				transaction.id = "PRE-$suffix"
			} else if (transaction.isKnockout()) {
				transaction.id = "KNO-$suffix"
			} else if (transaction.isTaxes()) {
				transaction.id = "TAX-$suffix"
			} else {
				transaction.id = Utils.find1stGroup(content, /(?m)^Ordernummer (.+)$/)
			}
		}

		transaction.id = transaction.id?.replace(".", "#")
		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
		// Nominale Stück 350,00
		// Nominale 30,00 Stück
		// 3.750,00 Stück 20.08.2018 0013379263
		def patterns = [
				/(?m)^Nominale Stück ([,.0-9]+)/,
				/(?m)^Nominale ([,.0-9]+) Stück/,
				/(?m)^([,.0-9]+) Stück/
		]
		transaction.amount = patterns.findResult { Utils.find1stGroup(content, it) }
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

}
