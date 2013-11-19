package com.nhinds.lastpass.impl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.nhinds.lastpass.LastPassException;
import com.nhinds.lastpass.impl.dto.LastPassError;
import com.nhinds.lastpass.impl.dto.LastPassOk;
import com.nhinds.lastpass.impl.dto.LastPassResponse;

public class LastPassLoginProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(LastPassLoginProvider.class);

	public static class LoginResult {
		private final String sessionId;
		private final byte[] key;
		private final int accountsVersion;
		private final int iterations;

		public LoginResult(String sessionId, byte[] key, int accountsVersion, int iterations) {
			this.sessionId = sessionId;
			this.key = key;
			this.accountsVersion = accountsVersion;
			this.iterations = iterations;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		public byte[] getKey() {
			return this.key;
		}

		public int getAccountsVersion() {
			return this.accountsVersion;
		}

		public int getIterations() {
			return this.iterations;
		}
	}

	private static final HttpRequestInitializer XML_REQUEST_INITIALIZER = new HttpRequestInitializer() {
		@Override
		public void initialize(HttpRequest request) throws IOException {
			XmlNamespaceDictionary namespaceDictionary = new XmlNamespaceDictionary();
			// google-http-client errors if elements have no xml namespace if we don't do this
			namespaceDictionary.set("", "");
			request.setParser(new XmlObjectParser(namespaceDictionary));
		}
	};

	private final KeyProvider keyProvider;
	private final String deviceId;
	private final CacheProvider cacheProvider;
	private final HttpRequestFactory requestFactory;

	public LastPassLoginProvider(KeyProvider keyProvider, String deviceId, CacheProvider cacheProvider, HttpTransport transport) {
		this.keyProvider = keyProvider;
		this.deviceId = deviceId;
		this.cacheProvider = cacheProvider;
		this.requestFactory = transport.createRequestFactory(XML_REQUEST_INITIALIZER);
	}

	public LoginResult login(String username, String password, final String otp, final String trustLabel, final int iterations)
			throws GeneralSecurityException, IOException {
		if (this.deviceId == null && trustLabel != null)
			throw new IllegalArgumentException("Cannot specify a trusted device label if no device ID was provided");

		return login(username, password, otp, trustLabel, iterations, false);
	}

	private LoginResult login(String username, String password, final String otp, final String trustLabel, final int iterations,
			boolean serverProvided) throws GeneralSecurityException, IOException {
		LOGGER.debug("Sending login request (serverProvided: {})", serverProvided);
		
		byte[] key = this.keyProvider.getKey(username, password, iterations);
		final String hash = this.keyProvider.getHash(key, password, iterations);
		final Map<String, Object> options = new HashMap<String, Object>();
		options.put("method", "cr");
		options.put("web", "1");
		options.put("xml", "2");
		options.put("username", username);
		options.put("hash", hash);
		if (this.deviceId != null) {
			options.put("uuid", this.deviceId);
		}
		if (otp != null) {
			options.put("otp", otp);
			if (trustLabel != null) {
				options.put("trustlabel", trustLabel);
			}
		}
		options.put("iterations", iterations);

		final LastPassResponse response;
		final HttpResponse clientResponse;
		try {
			clientResponse = this.requestFactory.buildPostRequest(new GenericUrl("https://lastpass.com/login.php"),
					new UrlEncodedContent(options)).execute();
			response = clientResponse.parseAs(LastPassResponse.class);
		} catch (final UnknownHostException e) {
			LOGGER.debug("Unknown host, attempting offline login", e);
			return offlineLogin(username, iterations, key, e);
		} catch (final ConnectException e) {
			LOGGER.debug("Connect exception, attempting offline login", e);
			return offlineLogin(username, iterations, key, e);
		} catch (final RuntimeException e) {
			throw new LastPassException("Error parsing login response: " + e.getMessage(), e);
		}

		if (response != null) {
			// Try interpreting it as an OK response
			LastPassOk ok = response.getOk();
			if (ok != null) {
				LOGGER.debug("Login successful: {}", ok);
				return new LoginResult(ok.getSessionId(), key, ok.getAccountsVersion(), iterations);
			}

			// Try interpreting it as an error response
			final LastPassError error = response.getError();
			if (error != null) {
				LOGGER.debug("Login unsuccessful: {}", error);
				// If the error is caused by using the incorrect number of
				// iterations in the encryption, try again with the correct number
				if (error.getIterations() != null && error.getIterations().intValue() != iterations) {
					if (serverProvided)
						throw new IllegalStateException("Expected " + iterations + " iterations but response indicated "
								+ error.getIterations());
					return login(username, password, otp, trustLabel, error.getIterations(), true);
				} else
					throw new ErrorResponseException(error);
			}
		}
		throw new LastPassException("No error found but unsuccessful response: " + clientResponse + " (" + response + ")");
	}

	private LoginResult offlineLogin(String username, int iterations, byte[] key, IOException e) throws IOException {
		Integer cachedIterations = this.cacheProvider.getIterations(username);
		Integer cachedAccountsVersion = this.cacheProvider.getAccountVersion(username);
		if (cachedIterations != null && cachedAccountsVersion != null && cachedIterations.intValue() == iterations) {
			return new LoginResult(null, key, cachedAccountsVersion, iterations);
		}
		throw e;
	}
}
