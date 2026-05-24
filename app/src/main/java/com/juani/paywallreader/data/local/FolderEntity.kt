package com.juani.paywallreader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
