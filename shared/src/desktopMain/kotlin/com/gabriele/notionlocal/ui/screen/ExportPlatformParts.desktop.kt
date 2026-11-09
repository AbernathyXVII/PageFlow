package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.gabriele.notionlocal.data.export.ExportedFile
import com.gabriele.notionlocal.data.export.defaultPdfBackend
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.ExportStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** "Save" sul PC: la finestra "Salva con nome" di Windows, col nome del file già scritto. */
@Composable
actual fun rememberExportSaver(onDone: (Boolean) -> Unit): (ExportedFile) -> Unit {
    val scope = rememberCoroutineScope()
    val done by rememberUpdatedState(onDone)
    return { exported ->
        val dialog = FileDialog(null as Frame?, ExportStrings.save, FileDialog.SAVE).apply {
            file = exported.name
            isVisible = true
        }
        dialog.file?.let { name ->
            val target = File(dialog.directory, name)
            scope.launch {
                done(withContext(Dispatchers.IO) { runCatching { exported.file.copyTo(target, overwrite = true) }.isSuccess })
            }
        }
    }
}

// Sul PC non c'è la scelta delle app a cui mandare un file: si salva e lo si manda da lì.
actual val canShareExports: Boolean = false

actual fun shareExport(context: PlatformContext, file: ExportedFile, title: String) = Unit

/** Il PDF alla stampante di Windows; se non si può, si apre nel programma dei PDF, che sa stamparlo. */
actual fun printExport(context: PlatformContext, file: ExportedFile, title: String) {
    runCatching { Desktop.getDesktop().print(file.file) }.onFailure { runCatching { Desktop.getDesktop().open(file.file) } }
}

actual val canExportPdf: Boolean get() = defaultPdfBackend() != null
