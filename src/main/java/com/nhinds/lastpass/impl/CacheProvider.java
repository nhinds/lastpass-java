package com.nhinds.lastpass.impl;

import java.io.IOException;
import java.io.InputStream;

/**
 * Cache provider for offline logins and login optimization
 * <p>
 * Providers are not required to store data for any period of time, and any of the <code>get*</code> methods may return null at any time
 */
public interface CacheProvider {
	/**
	 * @return the stored number of iterations for the given username, or null if no iterations are available for the given username
	 */
	Integer getIterations(String username) throws IOException;

	/**
	 * @return the stored account version for the given username, or null if no account version is available for the given username
	 */
	Integer getAccountVersion(String username) throws IOException;

	/**
	 * @return a stream of the stored account data for the given username, or null if no account data is available for the given username
	 */
	InputStream getAccountData(String username) throws IOException;

	/**
	 * Store the given information against a username. If existing data is stored for the username, it should be replaced with the given
	 * information.
	 */
	void storeAccountData(String username, int iterations, int accountVersion, InputStream accountData) throws IOException;
}
