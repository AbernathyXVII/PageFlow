package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import kotlinx.serialization.json.Json

// ---------------------------------------------------------------------------
// **Esportare una pagina** (dal 02/11/2026, chiesto dall'utente: "Export in PDF
// o Markdown. Serve per mandare una pagina a qualcuno, stamparla, o non
// restare chiuso dentro l'app"). Qui c'è quello che i due formati hanno in
// comune: la pagina letta dal database e le sue righe in ordine di lettura.
// Il Markdown lo scrive `MarkdownExport`, il PDF `PdfExport`, il file lo
// prepara `PageExporter`.
// ---------------------------------------------------------------------------

enum class ExportFormat(val extension: String, val mime: String) {
    PDF("pdf", "application/pdf"),
    MARKDOWN("md", "text/markdown")
}

/** Una pagina da esportare: il suo contenuto e, se richieste, le sottopagine. */
data class ExportPage(
    val page: PageEntity,
    val blocks: List<BlockEntity>,
    val cells: List<TableCellEntity>,
    /** I titoli delle pagine e dei database a cui le righe portano. */
    val linkedTitles: Map<String, String> = emptyMap(),
    /** Le sottopagine, nell'ordine in cui la pagina le mostra; vuoto se non richieste. */
    val subpages: List<ExportPage> = emptyList(),
    /** I database della pagina, per id del blocco che li mostra (vedi `DatabaseExport.kt`). */
    val databases: Map<String, ExportDatabase> = emptyMap(),
    /** I bottoni, per id del blocco: il nome e cosa fanno (vedi `ButtonExport.kt`). */
    val buttons: Map<String, ExportButton> = emptyMap(),
    /** I disegni a mano, se si esportano (solo nel PDF). */
    val strokes: List<InkStrokeEntity> = emptyList()
)

/**
 * **Un database come lo mostra la sua vista** (dal 03/11/2026, chiesto
 * dall'utente: "il database sarà esportato come lo fa Notion"): le colonne
 * che si vedono, Name compreso, nell'ordine della tabella, e le righe
 * filtrate e ordinate, coi valori scritti come nelle celle (date, numeri,
 * formule, relazioni). Per il CSV, tutte le colonne, anche quelle nascoste.
 */
data class ExportDatabase(
    val title: String,
    val headers: List<String>,
    val rows: List<ExportDatabaseRow>,
    val csvHeaders: List<String> = headers,
    /** Dove sta Name fra le colonne di `headers`. */
    val nameIndex: Int = 0
)

data class ExportDatabaseRow(
    val title: String,
    /** I valori delle colonne di `headers`, Name compreso. */
    val cells: List<String>,
    /** I valori delle colonne di `csvHeaders`. */
    val csvCells: List<String> = cells,
    /** La pagina della riga, se ha qualcosa di scritto: diventa un file suo. */
    val page: ExportPage? = null
)

/** **Un bottone**: il suo nome e, una per riga, le azioni che fa toccandolo. */
data class ExportButton(val name: String, val steps: List<String>)

/** Se una pagina ha qualcosa: una riga scritta, o un blocco che non sia una riga vuota, o un disegno. */
fun hasContent(page: ExportPage): Boolean =
    page.strokes.isNotEmpty() || page.blocks.any { block ->
        block.type != BlockType.PARAGRAPH || spansOf(block).any { it.text.isNotBlank() }
    }

/** Una riga della pagina, in ordine di lettura. */
data class ExportLine(
    val block: BlockEntity,
    val spans: List<RichTextSpan>,
    /** Il rientro: quello della riga più quello delle righe che la contengono (un interruttore). */
    val depth: Int,
    /** Il numero, per un elenco numerato; 0 per le altre righe. */
    val number: Int,
    /** Le celle, per una tabella. */
    val cells: List<TableCellEntity>
) {
    val text: String get() = spans.joinToString("") { it.text }
}

private val spansJson = Json { ignoreUnknownKeys = true }

fun spansOf(block: BlockEntity): List<RichTextSpan> =
    runCatching { spansJson.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrDefault(emptyList())

/**
 * Le righe di [page] in ordine di lettura, come le mostra l'editor: anche
 * quelle dentro un interruttore chiuso, perché fuori dall'app non si apre.
 * Gli elenchi numerati contano come nell'editor: un'altra riga allo stesso
 * rientro fa ripartire il numero.
 */
fun exportLines(page: ExportPage): List<ExportLine> {
    val byParent = page.blocks.groupBy { it.parentBlockId }
    val cellsByBlock = page.cells.groupBy { it.blockId }
    val result = ArrayList<ExportLine>()
    fun visit(parentId: String?, depth: Int, seen: Set<String>) {
        val counters = HashMap<Int, Int>()
        for (block in byParent[parentId].orEmpty().sortedBy { it.orderIndex }) {
            if (block.id in seen) continue
            val indent = block.indentLevel
            // Un blocco sincronizzato è solo una cornice (dal 03/11/2026): le
            // sue righe si esportano al suo posto, come le altre. Così anche le
            // colonne (dal 12/11/2026): una dopo l'altra, come sul telefono.
            if (block.type == BlockType.SYNCED || block.type == BlockType.COLUMN_LIST || block.type == BlockType.COLUMN) {
                visit(block.id, depth + indent, seen + block.id)
                continue
            }
            counters.keys.removeAll { it > indent }
            val number = if (block.type == BlockType.NUMBERED_LIST_ITEM) {
                (if (block.numberResetHere || block.numberStartsAt != null) block.numberStartsAt ?: 1 else (counters[indent] ?: 0) + 1)
                    .also { counters[indent] = it }
            } else {
                counters.remove(indent)
                0
            }
            result += ExportLine(block, spansOf(block), depth + indent, number, cellsByBlock[block.id].orEmpty())
            visit(block.id, depth + indent + 1, seen + block.id)
        }
    }
    visit(null, 0, emptySet())
    return result
}

/** Le sottopagine che si esportano insieme alla pagina: le pagine figlie a cui portano le sue righe. */
fun subpageIds(page: ExportPage): List<String> =
    exportLines(page).mapNotNull { line ->
        line.block.linkedPageId?.takeIf { line.block.type == BlockType.PAGE_LINK }
    }.distinct()

/**
 * Il nome del file per una pagina: il titolo senza i caratteri che i
 * telefoni e i computer non accettano nei nomi (`/ \ : * ? " < > |`).
 */
fun exportFileName(title: String, untitled: String): String {
    val clean = title.replace(Regex("""[\\/:*?"<>|\u0000-\u001F]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trimEnd('.')
        .take(FILE_NAME_MAX)
        .trim()
    return clean.ifEmpty { untitled }
}

private const val FILE_NAME_MAX = 80
