package com.juani.paywallreader.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juani.paywallreader.data.local.AppDatabase
import com.juani.paywallreader.data.repository.SourceRepository
import com.juani.paywallreader.domain.model.HistoryItem
import com.juani.paywallreader.domain.model.ReadingItem
import com.juani.paywallreader.domain.model.Source
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SourceRepository(
        AppDatabase.getInstance(application).sourceDao(),
    )

    val uiState: StateFlow<HomeUiState> = combine(
        repository.sources,
        repository.readingItems,
        repository.historyItems,
    ) { sources, readingItems, historyItems ->
        HomeUiState(
            sources = sources,
            readingItems = readingItems,
            historyItems = historyItems,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun addSource(name: String, url: String, folderName: String) {
        viewModelScope.launch {
            repository.addSource(name, url, folderName)
        }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch {
            repository.deleteSource(source)
        }
    }

    fun saveForLater(title: String, url: String, sourceName: String) {
        viewModelScope.launch {
            repository.saveForLater(title, url, sourceName)
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

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

data class HomeUiState(
    val sources: List<Source> = emptyList(),
    val readingItems: List<ReadingItem> = emptyList(),
    val historyItems: List<HistoryItem> = emptyList(),
)
