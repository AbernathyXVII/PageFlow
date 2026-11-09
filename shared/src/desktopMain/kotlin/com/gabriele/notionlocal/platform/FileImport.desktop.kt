package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URLConnection
import java.nio.file.Files

/** La finestra di Windows o del Mac per scegliere un file (con [extensions], se non è vuoto). */
private fun chooseFile(title: String, extensions: Set<String>): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
        if (extensions.isNotEmpty()) setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in extensions }
        isVisible = true
    }
    val name = dialog.file ?: return null
    return File(dialog.directory, name)
}

private val IMAGES = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
private val VIDEOS = setOf("mp4", "m4v", "mov", "webm", "mkv", "avi", "3gp")
private val AUDIOS = setOf("mp3", "m4a", "aac", "wav", "ogg", "oga", "opus", "flac", "amr")

@Composable
actual fun rememberImageImporter(
    store: PageImageStore,
    onStart: () -> Unit,
    onImported: (Result<PickedImage>) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val imported by rememberUpdatedState(onImported)
    return {
        chooseFile(EditorStrings.upload, IMAGES)?.let { file ->
            start()
            scope.launch {
                val saved = store.saveFromStream({ file.inputStream() }, mustBeImage = true, cannotOpen = EditorStrings.imageReadFailed)
                imported(saved.map { PickedImage(it, file.name) })
            }
        }
    }
}

@Composable
actual fun rememberFileImporter(
    store: PageImageStore,
    kind: FileKind,
    onStart: () -> Unit,
    onImported: (BlockAttrs?) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val imported by rememberUpdatedState(onImported)
    return {
        val extensions = when (kind) {
            FileKind.VIDEO -> VIDEOS
            FileKind.AUDIO -> AUDIOS
            FileKind.ANY -> emptySet()
        }
        chooseFile(BlockStrings.chooseFile, extensions)?.let { file ->
            start()
            scope.launch {
                val name = file.name
                val saved = store.saveFromStream(
                    { file.inputStream() },
                    extension = store.cleanExtension(name.substringAfterLast('.', "")),
                    mustBeImage = false,
                    cannotOpen = BlockStrings.fileReadFailed
                ).getOrNull()
                val mime = withContext(Dispatchers.IO) {
                    runCatching { Files.probeContentType(file.toPath()) }.getOrNull() ?: URLConnection.guessContentTypeFromName(name)
                }
                imported(saved?.let { BlockAttrs(file = it, name = name, mime = mime, size = store.fileFor(it).length()) })
            }
        }
    }
}

@Composable
actual fun rememberSaveCopy(
    store: PageImageStore,
    mime: String?,
    onDone: (Boolean) -> Unit
): (file: String, name: String) -> Unit {
    val scope = rememberCoroutineScope()
    val done by rememberUpdatedState(onDone)
    return { fileName, name ->
        val dialog = FileDialog(null as Frame?, BlockStrings.download, FileDialog.SAVE).apply {
            file = name
            isVisible = true
        }
        val chosen = dialog.file
        if (chosen != null) {
            val target = File(dialog.directory, chosen)
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching { store.fileFor(fileName).copyTo(target, overwrite = true) }.isSuccess
                }
                done(saved)
            }
        }
    }
}
