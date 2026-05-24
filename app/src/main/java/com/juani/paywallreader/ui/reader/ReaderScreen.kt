package com.juani.paywallreader.ui.reader

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.juani.paywallreader.R
import java.io.ByteArrayInputStream

@Composable
fun ReaderRoute(
    sourceName: String,
    sourceUrl: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    ReaderScreen(
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        onBack = onBack,
        showBackButton = showBackButton,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderScreen(
    sourceName: String,
    sourceUrl: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var canNavigateBack by remember { mutableStateOf(false) }
    var canNavigateForward by remember { mutableStateOf(false) }
    val initialUrl = remember(sourceUrl) {
        sourceUrl.trim()
    }
    val openOriginal = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
    }
    val shareOriginal = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sourceUrl)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.reader_share)))
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
        val toolbarAlignment = if (maxWidth >= 600.dp) {
            Alignment.BottomEnd
        } else {
            Alignment.BottomCenter
        }
        val showShareAction = maxWidth >= 380.dp

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        configureReaderSettings()
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                updateNavigationState(view)
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                                updateNavigationState(view)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                updateNavigationState(view)
                                view?.applyAdCleanup()
                                if (url?.isRemovePaywallsUrl() == true) {
                                    view?.applyReaderChrome()
                                }
                            }

                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                url: String?,
                                isReload: Boolean,
                            ) {
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

                                if (url.host in ALLOWED_READER_HOSTS) {
                                    return false
                                }

                                if (!request.hasGesture()) {
                                    return false
                                }

                                view?.loadUrl(url.toArticleReaderUrl())
                                return true
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

            if (isLoading && progress in 1..99) {
                LinearProgressIndicator(
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

            ReaderFloatingToolbar(
                showBackButton = showBackButton,
                canNavigateBack = canNavigateBack,
                canNavigateForward = canNavigateForward,
                isLoading = isLoading,
                showShareAction = showShareAction,
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
                onShare = shareOriginal,
                modifier = Modifier
                    .align(toolbarAlignment)
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(16.dp)
                    .widthIn(max = 520.dp),
            )
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
    showShareAction: Boolean,
    onBack: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onRefreshOrStop: () -> Unit,
    onOpenOriginal: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = onRefreshOrStop,
            ) {
                Icon(
                    imageVector = if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                    contentDescription = stringResource(
                        if (isLoading) R.string.reader_stop_loading else R.string.reader_refresh,
                    ),
                )
            }
        },
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End,
    ) {
        if (showBackButton) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.reader_back),
                )
            }
        }
        IconButton(
            onClick = onNavigateBack,
            enabled = canNavigateBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.reader_web_back),
            )
        }
        IconButton(
            onClick = onNavigateForward,
            enabled = canNavigateForward,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = stringResource(R.string.reader_web_forward),
            )
        }
        IconButton(onClick = onOpenOriginal) {
            Icon(
                imageVector = Icons.Rounded.OpenInBrowser,
                contentDescription = stringResource(R.string.reader_open_original),
            )
        }
        if (showShareAction) {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.reader_share),
                )
            }
        }
    }
}

private const val REMOVE_PAYWALLS_HOST = "removepaywalls.com"
private const val ARTICLE_READER_HOST = "accessarticlenow.com"

private val ALLOWED_READER_HOSTS = setOf(
    REMOVE_PAYWALLS_HOST,
    ARTICLE_READER_HOST,
    "archive.today",
    "archive.fo",
    "archive.is",
    "archive.ph",
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

private fun String.isRemovePaywallsUrl(): Boolean =
    runCatching { Uri.parse(this).host == REMOVE_PAYWALLS_HOST }.getOrDefault(false)

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
            CircularProgressIndicator()
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

private fun WebView.configureReaderSettings() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.javaScriptCanOpenWindowsAutomatically = false
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
              #topFrame {
                position: fixed !important;
                top: 12px !important;
                left: 50% !important;
                transform: translateX(-50%) !important;
                z-index: 2147483647 !important;
                width: auto !important;
                max-width: calc(100vw - 20px) !important;
                height: auto !important;
                padding: 0 !important;
                margin: 0 !important;
                background: transparent !important;
                border: 0 !important;
                box-shadow: none !important;
              }
              #topFrame .brand,
              #topFrame .brand-name,
              #topFrame .header-divider,
              #topFrame .header-spacer,
              #topFrame img {
                display: none !important;
              }
              #topFrame .header-row,
              #topFrame .option-row {
                display: flex !important;
                width: auto !important;
                gap: 6px !important;
                align-items: center !important;
                justify-content: center !important;
                flex-wrap: wrap !important;
                padding: 0 !important;
                margin: 0 !important;
                background: transparent !important;
              }
              #topFrame .option-button {
                min-width: 42px !important;
                border-radius: 10px !important;
                padding: 9px 11px !important;
                background: rgba(16, 20, 17, 0.9) !important;
                color: white !important;
                border: 1px solid rgba(255,255,255,0.18) !important;
                box-shadow: 0 8px 22px rgba(0,0,0,0.22) !important;
                text-decoration: none !important;
                font-weight: 700 !important;
                letter-spacing: 0 !important;
              }
              body { padding-top: 58px !important; }
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
