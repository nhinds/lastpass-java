package com.nhinds.lastpass;

/** Indicates that the login could not be completed because the user needs to be prompted for google authenticator credentials */
public class GoogleAuthenticatorRequired extends Exception {
	private static final long serialVersionUID = 1L;

	public GoogleAuthenticatorRequired(String message) {
		super(message);
	}

	public GoogleAuthenticatorRequired(Throwable cause) {
		super(cause);
	}

	public GoogleAuthenticatorRequired(String message, Throwable cause) {
		super(message, cause);
	}

}
