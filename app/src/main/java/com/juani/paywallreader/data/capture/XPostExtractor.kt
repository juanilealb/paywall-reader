package com.juani.paywallreader.data.capture

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
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

    fun fallbackArticle(url: String): CapturedArticle {
        val tweetUrl = parseTweetUrl(url)
        val resolvedUrl = tweetUrl?.canonicalUrl ?: url
        val author = tweetUrl?.handle?.let { "@$it" }
        val title = author?.let { "Post de $it" } ?: "Post de X"
        val text = "No se pudo extraer el contenido del post. Abrilo en X para verlo completo."
        return CapturedArticle(
            title = title,
            requestedUrl = url,
            resolvedUrl = resolvedUrl,
            author = author,
            excerpt = text,
            text = text,
            markdown = buildString {
                append("# ").append(title).append("\n\n")
                author?.let { append("_Por ").append(it).append("_\n\n") }
                append(text)
                append("\n\n---\n")
                append("Original: ").append(resolvedUrl)
            }.trim(),
            imageUrl = null,
        )
    }

    fun fetch(url: String): CapturedArticle {
        val tweetUrl = parseTweetUrl(url)?.canonicalUrl ?: error("No es un enlace de post de X")
        fetchXArticleJson(tweetUrl)
            ?.let { payload -> return fromXArticleJson(requestedUrl = url, resolvedUrl = tweetUrl, jsonPayload = payload) }

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

    internal fun fromXArticleJson(
        requestedUrl: String,
        resolvedUrl: String,
        jsonPayload: String,
    ): CapturedArticle {
        val payload = json.parseToJsonElement(jsonPayload).jsonObject
        val article = payload["article"]?.jsonObject ?: error("No X article payload")
        val author = payload["author"]?.jsonObject
        val title = article.stringOrNull("title") ?: "Artículo de X"
        val blocks = article["blocks"]?.jsonArray.orEmpty()
        val entityMap = article["entityMap"]?.toEntityMap().orEmpty()
        val markdownBody = blocks.toDraftMarkdown(entityMap).trim()
        val fallbackText = article.stringOrNull("previewText").orEmpty()
        val text = markdownBody.markdownToPlainText().ifBlank { fallbackText }.ifBlank { title }
        val authorLabel = author?.let {
            listOfNotNull(
                it.stringOrNull("name"),
                it.stringOrNull("handle")?.let { handle -> "@$handle" },
            ).joinToString(" ").takeIf { value -> value.isNotBlank() }
        }
        val createdAt = article.stringOrNull("createdAt")
        val coverImage = article.stringOrNull("coverImage")
        val markdown = buildString {
            append("# ").append(title).append("\n\n")
            authorLabel?.let { append("_Por ").append(it).append("_\n\n") }
            createdAt?.take(10)?.let { append("Fecha: ").append(it).append("\n\n") }
            append(markdownBody.ifBlank { fallbackText })
            append("\n\n---\n")
            append("Original: ").append(resolvedUrl)
        }.trim()

        return CapturedArticle(
            title = title,
            requestedUrl = requestedUrl,
            resolvedUrl = resolvedUrl,
            author = authorLabel,
            excerpt = null,
            html = null,
            text = text,
            markdown = markdown,
            imageUrl = coverImage,
        )
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
        val readableText = if (tweetText.isGenericXFallback()) {
            "No se pudo extraer el contenido del post. Abrilo en X para verlo completo."
        } else {
            tweetText
        }
        val markdown = buildString {
            append("# ").append(title).append("\n\n")
            author?.let { append("_Por ").append(it).append("_\n\n") }
            append(readableText)
            append("\n\n---\n")
            append("Original: ").append(resolvedUrl)
        }.trim()

        return CapturedArticle(
            title = title,
            requestedUrl = requestedUrl,
            resolvedUrl = resolvedUrl,
            author = author,
            excerpt = readableText.take(280),
            html = html,
            text = readableText,
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

    private fun fetchXArticleJson(tweetUrl: String): String? {
        val connection = (URL("https://xtomd.com/api/fetch").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 PaywallReader/1.0")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
        }
        return try {
            val body = """{"url":"${tweetUrl.escapeJsonString()}"}"""
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..399) {
                return null
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val payload = runCatching { json.parseToJsonElement(response).jsonObject }.getOrNull() ?: return null
            if (payload["article"] == null) {
                null
            } else {
                response
            }
        } catch (_: Throwable) {
            null
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

    private fun String.escapeJsonString(): String =
        buildString {
            this@escapeJsonString.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }

    private fun String.collapseWhitespace(): String =
        replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    private fun String.isGenericXFallback(): Boolean {
        val normalized = lowercase(Locale.US)
        return normalized.contains("es lo que está pasando") ||
            normalized.contains("lo que está pasando") ||
            normalized.contains("what's happening") ||
            normalized.contains("obtén las historias completas") ||
            normalized.contains("get the full story")
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }

    private fun JsonElement.toEntityMap(): Map<Int, JsonObject> {
        return when (this) {
            is JsonArray -> mapNotNull { entry ->
                val obj = entry.jsonObject
                val key = obj["key"]?.jsonPrimitive?.intOrNull
                val value = obj["value"]?.jsonObject
                if (key != null && value != null) key to value else null
            }.toMap()
            is JsonObject -> mapNotNull { (key, value) ->
                key.toIntOrNull()?.let { it to value.jsonObject }
            }.toMap()
            else -> emptyMap()
        }
    }

    private fun List<JsonElement>.toDraftMarkdown(entityMap: Map<Int, JsonObject>): String = buildString {
        var lastWasList = false
        for (blockElement in this@toDraftMarkdown) {
            val block = blockElement.jsonObject
            val type = block.stringOrNull("type").orEmpty()
            if (type == "atomic") {
                resolveAtomicBlock(block, entityMap)?.let {
                    append(it).append("\n\n")
                }
                lastWasList = false
                continue
            }

            val rawText = block.stringOrNull("text").orEmpty()
            val text = rawText.applyInlineStyles(block["inlineStyleRanges"]?.jsonArray.orEmpty())
            if (text.isBlank()) continue

            val isList = type == "unordered-list-item" || type == "ordered-list-item"
            if (!isList && lastWasList) appendLine()
            lastWasList = isList

            when (type) {
                "header-one", "header-two" -> append("## ").append(text.stripMarkdownMarkers()).append("\n\n")
                "header-three" -> append("### ").append(text.stripMarkdownMarkers()).append("\n\n")
                "blockquote" -> append("> ").append(text).append("\n\n")
                "unordered-list-item" -> append("- ").append(text).appendLine()
                "ordered-list-item" -> append("1. ").append(text).appendLine()
                "code-block" -> append("```\n").append(rawText).append("\n```\n\n")
                else -> append(text).append("\n\n")
            }
        }
    }

    private fun String.applyInlineStyles(styleRanges: List<JsonElement>): String {
        if (styleRanges.isEmpty() || isEmpty()) return this
        data class Marker(val index: Int, val text: String, val closing: Boolean)
        val markers = mutableListOf<Marker>()
        styleRanges.forEach { element ->
            val range = element.jsonObject
            val offset = range["offset"]?.jsonPrimitive?.intOrNull ?: return@forEach
            val length = range["length"]?.jsonPrimitive?.intOrNull ?: return@forEach
            val style = range.stringOrNull("style") ?: return@forEach
            val marker = when (style.lowercase(Locale.US)) {
                "bold" -> "**"
                "italic" -> "_"
                "strikethrough" -> "~~"
                else -> return@forEach
            }
            val start = offset.coerceIn(0, this.length)
            val end = (offset + length).coerceIn(0, this.length)
            if (start >= end) return@forEach
            markers += Marker(start, marker, closing = false)
            markers += Marker(end, marker, closing = true)
        }
        return buildString {
            var cursor = 0
            markers.sortedWith(compareBy<Marker> { it.index }.thenByDescending { it.closing }).forEach { marker ->
                if (marker.index > cursor) {
                    append(this@applyInlineStyles.substring(cursor, marker.index))
                    cursor = marker.index
                }
                append(marker.text)
            }
            append(this@applyInlineStyles.substring(cursor))
        }
    }

    private fun resolveAtomicBlock(block: JsonObject, entityMap: Map<Int, JsonObject>): String? {
        val entityKey = block["entityRanges"]?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("key")
            ?.jsonPrimitive
            ?.intOrNull
            ?: return null
        val entity = entityMap[entityKey] ?: return null
        val type = entity.stringOrNull("type")?.uppercase(Locale.US).orEmpty()
        val data = entity["data"]?.jsonObject
        return when (type) {
            "IMAGE", "PHOTO" -> {
                val url = data?.stringOrNull("src")
                    ?: data?.stringOrNull("url")
                    ?: data?.stringOrNull("media_url_https")
                url?.let { "![Imagen]($it)" }
            }
            "DIVIDER" -> "---"
            "LINK" -> data?.stringOrNull("url")?.let { "[$it]($it)" }
            else -> null
        }
    }

    private fun String.stripMarkdownMarkers(): String =
        replace("**", "").replace("_", "").replace("~~", "")

    private fun String.markdownToPlainText(): String =
        lineSequence()
            .map { line ->
                line.removePrefix("#")
                    .removePrefix("##")
                    .removePrefix("###")
                    .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")
                    .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
                    .replace("**", "")
                    .replace("_", "")
                    .replace("~~", "")
                    .trim()
            }
            .filter { it.isNotBlank() && it != "---" }
            .joinToString("\n")

    private data class TweetUrl(
        val handle: String,
        val statusId: String,
        val canonicalUrl: String,
    )
}
