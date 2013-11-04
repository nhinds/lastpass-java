package com.nhinds.lastpass.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.CountingInputStream;

public class FileCacheProvider implements CacheProvider {

	private final File cacheFile;
	private String username;
	private Integer iterations;
	private Integer accountVersion;
	private long skipBytes;

	public FileCacheProvider(File cacheFile) {
		this.cacheFile = cacheFile;
		if (cacheFile != null && cacheFile.isFile()) {
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
		if (this.cacheFile != null) {
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
	}

}
