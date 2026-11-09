package com.gabriele.notionlocal.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.export.ExportFormat
import com.gabriele.notionlocal.data.export.ExportedFile
import com.gabriele.notionlocal.data.export.PageExporter
import com.gabriele.notionlocal.data.export.SaveExport
import com.gabriele.notionlocal.ui.i18n.ExportStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext

const val PAGE_EXPORT_TAG = "page_export"

/**
 * **"Export"** nel menu "..." di una pagina (dal 02/11/2026, chiesto
 * dall'utente: "Serve per mandare una pagina a qualcuno, stamparla, o non
 * restare chiuso dentro l'app"). Si sceglie il formato — PDF o Markdown — e
 * cosa farne: mandarlo con un'altra app, salvarlo in una cartella del
 * telefono (o su Drive), o stamparlo (solo il PDF).
 *
 * [export] prepara il file (vedi `PageExporter`); null se non riesce.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun PageExportSheet(
    title: String,
    hasSubpages: Boolean,
    hasDrawings: Boolean,
    export: suspend (ExportOptions) -> ExportedFile?,
    onDismiss: () -> Unit,
    share: ((ExportedFile) -> Unit)?
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var format by remember { mutableStateOf(ExportFormat.PDF) }
    var withSubpages by remember { mutableStateOf(false) }
    var withDrawings by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val shownTitle = title.ifBlank { Strings.untitled }

    var saving by remember { mutableStateOf<ExportedFile?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(SaveExport()) { uri ->
        val file = saving
        saving = null
        if (uri == null || file == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.Main.immediate) {
            val saved = PageExporter.copyTo(context, file, uri)
            showMessage(context, if (saved) ExportStrings.saved else ExportStrings.couldNotExport)
            if (saved) onDismiss()
        }
    }

    /** Prepara il file e poi fa [then]; uno alla volta. Per la stampa, [single]: un PDF solo. */
    fun prepare(single: Boolean = false, then: (ExportedFile) -> Unit) {
        if (working) return
        working = true
        val options = ExportOptions(
            format = format,
            subpages = withSubpages && hasSubpages,
            drawings = withDrawings && hasDrawings && format == ExportFormat.PDF,
            singleFile = single
        )
        scope.launch(Dispatchers.Main.immediate) {
            val file = runCatching { export(options) }.getOrNull()
            working = false
            if (file == null) {
                showMessage(context, ExportStrings.couldNotExport)
            } else {
                runCatching { then(file) }.onFailure {
                    showMessage(context, ExportStrings.couldNotExport)
                }
            }
        }
    }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PAGE_EXPORT_TAG)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(ExportStrings.exportPage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                shownTitle,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.size(8.dp))
            FormatLine("PDF", ExportStrings.pdfHint, format == ExportFormat.PDF) { format = ExportFormat.PDF }
            FormatLine("Markdown", ExportStrings.markdownHint, format == ExportFormat.MARKDOWN) { format = ExportFormat.MARKDOWN }
            if (hasDrawings && format == ExportFormat.PDF) {
                SwitchLine(ExportStrings.includeDrawings, ExportStrings.includeDrawingsHint, withDrawings) { withDrawings = it }
            }
            if (hasSubpages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { withSubpages = !withSubpages }
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ExportStrings.includeSubpages, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = withSubpages, onCheckedChange = { withSubpages = it })
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (format == ExportFormat.PDF) {
                    OutlinedButton(
                        onClick = { prepare(single = true) { PageExporter.print(context, it, shownTitle); onDismiss() } },
                        enabled = !working,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(ExportStrings.print, maxLines = 1)
                    }
                }
                OutlinedButton(
                    onClick = { prepare { saving = it; saveLauncher.launch(it) } },
                    enabled = !working,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(ExportStrings.save, maxLines = 1)
                }
                Button(
                    onClick = {
                        prepare { file ->
                            share?.invoke(file) ?: PageExporter.share(context, file, shownTitle)
                            onDismiss()
                        }
                    },
                    enabled = !working,
                    modifier = Modifier.weight(1f)
                ) {
                    if (working) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(ExportStrings.share, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SwitchLine(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FormatLine(label: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
        }
    }
}
