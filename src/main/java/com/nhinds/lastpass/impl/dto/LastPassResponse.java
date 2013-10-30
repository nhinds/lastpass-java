package com.nhinds.lastpass.impl.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "response")
@XmlAccessorType(XmlAccessType.FIELD)
public class LastPassResponse {

	@XmlElement
	private LastPassError error;

	public LastPassError getError() {
		return error;
	}

	@XmlType
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class LastPassError {
		@XmlAttribute
		private String message;
		@XmlAttribute
		private String cause;
		@XmlAttribute
		private Integer iterations;

		public String getCause() {
			return cause;
		}

		public Integer getIterations() {
			return iterations;
		}

		public String getMessage() {
			return message;
		}
	}
}
