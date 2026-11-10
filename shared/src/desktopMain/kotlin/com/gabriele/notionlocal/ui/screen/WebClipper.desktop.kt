package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.webclip.DesktopWebClipEngine
import com.gabriele.notionlocal.data.webclip.EdgeBrowser
import com.gabriele.notionlocal.data.webclip.WEB_CLIP_ARTICLE_CSS_WIDTH
import com.gabriele.notionlocal.data.webclip.WEB_CLIP_CSS_WIDTH
import com.gabriele.notionlocal.data.webclip.WebClipDownloads
import com.gabriele.notionlocal.data.webclip.WebClipMode
import com.gabriele.notionlocal.data.webclip.WebClipRequest
import com.gabriele.notionlocal.data.webclip.imageSources
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WebClipStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.io.File
import kotlin.math.roundToInt

/** La finestra della cattura in corso: il titolo, il passo, la barra, "Cancel". */
@Composable
private fun ClipProgress(status: String, progress: Float, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(dismissOnClickOutside = false)) {
        Column(modifier = Modifier.width(460.dp).background(DarkSheet, MaterialTheme.shapes.large).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(WebClipStrings.clipping, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text(Strings.cancel) }
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = NotionGray400)
            Spacer(modifier = Modifier.height(12.dp))
            if (progress < 0f) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * **La cattura sul PC** (Full Page, Article, Text and Images): gli stessi passi
 * del telefono, con Edge nascosto al posto del WebView (vedi
 * `DesktopWebClipEngine`). Senza Edge (né Chrome) lo dice e si chiude.
 */
@Composable
actual fun WebClipCaptureDialog(
    request: WebClipRequest,
    store: PageImageStore,
    onDone: (WebClipResult) -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit,
    onContent: (WebClipTextResult) -> Unit
) {
    val context = LocalPlatformContext.current
    val article = request.mode == WebClipMode.ARTICLE
    var status by remember { mutableStateOf(WebClipStrings.stepOpening) }
    var progress by remember { mutableFloatStateOf(-1f) }
    val latestDone by rememberUpdatedState(onDone)
    val latestFailed by rememberUpdatedState(onFailed)
    val latestContent by rememberUpdatedState(onContent)
    val latestCancel by rememberUpdatedState(onCancel)

    LaunchedEffect(request) {
        if (EdgeBrowser.find() == null) {
            showMessage(context, WebClipStrings.noBrowserOnPc)
            latestCancel()
            return@LaunchedEffect
        }
        val engine = DesktopWebClipEngine(context, cssWidth = if (article) WEB_CLIP_ARTICLE_CSS_WIDTH else WEB_CLIP_CSS_WIDTH, dark = request.dark)
        var reserved: File? = null
        var finished = false
        val downloaded = mutableListOf<String>()
        try {
            engine.load(request.url) ?: error("page not loaded")
            if (request.mode == WebClipMode.TEXT_AND_IMAGES) {
                status = WebClipStrings.stepCleaning
                engine.cleanUp()
                engine.loadLazyContent()
                status = WebClipStrings.stepReading
                val content = engine.readContent()?.takeIf { it.blocks.isNotEmpty() }
                if (content == null) {
                    showMessage(context, WebClipStrings.noArticle)
                    latestCancel()
                    return@LaunchedEffect
                }
                val sources = content.imageSources
                val images = LinkedHashMap<String, String>()
                sources.forEachIndexed { i, src ->
                    status = WebClipStrings.stepImages(i + 1, sources.size)
                    progress = i.toFloat() / sources.size.coerceAtLeast(1)
                    val bytes = WebClipDownloads.image(src, request.url) ?: return@forEachIndexed
                    store.saveImageBytes(bytes)?.let { file ->
                        images[src] = file
                        downloaded += file
                    }
                }
                finished = true
                latestContent(WebClipTextResult(content, images))
                return@LaunchedEffect
            }
            var title = engine.pageTitle
            if (article) {
                status = WebClipStrings.stepArticle
                engine.loadLazyContent()
                title = engine.keepOnlyArticle() ?: title
                delay(1_500)
                engine.loadLazyContent()
            } else {
                status = WebClipStrings.stepCleaning
                engine.prepare()
            }
            status = WebClipStrings.stepCapturing
            progress = 0f
            val (name, file) = store.newFile("jpg")
            reserved = file
            val image = engine.captureFullPage(file) { progress = it } ?: error("no picture")
            finished = true
            latestDone(WebClipResult(name, image, title))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (problem: Throwable) {
            latestFailed()
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                engine.close()
                if (!finished) {
                    reserved?.delete()
                    // Le immagini già scaricate per un "Text and Images" fermato non restano.
                    downloaded.forEach { store.fileFor(it).delete() }
                }
            }
        }
    }

    ClipProgress(status, progress, onCancel)
}

/**
 * **"Region" sul PC**: la pagina intera fotografata con Edge nascosto, poi
 * mostrata qui, larga quanto la finestra; col mouse si trascina il rettangolo
 * da tenere (la rotella scorre), e "Clip" lo ritaglia dalla foto nitida.
 */
@Composable
actual fun WebClipRegionDialog(
    request: WebClipRequest,
    store: PageImageStore,
    onDone: (WebClipResult) -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(WebClipStrings.stepOpening) }
    var progress by remember { mutableFloatStateOf(-1f) }
    // La foto della pagina intera (un file a parte), la sua anteprima e il titolo.
    var page by remember { mutableStateOf<Triple<File, ImageBitmap, Int>?>(null) }
    var title by remember { mutableStateOf<String?>(null) }
    var area by remember { mutableStateOf<Rect?>(null) }
    var busy by remember { mutableStateOf(false) }
    // Quanto è larga l'anteprima sullo schermo, in pixel: per passare dal rettangolo alla foto.
    var shownWidth by remember { mutableFloatStateOf(1f) }
    val latestFailed by rememberUpdatedState(onFailed)
    val latestCancel by rememberUpdatedState(onCancel)

    LaunchedEffect(request) {
        if (EdgeBrowser.find() == null) {
            showMessage(context, WebClipStrings.noBrowserOnPc)
            latestCancel()
            return@LaunchedEffect
        }
        val engine = DesktopWebClipEngine(context, dark = request.dark)
        val whole = File.createTempFile("pageflow-region", ".jpg")
        var shown = false
        try {
            engine.load(request.url) ?: error("page not loaded")
            status = WebClipStrings.stepCleaning
            engine.prepare()
            status = WebClipStrings.stepCapturing
            progress = 0f
            val image = engine.captureFullPage(whole) { progress = it } ?: error("no picture")
            val preview = withContext(Dispatchers.IO) { DesktopWebClipEngine.preview(whole, PREVIEW_WIDTH) } ?: error("no preview")
            title = engine.pageTitle
            page = Triple(whole, preview.toComposeImageBitmap(), image.width)
            shown = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (problem: Throwable) {
            latestFailed()
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                engine.close()
                if (!shown) whole.delete()
            }
        }
    }

    val current = page
    if (current == null) {
        ClipProgress(status, progress) {
            onCancel()
        }
        return
    }
    val (whole, preview, fullWidth) = current
    Dialog(
        onDismissRequest = {
            whole.delete()
            onCancel()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).background(DarkSheet, MaterialTheme.shapes.large)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    whole.delete()
                    onCancel()
                }) { Text(Strings.cancel) }
                Text(
                    WebClipStrings.regionHintOnPc,
                    style = MaterialTheme.typography.bodySmall,
                    color = NotionGray400,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    enabled = !busy && area != null,
                    onClick = {
                        val rect = area ?: return@Button
                        busy = true
                        scope.launch {
                            // Dai punti dell'anteprima (larga quanto la finestra) ai pixel della foto.
                            val k = fullWidth / shownWidth
                            val (name, file) = store.newFile("jpg")
                            val image = withContext(Dispatchers.IO) {
                                runCatching {
                                    DesktopWebClipEngine.crop(
                                        whole,
                                        Rectangle((rect.left * k).roundToInt(), (rect.top * k).roundToInt(), (rect.width * k).roundToInt(), (rect.height * k).roundToInt()),
                                        file
                                    )
                                }.getOrNull()
                            }
                            whole.delete()
                            if (image != null) {
                                onDone(WebClipResult(name, image, title))
                            } else {
                                file.delete()
                                busy = false
                                onFailed()
                            }
                        }
                    }
                ) { Text(WebClipStrings.clip) }
            }
            // L'anteprima larga quanto la finestra; il rettangolo si disegna sopra, nei suoi punti.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = widthPx * preview.height / preview.width
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { heightPx.toDp() })
                        .onSizeChanged { shownWidth = it.width.toFloat().coerceAtLeast(1f) }
                ) {
                    Image(preview, contentDescription = null, modifier = Modifier.fillMaxSize())
                    SelectionLayer(area = area, onArea = { area = it })
                }
            }
        }
    }
}

/** Quanto è larga l'anteprima della pagina in cui si sceglie il rettangolo, in pixel. */
private const val PREVIEW_WIDTH = 1200
