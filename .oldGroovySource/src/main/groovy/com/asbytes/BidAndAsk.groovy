package com.asbytes

import com.asbytes.helpers.Utils
import groovy.json.JsonOutput

class BidAndAsk {
	Double bid
	Double ask
	Double hi52w
	String exchange
	String isin

	BidAndAsk(Double bid, Double ask, Double hi52w, String exchange, String isin) {
		this.bid = Utils.toDouble(bid)
		this.ask = Utils.toDouble(ask)
		this.hi52w = hi52w
		this.exchange = exchange
		this.isin = isin
	}

	@Override
	String toString() {
		return JsonOutput.toJson(this)
	}
}
