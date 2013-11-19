package com.nhinds.lastpass.encryption;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.nhinds.lastpass.LastPassException;

public class AES256EncryptionProvider extends RequiresSpongyCastle implements EncryptionProvider {
	private static final char CBC_MARKER = '!';
	private static final int IV_LENGTH = 16;
	private static final String AES_ALGORITHM = "AES";
	private static final String PKCS5_PADDING = "PKCS5Padding";
	private static final String CBC_PLAIN_CIPHER = AES_ALGORITHM+"/CBC/"+PKCS5_PADDING;
	private static final String ECB_PLAIN_CIPHER = AES_ALGORITHM+"/ECB/"+PKCS5_PADDING;
	
	
	private final byte[] encryptionKey;

	public AES256EncryptionProvider(final byte[] encryptionKey) {
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
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * This currently encrypts the data with AES-256 CBC
	 */
	@Override
	public byte[] encrypt(String plainText) {
		final byte[] plainTextBytes = plainText.getBytes();
		try {
			final byte[] iv = new byte[IV_LENGTH];
			new Random().nextBytes(iv);
			
			final Cipher cipher = Cipher.getInstance(CBC_PLAIN_CIPHER);
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, AES_ALGORITHM), new IvParameterSpec(iv));
			final byte[] encryptedData = new byte[cipher.getOutputSize(plainTextBytes.length) + IV_LENGTH + 1];
			encryptedData[0] = CBC_MARKER;
			System.arraycopy(iv, 0, encryptedData, 1, iv.length);
			cipher.doFinal(plainTextBytes, 0, plainTextBytes.length, encryptedData, IV_LENGTH + 1);
			return encryptedData;
		} catch (final GeneralSecurityException e) {
			throw new LastPassException("Error encrypting data", e);
		}
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
		assert aesItem[0] == CBC_MARKER;
		try {
			final Cipher cipher = Cipher.getInstance(CBC_PLAIN_CIPHER);
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(this.encryptionKey, AES_ALGORITHM), new IvParameterSpec(aesItem, 1, IV_LENGTH));
			return new String(cipher.doFinal(aesItem, IV_LENGTH + 1, aesItem.length - IV_LENGTH - 1));
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
		return (obj instanceof AES256EncryptionProvider)
				&& Arrays.equals(this.encryptionKey, ((AES256EncryptionProvider) obj).encryptionKey);
	}
}