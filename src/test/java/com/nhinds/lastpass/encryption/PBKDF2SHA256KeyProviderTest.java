package com.nhinds.lastpass.encryption;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;

import org.junit.Test;

import com.nhinds.lastpass.encryption.PBKDF2SHA256KeyProvider;

public class PBKDF2SHA256KeyProviderTest {

	private static byte[] KEY_a_b_1 = new byte[] { -5, -114, 32, -4, 46, 76, 63, 36, -116, 96, -61, -101, -42, 82, -13, -63, 52, 114, -104,
		-69, -105, 123, -117, 77, 89, 3, -72, 80, 85, 98, 6, 3 };
	private static byte[] KEY_a_b_2 = new byte[] { 33, 19, 73, -103, 24, 16, 9, -85, 41, -31, 47, -34, 1, -28, -104, -31, -98, 45, 53, -57,
		27, 94, -20, -47, 107, 32, 42, 20, -84, 35, -30, -8 };
	private static byte[] KEY_a_b_200 = new byte[] { 23, -63, 45, 35, 126, -29, 22, 2, -4, 78, -123, 115, -71, -66, -126, -107, -10, 87,
		-65, -128, -79, -83, 45, -26, 117, 127, -73, -5, -102, 99, 2, -1 };

	private static final String HASH_a_b_1 = "76c606f5932ddfe0dfb125ac4d33426b7b172cfdd024d6a79ec69608b678961b";
	private static final String HASH_a_b_2 = "266cd96b9060ed4ea3346a18f040ee137a8c7f43a3df90ff13d0c67d02669828";
	private static final String HASH_a_b_200 = "80eaf751bebacb74fe0ac800b78a74c725c2ae94d7f092da9f98e398c93383b1";

	private final PBKDF2SHA256KeyProvider provider = new PBKDF2SHA256KeyProvider();

	@Test
	public void testKeyAB1() throws GeneralSecurityException {
		assertArrayEquals(KEY_a_b_1, this.provider.getKey("a", "b", 1));
	}

	@Test
	public void testKeyAB2() throws GeneralSecurityException {
		assertArrayEquals(KEY_a_b_2, this.provider.getKey("a", "b", 2));
	}

	@Test
	public void testKeyAB200() throws GeneralSecurityException {
		assertArrayEquals(KEY_a_b_200, this.provider.getKey("a", "b", 200));
	}

	@Test
	public void testHashAB1() throws GeneralSecurityException {
		assertEquals(HASH_a_b_1, this.provider.getHash(KEY_a_b_1, "b", 1));
	}

	@Test
	public void testHashAB2() throws GeneralSecurityException {
		assertEquals(HASH_a_b_2, this.provider.getHash(KEY_a_b_2, "b", 2));
	}

	@Test
	public void testHashAB200() throws GeneralSecurityException {
		assertEquals(HASH_a_b_200, this.provider.getHash(KEY_a_b_200, "b", 200));
	}
}
