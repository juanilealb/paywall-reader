package com.juani.paywallreader.data.repository

import com.juani.paywallreader.data.local.SourceDao
import com.juani.paywallreader.data.local.SourceEntity
import com.juani.paywallreader.domain.model.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourceRepository(
    private val sourceDao: SourceDao,
) {
    val sources: Flow<List<Source>> = sourceDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addSource(name: String, url: String) {
        sourceDao.insert(
            SourceEntity(
                name = name.trim(),
                url = url.trim(),
                isDefault = false,
            ),
        )
    }

    suspend fun deleteSource(source: Source) {
        if (!source.isDefault) {
            sourceDao.delete(source.toEntity())
        }
    }
}

private fun SourceEntity.toDomain(): Source =
    Source(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
    )

private fun Source.toEntity(): SourceEntity =
    SourceEntity(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
    )
