package com.juani.paywallreader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juani.paywallreader.data.local.AppDatabase
import com.juani.paywallreader.data.reader.CAPTURE_PROVIDER_ORIGINAL
import com.juani.paywallreader.data.repository.SourceRepository
import com.juani.paywallreader.domain.model.ReadingItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SourceRepository(
        AppDatabase.getInstance(application).sourceDao(),
    )

    val uiState: StateFlow<ReaderUiState> = combine(
        repository.readingFolders,
        repository.readingItems,
    ) { folders, readingItems ->
        ReaderUiState(
            readingFolders = folders,
            readingItems = readingItems,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderUiState(),
    )

    fun saveForLater(
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String? = null,
        author: String? = null,
        excerpt: String? = null,
        html: String? = null,
        text: String? = null,
        markdown: String? = null,
        imageUrl: String? = null,
        captureProvider: String? = null,
    ) {
        viewModelScope.launch {
            repository.saveForLater(
                title = title,
                url = url,
                sourceName = sourceName,
                resolvedUrl = resolvedUrl,
                author = author,
                excerpt = excerpt,
                html = html,
                text = text,
                markdown = markdown,
                imageUrl = imageUrl,
                captureProvider = captureProvider ?: CAPTURE_PROVIDER_ORIGINAL,
            )
        }
    }

    fun moveBookmarkToFolder(url: String, folderName: String) {
        viewModelScope.launch {
            repository.moveBookmarkToFolder(url, folderName)
        }
    }

    fun createReadingFolder(folderName: String) {
        viewModelScope.launch {
            repository.createReadingFolder(folderName)
        }
    }

    fun markRead(url: String) {
        viewModelScope.launch {
            repository.markRead(url)
        }
    }

    fun archiveBookmark(url: String) {
        viewModelScope.launch {
            repository.archiveBookmark(url)
        }
    }

    fun recordVisit(title: String, url: String, sourceName: String) {
        viewModelScope.launch {
            repository.recordVisit(title, url, sourceName)
        }
    }

    fun updateCaptureStatus(url: String, status: String) {
        viewModelScope.launch {
            repository.updateCaptureStatus(url, status)
        }
    }
}

data class ReaderUiState(
    val readingFolders: List<String> = emptyList(),
    val readingItems: List<ReadingItem> = emptyList(),
)
