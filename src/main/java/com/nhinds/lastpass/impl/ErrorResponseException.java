package com.nhinds.lastpass.impl;

import com.nhinds.lastpass.LastPassException;
import com.nhinds.lastpass.impl.dto.LastPassError;

public class ErrorResponseException extends LastPassException {
	private static final long serialVersionUID = 1L;

	private final LastPassError error;

	public ErrorResponseException(final LastPassError error) {
		super("Error logging in: " + error.getMessage() + " (" + error.getCause() + ")");
		this.error = error;
	}

	public LastPassError getError() {
		return this.error;
	}
}