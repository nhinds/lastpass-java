package com.nhinds.lastpass.impl.dto;

import com.google.api.client.util.Key;

public class LastPassResponse {
	@Key
	private LastPassError error;

	@Key
	private LastPassOk ok;

	public LastPassError getError() {
		return this.error;
	}

	public LastPassOk getOk() {
		return this.ok;
	}

	@Override
	public String toString() {
		return "LastPassResponse [error=" + this.error + ", ok=" + this.ok + "]";
	}
}
