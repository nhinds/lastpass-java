package com.nhinds.lastpass.impl;

import java.util.Arrays;

import com.nhinds.lastpass.PasswordInfo;

public class AccountData implements PasswordInfo {
	private class EncryptedString {
		private final byte[] encryptedBytes;
		private String unencrypted;

		private EncryptedString(final byte[] encryptedBytes) {
			this.encryptedBytes = encryptedBytes;
		}

		public String get() {
			if (this.unencrypted == null) {
				this.unencrypted = AccountData.this.decryptionProvider.decrypt(this.encryptedBytes);
			}
			return this.unencrypted;
		}

		@Override
		public String toString() {
			return "Encrypted String [encrypted=" + Arrays.toString(this.encryptedBytes) + "]";
		}
	}

	private final long id;
	private final String name;
	private final EncryptedString group;
	private final String url;
	private final String extra;
	private final String favourite;
	private final String sharedFromId;
	private final EncryptedString username;
	private final EncryptedString password;
	private final String passwordProtected;
	private final String sn;
	private final String lastTouched;
	private final String autoLogin;
	private final String neverAutofill;
	private final String realmData;
	private final String fiid;
	private final String customJs;
	private final String submitId;
	private final String captchaId;
	private final String urid;
	private final String basicAuthorization;
	private final String method;
	private final String action;
	private final String groupId;
	private final String deleted;
	private final String attachKey;
	private final String attachPresent;
	private final String individualShare;
	private final String unknown1;
	private final DecryptionProvider decryptionProvider;

	public AccountData(final long id, final byte[] name, final byte[] group, final String url, final String extra, final String favourite,
			final String sharedFromId,
			final byte[] username, final byte[] password, final String passwordProtected, final String sn, final String lastTouched, final String autoLogin,
			final String neverAutofill, final String realmData, final String fiid, final String customJs, final String submitId, final String captchaId, final String urid,
			final String basicAuthorization, final String method, final String action, final String groupId, final String deleted, final String attachKey,
			final String attachPresent, final String individualShare, final String unknown1, final DecryptionProvider decryptionProvider) {
		this.id = id;
		// Decrypt the name up front because it is normally used for displaying/sorting. This also ensures that the decryption provider has
		// the correct decryption key so future decryptions should succeed if this one does
		this.name = decryptionProvider.decrypt(name);
		this.group = new EncryptedString(group);
		this.url = url;
		this.extra = extra;
		this.favourite = favourite;
		this.sharedFromId = sharedFromId;
		this.username = new EncryptedString(username);
		this.password = new EncryptedString(password);
		this.passwordProtected = passwordProtected;
		this.sn = sn;
		this.lastTouched = lastTouched;
		this.autoLogin = autoLogin;
		this.neverAutofill = neverAutofill;
		this.realmData = realmData;
		this.fiid = fiid;
		this.customJs = customJs;
		this.submitId = submitId;
		this.captchaId = captchaId;
		this.urid = urid;
		this.basicAuthorization = basicAuthorization;
		this.method = method;
		this.action = action;
		this.groupId = groupId;
		this.deleted = deleted;
		this.attachKey = attachKey;
		this.attachPresent = attachPresent;
		this.individualShare = individualShare;
		this.unknown1 = unknown1;
		this.decryptionProvider = decryptionProvider;
	}

	// Encrypted fields

	public String getGroup() {
		return this.group.get();
	}

	@Override
	public String getUsername() {
		return this.username.get();
	}

	@Override
	public String getPassword() {
		return this.password.get();
	}

	// Regular fields

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public long getId() {
		return this.id;
	}

	@Override
	public String getUrl() {
		return this.url;
	}

	public String getExtra() {
		return this.extra;
	}

	public String getFavourite() {
		return this.favourite;
	}

	public String getSharedFromId() {
		return this.sharedFromId;
	}

	public String getPasswordProtected() {
		return this.passwordProtected;
	}

	public String getSn() {
		return this.sn;
	}

	public String getLastTouched() {
		return this.lastTouched;
	}

	public String getAutoLogin() {
		return this.autoLogin;
	}

	public String getNeverAutofill() {
		return this.neverAutofill;
	}

	public String getRealmData() {
		return this.realmData;
	}

	public String getFiid() {
		return this.fiid;
	}

	public String getCustomJs() {
		return this.customJs;
	}

	public String getSubmitId() {
		return this.submitId;
	}

	public String getCaptchaId() {
		return this.captchaId;
	}

	public String getUrid() {
		return this.urid;
	}

	public String getBasicAuthorization() {
		return this.basicAuthorization;
	}

	public String getMethod() {
		return this.method;
	}

	public String getAction() {
		return this.action;
	}

	public String getGroupId() {
		return this.groupId;
	}

	public String getDeleted() {
		return this.deleted;
	}

	public String getAttachKey() {
		return this.attachKey;
	}

	public String getAttachPresent() {
		return this.attachPresent;
	}

	public String getIndividualShare() {
		return this.individualShare;
	}

	public String getUnknown1() {
		return this.unknown1;
	}

	@Override
	public String toString() {
		return "AccountData [id=" + this.id + ", name=" + this.name + ", group=" + this.group + ", url=" + this.url + ", extra=" + this.extra + ", favourite="
				+ this.favourite + ", sharedFromId=" + this.sharedFromId + ", username=" + this.username + ", password=" + this.password
				+ ", passwordProtected=" + this.passwordProtected + ", sn=" + this.sn + ", lastTouched=" + this.lastTouched + ", autoLogin="
				+ this.autoLogin + ", neverAutofill=" + this.neverAutofill + ", realmData=" + this.realmData + ", fiid=" + this.fiid + ", customJs="
				+ this.customJs + ", submitId=" + this.submitId + ", captchaId=" + this.captchaId + ", urid=" + this.urid + ", basicAuthorization="
				+ this.basicAuthorization + ", method=" + this.method + ", action=" + this.action + ", groupId=" + this.groupId + ", deleted=" + this.deleted
				+ ", attachKey=" + this.attachKey + ", attachPresent=" + this.attachPresent + ", individualShare=" + this.individualShare
				+ ", unknown1=" + this.unknown1 + "]";
	}

}