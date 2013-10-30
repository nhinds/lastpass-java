package com.nhinds.lastpass;

import java.io.File;

public interface LastPass {
	/**
	 * An interface to retrieve the password store for a given user
	 */
	interface PasswordStoreBuilder {
		/**
		 * Attempt to get the password store without a one-time password
		 * 
		 * @return The password store for the configured user
		 * @throws GoogleAuthenticatorRequired
		 *             if a one-time password is required. In this case, prompt the user for a one-time password then call
		 *             {@link #getPasswordStore(String)}
		 */
		PasswordStore getPasswordStore() throws GoogleAuthenticatorRequired;

		/**
		 * Attempt to get the password store specifying a one-time password. This should generally only be called if
		 * {@link #getPasswordStore()} throws a {@link GoogleAuthenticatorRequired} exception
		 * 
		 * @param otp
		 *            The one-time password
		 * @return The password store for the configured user
		 */
		PasswordStore getPasswordStore(String otp);
	}

	/**
	 * Construct a {@link PasswordStoreBuilder} to retrieve a password store with the given details
	 * 
	 * @param username
	 *            The LastPass username to log in with
	 * @param password
	 *            The password for the LastPass account
	 * @param cacheFile
	 *            TODO use this
	 * @return A builder which can retrieve the password store for the given user
	 * @throws LastPassException
	 *             if there is an error logging in
	 */
	PasswordStoreBuilder getPasswordStoreBuilder(String username, String password, File cacheFile);
}
