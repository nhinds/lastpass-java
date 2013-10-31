package com.nhinds.lastpass.impl.dto;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "response")
public class LastPassResponse {

	@XmlElement
	private LastPassError error;

	public LastPassError getError() {
		return this.error;
	}

	@XmlType
	public static class LastPassError {
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
}
