package com.juani.paywallreader.ui.home

import com.juani.paywallreader.domain.model.ReadingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineArticleReaderModelTest {
    @Test
    fun `display model keeps hero image metadata and parses markdown hierarchy`() {
        val item = ReadingItem(
            id = 1,
            title = "Captured Title",
            url = "https://www.example.com/story",
            sourceName = "Example",
            addedAt = 0L,
            author = "Jane Doe",
            excerpt = "A good summary",
            markdown = """
                # Captured Title

                Source: https://www.example.com/story

                ## First section

                Opening paragraph with enough words.

                - First point
                - Second point

                > A quoted idea
            """.trimIndent(),
            imageUrl = "https://cdn.example.com/hero.jpg",
        )

        val model = item.toOfflineArticleReaderModel()

        assertEquals("Captured Title", model.title)
        assertEquals("Jane Doe · Example · example.com", model.byline)
        assertEquals("https://cdn.example.com/hero.jpg", model.heroImageUrl)
        assertFalse(model.blocks.any { it.text.startsWith("Source:") })
        assertTrue(model.blocks.any { it.type == OfflineArticleBlockType.Heading && it.text == "First section" })
        assertTrue(model.blocks.any { it.type == OfflineArticleBlockType.Bullet && it.text == "First point" })
        assertTrue(model.blocks.any { it.type == OfflineArticleBlockType.Quote && it.text == "A quoted idea" })
    }

    @Test
    fun `display model falls back to excerpt when captured body is missing`() {
        val item = ReadingItem(
            id = 2,
            title = "Short",
            url = "https://news.example.com/a",
            sourceName = "",
            addedAt = 0L,
            excerpt = "Only summary available",
        )

        val model = item.toOfflineArticleReaderModel()

        assertEquals("news.example.com", model.byline)
        assertEquals(listOf(OfflineArticleBlock(OfflineArticleBlockType.Paragraph, "Only summary available")), model.blocks)
    }
}
