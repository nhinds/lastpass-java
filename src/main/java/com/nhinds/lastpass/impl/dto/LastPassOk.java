package com.nhinds.lastpass.impl.dto;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class LastPassOk {
	@XmlAttribute
	private int uid;

	@XmlAttribute(name = "sessionid")
	private String sessionId;

	@XmlAttribute(name = "pwresetreqd")
	private boolean passwordResetRequired;

	@XmlAttribute
	private String token;

	@XmlAttribute(name = "adlogin")
	private boolean adLogin;

	@XmlAttribute(name = "disableoffline")
	private boolean disableoffline;

	@XmlAttribute(name = "accts_version")
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

}
