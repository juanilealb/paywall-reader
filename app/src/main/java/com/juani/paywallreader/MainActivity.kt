package com.juani.paywallreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.juani.paywallreader.ui.navigation.AppNavigation
import com.juani.paywallreader.ui.theme.PaywallReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaywallReaderTheme {
                AppNavigation()
            }
        }
    }
}
