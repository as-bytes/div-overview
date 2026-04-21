package com.asbytes

import com.asbytes.helpers.Utils
import org.ccil.cowan.tagsoup.Parser

import groovy.json.JsonSlurper
import org.codehaus.groovy.runtime.IOGroovyMethods

import java.text.NumberFormat
import java.text.ParseException

public class Exchange {
	static final userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB;     rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 (.NET CLR 3.5.30729)"

	static List<Exchange> exchanges = []

	String urlFormat
	String pattern
	String name

	static class Finanzen100 {
		static BidAndAsk getBidAndAsk(String isin, String html) {
			if (html == null) {
				return new BidAndAsk(0, 0, 0, "None", isin)
			}

			def slurper = new groovy.xml.XmlSlurper(new Parser()).parseText(html)
			List<BidAndAsk> bidAsks = []

			def hi52w = Utils.toDouble(slurper['**'].find { it.text() == '52W-Hoch' }.parent()['**'].find { it['@class'] == 'performance-overview__label__value' })
			if (hi52w == 0) {
				println "no 52w for $isin"
			}

			def container = slurper['**'].find { it['@id'] == 'modal-location-prices' }
			if (!container) {
				def bidask = slurper['**'].find { it['@class'] == 'bid-ask' }
				def ask = 0d
				def bid = 0d
				if (bidask) {
					ask = Utils.toDouble(bidask['**'].find { it['@class'] == 'bid-ask__value' })
					bid = Utils.toDouble(bidask['**'].find { it['@class'] == 'bid-ask__value' })
				}
				new BidAndAsk(bid, ask, hi52w, 'Unknown', isin).with {
					if (it.bid && it.ask) {
						bidAsks << it
					}
				}
			}

			if (!bidAsks.any()) {
				def quote = Utils.toDouble(slurper.'**'.find { it.'@class' == 'quote__price__price'}.text())
				def exchange = slurper.'**'.find { it.'@class' == 'quote__price__extributor'}.text()
				new BidAndAsk(quote, quote, hi52w, exchange, isin).with {
					if (exchange && it.bid && it.ask) {
						bidAsks << it
					}
				}
			}

			if (!bidAsks.any()) {
				def file = File.createTempFile(System.nanoTime().toString(), "_${isin}.html")
				file.write(html)
				println "kein Container ${file.absolutePath}"
			}

			def exchanges = slurper['**'].findAll { it['@class'] == 'price-location-list__cell__exchange' }
			exchanges.each {
				def tableNode = it.parent().parent()
				def exchange = tableNode.'**'.find { it.'@class' == 'price-location-list__cell__exchange'}.strong.text()
				def bid = Utils.toDouble(tableNode.'**'.find { it.'@class' == 'price-location-list__cell__bid' }.'**'.find { it.'@class' == 'price-location-list__number' }?.text())
				def ask = Utils.toDouble(tableNode.'**'.find { it.'@class' == 'price-location-list__cell__ask' }.'**'.find { it.'@class' == 'price-location-list__number' }?.text())
				new BidAndAsk(bid, ask, hi52w, exchange, isin).with {
					if (exchange && it.bid && it.ask) {
						bidAsks << it
					}
				}
			}


			def tradegate = bidAsks.find { it.exchange == 'Tradegate' }
			def xetra = bidAsks.find { it.exchange == 'Xetra' }
			def frankfurt = bidAsks.find { it.exchange.contains('Frankfurt') }
			def lunds = bidAsks.find { it.exchange == 'Lang & Schwarz' }
			def first = (bidAsks.any()) ? bidAsks.first() : null
			return tradegate ?: xetra ?:frankfurt ?: lunds ?: first ?: null
		}

		static String getHtml(String isin) {
			def url = new URL("https://json.finanzen100.de/v1/search/instrument_list?QUERY=$isin")
			def json = new JsonSlurper().parse(url)
			if (json['INSTRUMENT_LIST'].size() == 0) {
				return null
			}
			assert json['INSTRUMENT_LIST'].size() == 1: "INSTRUMENT_LIST count issue for $url"
			assert json['INSTRUMENT_LIST']['IDENTIFIER']['URL_LIST'].size() == 1: "INSTRUMENT_LIST IDENTIFIER URL_LIST count issue for $url"
			assert json['INSTRUMENT_LIST']['IDENTIFIER']['URL_LIST'][0]['URL'].size() == 1: "INSTRUMENT_LIST IDENTIFIER URL_LIST URL count issue for $url"
			def isinUrl = json['INSTRUMENT_LIST']['IDENTIFIER']['URL_LIST'][0]['URL'][0]

			def html = getHtmlFromUrl(isinUrl)
			return html
		}
	}



	def String getTestHtml() {
		"""
"""
	}


	static {
		// https://json.finanzen100.de/v1/search/instrument_list?QUERY=DE0008232125
		//exchanges.add(new Exchange('tradegate.de', 'https://www.tradegate.de/orderbuch.php?isin=%s', /<strong id="bid">(?<bid>[\d,.]+)/))
		//exchanges.add(new Exchange('comdirect.de', 'https://www.comdirect.de/inf/etfs/%s', /<span class="realtime[-a-z ]+">(?<bid>[\d,.]+)/))
		//exchanges.add(new Exchange('justeft.com', 'https://www.justetf.com/de/etf-profile.html?isin=%s', /<strong id="bid">(?<bid>[\d,.]+)/))
		exchanges.add(new Exchange('finanzen.net', 'https://www.finanzen.net/suchergebnis.asp?_search=%s', /<strong id="bid">(?<bid>[\d,.]+)/))
	}

	Exchange(String name, String urlFormat, String pattern) {
		this.name = name
		this.urlFormat = urlFormat
		this.pattern = pattern
	}

	static Double getBid(String isin) {
		println "$isin"
		return exchanges.findResult { exchange ->
			print "${exchange.name}: ".padLeft(12)
			def url = String.format(exchange.urlFormat, isin)
			def html = getHtmlFromUrl(url)
			def bid = null
			if (html.contains(isin)) {
				bid = parseHtmlForBid(html, exchange.pattern)
				println bid ?: "-,--"
			}
			return bid
		}
	}

	public static String getHtmlFromUrl(String url) {
		def urlConn = new URL(url).openConnection()
		urlConn.setRequestProperty("User-Agent", userAgent)
		urlConn.connect()
		urlConn.connectTimeout = 5000
		urlConn.readTimeout = 5000
		return IOGroovyMethods.getText(urlConn.inputStream)
	}

	private static Double parseHtmlForBid(String html, String pattern) {
		def match = html =~ pattern
		if (match.find()) {
			def bid = match.group('bid')
			return currencyToDouble(bid)
		} else {
			return null
		}
	}

	@SuppressWarnings('GroovyMultipleReturnPointsPerMethod')
	def static Double currencyToDouble(String value) {
		if (!value) {
			return null
		}

		def number = value.replace('EUR', '').replace('€', '').replace('- ', '-').replace('+', '').replace(' ', '').trim()
		def indexDot = number.indexOf('.')
		def indexComma = number.indexOf(',')

		if (indexDot != -1 && indexComma == -1) {
			def numberParts = number.split(/\./)
			if (numberParts.last().length() > 2) {
				number = numberParts.join('')
			}
		}

		def locale = (indexComma > indexDot) ? Locale.GERMAN : Locale.US
		try {
			return NumberFormat.getInstance(locale).parse(number)
		} catch(ParseException ignored) {
			return null
		}
	}
}
