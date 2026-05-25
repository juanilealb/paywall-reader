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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
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
import java.io.ByteArrayInputStream

private val FoldFloatingActionButtonSize = 64.dp
private val ReaderToolbarHeight = 76.dp
private val ReaderToolbarActionSize = 48.dp
private val ReaderToolbarIconSize = 22.dp

@Composable
fun ReaderRoute(
    sourceName: String,
    sourceUrl: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = readerViewModel(),
) {
    ReaderScreen(
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        onBack = onBack,
        showBackButton = showBackButton,
        onSaveForLater = viewModel::saveForLater,
        onMarkRead = viewModel::markRead,
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
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    onSaveForLater: (title: String, url: String, sourceName: String) -> Unit,
    onMarkRead: (url: String) -> Unit,
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
    var currentUrl by remember(sourceUrl) { mutableStateOf(sourceUrl.trim()) }
    var currentTitle by remember(sourceUrl) { mutableStateOf(sourceName) }
    val initialUrl = remember(sourceUrl) {
        sourceUrl.trim()
    }
    val detectsAuthSurfaces = remember(initialUrl) {
        initialUrl.isBlogAuthHost()
    }
    var toolbarExpanded by rememberSaveable(sourceUrl) { mutableStateOf(false) }
    val openOriginal = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl.ifBlank { sourceUrl })))
    }
    val shareOriginal = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentUrl.ifBlank { sourceUrl })
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.reader_share)))
    }
    val openReaderVersion = {
        val url = webView?.url ?: currentUrl.ifBlank { sourceUrl }
        if (!url.isReaderServiceUrl()) {
            webView?.loadArchiveSearch(url.toOriginalArticleUrl())
        }
    }
    val saveCurrentForLater = {
        val url = (webView?.url ?: currentUrl.ifBlank { sourceUrl }).toOriginalArticleUrl()
        onSaveForLater(currentTitle.ifBlank { sourceName }, url, sourceName)
    }
    val markCurrentRead = {
        onMarkRead((webView?.url ?: currentUrl.ifBlank { sourceUrl }).toOriginalArticleUrl())
    }
    fun updateNavigationState(view: WebView?) {
        canNavigateBack = view?.canGoBack() == true
        canNavigateForward = view?.canGoForward() == true
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
                            configureReaderSettings(useBrowserUserAgent = detectsAuthSurfaces)
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
                                    if (!detectsAuthSurfaces || view == null || resultMsg == null) {
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
                                    view?.installPageStateSignals(detectsAuthSurfaces)
                                    if (url?.isReaderServiceUrl() == true) {
                                    view?.applyReaderChrome()
                                    view?.postDelayed({ view.applyReaderChrome() }, 700L)
                                        view?.loadFallbackReaderIfBlank(url, 2_500L)
                                }
                                if (!isAuthSurface) {
                                    view?.loadFallbackReaderIfPaywalled(url, 2_500L)
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
                    onOpenReader = openReaderVersion,
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
        }
    }
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
    onOpenReader: () -> Unit,
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
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
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
                onOpenReader = onOpenReader,
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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
                onOpenReader = onOpenReader,
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
    onOpenReader: () -> Unit,
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
        IconButton(onClick = onOpenReader, modifier = Modifier.size(ReaderToolbarActionSize)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Article,
                contentDescription = stringResource(R.string.reader_open_reader),
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

private const val REMOVE_PAYWALLS_HOST = "removepaywalls.com"
private const val ARTICLE_READER_HOST = "accessarticlenow.com"
private const val UNWALL_HOST = "unwall.app"
private const val ARCHIVE_FO_HOST = "archive.fo"

private val ALLOWED_READER_HOSTS = setOf(
    REMOVE_PAYWALLS_HOST,
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

private fun Uri.toArticleReaderUrl(): String =
    "https://$ARTICLE_READER_HOST/api/c/full?q=${Uri.encode(toString())}"

private fun Uri.toUnwallUrl(): String =
    "https://$UNWALL_HOST/${toString().removePrefix("https://").removePrefix("http://")}?reader=1"

private fun String.isReaderServiceUrl(): Boolean =
    runCatching { Uri.parse(this).isReaderServiceUrl() }.getOrDefault(false)

private fun Uri.isReaderServiceUrl(): Boolean = host in ALLOWED_READER_HOSTS

private fun String.isArchiveServiceUrl(): Boolean =
    runCatching {
        Uri.parse(this).host in setOf("archive.today", ARCHIVE_FO_HOST, "archive.is", "archive.ph")
    }.getOrDefault(false)

private fun String.toOriginalArticleUrl(): String =
    runCatching {
        val uri = Uri.parse(this)
        uri.readerOriginalUrl() ?: this
    }.getOrDefault(this)

private fun String.toArchiveSearchUrl(): String =
    "https://archive.ph/search/?q=${Uri.encode(this)}"

private fun String.isLikelyWebUrl(): Boolean =
    runCatching {
        val uri = Uri.parse(this)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

private fun String.isBlogAuthHost(): Boolean =
    runCatching { Uri.parse(this).isBlogAuthHost() }.getOrDefault(false)

private fun Uri.isBlogAuthHost(): Boolean {
    val normalizedHost = host?.removePrefix("www.") ?: return false
    return BLOG_AUTH_HOSTS.any { normalizedHost == it || normalizedHost.endsWith(".$it") }
}

private fun Uri.isPaywallFallbackHost(): Boolean {
    val normalizedHost = host?.removePrefix("www.") ?: return false
    return PAYWALL_FALLBACK_HOSTS.any { normalizedHost == it || normalizedHost.endsWith(".$it") }
}

private fun Uri.readerOriginalUrl(): String? {
    return when (host) {
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

private fun Uri.isLikelyArticleUrl(): Boolean {
    val segments = pathSegments.filter { it.isNotBlank() }
    val firstSegment = segments.firstOrNull().orEmpty()
    val lastSegment = segments.lastOrNull().orEmpty()
    return (segments.size >= 2 && firstSegment in ARTICLE_PATH_PREFIXES) ||
        segments.size >= 3 ||
        (segments.size >= 2 && lastSegment.length >= 20 && ("-" in lastSegment || lastSegment.endsWith(".html")))
}

private val ARTICLE_PATH_PREFIXES = setOf(
    "article",
    "articles",
    "news",
    "review",
    "reviews",
    "story",
)

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
    if (loadedUri.host != ARTICLE_READER_HOST && loadedUri.host != UNWALL_HOST) {
        return
    }

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
                val currentHost = runCatching { Uri.parse(url).host }.getOrNull()
                if (pageWeight < 300 && (currentHost == ARTICLE_READER_HOST || currentHost == UNWALL_HOST)) {
                    val originalUrl = runCatching { Uri.parse(url).readerOriginalUrl() }.getOrNull()
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
    val loadedHost = loadedUri.host ?: return
    if (loadedHost !in ALLOWED_READER_HOSTS && !loadedUri.isPaywallFallbackHost()) {
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
                imageVector = Icons.Rounded.CloudOff,
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
