package com.juani.paywallreader.data.capture

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.juani.paywallreader.data.local.AppDatabase
import com.juani.paywallreader.data.repository.SourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CaptureArticleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)?.trim().orEmpty()
        if (url.isBlank()) return Result.failure()

        val repository = SourceRepository(AppDatabase.getInstance(applicationContext).sourceDao())
        repository.queueCaptureAttempt(url)

        return try {
            val article = withContext(Dispatchers.IO) { BackgroundArticleExtractor.fetch(url) }
            repository.saveForLater(
                title = article.title,
                url = article.requestedUrl,
                sourceName = article.resolvedUrl.toDisplaySourceName(),
                resolvedUrl = article.resolvedUrl,
                author = article.author,
                excerpt = article.excerpt,
                html = article.html,
                text = article.text,
                markdown = article.markdown,
                imageUrl = article.imageUrl,
            )
            Result.success()
        } catch (throwable: Throwable) {
            val message = throwable.message?.takeIf { it.isNotBlank() }
                ?: "No se pudo extraer el artículo"
            if (runAttemptCount < MAX_AUTOMATIC_RETRIES) {
                Result.retry()
            } else {
                repository.markCaptureFailed(url, message)
                Result.failure()
            }
        }
    }

    private fun String.toDisplaySourceName(): String =
        removePrefix("https://")
            .removePrefix("http://")
            .substringBefore("/")
            .removePrefix("www.")

    companion object {
        const val KEY_URL = "url"
        private const val MAX_AUTOMATIC_RETRIES = 2
    }
}

object CaptureWorkManager {
    fun enqueue(context: Context, url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return
        val request = OneTimeWorkRequestBuilder<CaptureArticleWorker>()
            .setInputData(workDataOf(CaptureArticleWorker.KEY_URL to trimmedUrl))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .addTag(CAPTURE_WORK_TAG)
            .addTag(workTagForUrl(trimmedUrl))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueWorkNameForUrl(trimmedUrl),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun uniqueWorkNameForUrl(url: String): String = "capture-article-${url.trim().hashCode()}"

    fun workTagForUrl(url: String): String = "capture-url-${url.trim().hashCode()}"
}

const val CAPTURE_WORK_TAG = "article-capture"
