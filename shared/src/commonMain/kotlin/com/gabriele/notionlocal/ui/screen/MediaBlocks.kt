package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.launch
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.isPc
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.rememberImageImporter
import com.gabriele.notionlocal.data.ocr.PhotoTextRequests
import com.gabriele.notionlocal.data.toImageBitmap

/** Quanto può stringersi un'immagine: un quarto della pagina. */
private const val MIN_IMAGE_WIDTH = 0.25f

/**
 * **Il riquadro di un blocco-file ancora vuoto** (dal 06/10/2026), come su
 * Notion: l'icona grigia e "Add an image", "Embed or upload a video"…
 * Toccandolo si sceglie il file; tenendolo premuto si apre il menu del blocco.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPlaceholder(
    icon: ImageVector,
    text: String,
    busy: Boolean,
    onClick: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .onRightClick(onLongPress)
            .combinedClickable(
                enabled = !busy,
                onClick = { onClick?.invoke() },
                onLongClick = onLongPress
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(icon, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = NotionGray400)
    }
}

/**
 * Un'immagine intera che **riempie il suo riquadro** (la forma del riquadro la
 * decide il blocco: la sua, o quella scelta con "Full resize"). Letta già
 * rimpicciolita a quanto serve (vedi `PageImageStore.load`).
 */
@Composable
private fun WholeImage(fileName: String, store: PageImageStore, description: String?, modifier: Modifier = Modifier) {
    // Un'immagine molto alta (la foto del Web Clipper) si mostra a strisce: vedi `StripImage`.
    val size = remember(fileName) { store.imageSize(fileName) }
    if (size != null && isTallImage(size.first, size.second)) {
        StripImage(store.fileFor(fileName), size.first, size.second, description, modifier)
        return
    }
    var widthPx by remember(fileName) { mutableIntStateOf(0) }
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(fileName, widthPx) {
        if (widthPx <= 0) return@LaunchedEffect
        // Due volte la larghezza: ingrandita a tutto schermo resta nitida.
        bitmap = store.load(fileName, widthPx * 2)?.toImageBitmap()
    }
    Box(modifier = modifier.fillMaxSize().onSizeChanged { widthPx = it.width }) {
        val image = bitmap
        if (image == null) {
            Box(modifier = Modifier.fillMaxSize().background(DarkSurface))
        } else {
            Image(
                bitmap = image,
                contentDescription = description,
                contentScale = ContentScale.FillBounds,
                // Letta al doppio e rimpicciolita: sul PC il filtro di serie faceva i bordi a scalette (sul telefono è lo stesso).
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * **Il blocco "Image"** (dal 06/10/2026, chiesto dall'utente): un'immagine
 * caricata dal telefono — solo così, come ha chiesto — larga quanto la
 * pagina di partenza e centrata.
 *
 * - **Toccandola** si apre a tutto schermo: lì la si ingrandisce con due
 *   dita e la si scarica nella Galleria;
 * - **tenendola premuta** si apre il menu: ridimensionarla, guardarla,
 *   scaricarla, sostituirla, spostarla, copiarla, toglierla;
 * - **le maniglie** si vedono solo dopo "Resize" (le due ai lati, la forma
 *   resta) o "Full resize" (i quattro lati e i quattro angoli, come una
 *   finestra) dal menu (dal 20/10/2026, chiesto dall'utente: prima erano
 *   sempre lì). "Done", un tocco sull'immagine o il gesto indietro le tolgono.
 *
 * Il menu chiede, il blocco fa (vedi `PageEditorViewModel.mediaRequest`).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    val scope = rememberCoroutineScope()
    var busy by remember(block.id) { mutableStateOf(false) }
    var viewing by remember(block.id) { mutableStateOf(false) }
    // Le maniglie si vedono solo dopo "Resize" o "Full resize" dal menu (dal 20/10/2026).
    var resizing by remember(block.id) { mutableStateOf(ImageResizeMode.NONE) }
    val request by viewModel.mediaRequest.collectAsStateWithLifecycle()

    val pick = rememberImageImporter(store, onStart = { busy = true }) { result ->
        result.fold(
            onSuccess = { (file, name) ->
                viewModel.updateBlockAttrs(block.id) { it.copy(file = file, name = name) }
                // Il suo testo, per la ricerca (dal 03/11/2026).
                PhotoTextRequests.request(delayMs = 1_000)
            },
            onFailure = { showMessage(context, EditorStrings.imageReadFailed) }
        )
        busy = false
    }
    val download: () -> Unit = {
        attrs.file?.let { file ->
            scope.launch {
                val saved = BlockFiles.saveImageToGallery(context, store, file, attrs.name)
                showMessage(context, if (saved) BlockStrings.imageSaved else BlockStrings.saveFailed)
            }
        }
    }

    LaunchedEffect(request) {
        val asked = request?.takeIf { it.blockId == block.id } ?: return@LaunchedEffect
        viewModel.consumeMediaRequest()
        when (asked.kind) {
            MediaRequestKind.PICK -> if (!locked) pick()
            MediaRequestKind.VIEW -> if (attrs.file != null) viewing = true
            MediaRequestKind.DOWNLOAD -> download()
            MediaRequestKind.OPEN_ELSEWHERE -> attrs.file?.let {
                BlockFiles.openWithAnotherApp(context, store, it, BlockFiles.imageMime(store, it))
            }
            MediaRequestKind.RESIZE -> if (!locked && attrs.file != null) resizing = ImageResizeMode.RESIZE
            MediaRequestKind.FULL_RESIZE -> if (!locked && attrs.file != null) resizing = ImageResizeMode.FULL
            // Si registra solo nei blocchi audio.
            MediaRequestKind.RECORD -> Unit
        }
    }
    // Il gesto indietro chiude il ridimensionamento, invece di uscire dalla pagina.
    BackHandler(enabled = resizing != ImageResizeMode.NONE) { resizing = ImageResizeMode.NONE }
    LaunchedEffect(locked) { if (locked) resizing = ImageResizeMode.NONE }

    val file = attrs.file
    if (file == null) {
        MediaPlaceholder(
            icon = Icons.Outlined.Image,
            text = BlockStrings.addImage,
            busy = busy,
            onClick = if (locked) null else pick,
            onLongPress = onLongPress,
            modifier = modifier
        )
    } else {
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val density = LocalDensity.current
            val pageWidth = constraints.maxWidth.toFloat()
            val size = remember(file) { store.imageSize(file) }
            val natural = size?.let { (w, h) -> h.toFloat() / w } ?: DEFAULT_IMAGE_ASPECT
            // Quanto è larga l'immagine vera sullo schermo: sul telefono un pixel dell'immagine è un
            // pixel dello schermo (un 512x512 prende un terzo della larghezza); sul PC un punto, che
            // segue l'ingrandimento di Windows e di PageFlow, come le immagini di Notion.
            val naturalWidth = size?.let { (w, _) -> if (isPc) w * density.density else w.toFloat() }
            // **Uno stato solo, aggiornato.** Rifatto a ogni misura salvata,
            // le maniglie — che si tengono il primo — avrebbero scritto in
            // quello vecchio, e dal secondo trascinamento non si muoveva più.
            var frame by remember(block.id) { mutableStateOf(frameOf(attrs, pageWidth, natural, naturalWidth)) }
            LaunchedEffect(attrs.width, attrs.left, attrs.aspect, pageWidth, natural, naturalWidth) { frame = frameOf(attrs, pageWidth, natural, naturalWidth) }
            val save: (Boolean) -> Unit = { full -> viewModel.updateBlockAttrs(block.id) { it.withFrame(frame, pageWidth, full) } }
            val minPx = with(density) { MIN_FRAME_SIDE.toPx() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(frame.left.roundToInt(), 0) }
                    .size(with(density) { frame.width.toDp() }, with(density) { frame.height.toDp() })
            ) {
                WholeImage(
                    fileName = file,
                    store = store,
                    description = attrs.name ?: EditorStrings.blockType("Image"),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .onRightClick(onLongPress)
                        .combinedClickable(
                            // Mentre si ridimensiona, un tocco sull'immagine finisce.
                            onClick = { if (resizing != ImageResizeMode.NONE) resizing = ImageResizeMode.NONE else viewing = true },
                            onLongClick = onLongPress
                        )
                )
                when (resizing) {
                    ImageResizeMode.NONE -> Unit
                    ImageResizeMode.RESIZE -> {
                        // Le due maniglie di sempre: l'immagine tiene il suo centro, quindi
                        // ognuna sposta tutti e due i lati, e l'altezza segue.
                        listOf(Alignment.CenterStart to -1, Alignment.CenterEnd to 1).forEach { (side, sign) ->
                            ResizeHandle(
                                shape = HandleShape.VERTICAL,
                                modifier = Modifier.align(side),
                                onDrag = { delta -> frame = frame.widened(sign, delta.x, pageWidth, MIN_IMAGE_WIDTH * pageWidth) },
                                onDragEnd = { save(false) }
                            )
                        }
                    }
                    ImageResizeMode.FULL -> {
                        // Come una finestra: i quattro lati e i quattro angoli, si muove solo il bordo afferrato.
                        FULL_HANDLES.forEach { handle ->
                            ResizeHandle(
                                shape = handle.shape,
                                modifier = Modifier.align(handle.alignment),
                                onDrag = { delta -> frame = frame.dragged(handle.edgeX, handle.edgeY, delta.x, delta.y, pageWidth, minPx) },
                                onDragEnd = { save(true) }
                            )
                        }
                    }
                }
                if (resizing != ImageResizeMode.NONE) {
                    ResizeToolbar(
                        onOriginalSize = { viewModel.updateBlockAttrs(block.id) { it.withOriginalSize() } },
                        onDone = { resizing = ImageResizeMode.NONE },
                        // Sotto la maniglia in alto, quando c'è.
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 36.dp)
                    )
                }
            }
        }
    }

    if (viewing && file != null) {
        val size = remember(file) { store.imageSize(file) }
        if (size != null && isTallImage(size.first, size.second)) {
            TallImageViewer(
                file = store.fileFor(file),
                imageWidth = size.first,
                imageHeight = size.second,
                title = attrs.name ?: EditorStrings.blockType("Image"),
                onDismiss = { viewing = false },
                onDownload = download
            )
        } else {
            FullScreenImage(
                fileName = file,
                store = store,
                title = attrs.name ?: EditorStrings.blockType("Image"),
                onDismiss = { viewing = false },
                onDownload = download
            )
        }
    }
}
