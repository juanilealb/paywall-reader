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

    @Query("SELECT * FROM reading_items ORDER BY addedAt DESC")
    fun getReadingItems(): Flow<List<ReadingItemEntity>>

    @Query("SELECT * FROM history_items ORDER BY visitedAt DESC LIMIT 100")
    fun getHistoryItems(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Delete
    suspend fun delete(source: SourceEntity)

    @Query("SELECT COUNT(*) FROM sources WHERE url = :url")
    suspend fun countByUrl(url: String): Int

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingItem(item: ReadingItemEntity)

    @Query("DELETE FROM reading_items WHERE url = :url")
    suspend fun deleteReadingItem(url: String)

    @Query("SELECT COUNT(*) FROM reading_items WHERE url = :url")
    suspend fun countReadingItem(url: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryEntity): Long

    @Query("DELETE FROM history_items WHERE id NOT IN (SELECT id FROM history_items ORDER BY visitedAt DESC LIMIT :limit)")
    suspend fun trimHistory(limit: Int = 100)
}
