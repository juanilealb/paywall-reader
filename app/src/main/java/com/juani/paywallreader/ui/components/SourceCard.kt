package com.juani.paywallreader.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juani.paywallreader.R
import com.juani.paywallreader.domain.model.Source
import com.juani.paywallreader.ui.theme.PaywallReaderTheme
import com.juani.paywallreader.ui.theme.PaywallReaderMotion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceCard(
    source: Source,
    onClick: (Source) -> Unit,
    onDelete: (Source) -> Unit,
    deleteLabel: String,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = PaywallReaderMotion.standardSpring,
        label = "source-card-press",
    )
    val host = remember(source.url) { source.url.toDisplayHost() }

    Box(modifier = modifier) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
                .scale(cardScale)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onClick(source) },
                    onLongClick = {
                        if (!source.isDefault) menuExpanded = true
                    },
                ),
            shape = MaterialTheme.shapes.small,
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (source.isDefault) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SourceMark(source.name)
                    if (source.isDefault) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.default_source_label)) },
                        )
                    }
                }
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!source.isDefault) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp),
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.source_options),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(deleteLabel) },
                onClick = {
                    menuExpanded = false
                    showDeleteConfirmation = true
                },
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_source_title)) },
            text = { Text(stringResource(R.string.delete_source_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(source)
                    },
                ) {
                    Text(deleteLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SourceMark(name: String) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Surface(
        modifier = Modifier.size(48.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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

@Preview(showBackground = true)
@Composable
private fun SourceCardPreview() {
    PaywallReaderTheme {
        SourceCard(
            source = Source(
                id = 1,
                name = "Example",
                url = "https://example.com",
                isDefault = false,
            ),
            onClick = {},
            onDelete = {},
            deleteLabel = "Delete",
            modifier = Modifier.padding(16.dp),
        )
    }
}
