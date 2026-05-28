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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
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

private val OfflineReaderToolbarHeight = 72.dp
private val OfflineReaderToolbarActionSize = 42.dp
private val OfflineReaderToolbarIconSize = 20.dp
private val OfflineReaderFabSize = 60.dp

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
                line.startsWith("Original:", ignoreCase = true) ||
                line.startsWith("Fecha:", ignoreCase = true) ||
                (line.startsWith("_") && line.endsWith("_")) ||
                line == "---" ||
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    .align(Alignment.BottomCenter)
                    .padding(WindowInsets.safeDrawing.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .widthIn(max = 520.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OfflineArticleTopBar(
    byline: String,
    onBack: () -> Unit,
    onOpenWeb: () -> Unit,
    onMarkRead: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(OfflineReaderToolbarActionSize),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    modifier = Modifier.size(OfflineReaderToolbarIconSize),
                )
            }
            Text(
                text = byline,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = onOpenWeb,
                modifier = Modifier.size(OfflineReaderToolbarActionSize),
            ) {
                Icon(
                    Icons.Rounded.OpenInBrowser,
                    contentDescription = "Abrir web",
                    modifier = Modifier.size(OfflineReaderToolbarIconSize),
                )
            }
            IconButton(
                onClick = onMarkRead,
                modifier = Modifier.size(OfflineReaderToolbarActionSize),
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Marcar leído",
                    modifier = Modifier.size(OfflineReaderToolbarIconSize),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingReaderActions(
    onArchiveAndNext: () -> Unit,
    onMoveToFolderAndNext: () -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    HorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = {
            Box(
                modifier = Modifier.size(OfflineReaderFabSize),
                contentAlignment = Alignment.Center,
            ) {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = { expanded = !expanded },
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        contentDescription = if (expanded) "Ocultar acciones" else "Mostrar acciones",
                    )
                }
            }
        },
        modifier = modifier.height(OfflineReaderToolbarHeight),
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
        floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End,
    ) {
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            ReadingActionToolbar(
                onArchiveAndNext = onArchiveAndNext,
                onMoveToFolderAndNext = onMoveToFolderAndNext,
                onExitToMenu = onExitToMenu,
            )
        }
    }
}

@Composable
private fun ReadingActionToolbar(
    onArchiveAndNext: () -> Unit,
    onMoveToFolderAndNext: () -> Unit,
    onExitToMenu: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onArchiveAndNext,
            modifier = Modifier.size(OfflineReaderToolbarActionSize),
        ) {
            Icon(
                Icons.Rounded.Archive,
                contentDescription = "Archivar y siguiente",
                modifier = Modifier.size(OfflineReaderToolbarIconSize),
            )
        }
        IconButton(
            onClick = onMoveToFolderAndNext,
            modifier = Modifier.size(OfflineReaderToolbarActionSize),
        ) {
            Icon(
                Icons.Rounded.Folder,
                contentDescription = "Mover a carpeta y siguiente",
                modifier = Modifier.size(OfflineReaderToolbarIconSize),
            )
        }
        IconButton(
            onClick = onExitToMenu,
            modifier = Modifier.size(OfflineReaderToolbarActionSize),
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Salir",
                modifier = Modifier.size(OfflineReaderToolbarIconSize),
            )
        }
    }
}

@Composable
private fun OfflineArticleHero(
    title: String,
    imageUrl: String?,
) {
    if (imageUrl.isNullOrBlank()) {
        return
    }

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
                text = title,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(22.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFEADDC7),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
