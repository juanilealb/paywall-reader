package com.juani.paywallreader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.juani.paywallreader.domain.model.UNFILED_FOLDER_NAME

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
    val folderName: String = UNFILED_FOLDER_NAME,
    val createdAt: Long = System.currentTimeMillis(),
)
