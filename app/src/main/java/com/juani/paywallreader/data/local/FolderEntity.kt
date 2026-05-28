package com.juani.paywallreader.data.local

import androidx.room.Entity

const val FOLDER_TYPE_SOURCE = "source"
const val FOLDER_TYPE_READING = "reading"

@Entity(tableName = "folders", primaryKeys = ["name", "type"])
data class FolderEntity(
    val name: String,
    val type: String = FOLDER_TYPE_SOURCE,
    val createdAt: Long = System.currentTimeMillis(),
)
