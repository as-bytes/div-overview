package com.asbytes

import com.asbytes.helpers.Utils
import com.asbytes.parser.OrderParser
import com.asbytes.parser.OrderParserType
import groovy.json.JsonSlurper
import groovy.transform.ToString
import groovy.xml.slurpersupport.NodeChild
import groovyx.gpars.GParsPool
import org.jsoup.Jsoup
import org.junit.Ignore
import org.junit.Test

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

//TODO test chart from https://www.fusioncharts.com/dev/university/creating-a-realtime-bitcoin-ticker-in-javascript
class UnitTests {

	static class DatastoreTests {

		Transaction getTestTransaction() {
			def tempSourceFile = File.createTempFile('ignoreme', 'pdf')
			tempSourceFile << "Lorem ipsum dolor ipset"

			def transaction = new Transaction(OrderParserType.DEGIRO, tempSourceFile)
			transaction.with {
				sqlDataVersion = 1
				name = "TestTrans"
				addTax(9.90)
				addFee(4.50)
				total = 120.50 + 9.90 + 4.50
				amount = 10
				openAmount = 5
				currency = Transaction.Currency.EUR
				setRate(120.50 / 10)
				setTemporaryStatement(true)
				setDate("2020-12-31")
				id = "FakeTestId"
				isin = "DE1234567890"
				hasBuyTransAgeUnder365Days = true
				newWknOrIsin = null
				overridden = false
				winlossFifo = 543.21
				deGiroSaldo = 12345.67
				setSmartBrokerStatus(Transaction.SmartBrokerStatus.EXECUTED)
			}

			assert OrderParser.validate(transaction) != null
			assert transaction.file.exists()

			return transaction
		}

		private File ensureTestDBCleared() {
			def testDB = new File("./~testdb.sqlite")
			testDB.delete()
			testDB.deleteOnExit()
			assert !testDB.exists()
			return testDB
		}

		@Test
		public void testSerializeTransaction() {
			def transaction = this.getTestTransaction()
			def bytes = transaction.toBlob()
			assert bytes.length > 0

			def deseralized = Transaction.fromBlob(bytes)
			assert deseralized != null

			assert transaction.toString() == deseralized.toString()
			assert OrderParser.validate(deseralized) != null
		}

		@Test
		public void testCreateDatabaseOnConnect() {
			// arrange
			def testDB = this.ensureTestDBCleared()

			// act
			new Datastore(testDB.absolutePath).openConnection().withCloseable {
				assert it.ensureTableExists() != null
				assert it.sql.updateCount == 0
			}

			// assert
			assert testDB.exists()
		}

		@Test
		public void testStoreTransaction() {
			def testDB = this.ensureTestDBCleared()
			def testTransaction = this.getTestTransaction()
			def testStore = new Datastore(testDB.absolutePath)

			testStore.openConnection().withCloseable {
				assert it.ensureTableExists() != null
				assert testDB.exists()
				assert it.saveTransaction(testTransaction) == 1
			}

			// assert
			assert testStore.sql.updateCount == 1

			testStore.openConnection().withCloseable {
				def stored = testStore.loadTransactions()
				assert stored.size() == 1
				assert stored.first().get('md5')
				assert stored.first().get('source')
				assert stored.first().get('data') == testTransaction.toBlob()
			}
		}

		@Test
		public void testLoadTransaction() {
			def testDB = this.ensureTestDBCleared()
			def testTransaction = this.getTestTransaction()
			def testStore = new Datastore(testDB.absolutePath)

			// act
			try {
				assert testStore.openConnection() != null
				assert testStore.ensureTableExists() != null
				testStore.saveTransaction(testTransaction)
			} finally {
				assert testStore.closeConnection() == true
			}

			// assert
			assert testDB.exists()
			assert testStore.sql.updateCount == 1

			try {
				assert testStore.openConnection() != null
				def stored = testStore.executeQuery("SELECT * FROM transactions;")
				assert stored.size() == 1
				assert stored.first().get('md5')
				assert stored.first().get('source')
				assert stored.first().get('data') == testTransaction.toBlob()
			} finally {
				assert testStore.closeConnection() == true
			}
		}

		@Test
		public void testFindTransactions() {
			// arrange
			def testTransaction = this.getTestTransaction()
			def testStore = new Datastore(this.ensureTestDBCleared()?.absolutePath)
			try {
				assert testStore.openConnection()?.ensureTableExists() != null
				testStore.saveTransaction(testTransaction)
			} finally {
				assert testStore.closeConnection() == true
			}

			// act
			def foundTransaction = null
			try {
				assert testStore.openConnection()?.ensureTableExists() != null
				foundTransaction = testStore.findTransaction(testTransaction.sqlDataVersion, testTransaction.file)
			} finally {
				assert testStore.closeConnection() == true
			}

			// assert
			assert foundTransaction != null
		}
	}

	@ToString
	class Bitwarden {
		String id
		String organizationId
		String folderId
		String notes
		boolean favorite
		int type
		int reprompt
		int number
		String name
		String collectionIds
		BitwardenLogin login
	}

	@ToString
	class BitwardenLogin {
		BitwardenUri[] uris
		String username
		String password
		String totp
	}

	@ToString
	class BitwardenUri {
		String match
		String uri
	}


	@Test
	public void testEnticklerHeld() {
		def str = "ABCDEF"
		def arr1 = str.split("\\w{3}")
		def arr2 = str.split("[A-Z]{3}")
		def arr3 = str.split("\\w{3}/gm")
		def arr4 = str.split("[A-Z]{3}/gm")

		println arr4
		// new JsonBuilder(json).toPrettyString()
	}

	@Test
	public void findBitwardenDupes() {
		def json = new JsonSlurper().parse(new File('D:\\My Desktop\\Bitwarden-DeDupe\\bitwarden_export_20220320145611.json'))
		json.items.each { entry ->
			def login = new Bitwarden(entry)
			//println login
			if (login.collectionIds) {
				println login
			}
			//println "-" * 80
		}

		// new JsonBuilder(json).toPrettyString()
	}

	@Test
	@Ignore
	public void testDuckDuckGoFindIsin() {
		def url = "https://html.duckduckgo.com/html/?q="
		def query = "isin+'Vanguard FTSE Japan UCITS ETF Registered Shares USD Dis.oN'"
		def expectedIsin = 'IE00B95PGT31'
		def isinPattern = /(?m)([A-Za-z]{2}[0-9A-Za-z]{9}[0-9])/

		println url + query
		def doc = Jsoup.connect(url + query).get().toString()

		def matches = doc.findAll(isinPattern)


		matches.collect { it.toUpperCase() }.unique().each {unique ->
			print matches.findAll { unique == it.toUpperCase() }.size()
			println "x$unique"
		}

		println doc
		//['userAgent': 'Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36']
	}

	@Test
	public void testXslt() {
		def xsl = new File("src\\main\\groovy\\com\\asbytes\\summary.xsl")
		def xml = new File("summaries\\Summary.xml")

		assert xsl.exists()
		assert xml.exists()

		def html = new File("D:/test.html")
		println Main.toHtml(xsl, xml, html)
	}

	@Test
	public void plotValues() {
		def vals = []
		vals << 1000
		vals << 0
		vals << -1000
		vals << -2000
		vals << -12000

		def maxHeight = 10
		def maxEntries = 10

		def limitedVals = vals.takeRight(maxEntries)

		def minVal = limitedVals.min()
		def maxVal = limitedVals.max()
		def diffRange = maxVal - minVal
		def valEachHeight = diffRange / maxHeight

		println "min: $minVal"
		println "max: $maxVal"
		println "diff: $diffRange"
		println "valEach: $valEachHeight"

		def entries = (1..maxHeight).collect { [] }

		println "preSortIn:"
		println entries

		//sort in vals
		limitedVals.each { val ->
			def index = Math.floor(val / diffRange)
			entries[index] << val
		}

		println "postSortIn:"
		println entries

		println "██"
		println "──────────────"
		println "    ██  ██  ██"
		println "        ██  ██"
		println "            ██"
		println "            ██"
		println "            ██"
		println "            ██"
		println "            ██"
		println "            ██"
		println "01  02  03  04"
		println "20  20  20  20"
	}

	@Test
	public void getBidAskFromHtmlFinanzen100OfXml() {
		def isins = ['DE0006204407']

		//def xml = new XmlSlurper(false, false).parse(new File('O:\\OneDrive\\Projects\\Groovy\\OrderStatementParser\\summaries\\Summary.xml'))
		//isins = xml.share.findAll { it['@closed'] != 'true' }.collect { it['@isin'].text() }

		def bidAsks = []
		GParsPool.withPool(20) {
			isins.eachParallel { isin ->
				def html = Exchange.Finanzen100.getHtml(isin)
				def bidAndAsk = Exchange.Finanzen100.getBidAndAsk(isin, html)
				assert bidAndAsk.isin != null
				assert bidAndAsk.bid != null
				assert bidAndAsk.ask != null
				assert bidAndAsk.hi52w != null
				bidAsks << bidAndAsk
				println "$isin $bidAndAsk"
			}
		}

		def summaryFile = new File('./summaries/summary.xml')
		def shares = new groovy.xml.XmlParser().parse(summaryFile)
		bidAsks.each { BidAndAsk bidAsk ->
			def share = shares.find { it['@isin'] == bidAsk.isin }
			share['@bid'] = bidAsk.bid
		}

		new FileWriter(summaryFile).withCloseable { fileWriter ->
			new XmlNodePrinter(new PrintWriter(fileWriter)).print(shares)
		}
	}

	@Test
	public void getBidAskFromHtmlFinanzen100_TestHtml() {
		def html = new File('O:\\OneDrive\\Projects\\Groovy\\OrderStatementParser\\src\\test\\resources\\TestHtmlFinanzen100.html').getText('UTF-8')
		def bidAndAsk = Exchange.Finanzen100.getBidAndAsk("DE12345678", html)
		assert bidAndAsk.exchange == 'Tradegate'
		assert bidAndAsk.bid == 8.75
		assert bidAndAsk.ask == 8.76
		assert bidAndAsk.hi52w == 22.7
	}

	private getRateFromCoingecko(map) {
		return map['tickers'].find { it['target'] == 'EUR'}['last']
	}


	/*
		Bison:				Closest
		5650 				cex.io 		cg:bitpanda  		cg:cex 		cg:gdax 		cg:kraken
		5651							cg:bitpanda										cg:kraken
		5642												cg:cex 		cg:gdax
		5646				cex:io 							cg:cex		cg:gdax			cg:kraken
		5657							cg:bitpanda
		5658															cg:gdax			cg:kraken
		5647												cg:cex
		5600 5643 5558		cex.io		cg:bitpanda			cg:cex						cg:kraken
	 */

	@Test
	public void testBtcCoingeckoLive() {
		def bisonSpread = 1.5;
		def url = 'https://api.coingecko.com/api/v3/coins/bitcoin/tickers?exchange_ids=kraken,gdax,bitpanda,cex'
		def json = Exchange.getHtmlFromUrl(url.toString())
		def root = new JsonSlurper().parseText(json)
		def list = []
		root['tickers'].findAll { it['target'] == 'EUR' && (['BTC', 'XBT'].contains(it['base'])) }.each {
			def map = [:]
			map.name = it['market']['identifier'].toString()
			def bid = new BigDecimal(it['last']).round(0)
			def bisonBid = bid * (1 + bisonSpread / 100 / 2)
			def bisonAsk = bid * (1 - bisonSpread / 100 / 2)
			def time = OffsetDateTime.parse(it['last_fetch_at'])
			def age = Instant.now().until(time.toInstant(), ChronoUnit.SECONDS)
			map.range = "${bisonAsk.round(0)}-> $bid <-${bisonBid.round(0)}".padLeft(15)
			map.age =  age
			list << map
		}

		list.sort { it['age'] * -1 }.each {
			print "${it['name'][0]} ${it['range']}"
			print "${it['age']} secs ago".padLeft(15)
			println()
		}


	}

	@Test
	public void testBtcLive() {
		def bisonSpread = 1.5;
		def map = [:]
//		map.put(new URL('https://api.coinbase.com/v2/prices/spot?currency=EUR'), [name: 'coinbase', bid: { it['data']['amount'] }])
//		map.put(new URL('https://api.coindesk.com/v1/bpi/currentprice.json'), [name: 'coindesk', bid: { it['bpi']['EUR']['rate_float'] }])
//		map.put(new URL('https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=EUR'), [name: 'coingecko', bid: { it['bitcoin']['eur'] }])
//		map.put(new URL('https://api.coingecko.com/api/v3/coins/bitcoin/tickers?exchange_ids=binance'), [name: 'coingecko:binance', bid: this.&getRateFromCoingecko])
//		map.put(new URL('https://cex.io/api/ticker/BTC/EUR'), [name: 'cex.io', bid: { it['bid'] }])
		map.put(new URL('https://api.coingecko.com/api/v3/coins/bitcoin/tickers?exchange_ids=kraken'), [name: 'coingecko:kraken', bid: this.&getRateFromCoingecko])
		map.put(new URL('https://api.coingecko.com/api/v3/coins/bitcoin/tickers?exchange_ids=gdax'), [name: 'coingecko:gdax', bid: this.&getRateFromCoingecko])
		map.put(new URL('https://api.coingecko.com/api/v3/coins/bitcoin/tickers?exchange_ids=bitpanda'), [name: 'coingecko:bitpanda', bid: this.&getRateFromCoingecko])
		map.put(new URL('https://api.coingecko.com/api/v3/coins/bitcoin/tickers?exchange_ids=cex'), [name: 'coingecko:cex', bid: this.&getRateFromCoingecko])
		map.each { URL url, Map data ->
			def json = Exchange.getHtmlFromUrl(url.toString())
//			println json
			def root = new JsonSlurper().parseText(json)
			//def time = Long.parseLong(root[data.time]) * 1000
			//def date = new Timestamp(time).format('dd.MM.yyyy hh:mm')
			//def ask = root[data.ask].toString()
			def bid = new BigDecimal(data.bid(root)).round(0)
			def bisonBid = bid * (1 + bisonSpread / 100 / 2)
			def bisonAsk = bid * (1 - bisonSpread / 100 / 2)
			//def bid = root[data.bid].toString()

			print data.name.toString().padRight(20)
			//print date.padRight(25)
			//print "$ask - $bid"
			print "$bid".padRight(20)
			print "$bisonAsk - $bisonBid"
			println()
		}
	}

	@Test
	public void testWinLossPackaging() {
		Main.printout = false
		List shares = Main.runParserConfigs()
		println "Shares: ${shares.size()}"
		println "Winloss: ${ Utils.toCurrency(shares.sum { it.winloss }) }"
		// ....
	}


	@Test
	public void testXmlToHtml2() {
		/// Rate in XSL ist nicht richtig
		/// z.B. Bayer AG = 95,2 bei 80x und Invested 1429,41 sollte es 17,867625 sein
		def isin = 'DE000BAY0017'
		Main.printout = false
		def shares = new Main().runParserConfigs() as List<Share>
		def share = shares.find { it.isin == isin }
		println "open amount: ${share.openAmount}"
		println "open total: ${share.openTotal}"
		println "amount: ${share.amount}"
		println "rate: ${share.rate}"
		println "invested: ${share.invested}"
		println "trans: ${share.transactions.collect { it.toString() }.join('\n')}"
	}

	@Test
	public void testXmlToHtml() {
		Main.xmlToHtml()
	}

	@Test
	public void testRateParser() {
		def shares = []
		shares << [new Share('NO0010081235')] //('Nel ASA')]
		shares << [new Share('IE00B3WJKG14')] //('iShares SP500')]
		shares << [new Share('DE000TR8T4A1')] //('Long Thyssen')]
		shares << [new Share('LU0942970798')] //('XTracker')]
		shares << [new Share('DE000A0KRKM5')] //('ETF Oil')]

		shares.each { Share share ->
			println "$share.name ($share.isin)"
			def bid = Exchange.getBid(share.isin)
			assert bid != null
		}
	}

	@Ignore
	@Test
	@SuppressWarnings("GrUnresolvedAccess")
	public void testGoogleMapsTimeLine() {
		def file = 'F:\\takeout-20200228T202918Z-001\\Takeout\\Standortverlauf\\Semantic Location History\\2020\\2020_FEBRUARY.json'
		def root = new JsonSlurper().parse(new File(file))
		List visits = root['timelineObjects']['placeVisit']
		def matches = []
		def last = null
		(0..visits.size()-1).each {
			def keyword = "Stephen"
			def isInAddr = visits[it]?.location?.address?.contains(keyword)
			def isInName = visits[it]?.location?.name?.contains(keyword)
			if (isInAddr || isInName) {
				matches << it
				def zeroEpoch = Calendar.getInstance(TimeZone.getTimeZone('GMT+1'))
				def msec = "${visits[it]?.duration?.startTimestampMs}".toLong()
				zeroEpoch.setTimeInMillis(msec)
				def now = zeroEpoch.format("yyyy-MM-dd")
				if (now != last) {
					println zeroEpoch.format("yyyy-MM-dd HH:mm") + " ($msec)"
					last = now
				}
			}
		}

		println "Found ${visits.size()} visits"
		println "Found ${matches.size()} matches"
	}

	@Ignore
	@Test
	public void testShareRateIsLowerThanAllBuyRates() {
		def file = new File('O:\\OneDrive\\Projects\\Groovy\\OrderStatementParser\\summaries\\Summary.xml')
		def root = new groovy.xml.XmlSlurper(false, false).parse(file)
		root.'**'.findAll { NodeChild node -> node.name() == 'share' && node['@rate'] != '0.0' }.each { NodeChild node ->
			println "-" * 80
			println "Share-Rate: ${node['@rate']}"
			node.'**'.findAll { NodeChild child -> child.name() == 'buy' }.each { NodeChild sub ->
				println "    Buy-Rate: ${sub['@rate']}"
			}
		}
	}

	def Double getWinLossByXml(String indicator) {
		def root = new groovy.xml.XmlSlurper(false, false).parse(new File('./summaries/Summary.xml'))
		def nodes = root.'**'.findAll {
			//noinspection GrUnresolvedAccess
			it.'@date'.text().startsWith(indicator)
		}

		def sumXmlTrans = nodes.sum {
			//noinspection GrUnresolvedAccess
			it.'@winloss'?.toDouble() ?: 0
		}

		println "$indicator-Trans in XML: ${sumXmlTrans}"
		println "-" * 80

		return sumXmlTrans
	}

	def Double getWinLossByParser(String indicator) {
		def shares = new Main().runParserConfigs() as List<Share>
		def trans = shares.collectMany { it.transactions.findAll {
			it.dateAsStr.startsWith(indicator) && it.isDivKnoSell()
		}}

		def sumShareTrans = (trans.sum { (it.winlossFifo) } as Double).round(2)
		println "$indicator-Trans in Shares: $sumShareTrans"
		return sumShareTrans
	}

	@Test
	public void testSingleFileParsing1() {
		def file = 'O:/OneDrive/Documents/Finance/FinanzenNetZero/2021/2021-11-10_1165616_01_0__6244811_wertpapier-ereignis-swift-faehig_169154860_20211110_4475347.pdf'
		def trans = Transaction.fromPdfTest(new File(file))
		println trans
	}

	@Test
	public void testSingleFileParsing2() {
		def file = 'O:\\OneDrive\\Documents\\Finance\\Trade Republic\\orders\\2021\\pb161190676692211999405661016642-kosten.pdf'
		def trans = Transaction.fromPdfTest(new File(file))
		println trans
	}

	@Test
	public void testSingleFileParsing3() {
		def buy = 'O:/OneDrive/Documents/Finance/Trade Republic/orders/2020/pb15834032886671033634078567699.pdf'
		def sell = 'O:/OneDrive/Documents/Finance/Trade Republic/orders/2020/pb15841392501551769595566933345.pdf'
		def buytrans = Transaction.fromPdfTest(new File(buy))
		def selltrans = Transaction.fromPdfTest(new File(sell))
		def share = new Share([buytrans, selltrans])
		/**
		 * Stck 35, Rate 0,04 €
		 * Kapitalertragssteueroptimierung 7,42 €
		 * Soli Optimierung 0,40 €
		 * Gesamt:7,82 €
		 *
		 * WinLoss: 1,40 €
		 * Tax: 7,82 €
		 * Total: 7,86 €
		 */
		selltrans.printInfo()
		println "$selltrans"

		assert share.openTotal == 0.0
		assert share.openAmount == null
		assert share.tax == 7.82
		assert share.winloss == -29.70 + 0.04
		assert share.invested == -29.70
		assert share.rate == 0.0
		assert share.amount == 0.0

		assert selltrans.fee == 0.0
		assert selltrans.tax == 7.82
		assert selltrans.winlossFifo.round(2) == share.winloss.round(2)
		assert selltrans.rate.round(4) == 0.0011
		assert selltrans.amount == 35.0
	}

	@Test
	public void testSingleFileParsing4() {
//		def tax = 'O:\\OneDrive\\Documents\\Finance\\Trade Republic\\orders\\2022\\pb16527412334881489093035085189.pdf'
		def tax = 'O:/OneDrive/Documents/Finance/ScalableCapital/2022/1844287_01_0__108583036_security-settlement_250841485_20220526.pdf'
		def taxtrans = Transaction.fromPdfTest(new File(tax))
		def share = new Share([taxtrans])
		/**
		 * Stck 35, Rate 0,04 €
		 * Kapitalertragssteueroptimierung 7,42 €
		 * Soli Optimierung 0,40 €
		 * Gesamt:7,82 €
		 *
		 * WinLoss: 1,40 €
		 * Tax: 7,82 €
		 * Total: 7,86 €
		 */
		taxtrans.printInfo()
		println "$taxtrans"

		assert taxtrans.fee == 0
		assert taxtrans.tax == 0
		assert taxtrans.amount == 21.713
		// Todo is minus richtig??
		assert taxtrans.total == -200
		assert taxtrans.dateAsStr == "2022-05-25"
	}

	@Test
	public void testSingleFileParsing5() {
		def tax = 'O:/OneDrive/Documents/Finance/Trade Republic/orders/2020/pb16079755438608069492192268904.pdf'
		def taxtrans = Transaction.fromPdfTest(new File(tax))
		def share = new Share([taxtrans])
		/**
		 POSITION ANZAHL KURS BETRAG
		 Xiaomi Corp.
		 Registered Shares Cl.B o.N.
		 ISIN: KYG9830T1067
		 200 Stk. 3,04 EUR 607,60 EUR
		 GESAMT 607,60 EUR
		 ABRECHNUNG
		 POSITION BETRAG
		 Fremdkostenzuschlag -1,00 EUR
		 Kapitalertragssteuer -12,37 EUR
		 Solidaritätszuschlag -0,68 EUR
		 GESAMT 593,55 EUR
		 */
		taxtrans.printInfo()
		println "$taxtrans"

		assert taxtrans.fee == -1
		assert taxtrans.tax == -12.37 - 0.68
		assert taxtrans.amount == 200
		assert taxtrans.rate == 3.04
		assert taxtrans.total == 593.55
	}

	@Test
	public void testSummary2019() {
		Main.printout = false

		def sumShareTrans = getWinLossByParser('2019')
		def sumXmlTrans = getWinLossByXml('2019')

		assert sumShareTrans == 11943.55
		assert sumXmlTrans == 11943.55
	}


	@Test
	public void testSummary2018() {
		Main.printout = false
		def shares = new Main().runParserConfigs() as List<Share>
		def trans = shares.collectMany { it.transactions.findAll { it.dateAsStr.startsWith('2018') }}
		def sum = (trans.sum { (it.tax + it.fee) } as Double).round(2)
		assert "$sum" == "-1281.75"

		Double winloss = trans.findAll { it.isDiv() || it.isSell() }.sum { it.winlossFifo }
		assert "${winloss.round(2)}" == "3152.78"
	}


	@Test
	public void testRateCalulation() {
		def share = new Share('DE000NG0N1Z4')
		share.addTransactions([getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.BUY, 7000, 0.15, [-10.0], '2019-01-01')])
		def openAmount = 7000
		def openTotal = -7000 * 0.15 - 10 // -1060
		assert share.openAmount == openAmount
		assert share.openTotal == openTotal // -1060
		assert share.rate.round(5) == (1060/7000).round(5).toDouble() // +0.15143

		share.addTransactions([getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.BUY, 3000, 0.30, [-10.0], '2019-02-02')])
		openAmount += 3000
		openTotal += -3000 * 0.30 - 10
		assert share.openAmount == openAmount
		assert share.openTotal == openTotal // -1060 -910
		assert share.rate.round(5) == (Math.abs(openTotal) / openAmount).round(5).toDouble()

		share.addTransactions([getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.SELL, 1000, 0.35, [-5.0], '2019-03-03')])
		// openAmount -= 1000
		openTotal += 1000 * 0.35 - 5
		assert share.winloss == 193.57 // FIFO via openAmount
		assert share.openAmount == 9000
		assert share.openTotal.round(2) == openTotal - share.winloss // -1060 -910 -193.57 = openTotal without Winloss!!
		assert share.rate.round(5) == 0.197.toDouble()
	}

	@Test
	public void testMultipleLeftOverAmountsForBuyAndSell() {
		def transactions = [] as List<Transaction>
		transactions << getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.BUY, 7000, 0.15, [0.0], '2014-05-21')
		transactions << getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.BUY, 9200, 0.11, [0.0], '2014-05-22')
		transactions << getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.BUY, 9200, 0.12, [0.0], '2014-05-22')
		transactions << getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.SELL, 9200, 0.12, [-9.9], '2014-05-22')
		transactions << getTestTrans('DE000NG0N1Z4', Transaction.TransTypes.SELL, 16200, 0.13, [-9.9], '2014-05-22')

		transactions*.printInfo()
		println "*" * 65
		def summary = new SharesSummary([new Share(transactions)])
		transactions*.printInfo()

		summary.printSummaryShares(false)

		assert summary.shares.first().transactions.sum { it.openAmount } == 0
		assert summary.calcWinLoss(false) == '24,20 €'
		assert summary.calcOpenInvested(false) == '0,00 €'
	}

	@Test
	public void testSingleIsin() {
		// final isin = 'DE000A0JKHC9'
		/*
		final isin = 'DE000NG00K05'
		def trans = transactions.findAll { it.isin == isin }
		def summary = new SharesSummary([new Share(trans)]).printSummaryShares()
		assert summary.calcWinLoss(false) == '29,47 €'
		def transactions = Main.parseFolders(ParserConfig.getConfigs())
		 */
	}

	@Test
	public void testSingleFile() {
		def path = 'O:/OneDrive/Documents/Finance/FinanzenNetZero/2021/2021-10-06_1165616_01_0__57561634_wertpapierabrechnung_155198568_20211006_3941290.pdf'
//		def path = 'O:/OneDrive/Documents/Finance/Trade Republic/orders/2021/pb16330162428841907477586594889.pdf'
//		def path = 'O:/OneDrive/Documents/Finance/ING DiBa/Depot'
//		Transaction.fromPdfTest(new File("$pathTr/2019/pb156348528178714036823142690393.pdf"))
//		Transaction.fromPdfTest(new File("$pathTr/2019/pb15701809326799221368812102106.pdf"))
//		Transaction.fromPdfTest(new File("$pathIn/2020/Direkt_Depot_8004195622_Abrechnung_Kauf_DE000A2AHL75_Order_155609915_001_20200107.pdf"))
		println Transaction.fromPdfTest(new File("$path"))
	}

	@Test
	public void summaryTest_LeftOverBuy() {
		def trans = []
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10.0, [-3])
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 15, 10.0, [-3])
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 20, 20.0, [-1])

		def summary = new SharesSummary([new Share(trans)])
		assert summary.calcTaxesAndFees(false) == '-7,00 €'
		assert summary.calcOpenInvested(false) == '-51,00 €'
		assert summary.calcWinLoss(false) == '194,00 €'
	}

	@Test
	public void summaryTest_BuyGreater() {
		def trans = []
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 15, 10.0, [-3])
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 5, 10.0, [-3])
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 20, 20.0, [-1])

		def summary = new SharesSummary([new Share(trans)])
		assert summary.calcTaxesAndFees(false) == '-7,00 €'
		assert summary.calcOpenInvested(false) == '0,00 €'
		assert summary.calcWinLoss(false) == '193,00 €'
	}

	@Test
	public void summaryTest_BuyTransAmountGreater() {
		def trans = []
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 20, 10.0, [-6])
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 10, 20.0, [-1])

		def summary = new SharesSummary([new Share(trans)])
		assert summary.calcTaxesAndFees(false) == '-7,00 €'
		assert summary.calcOpenInvested(false) == '-103,00 €'
		assert summary.calcWinLoss(false) == '96,00 €'
		assert trans.find { it.buy }.openAmount == 10
	}

	@Test
	public void summaryTest_BuyEqual() {
		def trans = []
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10.0, [-3])  // -103
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10.0, [-3])  // -103
		trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 10, 20.0, [-1]) // +199

		def summary = new SharesSummary([new Share(trans)])
		assert summary.calcTaxesAndFees(false) == '-7,00 €'
		assert summary.calcInvested(false) == '-206,00 €'
		assert summary.calcOpenInvested(false) == '-103,00 €'
		assert summary.calcWinLoss(false) == '96,00 €'
		assert trans.find { it.isBuy() && it.openAmount == 10 }
		assert trans.find { it.isBuy() && it.openAmount == 0 }
	}

	public static class TransactionTests {
		@Test
		public void oneBuyTrans() {
			def trans = []
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 3, 123, [-1])

			def summary = new SharesSummary([new Share(trans)])
			assert summary.calcTaxesAndFees(false) == '-1,00 €'
			assert summary.calcOpenInvested(false) == '-370,00 €'
		}

		@Test
		public void twoBuyTrans() {
			def trans = []
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 3, 123, [-1])
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 3, 123, [-1])

			def summary = new SharesSummary([new Share(trans)])
			assert summary.calcTaxesAndFees(false) == '-2,00 €'
			assert summary.calcOpenInvested(false) == '-740,00 €'
		}

		@Test
		public void oneBuyAndDiv() {
			def trans = []
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 3, 123, [-1])
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.DIV, 1, 1, [0])

			def summary = new SharesSummary([new Share(trans)])
			assert summary.calcTaxesAndFees(false) == '-1,00 €'
			assert summary.calcOpenInvested(false) == '-370,00 €'
			assert summary.calcWinLoss(false) == '1,00 €'
		}

		@Test
		public void oneBuyAndFullSell() {
			def trans = []
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10, [-1])
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 10, 11, [-1])

			def summary = new SharesSummary([new Share(trans)])
			assert summary.calcTaxesAndFees(false) == '-2,00 €'
			assert summary.calcOpenInvested(false) == '0,00 €'
			assert summary.calcWinLoss(false) == '8,00 €'
		}


		@Test
		public void oneBuyAndFullSellAndNewBuyAndFullSell() {
			def trans = []
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10, [-1]).setDate("2020-01-01")
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 10, 11, [-1]).setDate("2020-01-02")

			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10, [-1]).setDate("2020-01-03")
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 10, 11, [-1]).setDate("2020-01-04")

			new SharesSummary([new Share(trans)]).with {
				assert calcTaxesAndFees(false) == '-4,00 €'
				assert calcOpenInvested(false) == '0,00 €'
				assert calcWinLoss(false) == '16,00 €'
			}
		}

		@Test
		public void oneBuyAndHalfSell() {
			def trans = []
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.BUY, 10, 10, [-1])
			trans << getTestTrans("DE1234567890", Transaction.TransTypes.SELL, 5, 11, [-1])
			def summary = new SharesSummary([new Share(trans)])
			assert summary.calcTaxesAndFees(false) == '-2,00 €'
			assert summary.calcOpenInvested(false) == '-50,50 €'
			assert summary.calcWinLoss(false) == '3,50 €'
			// BUY: 	10x-10-1 = 101 € Invested = 10x -10,10
			// SELL: 	 5x11-1 = 54 € Sell => 5x +10,8
			// Invested: 50,50 = -101 / 10 * 5
			// WinLoss: 10,10-10,8 * 5
		}
	}

	static transCounter = 1

	private static Transaction getTestTrans(String isin, Transaction.TransTypes transType, double amount, double rate, List<Double> taxes) {
		def rand = Utils.getRandomInt(1111, 9999)
		def transaction = new Transaction(type: transType, name: "TestTrans$transType$rand", id: "DE123#$rand", isin: isin)
		transaction.parserType = OrderParserType.TRADEREPUBLIC
		transaction.addTax(taxes.sum().toString())
		transaction.amount = amount
		transaction.rate = rate
		transaction.date = LocalDate.now().format('dd.MM.yyyy')
		transaction.setId("#${transCounter++}")
		def sign = (transaction.isBuy()) ? -1 : 1
		//noinspection GroovyOverlyComplexArithmeticExpression
		transaction.setTotal(rate * sign * amount + transaction.tax, false)
		OrderParser orderParser = transaction.parserType.parser()
		return orderParser.validate(this, "")
	}

	@SuppressWarnings("GroovyMethodParameterCount")
	private static Transaction getTestTrans(String isin, Transaction.TransTypes transType, double amount, double rate, List<Double> taxes, String date) {
		def transaction = getTestTrans(isin, transType, amount, rate, taxes)
		transaction.setDate(date)
		return transaction
	}
}
