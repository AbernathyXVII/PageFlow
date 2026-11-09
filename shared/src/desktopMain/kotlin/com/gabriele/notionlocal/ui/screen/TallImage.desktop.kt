package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.data.decodeImage
import com.gabriele.notionlocal.data.toImageBitmap
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Sul PC, per ora: l'immagine intera, rimpicciolita a [width] pixel di larghezza. */
@Composable
private fun rememberWholeImage(file: File, width: Int): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, file, width) {
        value = withContext(Dispatchers.IO) { runCatching { decodeImage(file, width, MAX_PIXELS)?.toImageBitmap() }.getOrNull() }
    }
    return bitmap
}

private const val MAX_PIXELS = 40_000_000L

@Composable
actual fun StripImage(file: File, imageWidth: Int, imageHeight: Int, description: String?, modifier: Modifier) {
    val bitmap = rememberWholeImage(file, 1200)
    Box(modifier = modifier.fillMaxWidth().aspectRatio(imageWidth.toFloat() / imageHeight.coerceAtLeast(1))) {
        bitmap?.let { Image(it, contentDescription = description, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
actual fun TallImageViewer(
    file: File,
    imageWidth: Int,
    imageHeight: Int,
    title: String,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)?
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val bitmap = rememberWholeImage(file, 2000)
        Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = EditorStrings.close, tint = NotionWhite) }
                Text(title, style = MaterialTheme.typography.titleMedium, color = NotionWhite, maxLines = 1, modifier = Modifier.weight(1f))
                if (onDownload != null) {
                    IconButton(onClick = onDownload) { Icon(Icons.Filled.Download, contentDescription = BlockStrings.download, tint = NotionWhite) }
                }
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(imageWidth.toFloat() / imageHeight.coerceAtLeast(1))) {
                    bitmap?.let { Image(it, contentDescription = title, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize()) }
                }
            }
        }
    }
}
