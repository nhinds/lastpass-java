package com.nhinds.lastpass.encryption;

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;
import com.nhinds.lastpass.encryption.AES256EncryptionProvider;

@RunWith(Parameterized.class)
public class AES256DecryptionProviderTest {
	private static Collection<String> plainTexts = ImmutableList.of("", "a", "some very long string that should exceed the block size");

	@Parameters
	public static List<Object[]> params() {
		final Random rand = new Random();
		final byte[] key1 = new byte[32];
		final byte[] key2 = new byte[32];
		rand.nextBytes(key1);
		rand.nextBytes(key2);
		final List<Object[]> params = new ArrayList<Object[]>();
		for (final byte[] key : ImmutableList.of(key1, key2)) {
			for (final String plainText : plainTexts) {
				params.add(new Object[] { key, encryptCBCPlain(plainText, key), plainText });
				params.add(new Object[] { key, encryptECBPlain(plainText, key), plainText });
			}
			// Empty cipherText should equal empty plaintext
			params.add(new Object[] { key, new byte[0], "" });
		}
		return params;
	}

	private static byte[] encryptCBCPlain(final String plaintext, final byte[] encryptionKey) {
		try {
			final byte[] iv = new byte[16];
			new Random().nextBytes(iv);
			final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(iv));
			final byte[] cipherText = cipher.doFinal(plaintext.getBytes());
			final byte[] encryptedData = new byte[cipherText.length + 17];
			encryptedData[0] = '!';
			System.arraycopy(iv, 0, encryptedData, 1, iv.length);
			System.arraycopy(cipherText, 0, encryptedData, iv.length + 1, cipherText.length);
			return encryptedData;
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private static byte[] encryptECBPlain(String plainText, byte[] encryptionKey) {
		try {
			final Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"));
			return cipher.doFinal(plainText.getBytes());
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	private final byte[] encryptionKey;
	private final byte[] cipherText;
	private final String plainText;
	
	private AES256EncryptionProvider provider;

	public AES256DecryptionProviderTest(final byte[] encryptionKey, final byte[] cipherText, final String plainText) {
		this.encryptionKey = encryptionKey;
		this.cipherText = cipherText;
		this.plainText = plainText;
	}
	
	@Before
	public void setup() {
		provider = new AES256EncryptionProvider(this.encryptionKey);
	}

	@Test
	public void decrypt() {
		assertEquals(this.plainText, provider.decrypt(this.cipherText));
	}

	@Test
	public void roundtrip() {
		assertEquals(this.plainText, provider.decrypt(provider.encrypt(this.plainText)));
	}
}
