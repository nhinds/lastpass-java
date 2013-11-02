package com.nhinds.lastpass.impl.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class LastPassResponse {

	@XmlElement
	private LastPassError error;

	@XmlElement
	private LastPassOk ok;

	public LastPassError getError() {
		return this.error;
	}

	public LastPassOk getOk() {
		return this.ok;
	}
}
