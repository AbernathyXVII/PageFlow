package com.gabriele.notionlocal.ui.screen

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
import com.gabriele.notionlocal.ui.i18n.ExportStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PlatformContext

const val PAGE_EXPORT_TAG = "page_export"

/**
 * **"Export"** nel menu "..." di una pagina (dal 02/11/2026, chiesto
 * dall'utente: "Serve per mandare una pagina a qualcuno, stamparla, o non
 * restare chiuso dentro l'app"). Si sceglie il formato — PDF o Markdown — e
 * cosa farne: mandarlo con un'altra app, salvarlo in una cartella del
 * telefono (o su Drive), o stamparlo (solo il PDF).
 *
 * [export] prepara il file (vedi `PageExporter`); null se non riesce.
 *
 * Nel codice comune dal 09/11/2026: sul PC "Save" apre la finestra "Salva
 * con nome" di Windows, "Share" non c'è e, finché il PC non sa fare il PDF,
 * c'è solo il Markdown (vedi `ExportPlatformParts`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageExportSheet(
    title: String,
    /** Se la pagina ha delle sottopagine: allora c'è "Include subpages". */
    hasSubpages: Boolean,
    /** Se la pagina ha dei disegni a mano: allora, per il PDF, c'è "Include drawings" (dal 03/11/2026). */
    hasDrawings: Boolean = false,
    export: suspend (ExportOptions) -> ExportedFile?,
    onDismiss: () -> Unit,
    /** Manda il file a un'altra app; di suo con la scelta di sistema (`shareExport`). */
    share: ((ExportedFile) -> Unit)? = null
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var format by remember { mutableStateOf(if (canExportPdf) ExportFormat.PDF else ExportFormat.MARKDOWN) }
    var withSubpages by remember { mutableStateOf(false) }
    var withDrawings by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val shownTitle = title.ifBlank { Strings.untitled }

    val save = rememberExportSaver { saved ->
        showMessage(context, if (saved) ExportStrings.saved else ExportStrings.couldNotExport)
        if (saved) onDismiss()
    }
    val canShare = share != null || canShareExports

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
            if (canExportPdf) {
                FormatLine("PDF", ExportStrings.pdfHint, format == ExportFormat.PDF) { format = ExportFormat.PDF }
            }
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
                        onClick = { prepare(single = true) { printExport(context, it, shownTitle); onDismiss() } },
                        enabled = !working,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(ExportStrings.print, maxLines = 1)
                    }
                }
                if (canShare) {
                    OutlinedButton(
                        onClick = { prepare { save(it) } },
                        enabled = !working,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(ExportStrings.save, maxLines = 1)
                    }
                }
                // Dove non si può mandare (il PC) il pulsante pieno è "Save".
                Button(
                    onClick = {
                        if (canShare) {
                            prepare { file ->
                                share?.invoke(file) ?: shareExport(context, file, shownTitle)
                                onDismiss()
                            }
                        } else {
                            prepare { save(it) }
                        }
                    },
                    enabled = !working,
                    modifier = Modifier.weight(1f)
                ) {
                    if (working) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (canShare) ExportStrings.share else ExportStrings.save, maxLines = 1)
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

/** **"Save"**: il file esportato dove si sceglie, col suo nome; poi [onDone] (vero se è salvato). */
@Composable
expect fun rememberExportSaver(onDone: (Boolean) -> Unit): (ExportedFile) -> Unit

/** Se si può mandare un file a un'altra app con la scelta del sistema (sul PC no). */
expect val canShareExports: Boolean

/** **"Share"**: il file a un'altra app (WhatsApp, la posta, Drive…). */
expect fun shareExport(context: PlatformContext, file: ExportedFile, title: String)

/** **"Print"**: il PDF alla finestra di stampa del sistema. */
expect fun printExport(context: PlatformContext, file: ExportedFile, title: String)

/** Se questo sistema sa fare il PDF (vedi `pdfWriter`). */
expect val canExportPdf: Boolean
