package com.juani.paywallreader.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.juani.paywallreader.ui.home.HomeRoute
import com.juani.paywallreader.ui.reader.ReaderRoute
import com.juani.paywallreader.ui.theme.PaywallReaderMotion
import kotlinx.serialization.Serializable

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(AppRoute.Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRoute.Home> {
                HomeRoute(
                    onSourceClick = { source ->
                        backStack.add(AppRoute.Reader(source.url, source.name))
                    },
                )
            }
            entry<AppRoute.Reader> { route ->
                ReaderRoute(
                    sourceName = route.name.ifBlank { route.url },
                    sourceUrl = route.url,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
        transitionSpec = {
            forwardEnterTransition() togetherWith forwardExitTransition()
        },
        popTransitionSpec = {
            backwardEnterTransition() togetherWith backwardExitTransition()
        },
        predictivePopTransitionSpec = {
            backwardEnterTransition() togetherWith backwardExitTransition()
        },
    )
}

@Serializable
private sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data class Reader(
        val url: String,
        val name: String,
    ) : AppRoute
}

private fun forwardEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = PaywallReaderMotion.emphasizedOffsetSpring,
        initialOffsetX = { it / 4 },
    ) + fadeIn(animationSpec = PaywallReaderMotion.standardTween)

private fun forwardExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = PaywallReaderMotion.emphasizedOffsetSpring,
        targetOffsetX = { -it / 8 },
    ) + fadeOut(animationSpec = PaywallReaderMotion.standardTween)

private fun backwardEnterTransition(): EnterTransition =
    slideInHorizontally(
        animationSpec = PaywallReaderMotion.emphasizedOffsetSpring,
        initialOffsetX = { -it / 8 },
    ) + fadeIn(animationSpec = PaywallReaderMotion.standardTween)

private fun backwardExitTransition(): ExitTransition =
    slideOutHorizontally(
        animationSpec = PaywallReaderMotion.emphasizedOffsetSpring,
        targetOffsetX = { it / 4 },
    ) + fadeOut(animationSpec = PaywallReaderMotion.standardTween)
