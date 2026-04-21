package com.asbytes

import com.asbytes.helpers.Utils
import com.asbytes.parser.OrderParser
import com.asbytes.parser.OrderParserType

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.time.LocalDateTime

@SuppressWarnings('unused')
@XmlRootElement(name="Transaction")
@XmlAccessorType(XmlAccessType.NONE)
public class IniTransaction implements Serializable {
	@SuppressWarnings(['unused', 'GroovyConstantNamingConvention'])
	private static final long serialVersionUID = 1L;

	@XmlAttribute(name = 'currency')
	Transaction.Currency currency

	@XmlAttribute(name = 'date')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.LocalDateTimeAdapter)
	def LocalDateTime date

	@XmlAttribute(name = 'parserType')
	def OrderParserType parserType

	@XmlAttribute(name = 'isin')
	def String isin
	@XmlAttribute(name = 'id')
	def String id

	@XmlAttribute(name = 'transType')
	def Transaction.TransTypes type

	@XmlAttribute(name = 'openAmount')
	def Double openAmount

	@XmlAttribute(name = 'fee')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double fee = 0.00d

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double total = 0.00d

	@XmlAttribute(name = 'winlossNoTax')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double winlossFifo = 0.00d

	@XmlAttribute(name = 'winlossAvg')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double winlossAvg = 0.00d

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double amount

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.RateAdapter)
	Double rate

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double tax = 0.00d

	@XmlAttribute
	File file

	IniTransaction() {
	}

	IniTransaction(Transaction transaction) {
		this.id = transaction.id
		this.fee = transaction.fee
		this.tax = transaction.tax
		this.date = transaction.date
		this.file = transaction.file
		this.isin = transaction.isin
		this.rate = transaction.rate
		this.type = transaction.type
		this.total = transaction.total
		this.amount = transaction.amount
		this.currency = transaction.currency
		this.parserType = transaction.parserType
		this.openAmount = transaction.openAmount
		this.winlossAvg = transaction.winlossAvg
		this.winlossFifo = transaction.winlossFifo
	}
}
