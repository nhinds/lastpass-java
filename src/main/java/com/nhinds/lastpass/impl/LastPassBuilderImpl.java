package com.nhinds.lastpass.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Objects;
import com.nhinds.lastpass.GoogleAuthenticatorRequired;
import com.nhinds.lastpass.LastPass.PasswordStoreBuilder;
import com.nhinds.lastpass.LastPass.ProgressListener;
import com.nhinds.lastpass.LastPass.ProgressStatus;
import com.nhinds.lastpass.LastPassException;
import com.nhinds.lastpass.PasswordStore;
import com.nhinds.lastpass.impl.LastPassLoginProvider.LoginResult;

class LastPassBuilderImpl implements PasswordStoreBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(LastPassBuilderImpl.class);
	
	static final String ACCOUNT_DATA_URL = "https://lastpass.com/getaccts.php?mobile=1&hash=0.0";
	static final String SESSION_COOKIE_NAME = "PHPSESSID";

	private final HttpRequestFactory requestFactory;
	private final String username;
	private final String password;
	private final CacheProvider cacheProvider;
	private final LastPassLoginProvider loginProvider;
	private final PasswordStoreFactory passwordStoreFactory;

	public LastPassBuilderImpl(final HttpTransport transport, final String username, final String password,
			final CacheProvider cacheProvider, final LastPassLoginProvider loginProvider) {
		this(transport, username, password, cacheProvider, loginProvider, new PasswordStoreFactory());
	}

	LastPassBuilderImpl(final HttpTransport transport, final String username, final String password,
			final CacheProvider cacheProvider, final LastPassLoginProvider loginProvider, final PasswordStoreFactory passwordStoreFactory) {
		this.username = username;
		this.password = password;
		this.cacheProvider = cacheProvider;
		this.passwordStoreFactory = passwordStoreFactory;
		this.requestFactory = transport.createRequestFactory();
		this.loginProvider = loginProvider;
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
		return Objects.equal(this.username, other.username) && Objects.equal(this.password, other.password);
	}

	@Override
	public PasswordStore getPasswordStore(final ProgressListener listener) throws GoogleAuthenticatorRequired {
		try {
			return getPasswordStore(null, null, listener);
		} catch (final ErrorResponseException e) {
			if ("googleauthrequired".equals(e.getError().getCause()))
				throw new GoogleAuthenticatorRequired(e.getError().getMessage(), e.getCause());
			throw e;
		}
	}

	@Override
	public PasswordStore getPasswordStore(final String otp, final String trustLabel, final ProgressListener listener) {
		try {
			if (listener != null)
				listener.statusChanged(ProgressStatus.LOGGING_IN);

			final LoginResult loginResult = this.loginProvider.login(this.username, this.password, otp, trustLabel, getIterations());

			InputStream accountData = getCachedAccountData(loginResult, loginResult.getAccountsVersion(), loginResult.getIterations());
			if (accountData == null) {
				LOGGER.debug("No cached account data found");
				if (loginResult.getSessionId() == null)
					throw new LastPassException("LastPass is offline and no cached account data is available");
				if (listener != null)
					listener.statusChanged(ProgressStatus.RETRIEVING);

				final HttpRequest request = this.requestFactory.buildGetRequest(new GenericUrl(ACCOUNT_DATA_URL));
				request.getHeaders().setCookie(SESSION_COOKIE_NAME + '=' + loginResult.getSessionId());
				final HttpResponse response = request.execute();
				accountData = new CachingInputStream(loginResult, response.getContent());
				LOGGER.debug("Account data retrieved");
			}

			if (listener != null)
				listener.statusChanged(ProgressStatus.DECRYPTING);

			return this.passwordStoreFactory.getPasswordStore(accountData, new AESCBCDecryptionProvider(loginResult.getKey()));
		} catch (final IOException e) {
			throw new LastPassException("Error connecting to LastPass: " + e.getMessage(), e);
		} catch (final GeneralSecurityException e) {
			throw new LastPassException(e);
		}
	}

	private InputStream getCachedAccountData(final LoginResult loginResult, final int accountsVersion, final int iterations) {
		try {
			final Integer cachedAccountsVersion = this.cacheProvider.getAccountVersion(this.username);
			if (cachedAccountsVersion != null && cachedAccountsVersion.equals(accountsVersion) && getIterations() == iterations) {
				LOGGER.debug("Cached account data found");
				return this.cacheProvider.getAccountData(this.username);
			}
		} catch (IOException ignore) {
		}
		return null;
	}

	private int getIterations() throws IOException {
		Integer cachedIterations;
		try {
			cachedIterations = this.cacheProvider.getIterations(this.username);
		} catch (IOException ignore) {
			cachedIterations = null;
		}
		return cachedIterations == null ? 1 : cachedIterations;
	}

	private class CachingInputStream extends FilterInputStream {
		private final ByteArrayOutputStream accountDataCopy = new ByteArrayOutputStream();
		private final LoginResult loginResult;

		public CachingInputStream(LoginResult loginResult, InputStream content) {
			super(content);
			this.loginResult = loginResult;
		}

		@Override
		public int read() throws IOException {
			int read = super.read();
			if (read != -1)
				this.accountDataCopy.write(read);
			return read;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = super.read(b, off, len);
			if (read != -1)
				this.accountDataCopy.write(b, off, read);
			return read;
		}
		
		@Override
		public long skip(long bytesToSkip) throws IOException {
			byte[] skipped = new byte[(int) bytesToSkip];
			return read(skipped);
		}

		@Override
		public void close() throws IOException {
			super.close();
			InputStream accountData = new ByteArrayInputStream(this.accountDataCopy.toByteArray());
			LastPassBuilderImpl.this.cacheProvider.storeAccountData(LastPassBuilderImpl.this.username, this.loginResult.getIterations(),
					this.loginResult.getAccountsVersion(), accountData);
		}
		
		// Simple implementation, does not support mark or reset
		
		@Override
		public boolean markSupported() {
			return false;
		}
		
		@Override
		public void mark(int readlimit) {
		}
		
		@Override
		public synchronized void reset() throws IOException {
			throw new IOException("mark/reset not supported");
		}
	}

	static class PasswordStoreFactory {
		public PasswordStore getPasswordStore(final InputStream accountsStream, final DecryptionProvider decryptionProvider) {
			return new PasswordStoreImpl(accountsStream, decryptionProvider);
		}
	}
}
