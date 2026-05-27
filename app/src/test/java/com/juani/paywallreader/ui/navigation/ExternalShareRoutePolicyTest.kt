package com.juani.paywallreader.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalShareRoutePolicyTest {
    @Test
    fun `valid external shared urls are captured headlessly without opening the visible reader`() {
        val decision = ExternalShareRoutePolicy.decide(" https://example.com/article ")

        assertEquals("https://example.com/article", decision.captureUrl)
        assertFalse(decision.openVisibleReader)
    }

    @Test
    fun `non web shared text is ignored`() {
        val decision = ExternalShareRoutePolicy.decide("not a url")

        assertNull(decision.captureUrl)
        assertFalse(decision.openVisibleReader)
    }
}
