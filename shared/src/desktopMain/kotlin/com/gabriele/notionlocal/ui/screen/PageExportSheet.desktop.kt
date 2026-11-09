package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.ExportedFile

// shortcut: sul PC l'esportazione si chiude subito (PDF e Markdown sono fatti col telefono), da fare con l'esportazione del PC.
@Composable
actual fun PageExportSheet(
    title: String,
    hasSubpages: Boolean,
    hasDrawings: Boolean,
    export: suspend (ExportOptions) -> ExportedFile?,
    onDismiss: () -> Unit,
    share: ((ExportedFile) -> Unit)?
) {
    LaunchedEffect(Unit) { onDismiss() }
}
