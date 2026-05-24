package com.juani.paywallreader.domain.model

data class Source(
    val id: Long,
    val name: String,
    val url: String,
    val isDefault: Boolean,
)
