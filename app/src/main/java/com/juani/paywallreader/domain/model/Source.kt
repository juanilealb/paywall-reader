package com.juani.paywallreader.domain.model

data class Source(
    val id: Long,
    val name: String,
    val url: String,
    val isDefault: Boolean,
    val folderName: String = "News",
)

data class ReadingItem(
    val id: Long,
    val title: String,
    val url: String,
    val sourceName: String,
    val addedAt: Long,
)

data class HistoryItem(
    val id: Long,
    val title: String,
    val url: String,
    val sourceName: String,
    val visitedAt: Long,
)
