package com.juani.paywallreader.data.repository

import com.juani.paywallreader.data.local.SourceDao
import com.juani.paywallreader.data.local.FOLDER_TYPE_READING
import com.juani.paywallreader.data.local.FOLDER_TYPE_SOURCE
import com.juani.paywallreader.data.local.FolderEntity
import com.juani.paywallreader.data.local.HistoryEntity
import com.juani.paywallreader.data.local.ReadingItemEntity
import com.juani.paywallreader.data.local.SourceEntity
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_ACCESS_ARTICLE_NOW
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_ARCHIVE
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_ORIGINAL
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_PERISCOPE
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_REMOVE_PAYWALLS
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_UNWALL
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_X
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_CAPTURING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_FAILED
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_PENDING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_READY
import com.juani.paywallreader.domain.model.HistoryItem
import com.juani.paywallreader.domain.model.ReadingItem
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.domain.model.UNFILED_FOLDER_NAME
import com.juani.paywallreader.domain.model.validateSourceUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.net.URI

class SourceRepository(
    private val sourceDao: SourceDao,
) {
    val sources: Flow<List<Source>> = sourceDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    val sourceFolders: Flow<List<String>> = combine(
        sourceDao.getFolders(FOLDER_TYPE_SOURCE),
        sourceDao.getAll(),
    ) { entities, sources ->
        (entities.map { it.name.normalizedFolderName() } + sources.map { it.folderName.normalizedFolderName() })
            .distinct()
            .filterNot {
                it == UNFILED_FOLDER_NAME && sourceDao.countByFolder(UNFILED_FOLDER_NAME) == 0
            }
            .sortedBy { it.lowercase() }
    }

    val readingFolders: Flow<List<String>> = combine(
        sourceDao.getFolders(FOLDER_TYPE_READING),
        sourceDao.getReadingItemFolders(),
    ) { entities, readingItemFolders ->
        (entities.map { it.name.normalizedFolderName() } + readingItemFolders.map { it.normalizedFolderName() })
            .distinct()
            .filterNot {
                it == UNFILED_FOLDER_NAME && sourceDao.countReadingItemsByFolder(UNFILED_FOLDER_NAME) == 0
            }
            .sortedBy { it.lowercase() }
    }

    val folders: Flow<List<String>> = sourceFolders

    val readingItems: Flow<List<ReadingItem>> = sourceDao.getReadingItems().map { entities ->
        entities.map { it.toDomain() }
    }

    val historyItems: Flow<List<HistoryItem>> = sourceDao.getHistoryItems().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addSource(name: String, url: String, folderName: String = UNFILED_FOLDER_NAME) {
        val validatedUrl = validateSourceUrl(url)
        if (name.isBlank() || !validatedUrl.isValid || sourceDao.countByUrl(validatedUrl.normalizedUrl) > 0) {
            return
        }

        val normalizedFolderName = folderName.normalizedFolderName()
        sourceDao.insertFolder(FolderEntity(name = normalizedFolderName, type = FOLDER_TYPE_SOURCE))
        sourceDao.insert(
            SourceEntity(
                name = name.trim(),
                url = validatedUrl.normalizedUrl,
                isDefault = false,
                folderName = normalizedFolderName,
            ),
        )
    }

    suspend fun createFolder(folderName: String) = createSourceFolder(folderName)

    suspend fun createSourceFolder(folderName: String) {
        val normalizedFolderName = folderName.normalizedFolderName()
        if (normalizedFolderName != UNFILED_FOLDER_NAME) {
            sourceDao.insertFolder(FolderEntity(name = normalizedFolderName, type = FOLDER_TYPE_SOURCE))
        }
    }

    suspend fun createReadingFolder(folderName: String) {
        val normalizedFolderName = folderName.normalizedFolderName()
        if (normalizedFolderName != UNFILED_FOLDER_NAME) {
            sourceDao.insertFolder(FolderEntity(name = normalizedFolderName, type = FOLDER_TYPE_READING))
        }
    }

    suspend fun deleteFolder(folderName: String) = deleteSourceFolder(folderName)

    suspend fun deleteSourceFolder(folderName: String) {
        val normalizedFolderName = folderName.normalizedFolderName()
        if (normalizedFolderName == UNFILED_FOLDER_NAME) return

        sourceDao.insertFolder(FolderEntity(name = UNFILED_FOLDER_NAME, type = FOLDER_TYPE_SOURCE))
        sourceDao.moveSourcesToFolder(
            folderName = normalizedFolderName,
            targetFolder = UNFILED_FOLDER_NAME,
        )
        sourceDao.deleteFolder(normalizedFolderName, FOLDER_TYPE_SOURCE)
    }

    suspend fun deleteReadingFolder(folderName: String) {
        val normalizedFolderName = folderName.normalizedFolderName()
        if (normalizedFolderName == UNFILED_FOLDER_NAME) return

        sourceDao.insertFolder(FolderEntity(name = UNFILED_FOLDER_NAME, type = FOLDER_TYPE_READING))
        sourceDao.moveReadingItemsToFolder(
            folderName = normalizedFolderName,
            targetFolder = UNFILED_FOLDER_NAME,
            updatedAt = System.currentTimeMillis(),
        )
        sourceDao.deleteFolder(normalizedFolderName, FOLDER_TYPE_READING)
    }

    suspend fun updateSource(source: Source, name: String, url: String, folderName: String) {
        val validatedUrl = validateSourceUrl(url)
        if (name.isBlank() || !validatedUrl.isValid) return
        val normalizedUrl = validatedUrl.normalizedUrl
        if (normalizedUrl != source.url && sourceDao.countByUrl(normalizedUrl) > 0) return

        val normalizedFolderName = folderName.normalizedFolderName()
        sourceDao.insertFolder(FolderEntity(name = normalizedFolderName, type = FOLDER_TYPE_SOURCE))
        sourceDao.updateSource(
            id = source.id,
            name = name.trim(),
            url = normalizedUrl,
            folderName = normalizedFolderName,
        )
        pruneEmptyUnfiledFolder()
    }

    suspend fun deleteSource(source: Source) {
        sourceDao.delete(source.toEntity())
        pruneEmptyUnfiledFolder()
    }

    suspend fun saveForLater(
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String? = null,
        author: String? = null,
        excerpt: String? = null,
        html: String? = null,
        text: String? = null,
        markdown: String? = null,
        imageUrl: String? = null,
        captureProvider: String = CAPTURE_PROVIDER_ORIGINAL,
    ) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return
        val now = System.currentTimeMillis()
        val existing = sourceDao.getReadingItemByUrl(validatedUrl.normalizedUrl)

        sourceDao.upsertReadingItem(
            ReadingItemEntity(
                id = existing?.id ?: 0,
                title = title.trim().ifBlank { validatedUrl.normalizedUrl.toDisplayTitle() },
                url = validatedUrl.normalizedUrl,
                sourceName = sourceName.ifBlank { validatedUrl.normalizedUrl.toDisplayTitle() },
                addedAt = existing?.addedAt ?: now,
                resolvedUrl = resolvedUrl?.trim()?.takeIf { it.isNotBlank() },
                author = author?.trim()?.takeIf { it.isNotBlank() },
                excerpt = excerpt?.trim()?.takeIf { it.isNotBlank() },
                html = html?.trim()?.takeIf { it.isNotBlank() },
                text = text?.trim()?.takeIf { it.isNotBlank() },
                markdown = markdown?.trim()?.takeIf { it.isNotBlank() },
                imageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() },
                folderName = existing?.folderName ?: UNFILED_FOLDER_NAME,
                isRead = existing?.isRead ?: false,
                readAt = existing?.readAt,
                isArchived = existing?.isArchived ?: false,
                archivedAt = existing?.archivedAt,
                updatedAt = now,
                captureStatus = CAPTURE_STATUS_READY,
                captureProvider = captureProvider.normalizedCaptureProvider(),
                captureAttemptCount = existing?.captureAttemptCount ?: 0,
                captureLastAttemptAt = existing?.captureLastAttemptAt,
                captureLastError = null,
            ),
        )
    }

    suspend fun saveBookmarkFromExternalShare(
        url: String,
        folderName: String = UNFILED_FOLDER_NAME,
    ) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return
        val normalizedUrl = validatedUrl.normalizedUrl
        val now = System.currentTimeMillis()
        val normalizedFolderName = folderName.normalizedFolderName()
        if (normalizedFolderName != UNFILED_FOLDER_NAME) {
            sourceDao.insertFolder(FolderEntity(name = normalizedFolderName, type = FOLDER_TYPE_READING))
        }
        val displayTitle = normalizedUrl.toDisplayTitle()
        val existing = sourceDao.getReadingItemByUrl(normalizedUrl)

        sourceDao.upsertReadingItem(
            existing?.copy(
                folderName = normalizedFolderName,
                isArchived = false,
                archivedAt = null,
                updatedAt = now,
            ) ?: ReadingItemEntity(
                title = displayTitle,
                url = normalizedUrl,
                sourceName = normalizedUrl.toHostName().ifBlank { displayTitle },
                addedAt = now,
                folderName = normalizedFolderName,
                updatedAt = now,
                captureStatus = CAPTURE_STATUS_PENDING,
            ),
        )
    }

    suspend fun updateCaptureStatus(url: String, status: String) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return
        val safeStatus = when (status) {
            CAPTURE_STATUS_PENDING,
            CAPTURE_STATUS_CAPTURING,
            CAPTURE_STATUS_READY,
            CAPTURE_STATUS_FAILED -> status
            else -> CAPTURE_STATUS_PENDING
        }
        sourceDao.updateReadingItemCaptureStatus(
            url = validatedUrl.normalizedUrl,
            status = safeStatus,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun queueCaptureAttempt(url: String) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return
        sourceDao.markReadingItemCaptureAttempt(
            url = validatedUrl.normalizedUrl,
            attemptedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markCaptureFailed(url: String, error: String) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return
        sourceDao.markReadingItemCaptureFailed(
            url = validatedUrl.normalizedUrl,
            error = error.trim().ifBlank { "No se pudo extraer el artículo" }.take(240),
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun moveBookmarkToFolder(url: String, folderName: String) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return

        val normalizedFolderName = folderName.normalizedFolderName()
        if (normalizedFolderName != UNFILED_FOLDER_NAME) {
            sourceDao.insertFolder(FolderEntity(name = normalizedFolderName, type = FOLDER_TYPE_READING))
        }
        sourceDao.moveReadingItemToFolder(
            url = validatedUrl.normalizedUrl,
            folderName = normalizedFolderName,
            updatedAt = System.currentTimeMillis(),
        )
        pruneEmptyUnfiledFolder()
    }

    suspend fun markRead(url: String) {
        val validatedUrl = validateSourceUrl(url)
        if (validatedUrl.isValid) {
            val now = System.currentTimeMillis()
            sourceDao.markReadingItemRead(validatedUrl.normalizedUrl, readAt = now, updatedAt = now)
        }
    }

    suspend fun markUnread(url: String) {
        val validatedUrl = validateSourceUrl(url)
        if (validatedUrl.isValid) {
            sourceDao.markReadingItemUnread(validatedUrl.normalizedUrl, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun archiveBookmark(url: String) {
        val validatedUrl = validateSourceUrl(url)
        if (validatedUrl.isValid) {
            val now = System.currentTimeMillis()
            sourceDao.archiveReadingItem(validatedUrl.normalizedUrl, archivedAt = now, updatedAt = now)
            pruneEmptyUnfiledFolder()
        }
    }

    suspend fun restoreBookmark(url: String) {
        val validatedUrl = validateSourceUrl(url)
        if (validatedUrl.isValid) {
            sourceDao.restoreReadingItem(validatedUrl.normalizedUrl, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun recordVisit(title: String, url: String, sourceName: String) {
        val validatedUrl = validateSourceUrl(url)
        if (!validatedUrl.isValid) return

        sourceDao.insertHistoryItem(
            HistoryEntity(
                title = title.trim().ifBlank { validatedUrl.normalizedUrl.toDisplayTitle() },
                url = validatedUrl.normalizedUrl,
                sourceName = sourceName.ifBlank { validatedUrl.normalizedUrl.toDisplayTitle() },
            ),
        )
        sourceDao.trimHistory()
    }

    suspend fun clearHistory() {
        sourceDao.clearHistory()
    }

    private suspend fun pruneEmptyUnfiledFolder() {
        if (sourceDao.countByFolder(UNFILED_FOLDER_NAME) == 0) {
            sourceDao.deleteFolder(UNFILED_FOLDER_NAME, FOLDER_TYPE_SOURCE)
        }
        if (sourceDao.countReadingItemsByFolder(UNFILED_FOLDER_NAME) == 0) {
            sourceDao.deleteFolder(UNFILED_FOLDER_NAME, FOLDER_TYPE_READING)
        }
    }
}

private fun SourceEntity.toDomain(): Source =
    Source(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
        folderName = folderName.normalizedFolderName(),
    )

private fun Source.toEntity(): SourceEntity =
    SourceEntity(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
        folderName = folderName.normalizedFolderName(),
    )

private fun ReadingItemEntity.toDomain(): ReadingItem =
    ReadingItem(
        id = id,
        title = title,
        url = url,
        sourceName = sourceName,
        addedAt = addedAt,
        resolvedUrl = resolvedUrl,
        author = author,
        excerpt = excerpt,
        html = html,
        text = text,
        markdown = markdown,
        imageUrl = imageUrl,
        readingProgress = readingProgress,
        folderName = folderName.normalizedFolderName(),
        isRead = isRead,
        readAt = readAt,
        isArchived = isArchived,
        archivedAt = archivedAt,
        updatedAt = updatedAt,
        captureStatus = captureStatus,
        captureProvider = captureProvider,
        captureAttemptCount = captureAttemptCount,
        captureLastAttemptAt = captureLastAttemptAt,
        captureLastError = captureLastError,
    )

private fun HistoryEntity.toDomain(): HistoryItem =
    HistoryItem(
        id = id,
        title = title,
        url = url,
        sourceName = sourceName,
        visitedAt = visitedAt,
    )

private fun String.normalizedFolderName(): String =
    trim().ifBlank { UNFILED_FOLDER_NAME }

private fun String.normalizedCaptureProvider(): String = when (trim()) {
    CAPTURE_PROVIDER_PERISCOPE,
    CAPTURE_PROVIDER_ACCESS_ARTICLE_NOW,
    CAPTURE_PROVIDER_UNWALL,
    CAPTURE_PROVIDER_ARCHIVE,
    CAPTURE_PROVIDER_REMOVE_PAYWALLS,
    CAPTURE_PROVIDER_X,
    CAPTURE_PROVIDER_ORIGINAL -> trim()
    else -> CAPTURE_PROVIDER_ORIGINAL
}

private fun String.toDisplayTitle(): String =
    removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .removePrefix("www.")

private fun String.toHostName(): String =
    runCatching { URI(this).host.orEmpty().removePrefix("www.") }.getOrDefault("")
