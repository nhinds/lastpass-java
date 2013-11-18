package com.nhinds.lastpass;

import java.io.File;

import com.nhinds.lastpass.LastPass.PasswordStoreBuilder;
import com.nhinds.lastpass.impl.FileCacheProvider;
import com.nhinds.lastpass.impl.LastPassImpl;
import com.nhinds.lastpass.impl.NullCacheProvider;

/**
 * Factory for retrieving instances of {@link LastPass}
 */
public class LastPassFactory {
	/**
	 * Get a LastPass implementation which does not cache account information
	 * 
	 * @return A non-caching LastPass implementation
	 */
	public static LastPass getLastPass() {
		return new LastPassImpl(new NullCacheProvider());
	}

	/**
	 * Get a LastPass implementation which caches account information in the given file
	 * <p>
	 * The returned {@link LastPass} and {@link PasswordStoreBuilder} objects from
	 * {@link LastPass#getPasswordStoreBuilder(String, String, String)} are not safe to use from multiple threads, although the
	 * {@link PasswordStore} objects created by the {@link PasswordStoreBuilder} are safe to use from multiple threads.
	 * 
	 * @param cacheFile
	 *            The file used to cache login and account data for offline logins. May not be null.
	 * @return A LastPass implementation which caches to a file
	 */
	public static LastPass getCachingLastPass(final File cacheFile) {
		return new LastPassImpl(new FileCacheProvider(cacheFile));
	}
}
