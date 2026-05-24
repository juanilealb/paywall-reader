package com.juani.paywallreader.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SourceEntity::class],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
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
        isDefault = true,
        createdAt = 1,
    ),
    SourceEntity(
        name = "The Verge",
        url = "https://www.theverge.com",
        isDefault = true,
        createdAt = 2,
    ),
    SourceEntity(
        name = "The New York Times",
        url = "https://www.nytimes.com",
        isDefault = true,
        createdAt = 3,
    ),
    SourceEntity(
        name = "WIRED",
        url = "https://www.wired.com",
        isDefault = true,
        createdAt = 4,
    ),
    SourceEntity(
        name = "The Economist",
        url = "https://www.economist.com",
        isDefault = true,
        createdAt = 5,
    ),
)

private fun SourceEntity.toContentValues(): ContentValues =
    ContentValues().apply {
        put("name", name)
        put("url", url)
        put("isDefault", isDefault)
        put("createdAt", createdAt)
    }
