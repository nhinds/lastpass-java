package com.nhinds.lastpass.encryption;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

public class PBKDF2SHA256KeyProvider extends RequiresSpongyCastle implements KeyProvider {

	private static final Hex HEX = new Hex();

	@Override
	public byte[] getKey(final String username, final String password, final int iterations) throws GeneralSecurityException {
		if (iterations == 1) {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256", "SC");
			digest.update(username.getBytes());
			digest.update(password.getBytes());
			return digest.digest();
		} else
			return doPBKDF2_SHA256(password, username, iterations);
	}

	@Override
	public String getHash(final byte[] key, final String password, final int iterations) throws GeneralSecurityException {
		if (iterations == 1) {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256", "SC");
			digest.update(HEX.encode(key));
			digest.update(password.getBytes());
			return new String(Hex.encodeHex(digest.digest()));
		} else
			return new String(Hex.encodeHex(doPBKDF2_SHA256(key, password, 1)));
	}

	private static byte[] doPBKDF2_SHA256(final String key, final String salt, final int iterations) {
		return doPBKDF2_SHA256(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(key.toCharArray()), salt, iterations);
	}

	private static byte[] doPBKDF2_SHA256(final byte[] key, final String salt, final int iterations) {
		final PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
		generator.init(key, salt.getBytes(), iterations);
		return ((KeyParameter) generator.generateDerivedMacParameters(32 * 8)).getKey();
	}

}
