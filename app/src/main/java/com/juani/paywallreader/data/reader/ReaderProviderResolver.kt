package com.juani.paywallreader.data.reader

import android.net.Uri

const val REMOVE_PAYWALLS_HOST = "removepaywalls.com"
const val PERISCOPE_HOST = "periscope.corsfix.com"
const val ARTICLE_READER_HOST = "accessarticlenow.com"
const val UNWALL_HOST = "unwall.app"
const val ARCHIVE_FO_HOST = "archive.fo"

val ALLOWED_READER_HOSTS = setOf(
    REMOVE_PAYWALLS_HOST,
    PERISCOPE_HOST,
    ARTICLE_READER_HOST,
    UNWALL_HOST,
    "archive.today",
    ARCHIVE_FO_HOST,
    "archive.is",
    "archive.ph",
)

private val PAYWALL_FALLBACK_HOSTS = setOf(
    "www.wired.com",
    "wired.com",
    "medium.com",
    "substack.com",
)

private val BLOG_AUTH_HOSTS = setOf(
    "medium.com",
    "substack.com",
)

private val ARTICLE_PATH_PREFIXES = setOf(
    "article",
    "articles",
    "news",
    "review",
    "reviews",
    "story",
)

fun Uri.toArticleReaderUrl(): String =
    "https://$ARTICLE_READER_HOST/api/c/full?q=${Uri.encode(toString())}"

fun Uri.toPeriscopeUrl(): String =
    "https://$PERISCOPE_HOST/?url=${Uri.encode(toString())}"

fun Uri.toUnwallUrl(): String =
    "https://$UNWALL_HOST/${toString().removePrefix("https://").removePrefix("http://")}?reader=1"

fun String.isReaderServiceUrl(): Boolean =
    runCatching { Uri.parse(this).isReaderServiceUrl() }.getOrDefault(false)

fun Uri.isReaderServiceUrl(): Boolean = host in ALLOWED_READER_HOSTS

fun String.isArchiveServiceUrl(): Boolean =
    runCatching {
        Uri.parse(this).host in setOf("archive.today", ARCHIVE_FO_HOST, "archive.is", "archive.ph")
    }.getOrDefault(false)

fun String.toOriginalArticleUrl(): String =
    runCatching {
        val uri = Uri.parse(this)
        uri.readerOriginalUrl() ?: this
    }.getOrDefault(this)

fun String.toArchiveSearchUrl(): String =
    "https://archive.ph/search/?q=${Uri.encode(this)}"

fun String.toPreferredReaderUrl(fallbackToOriginal: Boolean = true): String =
    runCatching {
        val uri = Uri.parse(this)
        when {
            uri.isReaderServiceUrl() -> this
            uri.isNewYorkTimesHost() -> uri.toPeriscopeUrl()
            fallbackToOriginal -> this
            else -> toArchiveSearchUrl()
        }
    }.getOrDefault(this)

fun String.isLikelyWebUrl(): Boolean =
    runCatching {
        val uri = Uri.parse(this)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

fun String.isBlogAuthHost(): Boolean =
    runCatching { Uri.parse(this).isBlogAuthHost() }.getOrDefault(false)

fun Uri.isBlogAuthHost(): Boolean {
    val normalizedHost = host?.removePrefix("www.") ?: return false
    return BLOG_AUTH_HOSTS.any { normalizedHost == it || normalizedHost.endsWith(".$it") }
}

fun Uri.isPaywallFallbackHost(): Boolean {
    val normalizedHost = host?.removePrefix("www.") ?: return false
    return PAYWALL_FALLBACK_HOSTS.any { normalizedHost == it || normalizedHost.endsWith(".$it") }
}

fun Uri.isNewYorkTimesHost(): Boolean {
    val normalizedHost = host?.removePrefix("www.") ?: return false
    return normalizedHost == "nytimes.com" || normalizedHost.endsWith(".nytimes.com")
}

fun Uri.readerOriginalUrl(): String? {
    return when (host) {
        PERISCOPE_HOST -> getQueryParameter("url")
        ARTICLE_READER_HOST -> getQueryParameter("q")
        UNWALL_HOST -> {
            val pathUrl = toString()
                .substringAfter("https://$UNWALL_HOST/", "")
                .substringBefore("?")
            if (pathUrl.isNotBlank()) "https://$pathUrl" else null
        }
        REMOVE_PAYWALLS_HOST -> toString().removePrefix("https://$REMOVE_PAYWALLS_HOST/")
            .removePrefix("http://$REMOVE_PAYWALLS_HOST/")
        ARCHIVE_FO_HOST -> toString().removePrefix("https://$ARCHIVE_FO_HOST/")
            .removePrefix("http://$ARCHIVE_FO_HOST/")
        else -> null
    }
}

fun Uri.isLikelyArticleUrl(): Boolean {
    val segments = pathSegments.filter { it.isNotBlank() }
    val firstSegment = segments.firstOrNull().orEmpty()
    val lastSegment = segments.lastOrNull().orEmpty()
    return (segments.size >= 2 && firstSegment in ARTICLE_PATH_PREFIXES) ||
        segments.size >= 3 ||
        (segments.size >= 2 && lastSegment.length >= 20 && ("-" in lastSegment || lastSegment.endsWith(".html")))
}
