package com.nhinds.lastpass.impl.dto;

import com.google.api.client.util.Key;

public class LastPassOk {
	@Key("@uid")
	private int uid;

	@Key("@sessionid")
	private String sessionId;

	@Key("@pwresetreqd")
	private boolean passwordResetRequired;

	@Key("@token")
	private String token;

	@Key("@adlogin")
	private boolean adLogin;

	@Key("@disableoffline")
	private boolean disableoffline;

	@Key("@accts_version")
	private int accountsVersion;

	public int getUid() {
		return this.uid;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public boolean isPasswordResetRequired() {
		return this.passwordResetRequired;
	}

	public String getToken() {
		return this.token;
	}

	public boolean isAdLogin() {
		return this.adLogin;
	}

	public boolean isDisableoffline() {
		return this.disableoffline;
	}

	public int getAccountsVersion() {
		return this.accountsVersion;
	}

	@Override
	public String toString() {
		return "LastPassOk [uid=" + this.uid + ", sessionId=" + this.sessionId + ", passwordResetRequired=" + this.passwordResetRequired
				+ ", token=" + this.token + ", adLogin=" + this.adLogin + ", disableoffline=" + this.disableoffline + ", accountsVersion="
				+ this.accountsVersion + "]";
	}
}
