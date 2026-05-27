package com.juani.paywallreader.ui.navigation

import java.net.URI

data class ExternalShareRouteDecision(
    val captureUrl: String?,
    val openVisibleReader: Boolean,
)

object ExternalShareRoutePolicy {
    fun decide(sharedText: String?): ExternalShareRouteDecision {
        val url = sharedText
            ?.trim()
            ?.takeIf { it.hasWebSchemeAndHost() }

        return ExternalShareRouteDecision(
            captureUrl = url,
            openVisibleReader = false,
        )
    }
}

private fun String.hasWebSchemeAndHost(): Boolean = runCatching {
    val uri = URI(this)
    uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
}.getOrDefault(false)
