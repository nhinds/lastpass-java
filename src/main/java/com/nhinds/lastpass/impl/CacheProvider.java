package com.nhinds.lastpass.impl;

import java.io.IOException;
import java.io.InputStream;

public interface CacheProvider {
	Integer getIterations(String username) throws IOException;

	Integer getAccountVersion(String username) throws IOException;

	InputStream getAccountData(String username) throws IOException;

	void storeAccountData(String username, int iterations, int accountVersion, InputStream accountData) throws IOException;
}
