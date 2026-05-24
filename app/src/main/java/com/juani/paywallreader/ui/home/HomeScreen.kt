package com.juani.paywallreader.ui.home

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import java.util.Calendar
import kotlinx.coroutines.launch

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
        onClearHistory = viewModel::clearHistory,
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
    onClearHistory: () -> Unit,
    existingUrls: Set<String>,
    modifier: Modifier = Modifier,
) {
    var showAddSourceSheet by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryConfirmation by rememberSaveable { mutableStateOf(false) }
    var addSourceInitialUrl by rememberSaveable { mutableStateOf("") }
    var addSourceInitialFolder by rememberSaveable { mutableStateOf("News") }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf(HomeSection.Sources) }
    var readLaterFilter by rememberSaveable { mutableStateOf(ReadLaterFilter.All) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val folders = remember(uiState.sources) {
        uiState.sources.map { it.folderName }.distinct().sorted()
    }
    val normalizedSearch = searchQuery.trim()
    val visibleSources = remember(uiState.sources, selectedFolder, normalizedSearch) {
        uiState.sources
            .asSequence()
            .filter { source -> selectedFolder == null || source.folderName == selectedFolder }
            .filter { source ->
                normalizedSearch.isBlank() ||
                    source.name.contains(normalizedSearch, ignoreCase = true) ||
                    source.url.toDisplayHost().contains(normalizedSearch, ignoreCase = true)
            }
            .toList()
    }
    val visibleReadingItems = remember(uiState.readingItems, normalizedSearch, readLaterFilter) {
        uiState.readingItems
            .asSequence()
            .filter { item -> readLaterFilter.matches(item) }
            .filter { item ->
                normalizedSearch.isBlank() ||
                    item.title.contains(normalizedSearch, ignoreCase = true) ||
                    item.sourceName.contains(normalizedSearch, ignoreCase = true) ||
                    item.url.toDisplayHost().contains(normalizedSearch, ignoreCase = true)
            }
            .toList()
    }
    val visibleHistoryItems = remember(uiState.historyItems, normalizedSearch) {
        uiState.historyItems.filter { item ->
            normalizedSearch.isBlank() ||
                item.title.contains(normalizedSearch, ignoreCase = true) ||
                item.sourceName.contains(normalizedSearch, ignoreCase = true) ||
                item.url.toDisplayHost().contains(normalizedSearch, ignoreCase = true)
        }
    }
    val visibleHistoryGroups = remember(visibleHistoryItems) {
        visibleHistoryItems.take(30).groupByVisitBucket()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues),
        ) {
            val layoutSpec = rememberHomeLayoutSpec(maxWidth)
            val compactHeader = maxWidth < 480.dp
            val showSearch = when (selectedSection) {
                HomeSection.Sources -> true
                HomeSection.ReadLater -> uiState.readingItems.isNotEmpty()
                HomeSection.History -> uiState.historyItems.isNotEmpty()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = layoutSpec.horizontalPadding,
                    top = layoutSpec.topPadding,
                    end = layoutSpec.horizontalPadding,
                    bottom = layoutSpec.bottomPadding + 86.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(layoutSpec.itemSpacing),
            ) {
                item {
                    HomeHeader(
                        title = selectedSection.title,
                        subtitle = selectedSection.subtitle(uiState),
                        compact = compactHeader,
                        action = if (selectedSection == HomeSection.History && uiState.historyItems.isNotEmpty()) {
                            {
                                IconButton(onClick = { showClearHistoryConfirmation = true }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.clear_history),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }

                if (showSearch) {
                    item {
                        SearchPill(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = selectedSection.searchPlaceholder,
                        )
                    }
                }

                when (selectedSection) {
                    HomeSection.Sources -> {
                        if (folders.isNotEmpty()) {
                            item {
                                FolderChips(
                                    folders = folders,
                                    selectedFolder = selectedFolder,
                                    onFolderSelected = { selectedFolder = it },
                                    onNewFolder = {
                                        addSourceInitialUrl = ""
                                        addSourceInitialFolder = ""
                                        showAddSourceSheet = true
                                    },
                                )
                            }
                        }

                        if (visibleSources.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Rounded.Folder,
                                    title = if (searchQuery.isBlank()) {
                                        stringResource(R.string.empty_sources)
                                    } else {
                                        stringResource(R.string.empty_search_title)
                                    },
                                    body = if (searchQuery.isBlank()) {
                                        stringResource(R.string.empty_sources_subtitle)
                                    } else {
                                        stringResource(R.string.empty_search_subtitle)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = layoutSpec.emptyStateTopPadding),
                                )
                            }
                        } else {
                            item {
                                SectionHeader(
                                    title = selectedFolder ?: stringResource(R.string.all_sources),
                                    count = visibleSources.size,
                                )
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
                    }

                    HomeSection.ReadLater -> {
                        if (uiState.readingItems.isNotEmpty()) {
                            item {
                                ReadLaterFilterChips(
                                    selectedFilter = readLaterFilter,
                                    onFilterSelected = { readLaterFilter = it },
                                )
                            }
                        }

                        if (visibleReadingItems.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = if (uiState.readingItems.isEmpty()) {
                                        Icons.Rounded.Bookmark
                                    } else {
                                        Icons.Rounded.Search
                                    },
                                    title = if (uiState.readingItems.isEmpty()) {
                                        stringResource(R.string.empty_read_later)
                                    } else {
                                        stringResource(R.string.empty_search_title)
                                    },
                                    body = if (uiState.readingItems.isEmpty()) {
                                        stringResource(R.string.empty_read_later_subtitle)
                                    } else {
                                        stringResource(R.string.empty_search_subtitle)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = layoutSpec.emptyStateTopPadding),
                                )
                            }
                        } else {
                            item {
                                SectionHeader(
                                    title = stringResource(R.string.latest_saved),
                                    count = 1,
                                )
                            }
                            item {
                                ReadLaterHero(
                                    item = visibleReadingItems.first(),
                                    onClick = { onReadingItemClick(visibleReadingItems.first()) },
                                    onMarkRead = { onMarkRead(visibleReadingItems.first().url) },
                                )
                            }
                            val rest = visibleReadingItems.drop(1)
                            if (rest.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = stringResource(R.string.more_articles),
                                        count = rest.size,
                                    )
                                }
                                item {
                                    ReadingListGroup(
                                        items = rest,
                                        onItemClick = onReadingItemClick,
                                        onMarkRead = { item -> onMarkRead(item.url) },
                                    )
                                }
                            }
                        }
                    }

                    HomeSection.History -> {
                        if (visibleHistoryItems.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Rounded.History,
                                    title = stringResource(R.string.empty_history),
                                    body = stringResource(R.string.empty_history_subtitle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = layoutSpec.emptyStateTopPadding),
                                )
                            }
                        } else {
                            visibleHistoryGroups.forEach { group ->
                                item(key = "history-header-${group.title}") {
                                    SectionHeader(
                                        title = group.title,
                                        count = group.items.size,
                                    )
                                }
                                item(key = "history-group-${group.title}") {
                                    HistoryListGroup(
                                        items = group.items,
                                        onItemClick = onHistoryItemClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedSection == HomeSection.Sources) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val clipboardText = clipboard
                                .getClipEntry()
                                ?.clipData
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                            addSourceInitialUrl = clipboardText?.takeIf { it.looksLikeUrl() }.orEmpty()
                            addSourceInitialFolder = selectedFolder ?: "News"
                            showAddSourceSheet = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(end = layoutSpec.horizontalPadding, bottom = 82.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_source),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp,
            ) {
                SectionSelector(
                    selectedSection = selectedSection,
                    onSectionSelected = {
                        selectedSection = it
                        if (it != HomeSection.ReadLater) {
                            readLaterFilter = ReadLaterFilter.All
                        }
                        searchQuery = ""
                    },
                    readingCount = uiState.readingItems.size,
                    historyCount = uiState.historyItems.size,
                    modifier = Modifier.padding(
                        start = layoutSpec.horizontalPadding,
                        top = 10.dp,
                        end = layoutSpec.horizontalPadding,
                        bottom = 10.dp,
                    ),
                )
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
            initialFolderName = addSourceInitialFolder,
            existingFolders = folders,
        )
    }

    if (showClearHistoryConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirmation = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryConfirmation = false
                        onClearHistory()
                    },
                ) {
                    Text(stringResource(R.string.clear_history))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private enum class HomeSection(
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
    val searchPlaceholder: String,
) {
    Sources(
        title = "Fuentes",
        shortTitle = "Webs",
        icon = Icons.Rounded.Newspaper,
        searchPlaceholder = "Buscar fuente",
    ),
    ReadLater(
        title = "Para leer",
        shortTitle = "Leer",
        icon = Icons.Rounded.Bookmark,
        searchPlaceholder = "Buscar lectura",
    ),
    History(
        title = "Historial",
        shortTitle = "Hist.",
        icon = Icons.Rounded.History,
        searchPlaceholder = "Buscar historial",
    );

    fun subtitle(uiState: HomeUiState): String =
        when (this) {
            Sources -> "${uiState.sources.size} sitios guardados"
            ReadLater -> "${uiState.readingItems.size} artículos guardados"
            History -> "Últimos ${uiState.historyItems.size} artículos"
        }
}

private enum class ReadLaterFilter(
    val label: String,
) {
    All("Todo"),
    Today("De hoy");

    fun matches(item: ReadingItem): Boolean =
        when (this) {
            All -> true
            Today -> item.addedAt >= Calendar.getInstance().startOfDayMillis()
        }
}

@Composable
private fun SectionSelector(
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    readingCount: Int,
    historyCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeSection.entries.forEach { section ->
            val selected = selectedSection == section
            val count = when (section) {
                HomeSection.Sources -> 0
                HomeSection.ReadLater -> readingCount
                HomeSection.History -> historyCount
            }
            Surface(
                onClick = { onSectionSelected(section) },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                tonalElevation = if (selected) 2.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                        Text(
                            text = if (count > 0) {
                                "${section.shortTitle} ${count.coerceAtMost(99)}"
                            } else {
                                section.shortTitle
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadLaterFilterChips(
    selectedFilter: ReadLaterFilter,
    onFilterSelected: (ReadLaterFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = ReadLaterFilter.entries.toList(), key = { it.name }) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun FolderChips(
    folders: List<String>,
    selectedFolder: String?,
    onFolderSelected: (String?) -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedFolder == null,
                onClick = { onFolderSelected(null) },
                label = { Text(stringResource(R.string.all_sources)) },
            )
        }
        items(folders, key = { it }) { folder ->
            FilterChip(
                selected = selectedFolder == folder,
                onClick = { onFolderSelected(folder) },
                label = { Text(folder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    FolderDot(folderName = folder)
                },
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = onNewFolder,
                label = { Text(stringResource(R.string.new_folder)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                },
            )
        }
    }
}

@Composable
private fun FolderDot(
    folderName: String,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
    )
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(colors[kotlin.math.abs(folderName.hashCode()) % colors.size]),
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReadLaterHero(
    item: ReadingItem,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrowserFavicon(url = item.url, fallbackText = item.sourceName)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.sourceName.ifBlank { item.url.toDisplayHost() },
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.url.toDisplayHost(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onMarkRead) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.mark_read),
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
                Text(
                    text = item.addedAt.toRelativeTimeLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ReadingListGroup(
    items: List<ReadingItem>,
    onItemClick: (ReadingItem) -> Unit,
    onMarkRead: (ReadingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    GroupedListSurface(modifier = modifier) {
        items.forEachIndexed { index, item ->
            ReadingListItem(
                item = item,
                onClick = { onItemClick(item) },
                onMarkRead = { onMarkRead(item) },
            )
            if (index < items.lastIndex) {
                GroupDivider()
            }
        }
    }
}

@Composable
private fun HistoryListGroup(
    items: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    GroupedListSurface(modifier = modifier) {
        items.forEachIndexed { index, item ->
            HistoryListItem(
                item = item,
                onClick = { onItemClick(item) },
            )
            if (index < items.lastIndex) {
                GroupDivider()
            }
        }
    }
}

@Composable
private fun GroupedListSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 62.dp, end = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun ReadingListItem(
    item: ReadingItem,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = "${item.sourceName.ifBlank { item.url.toDisplayHost() }} · ${item.url.toDisplayHost()}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            BrowserFavicon(url = item.url, fallbackText = item.title)
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.addedAt.toRelativeTimeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                IconButton(onClick = onMarkRead) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.mark_read),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun HistoryListItem(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = "${item.sourceName.ifBlank { item.url.toDisplayHost() }} · ${item.url.toDisplayHost()}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            BrowserFavicon(
                url = item.url,
                fallbackText = item.title,
                modifier = Modifier.size(32.dp),
            )
        },
        trailingContent = {
            Text(
                text = item.visitedAt.toRelativeTimeLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

private data class HomeLayoutSpec(
    val horizontalPadding: Dp,
    val topPadding: Dp,
    val bottomPadding: Dp,
    val itemSpacing: Dp,
    val emptyStateTopPadding: Dp,
)

private data class HistoryGroup(
    val title: String,
    val items: List<HistoryItem>,
)

private fun List<HistoryItem>.groupByVisitBucket(): List<HistoryGroup> {
    val todayStart = Calendar.getInstance().startOfDayMillis()
    val yesterdayStart = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }.startOfDayMillis()

    return listOf(
        HistoryGroup("Hoy", filter { it.visitedAt >= todayStart }),
        HistoryGroup("Ayer", filter { it.visitedAt in yesterdayStart until todayStart }),
        HistoryGroup("Anteriores", filter { it.visitedAt < yesterdayStart }),
    ).filter { it.items.isNotEmpty() }
}

private fun Calendar.startOfDayMillis(): Long =
    apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun Long.toRelativeTimeLabel(now: Long = System.currentTimeMillis()): String {
    val elapsedMinutes = ((now - this).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        elapsedMinutes < 1 -> "Ahora"
        elapsedMinutes < 60 -> "${elapsedMinutes}m"
        elapsedMinutes < 24 * 60 -> "${elapsedMinutes / 60}h"
        elapsedMinutes < 7 * 24 * 60 -> "${elapsedMinutes / (24 * 60)}d"
        else -> "${elapsedMinutes / (7 * 24 * 60)}sem"
    }
}

private fun rememberHomeLayoutSpec(maxWidth: Dp): HomeLayoutSpec =
    when {
        maxWidth >= 1_200.dp -> HomeLayoutSpec(
            horizontalPadding = 28.dp,
            topPadding = 28.dp,
            bottomPadding = 112.dp,
            itemSpacing = 10.dp,
            emptyStateTopPadding = 80.dp,
        )

        maxWidth >= 840.dp -> HomeLayoutSpec(
            horizontalPadding = 18.dp,
            topPadding = 24.dp,
            bottomPadding = 108.dp,
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
    title: String,
    subtitle: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = if (compact) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            action?.invoke()
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
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
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun String.toDisplayHost(): String =
    removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .removePrefix("www.")

private fun String.looksLikeUrl(): Boolean =
    trim().let { text ->
        text.startsWith("http://") ||
            text.startsWith("https://") ||
            ('.' in text && ' ' !in text)
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
                readingItems = listOf(
                    ReadingItem(
                        id = 1,
                        title = "A saved article with a longer headline",
                        url = "https://example.com/article",
                        sourceName = "Example",
                        addedAt = 1,
                    ),
                ),
            ),
            onSourceClick = {},
            onReadingItemClick = {},
            onHistoryItemClick = {},
            onAddSource = { _, _, _ -> },
            onDeleteSource = {},
            onMarkRead = {},
            onClearHistory = {},
            existingUrls = emptySet(),
        )
    }
}
