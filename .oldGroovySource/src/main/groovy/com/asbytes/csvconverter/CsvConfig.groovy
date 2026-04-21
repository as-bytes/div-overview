package com.asbytes.csvconverter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CsvConfig {
	String name
	String separator
	String wallet

	int columnCount
	int dataLineStart

	Map<ColumnType, ColumnConfig> columns = [:]

	Map<TransactionType, TransactionConfig> types = [:]

	CsvConfig() {
		columns.put(ColumnType.DATE, new ColumnConfig())
	}

	Map<ColumnType, ColumnConfig> getDateColumn() {
		return this.columns.get(ColumnType.DATE)
	}

	enum TransactionType {
		BUY, SELL, CASHBACK, REBATE, REWARD, UNKNOWN, SENT
	}

	enum ColumnType {
		DATE
	}
}

class ColumnConfig{
	int index
	String pattern
}

class TransactionConfig {
	int index
	String pattern
}

class Koinly {
	LocalDateTime dateTime
	CsvConfig.TransactionType type
	Asset sent
	Asset recv
	Asset networth
	String desc

	@Override
	String toString() {
		def pad = 20
		def data = [
				dateTime.dateTimeString.padLeft(pad),
				type.toString().padLeft(pad),
				(sent?.amount ?: "").toString().padLeft(pad),
				(sent?.name ?: "").toString().padLeft(pad),
				(recv?.amount ?: "").toString().padLeft(pad),
				(recv?.name ?: "").toString().padLeft(pad),
		]
		return data.join("")
	}

	void parseDateTime(String[] fields, CsvConfig config) {
		def column = config.columns.get(CsvConfig.ColumnType.DATE)
		def source = fields[column.index]

		def match = source =~ column.pattern
		CsvConverter.ensure(match.matches(), "no regex match for pattern ${match.pattern().toString()} on ${source}")

		def date = ['year', 'mon', 'day'].collect { match.group(it) }
		def time = ['hour', 'min', 'sec'].collect { match.group(it) }

		this.dateTime = LocalDateTime.parse("${date.join('-')} ${time.join(':')}", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
	}

	void parseType(String[] fields, CsvConfig config) {
		def definedTypes = config.types.keySet()
		def transactionType = CsvConfig.TransactionType.UNKNOWN
		definedTypes.each {
			def definedType = config.types.get(it)
			def source = fields[definedType.index]
			def match = source =~ definedType.pattern
			if(match.find()) {
				transactionType = it
			}
		}

		CsvConverter.ensure(transactionType !== CsvConfig.TransactionType.UNKNOWN, "no transaction type was found for: ${fields}")
		this.type = transactionType
	}

	void parseSent(String[] fields, CsvConfig config) {
		if (this.type == CsvConfig.TransactionType.CASHBACK) {
			this.sent = null
//			this.recv = new Asset(fields[config.columns[CsvConfig.ColumnType.]])
		} else {
			CsvConverter.ensure(false, "no type match to determine sent")
		}
		CsvConverter.ensure(transactionType !== CsvConfig.TransactionType.UNKNOWN, "no transaction type was found for: ${fields}")
		this.type = transactionType
	}


}

class Asset {
	String name
	long amount

	Asset(String name, long amount) {
		this.name = name
		this.amount = amount
	}
}
