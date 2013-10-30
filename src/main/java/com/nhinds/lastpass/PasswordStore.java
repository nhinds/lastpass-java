package com.nhinds.lastpass;

import java.util.Collection;

public interface PasswordStore {
	Collection<? extends PasswordInfo> getPasswords();

	Collection<PasswordInfo> getPasswordsByHostname(String hostname);

	PasswordInfo getPassword(int id);
}
