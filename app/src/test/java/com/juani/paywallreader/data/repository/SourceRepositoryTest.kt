package com.juani.paywallreader.data.repository

import com.juani.paywallreader.data.local.SourceDao
import com.juani.paywallreader.data.local.FolderEntity
import com.juani.paywallreader.data.local.HistoryEntity
import com.juani.paywallreader.data.local.ReadingItemEntity
import com.juani.paywallreader.data.local.SourceEntity
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_PERISCOPE
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_UNWALL
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_X
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_CAPTURING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_FAILED
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_PENDING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_READY
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.domain.model.UNFILED_FOLDER_NAME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    fun `external shared urls are saved once as pending read later bookmarks`() = runTest {
        repository.saveBookmarkFromExternalShare(" https://example.com/article ")
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        val item = sourceDao.readingItems.value.single()
        assertEquals("https://example.com/article", item.url)
        assertEquals("example.com", item.title)
        assertEquals("example.com", item.sourceName)
        assertEquals(UNFILED_FOLDER_NAME, item.folderName)
        assertEquals(CAPTURE_STATUS_PENDING, item.captureStatus)
    }

    @Test
    fun `updateCaptureStatus records capture lifecycle states`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        repository.updateCaptureStatus(" https://example.com/article ", CAPTURE_STATUS_CAPTURING)

        assertEquals(CAPTURE_STATUS_CAPTURING, sourceDao.readingItems.value.single().captureStatus)
    }

    @Test
    fun `saveForLater marks captured bookmark ready`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")
        repository.updateCaptureStatus("https://example.com/article", CAPTURE_STATUS_CAPTURING)

        repository.saveForLater(
            title = "Captured Article",
            url = "https://example.com/article",
            sourceName = "Example",
            text = "Captured body",
            markdown = "# Captured Article\n\nCaptured body",
        )

        val item = sourceDao.readingItems.value.single()
        assertEquals("Captured Article", item.title)
        assertEquals(CAPTURE_STATUS_READY, item.captureStatus)
        assertEquals("Captured body", item.text)
    }

    @Test
    fun `saveForLater records capture provider for debugging`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        repository.saveForLater(
            title = "Captured Article",
            url = "https://example.com/article",
            sourceName = "Example",
            resolvedUrl = "https://periscope.corsfix.com/?url=https%3A%2F%2Fexample.com%2Farticle",
            captureProvider = CAPTURE_PROVIDER_PERISCOPE,
        )

        assertEquals(CAPTURE_PROVIDER_PERISCOPE, sourceDao.readingItems.value.single().captureProvider)
    }

    @Test
    fun `saveForLater preserves X capture provider`() = runTest {
        repository.saveForLater(
            title = "Captured X Post",
            url = "https://x.com/example/status/123",
            sourceName = "X",
            captureProvider = CAPTURE_PROVIDER_X,
        )

        assertEquals(CAPTURE_PROVIDER_X, sourceDao.readingItems.value.single().captureProvider)
    }

    @Test
    fun `saveForLater preserves archived state when background capture completes`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")
        repository.updateCaptureStatus("https://example.com/article", CAPTURE_STATUS_CAPTURING)
        repository.archiveBookmark("https://example.com/article")
        val archivedAt = sourceDao.readingItems.value.single().archivedAt

        repository.saveForLater(
            title = "Captured Article",
            url = "https://example.com/article",
            sourceName = "Example",
            text = "Captured body",
        )

        val item = sourceDao.readingItems.value.single()
        assertTrue(item.isArchived)
        assertEquals(archivedAt, item.archivedAt)
        assertEquals(CAPTURE_STATUS_READY, item.captureStatus)
        assertEquals("Captured body", item.text)
    }

    @Test
    fun `saveForLater falls back to original for unknown capture providers`() = runTest {
        repository.saveForLater(
            title = "Captured Article",
            url = "https://example.com/article",
            sourceName = "Example",
            captureProvider = "bad-provider",
        )

        assertEquals("original", sourceDao.readingItems.value.single().captureProvider)
    }

    @Test
    fun `saveForLater can replace capture provider on recapture`() = runTest {
        repository.saveForLater(
            title = "Captured Article",
            url = "https://example.com/article",
            sourceName = "Example",
            captureProvider = CAPTURE_PROVIDER_PERISCOPE,
        )

        repository.saveForLater(
            title = "Captured Article",
            url = "https://example.com/article",
            sourceName = "Example",
            captureProvider = CAPTURE_PROVIDER_UNWALL,
        )

        assertEquals(CAPTURE_PROVIDER_UNWALL, sourceDao.readingItems.value.single().captureProvider)
    }

    @Test
    fun `queueCaptureAttempt marks bookmark capturing and increments retry metadata`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        repository.queueCaptureAttempt(" https://example.com/article ")
        repository.queueCaptureAttempt("https://example.com/article")

        val item = sourceDao.readingItems.value.single()
        assertEquals(CAPTURE_STATUS_CAPTURING, item.captureStatus)
        assertEquals(2, item.captureAttemptCount)
        assertEquals(null, item.captureLastError)
        assertTrue(item.captureLastAttemptAt != null)
    }

    @Test
    fun `markCaptureFailed records retryable error without deleting bookmark`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")
        repository.queueCaptureAttempt("https://example.com/article")

        repository.markCaptureFailed("https://example.com/article", "Timeout while extracting")

        val item = sourceDao.readingItems.value.single()
        assertEquals(CAPTURE_STATUS_FAILED, item.captureStatus)
        assertEquals(1, item.captureAttemptCount)
        assertEquals("Timeout while extracting", item.captureLastError)
    }

    @Test
    fun `moveBookmarkToFolder updates bookmark folder and exposes it in reading folders`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        repository.moveBookmarkToFolder(" https://example.com/article ", " Long Reads ")

        val item = sourceDao.readingItems.value.single()
        assertEquals("Long Reads", item.folderName)
        assertEquals(listOf("Long Reads"), repository.readingFolders.first())
        assertEquals(emptyList<String>(), repository.sourceFolders.first())
    }

    @Test
    fun `deleteReadingFolder moves matching bookmarks to unfiled`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article", folderName = "Long Reads")

        repository.deleteReadingFolder("Long Reads")

        assertEquals(UNFILED_FOLDER_NAME, sourceDao.readingItems.value.single().folderName)
        assertEquals(listOf(UNFILED_FOLDER_NAME), repository.readingFolders.first())
    }

    @Test
    fun `markRead keeps read later item and records read state`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        repository.markRead("https://example.com/article")

        val item = sourceDao.readingItems.value.single()
        assertEquals("https://example.com/article", item.url)
        assertTrue(item.isRead)
        assertTrue(item.readAt != null)
        assertTrue(item.updatedAt >= item.readAt!!)
    }

    @Test
    fun `markUnread clears read state for undo`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")
        repository.markRead("https://example.com/article")

        repository.markUnread("https://example.com/article")

        val item = sourceDao.readingItems.value.single()
        assertEquals(false, item.isRead)
        assertEquals(null, item.readAt)
    }

    @Test
    fun `archive and restore bookmark drive visible read later list`() = runTest {
        repository.saveBookmarkFromExternalShare("https://example.com/article")

        repository.archiveBookmark("https://example.com/article")

        assertTrue(repository.readingItems.first().isEmpty())
        val archived = sourceDao.readingItems.value.single()
        assertEquals(true, archived.isArchived)
        assertTrue(archived.archivedAt != null)

        repository.restoreBookmark("https://example.com/article")

        val restored = repository.readingItems.first().single()
        assertEquals(false, restored.isArchived)
        assertEquals(null, restored.archivedAt)
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

    override fun getFolders(type: String): Flow<List<FolderEntity>> =
        folders.map { folderList -> folderList.filter { it.type == type } }

    override fun getReadingItems(): Flow<List<ReadingItemEntity>> =
        readingItems.map { items -> items.filterNot { it.isArchived }.sortedByDescending { it.addedAt } }

    override fun getReadingItemFolders(): Flow<List<String>> =
        readingItems.map { items ->
            items
                .filterNot { it.isArchived }
                .map { it.folderName }
                .distinct()
                .sortedBy { it.lowercase() }
        }

    override suspend fun getReadingItemByUrl(url: String): ReadingItemEntity? =
        readingItems.value.firstOrNull { it.url == url }

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
        if (folders.value.any { it.name == folder.name && it.type == folder.type }) {
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

    override suspend fun moveReadingItemsToFolder(folderName: String, targetFolder: String, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.folderName == folderName) {
                item.copy(folderName = targetFolder, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun delete(source: SourceEntity) {
        entities.value = entities.value.filterNot { it.id == source.id }
    }

    override suspend fun deleteFolder(folderName: String, type: String) {
        folders.value = folders.value.filterNot { it.name == folderName && it.type == type }
    }

    override suspend fun countByUrl(url: String): Int =
        entities.value.count { it.url == url }

    override suspend fun countByFolder(folderName: String): Int =
        entities.value.count { it.folderName == folderName }

    override suspend fun count(): Int = entities.value.size

    override suspend fun upsertReadingItem(item: ReadingItemEntity) {
        val existing = readingItems.value.firstOrNull { it.url == item.url }
        val entity = item.copy(id = item.id.takeIf { it != 0L } ?: existing?.id ?: nextReadingId++)
        readingItems.value = readingItems.value.filterNot { it.url == item.url } + entity
    }

    override suspend fun markReadingItemRead(url: String, readAt: Long, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(isRead = true, readAt = readAt, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun markReadingItemUnread(url: String, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(isRead = false, readAt = null, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun archiveReadingItem(url: String, archivedAt: Long, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(isArchived = true, archivedAt = archivedAt, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun restoreReadingItem(url: String, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(isArchived = false, archivedAt = null, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun moveReadingItemToFolder(url: String, folderName: String, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(folderName = folderName, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun updateReadingItemCaptureStatus(url: String, status: String, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(captureStatus = status, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun markReadingItemCaptureAttempt(url: String, attemptedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(
                    captureStatus = CAPTURE_STATUS_CAPTURING,
                    captureAttemptCount = item.captureAttemptCount + 1,
                    captureLastAttemptAt = attemptedAt,
                    captureLastError = null,
                    updatedAt = attemptedAt,
                )
            } else {
                item
            }
        }
    }

    override suspend fun markReadingItemCaptureFailed(url: String, error: String, updatedAt: Long) {
        readingItems.value = readingItems.value.map { item ->
            if (item.url == url) {
                item.copy(captureStatus = CAPTURE_STATUS_FAILED, captureLastError = error, updatedAt = updatedAt)
            } else {
                item
            }
        }
    }

    override suspend fun deleteReadingItem(url: String) {
        readingItems.value = readingItems.value.filterNot { it.url == url }
    }

    override suspend fun countReadingItem(url: String): Int =
        readingItems.value.count { it.url == url }

    override suspend fun countReadingItemsByFolder(folderName: String): Int =
        readingItems.value.count { it.folderName == folderName && !it.isArchived }

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
