package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageVersionInfo
import com.gabriele.notionlocal.data.entity.VersionReason
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.history.VersionBlock
import com.gabriele.notionlocal.data.history.VersionContent
import com.gabriele.notionlocal.data.history.plainTextOf
import com.gabriele.notionlocal.data.history.toEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.HistoryStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.VersionPreview
import java.time.Instant
import java.time.LocalDate

// ---------------------------------------------------------------------------
// **"Page history"** (dal 02/11/2026, chiesto dall'utente: "Cronologia con
// ripristino. Updates ti mostra cosa è cambiato, ma non riporta una pagina a
// com'era ieri"). Dai tre puntini della pagina, sotto "Updates": le versioni
// salvate (vedi `PageVersionEntity`), divise per giorno, dalla più recente.
// Toccandone una si legge com'era la pagina, e "Restore this version" la
// rimette, dopo la domanda; com'è adesso finisce prima nella cronologia.
// ---------------------------------------------------------------------------

const val PAGE_HISTORY_TAG = "pageHistory"
const val VERSION_ROW_TAG = "versionRow"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageHistorySheet(
    versions: List<PageVersionInfo>,
    /** Il contenuto di una versione; null se non si legge. */
    loadPreview: suspend (String) -> VersionPreview?,
    /** False con la pagina bloccata: le versioni si leggono, non si rimettono. */
    canRestore: Boolean,
    /** Rimette la versione; la funzione data dice com'è andata. */
    onRestore: (PageVersionInfo, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var opened by remember { mutableStateOf<PageVersionInfo?>(null) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PAGE_HISTORY_TAG)
                .navigationBarsPadding()
        ) {
            val version = opened
            if (version == null) {
                VersionList(versions, onOpen = { opened = it })
            } else {
                VersionPage(
                    version = version,
                    loadPreview = loadPreview,
                    canRestore = canRestore,
                    onBack = { opened = null },
                    onRestore = { done -> onRestore(version, done) }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.VersionList(versions: List<PageVersionInfo>, onOpen: (PageVersionInfo) -> Unit) {
    Text(
        text = HistoryStrings.pageHistory,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Text(
        text = HistoryStrings.note,
        style = MaterialTheme.typography.bodySmall,
        color = NotionGray400,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
    )
    if (versions.isEmpty()) {
        Text(
            text = HistoryStrings.noVersions,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    val zone = AppSettings.zoneId
    val today = LocalDate.now(zone)
    val byDay = versions.groupBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .padding(horizontal = 8.dp)
    ) {
        byDay.forEach { (day, ofDay) ->
            item(key = "day-$day") {
                Text(
                    text = when (day) {
                        today -> HistoryStrings.today
                        today.minusDays(1) -> HistoryStrings.yesterday
                        else -> Formats.weekdayAndDate(ofDay.first().at)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = NotionGray400,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            items(ofDay, key = { it.id }) { version -> VersionRow(version, onClick = { onOpen(version) }) }
        }
    }
}

@Composable
private fun VersionRow(version: PageVersionInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(VERSION_ROW_TAG)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Formats.time(version.at),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(72.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = version.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = HistoryStrings.linesAndWords(version.lines, version.words) +
                    when (version.reason) {
                        VersionReason.BEFORE_RESTORE -> " · ${HistoryStrings.beforeRestoring}"
                        VersionReason.BEFORE_SYNC -> " · ${HistoryStrings.beforeSyncing}"
                        else -> ""
                    },
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400
            )
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ColumnScope.VersionPage(
    version: PageVersionInfo,
    loadPreview: suspend (String) -> VersionPreview?,
    canRestore: Boolean,
    onBack: () -> Unit,
    onRestore: ((Boolean) -> Unit) -> Unit
) {
    // `Loading` mentre si legge; poi il contenuto, o `Unreadable`.
    var preview by remember(version.id) { mutableStateOf<PreviewState>(PreviewState.Loading) }
    LaunchedEffect(version.id) {
        preview = loadPreview(version.id)?.let { PreviewState.Ready(it) } ?: PreviewState.Unreadable
    }
    var confirming by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    val moment = Formats.dateTime(version.at)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back) }
        Column {
            Text(moment, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                HistoryStrings.linesAndWords(version.lines, version.words),
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400
            )
        }
    }
    when (val state = preview) {
        PreviewState.Loading -> Spacer(modifier = Modifier.size(120.dp))
        PreviewState.Unreadable -> Text(HistoryStrings.unreadable, modifier = Modifier.padding(16.dp))
        // L'anteprima prende quello che resta: il pulsante sotto si vede sempre.
        is PreviewState.Ready -> VersionText(state.preview, Modifier.weight(1f, fill = false))
    }
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    Text(
        text = if (canRestore) HistoryStrings.restoreNote else HistoryStrings.lockedNote,
        style = MaterialTheme.typography.bodySmall,
        color = NotionGray400,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
    )
    Button(
        onClick = { confirming = true },
        enabled = canRestore && !restoring && preview is PreviewState.Ready,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(HistoryStrings.restoreThisVersion)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(HistoryStrings.restoreTitle) },
            text = { Text(HistoryStrings.restoreText(moment)) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    restoring = true
                    onRestore { ok -> restoring = false; if (!ok) preview = PreviewState.Unreadable }
                }) { Text(Strings.restore) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(Strings.cancel) } }
        )
    }
}

private sealed interface PreviewState {
    data object Loading : PreviewState
    data object Unreadable : PreviewState
    data class Ready(val preview: VersionPreview) : PreviewState
}

/** La pagina com'era: il titolo e le sue righe, rientrate come nella pagina. */
@Composable
private fun VersionText(preview: VersionPreview, modifier: Modifier) {
    val lines = remember(preview) { versionOutline(preview.content, preview.linkedTitles) }
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item(key = "title") {
            Text(
                text = preview.content.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(lines.size) { i ->
            val line = lines[i]
            if (line.kind == OutlineKind.DIVIDER) {
                HorizontalDivider(modifier = Modifier.padding(start = (16 * line.depth).dp, top = 8.dp, bottom = 8.dp))
                return@items
            }
            Row(
                modifier = Modifier.padding(start = (16 * line.depth).dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                if (line.marker.isNotEmpty()) {
                    Text(line.marker, color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 6.dp))
                }
                Text(
                    text = line.text,
                    color = if (line.kind == OutlineKind.MUTED) NotionGray400 else NotionWhite,
                    fontFamily = if (line.kind == OutlineKind.CODE) FontFamily.Monospace else null,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

enum class OutlineKind { TEXT, CODE, MUTED, DIVIDER }

/** Una riga dell'anteprima: quanto rientra, il segno davanti (•, 1., ☐…) e il testo. */
data class OutlineLine(val depth: Int, val marker: String, val text: String, val kind: OutlineKind = OutlineKind.TEXT)

/**
 * **Le righe di una versione, come si leggono nella pagina**: nell'ordine
 * della pagina, i figli di un toggle sotto di lui e rientrati, col segno
 * del loro tipo davanti. Le tabelle una riga per riga; le immagini, i file
 * e i collegamenti col loro nome.
 */
fun versionOutline(content: VersionContent, linkedTitles: Map<String, String>): List<OutlineLine> {
    val byParent = content.blocks.groupBy { it.parentBlockId }
    val cellsByBlock = content.cells.groupBy { it.blockId }
    val result = ArrayList<OutlineLine>()
    fun visit(parentId: String?, depth: Int) {
        // Il numero di ogni elenco numerato, per rientro: un'altra riga allo
        // stesso rientro lo fa ripartire.
        val counters = HashMap<Int, Int>()
        for (block in byParent[parentId].orEmpty().sortedBy { it.orderIndex }) {
            val indent = block.indentLevel
            counters.keys.removeAll { it > indent }
            val number = if (block.type == BlockType.NUMBERED_LIST_ITEM) {
                (if (block.numberResetHere || block.numberStartsAt != null) block.numberStartsAt ?: 1 else (counters[indent] ?: 0) + 1)
                    .also { counters[indent] = it }
            } else {
                counters.remove(indent)
                0
            }
            result += linesOf(block, depth + indent, number, cellsByBlock[block.id].orEmpty(), linkedTitles)
            // Le righe di un blocco sincronizzato stanno al suo posto, senza rientro.
            visit(block.id, depth + indent + if (block.type == BlockType.SYNCED) 0 else 1)
        }
    }
    visit(null, 0)
    return result
}

private fun linesOf(
    block: VersionBlock,
    depth: Int,
    number: Int,
    cells: List<com.gabriele.notionlocal.data.history.VersionCell>,
    linkedTitles: Map<String, String>
): List<OutlineLine> {
    val text = plainTextOf(block)
    fun one(marker: String, shown: String = text, kind: OutlineKind = OutlineKind.TEXT) = listOf(OutlineLine(depth, marker, shown, kind))
    fun named(marker: String, english: String): List<OutlineLine> {
        val attrs = block.toEntity("").attrs
        return one(marker, attrs.name ?: attrs.url ?: text.ifBlank { EditorStrings.blockType(english) }, OutlineKind.MUTED)
    }
    return when (block.type) {
        BlockType.PARAGRAPH -> one("")
        BlockType.BULLET_LIST_ITEM -> one("•")
        BlockType.NUMBERED_LIST_ITEM -> one("$number.")
        BlockType.CHECKBOX -> one(if (block.isChecked) "☑" else "☐")
        BlockType.TOGGLE -> one("▸")
        BlockType.QUOTE -> one("│")
        BlockType.CALLOUT -> one(block.toEntity("").attrs.icon ?: "💡")
        BlockType.CODE -> one("", kind = OutlineKind.CODE)
        BlockType.NOTIFICATION -> one("🔔")
        BlockType.BUTTON -> one("▢")
        BlockType.SYNCED -> emptyList()
        BlockType.PAGE_MENTION -> one("@")
        BlockType.DIVIDER -> one("", "", OutlineKind.DIVIDER)
        BlockType.PAGE_LINK -> one("📄", block.linkedPageId?.let { linkedTitles[it] }?.ifBlank { Strings.untitled } ?: text.ifBlank { Strings.untitled })
        BlockType.DATABASE_LINK -> one("▦", block.linkedPageId?.let { linkedTitles[it] }?.ifBlank { Strings.untitled } ?: EditorStrings.blockType("Database"))
        BlockType.IMAGE -> named("🖼", "Image")
        BlockType.VIDEO -> named("🎬", "Video")
        BlockType.AUDIO -> named("🔊", "Audio")
        BlockType.FILE -> named("📎", "File")
        BlockType.BOOKMARK -> named("🔖", "Web bookmark")
        BlockType.TABLE -> {
            val byCell = cells.associateBy { it.rowIndex to it.colIndex }
            (0 until block.tableRows).map { row ->
                OutlineLine(depth, "", (0 until block.tableCols).joinToString("  │  ") { col -> byCell[row to col]?.text.orEmpty() })
            }
        }
    }
}
