package com.asbytes

import com.asbytes.parser.OrderParser
import com.asbytes.parser.OrderParserConfig
import com.asbytes.parser.OrderParserType
import groovy.json.JsonOutput
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.runtime.InvokerHelper
import org.junit.Test

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import java.nio.file.Paths

import static groovyx.gpars.GParsPool.withPool
import com.asbytes.helpers.Utils

class Main extends Script {
	static printout = true
	static useDatastore = false
	static nonStopingParserErrors = []

	static void printout(Object obj) {
		if (Main.printout) {
			println "$obj"
		}
	}

	static void main(String[] args) {
		def start = System.currentTimeMillis()
		InvokerHelper.runScript(Main, args)
		def diff = System.currentTimeMillis() - start
		println "done after $diff ms"
		if (nonStopingParserErrors.size() > 0) {
			println Utils.ANSI_RED
			println nonStopingParserErrors.join('\n' + ('-' * 80) + '\n')
			println Utils.ANSI_DEFAULT
		}
	}

	static Object stopwatch(String context, Closure closure) {
		def start = System.currentTimeMillis()
		Object obj = null
		try {
			obj = closure.call()
			def diff = System.currentTimeMillis() - start
			Main.printout "$context done after $diff msec"
		} catch(IllegalStateException ise) {
			println "\n"
			println "!" * 80
			println ise.message
			println "!" * 80
		} catch(Throwable throwable) {
			def diff = System.currentTimeMillis() - start
			println "$context done after $diff msec with Exception ${throwable.class.simpleName}"
			println throwable.message
			println "-" * 100
			println throwable.stackTrace.join('\n')
			if (throwable.cause) {
				println "-" * 100
				println throwable.cause.message
				println "-" * 100
				println throwable.cause.stackTrace.join('\n')
				if (throwable.cause.cause) {
					println "-" * 100
					println throwable.cause.cause.message
					println "-" * 100
					println throwable.cause.cause.stackTrace.join('\n')
				}
			}
			throw throwable
		}
		return obj
	}

	static List<Share> runParserConfigs() {
		List<Transaction> transactions = stopwatch("File Parser", { parseFolders(OrderParserConfig.getConfigs()) })
		Main.printout "${transactions?.size() ?: 0} Transactions"

		def removeTransactions = []
		// find dupes
		transactions.groupBy { it.parserType }.each {
			Map<String, Transaction> dupeTrans = [:]
			int dupeCount = 0
			it.value.each { Transaction trans ->
				def hash = trans.createHash()
				if (dupeTrans.containsKey(hash) && trans.total != 0 && trans.getDate().year > 2018) {
					def dupeDate = dupeTrans.get(hash).getDate()
					def datediff = trans.getDate().compareTo(dupeDate)
					if (datediff < 7) {
						def skipDetection = false
						if (trans.isQuirion() || trans.isFinZeroCsv()) {
							skipDetection = true
						} else if (trans.parserType == OrderParserType.FINNETZERO_PDF) {
							def content = new com.asbytes.helpers.PdfParser(trans.file).pdfText
							if (content.contains("Dividendenabrechnung STORNOKOPIE")) {
								println "-" * 80
								println "Found Duplicates, but matches Stornokopie, removing both transactions"
								removeTransactions << trans
								removeTransactions << dupeTrans.get(hash)
								dupeTrans.remove(hash)
								skipDetection = true
							}
						}

						if (!skipDetection) {
							println "-" * 80
							println "Possible Duplicates:"
							println "-" * 80
							println "- $trans"
							println "- ${dupeTrans.get(hash)}"
							// todo debug removing printout of pdf-text for dupes duplicate
//							if (trans.file?.exists() && trans.file.name.endsWith('.pdf')) {
//								println new com.asbytes.helpers.PdfParser(trans.file).pdfText
//							}
							println "-" * 80
							dupeCount++
						}
					} else if (trans.parserType != OrderParserType.QUIRION) {
						println "-" * 80
						println "Possible Duplicates but older than 7 days:"
						println "-" * 80
						println "- $trans"
						println "- ${dupeTrans.get(hash)}"
						println "-" * 80
					}
				} else {
					dupeTrans.put(hash, trans)
				}
			}

			if (dupeCount > 0) {
				throw new IllegalStateException("$dupeCount possible dupes")
			}
		}

		removeTransactions.each {
			transactions.remove(it)
		}

		return (transactions) ? stopwatch("Shares", { createSharesFromTransactions(transactions) }) : []
	}


	/** groovy invokescript entry */
	def run() {
		this.execute()
	}

	@Test
	void execute() {
		def withRates = false
		def withJson = true
		def withExcel = true

		List<Share> shares = runParserConfigs() ?: []
		println()
		println "${shares.size()} Shares"

		if (shares.any()) {
			def xmlShares = XmlShares.fromOrders(shares)
			stopwatch("Pre-Rates-XML", { println xmlShares.toXml("./summaries/Summary.xml").absolutePath })

			stopwatch("Summary", { new SharesSummary(shares).printSummaryShares() })

			if (withJson) {
				println "Creating Json"
				toJson(shares)
			}

			if (withExcel) {
				println "Creating Excel CSV"
				toExcelCsv(shares)
			}

			if (withRates) {
				stopwatch("Rates", {
					Main.printout("Rates:")
					def isins = shares.findResults { (it.isClosed()) ? null : it.isin }
					def bidAsks = []
					def summaryFile = new File('./summaries/summary.xml')
					def summaryShares = new groovy.xml.XmlParser().parse(summaryFile)
					withPool(4, {
						isins.eachWithIndexParallel { isin, idx ->
							def html = Exchange.Finanzen100.getHtml(isin)
							def bidAsk = Exchange.Finanzen100.getBidAndAsk(isin, html)
							if (bidAsk) {
								bidAsks << bidAsk
							}
							println "${(idx+1).toString().padLeft(3)}/${isins.size()} $isin"
						}
					})

					def marketVal = 0.0d
					def winLoss = 0.0d
					def posWinLoss = 0.0d
					def posFunds = 0.0d
					def maxWinLoss = 0.0d
					bidAsks.each { BidAndAsk bidAsk ->
						def xmlShare = summaryShares.find { it['@isin'] == bidAsk.isin }
						xmlShare['@bid'] = bidAsk.bid ?: 0

						Share share = shares.find { Share share -> share.isin == bidAsk.isin }
						def shareMarketVal = share.openAmount * bidAsk.bid
						marketVal += shareMarketVal
						def shareMarketWinloss = shareMarketVal + share.openTotal
						winLoss += shareMarketVal + share.openTotal

						maxWinLoss += share.openAmount * bidAsk.hi52w + share.openTotal

						if (shareMarketWinloss > 0) {
							posWinLoss += shareMarketWinloss
							posFunds += shareMarketVal
						}

					}

					println()
					println "-" * 80
					println "Open Shares =Market Value: ".padRight(33) + Utils.toCurrency(marketVal).padLeft(44)
					println "Open Shares =WinLoss Value: ".padRight(33) + Utils.toCurrency(winLoss).padLeft(44)
					println "Open Shares +WinLoss Value: ".padRight(33) + Utils.toCurrency(posWinLoss).padLeft(44)
					println "Open Shares +WinLoss Funds: ".padRight(33) + Utils.toCurrency(posFunds).padLeft(44)
					println "Open Shares =52Week WinLoss: ".padRight(33) + Utils.toCurrency(maxWinLoss).padLeft(44)

					new FileWriter(summaryFile).withCloseable { fileWriter ->
						new groovy.xml.XmlNodePrinter(new PrintWriter(fileWriter)).print(summaryShares)
					}
				})
			}

			println "Creating HTML"
			xmlToHtml()
		}

		println new Date()
	}

	private static void toJson(List<Share> shares) {
		stopwatch("OpenSharesJson", {
			List<Map<String, String>> json = []
			shares.findAll { !it.isClosed() }.each {
				def share = [:]
				share.put('Isin', it.isin)
				share.put('Amount', it.getOpenAmount())
				share.put('Rate', it.getRate())
				def parsers = it.transactions.findAll {
					it.openAmount > 0
				}.collect { it.parserType.getShortName() }.unique()
				share.put('Name', it.getName() + " (@${parsers.join('|')})")
				json << share
			}

			def data = JsonOutput.prettyPrint(JsonOutput.toJson(json))
			def file = new File('./summaries/openShares.json')
			file.setText(data, 'UTF-8')
			println file.getAbsolutePath()
			})
	}

	public static void xmlToHtml() {
		def xsl = new File('./src/main/groovy/com/asbytes/summary.xsl')
		stopwatch("HTML", {
			def lastHtml = toHtml(xsl, "./summaries/Summary.html")
			println "${lastHtml.absolutePath}\n"

			def backupName = lastHtml.name.replace('.html', "${new Date().format('-yyyy-MM')}.html")
			def backupDir = Paths.get(lastHtml.parentFile.getAbsolutePath(), new Date().format('yyyy'))
			backupDir.toFile().mkdirs()

			FileUtils.copyFile(lastHtml, Paths.get(backupDir.toString(), backupName).toFile(), true)
		})
	}

	def static File toHtml(File xsl, File xml, File html) {
		def fact = TransformerFactory.newInstance()
		def xslt = new StreamSource(xsl)
		def transformer = fact.newTransformer(xslt)

		html.write("")
		def result = new StreamResult(html)
		def source = new StreamSource(xml);
		transformer.transform(source, result)
		return html
	}

	def static File toHtml(File xsl, String htmlPath) {
		def xml = "./summaries/Summary.xml"
		return toHtml(xsl, new File(xml), new File(htmlPath))
	}

	public static List<Transaction> parseFolders(List<OrderParserConfig> configs) {
		def transactions = Collections.synchronizedList(new ArrayList<Transaction>())
		configs.each { OrderParserConfig config ->
			if (config.singleTransFiles) {
				transactions.addAll(parseFoldersWithCsv(config))
			} else if (!config.singleTransFiles) {
				transactions.addAll(parseFoldersWithoutDat(config))

			}
			Main.printout "${transactions.size()} Files [${config.parserType.name()}]"
		}

		return transactions
	}

	public static List<Share> createSharesFromTransactions(List<Transaction> transactions) {
		transactions.groupBy { it.isin }.collect { String isin, List<Transaction> isinTransactions ->
			if (isin != null) {
				def share = new Share(isin)

				def nonTempTransactions = isinTransactions.findAll { !it.isTemporaryStatement() }
				try {
					// ensure done transactions already added for dupe check in addTransactions afterwards
					nonTempTransactions.groupBy { it.parserType }.each { parser, parserTransactions ->
							share.addTransactions(parserTransactions)
					}
				} catch(err) {
					nonStopingParserErrors << ["*" * 80, err.message, "*" * 80].join('\n')
					return null
				}

				def tempTransactions = isinTransactions.findAll { it.isTemporaryStatement() }
				def unmatchedTempTransactions = [] as List<Transaction>
				tempTransactions.each { tempTransaction ->
					def nonTemps = nonTempTransactions.findAll { it.parserType == tempTransaction.parserType }
					def match = nonTemps.find { it.id.startsWith(tempTransaction.id)}
					if (!match) {
						unmatchedTempTransactions << tempTransaction
					} else {
						println "-" * 80
						println "Temporary Trans: ${tempTransaction.getFileName()}"
						println "Was deleted due to override by: $match"
						tempTransaction.file.deleteOnExit()
					}
				}

				unmatchedTempTransactions.findAll { it.isTemporaryStatement()}.groupBy { it.parserType }.each { parser, parserTransactions ->
					share.addTransactions(parserTransactions)
				}
				return share
			} else {
				return null
			}
		}.findAll { it != null }
	}

	public static List<Transaction> parseFoldersWithCsv(OrderParserConfig config) {
		def files = stopwatch("Getting CSV-Files from Folders (${config.parserType.name()})", {
			config.folderMaps.collectMany { String folderPath, String fileFilter ->
				Utils.getFilesOfFolder(folderPath, fileFilter).collect {
					return it
				}
			}
		})

		List<Transaction> transactions = []
		files.each { File file ->
			try {
				def fileTransactions = config.parserType.createTransactionsFromCsv(file, config.firstLineOfCsv);
				fileTransactions.each { Transaction fileTrans ->
					def existsAlready = transactions.find { it.isin == fileTrans.isin && it.id == fileTrans.id && it.amount == fileTrans.amount && it.tax == fileTrans.tax }
					if (existsAlready && !fileTrans.isQuirion() && !fileTrans.isFinZeroCsv()) {
						if (fileTrans.isSmartbroker() && Transaction.isUpdatedSmartBrokerStatus(existsAlready, fileTrans)) {
							transactions.remove(existsAlready)
						} else if (fileTrans.isDegiro()) {
							if (fileTrans.deGiroSaldo == existsAlready.deGiroSaldo) {
								transactions.remove(existsAlready)
							}
						} else {
							throw new IllegalStateException("Duplicate transaction?\nnew:\n${fileTrans}\nold:\n${existsAlready}")
						}
					}
					transactions.push(fileTrans)
				}
			} catch(Throwable throwable) {
				Main.printout("-" * 80)
				Main.printout(throwable.message)
				Main.printout("-" * 80)
				throw throwable
			}
		}
		transactions.removeAll([null])

		return transactions ?: []
	}

	public static List<Transaction> parseFoldersWithoutDat(OrderParserConfig config) {
		List<File> files = stopwatch("Getting Files from Folders (${config.parserType.name()})", {
			config.folderMaps.collectMany { String folderPath, String fileFilter ->
				return Utils.getFilesOfFolder(folderPath, fileFilter).collect {
					return it
				}
			}
		})

		List<Transaction> transactions = stopwatch("Parsing ${files.size()} files", {
			def transactions = Collections.synchronizedList(new ArrayList<Transaction>())
			if (Main.useDatastore) {
				new Datastore().openConnection()?.ensureTableExists()?.withCloseable { dataStore ->
					def storedTransactions = dataStore.loadTransactions()
					files.each {
						transactions.addAll(loadOrSaveTransaction(config, dataStore, storedTransactions, it))
					}
				}
			} else {
				files.each {
					transactions.addAll(config.parserType.createTransactionFromPdf(it))
				}
			}
			transactions.removeAll([null])
			return transactions
		})

		return transactions ?: []
	}

	public static List<Transaction> loadOrSaveTransaction(OrderParserConfig config, Datastore dataStore, List storedTransactions, File file) {
		try {
			def storedTransaction = storedTransactions.findResult {
				def isMatch = (it['version'] == Transaction.storeDataVersion
						&& it['source'] == Transaction.getRelativePath(file)
						&& it['size'] == file.size()
						&& it['date'] == file.lastModified())
				return (isMatch) ? Transaction.fromBlob(it['data']) : null
			}

			if (storedTransaction) {
				def overwrite = config.parserType.parser().getOverwrites().getOrDefault(storedTransaction.id, null);
				if (overwrite) {
					overwrite(storedTransaction);
					storedTransaction.overridden = true
				}
				return [storedTransaction]
			}

			def parsedTransactions = config.parserType.createTransactionFromPdf(file)

			def noSplitTransaction = parsedTransactions.size() == 1
			if (noSplitTransaction) {
				dataStore.saveTransaction(parsedTransactions.first())
			} else {
				println "  was split"
			}

			return parsedTransactions
		} catch (Throwable throwable) {
			Main.printout("-" * 80)
			Main.printout(file.absolutePath)
			Main.printout(throwable.message)
			Main.printout("-" * 80)
			throw throwable
		}
	}

	private static void toExcelCsv(List<Share> shares) {
		stopwatch("OpenSharesExcel", {
			final separator = ';'
			List<String> csv = []
			def allTransactions = shares.collectMany { share -> share.transactions }
			def last40 = allTransactions.sort { it.getDate() }.findAll { !it.isDiv() }.reverse(true)

			csv << ['Type', 'Date', 'Id', 'Isin', 'Name', 'Amount', 'Rate', 'Fee', 'Invested'].join(separator)
			last40.each { trans ->
				def line = []
				def prefix = (trans.isDivKnoSell()) ? "VK" : "EK"
				line << "$prefix ${trans.parserType.getShortName()}"
				line << trans.getDateAsStr("dd.MM.yyyy")
				line << '#' + ((trans.id?.contains('#')) ? trans.id.split('#').first() : trans.id)
				line << trans.isin
				line << trans.name
				line << trans.amount.toString().replace('.', ',')
				line << trans.rate.toString().replace('.', ',')
				line << trans.fee.toString().replace('.', ',')
				line << trans.total.toString().replace('.', ',')

				csv << line.join(separator)
			}

			def data = csv.join('\n')
			def file = new File("./summaries/last-all.csv")
			file.setText(data, 'UTF-8')
			println file.getAbsolutePath()
		})
	}
}
