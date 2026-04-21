package com.asbytes

import com.asbytes.helpers.Utils
import com.asbytes.parser.OrderParser
import com.asbytes.parser.OrderParserType

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SharesSummary {
	def List<Share> shares = []

	public SharesSummary(List<Share> shares) {
		this.shares = shares
	}

	public String calcInvested(boolean colored = true) {
		return calcInvested(this.shares, colored)
	}

	public String calcOpenInvested(boolean colored = true) {
		return calcOpenInvested(this.shares, colored)
	}

	public String calcInvested(List<Share> shares, boolean colored = true) {
		return Utils.toCurrency(shares.sum { it.openTotal }, colored)
	}

	public String calcOpenInvested(List<Share> shares, boolean colored = true) {
		return Utils.toCurrency(shares.sum { it.openTotal }, colored)
	}

	public String calcWinLoss(boolean colored = true) {
		return calcWinLoss(this.shares, colored)
	}

	public String calcWinLoss(List<Share> shares, boolean colored = true) {
		return Utils.toCurrency(shares.sum { it.winloss }, colored)
	}

	public String calcTaxesAndFees(boolean colored = true) {
		return calcTaxesAndFees(this.shares, colored)
	}

	public String calcTaxesAndFees(List<Share> shares, boolean colored = true) {
		return Utils.toCurrency(shares.sum { it.tax + it.fee }, colored)
	}

	public String calcTaxes(boolean colored = true) {
		return calcTaxes(this.shares, colored)
	}

	public String calcTaxes(List<Share> shares, boolean colored = true) {
		return Utils.toCurrency(shares.sum { it.tax }, colored)
	}

	public void logToFile(StringBuilder builder) {
		def date = new Date().format('yyyy-MM-dd')
		def fileLast = new File("./summaries/summary.txt")
		def fileDate = new File("./summaries/${new Date().format('yyyy')}/summary-${date}.txt")
		fileDate.parentFile.mkdirs()
		fileDate.write(Utils.clearColors(builder))
		fileLast.write(Utils.clearColors(builder))
		println builder.toString()
	}

	public class Summary {
		OrderParserType parser
		Double winloss
		Double dividends
	}

	static separatorCharCount = 100
	final separatorLine = "-" * separatorCharCount + "\n"

	@SuppressWarnings(['GroovyOverlyLongMethod', 'DuplicatedCode', "GroovyOverlyComplexMethod"])
	public SharesSummary printSummaryShares(boolean shouldLogToFile = true) {

		List<Transaction> allTransactions = this.shares.collectMany { share -> share.transactions }
		def nonBuyTransactions = allTransactions.findAll { trans -> !trans.isBuy() && !trans.isPreEmptive() }

		def builder = new StringBuilder()

		printOpenShares(builder)

		printOpenSharesByParser(builder, allTransactions)

		printWinLossByDate(builder, nonBuyTransactions)

		printWinLossByIsin(builder, nonBuyTransactions)

		printDividendeRevenue(builder, allTransactions)

		printSharesSummary(builder, allTransactions)

		printWinlossHistory(builder)

		printAnnualOpenInvested(builder, allTransactions)

		printCalcedTaxes(builder)

		def currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
		allTransactions.findAll { it.isDivKnoSell() && it.dateAsStr.startsWith(currentYear) }.groupBy { it.parserType }.each {
			if (it.key != OrderParserType.BISON) {
				builder << "   Parser: $it.key".padRight(33)
				builder << Utils.toCurrency(it.value.sum { it.getTax() }).padLeft(23)
				builder << "\n"
			}
		}

		printLastDateByParser(builder, allTransactions)

		printTemporaryOrders(builder, allTransactions)

		logBuilderToFileOrPrint(builder, shouldLogToFile)
		return this
	}

	def printOpenShares(StringBuilder builder) {
		builder << "\n"
		builder << "=" * separatorCharCount + "\n"
		builder << "Open Shares\n"
		builder << "=" * separatorCharCount + "\n"

		def openShares = shares.findAll { it.openAmount > 0 }
		openShares.each {
			builder.append("${it.isin}".padRight(18))
			builder.append("${Utils.toCurrency(it.rate)}".padLeft(20))
			builder.append(" x ${it.openAmount.round(3).toString()} =".padLeft(13))
			builder.append("${Utils.toCurrency(it.rate * it.openAmount)}".padLeft(25))
			builder.append("    ${it.name}\n")
		}
		builder << "=" * separatorCharCount + "\n\n"
	}

	def printLastDateByParser(StringBuilder builder, List<Transaction> allTransactions) {
		builder << "\n"
		builder << "=" * separatorCharCount + "\n"
		builder << " Last Transactions-Date By Parser\n"
		builder << "-" * separatorCharCount + "\n"

		allTransactions.groupBy { it.parserType }.sort { it.key }.each {
			def latest = it.value.sort { it.getDate().toEpochSecond(ZoneOffset.UTC) }.last();
			builder.append("${it.key.toString().padLeft(15, ' ')}: ")
			builder.append("${latest.getDateAsStr()} ")
			builder.append("${Utils.toCurrency(latest.total).padLeft(13, ' ')} ")
			builder.append("${latest.name}")
			builder << "\n"
		}
		builder << "=" * separatorCharCount + "\n"
	}

	def printOpenSharesByParser(StringBuilder builder, List<Transaction> allTransactions) {
		builder << "\n"
		builder << "=" * separatorCharCount + "\n"
		builder << " Open Shares By Parser\n"

		allTransactions.findAll { it.isBuy() && it.openAmount > 0 }.groupBy { it.parserType }.each {
			builder << "-" * separatorCharCount + "\n"
			builder << "\nParser: $it.key\n"
			builder << "-" * separatorCharCount + "\n"
			it.value.sort { it.name }.groupBy { it.isin }.each {
				builder.append("${it.value.first().name} (${it.key})".padRight(90, '.'))
				builder.append("x${Utils.roundUp(it.value.sum { it.openAmount }, 3)}".padLeft(10, '.'))
				builder << "\n"
			}
		}
		builder << "=" * separatorCharCount + "\n"
	}

	def printSharesSummary(StringBuilder builder, List<Transaction> allTransactions) {
		def shares = this.shares //.findAll { it.amount == 0 }
		builder << " Shares Summary ${new Date().format('yyyy-MM-dd')}" + "\n"
		builder << separatorLine
		// builder << " Total shares     : ${shares.size().toString().padLeft(13)}\n"
		builder << " Total invested   : ${calcInvested(shares).padLeft(22)}\n"
		builder << " Total winloss    : ${(Utils.toCurrency(shares.sum { it.winloss })).padLeft(22)}\n"
		builder << " Total fees/taxes : ${calcTaxesAndFees(shares).padLeft(22)}\n"
		builder << " Total interest   : ${Utils.toCurrency(shares.sum { it.interest }, true).padLeft(22)}\n"
		builder << " Total dividends  : ${Utils.toCurrency(allTransactions.sum { (it.isDiv()) ? it.winlossFifo : 0 }).padLeft(22)}\n"
		builder << "$separatorLine\n\n"
	}

	def printWinLossByDate(StringBuilder builder, List<Transaction> nonBuyTransactions) {
		def lastYear = null
		def lastYearSaldos = [:] as Map<OrderParserType, Double>
		builder << separatorLine
		builder << "Winloss By Date\n"
		builder << separatorLine

		def winLossByMonth = nonBuyTransactions.groupBy { it.date.format("yyyy-MM") }
		winLossByMonth.sort().each { String datStr, List<Transaction> transactions ->
			if (lastYear && !"${datStr}".startsWith(lastYear)) {
				builder << separatorLine
				lastYearSaldos.sort().each {
					builder << "$lastYear ${it.key}".padRight(44, '.') + Utils.toCurrency(it.value).padLeft(30, '.') + "\n"
				}
				if (lastYearSaldos.size() > 1) {
					builder << "$lastYear".padRight(44, '.') + Utils.toCurrency(lastYearSaldos.values().sum { it }).padLeft(30, '.') + "\n"
				}
				builder << separatorLine
				lastYearSaldos.clear()
			}

			def transByParser = transactions.groupBy { it.parserType }
			transByParser.each { parser, parserTransactions ->
				def sum = parserTransactions.sum { it.winlossFifo }
				if (lastYearSaldos.containsKey(parser)) {
					lastYearSaldos[parser] += sum
				} else {
					lastYearSaldos.put(parser, sum)
				}
			}

			//TODO: is glaub doch besser hier als XML zu machen, weisch du?! :D
			def monthWinLoss = transactions.sum { it.winlossFifo }
			builder << datStr + Utils.toCurrency(monthWinLoss).padLeft(32, '.') + "\n"
			// lastYearSaldo += monthWinLoss
			lastYear = datStr.split('-').first()

			transactions.groupBy { it.isin }.sort().each { isin, trans ->
				def isinWinLoss = trans.sum { it.winlossFifo }
				builder.append("${isin}".padLeft(15))
				builder.append(Utils.toCurrency(isinWinLoss).padLeft(24, '.'))
				builder.append("  ${trans.first().name}\n")

				trans.sort { a, b ->
					a.date <=> b.date ?: a.winlossFifo <=> b.winlossFifo
				}.each {
					builder.append(it.date.format('yyyy-MM-dd').padLeft(15))
					builder.append(Utils.toCurrency(it.winlossFifo).padLeft(24, '.'))
					builder.append("     ${it.parserType.identifier}")
					if (it.hasBuyTransAgeUnder365Days) {
						builder.append(" (unterjährig)")
					}
					builder.append("\n")
				}
			}
			return this
		}

		if (lastYear) {
			builder << separatorLine
			lastYearSaldos.sort().each {
				builder << "$lastYear ${it.key}".padRight(44, '.') + Utils.toCurrency(it.value).padLeft(30, '.') + "\n"
			}
			if (lastYearSaldos.size() > 1) {
				builder << "$lastYear".padRight(44, '.') + Utils.toCurrency(lastYearSaldos.values().sum { it }).padLeft(30, '.') + "\n"
			}
			builder << separatorLine
		}
	}

	def printWinLossByIsin(StringBuilder builder, List<Transaction> nonBuyTransactions) {
		builder << "\n"
		builder << "=" * separatorCharCount + "\n"
		builder << "WinLoss by ISIN\n"
		builder << "=" * separatorCharCount + "\n"
		nonBuyTransactions.groupBy { it.isin }.sort().each { isin, transactions ->
			builder.append("${isin} [${transactions.size().toString().padLeft(2, '0')}]".padLeft(17))
			builder.append(Utils.toCurrency(transactions.sum { it.winlossFifo }).padLeft(24, '.'))
			builder.append("  ${transactions.first().name}\n")
		}
		builder << "=" * separatorCharCount + "\n\n"
	}

	def printCalcedTaxes(StringBuilder builder) {
		def currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
		builder << "\n$separatorLine"
		builder << " Current Taxes (${currentYear}): " + calcTaxes(shares).padLeft(33)
		builder << "\n$separatorLine"
	}

	def printTemporaryOrders(StringBuilder builder, List<Transaction> allTransactions) {
		builder << "\n$separatorLine"
		def tempOrders = allTransactions.findAll { it.isTemporaryStatement() }
		if (tempOrders.any()) {
			builder << " Temporary Orders (not fully cleared)\n"
			builder << "$separatorLine"
			tempOrders.each {
				builder << "\t${it.toString().replaceAll('\n', ',')}\n"
			}
			builder << "$separatorLine"
		}
	}

	def logBuilderToFileOrPrint(StringBuilder builder, boolean shouldLogToFile) {
		if (shouldLogToFile) {
			logToFile(builder)
		} else {
			println builder.toString()
		}
	}

	public void printAnnualOpenInvested(StringBuilder builder, List<Transaction> allTransactions) {
		builder << "\n$separatorLine"

		def preDatedIssues = []
		def lastYearInvestedByParser = [:] as Map<OrderParserType, Double>
		def debugCounter = 0
		def years = allTransactions.groupBy { it.getDate().year }.sort { it.key }.keySet().toList()
		years.each { currentYear ->
			def annualSumByParser = [:] as Map<OrderParserType, Double>
			def transUpTill = allTransactions.findAll { it.getDate().compareTo(LocalDateTime.of(currentYear, 12, 31, 23, 59)) <= 0 }.collect().sort{ it.getDate() }.groupBy { it.parserType }
			transUpTill.each { parser, parserTrans ->
				def buyTransactions = parserTrans.findAll { (it.isBuy() || it.isCapital() || it.isPreEmptive()) }
				def sellTransactions = parserTrans.findAll { (it.isSell() || it.isKnockout()) }

				buyTransactions.each { it.openAmount = it.amount }
				sellTransactions.each { sellTrans ->
					sellTrans.openAmount = sellTrans.amount

					def preDatedBuyTransactions = appendAnnualOpenInvested_FindBuys(buyTransactions, sellTrans)
					if (!preDatedBuyTransactions.any()) {
						preDatedIssues << "No pre-dated buy transactions for\n$sellTrans"
					} else {
						appendAnnualOpenInvested_MatchBuys(preDatedBuyTransactions, sellTrans)
					}
				}

				def annualSum = buyTransactions.sum { buy ->
					def val = (buy.openAmount > 0 && buy.isBuy()) ? (buy.total / buy.amount) * buy.openAmount : 0.0d
					return val
				} ?: 0

				annualSumByParser.put(parser, annualSum)
			}

			if (preDatedIssues) {
				throw new IllegalStateException(preDatedIssues.join('\n----------------------------------------------\n'))
			}

			def fullsum = Utils.toCurrency(annualSumByParser.values().sum(), true)
			builder << "\n\n Open Invested ${currentYear}: ${fullsum.padLeft(38)}" + "LastYearDiff".padLeft(20)
			builder << "\n${"-" * separatorCharCount + "\n"}"
			annualSumByParser.each { parser, annualSum ->
				def total = Utils.toCurrency(annualSum, true)
				def diff = Utils.toCurrency(annualSum - lastYearInvestedByParser.get(parser, 0), true)
				builder << "\n      ${parser.name().padRight(15)} " + total.padLeft(36) + diff.padLeft(29)
				lastYearInvestedByParser.put(parser, annualSum)
			}
			builder << "\n$separatorLine"
		}
	}

	def appendAnnualOpenInvested_FindBuys(List<Transaction> buyTransactions, Transaction sellTrans) {
		return buyTransactions.findAll {
			def hasOpenAmount = it.openAmount > 0
			def wasBeforeSell = it.getDate() <= sellTrans.getDate()
			def sameIsin = it.isin == sellTrans.isin || (it.newWknOrIsin && it.newWknOrIsin == sellTrans.isin) || (sellTrans.newWknOrIsin && it.isin == sellTrans.newWknOrIsin)
			def sameOrigin = it.parserType == sellTrans.parserType
			return (it.isBuy() || it.isCapital() || it.isPreEmptive()) && sameIsin && hasOpenAmount && wasBeforeSell && sameOrigin
		}.sort { it.getDate() }
	}

	def appendAnnualOpenInvested_MatchBuys(List<Transaction> preDatedBuyTransactions, Transaction sellTrans) {
		def totalAmount = (preDatedBuyTransactions.sum { it.openAmount } as Double).round(8);
		if (totalAmount < sellTrans.openAmount) {
			def msg = "Cannot sell ${sellTrans.openAmount}, only $totalAmount available for\n$sellTrans"
			throw new IllegalStateException(msg)
		}

//		println "SELL ${sellTrans.isin} ${sellTrans.dateAsStr} ${sellTrans.openAmount}/${sellTrans.amount} ${sellTrans.id}"
		def fifo = preDatedBuyTransactions.iterator()
		while (fifo.hasNext() && sellTrans.openAmount > 0) {
			def buyTrans = fifo.next()
			def sellOpen = sellTrans.openAmount.round(10)
			def buyOpen = buyTrans.openAmount.round(10)
			sellTrans.openAmount -= (buyOpen >= sellOpen) ? sellOpen : buyOpen
			buyTrans.openAmount = Math.max(0, buyOpen - sellOpen)
//			println " BUY ${buyTrans.isin} ${buyTrans.dateAsStr} ${buyTrans.openAmount}/${buyTrans.amount}"
		}

		if (sellTrans.openAmount.round(10) > 0) {
			def msg = "Not fully sold ${sellTrans.amount}, only ${sellTrans.openAmount} available for\n$sellTrans"
			throw new IllegalStateException(msg)
		}
	}

	class DivDto {
		def isin
		def count
		def total
		def percent
		def name
		def year

		DivDto(isin, count, total, percent, name, year) {
			this.isin = isin
			this.count = count
			this.total = total
			this.percent = percent
			this.name = name
			this.year = year
		}
	}

	def printDividendeRevenue(StringBuilder builder, List<Transaction> allTransactions) {
		List<DivDto> dtos = []

		def allDividends = allTransactions.findAll { it.isDiv() && it.winlossFifo > 0 }
				.groupBy { it.getDate().year }.sort { it.key }
		/*
		def allDividends = allTransactions.findAll { it.isDiv() && it.winloss > 0 }
		allDividends.groupBy { it.isin }.each { isinAndDividendList ->
			years.each {year ->
				def totalPercent = 0.0d
				def totalDividend = 0.0d
				def dividends = isinAndDividendList.getValue().findAll { it.getDate().year == year }
				dividends.each { dividend ->
					def sameIsin = allTransactions.findAll {
						it.isBuy() && it.isin == dividend.isin && it.date.compareTo(dividend.date) < 1
					}
					def priorOpenInvestedByTotal = Math.abs(sameIsin.sum { it.total } ?: 0)
					float percentByTotal = (priorOpenInvestedByTotal == 0) ? 100 : Utils.roundUp((dividend.winloss / priorOpenInvestedByTotal * 100), 2)

					totalPercent += percentByTotal
					totalDividend += dividend.winloss
				}

				def dividend = dividends.first()
				dtos << new DivDto(dividend.isin, isinAndDividendList.value.size(), totalDividend, totalPercent, "${dividend.name} (${dividend.parserType})", )
			}
		}
		builder << "Dividend-Revenue ${currentYear}: ${Utils.toCurrency(dtos.sum { it.total })}\n"
		builder << "=" * separatorCharCount + "\n"
		dtos.sort { it.percent }.each { dto ->
			builder.append("${dto.isin} [x${dto.count}]".padLeft(15))
			builder.append(Utils.toCurrency(dto.total).padLeft(24, '.'))
			builder.append(String.format(Locale.ENGLISH, "%.2f %%", dto.percent).padLeft(14, '.'))
			builder.append("    $dto.name}\n")
		}
		*/

		allDividends.each {
			def dividends = it.getValue()
			def annualDividend = "${it.getKey()}: ${Utils.toCurrency(dividends.sum { it.winlossFifo })}"
			builder << "Dividend-Revenue $annualDividend\n"
			builder << "=" * separatorCharCount + "\n"
			dividends.groupBy { it.isin }.sort().each {
				def isin = it.getKey()
				def isinDividendsByParser = it.getValue().groupBy { it.parserType }

				isinDividendsByParser.each {
					def isinDividends = it.value
					builder.append("${isin} [x${isinDividends.size()}]".padLeft(15))
					builder.append(Utils.toCurrency(isinDividends.sum { it.winlossFifo }).padLeft(24, '.'))
					// builder.append(String.format(Locale.ENGLISH, "%.2f %%", dto.percent).padLeft(14, '.'))
					builder.append("    ${isinDividends.first().name} (${it.key.shortName})\n")

				}

			}
			builder << "=" * separatorCharCount + "\n\n"
		}
	}

	public void printWinlossHistory(StringBuilder builder) {
		Main.printout = false

		def winlosses = [:] as Map<LocalDate, Double>
		def root = new groovy.xml.XmlSlurper(false, false).parse(new File('./summaries/Summary.xml'))

		root.childNodes().each { groovy.xml.slurpersupport.Node share ->
			share.childNodes().each { groovy.xml.slurpersupport.Node trans ->
				def attribs = trans.attributes()
				//noinspection GrUnresolvedAccess
				def loss = attribs['winloss']
//			def loss = it['@winlossNoTax'].text()
				if (loss) {
					def date = LocalDate.parse(attribs['date'].toString().substring(0, 10))
					def val = winlosses.getOrDefault(date, 0.0d)
					winlosses[date] = val + Double.parseDouble(loss)
				}
			}
		}

		def wlset = winlosses.sort().entrySet()
		if (wlset.size() == 0) {
			return
		}

		Double totalWinloss = winlosses.values().sum()
		Double minWinloss = winlosses.values().min()
		Double maxWinloss = winlosses.values().max()

		def dateStart = LocalDate.of(LocalDate.now().year, 1, 1)
		def dateEnd = LocalDate.now()

		def last = wlset.last().key.toDate().format("yyyy-MMM-dd")
		def mins = winlosses.findAll { it.value == minWinloss }.entrySet()
		assert minWinloss == 0 || mins.size() == 1
		def maxs = winlosses.findAll { it.value == maxWinloss }.entrySet()
		assert maxWinloss == 0 || maxs.size() == 1

		builder << "\n Win/Loss History ${dateStart.year}\n"
		builder << "-" * 80
		builder << "\n Total WinLoss:".padRight(24) + Utils.toCurrency(totalWinloss).padLeft(23) + " on $last"
		builder << "\n Max WinLoss:".padRight(24) + Utils.toCurrency(minWinloss).padLeft(23) + " on ${mins.first().key.toDate().format('yyyy-MMM-dd')}"
		builder << "\n Min WinLoss:".padRight(24) + Utils.toCurrency(maxWinloss).padLeft(23) + " on ${maxs.first().key.toDate().format('yyyy-MMM-dd')}"
		builder << "\n"
		builder << "-" * 80
		builder << "\n"
		Double total = 0.0d
		winlosses.sort().each {
			total += it.value
			if (it.key >= dateStart && it.key <= dateEnd) {
				builder << " ${it.key}:".padRight(24) + Utils.toCurrency(total).padLeft(22)
				builder << "\n"
			}
		}
		builder << "-" * 80
		builder << "\n"
	}
}
