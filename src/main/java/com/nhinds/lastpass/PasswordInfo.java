package com.nhinds.lastpass;

/** Information about a single account stored in LastPass */
public interface PasswordInfo {
	/**
	 * @return the internal identifier of this password
	 * @see PasswordStore#getPassword(long)
	 */
	long getId();

	/** @return the login URL for this account */
	String getUrl();

	/** @return the username for this account */
	String getUsername();

	/** @return the password for this account */
	String getPassword();

	/** @return the display name of this account */
	String getName();
}
