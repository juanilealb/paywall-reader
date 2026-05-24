package com.juani.paywallreader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceUrlTest {
    @Test
    fun `normalizes host without scheme to https`() {
        val result = validateSourceUrl("www.lanacion.com.ar")

        assertTrue(result.isValid)
        assertEquals("https://www.lanacion.com.ar", result.normalizedUrl)
    }

    @Test
    fun `keeps explicit http scheme`() {
        val result = validateSourceUrl("http://example.com")

        assertTrue(result.isValid)
        assertEquals("http://example.com", result.normalizedUrl)
    }

    @Test
    fun `rejects text with spaces`() {
        val result = validateSourceUrl("not a url")

        assertFalse(result.isValid)
    }

    @Test
    fun `rejects host without a dot`() {
        val result = validateSourceUrl("localhost")

        assertFalse(result.isValid)
    }

    @Test
    fun `removes trailing slash`() {
        val result = validateSourceUrl("https://www.clarin.com/")

        assertTrue(result.isValid)
        assertEquals("https://www.clarin.com", result.normalizedUrl)
    }

    @Test
    fun `normalizes scheme and host case`() {
        val result = validateSourceUrl("HTTPS://WWW.CLARIN.COM")

        assertTrue(result.isValid)
        assertEquals("https://www.clarin.com", result.normalizedUrl)
    }
}
