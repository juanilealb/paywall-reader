package com.juani.paywallreader.data.local

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.juani.paywallreader.R

@Database(
    entities = [SourceEntity::class],
    version = 1,
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
                .addCallback(DefaultSourcesCallback(context))
                .build()
    }
}

private class DefaultSourcesCallback(
    private val context: Context,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        defaultSources(context).forEach { source ->
            db.insert("sources", 0, source.toContentValues())
        }
    }

    private fun defaultSources(context: Context): List<SourceEntity> =
        listOf(
            SourceEntity(
                name = context.getString(R.string.default_source_clarin),
                url = "https://www.clarin.com",
                isDefault = true,
            ),
            SourceEntity(
                name = context.getString(R.string.default_source_lanacion),
                url = "https://www.lanacion.com.ar",
                isDefault = true,
            ),
            SourceEntity(
                name = context.getString(R.string.default_source_infobae),
                url = "https://www.infobae.com",
                isDefault = true,
            ),
            SourceEntity(
                name = context.getString(R.string.default_source_ambito),
                url = "https://www.ambito.com",
                isDefault = true,
            ),
            SourceEntity(
                name = context.getString(R.string.default_source_pagina12),
                url = "https://www.pagina12.com.ar",
                isDefault = true,
            ),
        )

    private fun SourceEntity.toContentValues(): ContentValues =
        ContentValues().apply {
            put("name", name)
            put("url", url)
            put("isDefault", isDefault)
            put("createdAt", createdAt)
        }
}
