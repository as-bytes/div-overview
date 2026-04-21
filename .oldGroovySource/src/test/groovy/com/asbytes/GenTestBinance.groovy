package com.asbytes

import groovy.json.JsonSlurper
import org.junit.Test

class GenTestBinance {
		URL uat(int offset, int limit) {
			return new URL("https://explorer.bnbchain.org/v1/staking/chains/bsc/delegators/bnb1fdkqwwgyprkwwz57t43ptxnh7x8gjsucctzpcq/rewards?limit=${limit}&offset=${offset}")
		}

		@Test
		public void testSerializeTransaction() {
			def rewards = []
			def initialRequest = new JsonSlurper().parse(uat(0, 100))
			rewards.addAll(initialRequest.rewardDetails)

			def max = initialRequest.total

			for (int offset=100; offset <= max; offset += 100) {
				def followRequest = new JsonSlurper().parse(uat(offset, 100))
				rewards.addAll(followRequest.rewardDetails)
			}

			println "max: " + max
			println "now: " + rewards.size()

			rewards.forEach {
				def reward = it.reward.toString().replace('.', ',')
				def time = it.rewardTime.replace('T', ' ').replace('.000+00:00', '')
				def line = "${it.id}\t${reward}\t${time}"
				new File('beacon-rewards.csv').append("${line}\n")
			}
		}

}
