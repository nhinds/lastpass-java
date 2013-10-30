package com.nhinds.lastpass.impl.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ok")
@XmlAccessorType(XmlAccessType.FIELD)
public class LastPassOk {
	@XmlAttribute
	private int uid;

	@XmlAttribute(name = "uidhash")
	private String uidHash;

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
		return uid;
	}

	public String getUidHash() {
		return uidHash;
	}

	public String getSessionId() {
		return sessionId;
	}

	public boolean isPasswordResetRequired() {
		return passwordResetRequired;
	}

	public String getToken() {
		return token;
	}

	public boolean isAdLogin() {
		return adLogin;
	}

	public boolean isDisableoffline() {
		return disableoffline;
	}

	public int getAccountsVersion() {
		return accountsVersion;
	}

}
