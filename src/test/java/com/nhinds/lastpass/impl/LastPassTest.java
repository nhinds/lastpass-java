package com.nhinds.lastpass.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.junit.Test;

import com.google.api.client.http.HttpTransport;
import com.nhinds.lastpass.LastPass.PasswordStoreBuilder;

public class LastPassTest {
	@Test
	public void getPasswordStoreBuilderReturnsConfiguredBuilder() {
		final HttpTransport transport = mock(HttpTransport.class);
		final PasswordStoreBuilder passwordStoreBuilder = new LastPassImpl(transport).getPasswordStoreBuilder("u", "b", new File("cache"),
				"id");
		assertEquals(new LastPassBuilderImpl(transport, "u", "b", null, null), passwordStoreBuilder);
	}
}
