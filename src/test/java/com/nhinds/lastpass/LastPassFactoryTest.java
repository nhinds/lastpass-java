package com.nhinds.lastpass;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.nhinds.lastpass.impl.FileCacheProvider;
import com.nhinds.lastpass.impl.LastPassImpl;
import com.nhinds.lastpass.impl.NullCacheProvider;

public class LastPassFactoryTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void nonCachingLastPass() {
		assertEquals(new LastPassImpl(new NullCacheProvider()), LastPassFactory.getLastPass());
	}

	@Test
	public void cachingLastPass() throws IOException {
		File cacheFile = this.temporaryFolder.newFile();
		assertEquals(new LastPassImpl(new FileCacheProvider(cacheFile)), LastPassFactory.getCachingLastPass(cacheFile));
	}
}
