package com.asbytes.parser


import com.asbytes.Transaction
import com.asbytes.helpers.PdfParser
import groovy.transform.InheritConstructors

@InheritConstructors
class ParserVivid extends OrderParser {

	int fieldCount = 16
	int idxDate = 0
	int idxTime = 1
	int idxTicker = 2
	int idxType = 3
	int idxAmount = 4
	int idxRate = 5
	int idxTotal = 7
	int idxFees = 9

	final String separator = ' '

	@Override
	List<Transaction> parseCsv(File pdf, int firstLine) {
		def lines = new PdfParser(pdf).getPdfText().split('\n')
		def startIndex = lines.findIndexOf { it.startsWith('Währungsumr') } + 1
		def endIndex = lines.findIndexOf { it.startsWith('Ausgegeben am ') }

		def csvLines = lines.toList().subList(startIndex, endIndex).collect { it.split(separator) }
		return parseCsvLines(csvLines, pdf)
	}

	List<Transaction> parseCsvLines(List<String[]> csvLines, File file) {
		def transactions = [] as List<Transaction>
		def dataLines = csvLines.findAll {it.length == fieldCount };
		def tickers = dataLines.collect {it[idxTicker] }.unique()
		tickers.findAll { it != "" }.each { ticker ->
			def tickerLines = dataLines.findAll {line -> line[idxTicker] == ticker }
			def buys = tickerLines.findAll { line -> line[idxType].trim().equals('BUY') }
			def sells = tickerLines.findAll { line -> line[idxType].trim().equals('SELL') }

			def parseLine = { line, transType ->
				def transaction = new Transaction(OrderParserType.VIVID, file)
				transaction.setType(transType)
				transaction.setDate("${line[idxDate]} ${line[idxTime]}")
				transaction.setName(line[idxTicker])
				transaction.setIsin(line[idxTicker])
				transaction.setAmount(line[idxAmount])
				transaction.setTotal(line[idxTotal])
				transaction.setRate(line[idxRate])
				transaction.addFee(line[idxFees])

				transaction.setId(line[idxDate] + "@" + line[idxTime] + "#" + line[idxTicker])

				transactions << transaction
			}

			buys.each { parseLine(it, Transaction.TransTypes.BUY) }
			sells.each { parseLine(it, Transaction.TransTypes.SELL) }
		}


		return transactions
	}

	@Override
	Map<String, Closure> getOverwrites() {
		def overwrites = [:]
		return overwrites
	}

	@Override
	boolean isPreEmptiveRights(String content) {
		return false
	}

	@Override
	boolean isDividend(String content) {
		return false
	}

	boolean isBuy(String content) {
		throw new IllegalAccessError('Use parseCSV')
	}

	boolean isSell(String content) {
		throw new IllegalAccessError('Use parseCSV')
	}

	boolean isKnockout(String content) {
		return false
	}

	@Override
	boolean isIsinSwitch(String content) {
		return false
	}

	@Override
	void parseId(Transaction transaction, String content) {
	}

	@Override
	void parseDate(Transaction transaction, String content) {
	}

	@Override
	void parseAmount(Transaction transaction, String content) {
	}

	@Override
	void parseTotal(Transaction transaction, String content) {
	}

	@Override
	void parseFee(Transaction transaction, String content) {
	}

	@Override
	void parseTax(Transaction transaction, String content) {
	}

	@Override
	void parseIsin(Transaction transaction, String content) {
	}

	@Override
	void parseName(Transaction transaction, String content) {
	}

	@Override
	void parseRate(Transaction transaction, String content) {
	}
}
