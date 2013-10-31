package com.nhinds.lastpass.impl;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.nhinds.lastpass.PasswordInfo;

@RunWith(MockitoJUnitRunner.class)
public class PasswordStoreTest {
	private final Map<Long, AccountData> accounts = new HashMap<Long, AccountData>();
	private final Map<String, Collection<String>> domains = new HashMap<String, Collection<String>>();
	@Mock
	private PasswordStoreReader passwordStoreReader;

	private PasswordStoreImpl passwordStore;

	@Before
	public void setup() {
		when(this.passwordStoreReader.getAccounts()).thenReturn(this.accounts);
		when(this.passwordStoreReader.getDomains()).thenReturn(this.domains);
		this.passwordStore = new PasswordStoreImpl(this.passwordStoreReader);
	}

	@Test
	public void getById() {
		final AccountData accountData = mock(AccountData.class);
		this.accounts.put(17L, accountData);

		final PasswordInfo password = this.passwordStore.getPassword(17);

		assertEquals(accountData, password);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getByIdThrowsExceptionOnInvalidId() {
		this.passwordStore.getPassword(18);
	}

	@Test
	public void listPasswords() {
		final AccountData accountData = mock(AccountData.class);
		final AccountData accountData2 = mock(AccountData.class);
		this.accounts.put(1L, accountData);
		this.accounts.put(2L, accountData2);

		final Collection<? extends PasswordInfo> passwords = this.passwordStore.getPasswords();

		assertThat(passwords, containsInAnyOrder((PasswordInfo) accountData, accountData2));
	}

	@Test
	public void listEmptyPasswords() {
		this.accounts.clear();
		final Collection<? extends PasswordInfo> passwords = this.passwordStore.getPasswords();
		assertThat(passwords, is(empty()));
	}

	@Test
	public void getCandidateDomainsReturnsSingleDomainWhenMissingHostname() {
		final Collection<String> candidateDomains = this.passwordStore.getCandidateDomains("example.com");
		assertThat(candidateDomains, contains("example.com"));
	}

	@Test
	public void getCandidateDomainsReturnsCandidatesForHostname() {
		this.domains.put("example.com", Arrays.asList("example.com", "foo.com"));

		final Collection<String> candidateDomains = this.passwordStore.getCandidateDomains("example.com");
		assertThat(candidateDomains, containsInAnyOrder("example.com", "foo.com"));
	}

	@Test
	public void getCandidateDomainsReturnsCandidatesForSubdomain() {
		this.domains.put("example.com", Arrays.asList("example.com", "foo.com"));

		final Collection<String> candidateDomains = this.passwordStore.getCandidateDomains("foo.example.com");
		assertThat(candidateDomains, containsInAnyOrder("example.com", "foo.com"));
	}

	@Test
	public void getCandidateDomainsReturnsCandidatesForIP() {
		this.domains.put("1.2.3.4", Arrays.asList("1.2.3.4", "foo.com"));

		final Collection<String> candidateDomains = this.passwordStore.getCandidateDomains("1.2.3.4");
		assertThat(candidateDomains, containsInAnyOrder("1.2.3.4", "foo.com"));
	}

	@Test
	public void getCandidateDomainsReturnsCandidatesForUnqualifiedHostname() {
		this.domains.put("host", Arrays.asList("host", "foo.com"));

		final Collection<String> candidateDomains = this.passwordStore.getCandidateDomains("host");
		assertThat(candidateDomains, containsInAnyOrder("host", "foo.com"));
	}

	@Test
	public void getPasswordsByHostnameReturnsOnlyMatchingUrls() {
		this.domains.put("myhost.com", Arrays.asList("myhost.com", "foo.com", "hostonly", "1.2.3.4"));
		// Matching urls
		final AccountData domainMatch = mock(AccountData.class);
		when(domainMatch.getUrl()).thenReturn("http://myhost.com");
		final AccountData domainAlias = mock(AccountData.class);
		when(domainAlias.getUrl()).thenReturn("http://foo.com/somepath");
		final AccountData subdomainMatch = mock(AccountData.class);
		when(subdomainMatch.getUrl()).thenReturn("http://sub.domain.of.foo.com/path?and=query&string");
		final AccountData hostMatch = mock(AccountData.class);
		when(hostMatch.getUrl()).thenReturn("http://hostonly#");
		final AccountData ipMatch = mock(AccountData.class);
		when(ipMatch.getUrl()).thenReturn("https://1.2.3.4");
		this.accounts.putAll(ImmutableMap.of(1L, domainMatch, 2L, domainAlias, 3L, subdomainMatch, 4L, hostMatch, 5L, ipMatch));
		// Non-matching urls
		final AccountData domainMismatch = mock(AccountData.class);
		when(domainMismatch.getUrl()).thenReturn("http://example.com");
		final AccountData subdomainMismatch = mock(AccountData.class);
		when(subdomainMismatch.getUrl()).thenReturn("http://sub.domain.com");
		final AccountData hostMismatch = mock(AccountData.class);
		when(hostMismatch.getUrl()).thenReturn("http://otherhost");
		final AccountData ipMismatch = mock(AccountData.class);
		when(ipMismatch.getUrl()).thenReturn("https://1.2.3.5");
		final AccountData invalidUrl = mock(AccountData.class);
		when(invalidUrl.getUrl()).thenReturn(":notvalid:url!");
		final AccountData invalidUrl2 = mock(AccountData.class);
		when(invalidUrl2.getUrl()).thenReturn("notvalidurl");
		this.accounts.putAll(ImmutableMap.of(11L, domainMismatch, 12L, ipMismatch, 13L, subdomainMismatch, 14L, hostMismatch, 15L,
				invalidUrl));
		this.accounts.put(16L, invalidUrl2);

		final Collection<PasswordInfo> passwords = this.passwordStore.getPasswordsByHostname("myhost.com");

		assertThat(passwords, containsInAnyOrder((PasswordInfo) domainMatch, domainAlias, subdomainMatch, hostMatch, ipMatch));
	}
}
