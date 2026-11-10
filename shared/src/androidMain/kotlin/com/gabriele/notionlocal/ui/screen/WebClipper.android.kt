package com.gabriele.notionlocal.ui.screen

import android.graphics.RectF
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.data.LinkPreviews
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.webclip.ClipContent
import com.gabriele.notionlocal.data.webclip.WebClipDownloads
import com.gabriele.notionlocal.data.webclip.WebClipEngine
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

/**
 * Un motore del Web Clipper legato alla composizione: il suo WebView si
 * chiude quando la finestra se ne va (un attimo dopo, quando non è più
 * attaccato).
 */
@Composable
private fun rememberWebClipEngine(create: () -> WebClipEngine): WebClipEngine {
    val engine = remember { create() }
    DisposableEffect(engine) {
        onDispose { Handler(Looper.getMainLooper()).post { engine.destroy() } }
    }
    return engine
}

/**
 * **La foto in corso** (Full Page e Article): a tutto schermo, coi passi
 * scritti ("Opening the page…", "Removing cookie banners…", "Taking the
 * picture…"), la barra che avanza e, sotto, la pagina stessa mentre scorre,
 * rimpicciolita. Il WebView deve stare sullo schermo: nascosto, Chrome non lo
 * disegna. "Cancel" ferma tutto e la foto a metà non resta.
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
    val engine = rememberWebClipEngine {
        if (article) {
            WebClipEngine(context, cssWidth = WebClipEngine.ARTICLE_CSS_WIDTH, dark = request.dark)
        } else {
            WebClipEngine(context, dark = request.dark)
        }
    }
    var status by remember { mutableStateOf(WebClipStrings.stepOpening) }
    var progress by remember { mutableFloatStateOf(-1f) }
    val latestDone by rememberUpdatedState(onDone)
    val latestFailed by rememberUpdatedState(onFailed)
    val latestContent by rememberUpdatedState(onContent)
    val latestCancel by rememberUpdatedState(onCancel)

    LaunchedEffect(engine) {
        var reserved: File? = null
        var finished = false
        val downloaded = mutableListOf<String>()
        try {
            engine.load(request.url) ?: error("page not loaded")
            if (request.mode == WebClipMode.TEXT_AND_IMAGES) {
                // Il testo e le immagini dell'articolo, non una foto: prima si scorre
                // la pagina perché le immagini dicano il loro indirizzo vero.
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
                engine.forceDesktopWidth()
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
            if (!finished) {
                reserved?.delete()
                // Le immagini già scaricate per un "Text and Images" fermato non restano.
                downloaded.forEach { store.fileFor(it).delete() }
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSheet)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(WebClipStrings.clipping, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text(Strings.cancel) }
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = NotionGray400, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.size(10.dp))
            if (progress < 0f) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            } else {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            Spacer(modifier = Modifier.size(14.dp))
            // La pagina vera, rimpicciolita fino a stare larga quanto lo schermo.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val scale = boxWidthPx / engine.viewWidthPx
                AndroidView(
                    factory = { engine.webView },
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(
                            with(density) { engine.viewWidthPx.toDp() },
                            with(density) { engine.viewHeightPx.toDp() }
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                )
            }
        }
    }
}

/**
 * **"Region"**: il sito a tutto schermo, come sul PC ma largo quanto il
 * telefono; si scorre e si ingrandisce come in un browser (e se serve si
 * entra col proprio account: resta per le foto dopo). "Select" ferma la
 * pagina e col dito si disegna il rettangolo; "Clip" lo fotografa, nitido
 * come la pagina intera.
 */
@Composable
actual fun WebClipRegionDialog(
    request: WebClipRequest,
    store: PageImageStore,
    onDone: (WebClipResult) -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSheet)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            var selecting by remember { mutableStateOf(false) }
            var area by remember { mutableStateOf<Rect?>(null) }
            var busy by remember { mutableStateOf(false) }
            var engineRef by remember { mutableStateOf<WebClipEngine?>(null) }
            val scope = rememberCoroutineScope()

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) { Text(Strings.cancel) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    selecting = !selecting
                    if (!selecting) area = null
                }) {
                    Text(if (selecting) WebClipStrings.browse else WebClipStrings.select)
                }
                Spacer(modifier = Modifier.size(4.dp))
                Button(
                    enabled = !busy && area != null && engineRef != null,
                    onClick = {
                        val engine = engineRef ?: return@Button
                        val rect = area ?: return@Button
                        busy = true
                        scope.launch {
                            val (name, file) = store.newFile("jpg")
                            val image = runCatching {
                                engine.captureRegion(RectF(rect.left, rect.top, rect.right, rect.bottom), file)
                            }.getOrNull()
                            if (image != null) {
                                onDone(WebClipResult(name, image, engine.pageTitle))
                            } else {
                                file.delete()
                                busy = false
                                onFailed()
                            }
                        }
                    }
                ) { Text(WebClipStrings.clip) }
            }
            Text(
                if (selecting) WebClipStrings.dragHint else WebClipStrings.regionHint,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
                val density = LocalDensity.current
                val context = LocalPlatformContext.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val scale = widthPx / WebClipEngine.DESKTOP_CSS_WIDTH
                val engine = rememberWebClipEngine {
                    WebClipEngine(context, cssHeight = (heightPx / scale).roundToInt().coerceAtLeast(1), outputScale = scale, dark = request.dark)
                }
                LaunchedEffect(engine) {
                    engineRef = engine
                    if (engine.load(request.url) == null) {
                        onFailed()
                        return@LaunchedEffect
                    }
                    engine.forceDesktopWidth()
                    delay(400)
                    engine.cleanUp()
                }
                AndroidView(
                    factory = { engine.webView },
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(with(density) { engine.viewWidthPx.toDp() }, with(density) { engine.viewHeightPx.toDp() })
                )
                if (selecting) {
                    SelectionLayer(area = area, onArea = { area = it })
                }
            }
        }
    }
}

