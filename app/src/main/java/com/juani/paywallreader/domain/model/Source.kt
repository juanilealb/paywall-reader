package com.juani.paywallreader.domain.model

const val UNFILED_FOLDER_NAME = "Sin carpeta"

data class Source(
    val id: Long,
    val name: String,
    val url: String,
    val isDefault: Boolean,
    val folderName: String = UNFILED_FOLDER_NAME,
)

data class ReadingItem(
    val id: Long,
    val title: String,
    val url: String,
    val sourceName: String,
    val addedAt: Long,
    val resolvedUrl: String? = null,
    val author: String? = null,
    val excerpt: String? = null,
    val html: String? = null,
    val text: String? = null,
    val markdown: String? = null,
    val imageUrl: String? = null,
    val readingProgress: Float = 0f,
    val folderName: String = UNFILED_FOLDER_NAME,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val updatedAt: Long = 0,
    val captureStatus: String = "ready",
)

data class HistoryItem(
    val id: Long,
    val title: String,
    val url: String,
    val sourceName: String,
    val visitedAt: Long,
)
