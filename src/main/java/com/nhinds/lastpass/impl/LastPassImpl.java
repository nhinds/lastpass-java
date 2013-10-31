package com.nhinds.lastpass.impl;

import java.io.File;
import java.security.GeneralSecurityException;

import com.nhinds.lastpass.GoogleAuthenticatorRequired;
import com.nhinds.lastpass.LastPass;
import com.nhinds.lastpass.PasswordStore;
import com.nhinds.lastpass.impl.dto.reader.DtoReader;
import com.sun.jersey.api.client.Client;

public class LastPassImpl implements LastPass {

	private final Client client;

	public LastPassImpl() {
		this(Client.create());
	}

	public LastPassImpl(final Client client) {
		this.client = client;
	}

	@Override
	public PasswordStoreBuilder getPasswordStoreBuilder(final String username, final String password, final File cacheFile) {
		return new LastPassBuilderImpl(this.client, username, password, cacheFile, new PBKDF2SHA256KeyProvider(), new DtoReader());
	}

	public static void main(final String[] args) throws GeneralSecurityException {
		final String username = System.console().readLine("Enter username: ");
		final String password = String.valueOf(System.console().readPassword("Enter password for %s: ", username));
		final LastPassImpl lastPass = new LastPassImpl();
		PasswordStore passwordStore;
		final PasswordStoreBuilder passwordStoreBuilder = lastPass.getPasswordStoreBuilder(username, password, null);
		try {
			passwordStore = passwordStoreBuilder.getPasswordStore();
		} catch (final GoogleAuthenticatorRequired e) {
			final String otp = System.console().readLine("Enter OTP: ");
			passwordStore = passwordStoreBuilder.getPasswordStore(otp);
		}
		System.out.println(passwordStore.getPasswords());
	}
}
