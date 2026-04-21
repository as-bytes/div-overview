package com.asbytes

import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name="entry")
@XmlAccessorType(XmlAccessType.NONE)
class XmlSummary implements Serializable {
	@SuppressWarnings(['unused', 'GroovyConstantNamingConvention'])
	private static final long serialVersionUID = 1L;

	@XmlAttribute
	def String key
	@XmlAttribute
	def Double value

	public XmlSummary() {
	}

	public XmlSummary(String key, Double value) {
		this.key = key
		this.value = value
	}
}
