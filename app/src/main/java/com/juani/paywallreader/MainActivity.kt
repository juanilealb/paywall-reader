package com.juani.paywallreader

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.juani.paywallreader.ui.navigation.AppNavigation
import com.juani.paywallreader.ui.theme.PaywallReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        enableEdgeToEdge()
        setContent {
            PaywallReaderTheme {
                AppNavigation()
            }
        }
    }
}
