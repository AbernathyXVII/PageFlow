package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.data.LinkPreviews
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.webclip.ClipContent
import com.gabriele.notionlocal.data.webclip.imageSources
import com.gabriele.notionlocal.data.webclip.isPicture
import com.gabriele.notionlocal.data.settings.AppSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Switch
import com.gabriele.notionlocal.data.webclip.WebClipImage
import com.gabriele.notionlocal.data.webclip.WebClipMode
import com.gabriele.notionlocal.data.webclip.WebClipRequest
import com.gabriele.notionlocal.data.webclip.webClipUrl
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WebClipStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PlatformContext

/** Una foto finita: il nome del file nell'app, le misure, e il titolo che la pagina ha detto di sé. */
class WebClipResult(val fileName: String, val image: WebClipImage, val pageTitle: String?)

/** "Text and Images" finito: l'articolo, e le sue immagini già nell'app (indirizzo → file). */
class WebClipTextResult(val content: ClipContent, val images: Map<String, String>)

/**
 * **Il pannello del Web Clipper** (dal 20/10/2026, chiesto dall'utente:
 * "le opzioni devono essere identiche a quelle che vedi in foto"), come
 * OneNote Web Clipper: il link, "What do you want to capture?" con Full Page,
 * Region, Bookmark e Article, Title, Note e Source. Niente "Location": la
 * foto va nella pagina aperta, nella prima riga vuota (l'utente: "la
 * feature Location non ha senso, toglila").
 *
 * Il titolo si riempie da solo col titolo del sito, finché non lo si scrive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebClipperSheet(onDismiss: () -> Unit, onClip: (WebClipRequest) -> Unit) {
    var link by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(WebClipMode.FULL_PAGE) }
    var title by remember { mutableStateOf("") }
    var titleTouched by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var dark by remember { mutableStateOf(AppSettings.webClipDark) }
    val url = webClipUrl(link)
    val clipboard = LocalClipboardManager.current

    // Il titolo del sito, letto appena il link è completo.
    LaunchedEffect(url) {
        if (url == null || titleTouched) return@LaunchedEffect
        delay(500)
        val found = LinkPreviews.fetch(url)?.title
        if (!titleTouched && found != null) title = found
    }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(WebClipStrings.webClipper, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.size(16.dp))
            ClipperLabel(WebClipStrings.link)
            // Un campo per indirizzi: `AutoCaps` lo lascia com'è, niente maiuscola.
            AutoCaps {
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it.replace("\n", "") },
                    placeholder = { Text(WebClipStrings.linkPlaceholder, color = NotionGray400) },
                    singleLine = true,
                    isError = link.isNotBlank() && url == null,
                    supportingText = if (link.isNotBlank() && url == null) {
                        { Text(WebClipStrings.notALink) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        IconButton(onClick = { clipboard.getText()?.text?.trim()?.let { link = it } }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = WebClipStrings.paste, tint = NotionGray400)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.size(16.dp))
            ClipperLabel(WebClipStrings.whatToCapture)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeRow(Icons.Filled.CropFree, WebClipStrings.fullPage, mode == WebClipMode.FULL_PAGE) { mode = WebClipMode.FULL_PAGE }
                ModeRow(Icons.Filled.HighlightAlt, WebClipStrings.region, mode == WebClipMode.REGION) { mode = WebClipMode.REGION }
                ModeRow(Icons.Filled.BookmarkBorder, WebClipStrings.bookmark, mode == WebClipMode.BOOKMARK) { mode = WebClipMode.BOOKMARK }
                ModeRow(Icons.AutoMirrored.Filled.Article, WebClipStrings.article, mode == WebClipMode.ARTICLE) { mode = WebClipMode.ARTICLE }
                ModeRow(Icons.AutoMirrored.Filled.Notes, WebClipStrings.textAndImages, mode == WebClipMode.TEXT_AND_IMAGES) {
                    mode = WebClipMode.TEXT_AND_IMAGES
                }
            }
            // **"Dark mode"** (dal 20/10/2026): solo per le foto, e si ricorda.
            if (mode.isPicture) {
                Spacer(modifier = Modifier.size(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            dark = !dark
                            AppSettings.webClipDark = dark
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(WebClipStrings.darkMode, color = NotionWhite, fontWeight = FontWeight.SemiBold)
                        Text(WebClipStrings.darkModeHint, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
                    }
                    Switch(
                        checked = dark,
                        onCheckedChange = {
                            dark = it
                            AppSettings.webClipDark = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.size(18.dp))
            ClipperLabel(WebClipStrings.title)
            AutoCaps {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.replace("\n", " ")
                        titleTouched = true
                    },
                    placeholder = { Text(WebClipStrings.titlePlaceholder, color = NotionGray400) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            ClipperLabel(WebClipStrings.note)
            AutoCaps {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(WebClipStrings.notePlaceholder, color = NotionGray400) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            ClipperLabel(WebClipStrings.source)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    url ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = NotionGray400,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.size(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(Strings.cancel) }
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    enabled = url != null,
                    onClick = { url?.let { onClip(WebClipRequest(it, mode, title.trim(), note.trim(), dark = dark && mode.isPicture)) } }
                ) {
                    Icon(Icons.Filled.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(WebClipStrings.clip)
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ClipperLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = NotionWhite,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/** Una delle quattro scelte: col bordo e il colore dell'app quando è quella scelta, come in OneNote. */
@Composable
private fun ModeRow(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else DarkSurface)
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else NotionGray400.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) accent else NotionWhite, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(12.dp))
        Text(label, color = if (selected) accent else NotionWhite, fontWeight = FontWeight.SemiBold)
    }
}

/** Il velo della selezione: fuori dal rettangolo scuro, il rettangolo col bordo dell'app. */
@Composable
internal fun SelectionLayer(area: Rect?, onArea: (Rect?) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    var start by remember { mutableStateOf(Offset.Zero) }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { at ->
                        start = at
                        onArea(Rect(at, at))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val now = change.position
                        onArea(Rect(min(start.x, now.x), min(start.y, now.y), max(start.x, now.x), max(start.y, now.y)))
                    }
                )
            }
    ) {
        val veil = Color.Black.copy(alpha = 0.45f)
        val r = area
        if (r == null || abs(r.width) < 1f || abs(r.height) < 1f) {
            drawRect(veil)
        } else {
            drawRect(veil, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, r.top))
            drawRect(veil, topLeft = Offset(0f, r.bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - r.bottom))
            drawRect(veil, topLeft = Offset(0f, r.top), size = androidx.compose.ui.geometry.Size(r.left, r.height))
            drawRect(veil, topLeft = Offset(r.right, r.top), size = androidx.compose.ui.geometry.Size(size.width - r.right, r.height))
            drawRect(accent, topLeft = r.topLeft, size = r.size, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

/** L'avviso se la foto non è riuscita. */
fun showClipFailed(context: PlatformContext) {
    showMessage(context, WebClipStrings.clipFailed)
}

/**
 * **La foto in corso** (Full Page e Article) e **"Text and Images"**: i passi
 * scritti, la barra che avanza, "Cancel" che ferma tutto. Sul telefono col suo
 * WebView; sul PC con Edge, nascosto (vedi `DesktopWebClipEngine`).
 */
@Composable
expect fun WebClipCaptureDialog(
    request: WebClipRequest,
    store: PageImageStore,
    onDone: (WebClipResult) -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit,
    /** "Text and Images": l'articolo letto, con le immagini scaricate. */
    onContent: (WebClipTextResult) -> Unit = {}
)

/**
 * **"Region"**: il rettangolo della pagina da tenere. Sul telefono si sfoglia
 * il sito e lo si disegna col dito; sul PC si disegna col mouse sulla foto
 * della pagina intera.
 */
@Composable
expect fun WebClipRegionDialog(
    request: WebClipRequest,
    store: PageImageStore,
    onDone: (WebClipResult) -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit
)
