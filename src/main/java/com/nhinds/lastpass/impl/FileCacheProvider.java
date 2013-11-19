package com.nhinds.lastpass.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingInputStream;

/**
 * Cache provider which caches data to a file. Only data for a single user is stored, and storing data for a new username deletes cached
 * data for other usernames.
 * <p>
 * It is not safe to use multiple instances of this class with the same cache file, or read from this class while calling
 * {@link #storeAccountData(String, int, int, InputStream)} from another thread (although it is safe to call the read mehods from multiple
 * threads at the same time)
 */
public class FileCacheProvider implements CacheProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheProvider.class);

	private final File cacheFile;
	private String username;
	private Integer iterations;
	private Integer accountVersion;
	private long skipBytes;

	public FileCacheProvider(File cacheFile) {
		this.cacheFile = Preconditions.checkNotNull(cacheFile);
		LOGGER.debug("Caching to file {}", cacheFile);
		if (cacheFile.isFile()) {
			LOGGER.debug("Cache file {} exists", cacheFile);
			try {
				final CountingInputStream countingInput = new CountingInputStream(new FileInputStream(cacheFile));
				final DataInputStream cacheInput = new DataInputStream(countingInput);
				try {
					this.username = cacheInput.readUTF();
					this.iterations = cacheInput.readInt();
					this.accountVersion = cacheInput.readInt();
					this.skipBytes = countingInput.getCount();
				} finally {
					cacheInput.close();
				}
			} catch (IOException ignore) {
				// Assume an invalid cache file can be thrown away
				this.username = null;
				this.iterations = null;
				this.accountVersion = null;
				this.skipBytes = 0;
			}
		}
	}

	@Override
	public Integer getIterations(String username) {
		if (username.equals(this.username)) {
			return this.iterations;
		}
		return null;
	}

	@Override
	public Integer getAccountVersion(String username) {
		if (username.equals(this.username)) {
			return this.accountVersion;
		}
		return null;
	}

	@Override
	public InputStream getAccountData(String username) throws IOException {
		if (username.equals(this.username)) {
			FileInputStream fileInputStream = new FileInputStream(this.cacheFile);
			if (this.skipBytes != fileInputStream.skip(this.skipBytes)) {
				throw new IOException("Could not skip " + this.skipBytes + " bytes from file");
			}
			return fileInputStream;
		}
		return null;
	}

	@Override
	public void storeAccountData(String username, int iterations, int accountVersion, InputStream accountData) throws IOException {
		this.username = username;
		this.iterations = iterations;
		this.accountVersion = accountVersion;
		final DataOutputStream out = new DataOutputStream(new FileOutputStream(this.cacheFile));
		try {
			out.writeUTF(username);
			out.writeInt(iterations);
			out.writeInt(accountVersion);
			// Record how many bytes we wrote before the account data to skip in getAccountData()
			this.skipBytes = out.size();
			ByteStreams.copy(accountData, out);
		} finally {
			accountData.close();
			out.close();
		}
	}

	@Override
	public int hashCode() {
		return this.cacheFile.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof FileCacheProvider && this.cacheFile.equals(((FileCacheProvider)obj).cacheFile);
	}
}
