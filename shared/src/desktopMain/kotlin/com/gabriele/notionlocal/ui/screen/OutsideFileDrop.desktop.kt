package com.gabriele.notionlocal.ui.screen

import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetDragEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.geometry.Offset
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.importOutsideFile
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

/**
 * Sul PC: i file portati da Esplora risorse sopra la pagina (vedi
 * `OutsideFileDrop.kt`). Mentre si trascina, il punto del mouse; lasciandoli,
 * la pagina dice dove vanno, i file si copiano nell'app e diventano blocchi.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
actual fun Modifier.outsideFileDrop(
    store: PageImageStore,
    onMove: (Offset?) -> Unit,
    onDrop: (Offset) -> ((List<DroppedFile>) -> Unit)?
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val latestMove by rememberUpdatedState(onMove)
    val latestDrop by rememberUpdatedState(onDrop)
    val target = remember(store) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = latestMove(event.windowPosition)
            override fun onMoved(event: DragAndDropEvent) = latestMove(event.windowPosition)
            override fun onExited(event: DragAndDropEvent) = latestMove(null)
            override fun onEnded(event: DragAndDropEvent) = latestMove(null)
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val files = filesOf(event)
                if (files.isEmpty()) return false
                val at = event.windowPosition ?: return false
                val place = latestDrop(at) ?: return false
                scope.launch {
                    val blocks = files.mapNotNull { file -> importOutsideFile(store, file)?.let { (type, attrs) -> DroppedFile(type, attrs) } }
                    if (blocks.isNotEmpty()) place(blocks)
                }
                return true
            }
        }
    }
    dragAndDropTarget(shouldStartDragAndDrop = { filesOf(it).isNotEmpty() }, target = target)
}

/**
 * Il punto del mouse nella finestra, come lo conta Compose: il punto che dà
 * Windows per la scala dello schermo (Compose lo tiene per sé).
 */
@OptIn(ExperimentalComposeUiApi::class)
private val DragAndDropEvent.windowPosition: Offset?
    get() {
        val (point, component) = when (val native = nativeEvent) {
            is DropTargetDragEvent -> native.location to native.dropTargetContext.component
            is DropTargetDropEvent -> native.location to native.dropTargetContext.component
            else -> return null
        }
        val scale = component.graphicsConfiguration?.defaultTransform?.scaleX?.toFloat() ?: 1f
        return Offset(point.x * scale, point.y * scale)
    }

/** I file (non le cartelle) trascinati, da quello che manda Windows. */
@OptIn(ExperimentalComposeUiApi::class)
private fun filesOf(event: DragAndDropEvent): List<File> =
    (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
        .mapNotNull { uri -> runCatching { File(URI(uri)) }.getOrNull() }
        .filter { it.isFile }
