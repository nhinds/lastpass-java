package com.nhinds.lastpass.impl.dto;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class LastPassError {
	@XmlAttribute
	private String message;
	@XmlAttribute
	private String cause;
	@XmlAttribute
	private Integer iterations;

	public String getCause() {
		return this.cause;
	}

	public Integer getIterations() {
		return this.iterations;
	}

	public String getMessage() {
		return this.message;
	}
}