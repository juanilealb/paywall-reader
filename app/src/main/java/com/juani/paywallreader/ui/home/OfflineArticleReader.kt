package com.juani.paywallreader.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.juani.paywallreader.domain.model.ReadingItem
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class OfflineArticleReaderModel(
    val title: String,
    val byline: String,
    val excerpt: String?,
    val heroImageUrl: String?,
    val blocks: List<OfflineArticleBlock>,
)

internal data class OfflineArticleBlock(
    val type: OfflineArticleBlockType,
    val text: String,
)

internal enum class OfflineArticleBlockType {
    Heading,
    Paragraph,
    Bullet,
    Quote,
    Code,
}

internal fun ReadingItem.toOfflineArticleReaderModel(): OfflineArticleReaderModel {
    val host = url.toOfflineDisplayHost()
    val byline = listOfNotNull(
        author?.trim()?.takeIf { it.isNotBlank() },
        sourceName.trim().takeIf { it.isNotBlank() },
        host.takeIf { it.isNotBlank() && it != sourceName.trim() },
    ).joinToString(" · ").ifBlank { host }
    val rawBody = markdown?.takeIf { it.isNotBlank() }
        ?: text?.takeIf { it.isNotBlank() }
        ?: excerpt?.takeIf { it.isNotBlank() }
        ?: ""
    return OfflineArticleReaderModel(
        title = title.trim().ifBlank { host.ifBlank { url } },
        byline = byline,
        excerpt = excerpt?.trim()?.takeIf { it.isNotBlank() },
        heroImageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() },
        blocks = rawBody.toOfflineArticleBlocks(title = title, url = url),
    )
}

private fun String.toOfflineArticleBlocks(title: String, url: String): List<OfflineArticleBlock> =
    lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { line ->
            line.equals("# ${title.trim()}", ignoreCase = true) ||
                line.equals(title.trim(), ignoreCase = true) ||
                line.startsWith("Source:", ignoreCase = true) ||
                line == url
        }
        .map { line ->
            when {
                line.startsWith("### ") -> OfflineArticleBlock(OfflineArticleBlockType.Heading, line.removePrefix("### ").trim())
                line.startsWith("## ") -> OfflineArticleBlock(OfflineArticleBlockType.Heading, line.removePrefix("## ").trim())
                line.startsWith("# ") -> OfflineArticleBlock(OfflineArticleBlockType.Heading, line.removePrefix("# ").trim())
                line.startsWith("- ") -> OfflineArticleBlock(OfflineArticleBlockType.Bullet, line.removePrefix("- ").trim())
                line.startsWith("* ") -> OfflineArticleBlock(OfflineArticleBlockType.Bullet, line.removePrefix("* ").trim())
                line.startsWith("> ") -> OfflineArticleBlock(OfflineArticleBlockType.Quote, line.removePrefix("> ").trim())
                line.startsWith("```") -> OfflineArticleBlock(OfflineArticleBlockType.Code, line.removePrefix("```").trim())
                else -> OfflineArticleBlock(OfflineArticleBlockType.Paragraph, line)
            }
        }
        .filter { it.text.isNotBlank() }
        .toList()

@Composable
internal fun OfflineArticleReaderScreen(
    item: ReadingItem,
    onBack: () -> Unit,
    onOpenWeb: () -> Unit,
    onMarkRead: () -> Unit,
    onArchiveAndNext: () -> Unit,
    onMoveToFolderAndNext: () -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = remember(item) { item.toOfflineArticleReaderModel() }
    BackHandler(onBack = onBack)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF10100E),
        contentColor = Color(0xFFF6F0E4),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B1A16), Color(0xFF10100E), Color(0xFF080807)),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 18.dp,
                    end = 24.dp,
                    bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 124.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    OfflineArticleTopBar(
                        byline = model.byline,
                        onBack = onBack,
                        onOpenWeb = onOpenWeb,
                        onMarkRead = onMarkRead,
                    )
                }
                item {
                    Column(
                        modifier = Modifier.widthIn(max = 720.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        OfflineArticleHero(
                            title = model.title,
                            imageUrl = model.heroImageUrl,
                        )
                        Text(
                            text = model.title,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF7EA),
                            lineHeight = MaterialTheme.typography.displaySmall.lineHeight * 1.05,
                        )
                        Text(
                            text = model.byline,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFC7BDA9),
                        )
                        model.excerpt?.let { excerpt ->
                            Text(
                                text = excerpt,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE2D8C6),
                                lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.2,
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                items(model.blocks) { block ->
                    OfflineArticleBlockView(
                        block = block,
                        modifier = Modifier.widthIn(max = 680.dp),
                    )
                }
            }
            FloatingReaderActions(
                onArchiveAndNext = onArchiveAndNext,
                onMoveToFolderAndNext = onMoveToFolderAndNext,
                onExitToMenu = onExitToMenu,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 14.dp,
                        bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 16.dp,
                    ),
            )
        }
    }
}

@Composable
private fun OfflineArticleTopBar(
    byline: String,
    onBack: () -> Unit,
    onOpenWeb: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 720.dp),
        shape = CircleShape,
        color = Color(0xFF26231D).copy(alpha = 0.92f),
        contentColor = Color(0xFFFFF7EA),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = byline,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFC7BDA9),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onOpenWeb) {
                Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Abrir web")
            }
            FilledTonalButton(onClick = onMarkRead) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Text("Leído")
            }
        }
    }
}

@Composable
private fun FloatingReaderActions(
    onArchiveAndNext: () -> Unit,
    onMoveToFolderAndNext: () -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ReadingActionToolbar(
                onArchiveAndNext = onArchiveAndNext,
                onMoveToFolderAndNext = onMoveToFolderAndNext,
                onExitToMenu = onExitToMenu,
            )
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFF26231D).copy(alpha = 0.96f),
            contentColor = Color(0xFFFFF7EA),
        ) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.MoreVert,
                    contentDescription = if (expanded) "Ocultar acciones" else "Mostrar acciones",
                )
            }
        }
    }
}

@Composable
private fun ReadingActionToolbar(
    onArchiveAndNext: () -> Unit,
    onMoveToFolderAndNext: () -> Unit,
    onExitToMenu: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF26231D).copy(alpha = 0.96f),
        contentColor = Color(0xFFFFF7EA),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            TextButton(onClick = onArchiveAndNext) {
                Icon(Icons.Rounded.Archive, contentDescription = null)
                Spacer(modifier = Modifier.widthIn(min = 8.dp))
                Text("Archivar + sig.")
            }
            TextButton(onClick = onMoveToFolderAndNext) {
                Icon(Icons.Rounded.Folder, contentDescription = null)
                Spacer(modifier = Modifier.widthIn(min = 8.dp))
                Text("Carpeta + sig.")
            }
            TextButton(onClick = onExitToMenu) {
                Icon(Icons.Rounded.Close, contentDescription = null)
                Spacer(modifier = Modifier.widthIn(min = 8.dp))
                Text("Salir")
            }
        }
    }
}

@Composable
private fun OfflineArticleHero(
    title: String,
    imageUrl: String?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF2D271F),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RemoteHeroImage(
                imageUrl = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC0E0D0B)),
                        ),
                    ),
            )
            Text(
                text = "Offline reader",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFEADDC7),
            )
        }
    }
}

@Composable
private fun RemoteHeroImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(imageUrl) {
        bitmap = null
        val url = imageUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") } ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching { URL(url).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.clip(RoundedCornerShape(32.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF554330), Color(0xFF231E18), Color(0xFF11100E)),
                ),
            ),
        )
    }
}

@Composable
private fun OfflineArticleBlockView(
    block: OfflineArticleBlock,
    modifier: Modifier = Modifier,
) {
    SelectionContainer {
        when (block.type) {
            OfflineArticleBlockType.Heading -> Text(
                text = block.text,
                modifier = modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFF7EA),
            )

            OfflineArticleBlockType.Bullet -> Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("•", color = Color(0xFFE5B76B), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFECE1CF),
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.35,
                )
            }

            OfflineArticleBlockType.Quote -> Surface(
                modifier = modifier,
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF211E19),
            ) {
                Text(
                    text = block.text,
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE9D6B8),
                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 1.25,
                )
            }

            OfflineArticleBlockType.Code -> Surface(
                modifier = modifier,
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF080807),
            ) {
                Text(
                    text = block.text,
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFFEDE1D0),
                )
            }

            OfflineArticleBlockType.Paragraph -> Text(
                text = block.text,
                modifier = modifier,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFECE1CF),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.42,
            )
        }
    }
}

private fun String.toOfflineDisplayHost(): String =
    removePrefix("https://")
        .removePrefix("http://")
        .substringBefore("/")
        .removePrefix("www.")
