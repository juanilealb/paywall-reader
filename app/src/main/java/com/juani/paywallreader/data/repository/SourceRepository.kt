package com.juani.paywallreader.data.repository

import com.juani.paywallreader.data.local.SourceDao
import com.juani.paywallreader.data.local.SourceEntity
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.domain.model.validateSourceUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SourceRepository(
    private val sourceDao: SourceDao,
) {
    val sources: Flow<List<Source>> = sourceDao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun addSource(name: String, url: String) {
        val validatedUrl = validateSourceUrl(url)
        if (name.isBlank() || !validatedUrl.isValid || sourceDao.countByUrl(validatedUrl.normalizedUrl) > 0) {
            return
        }

        sourceDao.insert(
            SourceEntity(
                name = name.trim(),
                url = validatedUrl.normalizedUrl,
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
