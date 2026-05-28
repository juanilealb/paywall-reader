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

    @Test
    fun `externalLinksFromOEmbedHtml keeps article links and skips X chrome links`() {
        val links = XPostExtractor.externalLinksFromOEmbedHtml(
            """
                <blockquote class="twitter-tweet">
                  <p><a href="https://t.co/cardlink">https://t.co/cardlink</a></p>
                  <a href="https://x.com/author/status/1234567890">May 22</a>
                  <a href="https://twitter.com/author">author</a>
                </blockquote>
            """.trimIndent(),
        )

        assertEquals(listOf("https://t.co/cardlink"), links)
    }

    @Test
    fun `fromXArticleJson converts Draft blocks into readable markdown`() {
        val article = XPostExtractor.fromXArticleJson(
            requestedUrl = "https://x.com/theo/status/2018091358251372601",
            resolvedUrl = "https://x.com/theo/status/2018091358251372601",
            jsonPayload = """
                {
                  "url": "https://x.com/theo/status/2018091358251372601",
                  "author": {"name": "Theo - t3.gg", "handle": "theo"},
                  "article": {
                    "title": "The Agentic Code Problem",
                    "previewText": "You hear a notification sound from a Claude Code workflow finishing.",
                    "coverImage": "https://pbs.twimg.com/media/HAGutiCbIAAnZ30.jpg",
                    "createdAt": "2026-02-01T22:37:28.000Z",
                    "blocks": [
                      {"type": "unstyled", "text": "*ding*", "inlineStyleRanges": [{"offset": 0, "length": 6, "style": "Italic"}], "entityRanges": [], "data": {}},
                      {"type": "header-two", "text": "Our tools were not built for how we work today.", "inlineStyleRanges": [{"offset": 0, "length": 47, "style": "Bold"}], "entityRanges": [], "data": {}},
                      {"type": "unstyled", "text": "Back in my day, we worked on one thing at a time.", "inlineStyleRanges": [], "entityRanges": [], "data": {}}
                    ],
                    "entityMap": []
                  }
                }
            """.trimIndent(),
        )

        assertEquals("The Agentic Code Problem", article.title)
        assertEquals("https://pbs.twimg.com/media/HAGutiCbIAAnZ30.jpg", article.imageUrl)
        assertTrue(article.markdown.startsWith("# The Agentic Code Problem"))
        assertTrue(article.markdown.contains("_*ding*_"))
        assertTrue(article.markdown.contains("## Our tools were not built for how we work today."))
        assertTrue(article.text.contains("Back in my day"))
    }
}
