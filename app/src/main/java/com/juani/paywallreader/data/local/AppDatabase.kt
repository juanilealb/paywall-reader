package com.juani.paywallreader.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SourceEntity::class, ReadingItemEntity::class, HistoryEntity::class, FolderEntity::class],
    version = 6,
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS folders (
                        name TEXT PRIMARY KEY NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO folders(name, createdAt)
                    SELECT DISTINCT folderName, MIN(createdAt)
                    FROM sources
                    GROUP BY folderName
                    """.trimIndent(),
                )
                db.execSQL("INSERT OR IGNORE INTO folders(name, createdAt) VALUES ('News', 1)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO folders(name, createdAt) VALUES ('$BLOGS_FOLDER', 2)")
                db.execSQL(
                    """
                    INSERT INTO sources(name, url, isDefault, folderName, createdAt)
                    SELECT '${MEDIUM_SOURCE.name}', '${MEDIUM_SOURCE.url}', 0, '$BLOGS_FOLDER', ${MEDIUM_SOURCE.createdAt}
                    WHERE NOT EXISTS (SELECT 1 FROM sources WHERE url = '${MEDIUM_SOURCE.url}')
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO folders(name, createdAt) VALUES ('$BLOGS_FOLDER', 2)")
                db.execSQL(
                    """
                    INSERT INTO sources(name, url, isDefault, folderName, createdAt)
                    SELECT '${SUBSTACK_SOURCE.name}', '${SUBSTACK_SOURCE.url}', 0, '$BLOGS_FOLDER', ${SUBSTACK_SOURCE.createdAt}
                    WHERE NOT EXISTS (SELECT 1 FROM sources WHERE url = '${SUBSTACK_SOURCE.url}')
                    """.trimIndent(),
                )
            }
        }
    }
}

private class DefaultSourcesCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.insert("folders", 0, "News".toContentValues(createdAt = 1))
        db.insert("folders", 0, BLOGS_FOLDER.toContentValues(createdAt = 2))
        DEFAULT_SOURCES.forEach { source ->
            db.insert("sources", 0, source.toContentValues())
        }
    }
}

private const val BLOGS_FOLDER = "Blogs"

private val MEDIUM_SOURCE = SourceEntity(
    name = "Medium",
    url = "https://medium.com",
    isDefault = false,
    folderName = BLOGS_FOLDER,
    createdAt = 6,
)

private val SUBSTACK_SOURCE = SourceEntity(
    name = "Substack",
    url = "https://substack.com",
    isDefault = false,
    folderName = BLOGS_FOLDER,
    createdAt = 7,
)

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
    MEDIUM_SOURCE,
    SUBSTACK_SOURCE,
)

private fun SourceEntity.toContentValues(): ContentValues =
    ContentValues().apply {
        put("name", name)
        put("url", url)
        put("isDefault", isDefault)
        put("folderName", folderName)
        put("createdAt", createdAt)
    }

private fun String.toContentValues(createdAt: Long): ContentValues =
    ContentValues().apply {
        put("name", this@toContentValues)
        put("createdAt", createdAt)
    }
