package com.nhinds.lastpass.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.common.io.ByteStreams;
import com.nhinds.lastpass.LastPass.ProgressListener;
import com.nhinds.lastpass.LastPass.ProgressStatus;
import com.nhinds.lastpass.PasswordStore;
import com.nhinds.lastpass.encryption.AES256EncryptionProvider;
import com.nhinds.lastpass.encryption.EncryptionProvider;
import com.nhinds.lastpass.impl.LastPassBuilderImpl.PasswordStoreFactory;
import com.nhinds.lastpass.impl.LastPassLoginProvider.LoginResult;

@RunWith(MockitoJUnitRunner.class)
public class LastPassBuilderTest {
	private static final String USERNAME = "user";
	private static final String PASSWORD = "password";
	private static final byte[] KEY = { 5, 6, 17 };

	@Mock
	private CacheProvider cacheProvider;
	@Mock
	private LastPassLoginProvider loginProvider;
	@Mock
	private PasswordStoreFactory passwordStoreFactory;
	@Mock
	private HttpHeaders httpHeaders;
	@Mock
	private LowLevelHttpRequest httpRequest;
	@Mock
	private LowLevelHttpResponse httpResponse;

	private LastPassBuilderImpl lastPassBuilder;

	@Before
	public void setup() throws Exception {
		final HttpTransport transport = new MockHttpTransport() {
			@Override
			public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
				assertEquals(LastPassBuilderImpl.ACCOUNT_DATA_URL, url);
				return LastPassBuilderTest.this.httpRequest;
			}
		};
		when(this.httpRequest.execute()).thenReturn(this.httpResponse);
		when(this.httpResponse.getStatusCode()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);

		this.lastPassBuilder = new LastPassBuilderImpl(transport, USERNAME, PASSWORD, this.cacheProvider, this.loginProvider,
				this.passwordStoreFactory);
	}

	@Test
	public void getUncachedPasswordStoreWithoutOtpOrListener() throws Exception {
		when(this.cacheProvider.getAccountVersion(anyString())).thenReturn(null);
		when(this.cacheProvider.getIterations(anyString())).thenReturn(null);
		when(this.cacheProvider.getAccountData(anyString())).thenReturn(null);

		final byte[] content = { 1 };
		when(this.loginProvider.login(USERNAME, PASSWORD, null, null, 1)).thenReturn(new LoginResult("789", KEY, 11, 12));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));
		final PasswordStore mockPasswordStore = mock(PasswordStore.class);
		when(this.passwordStoreFactory.getPasswordStore(any(InputStream.class), any(EncryptionProvider.class))).thenReturn(
				mockPasswordStore);

		PasswordStore passwordStore = this.lastPassBuilder.getPasswordStore(null);
		assertEquals(mockPasswordStore, passwordStore);

		verify(this.httpRequest).addHeader("Cookie", LastPassBuilderImpl.SESSION_COOKIE_NAME + "=789");

		verifyKeyAndReadAccountInputStream();
		verifyStoreAccountData(12, 11, content);
	}

	@Test
	public void getUncachedPasswordStoreWithoutOtpWithListener() throws Exception {
		final ProgressListener listener = mock(ProgressListener.class);

		when(this.cacheProvider.getAccountVersion(anyString())).thenReturn(null);
		when(this.cacheProvider.getIterations(anyString())).thenReturn(null);
		when(this.cacheProvider.getAccountData(anyString())).thenReturn(null);

		final byte[] content = { 1 };
		when(this.loginProvider.login(USERNAME, PASSWORD, null, null, 1)).thenReturn(new LoginResult("500", KEY, 11, 12));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));

		this.lastPassBuilder.getPasswordStore(listener);

		InOrder inOrder = inOrder(this.loginProvider, listener, this.httpRequest, this.passwordStoreFactory);
		inOrder.verify(listener).statusChanged(ProgressStatus.LOGGING_IN);
		inOrder.verify(this.loginProvider).login(USERNAME, PASSWORD, null, null, 1);
		inOrder.verify(listener).statusChanged(ProgressStatus.RETRIEVING);
		inOrder.verify(this.httpRequest).execute();
		inOrder.verify(listener).statusChanged(ProgressStatus.DECRYPTING);
		inOrder.verify(this.passwordStoreFactory).getPasswordStore(any(InputStream.class), any(EncryptionProvider.class));
	}

	@Test
	public void getUncachedPasswordStoreWithOtpWithoutListener() throws Exception {
		when(this.cacheProvider.getAccountVersion(anyString())).thenReturn(null);
		when(this.cacheProvider.getIterations(anyString())).thenReturn(null);
		when(this.cacheProvider.getAccountData(anyString())).thenReturn(null);

		final byte[] content = { 4, 5, 6 };
		when(this.loginProvider.login(USERNAME, PASSWORD, "otp", "trustMe", 1)).thenReturn(new LoginResult("12345", KEY, 65, 55));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));
		final PasswordStore mockPasswordStore = mock(PasswordStore.class);
		when(this.passwordStoreFactory.getPasswordStore(any(InputStream.class), any(EncryptionProvider.class))).thenReturn(
				mockPasswordStore);

		PasswordStore passwordStore = this.lastPassBuilder.getPasswordStore("otp", "trustMe", null);
		assertEquals(mockPasswordStore, passwordStore);

		verify(this.httpRequest).addHeader("Cookie", LastPassBuilderImpl.SESSION_COOKIE_NAME + "=12345");

		verifyKeyAndReadAccountInputStream();
		verifyStoreAccountData(55, 65, content);
	}

	@Test
	public void getUncachedPasswordStoreWithOtpAndListener() throws Exception {
		final ProgressListener listener = mock(ProgressListener.class);

		when(this.cacheProvider.getAccountVersion(anyString())).thenReturn(null);
		when(this.cacheProvider.getIterations(anyString())).thenReturn(null);
		when(this.cacheProvider.getAccountData(anyString())).thenReturn(null);

		final byte[] content = { 4, 5, 6 };
		when(this.loginProvider.login(USERNAME, PASSWORD, "otp", null, 1)).thenReturn(new LoginResult("12345", KEY, 65, 55));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));

		this.lastPassBuilder.getPasswordStore("otp", null, listener);

		InOrder inOrder = inOrder(this.loginProvider, listener, this.httpRequest, this.passwordStoreFactory);
		inOrder.verify(listener).statusChanged(ProgressStatus.LOGGING_IN);
		inOrder.verify(this.loginProvider).login(USERNAME, PASSWORD, "otp", null, 1);
		inOrder.verify(listener).statusChanged(ProgressStatus.RETRIEVING);
		inOrder.verify(this.httpRequest).execute();
		inOrder.verify(listener).statusChanged(ProgressStatus.DECRYPTING);
		inOrder.verify(this.passwordStoreFactory).getPasswordStore(any(InputStream.class), eq(new AES256EncryptionProvider(KEY)));
	}

	@Test
	public void getCachedPasswordStoreWithoutListener() throws Exception {
		final byte[] content = { 55, 102, 99, 100 };
		when(this.cacheProvider.getAccountVersion(USERNAME)).thenReturn(5);
		when(this.cacheProvider.getIterations(USERNAME)).thenReturn(5123);
		final ByteArrayInputStream accountDataStream = new ByteArrayInputStream(content);
		when(this.cacheProvider.getAccountData(USERNAME)).thenReturn(accountDataStream);

		when(this.loginProvider.login(USERNAME, PASSWORD, "myOtp", null, 5123)).thenReturn(new LoginResult("66", KEY, 5, 5123));

		this.lastPassBuilder.getPasswordStore("myOtp", null, null);

		verify(this.passwordStoreFactory).getPasswordStore(accountDataStream, new AES256EncryptionProvider(KEY));
		verify(this.cacheProvider, never()).storeAccountData(anyString(), anyInt(), anyInt(), any(InputStream.class));
	}

	@Test
	public void getCachedPasswordStoreWithListener() throws Exception {
		final ProgressListener listener = mock(ProgressListener.class);
		final byte[] content = { 55, 102, 99, 100 };
		when(this.cacheProvider.getAccountVersion(USERNAME)).thenReturn(5);
		when(this.cacheProvider.getIterations(USERNAME)).thenReturn(5123);
		final ByteArrayInputStream accountDataStream = new ByteArrayInputStream(content);
		when(this.cacheProvider.getAccountData(USERNAME)).thenReturn(accountDataStream);

		when(this.loginProvider.login(USERNAME, PASSWORD, "myOtp", null, 5123)).thenReturn(new LoginResult("66", KEY, 5, 5123));

		this.lastPassBuilder.getPasswordStore("myOtp", null, listener);

		final InOrder inOrder = inOrder(this.loginProvider, listener, this.httpRequest, this.passwordStoreFactory);
		inOrder.verify(listener).statusChanged(ProgressStatus.LOGGING_IN);
		inOrder.verify(this.loginProvider).login(USERNAME, PASSWORD, "myOtp", null, 5123);
		inOrder.verify(listener).statusChanged(ProgressStatus.DECRYPTING);
		inOrder.verify(this.passwordStoreFactory).getPasswordStore(any(InputStream.class), eq(new AES256EncryptionProvider(KEY)));

		verify(this.passwordStoreFactory).getPasswordStore(accountDataStream, new AES256EncryptionProvider(KEY));

		verify(listener, never()).statusChanged(ProgressStatus.RETRIEVING);
		verify(this.httpRequest, never()).execute();
		verify(this.cacheProvider, never()).storeAccountData(anyString(), anyInt(), anyInt(), any(InputStream.class));
	}

	@Test
	public void getExpiredCachedPasswordStoreWithoutListener() throws Exception {
		final byte[] cachedContent = { 1 };
		final byte[] content = { 2 };
		when(this.cacheProvider.getAccountVersion(USERNAME)).thenReturn(5);
		when(this.cacheProvider.getIterations(USERNAME)).thenReturn(5123);
		when(this.cacheProvider.getAccountData(USERNAME)).thenReturn(new ByteArrayInputStream(cachedContent));

		when(this.loginProvider.login(USERNAME, PASSWORD, "myOtp", null, 5123)).thenReturn(new LoginResult("67", KEY, 16, 5123));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));

		this.lastPassBuilder.getPasswordStore("myOtp", null, null);

		verify(this.httpRequest).addHeader("Cookie", LastPassBuilderImpl.SESSION_COOKIE_NAME + "=67");

		verifyKeyAndReadAccountInputStream();
		verifyStoreAccountData(5123, 16, content);
	}

	@Test
	public void getExpiredCachedPasswordStoreWithListener() throws Exception {
		final ProgressListener listener = mock(ProgressListener.class);
		final byte[] cachedContent = { 55, 102, 99, 100 };
		final byte[] content = { 0 };
		when(this.cacheProvider.getAccountVersion(USERNAME)).thenReturn(50);
		when(this.cacheProvider.getIterations(USERNAME)).thenReturn(5123);
		when(this.cacheProvider.getAccountData(USERNAME)).thenReturn(new ByteArrayInputStream(cachedContent));

		when(this.loginProvider.login(USERNAME, PASSWORD, "myOtp", null, 5123)).thenReturn(new LoginResult("610", KEY, 50, 4));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));

		this.lastPassBuilder.getPasswordStore("myOtp", null, listener);

		final InOrder inOrder = inOrder(this.loginProvider, listener, this.httpRequest, this.passwordStoreFactory);
		inOrder.verify(listener).statusChanged(ProgressStatus.LOGGING_IN);
		inOrder.verify(this.loginProvider).login(USERNAME, PASSWORD, "myOtp", null, 5123);
		inOrder.verify(listener).statusChanged(ProgressStatus.RETRIEVING);
		inOrder.verify(this.httpRequest).addHeader("Cookie", LastPassBuilderImpl.SESSION_COOKIE_NAME + "=610");
		inOrder.verify(this.httpRequest).execute();
		inOrder.verify(listener).statusChanged(ProgressStatus.DECRYPTING);
		inOrder.verify(this.passwordStoreFactory).getPasswordStore(any(InputStream.class), eq(new AES256EncryptionProvider(KEY)));

		verifyKeyAndReadAccountInputStream();
		verifyStoreAccountData(4, 50, content);
	}

	@Test
	public void skippedBytesAreWrittenToCacheProvider() throws Exception {
		when(this.cacheProvider.getAccountVersion(anyString())).thenReturn(null);
		when(this.cacheProvider.getIterations(anyString())).thenReturn(null);
		when(this.cacheProvider.getAccountData(anyString())).thenReturn(null);

		final byte[] content = { 1,2,3,4,5,6,7,8,9,10 };
		when(this.loginProvider.login(USERNAME, PASSWORD, null, null, 1)).thenReturn(new LoginResult("789", KEY, 11, 12));
		when(this.httpResponse.getContent()).thenReturn(new ByteArrayInputStream(content));

		this.lastPassBuilder.getPasswordStore(null);
		
		ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
		// Verify the key was correct
		verify(this.passwordStoreFactory).getPasswordStore(inputStreamCaptor.capture(), eq(new AES256EncryptionProvider(KEY)));
		// Read the input stream fully and close it to force the data to be cached
		InputStream in = inputStreamCaptor.getValue();
		try {
			assertEquals(1, in.read());
			assertEquals(2, in.read());
			in.skip(5);
			assertEquals(8, in.read());
			in.skip(2);
			assertEquals(-1, in.read());
		} finally {
			in.close();
		}
		
		verifyStoreAccountData(12, 11, content);
	}

	private void verifyStoreAccountData(int iterations, int accountsVersion, final byte[] content) throws IOException {
		// Check the cached data is correct
		ArgumentCaptor<ByteArrayInputStream> cacheInputStreamCaptor = ArgumentCaptor.forClass(ByteArrayInputStream.class);
		verify(this.cacheProvider).storeAccountData(eq(USERNAME), eq(iterations), eq(accountsVersion), cacheInputStreamCaptor.capture());
		ByteArrayInputStream cachedAccountData = cacheInputStreamCaptor.getValue();
		cachedAccountData.reset();
		assertThat(ByteStreams.toByteArray(cachedAccountData), equalTo(content));
	}

	private byte[] verifyKeyAndReadAccountInputStream() throws IOException {
		ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
		// Verify the key was correct
		verify(this.passwordStoreFactory).getPasswordStore(inputStreamCaptor.capture(), eq(new AES256EncryptionProvider(KEY)));
		// Read the input stream fully and close it to force the data to be cached
		try {
			return ByteStreams.toByteArray(inputStreamCaptor.getValue());
		} finally {
			inputStreamCaptor.getValue().close();
		}
	}
}
