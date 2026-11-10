package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.webclip.WebClipMode
import com.gabriele.notionlocal.data.webclip.WebClipRequest
import com.gabriele.notionlocal.data.webclip.webClipCaptions
import com.gabriele.notionlocal.data.webclip.webClipHost
import com.gabriele.notionlocal.data.webclip.headingSize
import com.gabriele.notionlocal.data.webclip.toNewBlocks
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.ui.i18n.WebClipStrings
import com.gabriele.notionlocal.data.PageImageStore
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext

/**
 * **Il Web Clipper dalla pagina** (dal 09/11/2026 fuori da `PageEditorScreen`,
 * dal 10/11/2026 lo stesso su telefono e PC): con [open] la finestra per
 * scegliere il sito e come prenderlo, poi la cattura, e il risultato nella
 * pagina. La cattura è di ogni sistema (`WebClipCaptureDialog`, `WebClipRegionDialog`).
 */
@Composable
fun WebClipperFlow(open: Boolean, onClose: () -> Unit, viewModel: PageEditorViewModel, imageStore: PageImageStore) {
    val context = LocalPlatformContext.current
    // La pagina da prendere, finché la finestra della cattura è aperta.
    var webClipRequest by remember { mutableStateOf<WebClipRequest?>(null) }
    if (open) {
        WebClipperSheet(
            onDismiss = onClose,
            onClip = { request ->
                onClose()
                if (request.mode == WebClipMode.BOOKMARK) {
                    // Il segnalibro prende da solo titolo, descrizione e immagine del sito.
                    viewModel.insertWebClip(
                        BlockType.BOOKMARK,
                        BlockAttrs(url = request.url),
                        webClipCaptions(title = null, note = request.note, url = null, clippedFrom = WebClipStrings.clippedFrom)
                    )
                } else {
                    webClipRequest = request
                }
            }
        )
    }
    webClipRequest?.let { request ->
        val placeClip: (WebClipResult) -> Unit = { result ->
            webClipRequest = null
            val title = request.title.ifBlank { result.pageTitle.orEmpty() }
            viewModel.insertWebClip(
                BlockType.IMAGE,
                BlockAttrs(
                    file = result.fileName,
                    name = title.ifBlank { webClipHost(request.url) },
                    url = request.url,
                    mime = "image/jpeg",
                    size = result.image.file.length()
                ),
                webClipCaptions(title, request.note, request.url, WebClipStrings.clippedFrom)
            )
            showMessage(context, WebClipStrings.clipped)
        }
        val failed = {
            webClipRequest = null
            showClipFailed(context)
        }
        // "Text and Images": in cima il titolo grande, la nota e "Clipped from", come
        // OneNote; poi l'articolo, riga per riga, con le sue immagini.
        val placeText: (WebClipTextResult) -> Unit = { result ->
            webClipRequest = null
            val title = request.title.ifBlank { result.content.title }
            val header = webClipCaptions(title, request.note, request.url, WebClipStrings.clippedFrom, titleSize = headingSize(1))
                .map { NewBlock(spans = it) }
            viewModel.insertWebClipBlocks(header + result.content.toNewBlocks(result.images))
            showMessage(context, WebClipStrings.clipped)
        }
        if (request.mode == WebClipMode.REGION) {
            WebClipRegionDialog(request, imageStore, onDone = placeClip, onFailed = failed, onCancel = { webClipRequest = null })
        } else {
            WebClipCaptureDialog(
                request,
                imageStore,
                onDone = placeClip,
                onFailed = failed,
                onCancel = { webClipRequest = null },
                onContent = placeText
            )
        }
    }
}
