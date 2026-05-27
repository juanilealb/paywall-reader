package com.juani.paywallreader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juani.paywallreader.data.local.AppDatabase
import com.juani.paywallreader.data.repository.SourceRepository
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SourceRepository(
        AppDatabase.getInstance(application).sourceDao(),
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
                captureProvider = captureProvider.orEmpty(),
            )
        }
    }

    fun markRead(url: String) {
        viewModelScope.launch {
            repository.markRead(url)
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
