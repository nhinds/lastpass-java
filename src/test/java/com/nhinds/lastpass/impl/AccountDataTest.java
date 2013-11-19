package com.nhinds.lastpass.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nhinds.lastpass.encryption.EncryptionProvider;


@RunWith(MockitoJUnitRunner.class)
public class AccountDataTest {
	private static final byte[] NAME_BYTES = new byte[] { 1, 2, 3 };
	private static final byte[] GROUP_BYTES = new byte[] { 4, 5, 6 };
	private static final byte[] USERNAME_BYTES = new byte[] { 7, 8, 9 };
	private static final byte[] PASSWORD_BYTES = new byte[] { 4 };
	private static final byte[] EXTRA_BYTES = new byte[] { 5 };
	private static final byte[] FIID_BYTES = new byte[] { 6 };
	@Mock
	private EncryptionProvider decryptionProvider;

	private AccountData accountData;

	@Before
	public void setup() {
		when(this.decryptionProvider.decrypt(NAME_BYTES)).thenReturn("acName");
		when(this.decryptionProvider.decrypt(GROUP_BYTES)).thenReturn("acGroup");
		when(this.decryptionProvider.decrypt(USERNAME_BYTES)).thenReturn("acUser");
		when(this.decryptionProvider.decrypt(PASSWORD_BYTES)).thenReturn("acPass");
		when(this.decryptionProvider.decrypt(EXTRA_BYTES)).thenReturn("extra");
		when(this.decryptionProvider.decrypt(FIID_BYTES)).thenReturn("fiid");

		this.accountData = new AccountData(1, NAME_BYTES, GROUP_BYTES, "http://foo/bar/baz", EXTRA_BYTES, "favourite", "from",
				USERNAME_BYTES, PASSWORD_BYTES, "passwordProtected", "sn", "lastTouched", "autoLogin", "neverAutofill", "realmData",
				FIID_BYTES, "customJs",
				"submitId", "captchaId", "urid", "basicAuthorization", "method", "action", "groupId", "deleted", "attachKey",
				"attachPresent", "individualShare", "unknown1", this.decryptionProvider);
	}

	@Test
	public void testSimpleProperties() {
		assertEquals(1, this.accountData.getId());
		assertEquals("http://foo/bar/baz", this.accountData.getUrl());
		assertEquals("favourite", this.accountData.getFavourite());
		assertEquals("from", this.accountData.getSharedFromId());
		assertEquals("passwordProtected", this.accountData.getPasswordProtected());
		assertEquals("sn", this.accountData.getSn());
		assertEquals("lastTouched", this.accountData.getLastTouched());
		assertEquals("autoLogin", this.accountData.getAutoLogin());
		assertEquals("neverAutofill", this.accountData.getNeverAutofill());
		assertEquals("realmData", this.accountData.getRealmData());
		assertEquals("customJs", this.accountData.getCustomJs());
		assertEquals("submitId", this.accountData.getSubmitId());
		assertEquals("captchaId", this.accountData.getCaptchaId());
		assertEquals("urid", this.accountData.getUrid());
		assertEquals("basicAuthorization", this.accountData.getBasicAuthorization());
		assertEquals("method", this.accountData.getMethod());
		assertEquals("action", this.accountData.getAction());
		assertEquals("groupId", this.accountData.getGroupId());
		assertEquals("deleted", this.accountData.getDeleted());
		assertEquals("attachKey", this.accountData.getAttachKey());
		assertEquals("attachPresent", this.accountData.getAttachPresent());
		assertEquals("individualShare", this.accountData.getIndividualShare());
		assertEquals("unknown1", this.accountData.getUnknown1());
	}

	@Test
	public void testName() {
		// Should decrypt at construction before getName() is called
		verify(this.decryptionProvider).decrypt(NAME_BYTES);

		assertEquals("acName", this.accountData.getName());
		assertEquals("acName", this.accountData.getName());

		// Should only decrypt once
		verify(this.decryptionProvider).decrypt(NAME_BYTES);
	}

	@Test
	public void testGroup() {
		assertEquals("acGroup", this.accountData.getGroup());
		assertEquals("acGroup", this.accountData.getGroup());

		// Should only decrypt once
		verify(this.decryptionProvider).decrypt(GROUP_BYTES);
	}

	@Test
	public void testUsername() {
		assertEquals("acUser", this.accountData.getUsername());
		assertEquals("acUser", this.accountData.getUsername());

		// Should only decrypt once
		verify(this.decryptionProvider).decrypt(USERNAME_BYTES);
	}

	@Test
	public void testPassword() {
		assertEquals("acPass", this.accountData.getPassword());
		assertEquals("acPass", this.accountData.getPassword());

		// Should only decrypt once
		verify(this.decryptionProvider).decrypt(PASSWORD_BYTES);
	}

	@Test
	public void testExtra() {
		assertEquals("extra", this.accountData.getExtra());
		assertEquals("extra", this.accountData.getExtra());

		// Should only decrypt once
		verify(this.decryptionProvider).decrypt(EXTRA_BYTES);
	}

	@Test
	public void testFiid() {
		assertEquals("fiid", this.accountData.getFiid());
		assertEquals("fiid", this.accountData.getFiid());

		// Should only decrypt once
		verify(this.decryptionProvider).decrypt(FIID_BYTES);
	}
}
