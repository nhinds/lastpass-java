package com.nhinds.lastpass.impl;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.common.net.InternetDomainName;
import com.nhinds.lastpass.PasswordInfo;
import com.nhinds.lastpass.PasswordStore;

public class PasswordStoreImpl implements PasswordStore {
	private final PasswordStoreReader passwordStoreReader;

	public PasswordStoreImpl(final InputStream accountsStream, final DecryptionProvider decryptionProvider) {
		this(new PasswordStoreReader(accountsStream, decryptionProvider));
	}

	PasswordStoreImpl(final PasswordStoreReader passwordStoreReader) {
		this.passwordStoreReader = passwordStoreReader;
	}

	@Override
	public Collection<? extends PasswordInfo> getPasswords() {
		return this.passwordStoreReader.getAccounts().values();
	}

	@Override
	public PasswordInfo getPassword(final long id) {
		final AccountData accountData = this.passwordStoreReader.getAccounts().get(id);
		if (accountData == null)
			throw new IllegalArgumentException("Unknown account " + id);
		return accountData;
	}

	@Override
	public Collection<PasswordInfo> getPasswordsByHostname(final String hostname) {
		final Collection<String> candidateDomains = getCandidateDomains(hostname);
		final Collection<PasswordInfo> passwordsForUrl = new ArrayList<PasswordInfo>();
		for (final AccountData account : this.passwordStoreReader.getAccounts().values()) {
			String accountUrl;
			try {
				final String host = new URI(account.getUrl()).getHost();
				if (host != null) {
					accountUrl = resolveHost(host);
					if (candidateDomains.contains(accountUrl)) {
						passwordsForUrl.add(account);
					}
				}
			} catch (final URISyntaxException e) {

			}
		}
		return passwordsForUrl;
	}

	Collection<String> getCandidateDomains(final String host) {
		final String resolvedName = resolveHost(host);
		final Collection<String> candidateDomains = this.passwordStoreReader.getDomains().get(resolvedName);
		if (candidateDomains != null)
			return candidateDomains;
		return Collections.singleton(host);
	}

	private String resolveHost(final String host) {
		try {
			InternetDomainName domainName = InternetDomainName.from(host);
			if (domainName.isUnderPublicSuffix()) {
				domainName = domainName.topPrivateDomain();
			}
			return domainName.name();
		} catch (final IllegalArgumentException e) {
			return host;
		}
	}

}
