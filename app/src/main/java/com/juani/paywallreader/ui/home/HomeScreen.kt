package com.juani.paywallreader.ui.home

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juani.paywallreader.R
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_CAPTURING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_FAILED
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_PENDING
import com.juani.paywallreader.domain.model.CAPTURE_STATUS_READY
import com.juani.paywallreader.domain.model.HistoryItem
import com.juani.paywallreader.domain.model.ReadingItem
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.domain.model.UNFILED_FOLDER_NAME
import com.juani.paywallreader.data.reader.toPreferredReaderUrl
import com.juani.paywallreader.ui.components.AddSourceSheet
import com.juani.paywallreader.ui.components.BrowserFavicon
import com.juani.paywallreader.ui.components.SourceCard
import com.juani.paywallreader.ui.navigation.ExternalShareRoutePolicy
import com.juani.paywallreader.ui.reader.HeadlessArticleCaptureHost
import com.juani.paywallreader.ui.theme.PaywallReaderTheme
import java.util.Calendar
import kotlinx.coroutines.launch

private val HomeBottomActionSize = 64.dp

@Composable
fun HomeRoute(
    onSourceClick: (Source) -> Unit,
    modifier: Modifier = Modifier,
    addSourceRequest: Int = 0,
    pendingSharedUrl: String? = null,
    selectedSourceUrl: String? = null,
    showAddSourceFab: Boolean = true,
    showBottomControls: Boolean = true,
    viewModel: HomeViewModel = homeViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val readLaterLabel = stringResource(R.string.read_later)
    val historyLabel = stringResource(R.string.history)
    val context = LocalContext.current
    var consumedSharedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var openReadLaterRequest by rememberSaveable { mutableStateOf(0) }
    var cachedArticleToRead by remember { mutableStateOf<ReadingItem?>(null) }
    var headlessCaptureUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showReaderMoveBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    var readerMoveBookmarkNewFolder by rememberSaveable { mutableStateOf("") }
    val readerFolders = remember(uiState.folders, uiState.readingItems) {
        val hasUnfiledItems = uiState.readingItems.any { it.folderName == UNFILED_FOLDER_NAME }
        (listOf(UNFILED_FOLDER_NAME) + uiState.folders + uiState.readingItems.map { it.folderName })
            .distinct()
            .filter { it.isNotBlank() }
            .filterNot { it == UNFILED_FOLDER_NAME && !hasUnfiledItems }
            .sortedBy { it.lowercase() }
    }

    fun nextReadableAfter(current: ReadingItem): ReadingItem? {
        val currentIndex = uiState.readingItems.indexOfFirst { it.url == current.url }
        val candidates = uiState.readingItems
            .mapIndexed { index, readingItem -> index to readingItem }
            .filter { (_, readingItem) ->
                readingItem.url != current.url &&
                    !readingItem.isRead &&
                    readingItem.captureStatus == CAPTURE_STATUS_READY &&
                    readingItem.hasCapturedBody()
            }
        return candidates.firstOrNull { (index, _) -> currentIndex >= 0 && index > currentIndex }?.second
            ?: candidates.firstOrNull()?.second
    }

    LaunchedEffect(pendingSharedUrl) {
        val decision = ExternalShareRoutePolicy.decide(pendingSharedUrl)
        val url = decision.captureUrl
        if (url != null && url != consumedSharedUrl) {
            consumedSharedUrl = url
            viewModel.markSharedUrlPending(url)
            headlessCaptureUrl = url
            openReadLaterRequest++
            Toast.makeText(context, "Capturando en segundo plano", Toast.LENGTH_SHORT).show()
        }
    }

    HomeScreen(
        uiState = uiState,
        onSourceClick = onSourceClick,
        onReadingItemClick = { item ->
            if (item.captureStatus != CAPTURE_STATUS_READY) {
                viewModel.retryCapture(item.url)
                openReadLaterRequest++
                Toast.makeText(context, "Reintentando extracción en background", Toast.LENGTH_SHORT).show()
            } else if (item.hasCapturedBody()) {
                cachedArticleToRead = item
            } else {
                onSourceClick(
                    Source(
                        id = -item.id,
                        name = item.title,
                        url = item.url.toPreferredReaderUrl(fallbackToOriginal = false),
                        isDefault = false,
                        folderName = readLaterLabel,
                    ),
                )
            }
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
        onSetRead = { item, isRead -> viewModel.setRead(item.url, isRead) },
        onArchiveItem = { item -> viewModel.archiveBookmark(item.url) },
        onRestoreItem = { item -> viewModel.restoreBookmark(item.url) },
        onMoveBookmarkToFolder = { item, folderName ->
            viewModel.moveBookmarkToFolder(item.url, folderName)
            Toast.makeText(context, "Movido a $folderName", Toast.LENGTH_SHORT).show()
        },
        onRetryCapture = { item ->
            viewModel.retryCapture(item.url)
            Toast.makeText(context, "Reintento agregado a la cola", Toast.LENGTH_SHORT).show()
        },
        onClearHistory = viewModel::clearHistory,
        onCreateFolder = viewModel::createFolder,
        onDeleteFolder = viewModel::deleteFolder,
        onUpdateSource = viewModel::updateSource,
        existingUrls = uiState.sources.map { it.url }.toSet(),
        addSourceRequest = addSourceRequest,
        openReadLaterRequest = openReadLaterRequest,
        selectedSourceUrl = selectedSourceUrl,
        showAddSourceFab = showAddSourceFab,
        showBottomControls = showBottomControls,
        modifier = modifier,
    )

    cachedArticleToRead?.let { item ->
        OfflineArticleReaderScreen(
            item = item,
            onBack = { cachedArticleToRead = null },
            onOpenWeb = {
                cachedArticleToRead = null
                onSourceClick(
                    Source(
                        id = -item.id,
                        name = item.title,
                        url = item.url.toPreferredReaderUrl(fallbackToOriginal = false),
                        isDefault = false,
                        folderName = readLaterLabel,
                    ),
                )
            },
            onMarkRead = {
                viewModel.markRead(item.url)
                cachedArticleToRead = nextReadableAfter(item)
            },
            onArchiveAndNext = {
                viewModel.archiveBookmark(item.url)
                cachedArticleToRead = nextReadableAfter(item)
            },
            onMoveToFolderAndNext = {
                readerMoveBookmarkNewFolder = ""
                showReaderMoveBookmarkDialog = true
            },
            onExitToMenu = { cachedArticleToRead = null },
        )
    }

    if (showReaderMoveBookmarkDialog) {
        val item = cachedArticleToRead
        if (item != null) {
            MoveBookmarkDialog(
                item = item,
                folders = readerFolders,
                newFolderName = readerMoveBookmarkNewFolder,
                onNewFolderNameChange = { readerMoveBookmarkNewFolder = it },
                onMove = { folderName ->
                    viewModel.moveBookmarkToFolder(item.url, folderName)
                    Toast.makeText(context, "Movido a $folderName", Toast.LENGTH_SHORT).show()
                    showReaderMoveBookmarkDialog = false
                    readerMoveBookmarkNewFolder = ""
                    cachedArticleToRead = nextReadableAfter(item)
                },
                onDismiss = { showReaderMoveBookmarkDialog = false },
            )
        } else {
            showReaderMoveBookmarkDialog = false
        }
    }
    HeadlessArticleCaptureHost(
        captureUrl = headlessCaptureUrl,
        onSaveForLater = { title, url, sourceName, resolvedUrl, author, excerpt, html, text, markdown, imageUrl, captureProvider ->
            viewModel.saveForLater(
                title = title,
                url = url,
                sourceName = sourceName,
                resolvedUrl = resolvedUrl,
                author = author,
                excerpt = excerpt,
                html = html,
                text = text,
                markdown = markdown,
                imageUrl = imageUrl,
                captureProvider = captureProvider,
            )
        },
        onCaptureStatusChange = viewModel::updateCaptureStatus,
        onCaptureComplete = { completedUrl ->
            if (completedUrl == headlessCaptureUrl || completedUrl.isNotBlank()) {
                headlessCaptureUrl = null
            }
        },
    )

}

@Composable
private fun homeViewModel(): HomeViewModel {
    val application = LocalContext.current.applicationContext as Application
    return viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSourceClick: (Source) -> Unit,
    onReadingItemClick: (ReadingItem) -> Unit,
    onHistoryItemClick: (HistoryItem) -> Unit,
    onAddSource: (name: String, url: String, folderName: String) -> Unit,
    onDeleteSource: (Source) -> Unit,
    onMarkRead: (String) -> Unit,
    onSetRead: (ReadingItem, Boolean) -> Unit,
    onArchiveItem: (ReadingItem) -> Unit,
    onRestoreItem: (ReadingItem) -> Unit,
    onMoveBookmarkToFolder: (ReadingItem, String) -> Unit,
    onRetryCapture: (ReadingItem) -> Unit,
    onClearHistory: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onUpdateSource: (Source, String, String, String) -> Unit,
    existingUrls: Set<String>,
    addSourceRequest: Int = 0,
    openReadLaterRequest: Int = 0,
    selectedSourceUrl: String? = null,
    showAddSourceFab: Boolean = true,
    showBottomControls: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showAddSourceSheet by rememberSaveable { mutableStateOf(false) }
    var showEditSourceSheet by rememberSaveable { mutableStateOf(false) }
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteFolderConfirmation by rememberSaveable { mutableStateOf(false) }
    var showClearHistoryConfirmation by rememberSaveable { mutableStateOf(false) }
    var addSourceInitialUrl by rememberSaveable { mutableStateOf("") }
    var addSourceInitialFolder by rememberSaveable { mutableStateOf(UNFILED_FOLDER_NAME) }
    var sourceToEdit by remember { mutableStateOf<Source?>(null) }
    var newFolderName by rememberSaveable { mutableStateOf("") }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSection by rememberSaveable { mutableStateOf(HomeSection.Sources) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showUndoSnackbar(message: String, undo: () -> Unit) {
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Deshacer",
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) {
                undo()
            }
        }
    }

    fun archiveWithUndo(item: ReadingItem) {
        onArchiveItem(item)
        showUndoSnackbar("Archivado", undo = { onRestoreItem(item) })
    }

    fun toggleReadWithUndo(item: ReadingItem) {
        val newReadState = !item.isRead
        onSetRead(item, newReadState)
        showUndoSnackbar(
            message = if (newReadState) "Marcado como leído" else "Marcado como no leído",
            undo = { onSetRead(item, item.isRead) },
        )
    }

    val folders = remember(uiState.folders, uiState.sources, uiState.readingItems) {
        val hasUnfiledItems = uiState.sources.any { it.folderName == UNFILED_FOLDER_NAME } ||
            uiState.readingItems.any { it.folderName == UNFILED_FOLDER_NAME }
        (uiState.folders + uiState.sources.map { it.folderName } + uiState.readingItems.map { it.folderName })
            .distinct()
            .filterNot { it == UNFILED_FOLDER_NAME && !hasUnfiledItems }
            .sortedBy { it.lowercase() }
    }
    val readingFolders = remember(uiState.folders, uiState.readingItems) {
        val hasUnfiledItems = uiState.readingItems.any { it.folderName == UNFILED_FOLDER_NAME }
        (uiState.folders + uiState.readingItems.map { it.folderName })
            .distinct()
            .filterNot { it == UNFILED_FOLDER_NAME && !hasUnfiledItems }
            .sortedBy { it.lowercase() }
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
    val visibleReadingItems = remember(uiState.readingItems, selectedFolder, normalizedSearch) {
        uiState.readingItems
            .asSequence()
            .filter { item ->
                !item.isRead &&
                    if (selectedFolder == null) {
                        item.folderName == UNFILED_FOLDER_NAME
                    } else {
                        item.folderName == selectedFolder
                    }
            }
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

    LaunchedEffect(addSourceRequest) {
        if (addSourceRequest > 0) {
            addSourceInitialUrl = ""
            addSourceInitialFolder = selectedFolder ?: UNFILED_FOLDER_NAME
            showAddSourceSheet = true
        }
    }

    LaunchedEffect(openReadLaterRequest) {
        if (openReadLaterRequest > 0) {
            selectedSection = HomeSection.ReadLater
            selectedFolder = null
            searchQuery = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    bottom = layoutSpec.bottomPadding + if (showBottomControls) 86.dp else 0.dp,
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
                                        newFolderName = ""
                                        showNewFolderDialog = true
                                    },
                                )
                            }
                        }

                        item {
                            SectionHeader(
                                title = selectedFolder ?: stringResource(R.string.all_sources),
                                count = visibleSources.size,
                                action = selectedFolder?.takeIf { it != UNFILED_FOLDER_NAME }?.let { folder ->
                                    {
                                        IconButton(onClick = { showDeleteFolderConfirmation = true }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = stringResource(R.string.delete_folder),
                                            )
                                        }
                                    }
                                },
                            )
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
                            items(
                                items = visibleSources,
                                key = { "source-${it.id}" },
                            ) { source ->
                                SourceCard(
                                    source = source,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onSourceClick(it)
                                    },
                                    onDelete = onDeleteSource,
                                    onEdit = {
                                        sourceToEdit = it
                                        showEditSourceSheet = true
                                    },
                                    deleteLabel = stringResource(R.string.delete),
                                    editLabel = stringResource(R.string.edit),
                                    selected = source.url == selectedSourceUrl,
                                )
                            }
                        }
                    }

                    HomeSection.ReadLater -> {
                        if (readingFolders.isNotEmpty() || uiState.readingItems.isNotEmpty()) {
                            item {
                                FolderChips(
                                    folders = readingFolders,
                                    selectedFolder = selectedFolder,
                                    allLabel = "Inbox",
                                    onFolderSelected = { selectedFolder = it },
                                    onNewFolder = {
                                        newFolderName = ""
                                        showNewFolderDialog = true
                                    },
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
                                    title = selectedFolder ?: "Inbox",
                                    count = visibleReadingItems.size,
                                )
                            }
                            item {
                                ReadingListGroup(
                                    items = visibleReadingItems,
                                    onItemClick = {
                                        focusManager.clearFocus()
                                        onReadingItemClick(it)
                                    },
                                    onArchive = { item -> archiveWithUndo(item) },
                                    onToggleRead = { item -> toggleReadWithUndo(item) },
                                    onRetryCapture = onRetryCapture,
                                )
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
                                        onItemClick = {
                                            focusManager.clearFocus()
                                            onHistoryItemClick(it)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showBottomControls) {
                BottomHomeControls(
                    selectedSection = selectedSection,
                    onSectionSelected = {
                        focusManager.clearFocus()
                        selectedSection = it
                        searchQuery = ""
                    },
                    readingCount = uiState.readingItems.size,
                    historyCount = uiState.historyItems.size,
                    showAddSourceAction = showAddSourceFab,
                    onAddSource = {
                        focusManager.clearFocus()
                        coroutineScope.launch {
                            val clipboardText = clipboard
                                .getClipEntry()
                                ?.clipData
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                            addSourceInitialUrl = clipboardText?.takeIf { it.looksLikeUrl() }.orEmpty()
                            addSourceInitialFolder = selectedFolder ?: UNFILED_FOLDER_NAME
                            showAddSourceSheet = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
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

    if (showEditSourceSheet) {
        val source = sourceToEdit
        if (source != null) {
            AddSourceSheet(
                onSave = { name, url, folderName ->
                    onUpdateSource(source, name, url, folderName)
                },
                onSaveAndOpen = { name, url, folderName ->
                    onUpdateSource(source, name, url, folderName)
                    onSourceClick(source.copy(name = name, url = url, folderName = folderName))
                },
                onDismiss = {
                    showEditSourceSheet = false
                    sourceToEdit = null
                },
                existingUrls = existingUrls - source.url,
                initialName = source.name,
                initialUrl = source.url,
                initialFolderName = source.folderName,
                existingFolders = folders,
                title = stringResource(R.string.edit_source),
                subtitle = stringResource(R.string.edit_source_subtitle),
                primaryActionLabel = stringResource(R.string.save_changes),
                allowSaveOptions = false,
            )
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(stringResource(R.string.new_folder)) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newFolderName.isNotBlank(),
                    onClick = {
                        val folder = newFolderName.trim()
                        onCreateFolder(folder)
                        selectedFolder = folder
                        showNewFolderDialog = false
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteFolderConfirmation) {
        val folder = selectedFolder
        if (folder != null) {
            AlertDialog(
                onDismissRequest = { showDeleteFolderConfirmation = false },
                title = { Text(stringResource(R.string.delete_folder_title, folder)) },
                text = { Text(stringResource(R.string.delete_folder_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteFolderConfirmation = false
                            selectedFolder = null
                            onDeleteFolder(folder)
                        },
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteFolderConfirmation = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
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

private fun ReadingItem.hasCapturedBody(): Boolean =
    articleBodyForReading().isNotBlank()

private fun ReadingItem.articleBodyForReading(): String =
    markdown?.takeIf { it.isNotBlank() }
        ?: text?.takeIf { it.isNotBlank() }
        ?: excerpt?.takeIf { it.isNotBlank() }
        ?: ""

@Composable
private fun MoveBookmarkDialog(
    item: ReadingItem,
    folders: List<String>,
    newFolderName: String,
    onNewFolderNameChange: (String) -> Unit,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val availableFolders = remember(folders, item.folderName) {
        (listOf(UNFILED_FOLDER_NAME) + folders + item.folderName)
            .distinct()
            .filter { it.isNotBlank() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar en carpeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableFolders, key = { it }) { folder ->
                        val selected = item.folderName == folder
                        FilterChip(
                            selected = selected,
                            onClick = { onMove(folder) },
                            label = { Text(folder) },
                            leadingIcon = { FolderDot(folderName = folder) },
                        )
                    }
                }
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = onNewFolderNameChange,
                    label = { Text("Nueva carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newFolderName.isNotBlank(),
                onClick = { onMove(newFolderName.trim()) },
            ) {
                Text("Mover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BottomHomeControls(
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    readingCount: Int,
    historyCount: Int,
    showAddSourceAction: Boolean,
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionSelector(
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected,
            readingCount = readingCount,
            historyCount = historyCount,
            modifier = Modifier.weight(1f),
        )
        if (showAddSourceAction) {
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(HomeBottomActionSize),
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

@Composable
private fun SectionSelector(
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    readingCount: Int,
    historyCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeSection.entries.forEach { section ->
                    val selected = selectedSection == section
                    val count = when (section) {
                        HomeSection.Sources -> 0
                        HomeSection.ReadLater -> readingCount
                        HomeSection.History -> historyCount
                    }
                    SectionSelectorItem(
                        section = section,
                        selected = selected,
                        count = count,
                        onClick = { onSectionSelected(section) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionSelectorItem(
    section: HomeSection,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BadgedBox(
                badge = {
                    if (count > 0) {
                        Badge {
                            Text(count.coerceAtMost(99).toString())
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = section.shortTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
    var editing by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing) {
        if (editing) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { editing = true }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (editing || value.isNotBlank()) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            editing = false
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
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
            } else {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
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
    allLabel: String = stringResource(R.string.all_sources),
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedFolder == null,
                onClick = { onFolderSelected(null) },
                label = { Text(allLabel) },
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
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
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
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            action?.invoke()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SwipeableReadingItem(
    item: ReadingItem,
    onArchive: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onToggleRead()
                SwipeToDismissBoxValue.EndToStart -> onArchive()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val target = dismissState.targetValue
            val isArchive = target == SwipeToDismissBoxValue.EndToStart
            val backgroundColor = if (isArchive) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
            val foregroundColor = if (isArchive) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            }
            val alignment = if (isArchive) Alignment.CenterEnd else Alignment.CenterStart
            val icon = if (isArchive) Icons.Rounded.Archive else Icons.Rounded.Check
            val label = if (isArchive) {
                "Archivar"
            } else if (item.isRead) {
                "No leído"
            } else {
                "Leído"
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isArchive) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = foregroundColor)
                        Icon(imageVector = icon, contentDescription = label, tint = foregroundColor)
                    } else {
                        Icon(imageVector = icon, contentDescription = label, tint = foregroundColor)
                        Text(label, style = MaterialTheme.typography.labelLarge, color = foregroundColor)
                    }
                }
            }
        },
        content = { Box(modifier = Modifier.fillMaxWidth()) { content() } },
    )
}

@Composable
private fun ReadLaterHero(
    item: ReadingItem,
    onClick: () -> Unit,
    onRetryCapture: () -> Unit,
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
                item.captureStatus.toCaptureStatusLabel()?.let { statusLabel ->
                    Text(
                        text = "· $statusLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (item.captureStatus == CAPTURE_STATUS_FAILED) {
                    TextButton(onClick = onRetryCapture) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Reintentar extracción")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingListGroup(
    items: List<ReadingItem>,
    onItemClick: (ReadingItem) -> Unit,
    onArchive: (ReadingItem) -> Unit,
    onToggleRead: (ReadingItem) -> Unit,
    onRetryCapture: (ReadingItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    GroupedListSurface(modifier = modifier) {
        items.forEachIndexed { index, item ->
            SwipeableReadingItem(
                item = item,
                onArchive = { onArchive(item) },
                onToggleRead = { onToggleRead(item) },
            ) {
                ReadingListItem(
                    item = item,
                    onClick = { onItemClick(item) },
                    onRetryCapture = { onRetryCapture(item) },
                )
            }
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
    onRetryCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrowserFavicon(url = item.url, fallbackText = item.title, modifier = Modifier.size(42.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.readingMetadataLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.addedAt.toRelativeTimeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (item.captureStatus == CAPTURE_STATUS_FAILED) {
                    IconButton(onClick = onRetryCapture, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reintentar extracción",
                        )
                    }
                }
            }
        }
    }
}

private fun ReadingItem.readingMetadataLabel(): String =
    listOfNotNull(
        captureStatus.toCaptureStatusLabel(),
        sourceName.ifBlank { url.toDisplayHost() },
        url.toDisplayHost(),
        folderName.takeIf { it != UNFILED_FOLDER_NAME },
    ).joinToString(" · ")

private fun String.toCaptureStatusLabel(): String? =
    when (this) {
        CAPTURE_STATUS_PENDING -> "En cola"
        CAPTURE_STATUS_CAPTURING -> "Capturando en background"
        CAPTURE_STATUS_FAILED -> "Falló · tocar Reintentar extracción"
        else -> null
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
            onSetRead = { _, _ -> },
            onArchiveItem = {},
            onRestoreItem = {},
            onMoveBookmarkToFolder = { _, _ -> },
            onRetryCapture = {},
            onClearHistory = {},
            onCreateFolder = {},
            onDeleteFolder = {},
            onUpdateSource = { _, _, _, _ -> },
            existingUrls = emptySet(),
        )
    }
}
