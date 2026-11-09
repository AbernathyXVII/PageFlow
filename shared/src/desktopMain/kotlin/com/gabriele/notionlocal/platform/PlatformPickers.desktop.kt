package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalWindowInfo
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI

/** Sul PC: la finestra di Windows o del Mac per scegliere un file. */
@Composable
actual fun rememberImagePicker(
    store: PageImageStore,
    onStart: () -> Unit,
    onPicked: (Result<String>) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val start by rememberUpdatedState(onStart)
    val picked by rememberUpdatedState(onPicked)
    return remember(store) {
        {
            val dialog = FileDialog(null as Frame?, EditorStrings.upload, FileDialog.LOAD).apply {
                setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS }
                isVisible = true
            }
            val name = dialog.file
            if (name != null) {
                val file = File(dialog.directory, name)
                start()
                scope.launch {
                    picked(store.saveFromStream({ file.inputStream() }, mustBeImage = true, cannotOpen = EditorStrings.imageReadFailed))
                }
            }
        }
    }
}

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")

actual fun urlHost(url: String): String? = runCatching { URI(url).host }.getOrNull()

actual fun urlScheme(url: String): String? = runCatching { URI(url).scheme }.getOrNull()

actual fun urlLastPathSegment(url: String): String? =
    runCatching { URI(url).path?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotEmpty() } }.getOrNull()

@Composable
actual fun rememberRootHeight(): () -> Int {
    val info = LocalWindowInfo.current
    return remember(info) { { info.containerSize.height } }
}
