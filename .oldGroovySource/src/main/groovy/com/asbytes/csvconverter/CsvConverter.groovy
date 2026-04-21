package com.asbytes.csvconverter

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher
import java.util.regex.Pattern

class CsvConverter {

	static testCsv = "O:\\OneDrive\\Documents\\Finance\\Crypto\\crypto.com\\crypto.com app\\crypto_transactions_record_20210827_222242.csv"

	static void main(String[] args) {
		run(new File(testCsv))
	}

	static run(csvFile) {
//		def csvConfig = getCsvConfig(csvFile)
		def csvConfig = debugSetCsvConfig("${testCsv}.config")
		parseCsvWithConfig(csvFile, csvConfig)
	}

	static parseCsvWithConfig(File csvFile, CsvConfig csvConfig) {
		int lineCount = 0
		def koinlyRows = [['Date', 'Type', 'Sent n', 'Sent x', 'Recv n', 'Recv x', 'Desc']]
		csvFile.eachLine({ currentLine ->
			def currentFields = "${currentLine} ".split(csvConfig.separator)
			if (lineCount++ > csvConfig.dataLineStart && currentFields.size() == csvConfig.columnCount) {
				def koinly = new Koinly()

				koinly.parseDateTime(currentFields, csvConfig)
				koinly.parseType(currentFields, csvConfig)
				koinly.parseSent(currentFields, csvConfig)

				koinlyRows << koinly
			}
		})

		koinlyRows.each {
			println(it.toString())
		}
	}

	static CsvConfig debugSetCsvConfig(String configPath) {
		def exampleConfig = new CsvConfig()
		exampleConfig.with {
			it.name = "crypto.com/app/crypto-transactions-record"
			it.wallet = "crypto.com/app/spot"
			it.separator = ','
			it.dataLineStart = 5
			it.columnCount = 10
			it.columns[CsvConfig.ColumnType.DATE].with {
				it.index = 0
				it.pattern = '(?<year>[0-9]{4})-(?<mon>[0-9]{2})-(?<day>[0-9]{2}) (?<hour>[0-9]{2}):(?<min>[0-9]{2}):(?<sec>[0-9]{2})'
			}
			it.types.put(CsvConfig.TransactionType.CASHBACK, new TransactionConfig(index: 1, pattern: "^Card Cashback"))
			it.types.put(CsvConfig.TransactionType.SENT, new TransactionConfig(index: 1, pattern: "^Supercharger Deposit"))
		}

		def json = JsonOutput.prettyPrint(JsonOutput.toJson(exampleConfig))
		new File(configPath).write(json)
		println "-" * 80
		println json
		println "-" * 80

		return exampleConfig
	}

	static getCsvConfig(File csv) {
		def configFile = new File("${csv.absolutePath}.config")
		ensure(configFile.exists(), "Current file has no config, expected ${configFile.absolutePath}")
		def configJson = new JsonSlurper().parse(configFile)
		return configJson
	}

	public static ensure(boolean expected, String msgOnFail) {
		if (!expected) {
			throw new Exception(msgOnFail)
		}
	}
}

