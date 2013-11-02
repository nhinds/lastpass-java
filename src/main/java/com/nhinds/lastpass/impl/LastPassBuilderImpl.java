package com.nhinds.lastpass.impl;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.google.common.base.Objects;
import com.nhinds.lastpass.GoogleAuthenticatorRequired;
import com.nhinds.lastpass.LastPass.PasswordStoreBuilder;
import com.nhinds.lastpass.LastPassException;
import com.nhinds.lastpass.PasswordStore;
import com.nhinds.lastpass.impl.dto.LastPassError;
import com.nhinds.lastpass.impl.dto.LastPassOk;
import com.nhinds.lastpass.impl.dto.LastPassResponse;

class LastPassBuilderImpl implements PasswordStoreBuilder {
	private static final String SESSION_COOKIE_NAME = "PHPSESSID";

	private static final HttpRequestInitializer XML_REQUEST_INITIALIZER = new HttpRequestInitializer() {
		@Override
		public void initialize(HttpRequest request) throws IOException {
			XmlNamespaceDictionary namespaceDictionary = new XmlNamespaceDictionary();
			// google-http-client errors if elements have no xml namespace if we don't do this
			namespaceDictionary.set("", "");
			request.setParser(new XmlObjectParser(namespaceDictionary));
		}
	};

	private final HttpRequestFactory requestFactory;
	private final String username;
	private final String password;
	private final File cacheFile;// TODO use me
	private final String deviceId;
	private final KeyProvider keyProvider;

	private int iterations = 1;
	private byte[] key;
	private String hash;

	public LastPassBuilderImpl(final HttpTransport transport, final String username, final String password, final File cacheFile,
			final String deviceId, final KeyProvider keyProvider) {
		this.username = username;
		this.password = password;
		this.cacheFile = cacheFile;
		this.deviceId = deviceId;
		this.keyProvider = keyProvider;
		this.requestFactory = transport.createRequestFactory(XML_REQUEST_INITIALIZER);
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
				&& Objects.equal(this.cacheFile, other.cacheFile) && Objects.equal(this.deviceId, other.deviceId);
	}

	@Override
	public PasswordStore getPasswordStore() throws GoogleAuthenticatorRequired {
		try {
			return getPasswordStore(null, null);
		} catch (final LastPassBuilderImpl.ErrorResponseException e) {
			if ("googleauthrequired".equals(e.getError().getCause()))
				throw new GoogleAuthenticatorRequired(e.getError().getMessage(), e.getCause());
			throw e;
		}
	}

	@Override
	public PasswordStore getPasswordStore(final String otp, final String trustLabel) {
		if (this.deviceId == null && trustLabel != null)
			throw new IllegalArgumentException("Cannot specify a trusted device label if no device ID was provided");

		try {
			// try {
			// TODO
			// if (cacheFile.isFile())
			// return new PasswordStoreImpl(new FileInputStream(cacheFile), getKey(username, password, ???));
			final LastPassOk loginResponse = login(otp, trustLabel);
			final HttpRequest request = this.requestFactory
					.buildGetRequest(new GenericUrl("https://lastpass.com/getaccts.php?mobile=1&hash=0.0"));
			request.getHeaders().setCookie(SESSION_COOKIE_NAME + '=' + loginResponse.getSessionId());
			final HttpResponse response = request.execute();
			// clientResponse.bufferEntity();
			// final InputStream entityInputStream = clientResponse.getEntityInputStream();
			// try (FileOutputStream out = new FileOutputStream(cache)) {
			// IOUtils.copy(entityInputStream, out);
			// }
			// entityInputStream.reset();
			assert this.key != null;
			return new PasswordStoreImpl(response.getContent(), new AESCBCDecryptionProvider(this.key));
			// } catch (final IOException e) {
			// throw new LastPassException(e);
			// }
		} catch (final IOException e) {
			throw new LastPassException("Error connecting to LastPass", e);
		} catch (final GeneralSecurityException e) {
			throw new LastPassException(e);
		}
	}

	private LastPassOk login(final String otp, final String trustLabel) throws GeneralSecurityException, IOException {
		if (this.key == null) {
			this.key = this.keyProvider.getKey(this.username, this.password, this.iterations);
		}
		if (this.hash == null) {
			this.hash = this.keyProvider.getHash(this.key, this.password, this.iterations);
		}
		final Map<String, String> options = new HashMap<String, String>();
		options.put("method", "cr");
		options.put("web", "1");
		options.put("xml", "2");
		options.put("username", this.username);
		options.put("hash", this.hash);
		if (this.deviceId != null) {
			options.put("uuid", this.deviceId);
		}
		if (otp != null) {
			options.put("otp", otp);
			if (trustLabel != null) {
				options.put("trustlabel", trustLabel);
			}
		}
		options.put("iterations", Integer.toString(this.iterations));

		final HttpResponse clientResponse = this.requestFactory.buildPostRequest(new GenericUrl("https://lastpass.com/login.php"),
				new UrlEncodedContent(options)).execute();
		final LastPassResponse response = clientResponse.parseAs(LastPassResponse.class);

		if (response != null) {
			// Try interpreting it as an OK response
			if (response.getOk() != null) {
				return response.getOk();
			}

			// Try interpreting it as an error response
			if (response.getError() != null) {
				final LastPassError error = response.getError();
				// If the error is caused by using the incorrect number of
				// iterations in the encryption, try again with the correct number
				if (error.getIterations() != null && error.getIterations().intValue() != this.iterations) {
					if (this.iterations != 1)
						throw new IllegalStateException("Expected " + this.iterations + " iterations but response indicated "
								+ error.getIterations());
					this.iterations = error.getIterations();
					this.key = null;
					this.hash = null;
					return login(otp, trustLabel);
				} else
					throw new ErrorResponseException(error);
			}
		}
		throw new LastPassException("No error found but unsuccessful response: " + clientResponse + " (" + response + ")");
	}

	private static class ErrorResponseException extends LastPassException {
		private static final long serialVersionUID = 1L;

		private final LastPassError error;

		public ErrorResponseException(final LastPassError error) {
			super("Error logging in: " + error.getMessage() + " (" + error.getCause() + ")");
			this.error = error;
		}

		public LastPassError getError() {
			return this.error;
		}
	}
}
