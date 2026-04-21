package com.asbytes

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.*
import javax.xml.bind.util.JAXBSource
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

@XmlRootElement(name="Shares")
@XmlAccessorType(XmlAccessType.NONE)
class XmlShares implements Serializable {
	@SuppressWarnings(['unused', 'GroovyConstantNamingConvention'])
	private static final long serialVersionUID = 1L;

	def List<Share> shares = []

	public XmlShares() {
	}

	private static Map<String, Double> winlossByMonth = [:]

	public static XmlShares fromOrders(List<Share> shares) {
		def xmlShares = new XmlShares()
		shares.each { share ->
			share?.transactions?.each { order ->
				def datstr = order.getDate().format("yyyy-MM")
				def gain = share.winloss
				if (!winlossByMonth.containsKey(datstr)) {
					winlossByMonth[datstr] = 0d
				}
				winlossByMonth[datstr] += gain
			}
			xmlShares.shares.add(share)
		}
		return xmlShares
	}

	def File toHtml(xslPath, filePath) {
		def jaxb = JAXBContext.newInstance(this.class)
		def marsh = jaxb.createMarshaller()
		marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
		marsh.setProperty(Marshaller.JAXB_ENCODING, "UTF-8")

		def fact = TransformerFactory.newInstance()
		def xslt = new StreamSource(xslPath)
		def transformer = fact.newTransformer(xslt)

		def source = new JAXBSource(jaxb, this)
		def html = new File(filePath)
		html.write("")
		def result = new StreamResult(html)
		transformer.transform(source, result)
		return html
	}

	@XmlAttribute
	public String getCalenderWeek() {
		return new Date().format("w")
	}

	@XmlAttribute
	public String getDate() {
		return new Date().format("yyyy-MM-dd HH:mm:ss")
	}

	@XmlElement(name = "share")
	def List<Share> getShares() {
		return shares.sort { it.name }
	}
/*
	@XmlElement(name = "summaries")
	def List<XmlSummary> getSummaries() {
		def summaries = []
		winlossByMonth.sort().each {
			summaries << new XmlSummary(it.key, it.value.round(2))
		}
		return summaries
	}
*/

	@XmlAttribute
	def Double getOpenTotal() { shares.sum { it.openTotal }.round(2) }

	@XmlAttribute
	def Double getDividend() { shares.sum { it.dividend }.round(2) }

	@XmlAttribute
	def Double getWinloss() { shares.sum { it.winloss }.round(2) }

	@XmlAttribute
	def Double getFee() { shares.sum { it.fee }.round(2) }

	@XmlAttribute
	def Double getInterest() { shares.sum { it.interest }.round(2) }

	@XmlAttribute
	def Double getTax() { shares.sum { it.tax }.round(2) }

	def XmlShares toStr() {
		this.shares.each { share ->
			println share
			share.transactions.each { order ->
				println order
			}
		}
		return this
	}

	def XmlShares toXml(filePath, xsl) {
		def jaxb = JAXBContext.newInstance(this.class)
		def marsh = jaxb.createMarshaller()
		marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
		marsh.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		String xslHeader = "<?xml-stylesheet type='text/xsl' href='$xsl' ?>"
		marsh.setProperty("com.sun.xml.internal.bind.xmlHeaders", xslHeader)
		marsh.marshal(this, new File(filePath))
		/*
        def fact = TransformerFactory.newInstance()
        def xslt = new StreamSource("./summary.xsl")
        def transformer = fact.newTransformer(xslt)

		def source = new JAXBSource(jaxb, this)
		def result = new StreamResult(System.out)
		transformer.transform(source, result)
		*/
		return this
	}

	def File toXml(filePath) {
		def file = new File(filePath)
		def jaxb = JAXBContext.newInstance(this.class)
		def marsh = jaxb.createMarshaller()
		marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
		marsh.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marsh.marshal(this, file)
		/*
        def fact = TransformerFactory.newInstance()
        def xslt = new StreamSource("./summary.xsl")
        def transformer = fact.newTransformer(xslt)

		def source = new JAXBSource(jaxb, this)
		def result = new StreamResult(System.out)
		transformer.transform(source, result)
		*/
		return file
	}
}
