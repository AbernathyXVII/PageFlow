package com.gabriele.notionlocal.platform

import java.io.File
import com.gabriele.notionlocal.data.entity.BlockType
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
import java.net.URLConnection
import java.nio.file.Files

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
        scope.launch {
            val file = SystemFileDialog.open(EditorStrings.upload, IMAGES) ?: return@launch
            start()
            val saved = store.saveFromStream({ file.inputStream() }, mustBeImage = true, cannotOpen = EditorStrings.imageReadFailed)
            imported(saved.map { PickedImage(it, file.name) })
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
        scope.launch {
            val file = SystemFileDialog.open(BlockStrings.chooseFile, extensions) ?: return@launch
            start()
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

@Composable
actual fun rememberSaveCopy(
    store: PageImageStore,
    mime: String?,
    onDone: (Boolean) -> Unit
): (file: String, name: String) -> Unit {
    val scope = rememberCoroutineScope()
    val done by rememberUpdatedState(onDone)
    return { fileName, name ->
        scope.launch {
            val target = SystemFileDialog.save(BlockStrings.download, name) ?: return@launch
            val saved = withContext(Dispatchers.IO) {
                runCatching { store.fileFor(fileName).copyTo(target, overwrite = true) }.isSuccess
            }
            done(saved)
        }
    }
}

/**
 * **Un file trascinato da fuori dentro la pagina** (dal 13/11/2026, vedi
 * `OutsideFileDrop.kt`), copiato nell'app: il tipo di blocco che diventa e i
 * suoi dati. Un'immagine che non si legge diventa un file. Null se non si
 * riesce a copiarlo.
 */
suspend fun importOutsideFile(store: PageImageStore, file: File): Pair<BlockType, BlockAttrs>? {
    if (!file.isFile) return null
    val extension = file.extension.lowercase()
    if (extension in IMAGES) {
        val image = store.saveFromStream({ file.inputStream() }, mustBeImage = true, cannotOpen = EditorStrings.imageReadFailed).getOrNull()
        if (image != null) return BlockType.IMAGE to BlockAttrs(file = image, name = file.name)
    }
    val saved = store.saveFromStream(
        { file.inputStream() },
        extension = store.cleanExtension(extension),
        mustBeImage = false,
        cannotOpen = BlockStrings.fileReadFailed
    ).getOrNull() ?: return null
    val mime = withContext(Dispatchers.IO) {
        runCatching { Files.probeContentType(file.toPath()) }.getOrNull() ?: URLConnection.guessContentTypeFromName(file.name)
    }
    val type = when (extension) {
        in VIDEOS -> BlockType.VIDEO
        in AUDIOS -> BlockType.AUDIO
        else -> BlockType.FILE
    }
    return type to BlockAttrs(file = saved, name = file.name, mime = mime, size = store.fileFor(saved).length())
}
