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
}
