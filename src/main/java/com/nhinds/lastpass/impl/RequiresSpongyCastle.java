package com.nhinds.lastpass.impl;

import java.security.Security;

/** Helper class which loads the SpongyCastle security provider when referenced */
abstract class RequiresSpongyCastle {
	static {
		Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
	}

}
