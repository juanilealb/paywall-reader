package com.juani.paywallreader.data.capture

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

object XPostExtractor {
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 18_000
    private const val MAX_REDIRECTS = 6
    private val X_HOSTS = setOf("x.com", "twitter.com")
    private val MOBILE_X_HOSTS = setOf("mobile.x.com", "mobile.twitter.com")
    private val SHORT_LINK_HOSTS = setOf("t.co")
    private val NON_ARTICLE_HOSTS = X_HOSTS + MOBILE_X_HOSTS + SHORT_LINK_HOSTS + setOf("pic.twitter.com")
    private val json = Json { ignoreUnknownKeys = true }

    fun canHandle(url: String): Boolean = parseTweetUrl(url) != null

    fun fetch(url: String): CapturedArticle {
        val tweetUrl = parseTweetUrl(url)?.canonicalUrl ?: error("No es un enlace de post de X")
        val jsonPayload = fetchOEmbedJson(tweetUrl)
        val tweetArticle = fromOEmbedJson(
            requestedUrl = url,
            resolvedUrl = tweetUrl,
            jsonPayload = jsonPayload,
        )
        val externalArticleUrl = extractExternalArticleUrl(tweetArticle.html.orEmpty())
        if (externalArticleUrl != null) {
            val externalArticle = runCatching { BackgroundArticleExtractor.fetch(externalArticleUrl) }.getOrNull()
            if (externalArticle != null && externalArticle.text.isNotBlank()) {
                return externalArticle.copy(
                    requestedUrl = url,
                    markdown = buildString {
                        append(externalArticle.markdown)
                        append("\n\n---\n")
                        append("Guardado desde X: ").append(tweetUrl)
                    }.trim(),
                )
            }
        }
        return tweetArticle
    }

    fun fromOEmbedJson(
        requestedUrl: String,
        resolvedUrl: String,
        jsonPayload: String,
    ): CapturedArticle {
        val payload = json.parseToJsonElement(jsonPayload).jsonObject
        val authorName = payload["author_name"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
        val authorUrl = payload["author_url"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotBlank() }
        val html = payload["html"]?.jsonPrimitive?.content.orEmpty()
        val tweetText = extractTweetText(html).ifBlank { resolvedUrl }
        val handle = authorUrl?.let { parseXHandle(it) } ?: parseTweetUrl(resolvedUrl)?.handle
        val author = listOfNotNull(
            authorName,
            handle?.let { "@$it" },
        ).joinToString(" ").takeIf { it.isNotBlank() }
        val title = when {
            author != null -> "Post de $author"
            handle != null -> "Post de @$handle"
            else -> "Post de X"
        }
        val markdown = buildString {
            append("# ").append(title).append("\n\n")
            author?.let { append("_Por ").append(it).append("_\n\n") }
            append(tweetText)
            append("\n\n---\n")
            append("Original: ").append(resolvedUrl)
        }.trim()

        return CapturedArticle(
            title = title,
            requestedUrl = requestedUrl,
            resolvedUrl = resolvedUrl,
            author = author,
            excerpt = tweetText.take(280),
            html = html,
            text = tweetText,
            markdown = markdown,
            imageUrl = null,
        )
    }

    internal fun externalLinksFromOEmbedHtml(html: String): List<String> =
        Regex("(?is)<a\\b[^>]*\\bhref=[\\\"']([^\\\"']+)[\\\"'][^>]*>")
            .findAll(html)
            .mapNotNull { match -> match.groupValues.getOrNull(1)?.decodeHtml()?.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .filter { !isXChromeHost(it) }
            .distinct()
            .toList()

    private fun fetchOEmbedJson(tweetUrl: String): String {
        val oEmbedUrl = "https://publish.twitter.com/oembed?omit_script=1&dnt=1&url=" +
            URLEncoder.encode(tweetUrl, StandardCharsets.UTF_8.name())
        val connection = (URL(oEmbedUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 PaywallReader/1.0",
            )
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..399) {
                error("X oEmbed HTTP $responseCode")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractExternalArticleUrl(html: String): String? =
        externalLinksFromOEmbedHtml(html)
            .asSequence()
            .map { link -> if (isShortLinkHost(link)) resolveRedirects(link) else link }
            .firstOrNull { !isNonArticleHost(it) }

    private fun resolveRedirects(url: String): String {
        var currentUrl = url
        repeat(MAX_REDIRECTS) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "HEAD"
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 PaywallReader/1.0",
                )
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }
            try {
                val responseCode = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (responseCode in 300..399 && !location.isNullOrBlank()) {
                    currentUrl = URL(URL(currentUrl), location).toString()
                } else {
                    return currentUrl
                }
            } catch (_: Throwable) {
                return currentUrl
            } finally {
                connection.disconnect()
            }
        }
        return currentUrl
    }

    private fun extractTweetText(html: String): String {
        val paragraphHtml = Regex("(?is)<p\\b[^>]*>(.*?)</p>")
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?: html.substringBefore("&mdash;")
        return paragraphHtml.toPlainText().collapseWhitespace()
    }

    private fun parseTweetUrl(url: String): TweetUrl? = runCatching {
        val uri = URI(url.trim())
        val host = uri.normalizedXHost() ?: return@runCatching null
        if (host !in X_HOSTS) return@runCatching null
        val segments = uri.path
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val statusIndex = segments.indexOfFirst { it == "status" || it == "statuses" }
        if (statusIndex <= 0 || statusIndex + 1 >= segments.size) return@runCatching null
        val handle = segments[statusIndex - 1]
        val statusId = segments[statusIndex + 1]
        if (handle.isBlank() || statusId.none { it.isDigit() }) return@runCatching null
        TweetUrl(handle = handle, statusId = statusId, canonicalUrl = "https://x.com/$handle/status/$statusId")
    }.getOrNull()

    private fun parseXHandle(url: String): String? = runCatching {
        val uri = URI(url.trim())
        val host = uri.normalizedXHost() ?: return@runCatching null
        if (host !in X_HOSTS) return@runCatching null
        uri.path
            ?.split('/')
            ?.firstOrNull { it.isNotBlank() && it != "i" }
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun URI.normalizedXHost(): String? {
        val normalizedHost = host?.lowercase(Locale.US)?.removePrefix("www.") ?: return null
        return when (normalizedHost) {
            in MOBILE_X_HOSTS -> normalizedHost.removePrefix("mobile.")
            else -> normalizedHost
        }
    }

    private fun isShortLinkHost(url: String): Boolean =
        normalizedHost(url) in SHORT_LINK_HOSTS

    private fun isXChromeHost(url: String): Boolean =
        normalizedHost(url) in X_HOSTS || normalizedHost(url) in MOBILE_X_HOSTS || normalizedHost(url) == "pic.twitter.com"

    private fun isNonArticleHost(url: String): Boolean =
        normalizedHost(url) in NON_ARTICLE_HOSTS

    private fun normalizedHost(url: String): String? = runCatching {
        URI(url.trim()).host?.lowercase(Locale.US)?.removePrefix("www.")
    }.getOrNull()

    private fun String.toPlainText(): String =
        replace(Regex("(?is)<br\\s*/?>"), "\n")
            .replace(Regex("(?is)</(p|div|blockquote)>"), "\n")
            .replace(Regex("(?is)<[^>]+>"), " ")
            .decodeHtml()

    private fun String.decodeHtml(): String =
        replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    private fun String.collapseWhitespace(): String =
        replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    private data class TweetUrl(
        val handle: String,
        val statusId: String,
        val canonicalUrl: String,
    )
}
