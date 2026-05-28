package com.juani.paywallreader.ui.reader

import android.content.Intent
import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.juani.paywallreader.R
import com.juani.paywallreader.data.capture.ArticleCaptureScripts
import com.juani.paywallreader.data.capture.CapturedArticle
import com.juani.paywallreader.data.capture.DEFUDDLE_ASSET_PATH
import com.juani.paywallreader.data.capture.XPostExtractor
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_CAPTURING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_FAILED
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_READY
import com.juani.paywallreader.domain.model.ReadingItem
import com.juani.paywallreader.domain.model.UNFILED_FOLDER_NAME
import com.juani.paywallreader.ui.home.OfflineArticleReaderScreen
import com.juani.paywallreader.data.reader.ARTICLE_READER_HOST
import com.juani.paywallreader.data.reader.ARCHIVE_FO_HOST
import com.juani.paywallreader.data.reader.UNWALL_HOST
import com.juani.paywallreader.data.reader.captureProviderKey
import com.juani.paywallreader.data.reader.isArchiveServiceUrl
import com.juani.paywallreader.data.reader.isBlogAuthHost
import com.juani.paywallreader.data.reader.isLikelyArticleUrl
import com.juani.paywallreader.data.reader.isLikelyWebUrl
import com.juani.paywallreader.data.reader.isNewYorkTimesHost
import com.juani.paywallreader.data.reader.isPaywallFallbackHost
import com.juani.paywallreader.data.reader.isReaderServiceUrl
import com.juani.paywallreader.data.reader.readerOriginalUrl
import com.juani.paywallreader.data.reader.toArchiveSearchUrl
import com.juani.paywallreader.data.reader.toArticleReaderUrl
import com.juani.paywallreader.data.reader.toOriginalArticleUrl
import com.juani.paywallreader.data.reader.toPeriscopeUrl
import com.juani.paywallreader.data.reader.toPreferredReaderUrl
import com.juani.paywallreader.data.reader.toUnwallUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FoldFloatingActionButtonSize = 60.dp
private val ReaderToolbarHeight = 72.dp
private val ReaderToolbarActionSize = 42.dp
private val ReaderToolbarIconSize = 20.dp

@Composable
fun HeadlessArticleCaptureHost(
    captureUrl: String?,
    onSaveForLater: (
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String?,
        author: String?,
        excerpt: String?,
        html: String?,
        text: String?,
        markdown: String?,
        imageUrl: String?,
        captureProvider: String?,
    ) -> Unit,
    onCaptureStatusChange: (url: String, status: String) -> Unit,
    onCaptureComplete: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val urlToCapture = captureUrl?.trim()?.takeIf { it.isLikelyWebUrl() } ?: return
    if (XPostExtractor.canHandle(urlToCapture)) {
        LaunchedEffect(urlToCapture) {
            onCaptureStatusChange(urlToCapture, CAPTURE_STATUS_CAPTURING)
            val article = withContext(Dispatchers.IO) {
                runCatching { XPostExtractor.fetch(urlToCapture) }
                    .getOrElse { XPostExtractor.fallbackArticle(urlToCapture) }
            }
            saveCapturedArticle(onSaveForLater, article)
            onCaptureComplete(urlToCapture)
        }
        return
    }

    key(urlToCapture) {
        var webView by remember { mutableStateOf<WebView?>(null) }
        AndroidView(
            modifier = modifier.size(1.dp),
            factory = { context ->
                var captureStarted = false
                var captureFinished = false
                val originalUrl = urlToCapture.toOriginalArticleUrl()

                WebView(context).apply {
                    alpha = 0f
                    configureReaderSettings(useBrowserUserAgent = urlToCapture.isBlogAuthHost())
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (!captureStarted) {
                                onCaptureStatusChange(originalUrl, CAPTURE_STATUS_CAPTURING)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            val resolvedUrl = url ?: view?.url ?: urlToCapture
                            CookieManager.getInstance().flush()
                            view?.applyAdCleanup()
                            view?.applySiteChromeCleanup()
                            view?.postDelayed({ view.applySiteChromeCleanup() }, 700L)
                            view?.postDelayed({ view.applySiteChromeCleanup() }, 1_800L)
                            if (resolvedUrl.isReaderServiceUrl()) {
                                view?.applyReaderChrome()
                                view?.postDelayed({ view.applyReaderChrome() }, 700L)
                                view?.loadFallbackReaderIfBlank(resolvedUrl, 2_500L)
                            }
                            view?.loadFallbackReaderIfPaywalled(resolvedUrl, 2_500L)

                            if (!captureStarted) {
                                captureStarted = true
                                val captureOriginalUrl = resolvedUrl.toOriginalArticleUrl()
                                onCaptureStatusChange(captureOriginalUrl, CAPTURE_STATUS_CAPTURING)
                                view?.postDelayed(
                                    {
                                        if (captureFinished) return@postDelayed
                                        captureFinished = true
                                        captureHeadlessArticle(
                                            sourceUrl = urlToCapture,
                                            sourceName = captureOriginalUrl.toDisplaySourceName(),
                                            onSaveForLater = onSaveForLater,
                                            onCaptured = { onCaptureComplete(captureOriginalUrl) },
                                        )
                                    },
                                    1_200L,
                                )
                                view?.postDelayed(
                                    {
                                        if (!captureFinished) {
                                            captureFinished = true
                                            onCaptureStatusChange(captureOriginalUrl, CAPTURE_STATUS_FAILED)
                                            onCaptureComplete(captureOriginalUrl)
                                        }
                                    },
                                    12_000L,
                                )
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            if (request?.isForMainFrame != true) return false
                            val requestUrl = request.url
                            val scheme = requestUrl.scheme
                            if (scheme != "http" && scheme != "https") return true
                            if (requestUrl.isNewYorkTimesHost() && !requestUrl.isReaderServiceUrl()) {
                                view?.loadUrl(requestUrl.toPeriscopeUrl())
                                return true
                            }
                            return false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true && !captureFinished) {
                                captureFinished = true
                                onCaptureStatusChange(originalUrl, CAPTURE_STATUS_FAILED)
                                onCaptureComplete(originalUrl)
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): WebResourceResponse? {
                            val requestUri = request?.url ?: return null
                            return if (!request.isForMainFrame && requestUri.isBlockedAdResource()) {
                                emptyWebResponse()
                            } else {
                                null
                            }
                        }
                    }
                    webView = this
                    loadUrl(urlToCapture.toPreferredReaderUrl())
                }
            },
            update = {},
        )
        DisposableEffect(urlToCapture) {
            onDispose {
                webView?.stopLoading()
                webView?.destroy()
                webView = null
            }
        }
    }
}

private fun saveCapturedArticle(
    onSaveForLater: (
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String?,
        author: String?,
        excerpt: String?,
        html: String?,
        text: String?,
        markdown: String?,
        imageUrl: String?,
        captureProvider: String?,
    ) -> Unit,
    article: CapturedArticle,
) {
    onSaveForLater(
        article.title,
        article.requestedUrl,
        article.resolvedUrl.toDisplaySourceName(),
        article.resolvedUrl,
        article.author,
        article.excerpt,
        article.html,
        article.text,
        article.markdown,
        article.imageUrl,
        article.resolvedUrl.captureProviderKey(),
    )
}

@Composable
fun ReaderRoute(
    sourceName: String,
    sourceUrl: String,
    autoCaptureUrl: String? = null,
    onAutoCaptureComplete: () -> Unit = {},
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = readerViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    ReaderScreen(
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        autoCaptureUrl = autoCaptureUrl,
        onAutoCaptureComplete = onAutoCaptureComplete,
        onBack = onBack,
        showBackButton = showBackButton,
        onSaveForLater = viewModel::saveForLater,
        onCaptureStatusChange = viewModel::updateCaptureStatus,
        onMarkRead = viewModel::markRead,
        onArchiveBookmark = viewModel::archiveBookmark,
        onMoveBookmarkToFolder = viewModel::moveBookmarkToFolder,
        onCreateReadingFolder = viewModel::createReadingFolder,
        readingFolders = uiState.readingFolders,
        savedReadingItems = uiState.readingItems,
        onRecordVisit = viewModel::recordVisit,
        modifier = modifier,
    )
}

@Composable
private fun readerViewModel(): ReaderViewModel {
    val application = LocalContext.current.applicationContext as Application
    return viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderScreen(
    sourceName: String,
    sourceUrl: String,
    autoCaptureUrl: String? = null,
    onAutoCaptureComplete: () -> Unit = {},
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    onSaveForLater: (
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String?,
        author: String?,
        excerpt: String?,
        html: String?,
        text: String?,
        markdown: String?,
        imageUrl: String?,
        captureProvider: String?,
    ) -> Unit,
    onCaptureStatusChange: (url: String, status: String) -> Unit = { _, _ -> },
    onMarkRead: (url: String) -> Unit,
    onArchiveBookmark: (url: String) -> Unit = {},
    onMoveBookmarkToFolder: (url: String, folderName: String) -> Unit = { _, _ -> },
    onCreateReadingFolder: (folderName: String) -> Unit = {},
    readingFolders: List<String> = emptyList(),
    savedReadingItems: List<ReadingItem> = emptyList(),
    onRecordVisit: (title: String, url: String, sourceName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(view, darkTheme) {
        if (!view.isInEditMode) {
            val window = (view.context as android.app.Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = true
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
            onDispose {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.isAppearanceLightStatusBars = !darkTheme
            }
        } else {
            onDispose {}
        }
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var canNavigateBack by remember { mutableStateOf(false) }
    var canNavigateForward by remember { mutableStateOf(false) }
    var isAuthSurface by remember { mutableStateOf(false) }
    var currentUrl by remember(sourceUrl) { mutableStateOf(sourceUrl.trim().toPreferredReaderUrl()) }
    var currentTitle by remember(sourceUrl) { mutableStateOf(sourceName) }
    var savedArticleOverlayItem by remember { mutableStateOf<ReadingItem?>(null) }
    var offlineArticleToRead by remember { mutableStateOf<ReadingItem?>(null) }
    var showSavedArticleFolderDialog by rememberSaveable { mutableStateOf(false) }
    var savedArticleNewFolderName by rememberSaveable { mutableStateOf("") }
    var autoCaptureCompleted by rememberSaveable(sourceUrl, autoCaptureUrl) { mutableStateOf(false) }
    var autoCaptureCallbackFinished by rememberSaveable(sourceUrl, autoCaptureUrl) { mutableStateOf(false) }
    val initialUrl = remember(sourceUrl) {
        sourceUrl.trim().toPreferredReaderUrl()
    }
    val detectsAuthSurfaces = remember(initialUrl) {
        initialUrl.isBlogAuthHost()
    }
    val needsBrowserUserAgent = remember(initialUrl) {
        initialUrl.needsBrowserUserAgent()
    }
    val needsNoJavaScriptRetry = remember(initialUrl) {
        initialUrl.needsNoJavaScriptRetry()
    }
    var toolbarExpanded by rememberSaveable(sourceUrl) { mutableStateOf(true) }
    var readerFocusMode by rememberSaveable(sourceUrl) { mutableStateOf(false) }
    val openOriginal = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl.ifBlank { sourceUrl }.toOriginalArticleUrl())))
    }
    val shareOriginal = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentUrl.ifBlank { sourceUrl }.toOriginalArticleUrl())
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.reader_share)))
    }
    val openArchiveSearch = {
        val url = webView?.url ?: currentUrl.ifBlank { sourceUrl }
        val originalUrl = url.toOriginalArticleUrl()
        webView?.settings?.javaScriptEnabled = true
        webView?.loadUrl(originalUrl.toArchiveSearchUrl())
        Unit
    }
    fun saveArticleAndShowOverlay(
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String?,
        author: String?,
        excerpt: String?,
        html: String?,
        text: String?,
        markdown: String?,
        imageUrl: String?,
        captureProvider: String?,
    ) {
        onSaveForLater(
            title,
            url,
            sourceName,
            resolvedUrl,
            author,
            excerpt,
            html,
            text,
            markdown,
            imageUrl,
            captureProvider,
        )
        val existing = savedReadingItems.firstOrNull { it.url == url }
        savedArticleOverlayItem = existing ?: ReadingItem(
            id = 0,
            title = title.ifBlank { url.toDisplaySourceName() },
            url = url,
            sourceName = sourceName.ifBlank { url.toDisplaySourceName() },
            addedAt = System.currentTimeMillis(),
            resolvedUrl = resolvedUrl,
            author = author,
            excerpt = excerpt,
            html = html,
            text = text,
            markdown = markdown,
            imageUrl = imageUrl,
            folderName = UNFILED_FOLDER_NAME,
            captureStatus = CAPTURE_STATUS_READY,
            captureProvider = captureProvider.orEmpty().ifBlank { "original" },
        )
    }

    fun captureCurrentForLater(onCaptured: () -> Unit = {}) {
        val view = webView
        val resolvedUrl = view?.url ?: currentUrl.ifBlank { sourceUrl }
        val originalUrl = resolvedUrl.toOriginalArticleUrl()
        val captureProvider = resolvedUrl.captureProviderKey()
        val title = currentTitle.ifBlank { sourceName }
        if (view == null) {
            saveArticleAndShowOverlay(
                title,
                originalUrl,
                sourceName,
                resolvedUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                captureProvider,
            )
            onCaptured()
        } else {
            view.evaluateArticleCaptureScript { rawPayload ->
                val payload = rawPayload.decodeArticleCapturePayload()
                val capturedTitle = payload?.optString("title")?.takeIf { it.isNotBlank() } ?: title
                val capturedText = payload?.optString("text")?.takeIf { it.isNotBlank() }
                saveArticleAndShowOverlay(
                    capturedTitle,
                    originalUrl,
                    sourceName,
                    resolvedUrl,
                    payload?.optString("author")?.takeIf { it.isNotBlank() },
                    payload?.optString("excerpt")?.takeIf { it.isNotBlank() },
                    payload?.optString("html")?.takeIf { it.isNotBlank() },
                    capturedText,
                    payload?.optString("markdown")?.takeIf { it.isNotBlank() }
                        ?: capturedText?.toBasicMarkdown(capturedTitle, originalUrl),
                    payload?.optString("imageUrl")?.takeIf { it.isNotBlank() },
                    captureProvider,
                )
                onCaptured()
            }
        }
    }
    val saveCurrentForLater = { captureCurrentForLater() }
    val markCurrentRead = {
        onMarkRead((webView?.url ?: currentUrl.ifBlank { sourceUrl }).toOriginalArticleUrl())
    }
    fun updateNavigationState(view: WebView?) {
        canNavigateBack = view?.canGoBack() == true
        canNavigateForward = view?.canGoForward() == true
    }

    LaunchedEffect(readerFocusMode, webView) {
        webView?.setReaderFocusMode(readerFocusMode)
    }

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) {
            view.goBack()
        } else {
            onBack()
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val useVerticalToolbar = !showBackButton || maxWidth >= 420.dp
        val toolbarAlignment = Alignment.BottomEnd
        val showShareAction = maxWidth >= 380.dp
        val isArchivePage = currentUrl.isArchiveServiceUrl()

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            key(initialUrl) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            configureReaderSettings(useBrowserUserAgent = needsBrowserUserAgent)
                            addJavascriptInterface(
                                ReaderPageSignalBridge(this, detectsAuthSurfaces) { isAuthSurface = it },
                                "PaywallReaderBridge",
                            )
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                    currentTitle = view?.title?.takeIf { it.isNotBlank() } ?: currentTitle
                                    updateNavigationState(view)
                                }

                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?,
                                ): Boolean {
                                    if (!needsBrowserUserAgent || view == null || resultMsg == null) {
                                        return false
                                    }

                                    val popup = WebView(view.context).apply {
                                        configureReaderSettings(useBrowserUserAgent = true)
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                popupView: WebView?,
                                                request: WebResourceRequest?,
                                            ): Boolean {
                                                val popupUrl = request?.url?.toString() ?: return false
                                                view.loadUrl(popupUrl)
                                                popupView?.destroy()
                                                return true
                                            }

                                            override fun onPageStarted(
                                                popupView: WebView?,
                                                url: String?,
                                                favicon: Bitmap?,
                                            ) {
                                                if (!url.isNullOrBlank()) {
                                                    view.loadUrl(url)
                                                    popupView?.destroy()
                                                }
                                            }
                                        }
                                    }
                                    val transport = resultMsg.obj as WebView.WebViewTransport
                                    transport.webView = popup
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                                currentUrl = url ?: currentUrl
                                if (url?.isReaderServiceUrl() == true) {
                                    view?.loadFallbackReaderIfBlank(url, 8_000L)
                                }
                                updateNavigationState(view)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                currentUrl = url ?: view?.url ?: currentUrl
                                currentTitle = view?.title?.takeIf { it.isNotBlank() } ?: currentTitle
                                CookieManager.getInstance().flush()
                                updateNavigationState(view)
                                    view?.applyAdCleanup()
                                    if (!detectsAuthSurfaces) {
                                        view?.applySiteChromeCleanup()
                                        view?.postDelayed({ view.applySiteChromeCleanup() }, 700L)
                                        view?.postDelayed({ view.applySiteChromeCleanup() }, 1_800L)
                                    }
                                    if (readerFocusMode) {
                                        view?.setReaderFocusMode(true)
                                        view?.postDelayed({ view.setReaderFocusMode(true) }, 700L)
                                        view?.postDelayed({ view.setReaderFocusMode(true) }, 1_800L)
                                    }
                                    view?.installPageStateSignals(detectsAuthSurfaces)
                                    if (url?.isReaderServiceUrl() == true) {
                                    view?.applyReaderChrome()
                                    view?.postDelayed({ view.applyReaderChrome() }, 700L)
                                        view?.loadFallbackReaderIfBlank(url, 2_500L)
                                }
                                if (!isAuthSurface) {
                                    view?.loadFallbackReaderIfPaywalled(url, 2_500L)
                                }
                                if (autoCaptureUrl != null && !autoCaptureCompleted) {
                                    autoCaptureCompleted = true
                                    autoCaptureCallbackFinished = false
                                    val originalCaptureUrl = (url ?: view?.url ?: currentUrl).toOriginalArticleUrl()
                                    onCaptureStatusChange(originalCaptureUrl, CAPTURE_STATUS_CAPTURING)
                                    view?.postDelayed(
                                        {
                                            captureCurrentForLater(
                                                onCaptured = {
                                                    autoCaptureCallbackFinished = true
                                                    onAutoCaptureComplete()
                                                },
                                            )
                                        },
                                        1_200L,
                                    )
                                    view?.postDelayed(
                                        {
                                            if (!autoCaptureCallbackFinished) {
                                                onCaptureStatusChange(originalCaptureUrl, CAPTURE_STATUS_FAILED)
                                                onAutoCaptureComplete()
                                            }
                                        },
                                        12_000L,
                                    )
                                }
                                val originalUrl = currentUrl.toOriginalArticleUrl()
                                if (!currentUrl.isReaderServiceUrl() && originalUrl.isLikelyWebUrl()) {
                                    onRecordVisit(currentTitle, originalUrl, sourceName)
                                    onMarkRead(originalUrl)
                                }
                            }

                                override fun doUpdateVisitedHistory(
                                    view: WebView?,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    currentUrl = url ?: view?.url ?: currentUrl
                                    updateNavigationState(view)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    if (request?.isForMainFrame != true) {
                                        return false
                                    }

                                    val url = request.url
                                    val scheme = url.scheme
                                    if (scheme != "http" && scheme != "https") {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, url))
                                        return true
                                    }

                                    if (url.isNewYorkTimesHost() && !url.isReaderServiceUrl()) {
                                        view?.loadUrl(url.toPeriscopeUrl())
                                        return true
                                    }

                                if (
                                    request.hasGesture() &&
                                    !detectsAuthSurfaces &&
                                    !isAuthSurface &&
                                    !url.isReaderServiceUrl() &&
                                    url.isLikelyArticleUrl()
                                ) {
                                    settings.javaScriptEnabled = true
                                    view?.loadUrl(url.toArticleReaderUrl())
                                    return true
                                }

                                    return false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError = true
                                        updateNavigationState(view)
                                    }
                                }

                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): WebResourceResponse? {
                                    val url = request?.url ?: return null
                                    return if (!request.isForMainFrame && url.isBlockedAdResource()) {
                                        emptyWebResponse()
                                    } else {
                                        null
                                    }
                                }
                            }
                            loadUrl(initialUrl)
                            if (needsNoJavaScriptRetry) {
                                postDelayed(
                                    {
                                        if ((url ?: initialUrl).toOriginalArticleUrl().sameWebHostAs(initialUrl) && settings.javaScriptEnabled) {
                                            loadOriginalWithoutJavaScriptFallback(initialUrl)
                                        }
                                    },
                                    6_000L,
                                )
                            }
                            webView = this
                        }
                    },
                    update = { view ->
                        webView = view
                        updateNavigationState(view)
                        if (view.url == null) {
                            view.loadUrl(initialUrl)
                        }
                    },
                )
            }

            if (isLoading && progress in 1..99) {
                LinearWavyProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }

            when {
                hasError -> ReaderError(
                    onRetry = {
                        hasError = false
                        isLoading = true
                        webView?.loadUrl(webView?.url ?: initialUrl)
                    },
                    onOpenOriginal = openOriginal,
                    modifier = Modifier.align(Alignment.Center),
                )

                isLoading -> {
                    LoadingState(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            if (!isAuthSurface) {
                ReaderFloatingToolbar(
                    showBackButton = showBackButton,
                    canNavigateBack = canNavigateBack,
                    canNavigateForward = canNavigateForward,
                    isLoading = isLoading,
                    vertical = useVerticalToolbar,
                    expanded = toolbarExpanded,
                    showShareAction = showShareAction && !useVerticalToolbar,
                    onExpandedChange = { toolbarExpanded = it },
                    onBack = onBack,
                    onNavigateBack = {
                        webView?.goBack()
                        updateNavigationState(webView)
                    },
                    onNavigateForward = {
                        webView?.goForward()
                        updateNavigationState(webView)
                    },
                    onRefreshOrStop = {
                        if (isLoading) {
                            webView?.stopLoading()
                            isLoading = false
                        } else {
                            webView?.reload()
                        }
                    },
                    onOpenOriginal = openOriginal,
                    onOpenArchive = openArchiveSearch,
                    readerFocusMode = readerFocusMode,
                    onToggleReaderFocusMode = {
                        readerFocusMode = !readerFocusMode
                    },
                    onSaveForLater = saveCurrentForLater,
                    onMarkRead = markCurrentRead,
                    onShare = shareOriginal,
                    archiveMode = isArchivePage,
                    modifier = Modifier
                        .align(toolbarAlignment)
                        .then(
                            if (useVerticalToolbar) {
                                Modifier
                                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                                    .padding(end = 16.dp, bottom = 8.dp)
                                    .zIndex(1f)
                            } else {
                                Modifier
                                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                                    .padding(16.dp)
                                    .widthIn(max = 520.dp)
                            },
                        ),
                )
            }

            SavedArticleConfirmationOverlay(
                item = savedArticleOverlayItem,
                onDismiss = { savedArticleOverlayItem = null },
                onReadNow = { item ->
                    val latest = savedReadingItems.firstOrNull { it.url == item.url } ?: item
                    savedArticleOverlayItem = null
                    offlineArticleToRead = latest
                },
                onMoveToFolder = {
                    savedArticleNewFolderName = ""
                    showSavedArticleFolderDialog = true
                },
                onShare = shareOriginal,
                onArchive = { item ->
                    onArchiveBookmark(item.url)
                    savedArticleOverlayItem = null
                },
                modifier = Modifier.zIndex(3f),
            )
        }

        offlineArticleToRead?.let { item ->
            OfflineArticleReaderScreen(
                item = savedReadingItems.firstOrNull { it.url == item.url } ?: item,
                onBack = { offlineArticleToRead = null },
                onOpenWeb = { offlineArticleToRead = null },
                onMarkRead = { onMarkRead(item.url) },
                onArchiveAndNext = {
                    onArchiveBookmark(item.url)
                    offlineArticleToRead = null
                },
                onMoveToFolderAndNext = {
                    savedArticleOverlayItem = item
                    savedArticleNewFolderName = ""
                    showSavedArticleFolderDialog = true
                },
                onExitToMenu = { offlineArticleToRead = null },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f),
            )
        }

        if (showSavedArticleFolderDialog) {
            savedArticleOverlayItem?.let { item ->
                ReaderSaveFolderDialog(
                    item = item,
                    folders = readingFolders,
                    newFolderName = savedArticleNewFolderName,
                    onNewFolderNameChange = { savedArticleNewFolderName = it },
                    onMove = { folderName ->
                        onCreateReadingFolder(folderName)
                        onMoveBookmarkToFolder(item.url, folderName)
                        savedArticleOverlayItem = item.copy(folderName = folderName)
                        offlineArticleToRead = offlineArticleToRead?.let { current ->
                            if (current.url == item.url) current.copy(folderName = folderName) else current
                        }
                        showSavedArticleFolderDialog = false
                        savedArticleNewFolderName = ""
                    },
                    onDismiss = { showSavedArticleFolderDialog = false },
                )
            } ?: run {
                showSavedArticleFolderDialog = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedArticleConfirmationOverlay(
    item: ReadingItem?,
    onDismiss: () -> Unit,
    onReadNow: (ReadingItem) -> Unit,
    onMoveToFolder: () -> Unit,
    onShare: () -> Unit,
    onArchive: (ReadingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val savedItem = item ?: return
    val hapticFeedback = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val secondaryActions = listOf(
        SaveOverlayAction(
            icon = Icons.Rounded.Folder,
            label = R.string.choose_folder,
            onClick = onMoveToFolder,
        ),
        SaveOverlayAction(
            icon = Icons.Rounded.Share,
            label = R.string.reader_share,
            onClick = onShare,
        ),
        SaveOverlayAction(
            icon = Icons.Rounded.Archive,
            label = R.string.archive_saved_article,
            onClick = { onArchive(savedItem) },
        ),
    ).take(5)

    LaunchedEffect(savedItem.url) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 26.dp)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.padding(22.dp).size(44.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.saved_to_reader),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = savedItem.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }

            Button(
                onClick = { onReadNow(savedItem) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.read_now))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                secondaryActions.forEach { action ->
                    SaveOverlayCircleAction(action)
                }
            }
        }
    }
}

private data class SaveOverlayAction(
    val icon: ImageVector,
    val label: Int,
    val onClick: () -> Unit,
)

@Composable
private fun SaveOverlayCircleAction(action: SaveOverlayAction) {
    FilledIconButton(
        onClick = action.onClick,
        shape = CircleShape,
        modifier = Modifier.size(56.dp),
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = stringResource(action.label),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ReaderSaveFolderDialog(
    item: ReadingItem,
    folders: List<String>,
    newFolderName: String,
    onNewFolderNameChange: (String) -> Unit,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val candidateFolders = (listOf(UNFILED_FOLDER_NAME) + folders + item.folderName)
        .distinct()
        .filter { it.isNotBlank() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_folder)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                candidateFolders.take(6).forEach { folder ->
                    OutlinedButton(
                        onClick = { onMove(folder) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (folder == item.folderName) "✓ $folder" else folder)
                    }
                }
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = onNewFolderNameChange,
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newFolderName.isNotBlank(),
                onClick = { onMove(newFolderName.trim()) },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReaderFloatingToolbar(
    showBackButton: Boolean,
    canNavigateBack: Boolean,
    canNavigateForward: Boolean,
    isLoading: Boolean,
    vertical: Boolean,
    expanded: Boolean,
    showShareAction: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onRefreshOrStop: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenArchive: () -> Unit,
    readerFocusMode: Boolean,
    onToggleReaderFocusMode: () -> Unit,
    onSaveForLater: () -> Unit,
    onMarkRead: () -> Unit,
    onShare: () -> Unit,
    archiveMode: Boolean,
    modifier: Modifier = Modifier,
) {
    if (vertical) {
        VerticalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = {
                ReaderRefreshFab(
                    vertical = true,
                    expanded = expanded,
                    onClick = { onExpandedChange(!expanded) },
                )
            },
            modifier = modifier,
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp),
            floatingActionButtonPosition = FloatingToolbarVerticalFabPosition.Bottom,
        ) {
            ReaderToolbarActions(
                showBackButton = showBackButton,
                canNavigateBack = canNavigateBack,
                canNavigateForward = canNavigateForward,
                showShareAction = false,
                showOpenOriginal = false,
                showCollapseAction = false,
                showClosePageAction = true,
                showRefreshAction = true,
                onBack = onBack,
                onNavigateBack = onNavigateBack,
                onNavigateForward = onNavigateForward,
                onRefreshOrStop = onRefreshOrStop,
                onOpenOriginal = onOpenOriginal,
                onOpenArchive = onOpenArchive,
                readerFocusMode = readerFocusMode,
                onToggleReaderFocusMode = onToggleReaderFocusMode,
                onSaveForLater = onSaveForLater,
                onMarkRead = onMarkRead,
                onShare = onShare,
                onCollapse = { onExpandedChange(false) },
                vertical = true,
                archiveMode = archiveMode,
            )
        }
    } else {
        HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = {
                ReaderRefreshFab(
                    vertical = false,
                    expanded = expanded,
                    onClick = { onExpandedChange(!expanded) },
                )
            },
            modifier = modifier.height(ReaderToolbarHeight),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
            floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End,
        ) {
            ReaderToolbarActions(
                showBackButton = false,
                canNavigateBack = canNavigateBack,
                canNavigateForward = canNavigateForward,
                showShareAction = false,
                showOpenOriginal = false,
                showCollapseAction = false,
                showClosePageAction = true,
                showRefreshAction = true,
                onBack = onBack,
                onNavigateBack = onNavigateBack,
                onNavigateForward = onNavigateForward,
                onRefreshOrStop = onRefreshOrStop,
                onOpenOriginal = onOpenOriginal,
                onOpenArchive = onOpenArchive,
                readerFocusMode = readerFocusMode,
                onToggleReaderFocusMode = onToggleReaderFocusMode,
                onSaveForLater = onSaveForLater,
                onMarkRead = onMarkRead,
                onShare = onShare,
                onCollapse = { onExpandedChange(false) },
                vertical = false,
                archiveMode = archiveMode,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReaderRefreshFab(
    vertical: Boolean = false,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(FoldFloatingActionButtonSize),
        contentAlignment = Alignment.Center,
    ) {
        FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onClick) {
            Icon(
                imageVector = if (vertical && expanded) {
                    Icons.Rounded.KeyboardArrowDown
                } else if (vertical) {
                    Icons.Rounded.KeyboardArrowUp
                } else if (expanded) {
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight
                } else {
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft
                },
                contentDescription = stringResource(
                    if (expanded) R.string.reader_collapse_toolbar else R.string.reader_expand_toolbar,
                ),
            )
        }
    }
}

@Composable
private fun ReaderToolbarActions(
    showBackButton: Boolean,
    canNavigateBack: Boolean,
    canNavigateForward: Boolean,
    showShareAction: Boolean,
    showOpenOriginal: Boolean,
    showCollapseAction: Boolean,
    showClosePageAction: Boolean,
    showRefreshAction: Boolean,
    onBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onRefreshOrStop: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenArchive: () -> Unit,
    readerFocusMode: Boolean,
    onToggleReaderFocusMode: () -> Unit,
    onSaveForLater: () -> Unit,
    onMarkRead: () -> Unit,
    onShare: () -> Unit,
    onCollapse: () -> Unit,
    vertical: Boolean,
    archiveMode: Boolean,
) {
    if (showBackButton) {
        IconButton(onClick = onBack, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.reader_back),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
    }
    IconButton(
        onClick = onNavigateBack,
        enabled = canNavigateBack,
        modifier = Modifier.size(ReaderToolbarActionSize),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.reader_web_back),
            modifier = Modifier.size(ReaderToolbarIconSize),
        )
    }
    IconButton(
        onClick = onNavigateForward,
        enabled = canNavigateForward,
        modifier = Modifier.size(ReaderToolbarActionSize),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = stringResource(R.string.reader_web_forward),
            modifier = Modifier.size(ReaderToolbarIconSize),
        )
    }
    if (showOpenOriginal) {
        IconButton(onClick = onOpenOriginal, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.OpenInBrowser,
                contentDescription = stringResource(R.string.reader_open_original),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
    }
    if (!archiveMode) {
        IconButton(onClick = onSaveForLater, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = stringResource(R.string.reader_save_later),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
        IconButton(
            onClick = onToggleReaderFocusMode,
            modifier = Modifier.size(ReaderToolbarActionSize),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (readerFocusMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Article,
                contentDescription = stringResource(
                    if (readerFocusMode) {
                        R.string.reader_focus_mode_off
                    } else {
                        R.string.reader_focus_mode_on
                    },
                ),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
        IconButton(onClick = onOpenArchive, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.Archive,
                contentDescription = stringResource(R.string.reader_open_archive),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
        if (showClosePageAction) {
            IconButton(onClick = onBack, modifier = Modifier.size(ReaderToolbarActionSize)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.reader_close_page),
                    modifier = Modifier.size(ReaderToolbarIconSize),
                )
            }
        } else {
            IconButton(onClick = onMarkRead, modifier = Modifier.size(ReaderToolbarActionSize)) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.reader_mark_read),
                    modifier = Modifier.size(ReaderToolbarIconSize),
                )
            }
        }
    }
    if (showShareAction) {
        IconButton(onClick = onShare, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.reader_share),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
    }
    if (showRefreshAction) {
        IconButton(onClick = onRefreshOrStop, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.reader_refresh),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
    }
    if (showCollapseAction) {
        IconButton(onClick = onCollapse, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.reader_collapse_toolbar),
                modifier = Modifier.size(ReaderToolbarIconSize),
            )
        }
    }
}

private val BROWSER_USER_AGENT_HOSTS = setOf(
    "nytimes.com",
)

private val NO_JAVASCRIPT_RETRY_HOSTS = setOf(
    "nytimes.com",
)

private val BLOCKED_AD_HOST_PARTS = listOf(
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "google-analytics.com",
    "googletagmanager.com",
    "adtrafficquality.google",
    "taboola.com",
    "outbrain.com",
    "criteo.com",
    "adnxs.com",
)

private val BLOCKED_AD_PATH_PARTS = listOf(
    "/pagead/",
    "/ads?",
    "/ads/",
    "/gampad/",
    "/securepubads.",
)

private fun WebView.evaluateArticleCaptureScript(onResult: (String?) -> Unit) {
    val defuddleBundle = context.assets.readTextOrNull(DEFUDDLE_ASSET_PATH).orEmpty()
    if (defuddleBundle.isBlank()) {
        evaluateJavascript(ArticleCaptureScripts.CAPTURE_SCRIPT, onResult)
        return
    }

    evaluateJavascript(ArticleCaptureScripts.defuddleBootstrap(defuddleBundle)) {
        evaluateJavascript(ArticleCaptureScripts.CAPTURE_SCRIPT, onResult)
    }
}

private fun String.needsBrowserUserAgent(): Boolean =
    runCatching {
        val uri = Uri.parse(this)
        uri.isBlogAuthHost() || uri.hostMatchesAny(BROWSER_USER_AGENT_HOSTS)
    }.getOrDefault(false)

private fun String.needsNoJavaScriptRetry(): Boolean =
    runCatching { Uri.parse(this).hostMatchesAny(NO_JAVASCRIPT_RETRY_HOSTS) }.getOrDefault(false)

private fun String.sameWebHostAs(other: String): Boolean =
    runCatching {
        val host = Uri.parse(this).host?.removePrefix("www.")
        val otherHost = Uri.parse(other).host?.removePrefix("www.")
        !host.isNullOrBlank() && host == otherHost
    }.getOrDefault(false)

private fun Uri.hostMatchesAny(hosts: Set<String>): Boolean {
    val normalizedHost = host?.removePrefix("www.") ?: return false
    return hosts.any { normalizedHost == it.removePrefix("www.") || normalizedHost.endsWith(".${it.removePrefix("www.")}") }
}

private fun android.content.res.AssetManager.readTextOrNull(path: String): String? =
    runCatching {
        open(path).bufferedReader().use { it.readText() }
    }.getOrNull()

private fun String?.decodeArticleCapturePayload(): JSONObject? = runCatching {
    val decoded = JSONArray("[${this ?: "null"}]").optString(0)
    JSONObject(decoded)
}.getOrNull()

private fun WebView.captureHeadlessArticle(
    sourceUrl: String,
    sourceName: String,
    onSaveForLater: (
        title: String,
        url: String,
        sourceName: String,
        resolvedUrl: String?,
        author: String?,
        excerpt: String?,
        html: String?,
        text: String?,
        markdown: String?,
        imageUrl: String?,
        captureProvider: String?,
    ) -> Unit,
    onCaptured: () -> Unit,
) {
    val resolvedUrl = url ?: sourceUrl
    val originalUrl = resolvedUrl.toOriginalArticleUrl()
    val captureProvider = resolvedUrl.captureProviderKey()
    val fallbackTitle = title?.takeIf { it.isNotBlank() } ?: originalUrl.toDisplaySourceName()
    evaluateArticleCaptureScript { rawPayload ->
        val payload = rawPayload.decodeArticleCapturePayload()
        val capturedTitle = payload?.optString("title")?.takeIf { it.isNotBlank() } ?: fallbackTitle
        val capturedText = payload?.optString("text")?.takeIf { it.isNotBlank() }
        onSaveForLater(
            capturedTitle,
            originalUrl,
            sourceName.ifBlank { originalUrl.toDisplaySourceName() },
            resolvedUrl,
            payload?.optString("author")?.takeIf { it.isNotBlank() },
            payload?.optString("excerpt")?.takeIf { it.isNotBlank() },
            payload?.optString("html")?.takeIf { it.isNotBlank() },
            capturedText,
            payload?.optString("markdown")?.takeIf { it.isNotBlank() }
                ?: capturedText?.toBasicMarkdown(capturedTitle, originalUrl),
            payload?.optString("imageUrl")?.takeIf { it.isNotBlank() },
            captureProvider,
        )
        onCaptured()
    }
}

private fun String.toDisplaySourceName(): String =
    runCatching {
        Uri.parse(this).host.orEmpty().removePrefix("www.").ifBlank { this }
    }.getOrDefault(this)

private fun String.toBasicMarkdown(title: String, url: String): String = buildString {
    appendLine("# ${title.trim().ifBlank { url }}")
    appendLine()
    appendLine("Source: $url")
    appendLine()
    this@toBasicMarkdown.trim().lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { paragraph ->
            appendLine(paragraph)
            appendLine()
        }
}.trim()

private fun Uri.isBlockedAdResource(): Boolean {
    val normalizedHost = host?.lowercase().orEmpty()
    val normalizedUrl = toString().lowercase()
    return BLOCKED_AD_HOST_PARTS.any { it in normalizedHost } ||
        BLOCKED_AD_PATH_PARTS.any { it in normalizedUrl }
}

private fun emptyWebResponse(): WebResourceResponse =
    WebResourceResponse(
        "text/plain",
        "utf-8",
        204,
        "No Content",
        mapOf("Access-Control-Allow-Origin" to "*"),
        ByteArrayInputStream(ByteArray(0)),
    )

private fun WebView.setReaderFocusMode(enabled: Boolean) {
    val enabledArg = if (enabled) "true" else "false"
    evaluateJavascript(
        """
        (function(enabled) {
          var root = document.documentElement;
          var style = document.getElementById('paywall-reader-focus-mode-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'paywall-reader-focus-mode-style';
            style.textContent = `
              html.paywall-reader-focus-mode,
              html.paywall-reader-focus-mode body {
                background: #10100e !important;
                color: #f6f0e4 !important;
                color-scheme: dark !important;
                overflow-y: auto !important;
              }
              html.paywall-reader-focus-mode body {
                font-family: Georgia, 'Times New Roman', serif !important;
                line-height: 1.68 !important;
                font-size: 18px !important;
                margin: 0 !important;
                text-rendering: optimizeLegibility !important;
                -webkit-font-smoothing: antialiased !important;
              }
              html.paywall-reader-focus-mode :where(header, footer, nav, aside, [role="banner"], [role="navigation"], [role="complementary"], [aria-modal="true"], dialog, .modal, .overlay, .newsletter, .subscribe, .subscription, .paywall, .social-share, .share, .comments, .comment, .related, .recommended, .recommendations, .trending, .most-popular, .advertisement, .advert, .ad, [class*="ad-"], [class*="ad_"], [id*="ad-"], [id*="ad_"], iframe[src*="ads"], iframe[src*="doubleclick"], iframe[src*="googlesyndication"]) {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                max-height: 0 !important;
                overflow: hidden !important;
                pointer-events: none !important;
              }
              html.paywall-reader-focus-mode .paywall-reader-focus-target,
              html.paywall-reader-focus-mode :where(article, main, [role="main"]) {
                display: block !important;
                visibility: visible !important;
                max-width: 760px !important;
                width: calc(100% - 48px) !important;
                min-height: auto !important;
                height: auto !important;
                max-height: none !important;
                margin: 0 auto !important;
                padding: 28px 0 124px !important;
                background: #10100e !important;
                color: #f6f0e4 !important;
                overflow: visible !important;
                box-shadow: none !important;
                box-sizing: border-box !important;
              }
              html.paywall-reader-focus-mode .paywall-reader-hero {
                display: block !important;
                width: 100% !important;
                aspect-ratio: 16 / 9 !important;
                object-fit: cover !important;
                border-radius: 24px !important;
                margin: 0 0 28px !important;
                background: #231e18 !important;
              }
              html.paywall-reader-focus-mode .paywall-reader-meta {
                color: #c7bda9 !important;
                font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif !important;
                font-size: 15px !important;
                font-weight: 650 !important;
                line-height: 1.35 !important;
                margin: 0 0 18px !important;
              }
              html.paywall-reader-focus-mode .paywall-reader-excerpt {
                color: #e2d8c6 !important;
                font-family: Georgia, 'Times New Roman', serif !important;
                font-size: 22px !important;
                line-height: 1.35 !important;
                margin: 0 0 28px !important;
              }
              html.paywall-reader-focus-mode :where(h1, h2, h3, h4, h5, h6) {
                color: #fff7ea !important;
                font-family: Georgia, 'Times New Roman', serif !important;
                letter-spacing: 0 !important;
                line-height: 1.15 !important;
                max-width: 100% !important;
                overflow-wrap: break-word !important;
              }
              html.paywall-reader-focus-mode :where(h1) {
                font-size: 44px !important;
                margin: 0 0 16px !important;
              }
              html.paywall-reader-focus-mode :where(h2) {
                font-size: 30px !important;
              }
              html.paywall-reader-focus-mode :where(h3, h4, h5, h6) {
                font-size: 24px !important;
              }
              html.paywall-reader-focus-mode :where(p, li, blockquote, figcaption) {
                color: #ece1cf !important;
                font-family: Georgia, 'Times New Roman', serif !important;
                font-size: 20px !important;
                line-height: 1.72 !important;
                max-width: 100% !important;
                overflow-wrap: break-word !important;
                margin: 0 0 22px !important;
              }
              html.paywall-reader-focus-mode :where(a) {
                color: #e5b76b !important;
              }
              html.paywall-reader-focus-mode :where(img, video, picture, figure) {
                max-width: 100% !important;
                height: auto !important;
              }
              html.paywall-reader-focus-mode * {
                text-shadow: none !important;
              }
            `;
            document.head.appendChild(style);
          }

          Array.prototype.slice.call(document.querySelectorAll('.paywall-reader-focus-target')).forEach(function(node) {
            node.classList.remove('paywall-reader-focus-target');
          });

          if (!enabled) {
            root.classList.remove('paywall-reader-focus-mode');
            if (document.body && document.body.dataset.paywallReaderOriginalHtml) {
              document.body.innerHTML = document.body.dataset.paywallReaderOriginalHtml;
              delete document.body.dataset.paywallReaderOriginalHtml;
            }
            return 'off';
          }

              var target = document.querySelector('article') ||
            document.querySelector('[role="main"]') ||
            document.querySelector('main') ||
            document.querySelector('.article, .post, .entry-content, .story, .content') ||
            document.body;
          if (target) {
            target.classList.add('paywall-reader-focus-target');
            if (document.body && !document.body.dataset.paywallReaderOriginalHtml) {
              function meta(name) {
                var node = document.querySelector('meta[property="' + name + '"]') ||
                  document.querySelector('meta[name="' + name + '"]');
                return node ? (node.getAttribute('content') || '').trim() : '';
              }
              function firstUsefulImage() {
                var metaImage = meta('og:image') || meta('twitter:image');
                if (metaImage) return metaImage;
                var images = Array.prototype.slice.call(target.querySelectorAll('img'));
                for (var i = 0; i < images.length; i++) {
                  var image = images[i];
                  var src = image.currentSrc || image.src || image.getAttribute('data-src') || '';
                  var width = image.naturalWidth || image.width || 0;
                  var height = image.naturalHeight || image.height || 0;
                  if (src && width >= 240 && height >= 120) return src;
                }
                return '';
              }
              var title = document.querySelector('h1') || document.querySelector('meta[property="og:title"]');
              var reader = document.createElement('main');
              reader.className = 'paywall-reader-focus-target';
              var titleText = title ? (title.innerText || title.textContent || title.getAttribute('content') || '') : document.title;
              var imageUrl = firstUsefulImage();
              if (imageUrl) {
                var hero = document.createElement('img');
                hero.className = 'paywall-reader-hero';
                hero.src = imageUrl;
                hero.alt = titleText || '';
                reader.appendChild(hero);
              }
              if (titleText) {
                var h1 = document.createElement('h1');
                h1.textContent = titleText;
                reader.appendChild(h1);
              }
              var byline = meta('author') ||
                (document.querySelector('[rel="author"], .byline, [class*="byline"], [class*="author"]') || {}).textContent ||
                location.hostname.replace(/^www\./, '');
              if (byline) {
                var metaNode = document.createElement('div');
                metaNode.className = 'paywall-reader-meta';
                metaNode.textContent = byline.trim();
                reader.appendChild(metaNode);
              }
              var excerpt = meta('description') || meta('og:description');
              if (excerpt) {
                var excerptNode = document.createElement('p');
                excerptNode.className = 'paywall-reader-excerpt';
                excerptNode.textContent = excerpt;
                reader.appendChild(excerptNode);
              }
              var text = (target.innerText || target.textContent || '').trim();
              var lines = text.split(/\n+/)
                .map(function(line) { return line.trim(); })
                .filter(function(line) {
                  return line.length > 0 &&
                    line !== titleText &&
                    line.toLowerCase() !== (titleText || '').toLowerCase() &&
                    line.toLowerCase() !== (excerpt || '').toLowerCase() &&
                    line.toLowerCase() !== (byline || '').toLowerCase() &&
                    line.length > 24;
                });
              lines.slice(0, 120).forEach(function(line) {
                var node = document.createElement('p');
                node.textContent = line;
                reader.appendChild(node);
              });
              if (reader.children.length <= (titleText ? 1 : 0)) {
                reader.appendChild(target.cloneNode(true));
              }
              document.body.dataset.paywallReaderOriginalHtml = document.body.innerHTML;
              document.body.innerHTML = '';
              document.body.appendChild(reader);
            }
          }
          root.classList.add('paywall-reader-focus-mode');
          document.documentElement.classList.remove('modal-open', 'no-scroll', 'noscroll', 'scroll-lock');
          if (document.body) {
            document.body.classList.remove('modal-open', 'no-scroll', 'noscroll', 'scroll-lock');
          }
          return 'on';
        })($enabledArg);
        """.trimIndent(),
        null,
    )
}

private fun WebView.applySiteChromeCleanup() {
    evaluateJavascript(
        """
        (function() {
          var style = document.getElementById('paywall-reader-site-cleanup-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'paywall-reader-site-cleanup-style';
            style.textContent = `
              dual-offer-banner,
              #base-banner,
              .base-banner {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                min-height: 0 !important;
                pointer-events: none !important;
              }
              html,
              body {
                overflow-y: auto !important;
                overscroll-behavior: auto !important;
                touch-action: auto !important;
              }
              html.paywall-reader-economist-unlocked,
              html.paywall-reader-economist-unlocked body,
              html.paywall-reader-economist-unlocked #__next,
              html.paywall-reader-economist-unlocked main {
                height: auto !important;
                min-height: 100% !important;
                max-height: none !important;
                overflow-y: auto !important;
                position: static !important;
                touch-action: auto !important;
              }
              html.paywall-reader-wired-light,
              html.paywall-reader-wired-light body,
              html.paywall-reader-wired-light main,
              html.paywall-reader-wired-light article,
              html.paywall-reader-wired-light section {
                color-scheme: light !important;
                font-family: Arial, Helvetica, sans-serif !important;
              }
              html.paywall-reader-wired-light,
              html.paywall-reader-wired-light body,
              html.paywall-reader-wired-light main {
                background: #ffffff !important;
              }
              html.paywall-reader-wired-light :where([class*="Ad"], [class*="ad-"], [class*="ad_"], [class*=" ad"], .ad) {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                min-height: 0 !important;
                max-height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                pointer-events: none !important;
              }
              html.paywall-reader-wired-light :where(h1, h2, h3, h4, h5, h6, p, a, span, li, time, figcaption, small, strong, em, button, label) {
                position: relative !important;
                z-index: 2147483647 !important;
                background: transparent !important;
                color: #111111 !important;
                -webkit-text-fill-color: #111111 !important;
                font-family: Arial, Helvetica, sans-serif !important;
                text-shadow: none !important;
                -webkit-text-stroke-width: 0 !important;
                opacity: 1 !important;
                visibility: visible !important;
              }
            `;
            document.head.appendChild(style);
          }

          function isVisible(element) {
            return !!(element && element.getClientRects && element.getClientRects().length);
          }

          function clickIfVisible(selector) {
            document.querySelectorAll(selector).forEach(function(element) {
              if (isVisible(element)) {
                element.click();
              }
            });
          }

          clickIfVisible('dual-offer-banner[open] #banner-toggle[aria-label="Collapse"]');

          document.querySelectorAll('dual-offer-banner[open]').forEach(function(element) {
            element.removeAttribute('open');
          });

          document.querySelectorAll(
            '[data-testid="modal-close"], [data-testid="close-button"], ' +
            'button[aria-label="Close"], button[aria-label="close"], ' +
            'button[aria-label="Dismiss"], button[aria-label="dismiss"], ' +
            'button[aria-label="Collapse"]'
          ).forEach(function(button) {
            var container = button.closest('[role="dialog"], [aria-modal="true"], dialog, .modal, .overlay');
            var text = ((container || button.parentElement || button).innerText || '').toLowerCase();
            if (
              text.indexOf('subscribe') !== -1 ||
              text.indexOf('sign up') !== -1 ||
              text.indexOf('newsletter') !== -1 ||
              text.indexOf('unlimited access') !== -1 ||
              text.indexOf('get access') !== -1 ||
              text.indexOf('continue reading') !== -1
            ) {
              button.click();
            }
          });

          document.documentElement.classList.remove('modal-open', 'no-scroll', 'noscroll', 'scroll-lock');
          document.documentElement.style.overflow = '';
          document.documentElement.style.overflowY = 'auto';
          document.documentElement.style.touchAction = 'auto';
          if (document.body) {
            document.body.classList.remove('modal-open', 'no-scroll', 'noscroll', 'scroll-lock');
            document.body.style.overflow = '';
            document.body.style.overflowY = 'auto';
            document.body.style.touchAction = 'auto';
          }

          if (location.hostname.indexOf('economist.com') !== -1) {
            document.documentElement.classList.add('paywall-reader-economist-unlocked');
            [document.body, document.getElementById('__next'), document.querySelector('main')]
              .filter(Boolean)
              .forEach(function(element) {
                element.style.height = 'auto';
                element.style.maxHeight = 'none';
                element.style.overflowY = 'auto';
                element.style.position = 'static';
                element.style.touchAction = 'auto';
              });
          } else {
            document.documentElement.classList.remove('paywall-reader-economist-unlocked');
          }

          var decodedLocation = '';
          try {
            decodedLocation = decodeURIComponent(location.href);
          } catch (error) {
            decodedLocation = location.href;
          }

          if (location.hostname.indexOf('wired.com') !== -1 || decodedLocation.indexOf('wired.com') !== -1) {
            document.documentElement.classList.add('paywall-reader-wired-light');
            document.documentElement.style.colorScheme = 'light';
            if (document.body) {
              document.body.style.colorScheme = 'light';
            }
          } else {
            document.documentElement.classList.remove('paywall-reader-wired-light');
            document.documentElement.style.colorScheme = '';
            if (document.body) {
              document.body.style.colorScheme = '';
            }
          }
        })();
        """.trimIndent(),
        null,
    )
}

private class ReaderPageSignalBridge(
    private val webView: WebView,
    private val enabled: Boolean,
    private val onAuthSurfaceChanged: (Boolean) -> Unit,
) {
    @JavascriptInterface
    fun setAuthSurface(active: Boolean) {
        if (!enabled) return
        webView.post {
            onAuthSurfaceChanged(active)
        }
    }
}

private fun WebView.installPageStateSignals(enabled: Boolean) {
    if (!enabled) {
        return
    }

    evaluateJavascript(
        """
        (function() {
          if (!window.PaywallReaderBridge) {
            return;
          }

          var style = document.getElementById('paywall-reader-blog-auth-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'paywall-reader-blog-auth-style';
            style.textContent = `
              html.paywall-reader-auth-surface,
              html.paywall-reader-auth-surface body,
              html.paywall-reader-auth-surface [role="dialog"][aria-modal="true"] {
                background: #f7f4ed !important;
                color: #242424 !important;
              }
              html.paywall-reader-auth-surface [role="dialog"] {
                position: fixed !important;
                inset: 0 !important;
                width: 100vw !important;
                min-height: 100vh !important;
                height: auto !important;
                max-height: none !important;
                overflow-y: auto !important;
                z-index: 2147483647 !important;
              }
            `;
            document.head.appendChild(style);
          }

          function hasAuthSurface() {
            var text = document.body ? (document.body.innerText || '').toLowerCase() : '';
            var hasAuthCopy = [
              'welcome back',
              'sign in with google',
              'sign in with facebook',
              'sign in with apple',
              'sign in with x',
              'sign in with email',
              'continue with google',
              'continue with email',
              'remember me for faster sign in',
              'forgot email',
              'sign in to medium',
              'sign in to substack',
              'iniciar sesión en substack',
              'iniciar sesion en substack',
              'tu correo electrónico',
              'tu correo electronico',
              'crear cuenta'
            ].some(function(marker) { return text.indexOf(marker) !== -1; });
            var hasAuthControls = !!document.querySelector(
              '[role="dialog"], [aria-modal="true"], dialog, form, input[type="email"], input[type="password"]'
            );
            return hasAuthCopy && hasAuthControls;
          }

          function emitAuthSurface() {
            var active = hasAuthSurface();
            document.documentElement.classList.toggle('paywall-reader-auth-surface', active);
            window.PaywallReaderBridge.setAuthSurface(active);
          }

          if (!window.__paywallReaderAuthObserver) {
            window.__paywallReaderAuthObserver = new MutationObserver(emitAuthSurface);
            window.__paywallReaderAuthObserver.observe(document.documentElement, {
              childList: true,
              subtree: true,
              attributes: true
            });
          }

          emitAuthSurface();
          setTimeout(emitAuthSurface, 350);
          setTimeout(emitAuthSurface, 1200);
        })();
        """.trimIndent(),
        null,
    )
}

private fun WebView.loadFallbackReaderIfBlank(loadedUrl: String?, delayMillis: Long) {
    val loadedUri = runCatching { Uri.parse(loadedUrl) }.getOrNull() ?: return
    if (
        loadedUri.host != ARTICLE_READER_HOST &&
        loadedUri.host != UNWALL_HOST &&
        !loadedUri.isPaywallFallbackHost()
    ) {
        return
    }

    postDelayed(
        {
            if (!settings.javaScriptEnabled) {
                return@postDelayed
            }
            evaluateJavascript(
                """
                (function() {
                  var text = document.body ? (document.body.innerText || '').trim() : '';
                  var media = document.querySelectorAll('article, main, img, video, iframe').length;
                  return text.length + media;
                })();
                """.trimIndent(),
            ) { result ->
                val pageWeight = result?.trim('"')?.toIntOrNull() ?: 0
                val currentHost = runCatching { Uri.parse(url).host }.getOrNull()
                if (pageWeight < 300) {
                    val currentUrl = url ?: loadedUrl ?: return@evaluateJavascript
                    val originalUrl = runCatching { Uri.parse(currentUrl).readerOriginalUrl() }.getOrNull()
                        ?: currentUrl.takeIf { runCatching { Uri.parse(it).isPaywallFallbackHost() }.getOrDefault(false) }
                    if (!originalUrl.isNullOrBlank()) {
                        val originalUri = Uri.parse(originalUrl)
                        when (currentHost) {
                            ARTICLE_READER_HOST -> {
                                val fallbackUrl = originalUri.toUnwallUrl()
                                settings.javaScriptEnabled = true
                                loadUrl(fallbackUrl)
                            }

                            UNWALL_HOST -> {
                                loadOriginalWithoutJavaScriptFallback(originalUrl)
                            }

                            else -> {
                                loadOriginalWithoutJavaScriptFallback(originalUrl)
                            }
                        }
                    }
                }
            }
        },
        delayMillis,
    )
}

private fun WebView.loadFallbackReaderIfPaywalled(loadedUrl: String?, delayMillis: Long) {
    val loadedUri = runCatching { Uri.parse(loadedUrl) }.getOrNull() ?: return
    if (!loadedUri.isReaderServiceUrl() && !loadedUri.isPaywallFallbackHost()) {
        return
    }

    postDelayed(
        {
            evaluateJavascript(
                """
                (function() {
                  var text = document.body ? (document.body.innerText || '').toLowerCase() : '';
                  return [
                    'this article is exclusive to subscribers',
                    'start your free trial and plug in',
                    'subscribe to continue',
                    'sign in to continue reading',
                    'already a subscriber?',
                    'member-only story',
                    'become a medium member',
                    'get unlimited access to every story',
                    'upgrade to continue reading',
                    'this post is for paid subscribers',
                    'this post is only for paid subscribers',
                    'subscribe to keep reading'
                  ].some(function(marker) { return text.indexOf(marker) !== -1; });
                })();
                """.trimIndent(),
            ) { result ->
                if (result?.trim('"') != "true") {
                    return@evaluateJavascript
                }

                val currentUrl = url ?: loadedUrl ?: return@evaluateJavascript
                val currentUri = runCatching { Uri.parse(currentUrl) }.getOrNull() ?: return@evaluateJavascript
                val originalUrl = currentUri.readerOriginalUrl() ?: currentUrl
                val originalUri = runCatching { Uri.parse(originalUrl) }.getOrNull() ?: return@evaluateJavascript
                when (currentUri.host) {
                    ARTICLE_READER_HOST -> {
                        settings.javaScriptEnabled = true
                        loadUrl(originalUri.toUnwallUrl())
                    }

                    UNWALL_HOST -> loadOriginalWithoutJavaScriptFallback(originalUrl)

                    else -> {
                        if (settings.javaScriptEnabled) {
                            settings.javaScriptEnabled = true
                            loadUrl(originalUri.toArticleReaderUrl())
                        } else {
                            loadArchiveSearch(originalUrl)
                        }
                    }
                }
            }
        },
        delayMillis,
    )
}

private fun WebView.loadOriginalWithoutJavaScriptFallback(originalUrl: String) {
    settings.javaScriptEnabled = false
    loadUrl(originalUrl)
    postDelayed(
        {
            evaluateJavascript(
                """
                (function() {
                  var text = document.body ? (document.body.innerText || '').trim() : '';
                  var media = document.querySelectorAll('article, main, img, video, iframe').length;
                  return text.length + media;
                })();
                """.trimIndent(),
            ) { result ->
                val pageWeight = result?.trim('"')?.toIntOrNull() ?: 0
                val currentOriginalUrl = url?.toOriginalArticleUrl()
                if (pageWeight < 300 && currentOriginalUrl == originalUrl) {
                    loadArchiveSearch(originalUrl)
                }
            }
        },
        5_000L,
    )
}

private fun WebView.loadArchiveSearch(url: String) {
    settings.javaScriptEnabled = true
    loadUrl(url.toArchiveSearchUrl())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoadingIndicator()
            Text(
                text = stringResource(R.string.reader_loading),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ReaderError(
    onRetry: () -> Unit,
    onOpenOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.padding(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.reader_error_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.reader_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.reader_retry))
        }
        OutlinedButton(onClick = onOpenOriginal) {
            Text(stringResource(R.string.reader_open_original))
        }
    }
}

private fun WebView.configureReaderSettings(useBrowserUserAgent: Boolean = false) {
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setAcceptThirdPartyCookies(this@configureReaderSettings, true)
        }
    }
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    if (useBrowserUserAgent) {
        settings.userAgentString = settings.userAgentString
            .replace("; wv", "")
            .replace(" Version/4.0", "")
    }
    settings.loadsImagesAutomatically = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.javaScriptCanOpenWindowsAutomatically = useBrowserUserAgent
    settings.setSupportMultipleWindows(useBrowserUserAgent)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        settings.forceDark = WebSettings.FORCE_DARK_OFF
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        settings.isAlgorithmicDarkeningAllowed = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        settings.safeBrowsingEnabled = true
    }
}

private fun WebView.applyReaderChrome() {
    evaluateJavascript(
        """
        (function() {
          var style = document.getElementById('paywall-reader-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'paywall-reader-style';
            style.textContent = `
              .promo-bar { display: none !important; }
              #topFrame,
              [data-testid="reader-toolbar"] {
                display: none !important;
              }
              body { padding-top: 0 !important; }
            `;
            document.head.appendChild(style);
          }
        })();
        """.trimIndent(),
        null,
    )
}

private fun WebView.applyAdCleanup() {
    evaluateJavascript(
        """
        (function() {
          var style = document.getElementById('paywall-reader-ad-style');
          if (!style) {
            style = document.createElement('style');
            style.id = 'paywall-reader-ad-style';
            style.textContent = `
              iframe[src*="googlesyndication.com"],
              iframe[src*="doubleclick.net"],
              iframe[src*="googleadservices.com"],
              iframe[src*="adtrafficquality.google"],
              iframe[id^="google_ads_iframe"],
              ins.adsbygoogle,
              .adsbygoogle,
              [id^="google_ads_"],
              [id*="google_ads_iframe"],
              [class*="google-ad"],
              [class*="GoogleAd"],
              [data-ad-client],
              [data-ad-slot] {
                display: none !important;
                visibility: hidden !important;
                width: 0 !important;
                height: 0 !important;
                min-height: 0 !important;
                margin: 0 !important;
                padding: 0 !important;
                border: 0 !important;
              }
            `;
            document.head.appendChild(style);
          }
        })();
        """.trimIndent(),
        null,
    )
}
