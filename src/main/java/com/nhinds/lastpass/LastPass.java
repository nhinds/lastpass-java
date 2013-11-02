package com.nhinds.lastpass;

import java.io.File;

public interface LastPass {
	/** Represents login progress */
	enum ProgressStatus {
		LOGGING_IN, RETRIEVING, DECRYPTING;
	}

	/** Listener interface which is notified about login progress */
	interface ProgressListener {
		void statusChanged(ProgressStatus status);
	}
	/**
	 * An interface to retrieve the password store for a given user
	 */
	interface PasswordStoreBuilder {
		/**
		 * Attempt to get the password store without a one-time password
		 * 
		 * @param listener
		 *            Listener to notify of status changes while getting the password store, may be null
		 * @return The password store for the configured user
		 * @throws GoogleAuthenticatorRequired
		 *             if a one-time password is required. In this case, prompt the user for a one-time password then call
		 *             {@link #getPasswordStore(String)}
		 */
		PasswordStore getPasswordStore(ProgressListener listener) throws GoogleAuthenticatorRequired;

		/**
		 * Attempt to get the password store specifying a one-time password, and optionally trust the current device. This should generally
		 * only be called if {@link #getPasswordStore()} throws a {@link GoogleAuthenticatorRequired} exception
		 * 
		 * @param otp
		 *            The one-time password
		 * @param trustLabel
		 *            The label to use for this trusted device in lastpass, or null to not trust this device
		 * @param listener
		 *            Listener to notify of status changes while getting the password store, may be null
		 * @return The password store for the configured user
		 */
		PasswordStore getPasswordStore(String otp, String trustLabel, ProgressListener listener);
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
	 * @param deviceId
	 *            The identifier for the current device (may be null, in which case the device will not be able to be trusted)
	 * @return A builder which can retrieve the password store for the given user
	 * @throws LastPassException
	 *             if there is an error logging in
	 */
	PasswordStoreBuilder getPasswordStoreBuilder(String username, String password, File cacheFile, String deviceId);
}
