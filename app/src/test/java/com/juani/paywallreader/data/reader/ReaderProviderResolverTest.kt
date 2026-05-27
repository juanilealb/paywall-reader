package com.juani.paywallreader.data.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProviderResolverTest {
    @Test
    fun `captureProviderKey identifies known reader providers`() {
        assertEquals(
            CAPTURE_PROVIDER_PERISCOPE,
            "https://periscope.corsfix.com/?url=https%3A%2F%2Fnytimes.com%2Farticle".captureProviderKey(),
        )
        assertEquals(
            CAPTURE_PROVIDER_UNWALL,
            "https://unwall.app/example.com/article?reader=1".captureProviderKey(),
        )
        assertEquals(
            CAPTURE_PROVIDER_ARCHIVE,
            "https://archive.ph/search/?q=https%3A%2F%2Fexample.com%2Farticle".captureProviderKey(),
        )
        assertEquals(
            CAPTURE_PROVIDER_ACCESS_ARTICLE_NOW,
            "https://accessarticlenow.com/api/c/full?q=https%3A%2F%2Fexample.com%2Farticle".captureProviderKey(),
        )
        assertEquals(
            CAPTURE_PROVIDER_X,
            "https://x.com/Suryanshti777/status/2057854303499636946".captureProviderKey(),
        )
        assertEquals(
            CAPTURE_PROVIDER_X,
            "https://mobile.twitter.com/example/status/1234567890".captureProviderKey(),
        )
    }

    @Test
    fun `captureProviderKey falls back to original for normal article urls`() {
        assertEquals(CAPTURE_PROVIDER_ORIGINAL, "https://example.com/article".captureProviderKey())
    }

    @Test
    fun `captureProviderLabel returns readable pill labels`() {
        assertEquals("Periscope", CAPTURE_PROVIDER_PERISCOPE.captureProviderLabel())
        assertEquals("Archive", CAPTURE_PROVIDER_ARCHIVE.captureProviderLabel())
        assertEquals("X", CAPTURE_PROVIDER_X.captureProviderLabel())
        assertEquals("Original", "unknown".captureProviderLabel())
    }
}
