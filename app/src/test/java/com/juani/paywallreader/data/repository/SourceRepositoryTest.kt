package com.juani.paywallreader.data.repository

import com.juani.paywallreader.data.local.SourceDao
import com.juani.paywallreader.data.local.FolderEntity
import com.juani.paywallreader.data.local.HistoryEntity
import com.juani.paywallreader.data.local.ReadingItemEntity
import com.juani.paywallreader.data.local.SourceEntity
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.domain.model.UNFILED_FOLDER_NAME
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
    fun `createFolder persists empty folders`() = runTest {
        repository.createFolder("  Tech  ")

        assertEquals(listOf("Tech"), sourceDao.folders.value.map { it.name })
    }

    @Test
    fun `createFolder ignores unfiled ghost folder`() = runTest {
        repository.createFolder(UNFILED_FOLDER_NAME)

        assertTrue(sourceDao.folders.value.isEmpty())
    }

    @Test
    fun `deleteFolder moves sources to unfiled and removes folder`() = runTest {
        sourceDao.insertFolder(FolderEntity(name = "Tech"))
        sourceDao.seed(
            SourceEntity(
                id = 1,
                name = "Example",
                url = "https://example.com",
                folderName = "Tech",
            ),
        )

        repository.deleteFolder(" Tech ")

        assertEquals(UNFILED_FOLDER_NAME, sourceDao.entities.value.single().folderName)
        assertEquals(listOf(UNFILED_FOLDER_NAME), sourceDao.folders.value.map { it.name })
    }

    @Test
    fun `updateSource edits source and preserves createdAt`() = runTest {
        val source = Source(
            id = 1,
            name = "Old",
            url = "https://old.com",
            isDefault = false,
            folderName = "News",
        )
        sourceDao.seed(source.toEntity(createdAt = 99))

        repository.updateSource(source, " New name ", "new.com", " Tech ")

        assertEquals(
            SourceEntity(
                id = 1,
                name = "New name",
                url = "https://new.com",
                isDefault = false,
                folderName = "Tech",
                createdAt = 99,
            ),
            sourceDao.entities.value.single(),
        )
        assertEquals("Tech", sourceDao.folders.value.single().name)
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

    @Test
    fun `deleteSource prunes empty unfiled folder`() = runTest {
        val customSource = Source(
            id = 1,
            name = "Custom",
            url = "https://custom.com",
            isDefault = false,
            folderName = UNFILED_FOLDER_NAME,
        )
        sourceDao.insertFolder(FolderEntity(name = UNFILED_FOLDER_NAME))
        sourceDao.seed(customSource.toEntity(createdAt = 1))

        repository.deleteSource(customSource)

        assertTrue(sourceDao.entities.value.isEmpty())
        assertTrue(sourceDao.folders.value.isEmpty())
    }

    @Test
    fun `updateSource prunes unfiled folder when last source moves out`() = runTest {
        val customSource = Source(
            id = 1,
            name = "Custom",
            url = "https://custom.com",
            isDefault = false,
            folderName = UNFILED_FOLDER_NAME,
        )
        sourceDao.insertFolder(FolderEntity(name = UNFILED_FOLDER_NAME))
        sourceDao.seed(customSource.toEntity(createdAt = 1))

        repository.updateSource(customSource, "Custom", "https://custom.com", "Tech")

        assertEquals("Tech", sourceDao.entities.value.single().folderName)
        assertEquals(listOf("Tech"), sourceDao.folders.value.map { it.name })
    }

    @Test
    fun `clearHistory removes all history items`() = runTest {
        repository.recordVisit("First", "https://example.com/first", "Example")
        repository.recordVisit("Second", "https://example.com/second", "Example")

        repository.clearHistory()

        assertTrue(sourceDao.historyItems.value.isEmpty())
    }
}

private class FakeSourceDao : SourceDao {
    val entities = MutableStateFlow<List<SourceEntity>>(emptyList())
    val folders = MutableStateFlow<List<FolderEntity>>(emptyList())
    val readingItems = MutableStateFlow<List<ReadingItemEntity>>(emptyList())
    val historyItems = MutableStateFlow<List<HistoryEntity>>(emptyList())
    private var nextId = 1L
    private var nextReadingId = 1L
    private var nextHistoryId = 1L

    override fun getAll(): Flow<List<SourceEntity>> = entities

    override fun getFolders(): Flow<List<FolderEntity>> = folders

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

    override suspend fun insertFolder(folder: FolderEntity): Long {
        if (folders.value.any { it.name == folder.name }) {
            return -1
        }
        folders.value = folders.value + folder.copy(createdAt = folders.value.size + 1L)
        return folders.value.size.toLong()
    }

    override suspend fun updateSource(id: Long, name: String, url: String, folderName: String) {
        entities.value = entities.value.map { entity ->
            if (entity.id == id) {
                entity.copy(
                    name = name,
                    url = url,
                    folderName = folderName,
                )
            } else {
                entity
            }
        }
    }

    override suspend fun moveSourcesToFolder(folderName: String, targetFolder: String) {
        entities.value = entities.value.map { entity ->
            if (entity.folderName == folderName) {
                entity.copy(folderName = targetFolder)
            } else {
                entity
            }
        }
    }

    override suspend fun delete(source: SourceEntity) {
        entities.value = entities.value.filterNot { it.id == source.id }
    }

    override suspend fun deleteFolder(folderName: String) {
        folders.value = folders.value.filterNot { it.name == folderName }
    }

    override suspend fun countByUrl(url: String): Int =
        entities.value.count { it.url == url }

    override suspend fun countByFolder(folderName: String): Int =
        entities.value.count { it.folderName == folderName }

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

    override suspend fun clearHistory() {
        historyItems.value = emptyList()
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
