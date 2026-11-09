package com.gabriele.notionlocal.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.data.export.ExportActions
import com.gabriele.notionlocal.data.export.ExportedFile
import com.gabriele.notionlocal.data.export.SaveExport
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberExportSaver(onDone: (Boolean) -> Unit): (ExportedFile) -> Unit {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf<ExportedFile?>(null) }
    val launcher = rememberLauncherForActivityResult(SaveExport()) { uri ->
        val file = saving
        saving = null
        if (uri == null || file == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.Main.immediate) { onDone(ExportActions.copyTo(context, file, uri)) }
    }
    return { file ->
        saving = file
        launcher.launch(file)
    }
}

actual val canShareExports: Boolean = true

actual fun shareExport(context: PlatformContext, file: ExportedFile, title: String) = ExportActions.share(context, file, title)

actual fun printExport(context: PlatformContext, file: ExportedFile, title: String) = ExportActions.print(context, file, title)

actual val canExportPdf: Boolean = true
