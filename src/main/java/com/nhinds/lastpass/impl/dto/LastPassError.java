package com.nhinds.lastpass.impl.dto;

import com.google.api.client.util.Key;

public class LastPassError {
	@Key("@message")
	private String message;
	@Key("@cause")
	private String cause;
	@Key("@iterations")
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

	@Override
	public String toString() {
		return "LastPassError [message=" + this.message + ", cause=" + this.cause + ", iterations=" + this.iterations + "]";
	}
}