package com.nhinds.lastpass.impl;

interface DecryptionProvider {
	String decrypt(byte[] encryptedData);
}