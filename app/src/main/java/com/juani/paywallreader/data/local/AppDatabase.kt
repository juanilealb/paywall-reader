package com.juani.paywallreader.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SourceEntity::class, ReadingItemEntity::class, HistoryEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "paywall_reader.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(DefaultSourcesCallback())
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM sources WHERE isDefault = 1")
                DEFAULT_SOURCES.forEach { source ->
                    db.insert("sources", 0, source.toContentValues())
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sources ADD COLUMN folderName TEXT NOT NULL DEFAULT 'News'")
                db.execSQL("UPDATE sources SET isDefault = 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reading_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reading_items_url ON reading_items(url)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS history_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        visitedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_history_items_url ON history_items(url)")
            }
        }
    }
}

private class DefaultSourcesCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DEFAULT_SOURCES.forEach { source ->
            db.insert("sources", 0, source.toContentValues())
        }
    }
}

private val DEFAULT_SOURCES = listOf(
    SourceEntity(
        name = "Clarín",
        url = "https://www.clarin.com",
        isDefault = false,
        folderName = "News",
        createdAt = 1,
    ),
    SourceEntity(
        name = "The Verge",
        url = "https://www.theverge.com",
        isDefault = false,
        folderName = "News",
        createdAt = 2,
    ),
    SourceEntity(
        name = "The New York Times",
        url = "https://www.nytimes.com",
        isDefault = false,
        folderName = "News",
        createdAt = 3,
    ),
    SourceEntity(
        name = "WIRED",
        url = "https://www.wired.com",
        isDefault = false,
        folderName = "News",
        createdAt = 4,
    ),
    SourceEntity(
        name = "The Economist",
        url = "https://www.economist.com",
        isDefault = false,
        folderName = "News",
        createdAt = 5,
    ),
)

private fun SourceEntity.toContentValues(): ContentValues =
    ContentValues().apply {
        put("name", name)
        put("url", url)
        put("isDefault", isDefault)
        put("folderName", folderName)
        put("createdAt", createdAt)
    }
