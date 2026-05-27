package com.juani.paywallreader

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.juani.paywallreader.ui.navigation.AppNavigation
import com.juani.paywallreader.ui.theme.PaywallReaderTheme

class MainActivity : ComponentActivity() {
    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedUrl = intent.extractSharedUrl()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        enableEdgeToEdge()
        setContent {
            PaywallReaderTheme {
                AppNavigation(sharedUrl = sharedUrl)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedUrl = intent.extractSharedUrl()
    }
}

private fun Intent?.extractSharedUrl(): String? {
    if (this == null) return null
    val candidate = when (action) {
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> dataString
        else -> null
    }
    return candidate
        ?.lineSequence()
        ?.map { it.trim() }
        ?.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
}
