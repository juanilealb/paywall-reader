package com.juani.paywallreader.data.capture

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object BackgroundArticleExtractor {
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 18_000
    private const val MAX_HTML_CHARS = 1_200_000

    fun fetch(url: String): CapturedArticle {
        if (XPostExtractor.canHandle(url)) {
            return XPostExtractor.fetch(url)
        }

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 PaywallReader/1.0",
            )
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
        }
        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..399) {
                error("HTTP $responseCode")
            }
            val html = connection.inputStream.bufferedReader().use { reader ->
                buildString {
                    val buffer = CharArray(8_192)
                    while (length < MAX_HTML_CHARS) {
                        val read = reader.read(buffer)
                        if (read < 0) break
                        append(buffer, 0, minOf(read, MAX_HTML_CHARS - length))
                    }
                }
            }
            extractFromHtml(
                requestedUrl = url,
                resolvedUrl = connection.url?.toString() ?: url,
                html = html,
            )
        } finally {
            connection.disconnect()
        }
    }

    fun extractFromHtml(
        requestedUrl: String,
        resolvedUrl: String,
        html: String,
    ): CapturedArticle {
        val cleanHtml = html
            .replace(Regex("(?is)<script\\b[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style\\b[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<noscript\\b[^>]*>.*?</noscript>"), " ")
        val articleHtml = Regex("(?is)<article\\b[^>]*>(.*?)</article>")
            .find(cleanHtml)
            ?.groupValues
            ?.getOrNull(1)
            ?: Regex("(?is)<main\\b[^>]*>(.*?)</main>")
                .find(cleanHtml)
                ?.groupValues
                ?.getOrNull(1)
            ?: cleanHtml
        val title = firstNonBlank(
            metaContent(cleanHtml, "property", "og:title"),
            metaContent(cleanHtml, "name", "twitter:title"),
            Regex("(?is)<h1\\b[^>]*>(.*?)</h1>").find(articleHtml)?.groupValues?.getOrNull(1)?.toPlainText(),
            Regex("(?is)<title\\b[^>]*>(.*?)</title>").find(cleanHtml)?.groupValues?.getOrNull(1)?.toPlainText(),
        ) ?: requestedUrl.toDisplayTitle()
        val author = firstNonBlank(
            metaContent(cleanHtml, "name", "author"),
            metaContent(cleanHtml, "property", "article:author"),
        )
        val excerpt = firstNonBlank(
            metaContent(cleanHtml, "name", "description"),
            metaContent(cleanHtml, "property", "og:description"),
            metaContent(cleanHtml, "name", "twitter:description"),
        )
        val imageUrl = firstNonBlank(
            metaContent(cleanHtml, "property", "og:image"),
            metaContent(cleanHtml, "name", "twitter:image"),
        )
        val paragraphs = Regex("(?is)<(p|h2|h3|li|blockquote)\\b[^>]*>(.*?)</\\1>")
            .findAll(articleHtml)
            .mapNotNull { match -> match.groupValues.getOrNull(2)?.toPlainText() }
            .map { it.collapseWhitespace() }
            .filter { it.length >= 24 }
            .distinct()
            .take(80)
            .toList()
        val text = paragraphs.joinToString("\n\n").ifBlank {
            articleHtml.toPlainText().collapseWhitespace()
        }
        val markdown = buildString {
            append("# ").append(title).append("\n\n")
            author?.let { append("_Por ").append(it).append("_\n\n") }
            excerpt?.let { append("> ").append(it).append("\n\n") }
            append(text)
        }.trim()
        return CapturedArticle(
            title = title,
            requestedUrl = requestedUrl,
            resolvedUrl = resolvedUrl,
            author = author,
            excerpt = excerpt,
            html = html,
            text = text,
            markdown = markdown,
            imageUrl = imageUrl,
        )
    }

    private fun metaContent(html: String, attrName: String, attrValue: String): String? {
        val pattern = Regex(
            "(?is)<meta\\b(?=[^>]*\\b$attrName=[\\\"']${Regex.escape(attrValue)}[\\\"'])(?=[^>]*\\bcontent=[\\\"']([^\\\"']+)[\\\"'])[^>]*>",
        )
        return pattern.find(html)?.groupValues?.getOrNull(1)?.decodeHtml()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()

    private fun String.toPlainText(): String =
        replace(Regex("(?is)<br\\s*/?>"), "\n")
            .replace(Regex("(?is)</(p|h[1-6]|li|blockquote)>"), "\n")
            .replace(Regex("(?is)<[^>]+>"), " ")
            .decodeHtml()
            .collapseWhitespace()

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

    private fun String.toDisplayTitle(): String =
        removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .removePrefix("www.")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

data class CapturedArticle(
    val title: String,
    val requestedUrl: String,
    val resolvedUrl: String,
    val author: String? = null,
    val excerpt: String? = null,
    val html: String? = null,
    val text: String,
    val markdown: String,
    val imageUrl: String? = null,
)
