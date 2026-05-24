package com.juani.paywallreader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = RiverSecondary,
    onSecondary = RiverOnSecondary,
    secondaryContainer = RiverSecondaryContainer,
    onSecondaryContainer = RiverOnSecondaryContainer,
    tertiary = InkTertiary,
    onTertiary = InkOnTertiary,
    tertiaryContainer = InkTertiaryContainer,
    onTertiaryContainer = InkOnTertiaryContainer,
    error = WarmError,
    onError = WarmOnError,
    errorContainer = WarmErrorContainer,
    onErrorContainer = WarmOnErrorContainer,
    background = PaperBackground,
    onBackground = PaperOnSurface,
    surface = PaperSurface,
    onSurface = PaperOnSurface,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = PaperOnSurfaceVariant,
    outline = PaperOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = EmeraldOnPrimaryContainer,
    onPrimaryContainer = EmeraldPrimaryContainer,
    secondary = RiverSecondaryContainer,
    onSecondary = RiverOnSecondaryContainer,
    secondaryContainer = RiverOnSecondary,
    onSecondaryContainer = RiverSecondaryContainer,
    tertiary = InkTertiaryContainer,
    onTertiary = InkOnTertiaryContainer,
    tertiaryContainer = InkOnTertiary,
    onTertiaryContainer = InkTertiaryContainer,
    error = WarmErrorContainer,
    onError = WarmOnErrorContainer,
    errorContainer = WarmError,
    onErrorContainer = WarmOnError,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOnSurfaceVariant,
)

@Composable
fun PaywallReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> DarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        WindowCompat.getInsetsController(
            (view.context as android.app.Activity).window,
            view,
        ).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PaywallReaderTypography,
        content = content,
    )
}
