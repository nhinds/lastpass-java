package com.nhinds.lastpass.impl;

import java.security.GeneralSecurityException;

public interface KeyProvider {

	byte[] getKey(String username, String password, int iterations) throws GeneralSecurityException;

	String getHash(byte[] key, String password, int iterations) throws GeneralSecurityException;

}
