package com.nhinds.lastpass.impl;

import java.io.IOException;
import java.io.InputStream;

/** A cache provider which does not cache */
public class NullCacheProvider implements CacheProvider {

	@Override
	public Integer getIterations(String username) throws IOException {
		return null;
	}

	@Override
	public Integer getAccountVersion(String username) throws IOException {
		return null;
	}

	@Override
	public InputStream getAccountData(String username) throws IOException {
		return null;
	}

	@Override
	public void storeAccountData(String username, int iterations, int accountVersion, InputStream accountData) throws IOException {
		// nothing to do
	}

	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NullCacheProvider;
	}

}
