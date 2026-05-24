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
)
