package com.nhinds.lastpass.impl;

import java.io.File;
import java.security.GeneralSecurityException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Strings;
import com.nhinds.lastpass.GoogleAuthenticatorRequired;
import com.nhinds.lastpass.LastPass;
import com.nhinds.lastpass.PasswordStore;

public class LastPassImpl implements LastPass {

	private final HttpTransport transport;

	public LastPassImpl() {
		this(new NetHttpTransport());
	}

	public LastPassImpl(final HttpTransport transport) {
		this.transport = transport;
	}

	@Override
	public PasswordStoreBuilder getPasswordStoreBuilder(final String username, final String password, final File cacheFile,
			final String deviceId) {
		return new LastPassBuilderImpl(this.transport, username, password, cacheFile, new LastPassLoginProvider(
				new PBKDF2SHA256KeyProvider(), deviceId, this.transport));
	}

	public static void main(final String[] args) throws GeneralSecurityException {
		final String deviceId = Strings.emptyToNull(System.console().readLine("Enter \"device identifier\" (or keep blank for none): "));
		final String username = System.console().readLine("Enter username: ");
		final String password = String.valueOf(System.console().readPassword("Enter password for %s: ", username));
		final LastPassImpl lastPass = new LastPassImpl();
		PasswordStore passwordStore;
		final PasswordStoreBuilder passwordStoreBuilder = lastPass.getPasswordStoreBuilder(username, password, null, deviceId);
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
