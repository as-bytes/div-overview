package com.asbytes

import com.asbytes.helpers.Utils
import com.asbytes.parser.OrderParser
import com.asbytes.parser.OrderParserType

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlAccessorType(XmlAccessType.NONE)
class Share implements Serializable {
	@SuppressWarnings(['unused', 'GroovyConstantNamingConvention'])
	private static final long serialVersionUID = 1L;

	List<Transaction> transactions = []

	@XmlAttribute
	def String isin

	@XmlAttribute
	def Double bid = null

	def Double amount = 0.00d

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double dividend = 0.00d
	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double invested = 0.00d
	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double winloss = 0.00d
//	@XmlAttribute
//	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double tax = 0.00d
//	@XmlAttribute
//	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double fee = 0.00d
	def Double interest = 0.00d

	@XmlAttribute(name="name")
	public String getName() {
		def transWithName = this.transactions.find{ it.name != null}
		if (transWithName && transWithName.name) {
			return transWithName.name
		} else {
			return "Unnamed"
		}
	}

	@XmlAttribute
	def Boolean isClosed() {
		def openAmount = getOpenAmount()
		return openAmount == 0 || openAmount == null
	}

	@XmlAttribute
	@XmlJavaTypeAdapter(Utils.XmlAdapters.DoubleAdapter)
	def Double getRate() {
		// Original Rate
		return (isClosed()) ? 0 : Math.abs(getOpenTotal()/getOpenAmount())
	}

	def Double getOpenAmount() {
		Double amount = transactions.findAll { (it.isBuy() || it.isPreEmptive()) }.sum { it.openAmount } ?: null
		if (transactions.first().isBitPanda()) {
			return amount?.round(8)
		} else {
			return amount?.round(4)
		}
	}

	@XmlAttribute
	def Double getOpenTotal() {
		// def amount = transactions.sum { ((it.isBuy() || it.isPreEmptive()) && it.openAmount > 0) ? it.amount : 0 }
		// def openAmount = transactions.sum { ((it.isBuy() || it.isPreEmptive()) && it.openAmount > 0) ? it.openAmount : 0 }
		def openTrans = transactions.findAll { (it.isBuy() || it.isPreEmptive()) && it.openAmount > 0 }
		double sum = (openTrans.any()) ? openTrans.sum { (it.total / it.amount) * it.openAmount } : 0.0d
		if (transactions.first().isBitPanda()) {
			return sum.round(8)
		} else {
			return sum.round(4)
		}
	}

	@XmlElement(name = "buy")
	def List<Transaction> getOrdersBuy() {
		return transactions.findAll { (it.isBuy() || it.isPreEmptive()) && !it.isTemporaryStatement() }
	}

	@XmlElement(name = "sell")
	def List<Transaction> getOrdersSell() {
		return transactions.findAll { it.isSell() && !it.isTemporaryStatement() }
	}

	@XmlElement(name = "buy-tmp")
	def List<Transaction> getOrdersBuyTemp() {
		return transactions.findAll { (it.isBuy() || it.isPreEmptive()) && it.isTemporaryStatement() }
	}

	@XmlElement(name = "sell-tmp")
	def List<Transaction> getOrdersSellTemp() {
		return transactions.findAll { it.isSell() && it.isTemporaryStatement() }
	}


	@XmlElement(name = "div")
	def List<Transaction> getOrdersDividend() {
		return transactions.findAll { it.isDiv() }
	}

	@XmlElement(name = "kno")
	def List<Transaction> getOrdersKnockout() {
		return transactions.findAll { it.isKnockout() }
	}

	@XmlElement(name = "tax")
	def List<Transaction> getOrdersPreTax() {
		return transactions.findAll { it.isTaxes() }
	}

	@XmlElement(name = "fee")
	def List<Transaction> getOrdersAdrFee() {
		return transactions.findAll { it.type == Transaction.TransTypes.FEE }
	}

	@XmlElement(name = "interest")
	def List<Transaction> getInterestOrders() {
		return transactions.findAll { it.type == Transaction.TransTypes.INTEREST }
	}

	public Share(String isin) {
		this.isin = isin
	}

	public Share(List<Transaction> transactions) {
		if (!transactions.any()) {
			assert transactions.any()
		}
		assert transactions.any()
		this.isin = transactions.first().isin
		addTransactions(transactions)
	}

	@Override
	public String toString() {
		return "Share $isin - Orders: ${transactions.size()}"
	}

	@SuppressWarnings('GroovyOverlyLongMethod')
	def void addTransactions(List<Transaction> isinTransactions) {
		def cryptoTaxFactor = (1-0.42-0.055)

		def parserType = isinTransactions.first().parserType
		assert !this.fee.isNaN()
		assert !this.invested.isNaN()
		assert !this.tax.isNaN()

		// sum up fees and taxes
		isinTransactions.each { transaction ->
			assert !transaction.total.isNaN()

			this.transactions.each {
				if (!it.isSplit() && !transaction.isSplit() && it.getFileName() != null && it.getFileName() == transaction.getFileName()
						&& it.parserType != OrderParserType.VIVID
						&& !it.getFileName().endsWith('.tsv') && !it.getFileName().endsWith('.csv')
						&& it.type === transaction.type
				) {
					println "Transaction with this filename already exists:"
					println "old: ${it}\nnew: ${transaction}"
					throw new IllegalStateException('Dupe found')
				}
			}

			this.transactions << transaction
			this.fee += transaction.fee
			this.tax += transaction.getTax()

			if ((transaction.isBuy() || transaction.isPreEmptive() || transaction.isInterest())) {
				this.invested += transaction.total
				this.amount += transaction.amount
				if (transaction.isInterest()) {
					this.interest += transaction.total
				}
			} else if(!transaction.isTaxes() && !transaction.isIsinSwitch()) {
				this.amount -= transaction.amount
			}

			addDividendsAndPreTax(transaction)
		}

		def preDatedIssues = []
		isinTransactions.findAll { it.isSell() || it.isKnockout() }.sort { it.date }.each { sellTrans ->
			def sameTypeTransactions = this.transactions.findAll {
				def sameOrigin = it.parserType == sellTrans.parserType
				return (it.isBuy() || it.isCapital() || it.isPreEmptive()) && sameOrigin
			}
			def preDatedOpenBuyTransactions = sameTypeTransactions.findAll {
				def hasOpenAmount = it.openAmount > 0
				def wasBeforeSell = it.date <= sellTrans.date
				return hasOpenAmount && wasBeforeSell
			}
			def preDatedClosedBuyTransactions = sameTypeTransactions.findAll {
				def hasOpenAmount = it.openAmount == 0
				def wasBeforeSell = it.date <= sellTrans.date
				return hasOpenAmount && wasBeforeSell
			}

			if (!preDatedOpenBuyTransactions.any()) {
				preDatedIssues << "Missing pre-dated buy transactions for\n$sellTrans"
			} else {
				def totalOpenAmount = (preDatedOpenBuyTransactions.sum { it.amount } as Double).round(8);
				if (totalOpenAmount < sellTrans.openAmount) {
					def msg = [
						"Unable to sell ${sellTrans.openAmount} (${totalOpenAmount-sellTrans.openAmount})",
						"only $totalOpenAmount available for\n$sellTrans",
						"${preDatedOpenBuyTransactions.join('\n-')}\n",
						"${preDatedClosedBuyTransactions.collect { it.toStringOneLine() }.join('\n-')}",

					]
					throw new IllegalStateException(msg.join('\n'))
				}

				def totalInvest = (preDatedOpenBuyTransactions.sum { it.total } as Double).round(8)
				def preDatedBuyRateAvg = (totalInvest / totalOpenAmount).round(8)

				def fifo = preDatedOpenBuyTransactions.sort{ it.date }.iterator()
				while(fifo.hasNext() && sellTrans.openAmount > 0) {
					def buyTrans = fifo.next()
					def buyval = 0d
					def sellval = 0d

          			def winLossAvgAmount = 0;
					if (buyTrans.openAmount >= sellTrans.openAmount) {
						buyval = (buyTrans.getRateFromTotal() * -1 * sellTrans.openAmount).round(2)
						sellval = sellTrans.getRateFromTotal() * sellTrans.openAmount
						buyTrans.openAmount -= sellTrans.openAmount
						winLossAvgAmount = sellTrans.openAmount
						sellTrans.openAmount = 0
					} else if (buyTrans.openAmount < sellTrans.openAmount) {
						buyval = (buyTrans.getRateFromTotal() * -1 * buyTrans.openAmount).round(2)
						sellval = sellTrans.getRateFromTotal() * buyTrans.openAmount
						sellTrans.openAmount -= buyTrans.openAmount
						winLossAvgAmount = buyTrans.openAmount
						buyTrans.openAmount = 0
					}

					def winloss = sellval + buyval
					sellTrans.winlossFifo += winloss
          			sellTrans.winlossAvg += sellval + winLossAvgAmount * preDatedBuyRateAvg

					this.winloss += (winloss).round(2)

					if (sellTrans.getDate().minusYears(1).compareTo(buyTrans.getDate()) < 0) {
						sellTrans.hasBuyTransAgeUnder365Days = true
					}
				}

				if (sellTrans.crypto) {
					// TODO handle >365 merge with bison logic below
					def tempTax = winloss * cryptoTaxFactor
					sellTrans.setTotal(sellTrans.total - tempTax)
					sellTrans.addTax(tempTax, true)
					sellTrans.winlossFifo -= tempTax
					sellTrans.winlossAvg -= tempTax
					this.winloss -= tempTax
				}
			}
		}

		if (preDatedIssues) {
			throw new IllegalStateException(preDatedIssues.join('\n----------------------------------------------\n'))
		}

		if (parserType == OrderParserType.BISON) {
			def totalCryptoWinLossUnder365Days = isinTransactions.findAll { it.isSell() && it.hasBuyTransAgeUnder365Days }.sum { it.winlossFifo }
			if (totalCryptoWinLossUnder365Days > 0) {
				def totalCryptoTax = totalCryptoWinLossUnder365Days * cryptoTaxFactor
				println "~" * 80
				println "${this.name} - Total Crypto Winloss: ${Utils.toCurrency(totalCryptoWinLossUnder365Days)}, Total Crypto Tax: ${Utils.toCurrency(totalCryptoTax)}"
				this.tax -= totalCryptoWinLossUnder365Days * cryptoTaxFactor
			}
		}
	}

	def void addDividendsAndPreTax(Transaction transaction) {
		if (transaction.isDiv() || transaction.isTaxes() || transaction.isInterest()) {
			transaction.winlossFifo = transaction.total
			this.winloss += transaction.total
			if (transaction.isDiv()) {
				this.dividend += transaction.total
			}
		}
	}
}
