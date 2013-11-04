package com.nhinds.lastpass.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.google.common.io.ByteStreams;
import com.nhinds.lastpass.LastPassException;

public class PasswordStoreReader {
	private static final String END_MARKER_CHUNK_ID = "ENDM";
	private static final String ACCT_CHUNK_ID = "ACCT";
	private static final String EQDN_CHUNK_ID = "EQDN";

	private final DecryptionProvider decryptionProvider;
	private final Map<Long, AccountData> accounts = new HashMap<Long, AccountData>();
	private final Map<String, Collection<String>> domains = new HashMap<String, Collection<String>>();

	public PasswordStoreReader(final InputStream accountsStream, final DecryptionProvider decryptionProvider) {
		this.decryptionProvider = decryptionProvider;
		try {
			parseChunks(accountsStream);
		} catch (final IOException e) {
			throw new LastPassException("Error parsing blob", e);
		} finally {
			try {
				accountsStream.close();
			} catch (final IOException ignore) {
			}
		}
	}

	public Map<Long, AccountData> getAccounts() {
		return this.accounts;
	}

	public Map<String, Collection<String>> getDomains() {
		return this.domains;
	}

	private void parseChunks(final InputStream accountsStream) throws IOException {
		final Map<Long, Collection<String>> domainsById = new HashMap<Long, Collection<String>>();
		// # LastPass blob chunk is made up of 4-byte ID, 4-byte size and payload of that size
		// # Example:
		// # 0000: 'IDID'
		// # 0004: 4
		// # 0008: 0xDE 0xAD 0xBE 0xEF
		// # 000C: --- Next chunk ---
		final DataInputStream in = new DataInputStream(accountsStream);
		while (true) {
			final byte[] idBytes = new byte[4];
			in.readFully(idBytes);
			final String id = new String(idBytes);

			if (END_MARKER_CHUNK_ID.equals(id)) {
				// End of stream
				break;
			} else {
				// Create a new child DataInputStream for the next item in the stream.
				final int size = in.readInt();
				final DataInputStream chunkInputStream = new DataInputStream(ByteStreams.limit(in, size));
				if (ACCT_CHUNK_ID.equals(id)) {
					parseAccountData(chunkInputStream);
				} else if (EQDN_CHUNK_ID.equals(id)) {
					parseEquivalentDomain(chunkInputStream, domainsById);
				}
				// Skip over any remaining bytes in the child input stream so that the parent stream is ready to read the next chunk
				chunkInputStream.skipBytes(size);
			}
		}

		for (final Collection<String> equivalentDomains : domainsById.values()) {
			for (final String domain : equivalentDomains) {
				this.domains.put(domain, equivalentDomains);
			}
		}
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

	private void parseAccountData(final DataInputStream acctIn) throws IOException {
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
		this.accounts.put(id, new AccountData(id, name, group, url, extra, favourite, sharedFromId, username, password, passwordProtected,
				sn, lastTouched, autoLogin, neverAutofill, realmData, fiid, customJs, submitId, captchaId, urid, basicAuthorization,
				method, action, groupId, deleted, attachKey, attachPresent, individualShare, unknown1, this.decryptionProvider));
	}

	private static void parseEquivalentDomain(final DataInputStream eqdnIn, final Map<Long, Collection<String>> domainsById)
			throws IOException {
		final long id = readLongItem(eqdnIn);
		final String domain = readHexItem(eqdnIn);
		Collection<String> domainsForId = domainsById.get(id);
		if (domainsForId == null) {
			domainsForId = new ArrayList<String>();
			domainsById.put(id, domainsForId);
		}
		domainsForId.add(domain);
	}

}
