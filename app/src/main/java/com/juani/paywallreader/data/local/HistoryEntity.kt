package com.juani.paywallreader.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_items",
    indices = [Index(value = ["url"])],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val sourceName: String,
    val visitedAt: Long = System.currentTimeMillis(),
)
