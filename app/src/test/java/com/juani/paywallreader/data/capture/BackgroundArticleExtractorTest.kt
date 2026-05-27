package com.juani.paywallreader.data.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundArticleExtractorTest {
    @Test
    fun `extractFromHtml produces readable article metadata and markdown`() {
        val html = """
            <!doctype html>
            <html>
              <head>
                <title>Ignored shell title</title>
                <meta property="og:title" content="Premium Queue Article" />
                <meta name="author" content="Juani" />
                <meta name="description" content="A useful summary." />
              </head>
              <body>
                <nav>chrome</nav>
                <article>
                  <h1>Premium Queue Article</h1>
                  <p>First paragraph with enough text to look like article content.</p>
                  <p>Second paragraph that should survive the background extractor.</p>
                </article>
              </body>
            </html>
        """.trimIndent()

        val article = BackgroundArticleExtractor.extractFromHtml(
            requestedUrl = "https://example.com/story",
            resolvedUrl = "https://example.com/story?utm=1",
            html = html,
        )

        assertEquals("Premium Queue Article", article.title)
        assertEquals("Juani", article.author)
        assertEquals("A useful summary.", article.excerpt)
        assertTrue(article.text.contains("First paragraph"))
        assertTrue(article.text.contains("Second paragraph"))
        assertTrue(article.markdown.startsWith("# Premium Queue Article"))
    }
}
