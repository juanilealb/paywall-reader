package com.juani.paywallreader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSourceSheet(
    onSave: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    existingUrls: Set<String> = emptySet(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isContentVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
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
                Button(
                    enabled = canSave,
                    onClick = {
                        onSave(name, normalizedUrl)
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.save))
                }
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
            onSave = { _, _ -> },
            onDismiss = {},
        )
    }
}
