package com.juani.paywallreader

import android.content.Intent

const val EXTRA_OPEN_READER_URL = "com.juani.paywallreader.extra.OPEN_READER_URL"

fun Intent?.isExternalShareIntent(): Boolean =
    this?.action == Intent.ACTION_SEND || this?.action == Intent.ACTION_VIEW

fun Intent?.extractSharedUrl(): String? {
    if (this == null) return null
    val candidate = when (action) {
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> dataString
        else -> null
    }
    return candidate?.extractFirstWebUrl()
}

private fun String.extractFirstWebUrl(): String? =
    Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        .find(this)
        ?.value
        ?.trimEnd('.', ',', ';', ':', ')', ']', '}', '>', '"', '\'')
