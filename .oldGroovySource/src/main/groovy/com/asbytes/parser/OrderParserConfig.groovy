package com.asbytes.parser

import java.time.LocalDate

public class OrderParserConfig {
	static configDocFinFolders = [
		"O:/OneDrive/Documents/Finance",
		"G:/Meine Ablage/Documents/Finance",
	]

	static activeParsers = [
		 OrderParserType.SCALABLE,
		 OrderParserType.SCALABLE2,
		// OrderParserType.INGDIBA,
		OrderParserType.FINNETZERO_CSV,
		// OrderParserType.TRADEREPUBLIC,
		// OrderParserType.DEGIRO,
	]

	static EnumMap folders = new EnumMap<OrderParserType, Closure>(OrderParserType.class)

	static {
		def folder = configDocFinFolders.find { new File(it).exists() }

		// folderMap.put(OrderParserType.BISON, (year) -> [("${configDocFinFolder}/Crypto/Bison".toString()): 'Bison.csv'])
		// folderMap.put(OrderParserType.QUIRION, (year) -> [("${configDocFinFolder}/Quirion/$year".toString()): '*.tsv'])
		// folders.put(OrderParserType.INGDIBA,        (year) -> [("${folder}/ING DiBa/Depot/$year".toString()): '*.pdf'])
		// folders.put(OrderParserType.FINNETZERO_PDF, (year) -> [("${folder}/FinanzenNetZero/$year".toString()): '*.pdf'])
		// folders.put(OrderParserType.TRADEREPUBLIC,	 (year) -> [("${folder}/Trade Republic/orders/$year".toString()): '*.pdf'])
		// folders.put(OrderParserType.DEGIRO, 		     () -> [("${folder}/DeGiro/".toString()): 'Account.csv'])
		folders.put(OrderParserType.SCALABLE,		     () -> [("${folder}/ScalableCapital/".toString()): 'ScalableBaader-Broker-Transactions.csv'])
		folders.put(OrderParserType.SCALABLE2,		     () -> [("${folder}/ScalableCapital/".toString()): 'ScalableCapital-Broker-Transactions.csv'])
		folders.put(OrderParserType.FINNETZERO_CSV, 	 () -> [("${folder}/FinanzenNetZero/".toString()): 'ZERO-kontoumsaetze*.csv'])
//			OrderParserType.SCALABLE_PDF, (year) -> [("${configDocFinFolder}/ScalableCapital/$year".toString()): '*security*.pdf'],
//			OrderParserType.SMARTBROKER, (year) -> ["${configDocFinFolder}/Smartbroker/$year".toString()): 'auftraege-alle.csv'],
//			OrderParserType.TRADEREPUBLIC, (year) -> ["${configDocFinFolder}/Trade Republic/orders/$year".toString()): '*.pdf'],
//			OrderParserType.VIVID, (year) -> ["${configDocFinFolder}/Vivid Money/$year".toString()): 'Trade Confirmation.pdf'],
//			OrderParserType.BITPANDA, (year) -> [("${configDocFinFolder}/Bitpanda/trades".toString()): 'bitpanda-trades.csv'],
//			OrderParserType.SCALABLE_PDF, (year) -> [("${configDocFinFolder}/ScalableCapital/$year".toString()): '*security*.pdf'],
//			OrderParserType.SCALABLE_RKK, (year) -> [("${configDocFinFolder}/ScalableCapital/$year/RKK".toString()): '*.CSV'],
//			OrderParserType.SCALABLE_WUR, (year) -> [("${configDocFinFolder}/ScalableCapital/$year/WUR".toString()): '*.CSV'],
	}

	public static List<OrderParserConfig> getConfigs() {
		def configs = []


		configs << addConfig(OrderParserType.BISON, null, true, 1)
		configs << addConfig(OrderParserType.QUIRION, 2020, true, 1)
		configs << addConfig(OrderParserType.SMARTBROKER, 2020, true, 1)
		configs << addConfig(OrderParserType.TRADEREPUBLIC, 2019, false, 0)
		configs << addConfig(OrderParserType.INGDIBA, 2011, false, 0)
		configs << addConfig(OrderParserType.VIVID, 2020, true, 0)
		configs << addConfig(OrderParserType.BITPANDA, 2011, true, 5)
		configs << addConfig(OrderParserType.FINNETZERO_PDF, 2021, false, 0)
//		configs << addConfig(OrderParserType.SCALABLE_RKK, 2020, true, 0)
//		configs << addConfig(OrderParserType.SCALABLE_WUR, 2020, true, 0)
//		configs << addConfig(OrderParserType.SCALABLE_PDF, 2020, true, 0)
		configs << addConfig(OrderParserType.DEGIRO, null, true, 1)
		configs << addConfig(OrderParserType.FINNETZERO_CSV, null, true, 1)
		configs << addConfig(OrderParserType.SCALABLE, null, true, 1)
		configs << addConfig(OrderParserType.SCALABLE2, null, true, 1)

		return configs.findAll { it != null }
	}

	OrderParserType parserType
	def folderMaps = [:]
	boolean singleTransFiles
	int firstLineOfCsv = 0

	private static addConfig(parserType, startYear, oneTransactionsFile, firstLine) {
		if (activeParsers.contains(parserType) && startYear) {
			def parser = new OrderParserConfig(parserType, oneTransactionsFile, firstLine)
			def minYear = startYear ?: LocalDate.now().year
			(minYear..LocalDate.now().year).each { year ->
				parser.folderMaps << folders.get(parserType)(year)
			}
			return parser
		} else if (activeParsers.contains(parserType)) {
			def parser = new OrderParserConfig(parserType, oneTransactionsFile, firstLine)
			parser.folderMaps << folders.get(parserType)()
			return parser
		} else {
			return null
		}
	}

	OrderParserConfig(OrderParserType parserType, boolean singleTransFiles = false, int firstLineOfCsv = 0) {
		this.parserType = parserType
		this.singleTransFiles = singleTransFiles
		this.firstLineOfCsv = firstLineOfCsv
	}
}
