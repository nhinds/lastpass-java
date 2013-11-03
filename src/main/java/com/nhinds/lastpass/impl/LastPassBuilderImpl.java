package com.nhinds.lastpass.impl;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

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
	private static final String SESSION_COOKIE_NAME = "PHPSESSID";

	private final HttpRequestFactory requestFactory;
	private final String username;
	private final String password;
	private final File cacheFile;// TODO use me
	private final LastPassLoginProvider loginProvider;

	public LastPassBuilderImpl(final HttpTransport transport, final String username, final String password, final File cacheFile,
			final LastPassLoginProvider loginProvider) {
		this.username = username;
		this.password = password;
		this.cacheFile = cacheFile;
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
		return Objects.equal(this.username, other.username) && Objects.equal(this.password, other.password)
				&& Objects.equal(this.cacheFile, other.cacheFile);
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
			// try {
			// TODO
			// if (cacheFile.isFile())
			// return new PasswordStoreImpl(new FileInputStream(cacheFile), getKey(username, password, ???));
			if (listener != null)
				listener.statusChanged(ProgressStatus.LOGGING_IN);

			final LoginResult loginResult = this.loginProvider.login(this.username, this.password, otp, trustLabel, 1);

			if (listener != null)
				listener.statusChanged(ProgressStatus.RETRIEVING);

			final HttpRequest request = this.requestFactory
					.buildGetRequest(new GenericUrl("https://lastpass.com/getaccts.php?mobile=1&hash=0.0"));
			request.getHeaders().setCookie(SESSION_COOKIE_NAME + '=' + loginResult.getSessionId());
			final HttpResponse response = request.execute();
			// clientResponse.bufferEntity();
			// final InputStream entityInputStream = clientResponse.getEntityInputStream();
			// try (FileOutputStream out = new FileOutputStream(cache)) {
			// IOUtils.copy(entityInputStream, out);
			// }
			// entityInputStream.reset();

			if (listener != null)
				listener.statusChanged(ProgressStatus.DECRYPTING);

			return new PasswordStoreImpl(response.getContent(), new AESCBCDecryptionProvider(loginResult.getKey()));
			// } catch (final IOException e) {
			// throw new LastPassException(e);
			// }
		} catch (final IOException e) {
			throw new LastPassException("Error connecting to LastPass: " + e.getMessage(), e);
		} catch (final GeneralSecurityException e) {
			throw new LastPassException(e);
		}
	}
}
