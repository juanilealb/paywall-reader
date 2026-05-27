package com.juani.paywallreader.data.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XPostExtractorTest {
    @Test
    fun `canHandle identifies X and Twitter status urls`() {
        assertTrue(XPostExtractor.canHandle("https://x.com/Suryanshti777/status/2057854303499636946"))
        assertTrue(XPostExtractor.canHandle("https://twitter.com/example/statuses/1234567890?s=20"))
        assertTrue(XPostExtractor.canHandle("https://mobile.x.com/example/status/1234567890"))
        assertFalse(XPostExtractor.canHandle("https://x.com/example"))
        assertFalse(XPostExtractor.canHandle("https://example.com/story"))
    }

    @Test
    fun `fromOEmbedJson extracts readable tweet markdown`() {
        val article = XPostExtractor.fromOEmbedJson(
            requestedUrl = "https://twitter.com/Suryanshti777/status/2057854303499636946?s=20",
            resolvedUrl = "https://x.com/Suryanshti777/status/2057854303499636946",
            jsonPayload = """
                {
                  "author_name": "Suryansh Tiwari",
                  "author_url": "https://x.com/Suryanshti777",
                  "html": "<blockquote class=\"twitter-tweet\"><p lang=\"en\" dir=\"ltr\">A useful tweet with <a href=\"https://t.co/example\">https://t.co/example</a></p>&mdash; Suryansh Tiwari (@Suryanshti777) <a href=\"https://x.com/Suryanshti777/status/2057854303499636946\">May 22, 2026</a></blockquote>"
                }
            """.trimIndent(),
        )

        assertEquals("Post de Suryansh Tiwari @Suryanshti777", article.title)
        assertEquals("https://x.com/Suryanshti777/status/2057854303499636946", article.resolvedUrl)
        assertEquals("A useful tweet with https://t.co/example", article.text)
        assertTrue(article.markdown.startsWith("# Post de Suryansh Tiwari @Suryanshti777"))
        assertTrue(article.markdown.contains("Original: https://x.com/Suryanshti777/status/2057854303499636946"))
    }
}
