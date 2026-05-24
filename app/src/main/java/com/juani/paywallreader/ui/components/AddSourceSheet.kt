package com.juani.paywallreader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.juani.paywallreader.R
import com.juani.paywallreader.domain.model.validateSourceUrl
import com.juani.paywallreader.ui.theme.PaywallReaderTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddSourceSheet(
    onSave: (name: String, url: String, folderName: String) -> Unit,
    onSaveAndOpen: (name: String, url: String, folderName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    existingUrls: Set<String> = emptySet(),
    initialUrl: String = "",
    initialFolderName: String = "News",
    existingFolders: List<String> = emptyList(),
) {
    val sheetState = rememberBottomSheetState(
        SheetValue.Hidden,
        setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val scope = rememberCoroutineScope()
    var isContentVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(initialUrl) }
    var folderName by remember { mutableStateOf(initialFolderName) }
    var saveMenuExpanded by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    val validatedUrl = validateSourceUrl(url)
    val normalizedUrl = validatedUrl.normalizedUrl
    val isDuplicate = normalizedUrl in existingUrls
    val showUrlError = url.isNotBlank() && !validatedUrl.isValid
    val canSave = name.isNotBlank() && url.isNotBlank() && validatedUrl.isValid && !isDuplicate

    LaunchedEffect(Unit) {
        isContentVisible = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 520.dp,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = isContentVisible,
            enter = fadeIn(animationSpec = spring()) + expandVertically(animationSpec = spring()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_source),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.add_source_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.source_name)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Title,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text(stringResource(R.string.source_url)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    isError = showUrlError || isDuplicate,
                    supportingText = {
                        when {
                            showUrlError -> Text(stringResource(R.string.invalid_url))
                            isDuplicate -> Text(stringResource(R.string.duplicate_source))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (existingFolders.isNotEmpty()) {
                            Box {
                                FilledIconButton(
                                    onClick = { folderMenuExpanded = true },
                                    modifier = Modifier.padding(end = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = stringResource(R.string.choose_folder),
                                    )
                                }
                                DropdownMenu(
                                    expanded = folderMenuExpanded,
                                    onDismissRequest = { folderMenuExpanded = false },
                                ) {
                                    existingFolders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (folder == folderName) {
                                                        Icons.Rounded.Check
                                                    } else {
                                                        Icons.Rounded.Folder
                                                    },
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                folderName = folder
                                                folderMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    },
                    singleLine = true,
                    supportingText = {
                        if (folderName.isBlank()) {
                            Text(stringResource(R.string.new_folder_hint))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                AnimatedVisibility(visible = validatedUrl.isValid && !isDuplicate) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.source_preview),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = normalizedUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                SplitButtonLayout(
                    leadingButton = {
                        Button(
                            enabled = canSave,
                            onClick = {
                                onSave(name, normalizedUrl, folderName.ifBlank { "News" })
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    },
                    trailingButton = {
                        FilledIconButton(
                            enabled = canSave,
                            onClick = { saveMenuExpanded = true },
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = stringResource(R.string.save_more_options),
                            )
                        }
                        DropdownMenu(
                            expanded = saveMenuExpanded,
                            onDismissRequest = { saveMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_and_open)) },
                                onClick = {
                                    saveMenuExpanded = false
                                    onSaveAndOpen(name, normalizedUrl, folderName.ifBlank { "News" })
                                    scope.launch {
                                        sheetState.hide()
                                        onDismiss()
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_and_add_another)) },
                                onClick = {
                                    saveMenuExpanded = false
                                    onSave(name, normalizedUrl, folderName.ifBlank { "News" })
                                    name = ""
                                    url = ""
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddSourceSheetPreview() {
    PaywallReaderTheme {
        AddSourceSheet(
            onSave = { _, _, _ -> },
            onSaveAndOpen = { _, _, _ -> },
            onDismiss = {},
        )
    }
}
