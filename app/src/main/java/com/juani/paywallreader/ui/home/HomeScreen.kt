package com.juani.paywallreader.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juani.paywallreader.R
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.ui.components.AddSourceSheet
import com.juani.paywallreader.ui.components.SourceCard
import com.juani.paywallreader.ui.theme.PaywallReaderTheme

@Composable
fun HomeRoute(
    onSourceClick: (Source) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val sources by viewModel.sources.collectAsState()

    HomeScreen(
        sources = sources,
        onSourceClick = onSourceClick,
        onAddSource = viewModel::addSource,
        onDeleteSource = viewModel::deleteSource,
        existingUrls = sources.map { it.url }.toSet(),
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    sources: List<Source>,
    onSourceClick: (Source) -> Unit,
    onAddSource: (name: String, url: String) -> Unit,
    onDeleteSource: (Source) -> Unit,
    existingUrls: Set<String>,
    modifier: Modifier = Modifier,
) {
    var showAddSourceSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSourceSheet = true },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.add_source)) },
            )
        },
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 168.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader()
            }

            if (sources.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptySources(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                    )
                }
            } else {
                items(
                    items = sources,
                    key = { it.id },
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
    }

    if (showAddSourceSheet) {
        AddSourceSheet(
            onSave = onAddSource,
            onDismiss = { showAddSourceSheet = false },
            existingUrls = existingUrls,
        )
    }
}

@Composable
private fun HomeHeader(
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
            style = MaterialTheme.typography.displaySmall,
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PaywallReaderTheme {
        HomeScreen(
            sources = listOf(
                Source(
                    id = 1,
                    name = "Example",
                    url = "https://example.com",
                    isDefault = true,
                ),
                Source(
                    id = 2,
                    name = "News",
                    url = "https://news.example.com",
                    isDefault = false,
                ),
            ),
            onSourceClick = {},
            onAddSource = { _, _ -> },
            onDeleteSource = {},
            existingUrls = emptySet(),
        )
    }
}
