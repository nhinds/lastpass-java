package com.nhinds.lastpass.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.api.client.http.HttpTransport;

@RunWith(MockitoJUnitRunner.class)
public class LastPassBuilderTest {
	@Mock
	private HttpTransport transport;

	@Test
	public void getPasswordStore() throws Exception {
		// TODO
	}
}
