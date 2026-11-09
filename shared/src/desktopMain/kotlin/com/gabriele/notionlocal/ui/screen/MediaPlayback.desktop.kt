package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.openUrl
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import java.awt.Desktop
import java.net.URI
import java.net.URLDecoder

/** Sul PC, per ora: una riga che apre il video o l'audio col programma del computer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OpenOutsideRow(label: String, open: () -> Unit, modifier: Modifier, onLongPress: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .combinedClickable(onClick = open, onLongClick = onLongPress)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = NotionWhite, maxLines = 1)
    }
}

@Composable
private fun openerFor(source: MediaSource): () -> Unit {
    val context = LocalPlatformContext.current
    return {
        when (source) {
            is MediaSource.LocalFile -> runCatching { Desktop.getDesktop().open(source.file) }
            is MediaSource.Link -> openUrl(context, source.url)
        }
    }
}

private fun MediaSource.label(): String = when (this) {
    is MediaSource.LocalFile -> file.name
    is MediaSource.Link -> linkHost(url)
}

@Composable
actual fun InlineVideoPlayer(source: MediaSource, modifier: Modifier, onLongPress: (() -> Unit)?) =
    OpenOutsideRow(source.label(), openerFor(source), modifier, onLongPress)

@Composable
actual fun InlineAudioPlayer(source: MediaSource, title: String, modifier: Modifier) =
    OpenOutsideRow(title, openerFor(source), modifier)

@Composable
actual fun EmbeddedVideoPlayer(embedUrl: String, modifier: Modifier) {
    val context = LocalPlatformContext.current
    OpenOutsideRow(linkHost(embedUrl), { openUrl(context, embedUrl) }, modifier)
}

/** Un indirizzo a pezzi, come li dà l'`Uri` del telefono: il sito, il percorso, le domande. */
private class UrlParts(val host: String, val pathSegments: List<String>, val query: Map<String, String>)

private fun partsOf(url: String): UrlParts? = runCatching {
    val uri = URI(url.trim().replace(" ", "%20"))
    val host = uri.host?.lowercase() ?: return null
    val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    val query = uri.rawQuery.orEmpty().split('&').filter { '=' in it }
        .associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8") }
    UrlParts(host, segments, query)
}.getOrNull()

actual fun youTubeId(url: String): String? {
    val uri = partsOf(url) ?: return null
    val host = uri.host.removePrefix("www.").removePrefix("m.").removePrefix("music.")
    val id = when (host) {
        "youtu.be" -> uri.pathSegments.firstOrNull()
        "youtube.com", "youtube-nocookie.com" -> when (uri.pathSegments.firstOrNull()) {
            "watch" -> uri.query["v"]
            "shorts", "embed", "live", "v" -> uri.pathSegments.getOrNull(1)
            else -> null
        }
        else -> null
    }
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) }
}

actual fun vimeoId(url: String): String? {
    val uri = partsOf(url) ?: return null
    val host = uri.host.removePrefix("www.")
    if (host != "vimeo.com" && host != "player.vimeo.com") return null
    return uri.pathSegments.lastOrNull { it.all(Char::isDigit) }
}
