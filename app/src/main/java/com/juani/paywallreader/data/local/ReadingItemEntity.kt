package com.juani.paywallreader.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_items",
    indices = [Index(value = ["url"], unique = true)],
)
data class ReadingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val sourceName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val resolvedUrl: String? = null,
    val author: String? = null,
    val excerpt: String? = null,
    val html: String? = null,
    val text: String? = null,
    val markdown: String? = null,
    val imageUrl: String? = null,
    val readingProgress: Float = 0f,
    val folderName: String = "Sin carpeta",
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val isArchived: Boolean = false,
    val archivedAt: Long? = null,
    val updatedAt: Long = 0,
    val captureStatus: String = "ready",
    val captureProvider: String = "original",
    val captureAttemptCount: Int = 0,
    val captureLastAttemptAt: Long? = null,
    val captureLastError: String? = null,
)
