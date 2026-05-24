package com.juani.paywallreader.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.juani.paywallreader.R
import com.juani.paywallreader.ui.home.HomeRoute
import com.juani.paywallreader.ui.reader.ReaderRoute
import com.juani.paywallreader.ui.theme.PaywallReaderMotion
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(AppRoute.Home)
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val listDetailDirective = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = listDetailDirective,
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMultiPaneWidth = maxWidth >= 840.dp

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<AppRoute.Home>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            ReaderPlaceholder()
                        },
                    ),
                ) {
                    HomeRoute(
                        onSourceClick = { source ->
                            backStack.add(AppRoute.Reader(source.url, source.name))
                        },
                    )
                }
                entry<AppRoute.Reader>(
                    metadata = ListDetailSceneStrategy.detailPane(),
                ) { route ->
                    ReaderRoute(
                        sourceName = route.name.ifBlank { route.url },
                        sourceUrl = route.url,
                        onBack = { backStack.removeLastOrNull() },
                        showBackButton = !isMultiPaneWidth,
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

@Composable
private fun ReaderPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Article,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp),
                )
            }
            Text(
                text = stringResource(R.string.reader_placeholder_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.reader_placeholder_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
