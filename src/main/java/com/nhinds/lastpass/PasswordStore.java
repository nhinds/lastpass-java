package com.nhinds.lastpass;

import java.util.Collection;

/** Accessor for the passwords of a LastPass user */
public interface PasswordStore {
	/**
	 * Get all passwords for the LastPass user
	 * 
	 * @return All passwords for the LastPass user
	 */
	Collection<PasswordInfo> getPasswords();

	/**
	 * Get passwords which could be used to log into a site with the given hostname. This filters the passwords based on their URLs, taking
	 * into account the user's configured equivalent domains.
	 * <p>
	 * Note that LastPass considers all subdomains of a domain to be equivalent.
	 * <p>
	 * Example: if the user has configured <code>example.com</code> to be equivalent to <code>anotherexample.com</code>, then calling
	 * <code>getPasswordsByHostname("foo.example.com")</code> will return accounts with URLs:
	 * <ul>
	 * <li><code>http://foo.example.com</code>
	 * <li><code>http://bar.example.com/login</code>
	 * <li><code>https://example.com/index.html</code>
	 * <li><code>ftp://anotherexample.com</code>
	 * <li><code>http://baz.anotherexample.com/</code>
	 * <li>etc.
	 * </ul>
	 * 
	 * @param hostname
	 *            The hostname to retrieve passwords for
	 * @return Matching passwords for the given hostname
	 */
	Collection<PasswordInfo> getPasswordsByHostname(String hostname);

	/**
	 * Get a single password based on its internal identifier
	 * 
	 * @param id
	 *            The LastPass internal identifier for the password
	 * @return The password for the given identifier
	 * @see PasswordInfo#getId()
	 */
	PasswordInfo getPassword(long id);
}
