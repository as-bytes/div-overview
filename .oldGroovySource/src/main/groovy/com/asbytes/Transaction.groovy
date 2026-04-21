package com.asbytes

import com.asbytes.helpers.PdfParser
import com.asbytes.helpers.Utils
import com.asbytes.parser.OrderParser
import com.asbytes.parser.OrderParserConfig
import com.asbytes.parser.OrderParserType

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.time.LocalDateTime
import java.util.regex.Matcher

@SuppressWarnings('unused')
@XmlRootElement(name="Transaction")
@XmlAccessorType(XmlAccessType.NONE)
public class Transaction implements Serializable {
	@SuppressWarnings(['unused', 'GroovyConstantNamingConvention'])
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	def String id

	def hasBuyTransAgeUnder365Days = false

	def static storeDataVersion = 1
	def sqlDataVersion = Transaction.storeDataVersion

	Transaction.Currency currency

	Boolean crypto = null

	static boolean isUpdatedSmartBrokerStatus(Transaction existing, Transaction newer) {
		def newIsDone = false
		def notSameState = existing.getSmartBrokerStatus() != newer.getSmartBrokerStatus()
		if (notSameState) {
			newIsDone = newer.getSmartBrokerStatus() == Transaction.SmartBrokerStatus.CLEARED
		}
		return newIsDone
	}

	def String getRelativePath() {
		return Transaction.getRelativePath(this.file)
	}

	static String getRelativePath(File file) {
		def newPath = file.absolutePath
		OrderParserConfig.configDocFinFolders.each {
			newPath = newPath.replace(new File(it).absolutePath, '.')
		}
		return newPath
	}

	@XmlAttribute(name = 'currency')
	def String getCurrencySymbol() {
		return currency?.symbol ?: '€';
	}

	// TODO: def LocalDate date
	def LocalDateTime date

	@XmlAttribute
	public String getCalenderWeek() {
		return this.date.format("w")
	}

	@XmlAttribute(name = 'date')
	def String getDateAsStr() {
		return getDateAsStr("yyyy-MM-dd HH:mm")
	}

	def String getDateAsStr(String format) {
		return this.date.format(format)
	}
	def Transaction setDate(String date) {
		if (date == null) {
			throw new Exception('Date is null for: ' + this.toString())
		}

		def patterns = [] as List<Matcher>
		if (date.length() > 10) {
			patterns << (date =~ /(?<dd>\d\d)\.(?<mm>\d\d)\.(?<yy>\d\d\d\d) (?<HH>\d\d):(?<MM>\d\d)/)
			patterns << (date =~ /(?<yy>\d\d\d\d)-(?<mm>\d\d)-(?<dd>\d\d) (?<HH>\d\d):(?<MM>\d\d)/)
			patterns << (date =~ /(?<dd>\d\d)-(?<mm>\d\d)-(?<yy>\d\d\d\d) (?<HH>\d\d):(?<MM>\d\d)/)
			patterns << (date =~ /(?<dd>\d\d)\.(?<mm>\d\d)\.(?<yy>\d\d) (?<HH>\d\d)\.(?<MM>\d\d)/)
			// 2021-04-22T12:28:24+02:00
			patterns << (date =~ /(?<yy>\d\d\d\d)-(?<mm>\d\d)-(?<dd>\d\d)T(?<HH>\d\d):(?<MM>\d\d)/)
		} else if (date.length() == 10) {
			patterns << (date =~ /(?<dd>\d\d)\.(?<mm>\d\d)\.(?<yy>\d\d\d\d)/)
			patterns << (date =~ /(?<yy>\d\d\d\d)-(?<mm>\d\d)-(?<dd>\d\d)/)
			patterns << (date =~ /(?<dd>\d\d)-(?<mm>\d\d)-(?<yy>\d\d\d\d)/)
		} else if (date.length() == 8) {
			patterns << (date =~ /(?<yy>\d\d\d\d)(?<mm>\d\d)(?<dd>\d\d)/)
			patterns << (date =~ /(?<dd>\d\d)\.(?<mm>\d\d)\.(?<yy>\d\d)/)
		}

//		if (date.length() == 8) {
//			this.date = LocalDate.parse("${date}", DateTimeFormatter.ofPattern("dd.MM.yy")).atStartOfDay()
//		} else {
		def match = patterns.find { it.find() }
		if (match?.groupCount() == 3) {
			this.date = LocalDateTime.of(match.group('yy').toInteger(), match.group('mm').toInteger(), match.group('dd').toInteger(),
					0, 0, 0)
		} else if (match?.groupCount() == 5) {
			def year = match.group('yy').toInteger();
			if (year < 100) {
				year += 2000;
			}
			this.date = LocalDateTime.of(year, match.group('mm').toInteger(), match.group('dd').toInteger(),
					match.group('HH').toInteger(), match.group('MM').toInteger(), 0)
		}
//		}

		assert this.date: "Date parser error for $date"
		return this
	}

	@XmlAttribute
	def OrderParserType parserType

	def String getFileName() {
		return this.file?.name
	}
	def File file
	def String isin
	def String wkn

	def String newWknOrIsin
	def String name = null

	def boolean overridden = false

	private temporaryStatement = false
	public  boolean getTemporaryStatement() {
		return this.temporaryStatement
	}
	public setTemporaryStatement(boolean state) {
		this.temporaryStatement = state
	}
	public isTemporaryStatement() {
		return this.temporaryStatement == true
	}

	def spinOff = false
	def TransTypes type
	// only buy or premptive?!
	def Double openAmount
	@XmlAttribute(name = 'openAmount')
	// @XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	public Double getOpenAmountAttr() {
		return ((isBuy() || isPreEmptive()) && openAmount > 0) ? openAmount : null
	}

	@XmlAttribute(name = 'fee')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	public Double fee = 0.00d

	public boolean isFeeType() {
		return type == TransTypes.FEE
	}

	@XmlAttribute(name = 'interest')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	public Double interest = 0.00d

	public boolean isInterest() {
		return type == TransTypes.INTEREST
	}

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double total = 0.00d

	Double winlossFifo = 0.00d
	@XmlAttribute(name = 'winloss')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double getWinlossAttr() {
		return (winlossFifo == 0.00d) ? null : winlossFifo
	}

	Transaction calcTaxByWinlossFifo() {
		def kapErgSt = this.winlossFifo * 0.25
		def soli = kapErgSt * 5.5
		this.addTax(kapErgSt + soli)
		this.winlossFifo -= this.tax
		this.winlossAvg -= this.tax
		this.total -= this.tax
		return this
	}

	Double winlossAvg = 0.00d
	@XmlAttribute(name = 'winlossAvg')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double getWinlossAvgAttr() {
		return (winlossAvg == 0.00d) ? null : winlossAvg
	}

	@XmlAttribute(name = 'winlossNoTax')
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double getWinlossNoTaxAttr() {
		return (winlossFifo == 0.00d) ? null : (winlossFifo - tax)
	}

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	Double amount
	def void setAmount(String val) {
		// todo fee could be part of transaction, should validate val!
		def newVal = (isDiv() || isInterest() || isTaxes() || isFeeType()) ? 1 : val;

		if (isTradeRepublic() && isDiv()) {
			setAmount(Math.abs(Utils.toDouble(val)))
		} else {
			setAmount(Math.abs(Utils.toDouble(newVal)))
		}
	}

	def void setAmount(Double dbl) {
		this.amount = dbl
		this.openAmount = dbl
	}

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.RateAdapter)
	Double rate
	def void setRate(String val) {
		this.rate = Utils.toDouble(val) ?: 0d
	}
	def void setRate(Double dbl) { this.rate = dbl }

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	private Double tax = 0.00d
	def void addTax(String val, boolean switchSign = false) {
		addTax(Utils.toDouble(val), switchSign)
	}

	def void addTax(Double val, boolean switchSign = false) {
		this.tax += (val) ? val * ((switchSign) ? -1 : 1) : 0d
	}

	def Double getTax() {
		this.tax
	}

	def Double setTax(double tax) {
		this.tax = tax;
	}

	public Transaction() {
	}

	Transaction(IniTransaction transaction) {
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

	public Transaction(OrderParserType parserType, File file) {
		this.file = file
		this.parserType = parserType
	}

	public Transaction(OrderParserType parserType) {
		this.parserType = parserType
	}

	public isQuirion() {
		return this.parserType == OrderParserType.QUIRION
	}

	public isFinZeroCsv() {
		return this.parserType == OrderParserType.FINNETZERO_CSV
	}

	public isDegiro() {
		return this.parserType == OrderParserType.DEGIRO
	}

	public isSmartbroker() {
		return this.parserType == OrderParserType.SMARTBROKER
	}

	public isScalable() {
		return this.parserType == OrderParserType.SCALABLE
	}

	public isBitPanda() {
		return this.parserType == OrderParserType.BITPANDA
	}

	public isTradeRepublic() {
		return this.parserType == OrderParserType.TRADEREPUBLIC
	}

	public void printInfo() {
		print "$id ${type.toString().padLeft(4)}"
		print ", Amount: ${openAmount.toString().padLeft(8)} / ${amount.toString().padLeft(8)} x ${Utils.toCurrency(getRateFromTotal())}"
		print ", WinLoss: ${Utils.toCurrency(winlossFifo)}"
		println(" ")
	}

	public enum Currency {
		AUD('A$'), EUR('€'), CAD('C$'), USD('$'), NOK('NK'), CHF('SFr'), GBP('£'), SGD('S$'), HKD('H$'), JPY('¥'), UNKOWN('?')
		def symbol
		private Currency(String symbol) {
			this.symbol = symbol;
		}
		public static Currency fromString(String val) {
			return (Currency.values().find { "$it" == "$val" }) ?: UNKOWN
		}
	}

	static Transaction fromPdfTest(File pdf) {
		def docContent = new PdfParser(pdf).getPdfText()

		println "#" * 30 + " PDF-TEST" + "#" * 30
		println docContent;
		println "#" * (30 + " PDF-TEST".length() + 30)

		def parserType
		if (docContent.contains('ING-DiBa')) {
			parserType = OrderParserType.INGDIBA
		} else if (docContent.contains('Trade Republic')) {
			parserType = OrderParserType.TRADEREPUBLIC
		} else if (docContent.contains('finanzen.net zero')) {
			parserType = OrderParserType.FINNETZERO_PDF
		} else if (docContent.contains('Scalable Capital GmbH')) {
			parserType = OrderParserType.SCALABLE
		} else {
			throw new IllegalStateException("Could not determine OrderParser Type for ${pdf.absolutePath}")
		}
		return parserType.createTransactionFromPdfContent(pdf).first()
	}

	public float getRateFromTotal(boolean withTaxes = true) {
		def taxes = (withTaxes) ? 0 : this.tax + this.fee
		def totalWithoutFees = this.total + taxes * -1
		return (this.amount) ? Math.abs(totalWithoutFees / this.amount).round(8) : 0
	}

	def void addFee(val, boolean switchSign = false) {
		if (val != null) {
			Double num = (val instanceof Double) ? val : Utils.toDouble("$val".replace(' ', '')) ?: 0d
			fee += (switchSign) ? num * -1d : num
		}
	}

	def void addInterest(val, boolean switchSign = false) {
		if (val != null) {
			Double num = (val instanceof Double) ? val : Utils.toDouble("$val".replace(' ', '')) ?: 0d
			this.interest += (switchSign) ? num * -1d : num
		}
	}

	def void addTotal(val, boolean switchSign = false) {
		if (val != null) {
			Double num = (val instanceof Double) ? val : Utils.toDouble("$val".replace(' ', '')) ?: 0d
			this.total += (switchSign) ? num * -1d : num
		}
	}

	def void setTotal(val, boolean switchSignsForBuys = true) {
		def num = Utils.toDouble(val)
		this.total = ((isBuy() || isTaxes()) && switchSignsForBuys) ? num * -1d : num
	}

	static Transaction fromBlob(byte[] blob) {
		try {
			new ByteArrayInputStream(blob).withCloseable {bis ->
				new ObjectInputStream(bis).withCloseable { inputStream ->
					return inputStream.readObject() as Transaction
				}
			}
		} catch(ignored) {
			return null
		}
	}

	byte[] toBlob() {
		new ByteArrayOutputStream().withCloseable {bos ->
			new ObjectOutputStream(bos).withCloseable { out ->
				out.writeObject(this)
				out.flush()
				return bos.toByteArray()
			}
		}
	}

	def boolean isDivKnoSell() {
		return isDiv() || isKnockout() || isSell()
	}

	def boolean isDiv() {
		return type == TransTypes.DIV
	}

	def boolean isPreEmptive() {
		return type == TransTypes.PREEMPTIVE
	}

	def boolean isBuy() {
		return type == TransTypes.BUY
	}

	def boolean isIsinSwitch() {
		return type == TransTypes.ISINSWITCHIN || type == TransTypes.ISINSWITCHOUT
	}

	def boolean isSplit() {
		return type == TransTypes.SPLIT || type == TransTypes.SPLITSELL || type == TransTypes.SPLITBUY
	}

	def boolean isSell() {
		return type == TransTypes.SELL
	}

	def boolean isTaxes() {
		return type == TransTypes.TAX
	}

	def boolean isCapital() {
		return type == TransTypes.CAPITAL
	}

	def boolean isKnockout() {
		return type == TransTypes.KNO
	}

	def boolean isEUR() {
		return currency == Currency.EUR
	}
/*
	def Transaction overwrite(fileName) {
		def fix = this.overwrite[fileName]
		if (fix) {
			fix(this)
		}
		return this
	}
*/
	public enum TransTypes {
		BUY, SELL, DIV, KNO, TAX, CAPITAL, SPLIT, SPLITBUY, SPLITSELL, UNKOWN, PREEMPTIVE, UNSUPPORTED, ISINSWITCH, ISINSWITCHIN, ISINSWITCHOUT, INTEREST, FEE
	}

	public Double getTotalFromRate(int precision = 2) {
		double sign = ((isBuy() || isTaxes()) && !isTemporaryStatement()) ? -1 : 1
		double price = amount * rate * sign + tax + interest
		double calc = 0.0d
		try {
			calc = (Utils.roundUp(price, precision) + fee).round(precision)
		} catch(Exception exp) {
			println exp
		}

		/* FIX für ING DiBa\Depot\2019\Direkt_Depot_8004195622_Ertragsabrechnung_US3696041033_20190417.pdf
			Nachträgliche Berechnung der Quellensteuer (also Dividende, aber Minus -.-)
		 */
		if (isDiv() && total < 0 && calc > 0) {
			calc *= -1
		}

		return calc
	}

	@Override
	public String toString() {
		return "[$type@${parserType.shortName}] $isin ($name) $date\n[Amount $amount x Rate $rate ($currency) + Tax $tax + Fee $fee + Interest $interest = $total]\nfrom ${file?.absolutePath}\n$id${this.spinOff ? ' SPIN-OFF': ''}\n"
	}

	public String toStringOneLine() {
		return "[$type@${parserType.shortName}] $isin ($name) $date\n[Amount $amount x Rate $rate ($currency) + Tax $tax + Fee $fee + Interest $interest = $total]"
	}

	String createHash() {
		return "${this.isin}:${this.amount}_${this.rate}_${this.total}_${this.getDateAsStr("yy-MM HH:mm")}-${this.deGiroSaldo}_${this.getSmartBrokerStatus()}".toString()
	}

	def deGiroSaldo = 0d

	private SmartBrokerStatus smartBrokerStatus = null
	public SmartBrokerStatus getSmartBrokerStatus() {
		return this.smartBrokerStatus
	}
	public void setSmartBrokerStatus(String status) {
		this.smartBrokerStatus = SmartBrokerStatus.values().find { sbStatus ->
			return sbStatus.getStates().contains(status.toLowerCase())
		}
	}
	public void setSmartBrokerStatus(SmartBrokerStatus status) {
		this.smartBrokerStatus = status
	}
	public enum SmartBrokerStatus {
		EXECUTED("ausgeführt,teilausführung"), CLEARED("abgerechnet")
		def String[] states
		SmartBrokerStatus(String states) {
			this.states = states.split(',')
		}
	}
}
