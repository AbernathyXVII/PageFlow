package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.layout.Row
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.LocalPlatformContext

/**
 * **"Image"** (dal 29/09/2026, come la colonna delle figure del Pokédex di
 * Notion): una cella quadrata con dentro un'immagine, scelta come l'icona di
 * una pagina ("Upload" dalla galleria, "Remove") ma **intera**: si vede
 * tutta, piccola, senza essere ritagliata. Il valore salvato è il nome del
 * file, nella stessa cartella delle icone e delle copertine.
 */
@Composable
fun ImageCell(
    value: String,
    onValueChange: (String) -> Unit,
    /** Il nome della proprietà: il titolo della finestra per sceglierla. */
    title: String,
    /** Nella tabella la cella è un quadrato alto quanto la riga; nella scheda della riga un riquadro da 44. */
    inTable: Boolean
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    val locked = LocalDatabaseLocked.current
    var picking by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = IMAGE_CELL_SIZE)
            .clickable(enabled = !locked) { picking = true }
            .padding(IMAGE_CELL_PADDING),
        contentAlignment = if (inTable) LocalCellAlignment.current.box else Alignment.CenterStart
    ) {
        if (value.isNotEmpty()) {
            PageImage(
                fileName = value,
                store = store,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                placeholder = false,
                modifier = Modifier.size(IMAGE_CELL_SIZE - IMAGE_CELL_PADDING * 2)
            )
        }
    }
    if (picking) {
        PageImageSheet(
            target = PageImageTarget.ICON,
            title = title,
            hasImage = value.isNotEmpty(),
            store = store,
            onPicked = { picked ->
                val previous = value.takeIf { it.isNotEmpty() && it != picked }
                onValueChange(picked.orEmpty())
                // Il file vecchio se ne va: solo questa cella lo usava
                // (copiando righe e database il file si copia).
                previous
            },
            onReposition = null,
            onDismiss = { picking = false },
            onView = { viewing = true }
        )
    }
    if (viewing && value.isNotEmpty()) {
        FullScreenImage(fileName = value, store = store, title = title, onDismiss = { viewing = false })
    }
}

/**
 * **L'immagine a tutto schermo** (dal 29/09/2026), intera sul nero: con due
 * dita la si ingrandisce e la si sposta, con due tocchi si torna com'era.
 * Si chiude con la X o col gesto indietro. `onDownload` (dal 06/10/2026,
 * per le immagini delle pagine) mette accanto alla X il pulsante per
 * scaricarla.
 */
@Composable
fun FullScreenImage(
    fileName: String,
    store: PageImageStore,
    title: String,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 6f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    })
                }
        ) {
            PageImage(
                fileName = fileName,
                store = store,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                placeholder = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                if (onDownload != null) {
                    IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.Download, contentDescription = BlockStrings.download, tint = Color.White)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = EditorStrings.close, tint = Color.White)
                }
            }
        }
    }
}

/** Il quadrato della cella: alto quanto una riga della tabella, e l'immagine dentro coi suoi margini. */
val IMAGE_CELL_SIZE = 44.dp
private val IMAGE_CELL_PADDING = 4.dp
