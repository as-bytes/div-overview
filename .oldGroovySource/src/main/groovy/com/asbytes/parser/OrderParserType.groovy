package com.asbytes.parser

import com.asbytes.Transaction
import com.asbytes.helpers.PdfParser
import org.testng.SkipException

public enum OrderParserType {
	INGDIBA('ING-DiBa', 'Ing', { new ParserIngDiba(OrderParserType.INGDIBA) }),
	TRADEREPUBLIC('Trade Republic', 'TrR', { new ParserTradeRepublic(OrderParserType.TRADEREPUBLIC) }),
	SMARTBROKER('SmartBroker', 'SmB', { new ParserSmartBroker(OrderParserType.SMARTBROKER) }),
	DEGIRO('DeGiro', 'DeG', { new ParserDeGiro(OrderParserType.DEGIRO) }),
	QUIRION('Quirion', 'Qion', { new ParserQuirion(OrderParserType.QUIRION) }),
	BISON('Bison', 'Bis', { new ParserBison(OrderParserType.BISON) }),
	VIVID('VividMoney', 'Viv', { new ParserVivid(OrderParserType.VIVID) }),
	BITPANDA('Bitpanda', 'BitP', { new ParserBitpanda(OrderParserType.BITPANDA) }),
	FINNETZERO_PDF('FinanzenNetZeroPdf', 'FiNetP', { new ParserFinanzenZeroPdf(OrderParserType.FINNETZERO_PDF) }),
	FINNETZERO_CSV('FinanzenNetZeroCsv', 'FiNetC', { new ParserFinanzenZeroCsv(OrderParserType.FINNETZERO_CSV) }),
	SCALABLE2('ScalabeCapitalCSV', 'SC2csv', { new ParserScalableCapitalCsv(OrderParserType.SCALABLE2) }),
	SCALABLE('ScalabeBaaderCSV', 'SCcsv', { new ParserScalableCapitalCsv(OrderParserType.SCALABLE) }),

	// SCALABLE_RKK('ScalabeCapitalRKK', 'SCrkk', { new ParserScalableCapitalRkk(OrderParserType.SCALABLE_RKK) }),
	// SCALABLE_WUR('ScalabeCapitalWUR', 'SCwur', { new ParserScalableCapitalWur(OrderParserType.SCALABLE_WUR) }),
	// SCALABLE_PDF('ScalabeCapitalPDF', 'SCpdf', { new ParserScalableCapitalPdf(OrderParserType.SCALABLE_PDF) })

	final String identifier
	final String shortName
	def Closure<OrderParser> parser

	private OrderParserType(String identifier, String shortName, Closure<OrderParser> parser) {
		this.identifier = identifier
		this.shortName = shortName
		this.parser = parser
	}

	List<Transaction> createTransactionsFromCsv(File csv, firstLine = 0) {
		OrderParser parser = this.parser()
		return parser.parseCsv(csv, firstLine);
	}

	List<Transaction> createTransactionFromPdf(File pdf) {
		try {
//				def rootFolder = pdf.getParent()
//				def dataFile = new File("$rootFolder/transactions.v2.properties")
//				Properties properties = new Properties()
//				if (dataFile.exists()) {
//					dataFile.withInputStream {
//						properties.load(it)
//					}
//				}
//
//				def jaxb = JAXBContext.newInstance(IniTransaction.class)
//
//				 TODO if md5 changes, the a new data-entry will be added -> need to clear outdated entries?!
			def transactions = []
//				def currentChecksum = pdf.bytes.digest('MD5')
//				def data = properties.get(pdf.name + currentChecksum, "")
//
//				if (data) {
//					def source = new StreamSource(new java.io.StringReader(data))
//					def transaction  = (IniTransaction) jaxb.createUnmarshaller().unmarshal(source)
//					transactions.add(new Transaction(transaction))
//				} else {
			transactions.addAll(createTransactionFromPdfContent(pdf))
//					def noSplit = transactions.size() == 1
//					if (noSplit) {
//						def marsh = jaxb.createMarshaller()
//						marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
//						marsh.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
//						def xml = marsh.marshal(new IniTransaction(transactions.first()))
//						properties.put(pdf.name + currentChecksum, xml)
//						properties.store(dataFile.newOutputStream(), null)
//					}
//				}

			return transactions
		} catch (Exception exp) {
			println pdf.absolutePath
			println "threw $exp"
			if (exp.class !== SkipException.class) {
				throw new Exception(pdf.absolutePath, exp)
			}
		}

		return []
	}

	List<Transaction> createTransactionFromPdfContent(File pdf) {
		def docContent = new PdfParser(pdf).getPdfText()

		def parserPdf = this.parser() as OrderParser
		/** TODO
		 * variant 1 parse each time full content
		 * variant 2 go through each line and try parse everything
		 */
		def transaction = new Transaction(this, pdf)
		parserPdf.parseType(transaction, docContent)
		if (transaction.type == Transaction.TransTypes.UNSUPPORTED) {
			return []
		}
		if (transaction.type == Transaction.TransTypes.ISINSWITCH) {
			// todo
			// transaction = parser.handleIsinSwitch(docContent)
			throw new SkipException('ignored for now')
		} else {
			parserPdf.parseIsin(transaction, docContent)
		}

		parserPdf.parseDate(transaction, docContent)

		if (transaction.isSplit()) {
			throw new IllegalAccessError("split not implemented")
		} else {
			// todo parse first for debugging, remove after
			parserPdf.parseId(transaction, docContent)
			parserPdf.parseCurrency(transaction, docContent)
			parserPdf.parseAmount(transaction, docContent)
			parserPdf.parseTax(transaction, docContent)
			parserPdf.parseTotal(transaction, docContent)
			parserPdf.parseFee(transaction, docContent)
			parserPdf.parseName(transaction, docContent)
			// must occur after total to generate id for div/kno...
			parserPdf.parseId(transaction, docContent)

			// must occur last
			parserPdf.parseRate(transaction, docContent)

			def overwrite = parserPdf.getOverwrites().getOrDefault(transaction.id, null)
			if (overwrite) {
				overwrite(transaction)
				transaction.overridden = true
			}
		}

//			OrderParser orderParser = this.parser()
		return [parserPdf.validate(transaction)]
	}

	boolean isSmartBroker() {
		return this == OrderParserType.SMARTBROKER
	}

	def handleIsinSwitch(String content) {
		throw new IllegalAccessError('isin switch not implemented')
	}
}
