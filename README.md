Lastpass Java Client
========

This is a java client for [lastpass](http://lastpass.com). Efforts have been made to choose dependencies which are compatible with android applications.

Status
--------
Reads password information from lastpass (name, URL, username, password) and supports OTP prompting.  
Does not support saving back to lastpass or offline logins (i.e. cached logins)

Building
--------
`mvn install`
Usage
--------
	Lastpass lastPass = new LastPassImpl();
	PasswordStoreBuilder builder = lastPass.getPasswordStoreBuilder("user", "password", cacheFile);
	PasswordStore store;
	try {
		store = builder.getPasswordStore();
	} catch (GoogleAuthenticatorRequired req) {
		// Prompt user for one-time password
		store = builder.getPasswordStore(userProvidedOtp);
	}
	
	// Get passwords for a hostname
	Collection<? extends PasswordInfo> passwords = store.getPasswordsByHostname("google.com");
	// Get all passwords
	Collection<? extends PasswordInfo allPasswords = store.getPasswords();

License
--------
`lastpass-java` is released under the [MIT license](LICENSE)
