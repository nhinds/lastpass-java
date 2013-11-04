package com.nhinds.lastpass.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.nhinds.lastpass.LastPassException;

public class PasswordStoreReader {
	private static final String ACCT_CHUNK_ID = "ACCT";
	private static final String EQDN_CHUNK_ID = "EQDN";

	private final DecryptionProvider decryptionProvider;
	private final Map<String, Collection<byte[]>> chunks;
	private Map<Long, AccountData> accounts;
	private Map<String, Collection<String>> domains;

	public PasswordStoreReader(final InputStream accountsStream, final DecryptionProvider decryptionProvider) {
		this.decryptionProvider = decryptionProvider;
		try {
			this.chunks = readChunks(accountsStream);
		} catch (final IOException e) {
			throw new LastPassException("Error parsing blob", e);
		} finally {
			try {
				accountsStream.close();
			} catch (final IOException ignore) {
			}
		}
		verifyChunks(ACCT_CHUNK_ID);
		verifyChunks(EQDN_CHUNK_ID);
	}

	private void verifyChunks(final String chunkId) {
		if (!this.chunks.containsKey(chunkId))
			throw new IllegalStateException("Missing required chunk " + chunkId);
	}

	private static Map<String, Collection<byte[]>> readChunks(final InputStream accountsStream) throws IOException {
		// # LastPass blob chunk is made up of 4-byte ID, 4-byte size and payload of that size
		// # Example:
		// # 0000: 'IDID'
		// # 0004: 4
		// # 0008: 0xDE 0xAD 0xBE 0xEF
		// # 000C: --- Next chunk ---
		final Map<String, Collection<byte[]>> chunks = new HashMap<String, Collection<byte[]>>();
		final DataInputStream in = new DataInputStream(accountsStream);
		while (true) {
			final byte[] idBytes = new byte[4];
			in.readFully(idBytes);
			final String id = new String(idBytes);

			if ("ENDM".equals(id)) {
				// End of stream
				break;
			}

			Collection<byte[]> chunkList = chunks.get(id);
			if (chunkList == null) {
				chunkList = new ArrayList<byte[]>();
				chunks.put(id, chunkList);
			}
			chunkList.add(readItem(in));
		}
		return chunks;
	}

	private static byte[] readItem(final DataInputStream in) throws IOException {
		final int size = in.readInt();
		final byte[] payload = new byte[size];
		in.readFully(payload);
		return payload;
	}

	private static String readStringItem(final DataInputStream in) throws IOException {
		return new String(readItem(in));
	}

	private static long readLongItem(final DataInputStream acctIn) throws IOException {
		return Long.parseLong(readStringItem(acctIn));
	}

	private static String readHexItem(final DataInputStream in) throws IOException {
		final String item = readStringItem(in);
		try {
			return new String(Hex.decodeHex(item.toCharArray()));
		} catch (final DecoderException e) {
			throw new IOException("Error decoding as hex: " + item, e);
		}
	}

	public Map<Long, AccountData> getAccounts() {
		if (this.accounts == null) {
			this.accounts = new HashMap<Long, AccountData>();
			try {
				for (final byte[] chunkData : this.chunks.get(ACCT_CHUNK_ID)) {
					final DataInputStream acctIn = new DataInputStream(new ByteArrayInputStream(chunkData));
					// TODO how many of these "strings" are not strings?
					final long id = readLongItem(acctIn);
					final byte[] name = readItem(acctIn);
					final byte[] group = readItem(acctIn);
					final String url = readHexItem(acctIn);
					final String extra = readStringItem(acctIn);
					final String favourite = readStringItem(acctIn);
					final String sharedFromId = readStringItem(acctIn);
					final byte[] username = readItem(acctIn);
					final byte[] password = readItem(acctIn);
					final String passwordProtected = readStringItem(acctIn);
					final String sn = readStringItem(acctIn);
					final String lastTouched = readStringItem(acctIn);
					final String autoLogin = readStringItem(acctIn);
					final String neverAutofill = readStringItem(acctIn);
					final String realmData = readStringItem(acctIn);
					final String fiid = readStringItem(acctIn);
					final String customJs = readStringItem(acctIn);
					final String submitId = readStringItem(acctIn);
					final String captchaId = readStringItem(acctIn);
					final String urid = readStringItem(acctIn);
					final String basicAuthorization = readStringItem(acctIn);
					final String method = readStringItem(acctIn);
					final String action = readStringItem(acctIn);
					final String groupId = readStringItem(acctIn);
					final String deleted = readStringItem(acctIn);
					final String attachKey = readStringItem(acctIn);
					final String attachPresent = readStringItem(acctIn);
					final String individualShare = readStringItem(acctIn);
					final String unknown1 = readStringItem(acctIn);
					this.accounts.put(id, new AccountData(id, name, group, url, extra, favourite, sharedFromId, username, password,
							passwordProtected, sn, lastTouched, autoLogin, neverAutofill, realmData, fiid, customJs, submitId, captchaId,
							urid, basicAuthorization, method, action, groupId, deleted, attachKey, attachPresent, individualShare,
							unknown1, this.decryptionProvider));
				}
			} catch (final IOException e) {
				throw new LastPassException("Error parsing accounts data", e);
			}
		}
		return this.accounts;
	}

	public Map<String, Collection<String>> getDomains() {
		if (this.domains == null) {
			this.domains = new HashMap<String, Collection<String>>();
			try {
				final Map<Long, Collection<String>> domainsById = new HashMap<Long, Collection<String>>();
				for (final byte[] chunkData : this.chunks.get(EQDN_CHUNK_ID)) {
					final DataInputStream eqdnIn = new DataInputStream(new ByteArrayInputStream(chunkData));
					final long id = readLongItem(eqdnIn);
					final String domain = readHexItem(eqdnIn);
					Collection<String> domainsForId = domainsById.get(id);
					if (domainsForId == null) {
						domainsForId = new ArrayList<String>();
						domainsById.put(id, domainsForId);
					}
					domainsForId.add(domain);
				}
				for (final Collection<String> equivalentDomains : domainsById.values()) {
					for (final String domain : equivalentDomains) {
						this.domains.put(domain, equivalentDomains);
					}
				}
			} catch (final IOException e) {
				throw new LastPassException("Error parsing equivalent domain data", e);
			}
		}
		return this.domains;
	}

}
