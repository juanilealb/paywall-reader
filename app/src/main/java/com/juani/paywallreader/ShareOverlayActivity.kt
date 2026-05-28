package com.juani.paywallreader

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.juani.paywallreader.data.capture.CaptureWorkManager
import com.juani.paywallreader.data.local.AppDatabase
import com.juani.paywallreader.data.repository.SourceRepository
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_PENDING
import com.juani.paywallreader.ui.navigation.ExternalShareRoutePolicy
import com.juani.paywallreader.ui.theme.PaywallReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareOverlayActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureOverlayWindow()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun configureOverlayWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setFinishOnTouchOutside(true)
        enableEdgeToEdge()
    }

    private fun handleIntent(intent: Intent?) {
        val url = ExternalShareRoutePolicy.decide(intent.extractSharedUrl()).captureUrl
        if (url == null) {
            clearHandledIntent()
            finish()
            return
        }
        sharedUrl = url
        lifecycleScope.launch {
            persistSharedUrl(url)
            setContent {
                PaywallReaderTheme {
                    ShareOverlayRoute(
                        url = url,
                        onDismiss = {
                            clearHandledIntent()
                            finish()
                        },
                        onReadNow = {
                            clearHandledIntent()
                            openReader(url)
                        },
                    )
                }
            }
        }
    }

    private fun clearHandledIntent() {
        sharedUrl = null
        setIntent(Intent(this, ShareOverlayActivity::class.java))
    }

    private fun openReader(url: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_READER_URL, url)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }

    private suspend fun persistSharedUrl(url: String) {
        withContext(Dispatchers.IO) {
            val repository = SourceRepository(
                AppDatabase.getInstance(applicationContext).sourceDao(),
            )
            repository.saveBookmarkFromExternalShare(url)
            repository.updateCaptureStatus(url, CAPTURE_STATUS_PENDING)
        }
        CaptureWorkManager.enqueue(applicationContext, url)
    }
}

@Composable
private fun ShareOverlayRoute(
    url: String,
    onDismiss: () -> Unit,
    onReadNow: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ShareConfirmationSheet(
            url = url,
            onDismiss = onDismiss,
            onReadNow = onReadNow,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareConfirmationSheet(
    url: String,
    onDismiss: () -> Unit,
    onReadNow: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(url) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        delay(2_800)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(36.dp),
                )
            }
            Text(
                text = stringResource(R.string.saved_to_reader),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = url.toDisplayHost(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onReadNow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.read_now))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

private fun String.toDisplayHost(): String =
    removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .removePrefix("www.")
