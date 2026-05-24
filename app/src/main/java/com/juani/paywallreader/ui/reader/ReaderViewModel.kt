package com.juani.paywallreader.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val url: String = checkNotNull(savedStateHandle.get<String>("url"))
    val name: String = savedStateHandle.get<String>("name")
        ?.takeIf { it.isNotBlank() }
        ?: url
}
