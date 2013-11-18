package com.nhinds.lastpass.impl;

import java.security.GeneralSecurityException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.nhinds.lastpass.GoogleAuthenticatorRequired;
import com.nhinds.lastpass.LastPass;
import com.nhinds.lastpass.LastPassFactory;
import com.nhinds.lastpass.PasswordStore;

public class LastPassImpl implements LastPass {

	private final HttpTransport transport;
	private final CacheProvider cacheProvider;

	public LastPassImpl(final CacheProvider cacheProvider) {
		this(cacheProvider, new NetHttpTransport());
	}

	public LastPassImpl(final CacheProvider cacheProvider, final HttpTransport transport) {
		this.cacheProvider = Preconditions.checkNotNull(cacheProvider);
		this.transport = Preconditions.checkNotNull(transport);
	}

	@Override
	public PasswordStoreBuilder getPasswordStoreBuilder(final String username, final String password,
			final String deviceId) {
		final LastPassLoginProvider loginProvider = new LastPassLoginProvider(new PBKDF2SHA256KeyProvider(), deviceId, this.cacheProvider,
				this.transport);
		return new LastPassBuilderImpl(this.transport, username, password, this.cacheProvider, loginProvider);
	}

	@Override
	public int hashCode() {
		return this.cacheProvider.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LastPassImpl))
			return false;
		return Objects.equal(this.cacheProvider, ((LastPassImpl) obj).cacheProvider);
	}

	public static void main(final String[] args) throws GeneralSecurityException {
		final String deviceId = Strings.emptyToNull(System.console().readLine("Enter \"device identifier\" (or keep blank for none): "));
		final String username = System.console().readLine("Enter username: ");
		final String password = String.valueOf(System.console().readPassword("Enter password for %s: ", username));
		final LastPass lastPass = LastPassFactory.getLastPass();
		PasswordStore passwordStore;
		final PasswordStoreBuilder passwordStoreBuilder = lastPass.getPasswordStoreBuilder(username, password, deviceId);
		try {
			passwordStore = passwordStoreBuilder.getPasswordStore(null);
		} catch (final GoogleAuthenticatorRequired e) {
			final String otp = System.console().readLine("Enter OTP: ");
			final String trustLabel;
			if (deviceId != null)
				trustLabel = Strings.emptyToNull(System.console().readLine("Enter trust label (or keep blank for none): "));
			else
				trustLabel = null;
			passwordStore = passwordStoreBuilder.getPasswordStore(otp, trustLabel, null);
		}
		System.out.println(passwordStore.getPasswords());
	}
}
