package com.nhinds.lastpass.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.base.Objects;
import com.nhinds.lastpass.GoogleAuthenticatorRequired;
import com.nhinds.lastpass.LastPass.PasswordStoreBuilder;
import com.nhinds.lastpass.LastPassException;
import com.nhinds.lastpass.PasswordStore;
import com.nhinds.lastpass.impl.dto.LastPassOk;
import com.nhinds.lastpass.impl.dto.LastPassResponse;
import com.nhinds.lastpass.impl.dto.LastPassResponse.LastPassError;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

class LastPassBuilderImpl implements PasswordStoreBuilder {
	private static final String SESSION_COOKIE_NAME = "PHPSESSID";

	private final Client client;
	private final String username;
	private final String password;
	private final File cacheFile;// TODO use me
	private final KeyProvider keyProvider;

	private int iterations = 1;
	private byte[] key;
	private String hash;

	public LastPassBuilderImpl(final Client client, final String username, final String password, final File cacheFile,
			final KeyProvider keyProvider) {
		this.client = client;
		this.username = username;
		this.password = password;
		this.cacheFile = cacheFile;
		this.keyProvider = keyProvider;
	}

	@Override
	public int hashCode() {
		return this.username.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof LastPassBuilderImpl))
			return false;
		final LastPassBuilderImpl other = (LastPassBuilderImpl) obj;
		return Objects.equal(this.username, other.username) && Objects.equal(this.password, other.password)
				&& Objects.equal(this.cacheFile, other.cacheFile);
	}

	@Override
	public PasswordStore getPasswordStore() throws GoogleAuthenticatorRequired {
		try {
			return getPasswordStore(null);
		} catch (final LastPassBuilderImpl.ErrorResponseException e) {
			if ("googleauthrequired".equals(e.getError().getCause()))
				throw new GoogleAuthenticatorRequired(e.getError().getMessage(), e.getCause());
			throw e;
		}
	}

	@Override
	public PasswordStore getPasswordStore(final String otp) {
		try {
			// try {
			// TODO
			// if (cacheFile.isFile())
			// return new PasswordStoreImpl(new FileInputStream(cacheFile), getKey(username, password, ???));
			final LastPassOk loginResponse = login(otp);
			final ClientResponse clientResponse = this.client.resource("https://lastpass.com/getaccts.php?mobile=1&hash=0.0")
					.cookie(new Cookie(SESSION_COOKIE_NAME, loginResponse.getSessionId())).get(ClientResponse.class);
			// clientResponse.bufferEntity();
			// final InputStream entityInputStream = clientResponse.getEntityInputStream();
			// try (FileOutputStream out = new FileOutputStream(cache)) {
			// IOUtils.copy(entityInputStream, out);
			// }
			// entityInputStream.reset();
			assert this.key != null;
			return new PasswordStoreImpl(clientResponse.getEntity(InputStream.class), new AESCBCDecryptionProvider(this.key));
			// } catch (final IOException e) {
			// throw new LastPassException(e);
			// }
		} catch (final GeneralSecurityException e) {
			throw new LastPassException(e);
		}
	}

	private LastPassOk login(final String otp) throws GeneralSecurityException {
		if (this.key == null) {
			this.key = this.keyProvider.getKey(this.username, this.password, this.iterations);
		}
		if (this.hash == null) {
			this.hash = this.keyProvider.getHash(this.key, this.password, this.iterations);
		}
		final MultivaluedMap<String, String> options = new MultivaluedMapImpl();
		options.putSingle("method", "mobile");
		options.putSingle("web", "1");
		options.putSingle("xml", "1");
		options.putSingle("username", this.username);
		options.putSingle("hash", this.hash);
		if (otp != null) {
			options.putSingle("otp", otp);
		}
		options.putSingle("iterations", Integer.toString(this.iterations));
		final ClientResponse clientResponse = this.client.resource("https://lastpass.com/login.php").post(
				ClientResponse.class, options);
		clientResponse.bufferEntity();
		try {
			return clientResponse.getEntity(LastPassOk.class);
		} catch (final RuntimeException okException) {
			try {
				clientResponse.getEntityInputStream().reset();
			} catch (final IOException e) {
				throw new IllegalStateException("This should never happen, reset should always work on a ByteArrayInputStream", e);
			}
			final LastPassResponse response = clientResponse.getEntity(LastPassResponse.class);

			final LastPassError error = response.getError();
			if (error != null) {
				if (error.getIterations() != null && error.getIterations().intValue() != this.iterations) {
					if (this.iterations != 1)
						throw new IllegalStateException("Expected " + this.iterations + " iterations but response indicated "
								+ error.getIterations());
					this.iterations = error.getIterations();
					this.key = null;
					this.hash = null;
					return login(otp);
				} else
					throw new ErrorResponseException(error, okException);
			}
			throw new LastPassException("No error found but unsuccessful response: " + clientResponse, okException);
		}
	}

	private static class ErrorResponseException extends LastPassException {
		private static final long serialVersionUID = 1L;

		private final LastPassError error;

		public ErrorResponseException(final LastPassError error, final Throwable cause) {
			super("Error logging in: " + error.getMessage() + " (" + error.getCause() + ")", cause);
			this.error = error;
		}

		public LastPassError getError() {
			return this.error;
		}
	}
}