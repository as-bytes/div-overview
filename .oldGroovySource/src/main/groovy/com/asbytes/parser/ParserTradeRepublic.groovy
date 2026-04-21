package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.Exceptions
import com.asbytes.helpers.Utils
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserTradeRepublic extends OrderParser {
	@Override
	Map<String, Closure> getOverwrites() {
		def overwrites = [:]
		/** Rundungsfehler (zuwenig Nachkommastellen in Abrechnung) **/
		// pb157108874141421640037996272302
		overwrites.put('65674438#c49e-6196', { it.rate = 0.1264d })
		// pb15792019680494379530218709960
		overwrites.put('78cb-2113#916e-05ed', { it.rate = 0.8801d })
		// pb15795106314374688192918690090
		overwrites.put('0ce5-5f42#0bef-6398', { it.rate = 0.78792d })
		// pb15694473404105382638099575680
		overwrites.put('21334811#3ef6-0295', { it.total = 3.14d })
		// pb15833922821521022627563919270
		overwrites.put('9f0a-5fcc#e4a4-b41d', { it.rate = 0.6949d })
		// pb15837454667011375812112381427
		overwrites.put('7472-6081#3884-0fc1', { it.rate = 1.4933d })
		// pb15850797680872709725588518524
		overwrites.put('68fe-7041#88b3-476c', { it.rate = 0.2958d })
		// pb15858167071243446844305470349
		overwrites.put('6ea6-6f14#dcf4-8614', { it.rate = 0.9867d })
		// pb15869006162923998783149108774
		overwrites.put('24231816#aae1-a8af', { it.rate = 0.059711d }) // 5384615385

		// Einstellung der Zertifizierung DE000A0V9XY2 zu JE00B24DK975 am 02.09.2022

		/* Core One Labs
		2021-08-19 	CA21872J3073	VK	1,41
		2021-08-31	CA21872J3073	EK	50,90
		2021-12-23	CA21872J3073	EK	22,45
		2022-01-06	CA21872J3073	EK	96,82
		2022-01-24	CA21872J3073	EK	365,70

		2021-03-12	CA21872J2083	EK	15,70
		2021-04-01	CA21872J2083	EK	15,32
		2021-04-06	CA21872J2083	EK	44,68
		2021-06-03	CA21872J2083	EK	109,75
		*/
		overwrites.put('1473-4d71#b33a-71ea', { it.isin = 'CA21872J3073' })
		overwrites.put('530d-c6cd#9eea-5ecd', { it.isin = 'CA21872J3073' })
		overwrites.put('ec0c-1248#c644-9128', { it.isin = 'CA21872J3073' })
		overwrites.put('d6d8-8a33#c106-8bed', { it.isin = 'CA21872J3073' })

		// amount was 0,001 sale was to clear position, dunno exactly why this low amount existed but the value itself is 0 € and 1 € fee anyway
		overwrites.put('9271-04be#ea1a-91d0', { it.amount = 0; it.openAmount = 0; it.type = Transaction.TransTypes.FEE })

		return overwrites
	}

	@Override
	boolean isSplit(String content) {
		return false
	}

	@Override
	boolean isIsinSwitch(String content) {
		def patterns = [/(?m)^REVERSE SPLIT$/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	@Override
	def handleIsinSwitch(String content) {
		throw new IllegalStateException("not yet implemented")
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
	boolean isPreEmptiveRights(String content) {
		return false
	}

	@Override
	boolean isDividend(String content) {
		def patterns = [/(?m)^(?:Dividende|Ausschüttung) mit dem Ex-Tag/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isCrypto(Transaction transaction, String content) {
		if (transaction.getCrypto() == null) {
			transaction.setCrypto(content.contains('ABRECHNUNG CRYPTOGESCHÄFT'))
		}
		return transaction.getCrypto()
	}

	@Override
	public boolean isUnsupported(Transaction transaction, String content) {
		return false
	}

	@Override
	boolean isInterest(String content) {
		def patterns = [/(?m)^ABRECHNUNG ZINSEN$/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	public boolean isSpinOff(String content) {
		def pattern1 = /(?m)^(SPIN-OFF)$/
		def type = [pattern1].findResult { Utils.find1stGroup(content, it) }
		return type?.equalsIgnoreCase("SPIN-OFF")
	}

	boolean isBuy(String content) {
		def pattern1 = /(?m)^(?:Limit|Stop-Limit|Market|Stop-Market)-Order.(.+).am/
		def pattern2 = /(?m)^EX-ANTE KOSTENINFORMATION ZUM WERTPAPIER(.+)$/
		def type = [pattern1, pattern2].findResult { Utils.find1stGroup(content, it) }
		return type?.equalsIgnoreCase("KAUF")
	}

	boolean isSell(String content) {
		def pattern1 = /(?m)^(?:Limit|Stop-Limit|Market|Stop-Market)-Order.(.+).am/
		def pattern2 = /(?m)^EX-ANTE KOSTENINFORMATION ZUM WERTPAPIER(.+)$/
		def pattern3 = /(?m)^(Verkauf) am \d\d/
		def type = [pattern1, pattern2, pattern3].findResult { Utils.find1stGroup(content, it) }
		return type?.equalsIgnoreCase("VERKAUF")
	}

	boolean isKnockout(String content) {
		def patterns = [/(?m)^TILGUNG/, /(?m)^Ausbuchung/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	boolean isTax(String content) {
		def patterns = [/(?m)^Besteuerung der Vorabpauschale (.+)$/, /(?m)^STEUERABRECHNUNG$/]
		return patterns.any { Utils.isMatch(content, it) }
	}

	void parseId(Transaction transaction, String content) {
		if (transaction.isBuy() || transaction.isSell()) {
			transaction.id = Utils.find1stGroup(content, /(?m)^ORDER (.+)$/)
			transaction.id += "#" + (Utils.find1stGroup(content, /(?m)^AUSFÜHRUNG (.+)$/) ?: "")
		} else { //if (transaction.isKnockout() || transaction.isDiv() || transaction.isTax() || transaction.isCapital()) {
			transaction.id = getIdFromAbre(content)
		}

//		if (transaction.getTemporaryStatement()) {
//			transaction.id = ""
//		}

		Exceptions.assertNotNull(transaction.id, "Could not parse id\n$transaction")
	}

	@Override
	void parseDate(Transaction transaction, String content) {
		def pattern1 = /(?m)^(?:Market|Stop|Limit|Div|Auss).+(?:Kauf|Verkauf|dem) (?:am|Ex-Tag) (\d\d\.\d\d\.\d{4})[,.]/
		def pattern2 = [/(?m)^DATUM (.+)$/, /(?m)^DATUM (.+)[\r\n]+$/, /(?m)mit dem Ex-Tag.(.+)\.$/]
		transaction.date = ([pattern1] + pattern2).findResult { Utils.find1stGroup(content, it) }
		Exceptions.assertNotNull(transaction.date, "Could not parse date\n$transaction")
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
		if (transaction.isKnockout() || transaction.isTaxes() || transaction.isCapital() || isCrypto(transaction, content)) {
			if (isCrypto(transaction, content)) {
				transaction.amount = Utils.find1stGroup(content, /(?m) ([,.\d]+) Stk/)
			} else {
				transaction.amount = Utils.find1stGroup(content, /(?m)^([,.\d]+) Stk/)
			}
		} else {
			def currency = transaction.currency
			def arPattern = "(?m)^([,.\\d]+) Stk\\. [\\d,.]+ $currency"
			transaction.amount = Utils.find1stGroup(content, /${arPattern}/)

			if (!transaction.amount) {
				transaction.amount = Utils.find1stGroup(content, /(?m)^(?:Kauf|Verkauf)?[ ]*([.\d]+) Stk\. (?:[\d,.]+ EUR)/)
			}

			if (!transaction.amount) {
				transaction.amount = Utils.find1stGroup(content, /(?m)^([,.\d]+) Stk\.$/)
			}
		}
		Exceptions.assertNotNull(transaction.amount, "Could not parse amount\n$transaction")
	}

	@Override
	void parseTotal(Transaction transaction, String content) {
		def lines = content.split(/\n/)
		def buchIdxKey = transaction.isKnockout() ? "GELDBUCHUNG" : "BUCHUNG"
		def buchIdx = lines.findIndexOf { it.trim() == buchIdxKey }
		def buchung = lines[buchIdx..buchIdx + 2].join('\n')
		def total = Utils.find1stGroup(buchung, /(?m)^DE\d+ \d{2}.\d{2}.\d{4} ([-\d,. ]+) EUR$/)
		if (!total) {
			total = Utils.find1stGroup(buchung, /(?m)^DE\d+ \d{4}-\d{2}-\d{2} ([-\d,. ]+) EUR$/)
		}
		if (!total) {
			// Ex-Ante Kostinfo: Kauf  20 Stk. 67,00 EUR HSBC
			total = Utils.find1stGroup(content, /(?m)^(?:Kauf|Verkauf)?[ ]*[.\d]+ Stk\. ([\d,.]+) EUR/)
		}

		transaction.setTotal(total, false)

		if (transaction.total == 0.0d && transaction.isTaxes()) {
			transaction.setTotal(Utils.find1stGroup(content, /(?m)^GESAMT ([-\d,. ]+)/), false)
		}

		Exceptions.assertNotNull(transaction.total, "Total was not found\n$transaction")
	}

	@Override
	void parseFee(Transaction transaction, String content) {
		def pattern = /(?m)^(?:Fremdkostenzuschlag|Fremdkostenpauschale) ([-\d,. ]+) EUR$/
		transaction.addFee(Utils.find1stGroup(content, pattern))
	}

	@Override
	void parseTax(Transaction transaction, String content) {
		def patterns = []
		patterns << /(?m)Quellensteuer (?:Optimierung)?([-\d,. ]+) EUR$/
		patterns << /(?m)Kapitalertragssteuer (?:Optimierung)?([-\d,. ]+) EUR$/
		patterns << /(?m)Kapitalertragsteuer (?:Optimierung)?([-\d,. ]+) EUR$/
		patterns << /(?m)Solidaritätszuschlag (?:Optimierung)?([-\d,. ]+) EUR$/
		patterns << /(?m)Frz. Finanztransaktionssteuer (?:Optimierung)?([-\d,. ]+) EUR$/
		patterns.each { transaction.addTax(Utils.find1stGroup(content, it)) }
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
		if (isCrypto(transaction, content)) {
			def pattern = /(?m)^([a-zA-Z0-9]+) \(([A-Z0-9]+)\)/
			def (name, token) = Utils.findGroups(content, pattern)
			transaction.isin = "TR-" + token
			transaction.name = name
		} else if (transaction.isInterest()) {
			transaction.isin = "TR-Interest"
			transaction.name = "Interest"
		} else {
			def patterns = [
					/(?m)^([A-Z]{2}[0-9A-Z]{10})$/,
					/(?m)^([A-Z]{2}[0-9A-Z]{10})[\r\n]+$/,
					/(?m)^ISIN: ([A-Z]{2}[0-9A-Z]{10})$/
			]
			def isin = patterns.findResult { Utils.find1stGroup(content, it) }
			transaction.isin = isin
			if (!transaction.isTaxes()) {
				Exceptions.assertNotNull(transaction.isin, "ISIN was not found\n$transaction")
			} else if (isin == null) {
				transaction.isin = "STEUEROPTIM."
			}
		}
	}

	@Override
	void parseName(Transaction transaction, String content) {
		if (isCrypto(transaction, content) || transaction.isInterest()) {
			return
		}

		def ignoreStr = Utils.getIgnoreIsinName()

		def prevLinePatterns = []
		prevLinePatterns << /(?m)POSITION ANZAHL.*/

		def lines = content.split(/\n/)
		def index = prevLinePatterns.findResult { pattern ->
			lines.findIndexOf { line ->
				def matcher = (line =~ pattern)
				return matcher.find()
			}
		}

		def noPrevLineMatched = index == -1
		if (noPrevLineMatched) {
			def groupPatterns = []
			groupPatterns << /(?m)^\d+ Tilgung (.+)$/
			groupPatterns << /(?m)^\d+ Einbuchung (.+)$/
			groupPatterns << /(?m)^(.+) in Wertpapierrechnung\.$/
			index = groupPatterns.findResult { pattern ->
				lines.findIndexOf { line ->
					def matcher = (line =~ pattern)
					return matcher.find()
				}
			}
			index = (index == -1) ? index : index - 1
			transaction.name = groupPatterns.findResult { Utils.find1stGroup(content, it) }
		} else {
			transaction.name = lines[index + 1].trim()
		}

		if (index > -1) {
			if (!(lines[index + 3].trim() =~ '[A-Z]{2}\\d{10}').find()) {
				if (!ignoreStr.any { lines[index + 2].startsWith(it) }) {
					transaction.name += " ${lines[index + 2].trim()}"
				}
			}
		}

		// name from Ante-Ex Kosteninfo
		if (!transaction.name) {
			def start = lines.findIndexOf { it.startsWith('EX-ANTE') }
			if (start > -1) {
				start += 1 // Skip "WERTPAPIER ORDER / ANZAHL WERT AUSFÜHRUNGSPLATZ"
				def end = lines.findIndexOf(start, { it.startsWith('ISIN') })
				if (end > -1) {
					transaction.setTemporaryStatement(true)
					transaction.name = "[TMP] " + lines[start+1].trim()
					if (start-end > 1) {
						transaction.name += " " + lines[start+2].trim()
					}
				}
			}
		}

		if (transaction.name) {
			transaction.name = Utils.fixIsinName(transaction.name)
		}

		// name is egal, gibt ja vorherige Transaktion
		if (!transaction.isKnockout() && !transaction.isTaxes() && !transaction.isCapital()) {
			Exceptions.assertNotNull(transaction.name, "Name was not found\n$transaction")
		}
	}

	@Override
	void parseRate(Transaction transaction, String content) {
		if (transaction.isBuy() || transaction.isSell() || transaction.isDiv()) {
			String rate
			if (isCrypto(transaction, content)) {
				def arPattern = "(?m)[,.\\d]+ Stk\\. ([\\d,.]+) ${transaction.currency}"
				rate = Utils.find1stGroup(content, /${arPattern}/)
			} else {
				def arPattern = "(?m)^[,.\\d]+ Stk\\. ([\\d,.]+) ${transaction.currency}"
				rate = Utils.find1stGroup(content, /${arPattern}/)
			}

			if (!rate) {
				rate = Utils.find1stGroup(content, /(?m)^(?:Kauf|Verkauf) (?:[.\d]+) Stk\. ([\d,.]+ EUR)/)
			}
			transaction.setRate(rate)
		}

		if (!transaction.isEUR() || transaction.isKnockout() || transaction.isTemporaryStatement()  || transaction.isInterest()) {
			def rate = transaction.getRateFromTotal(false) ?: 0
			transaction.setRate(rate)
		} else if (transaction.isTaxes() || transaction.isCapital()) {
			transaction.setRate(0)
		}

		Exceptions.assertNotNull(transaction.rate, "Rate was not found\n$transaction")
	}

	String getIdFromAbre(String content) {
		// e.g. ABRE / 15.10.2019 / 63388640 / 24a5-f9a9
		def footerstr = Utils.find1stGroup(content, /(?m)^((?:ABRE|DUAN) .+)$/)
		def footerarr = footerstr?.split("/")
		if (footerarr) {
			def left = footerarr[-2]
			def right = footerarr[-1]
			return left.trim() + "#" + right.trim()
		} else {
			// AVISO / 80df-42c4
			footerstr = Utils.find1stGroup(content, /(?m)^(AVISO .+)$/)
			footerarr = footerstr?.split("/")
			def left = footerarr?.first()
			def right = footerarr?.last()
			if (!left || !right) {
				return "Tax"
			} else {
				return left.trim() + "#" + right.trim()
			}
		}
	}
}
