package com.nhinds.lastpass.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

public class FileCacheProviderTest {
	private static final String USER = "user";

	private static byte[] ACCOUNT_DATA = new byte[] { 1, 2, 100, 101 };

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File cacheFile;
	
	@Before
	public void setup() throws IOException {
		this.cacheFile = this.temporaryFolder.newFile();
	}

	@Test(expected = NullPointerException.class)
	public void nullFileThrowsException() throws IOException {
		new FileCacheProvider(null);
	}
	
	@Test
	public void missingFileReturnsNull() throws IOException {
		assertTrue(this.cacheFile.delete());

		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		assertNull(provider.getIterations(USER));
		assertNull(provider.getAccountVersion(USER));
		assertNull(provider.getAccountData(USER));
	}

	@Test
	public void missingFileStoresAccountData() throws IOException {
		assertTrue(this.cacheFile.delete());

		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		provider.storeAccountData(USER, 5, 15, getAccountDataInputStream());
		byte[] cachedData = Files.toByteArray(this.cacheFile);
		assertThat(cachedData, isCacheDataFor(USER, 5, 15, ACCOUNT_DATA));
	}

	@Test
	public void emptyFileDoesNotThrowExceptionAndReturnsNull() throws IOException {
		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		assertNull(provider.getIterations(USER));
		assertNull(provider.getAccountVersion(USER));
		assertNull(provider.getAccountData(USER));
	}

	@Test
	public void partialFileDoesNotThrowExceptionAndReturnsNull() throws IOException {
		final DataOutputStream out = new DataOutputStream(new FileOutputStream(this.cacheFile));
		try {
			out.writeUTF(USER);
			out.writeInt(5);
		} finally {
			out.close();
		}
		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		assertNull(provider.getIterations(USER));
		assertNull(provider.getAccountVersion(USER));
		assertNull(provider.getAccountData(USER));
	}

	@Test
	public void validFileReturnsData() throws IOException {
		writeValidFile(5, 7);
		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		assertEquals(Integer.valueOf(5), provider.getIterations(USER));
		assertEquals(Integer.valueOf(7), provider.getAccountVersion(USER));
		assertThat(ByteStreams.toByteArray(provider.getAccountData(USER)), is(equalTo(ACCOUNT_DATA)));
		assertThat("Calling getAccountData a second time should work (i.e. should return a different InputStream)",
				ByteStreams.toByteArray(provider.getAccountData(USER)), is(equalTo(ACCOUNT_DATA)));
	}

	@Test
	public void validFileReturnsNullForDifferentUser() throws IOException {
		writeValidFile(5, 7);
		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		assertNull(provider.getIterations("1"));
		assertNull(provider.getAccountVersion("1"));
		assertNull(provider.getAccountData("1"));
	}

	@Test
	public void newValuesReturnedAfterStoreAccountData() throws IOException {
		writeValidFile(2, 3);
		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		byte[] accountData2 = new byte[] { 1, 2, 3 };
		provider.storeAccountData(USER, 10, 11, new ByteArrayInputStream(accountData2));

		assertEquals(Integer.valueOf(10), provider.getIterations(USER));
		assertEquals(Integer.valueOf(11), provider.getAccountVersion(USER));
		assertThat(ByteStreams.toByteArray(provider.getAccountData(USER)), is(equalTo(accountData2)));
	}

	@Test
	public void storeDiscardsOriginalUsername() throws IOException {
		writeValidFile(2, 3);
		final FileCacheProvider provider = new FileCacheProvider(this.cacheFile);

		byte[] accountData2 = new byte[] { 1, 2, 3 };
		provider.storeAccountData("user2", 10, 11, new ByteArrayInputStream(accountData2));

		assertNull(provider.getIterations(USER));
		assertNull(provider.getAccountVersion(USER));
		assertNull(provider.getAccountData(USER));
	}

	private void writeValidFile(int iterations, int accountsVersion) throws IOException {
		final DataOutputStream out = new DataOutputStream(new FileOutputStream(this.cacheFile));
		try {
			out.writeUTF(USER);
			out.writeInt(iterations);
			out.writeInt(accountsVersion);
			out.write(ACCOUNT_DATA);
		} finally {
			out.close();
		}
	}

	private static ByteArrayInputStream getAccountDataInputStream() {
		return new ByteArrayInputStream(ACCOUNT_DATA);
	}

	private static Matcher<byte[]> isCacheDataFor(final String user, final int iterations, final int accountsVersion,
			final byte[] accountData) {
		return new TypeSafeDiagnosingMatcher<byte[]>() {

			@Override
			public void describeTo(Description description) {
				description.appendText("Cache data for user ").appendValue(user).appendText(", iterations ").appendValue(iterations)
						.appendText(", accounts version ").appendValue(accountsVersion).appendText(", account data ")
						.appendValue(accountData);
			}

			@Override
			protected boolean matchesSafely(byte[] item, Description mismatchDescription) {
				// Assumes that the username is ASCII - this is true for these tests
				final int metadataLength = 2 + user.length() + 4 + 4;
				final int expectedSize = metadataLength + accountData.length;
				final ByteArrayDataInput dataInput = ByteStreams.newDataInput(item);
				if (item.length != expectedSize) {
					mismatchDescription.appendText("Wrong size: expected " + expectedSize);
				} else if (!user.equals(dataInput.readUTF())) {
					mismatchDescription.appendText("Wrong username");
				} else if (dataInput.readInt() != iterations) {
					mismatchDescription.appendText("Wrong iterations");
				} else if (dataInput.readInt() != accountsVersion) {
					mismatchDescription.appendText("Wrong accounts version");
				} else if (!subArrayEquals(accountData, item, metadataLength)) {
					mismatchDescription.appendText("Wrong account data");
				} else {
					return true;
				}
				mismatchDescription.appendText(" got ").appendValue(item);
				return false;
			}

		};
	}

	private static boolean subArrayEquals(byte[] expected, byte[] actual, int offset) {
		byte[] subArray = Arrays.copyOfRange(actual, offset, actual.length);
		return Arrays.equals(expected, subArray);
	}
}
