package com.juani.paywallreader.domain.model

import java.net.URI

data class SourceUrlValidation(
    val normalizedUrl: String,
    val isValid: Boolean,
)

fun validateSourceUrl(input: String): SourceUrlValidation {
    val trimmed = input.trim()
    if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) {
        return SourceUrlValidation(normalizedUrl = "", isValid = false)
    }

    val candidate = when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed
        else -> "https://$trimmed"
    }

    val uri = runCatching { URI(candidate).normalize() }.getOrNull()
        ?: return SourceUrlValidation(normalizedUrl = candidate, isValid = false)
    val scheme = uri.scheme?.lowercase()
    val host = uri.host?.lowercase()
    val isValid = (scheme == "https" || scheme == "http") &&
        !host.isNullOrBlank() &&
        host.contains(".")

    val normalized = if (isValid) {
        URI(
            scheme,
            uri.userInfo,
            host,
            uri.port,
            uri.path?.takeUnless { it == "/" },
            uri.query,
            uri.fragment,
        ).toString().trimEnd('/')
    } else {
        candidate
    }

    return SourceUrlValidation(normalizedUrl = normalized, isValid = isValid)
}
