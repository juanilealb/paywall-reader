package com.juani.paywallreader.data.repository

import com.juani.paywallreader.data.local.SourceDao
import com.juani.paywallreader.data.local.HistoryEntity
import com.juani.paywallreader.data.local.ReadingItemEntity
import com.juani.paywallreader.data.local.SourceEntity
import com.juani.paywallreader.domain.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRepositoryTest {
    private val sourceDao = FakeSourceDao()
    private val repository = SourceRepository(sourceDao)

    @Test
    fun `addSource trims name and stores normalized url`() = runTest {
        repository.addSource("  La Nacion  ", "WWW.LANACION.COM.AR/")

        assertEquals(
            listOf(
                SourceEntity(
                    id = 1,
                    name = "La Nacion",
                    url = "https://www.lanacion.com.ar",
                    isDefault = false,
                    createdAt = 1,
                ),
            ),
            sourceDao.entities.value,
        )
    }

    @Test
    fun `addSource ignores blank names invalid urls and duplicates`() = runTest {
        repository.addSource("News", "example.com")
        repository.addSource("   ", "another.com")
        repository.addSource("Invalid", "not a url")
        repository.addSource("Duplicate", "https://example.com")

        assertEquals(1, sourceDao.entities.value.size)
        assertEquals("https://example.com", sourceDao.entities.value.single().url)
    }

    @Test
    fun `deleteSource removes former default sources`() = runTest {
        val source = Source(
            id = 1,
            name = "News",
            url = "https://news.com",
            isDefault = true,
        )
        sourceDao.seed(source.toEntity(createdAt = 1))

        repository.deleteSource(source)

        assertTrue(sourceDao.entities.value.isEmpty())
    }

    @Test
    fun `deleteSource removes custom sources`() = runTest {
        val customSource = Source(
            id = 1,
            name = "Custom",
            url = "https://custom.com",
            isDefault = false,
        )
        sourceDao.seed(customSource.toEntity(createdAt = 1))

        repository.deleteSource(customSource)

        assertTrue(sourceDao.entities.value.isEmpty())
    }
}

private class FakeSourceDao : SourceDao {
    val entities = MutableStateFlow<List<SourceEntity>>(emptyList())
    val readingItems = MutableStateFlow<List<ReadingItemEntity>>(emptyList())
    val historyItems = MutableStateFlow<List<HistoryEntity>>(emptyList())
    private var nextId = 1L
    private var nextReadingId = 1L
    private var nextHistoryId = 1L

    override fun getAll(): Flow<List<SourceEntity>> = entities

    override fun getReadingItems(): Flow<List<ReadingItemEntity>> = readingItems

    override fun getHistoryItems(): Flow<List<HistoryEntity>> = historyItems

    override suspend fun insert(source: SourceEntity): Long {
        if (countByUrl(source.url) > 0) {
            return -1
        }

        val entity = if (source.id == 0L) {
            source.copy(id = nextId, createdAt = nextId)
        } else {
            source
        }
        nextId = maxOf(nextId, entity.id + 1)
        entities.value = entities.value + entity
        return entity.id
    }

    override suspend fun delete(source: SourceEntity) {
        entities.value = entities.value.filterNot { it.id == source.id }
    }

    override suspend fun countByUrl(url: String): Int =
        entities.value.count { it.url == url }

    override suspend fun count(): Int = entities.value.size

    override suspend fun upsertReadingItem(item: ReadingItemEntity) {
        val entity = item.copy(id = item.id.takeIf { it != 0L } ?: nextReadingId++)
        readingItems.value = readingItems.value.filterNot { it.url == item.url } + entity
    }

    override suspend fun deleteReadingItem(url: String) {
        readingItems.value = readingItems.value.filterNot { it.url == url }
    }

    override suspend fun countReadingItem(url: String): Int =
        readingItems.value.count { it.url == url }

    override suspend fun insertHistoryItem(item: HistoryEntity): Long {
        val entity = item.copy(id = item.id.takeIf { it != 0L } ?: nextHistoryId++)
        historyItems.value = historyItems.value + entity
        return entity.id
    }

    override suspend fun trimHistory(limit: Int) {
        historyItems.value = historyItems.value.sortedByDescending { it.visitedAt }.take(limit)
    }

    fun seed(vararg sources: SourceEntity) {
        entities.value = sources.toList()
        nextId = (sources.maxOfOrNull { it.id } ?: 0L) + 1
    }
}

private fun Source.toEntity(createdAt: Long): SourceEntity =
    SourceEntity(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
        folderName = folderName,
        createdAt = createdAt,
    )
