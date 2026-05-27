package com.juani.paywallreader.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY folderName COLLATE NOCASE ASC, name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun getFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM reading_items WHERE isArchived = 0 ORDER BY addedAt DESC")
    fun getReadingItems(): Flow<List<ReadingItemEntity>>

    @Query("SELECT DISTINCT folderName FROM reading_items WHERE isArchived = 0 ORDER BY folderName COLLATE NOCASE ASC")
    fun getReadingItemFolders(): Flow<List<String>>

    @Query("SELECT * FROM reading_items WHERE url = :url LIMIT 1")
    suspend fun getReadingItemByUrl(url: String): ReadingItemEntity?

    @Query("SELECT * FROM history_items ORDER BY visitedAt DESC LIMIT 100")
    fun getHistoryItems(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE sources SET name = :name, url = :url, folderName = :folderName WHERE id = :id")
    suspend fun updateSource(id: Long, name: String, url: String, folderName: String)

    @Query("UPDATE sources SET folderName = :targetFolder WHERE folderName = :folderName")
    suspend fun moveSourcesToFolder(folderName: String, targetFolder: String)

    @Query("UPDATE reading_items SET folderName = :targetFolder, updatedAt = :updatedAt WHERE folderName = :folderName")
    suspend fun moveReadingItemsToFolder(folderName: String, targetFolder: String, updatedAt: Long)

    @Delete
    suspend fun delete(source: SourceEntity)

    @Query("DELETE FROM folders WHERE name = :folderName")
    suspend fun deleteFolder(folderName: String)

    @Query("SELECT COUNT(*) FROM sources WHERE url = :url")
    suspend fun countByUrl(url: String): Int

    @Query("SELECT COUNT(*) FROM sources WHERE folderName = :folderName")
    suspend fun countByFolder(folderName: String): Int

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingItem(item: ReadingItemEntity)

    @Query("UPDATE reading_items SET isRead = 1, readAt = :readAt, updatedAt = :updatedAt WHERE url = :url")
    suspend fun markReadingItemRead(url: String, readAt: Long, updatedAt: Long)

    @Query("UPDATE reading_items SET isArchived = 1, archivedAt = :archivedAt, updatedAt = :updatedAt WHERE url = :url")
    suspend fun archiveReadingItem(url: String, archivedAt: Long, updatedAt: Long)

    @Query("UPDATE reading_items SET folderName = :folderName, updatedAt = :updatedAt WHERE url = :url")
    suspend fun moveReadingItemToFolder(url: String, folderName: String, updatedAt: Long)

    @Query("DELETE FROM reading_items WHERE url = :url")
    suspend fun deleteReadingItem(url: String)

    @Query("SELECT COUNT(*) FROM reading_items WHERE url = :url")
    suspend fun countReadingItem(url: String): Int

    @Query("SELECT COUNT(*) FROM reading_items WHERE folderName = :folderName AND isArchived = 0")
    suspend fun countReadingItemsByFolder(folderName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryEntity): Long

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()

    @Query("DELETE FROM history_items WHERE id NOT IN (SELECT id FROM history_items ORDER BY visitedAt DESC LIMIT :limit)")
    suspend fun trimHistory(limit: Int = 100)
}
