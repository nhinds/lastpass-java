package com.nhinds.lastpass.impl;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.nhinds.lastpass.LastPassException;

class AESCBCDecryptionProvider extends RequiresSpongyCastle implements DecryptionProvider {
	private final byte[] encryptionKey;

	public AESCBCDecryptionProvider(final byte[] encryptionKey) {
		this.encryptionKey = encryptionKey;
	}

	@Override
	public String decrypt(final byte[] aesItem) {
		final int length = aesItem.length;
		if (length == 0)
			return "";
		if (length % 16 == 1) {
			try {
				return decodeAES256CBCPlain(aesItem);
			} catch (final IOException e) {
				throw new LastPassException("Error decrypting field", e);
			}
		}
		throw new UnsupportedOperationException("Length: " + length);
	}

	private String decodeAES256CBCPlain(final byte[] aesItem) throws IOException {
		assert aesItem[0] == '!';
		try {
			final byte[] iv = Arrays.copyOfRange(aesItem, 1, 17);
			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(this.encryptionKey, "AES"), new IvParameterSpec(iv));
			return new String(cipher.doFinal(aesItem, 17, aesItem.length - 17));
		} catch (final GeneralSecurityException e) {
			throw new IOException("Well, I tried: " + this.encryptionKey.length, e);
		}
	}
}