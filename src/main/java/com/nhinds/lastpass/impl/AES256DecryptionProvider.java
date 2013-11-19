package com.nhinds.lastpass.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.nhinds.lastpass.LastPassException;

class AES256DecryptionProvider extends RequiresSpongyCastle implements DecryptionProvider {
	private static final String AES_ALGORITHM = "AES";
	private static final String PKCS5_PADDING = "PKCS5Padding";
	private static final String CBC_PLAIN_CIPHER = AES_ALGORITHM+"/CBC/"+PKCS5_PADDING;
	private static final String ECB_PLAIN_CIPHER = AES_ALGORITHM+"/ECB/"+PKCS5_PADDING;
	
	
	private final byte[] encryptionKey;

	public AES256DecryptionProvider(final byte[] encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	@Override
	public String decrypt(final byte[] aesItem) {
		final int length = aesItem.length;
		if (length == 0)
			return "";
		try {
			if (length % 16 == 0) {
				return decodeAES256ECBPlain(aesItem);
			}
			if (length % 16 == 1) {
				return decodeAES256CBCPlain(aesItem);
			}
		} catch (final IOException e) {
			throw new LastPassException("Error decrypting field", e);
		}
		throw new UnsupportedOperationException("Length: " + length);
	}

	private String decodeAES256ECBPlain(final byte[] aesItem) throws IOException {
		try {
			final Cipher cipher = Cipher.getInstance(ECB_PLAIN_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(this.encryptionKey, AES_ALGORITHM));
			return new String(cipher.doFinal(aesItem));
		} catch (final GeneralSecurityException e) {
			throw new IOException(e);
		}
	}

	private String decodeAES256CBCPlain(final byte[] aesItem) throws IOException {
		assert aesItem[0] == '!';
		try {
			final Cipher cipher = Cipher.getInstance(CBC_PLAIN_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(this.encryptionKey, AES_ALGORITHM), new IvParameterSpec(aesItem, 1, 16));
			return new String(cipher.doFinal(aesItem, 17, aesItem.length - 17));
		} catch (final GeneralSecurityException e) {
			throw new IOException(e);
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.encryptionKey);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof AES256DecryptionProvider)
				&& Arrays.equals(this.encryptionKey, ((AES256DecryptionProvider) obj).encryptionKey);
	}
}