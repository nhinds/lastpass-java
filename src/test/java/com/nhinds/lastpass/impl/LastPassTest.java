package com.nhinds.lastpass.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Test;

import com.nhinds.lastpass.LastPass.PasswordStoreBuilder;
import com.nhinds.lastpass.impl.dto.reader.DtoReader;
import com.sun.jersey.api.client.Client;

public class LastPassTest {
	@Test
	public void getPasswordStoreBuilderReturnsConfiguredBuilder() {
		final Client client = mock(Client.class);
		final PasswordStoreBuilder passwordStoreBuilder = new LastPassImpl(client).getPasswordStoreBuilder("u", "b", new File("cache"));
		assertEquals(new LastPassBuilderImpl(client, "u", "b", new File("cache"), new PBKDF2SHA256KeyProvider(), new DtoReader()),
				passwordStoreBuilder);
	}
}
