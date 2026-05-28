package com.juani.paywallreader

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.juani.paywallreader.ui.navigation.AppNavigation
import com.juani.paywallreader.ui.navigation.ExternalShareRoutePolicy
import com.juani.paywallreader.ui.theme.PaywallReaderTheme

class MainActivity : ComponentActivity() {
    private var initialReaderUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialReaderUrl = intent.extractInitialReaderUrl()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        enableEdgeToEdge()
        setContent {
            PaywallReaderTheme {
                AppNavigation(
                    initialReaderUrl = initialReaderUrl,
                    onInitialReaderUrlHandled = {
                        initialReaderUrl = null
                        setIntent(Intent(this, MainActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initialReaderUrl = intent.extractInitialReaderUrl()
    }
}

private fun Intent.extractInitialReaderUrl(): String? =
    ExternalShareRoutePolicy.decide(getStringExtra(EXTRA_OPEN_READER_URL)).captureUrl
