package com.juani.paywallreader.ui.home

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juani.paywallreader.R
import com.juani.paywallreader.domain.model.HistoryItem
import com.juani.paywallreader.domain.model.ReadingItem
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.ui.components.AddSourceSheet
import com.juani.paywallreader.ui.components.BrowserFavicon
import com.juani.paywallreader.ui.components.SourceCard
import com.juani.paywallreader.ui.theme.PaywallReaderTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeRoute(
    onSourceClick: (Source) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = homeViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val readLaterLabel = stringResource(R.string.read_later)
    val historyLabel = stringResource(R.string.history)

    HomeScreen(
        uiState = uiState,
        onSourceClick = onSourceClick,
        onReadingItemClick = { item ->
            onSourceClick(
                Source(
                    id = -item.id,
                    name = item.title,
                    url = item.url,
                    isDefault = false,
                    folderName = readLaterLabel,
                ),
            )
        },
        onHistoryItemClick = { item ->
            onSourceClick(
                Source(
                    id = -item.id,
                    name = item.title,
                    url = item.url,
                    isDefault = false,
                    folderName = historyLabel,
                ),
            )
        },
        onAddSource = viewModel::addSource,
        onDeleteSource = viewModel::deleteSource,
        onMarkRead = viewModel::markRead,
        existingUrls = uiState.sources.map { it.url }.toSet(),
        modifier = modifier,
    )
}

@Composable
private fun homeViewModel(): HomeViewModel {
    val application = LocalContext.current.applicationContext as Application
    return viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSourceClick: (Source) -> Unit,
    onReadingItemClick: (ReadingItem) -> Unit,
    onHistoryItemClick: (HistoryItem) -> Unit,
    onAddSource: (name: String, url: String, folderName: String) -> Unit,
    onDeleteSource: (Source) -> Unit,
    onMarkRead: (String) -> Unit,
    existingUrls: Set<String>,
    modifier: Modifier = Modifier,
) {
    var showAddSourceSheet by rememberSaveable { mutableStateOf(false) }
    var addSourceInitialUrl by rememberSaveable { mutableStateOf("") }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val folders = remember(uiState.sources) {
        uiState.sources.map { it.folderName }.distinct().sorted()
    }
    val visibleSources = remember(uiState.sources, selectedFolder) {
        selectedFolder?.let { folder -> uiState.sources.filter { it.folderName == folder } } ?: uiState.sources
    }

    BackHandler(fabMenuExpanded) {
        fabMenuExpanded = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = fabMenuExpanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                    ) {
                        Icon(
                            imageVector = if (fabMenuExpanded) Icons.Rounded.Close else Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_source),
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        addSourceInitialUrl = ""
                        showAddSourceSheet = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                        )
                    },
                    text = { Text(stringResource(R.string.add_source)) },
                )
                FloatingActionButtonMenuItem(
                    onClick = {
                        fabMenuExpanded = false
                        addSourceInitialUrl = clipboardManager.getText()?.text.orEmpty()
                        showAddSourceSheet = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                        )
                    },
                    text = { Text(stringResource(R.string.add_source_from_clipboard)) },
                )
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
        ) {
            val layoutSpec = rememberHomeLayoutSpec(maxWidth)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = layoutSpec.horizontalPadding,
                    top = layoutSpec.topPadding,
                    end = layoutSpec.horizontalPadding,
                    bottom = layoutSpec.bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(layoutSpec.itemSpacing),
            ) {
                item {
                    HomeHeader(compact = maxWidth < 480.dp)
                }

                if (folders.isNotEmpty()) {
                    item {
                        FolderChips(
                            folders = folders,
                            selectedFolder = selectedFolder,
                            onFolderSelected = { selectedFolder = it },
                        )
                    }
                }

                if (uiState.readingItems.isNotEmpty()) {
                    item {
                        SectionHeader(
                            icon = Icons.Rounded.Bookmark,
                            title = stringResource(R.string.read_later),
                            count = uiState.readingItems.size,
                        )
                    }
                    items(
                        items = uiState.readingItems,
                        key = { "read-${it.id}" },
                    ) { item ->
                        ReadingListItem(
                            item = item,
                            onClick = { onReadingItemClick(item) },
                            onMarkRead = { onMarkRead(item.url) },
                        )
                    }
                }

                if (visibleSources.isEmpty() && uiState.readingItems.isEmpty()) {
                    item {
                        EmptySources(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = layoutSpec.emptyStateTopPadding),
                        )
                    }
                } else {
                    if (visibleSources.isNotEmpty()) {
                        item {
                            SectionHeader(
                                icon = Icons.Rounded.Folder,
                                title = selectedFolder ?: stringResource(R.string.all_sources),
                                count = visibleSources.size,
                            )
                        }
                    }
                    items(
                        items = visibleSources,
                        key = { "source-${it.id}" },
                    ) { source ->
                        SourceCard(
                            source = source,
                            onClick = onSourceClick,
                            onDelete = onDeleteSource,
                            deleteLabel = stringResource(R.string.delete),
                        )
                    }
                }

                if (uiState.historyItems.isNotEmpty()) {
                    item {
                        SectionHeader(
                            icon = Icons.Rounded.History,
                            title = stringResource(R.string.history),
                            count = uiState.historyItems.size,
                        )
                    }
                    items(
                        items = uiState.historyItems.take(12),
                        key = { "history-${it.id}" },
                    ) { item ->
                        HistoryListItem(
                            item = item,
                            onClick = { onHistoryItemClick(item) },
                        )
                    }
                }
            }
        }
    }

    if (showAddSourceSheet) {
        AddSourceSheet(
            onSave = onAddSource,
            onSaveAndOpen = { name, url, folderName ->
                onAddSource(name, url, folderName)
                onSourceClick(
                    Source(
                        id = 0,
                        name = name,
                        url = url,
                        isDefault = false,
                        folderName = folderName,
                    ),
                )
            },
            onDismiss = { showAddSourceSheet = false },
            existingUrls = existingUrls,
            initialUrl = addSourceInitialUrl,
        )
    }
}

@Composable
private fun FolderChips(
    folders: List<String>,
    selectedFolder: String?,
    onFolderSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedFolder == null,
            onClick = { onFolderSelected(null) },
            label = { Text(stringResource(R.string.all_sources)) },
        )
        folders.take(3).forEach { folder ->
            FilterChip(
                selected = selectedFolder == folder,
                onClick = { onFolderSelected(folder) },
                label = { Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Folder, contentDescription = null)
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReadingListItem(
    item: ReadingItem,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f),
    ) {
        ListItem(
            headlineContent = {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(item.url.toDisplayHost(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                BrowserFavicon(url = item.url, fallbackText = item.title)
            },
            trailingContent = {
                androidx.compose.material3.IconButton(onClick = onMarkRead) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.mark_read),
                    )
                }
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}

@Composable
private fun HistoryListItem(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        ListItem(
            headlineContent = {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(item.url.toDisplayHost(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                BrowserFavicon(url = item.url, fallbackText = item.title)
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}

private data class HomeLayoutSpec(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val itemSpacing: Dp,
    val emptyStateTopPadding: Dp,
)

private fun rememberHomeLayoutSpec(maxWidth: Dp): HomeLayoutSpec =
    when {
        maxWidth >= 1_200.dp -> HomeLayoutSpec(
            horizontalPadding = 40.dp,
            topPadding = 32.dp,
            bottomPadding = 112.dp,
            itemSpacing = 10.dp,
            emptyStateTopPadding = 96.dp,
        )

        maxWidth >= 840.dp -> HomeLayoutSpec(
            horizontalPadding = 20.dp,
            topPadding = 28.dp,
            bottomPadding = 108.dp,
            itemSpacing = 10.dp,
            emptyStateTopPadding = 72.dp,
        )

        maxWidth >= 600.dp -> HomeLayoutSpec(
            horizontalPadding = 20.dp,
            topPadding = 24.dp,
            bottomPadding = 104.dp,
            itemSpacing = 10.dp,
            emptyStateTopPadding = 64.dp,
        )

        else -> HomeLayoutSpec(
            horizontalPadding = 18.dp,
            topPadding = 18.dp,
            bottomPadding = 104.dp,
            itemSpacing = 10.dp,
            emptyStateTopPadding = 48.dp,
        )
    }

@Composable
private fun HomeHeader(
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = if (compact) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.displaySmall
            },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptySources(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 6.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp),
                )
            }
            Text(
                text = stringResource(R.string.empty_sources),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.empty_sources_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
private annotation class HomeFormFactorPreviews

@HomeFormFactorPreviews
@Composable
private fun HomeScreenPreview() {
    PaywallReaderTheme {
        HomeScreen(
            uiState = HomeUiState(
                sources = listOf(
                    Source(
                        id = 1,
                        name = "Example",
                        url = "https://example.com",
                        isDefault = false,
                    ),
                    Source(
                        id = 2,
                        name = "News",
                        url = "https://news.example.com",
                        isDefault = false,
                    ),
                ),
            ),
            onSourceClick = {},
            onReadingItemClick = {},
            onHistoryItemClick = {},
            onAddSource = { _, _, _ -> },
            onDeleteSource = {},
            onMarkRead = {},
            existingUrls = emptySet(),
        )
    }
}

private fun String.toDisplayHost(): String =
    removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .removePrefix("www.")
