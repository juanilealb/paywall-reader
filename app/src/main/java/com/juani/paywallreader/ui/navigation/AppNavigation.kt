package com.juani.paywallreader.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.juani.paywallreader.R
import com.juani.paywallreader.ui.home.HomeRoute
import com.juani.paywallreader.ui.reader.ReaderRoute
import com.juani.paywallreader.ui.theme.PaywallReaderMotion
import kotlinx.serialization.Serializable

private val FoldFloatingActionButtonSize = 64.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(AppRoute.Home)
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val listDetailDirective = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailPaneExpansionState = rememberPaneExpansionState(
        anchors = listOf(PaneExpansionAnchor.Proportion(0.33f)),
        initialAnchoredIndex = 0,
    )
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = listDetailDirective,
        paneExpansionState = listDetailPaneExpansionState,
    )
    var openReaderUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var addSourceRequest by rememberSaveable { mutableStateOf(0) }
    SideEffect {
        if (!view.isInEditMode) {
            WindowCompat.getInsetsController(
                (view.context as android.app.Activity).window,
                view,
            ).apply {
                isAppearanceLightStatusBars = openReaderUrl != null || !darkTheme
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (openReaderUrl != null) {
                    hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }
    val closeReader = {
        backStack.removeLastOrNull()
        openReaderUrl = backStack.filterIsInstance<AppRoute.Reader>().lastOrNull()?.url
        if (!view.isInEditMode) {
            WindowCompat.getInsetsController(
                (view.context as android.app.Activity).window,
                view,
            ).apply {
                if (openReaderUrl == null) {
                    show(WindowInsetsCompat.Type.statusBars())
                    isAppearanceLightStatusBars = !darkTheme
                } else {
                    hide(WindowInsetsCompat.Type.statusBars())
                    isAppearanceLightStatusBars = true
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMultiPaneWidth = maxWidth >= 600.dp

        NavDisplay(
            backStack = backStack,
            onBack = { closeReader() },
            sceneStrategies = listOf(listDetailStrategy),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<AppRoute.Home>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            ReaderPlaceholder(
                                showAddSourceFab = isMultiPaneWidth && openReaderUrl == null,
                                onAddSource = { addSourceRequest++ },
                            )
                        },
                    ),
                ) {
                    HomeRoute(
                        addSourceRequest = addSourceRequest,
                        selectedSourceUrl = openReaderUrl,
                        showAddSourceFab = !isMultiPaneWidth,
                        showBottomControls = isMultiPaneWidth || openReaderUrl == null,
                        onSourceClick = { source ->
                            openReaderUrl = source.url
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
                        onBack = { closeReader() },
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReaderPlaceholder(
    showAddSourceFab: Boolean,
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
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
            if (showAddSourceFab) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(WindowInsets.safeDrawing.asPaddingValues())
                        .padding(end = 16.dp, bottom = 8.dp)
                        .size(FoldFloatingActionButtonSize),
                    contentAlignment = Alignment.Center,
                ) {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = onAddSource,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_source),
                        )
                    }
                }
            }
        }
    }
}
