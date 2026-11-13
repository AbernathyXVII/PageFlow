package com.gabriele.notionlocal.data.repository

import com.gabriele.notionlocal.data.export.exportCellText
import com.gabriele.notionlocal.data.entity.titleColumnId
import com.gabriele.notionlocal.data.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.RowIconRef
import com.gabriele.notionlocal.data.chosenEmoji
import com.gabriele.notionlocal.data.dao.RowCover
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DATE_NOTIFY_FLAG
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.RelationConfig
import com.gabriele.notionlocal.data.entity.encodeRelationConfig
import com.gabriele.notionlocal.data.entity.encodeRelationIds
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationIds
import com.gabriele.notionlocal.data.entity.reverseChanges
import com.gabriele.notionlocal.data.notify.ReminderAlarms
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.gabriele.notionlocal.data.automations.DatabaseChanges

/**
 * Una riga di testo dell'anteprima di una scheda della galleria: il tipo
 * del blocco serve a disegnarla come nella pagina (un titolo più
 * marcato, un pallino davanti a un elemento di elenco, la casella).
 *
 * `ordinal` è il numero di un elemento di elenco numerato, contato come
 * lo conta la pagina (`PageEditorViewModel.numberedListOrdinal`); zero
 * per tutti gli altri tipi.
 */
data class RowPreviewLine(
    val type: BlockType,
    val text: String,
    val isChecked: Boolean,
    val indentLevel: Int,
    val ordinal: Int
)

/** Lo stesso tetto ai rientri dell'editor: oltre, i numeri si contano come all'ultimo livello. */
private const val PREVIEW_MAX_INDENT = 10

/**
 * Il testo di un blocco come va mostrato nell'anteprima: semplice, ma
 * **con i pezzi coperti ancora coperti**. Uno spoiler nella pagina è un
 * testo che l'utente ha scelto di non far leggere a colpo d'occhio, e
 * l'anteprima di una scheda è proprio un colpo d'occhio: ogni lettera
 * coperta diventa un quadratino, gli spazi restano, così si vede quanto
 * è lungo senza leggerlo.
 */
private fun List<RichTextSpan>.previewText(): String = joinToString("") { span ->
    if (span.spoiler) {
        span.text.map { if (it.isWhitespace()) it else '▒' }.joinToString("")
    } else {
        span.text
    }
}

/** I blocchi che hanno un testo da mostrare nell'anteprima. */
private val PREVIEW_TYPES = setOf(
    BlockType.PARAGRAPH,
    BlockType.BULLET_LIST_ITEM,
    BlockType.NUMBERED_LIST_ITEM,
    BlockType.CHECKBOX,
    BlockType.TOGGLE
)

/**
 * Punto unico di accesso ai dati delle pagine-database (tabelle con
 * campi personalizzati, tipo le Notion database view).
 */
class DatabaseRepository(private val db: AppDatabase) {

    private val dao = db.databaseDao()

    private val json = Json { ignoreUnknownKeys = true }

    fun getColumns(pageId: String): Flow<List<DatabaseColumnEntity>> =
        dao.getColumnsForPage(pageId)

    suspend fun addColumn(column: DatabaseColumnEntity) = dao.insertColumn(column)

    suspend fun updateColumn(column: DatabaseColumnEntity) = dao.updateColumn(column)

    /** Più colonne insieme, in una transazione: chi osserva la tabella riceve un avviso solo. */
    suspend fun updateColumns(columns: List<DatabaseColumnEntity>) {
        if (columns.isEmpty()) return
        db.withTransaction { columns.forEach { dao.updateColumn(it) } }
    }

    /**
     * Il nuovo ordine della tabella dopo uno spostamento: le colonne
     * rinumerate e, se è cambiato, il posto di Name (vedi `namePosition`).
     * Insieme, così la tabella si ridisegna una volta sola.
     */
    suspend fun reorderColumns(columns: List<DatabaseColumnEntity>, pageId: String, nameColumnOrder: Int?) {
        if (columns.isEmpty() && nameColumnOrder == null) return
        db.withTransaction {
            columns.forEach { dao.updateColumn(it) }
            if (nameColumnOrder != null) db.pageDao().setNameColumnOrder(pageId, nameColumnOrder)
        }
    }

    suspend fun deleteColumn(column: DatabaseColumnEntity) = dao.deleteColumn(column)

    /**
     * Le opzioni di una proprietà riscritte da sole, sulla proprietà com'è
     * adesso nel database: le azioni di un bottone (dal 27/10/2026), senza
     * rimettere com'era quello che nel frattempo è cambiato (il nome, la
     * larghezza).
     */
    suspend fun setColumnOptions(columnId: String, optionsJson: String) {
        val column = dao.getColumnById(columnId) ?: return
        if (column.optionsJson != optionsJson) dao.updateColumn(column.copy(optionsJson = optionsJson))
    }

    fun getRows(pageId: String): Flow<List<DatabaseRowEntity>> =
        dao.getRowsForPage(pageId)

    /** Riga → immagine dell'icona della sua pagina, per le righe che ne hanno una. */
    fun observeRowIcons(pageId: String): Flow<Map<String, RowIconRef>> =
        dao.observeRowIcons(pageId).map { icons ->
            icons.associate { it.rowId to RowIconRef(emoji = chosenEmoji(it.icon), image = it.iconImage, symbol = it.iconSymbol) }
        }

    /** Riga → copertina della sua pagina, con l'inquadratura, per le righe che ne hanno una. */
    fun observeRowCovers(pageId: String): Flow<Map<String, RowCover>> =
        dao.observeRowCovers(pageId).map { covers -> covers.associateBy { it.rowId } }

    /** Le righe la cui pagina è bloccata con la password. */
    fun observeLockedRowIds(pageId: String): Flow<Set<String>> =
        dao.observeLockedRowIds(pageId).map { it.toSet() }

    /**
     * Riga → le prime righe di testo della sua pagina, per l'anteprima
     * delle schede della galleria.
     *
     * Si prendono i blocchi che hanno del testo — paragrafi, titoli,
     * elenchi, caselle, toggle — e si saltano quelli che non ne hanno
     * (divisori, tabelle, collegamenti a pagine), fino a
     * `maxLinesPerRow`. Le righe vuote in testa si saltano anche loro:
     * una pagina che comincia con due a-capo avrebbe un'anteprima che
     * sembra vuota. Quelle in mezzo invece restano, perché separano i
     * paragrafi come nella pagina.
     *
     * Decodifica il testo solo dei blocchi che servono: gli altri si
     * scartano prima, e una pagina lunga non costa più di una corta.
     */
    fun observeRowContentPreviews(
        pageId: String,
        maxLinesPerRow: Int
    ): Flow<Map<String, List<RowPreviewLine>>> =
        dao.observeRowPreviewBlocks(pageId).map { blocks ->
            blocks.groupBy { it.rowId }.mapValues { (_, rowBlocks) ->
                val lines = mutableListOf<RowPreviewLine>()
                // I numeri degli elenchi si contano come nella pagina: un
                // contatore per livello, e i blocchi di altro tipo in
                // mezzo non interrompono il conteggio. Si conta anche
                // sulle righe che poi l'anteprima salta, altrimenti il
                // primo numero mostrato ripartirebbe da uno.
                val counters = mutableListOf<Int>()
                for (block in rowBlocks) {
                    if (lines.size >= maxLinesPerRow) break
                    if (block.type !in PREVIEW_TYPES) continue
                    val level = block.indentLevel.coerceIn(0, PREVIEW_MAX_INDENT)
                    val ordinal = if (block.type == BlockType.NUMBERED_LIST_ITEM) {
                        while (counters.size < level) counters.add(1)
                        if (counters.size > level) {
                            while (counters.size > level + 1) counters.removeAt(counters.size - 1)
                            counters[level] = block.numberStartsAt ?: (counters[level] + 1)
                        } else {
                            counters.add(block.numberStartsAt ?: 1)
                        }
                        counters[level]
                    } else {
                        0
                    }
                    val text = runCatching {
                        json.decodeFromString<List<RichTextSpan>>(block.textJson)
                    }.getOrNull()?.previewText().orEmpty()
                    if (lines.isEmpty() && text.isBlank()) continue
                    lines += RowPreviewLine(block.type, text, block.isChecked, level, ordinal)
                }
                // Le righe vuote in fondo non dicono niente e rubano
                // posto a una scheda che ha già poco spazio.
                lines.dropLastWhile { it.text.isBlank() }
            }.filterValues { it.isNotEmpty() }
        }

    /**
     * **Quello che c'è scritto dentro le pagine delle righe** di un
     * database, riga per riga, per la ricerca della lente accanto a Sort:
     * il testo dei blocchi (anche dentro i toggle chiusi, anche coperto
     * da uno spoiler — è testo dell'utente, e trovarlo non lo scopre:
     * si illumina la riga, non la parola), le celle delle tabelle e i
     * nomi delle sottopagine richiamate lì dentro.
     *
     * Si legge **una volta**, quando la ricerca si apre (e quando si
     * torna sulla schermata con la ricerca aperta): da lì in poi ogni
     * lettera scritta cerca in memoria, senza toccare il database. Le
     * righe di un database semplice non hanno pagine, e qui non danno
     * niente.
     */
    suspend fun searchIndex(databasePageId: String): Map<String, String> {
        val pieces = dao.getRowPageBlockTexts(databasePageId).map { piece ->
            val text = runCatching {
                json.decodeFromString<List<RichTextSpan>>(piece.text)
            }.getOrNull()?.joinToString("") { it.text }.orEmpty()
            piece.rowId to text
        } + dao.getRowPageTableTexts(databasePageId).map { it.rowId to it.text } +
            dao.getRowPageLinkTitles(databasePageId).map { it.rowId to it.text }
        return pieces
            .filter { it.second.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, texts) -> texts.joinToString("\n") }
    }

    suspend fun addRow(row: DatabaseRowEntity) {
        dao.insertRow(row)
        // Una riga nuova: le automazioni "Page added" (vedi `Automations`).
        DatabaseChanges.rowAdded(row.pageId, row.id)
    }

    suspend fun updateRow(row: DatabaseRowEntity) = dao.updateRow(row)

    /**
     * **Sposta una riga** (dal 29/09/2026, "Move up" / "Move down" tenendo
     * premuto il nome): subito prima (`before`) o subito dopo `targetId`,
     * nell'ordine di tutto il database. È il vicino che si vede, anche con
     * un filtro o dentro un gruppo, dove quello vero può essere nascosto o
     * in un altro gruppo. Poi le righe si rinumerano da 0: due a pari merito
     * non si saprebbe mai dove mostrarle.
     */
    suspend fun moveRowNextTo(pageId: String, rowId: String, targetId: String, before: Boolean) = db.withTransaction {
        if (rowId == targetId) return@withTransaction
        val rows = dao.getRowsForPageOnce(pageId).toMutableList()
        val moving = rows.find { it.id == rowId } ?: return@withTransaction
        rows.remove(moving)
        val target = rows.indexOfFirst { it.id == targetId }
        if (target < 0) return@withTransaction
        rows.add(if (before) target else target + 1, moving)
        rows.forEachIndexed { index, row -> if (row.orderIndex != index) dao.setRowOrderIndex(row.id, index) }
    }

    /**
     * Cambia il titolo di una riga, segnandola come modificata adesso.
     * Se la riga è già stata aperta come pagina, rinomina anche quella:
     * per l'utente è un oggetto solo, e vedere due nomi diversi a
     * seconda di dove guarda sarebbe incomprensibile.
     */
    suspend fun setRowTitle(rowId: String, title: String) {
        dao.setRowTitle(rowId, title, System.currentTimeMillis())
        val linkedPageId = dao.getRowById(rowId)?.linkedPageId ?: return
        val page = db.pageDao().getById(linkedPageId) ?: return
        db.pageDao().update(page.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    /**
     * Cancella una riga. **Le sue sotto-righe restano** (dal 29/09/2026):
     * salgono di un livello, sotto la riga da cui era nata lei — o fra le
     * principali. Buttarle insieme a lei vorrebbe dire perdere righe con le
     * loro pagine senza averle mai guardate.
     *
     * Restituisce la pagina della riga, se ne aveva una: chi chiama la
     * cancella con lei (`PageRepository.deleteRowPage`). È letta qui, non
     * dalla riga in mano a chi chiama, che può essere di prima che la
     * pagina nascesse.
     */
    suspend fun deleteRow(row: DatabaseRowEntity): String? = db.withTransaction {
        val current = dao.getRowById(row.id) ?: row
        dao.reparentRows(row.id, row.parentRowId)
        dao.deleteRow(current)
        current.linkedPageId
    }

    suspend fun setSubRowsCollapsed(rowId: String, collapsed: Boolean) = dao.setSubRowsCollapsed(rowId, collapsed)

    /** Il colore dell'evento di una riga (dal 30/09/2026): cambiarlo è modificare la riga. */
    suspend fun setRowEventColor(rowId: String, color: String?) =
        dao.setRowEventColor(rowId, color, System.currentTimeMillis())

    fun getAllCells(pageId: String): Flow<List<DatabaseCellEntity>> =
        dao.getAllCellsForPage(pageId)

    /**
     * Salva il valore di una cella. Se la cella non esiste ancora per
     * quella combinazione riga/colonna, la crea; altrimenti aggiorna
     * quella esistente. Questo evita che il chiamante (la UI) debba
     * sapere se sta creando o modificando.
     */
    suspend fun setCellValue(rowId: String, columnId: String, value: String) {
        // **Leggere e scrivere in un colpo solo** (dal 28/10/2026): ogni lettera
        // scritta salva la cella, e due salvataggi vicini su una cella vuota la
        // trovavano vuota tutti e due e ne creavano due. Nella transazione il
        // secondo aspetta il primo e trova la cella che lui ha appena creato.
        val existing = db.withTransaction {
            val existing = dao.getCell(rowId, columnId)
            if (existing != null) {
                // Svuotata, la cella non ha più un link da mostrare in un altro modo.
                dao.updateCell(
                    if (value.isBlank()) existing.copy(value = value, linkLabel = null, linkLabelIsTitle = false) else existing.copy(value = value)
                )
            } else {
                dao.insertCell(
                    DatabaseCellEntity(rowId = rowId, columnId = columnId, value = value)
                )
            }
            // Modificare una cella è modificare la riga: è quello che
            // "Last edited time" deve rispecchiare.
            dao.touchRow(rowId, System.currentTimeMillis())
            existing
        }
        // Una data con "Include notification", accesa, spenta o spostata:
        // la sveglia va rimessa (vedi `NoteAlarms`).
        if (value.endsWith(NOTIFY_SUFFIX) || existing?.value?.endsWith(NOTIFY_SUFFIX) == true) {
            ReminderAlarms.reschedule()
        }
        // Le automazioni "Property edited" (vedi `Automations`).
        DatabaseChanges.cellChanged(rowId, columnId, existing?.value, value)
        syncTitle(rowId, columnId)
    }

    /**
     * **La riga si chiama come la sua colonna dei nomi** (dal 13/11/2026, vedi
     * `TitleColumn.kt`): scritta una cella di quella colonna, il nome della
     * riga (e della sua pagina) diventa il valore come si legge nella tabella.
     */
    private suspend fun syncTitle(rowId: String, columnId: String) {
        val row = dao.getRowById(rowId) ?: return
        if (db.pageDao().getById(row.pageId)?.titleColumnId() != columnId) return
        val column = dao.getColumnById(columnId) ?: return
        val title = titleText(column, row, dao.getCell(rowId, columnId)?.value.orEmpty())
        if (title != row.title) setRowTitle(rowId, title)
    }

    /** I nomi delle righe di [databaseId], per le opzioni di una colonna a tag nata da Name. */
    suspend fun rowTitles(databaseId: String): List<String> = dao.getRowsForPageOnce(databaseId).map { it.title }

    /** Il nome di ogni riga di [databaseId] scritto nella sua cella di [columnId], dove è vuota: la colonna che prende il posto di Name. */
    suspend fun fillFromTitles(databaseId: String, columnId: String) = db.withTransaction {
        dao.getRowsForPageOnce(databaseId).forEach { row ->
            if (row.title.isNotEmpty() && dao.getCell(row.id, columnId) == null) {
                dao.insertCell(DatabaseCellEntity(rowId = row.id, columnId = columnId, value = row.title))
            }
        }
    }

    /** Tutte le righe di [databaseId] col nome dalla loro colonna dei nomi, se ce n'è una (dopo "Change type" su Name o sulla colonna). */
    suspend fun syncTitles(databaseId: String) {
        val columnId = db.pageDao().getById(databaseId)?.titleColumnId() ?: return
        val column = dao.getColumnById(columnId) ?: return
        dao.getRowsForPageOnce(databaseId).forEach { row ->
            val title = titleText(column, row, dao.getCell(row.id, columnId)?.value.orEmpty())
            if (title != row.title) setRowTitle(row.id, title)
        }
    }

    /** Il valore di una cella come si legge (vedi `exportCellText`): il nome della riga. */
    private fun titleText(column: DatabaseColumnEntity, row: DatabaseRowEntity, value: String): String =
        exportCellText(column, row, value, emptyMap(), emptyMap(), null).replace("\n", " ").trim()

    /**
     * **La scritta di una cella URL** (dal 27/10/2026, "Show as…"): [label]
     * al posto del link, o null per il link per intero. Solo in una cella
     * che ha già il suo link. [expectedValue], se c'è, è il link per cui la
     * scritta è stata letta: se nel frattempo è cambiato, non si scrive.
     */
    suspend fun setCellLinkLabel(rowId: String, columnId: String, label: LinkLabel?, expectedValue: String? = null) = db.withTransaction {
        val existing = dao.getCell(rowId, columnId) ?: return@withTransaction
        if (existing.value.isBlank() || (expectedValue != null && existing.value != expectedValue)) return@withTransaction
        if (existing.label == label) return@withTransaction
        dao.updateCell(existing.copy(linkLabel = label?.text, linkLabelIsTitle = label?.isTitle == true))
        dao.touchRow(rowId, System.currentTimeMillis())
    }

    /**
     * **Una casella, e la riga principale che la segue** (dal 26/10/2026,
     * chiesto dall'utente): la riga da cui sono nate le sotto-righe è
     * spuntata solo quando lo sono tutte. Spuntata l'ultima che mancava, si
     * spunta anche lei; tolta la spunta a una, si toglie anche a lei; e una
     * spuntata a mano con sotto qualcosa ancora da fare si sistema appena se
     * ne tocca una. Lo stesso verso l'alto, se anche lei è una sotto-riga. In
     * una transazione: le righe si ridisegnano una volta sola, già al loro posto.
     *
     * **E verso il basso** (dal 27/10/2026, chiesto dall'utente: "se spunto o
     * tolgo la spunta alla riga principale, fai lo stesso su tutte le sue
     * sotto-righe"): la casella di una riga va uguale a tutte le sue
     * sotto-righe, e alle loro, fino in fondo. Si scrivono solo quelle
     * diverse, così le altre non risultano modificate adesso.
     */
    suspend fun setCheckbox(rowId: String, columnId: String, value: String) = db.withTransaction {
        setCellValue(rowId, columnId, value)
        val checked = value == "true"
        val below = ArrayDeque(dao.getSubRows(rowId))
        val visited = hashSetOf(rowId)
        while (below.isNotEmpty()) {
            val sub = below.removeFirst()
            if (!visited.add(sub.id)) continue
            if ((dao.getCell(sub.id, columnId)?.value == "true") != checked) setCellValue(sub.id, columnId, value)
            below.addAll(dao.getSubRows(sub.id))
        }
        val seen = hashSetOf(rowId)
        var child = dao.getRowById(rowId)
        while (child != null) {
            val parentId = child.parentRowId?.takeIf { seen.add(it) } ?: break
            val allChecked = dao.getSubRows(parentId).all { dao.getCell(it.id, columnId)?.value == "true" }
            val parentChecked = dao.getCell(parentId, columnId)?.value == "true"
            // Già com'è giusto: sopra di lei non cambia niente.
            if (allChecked == parentChecked) break
            setCellValue(parentId, columnId, if (allChecked) "true" else "false")
            child = dao.getRowById(parentId)
        }
    }

    // --- "Relation" (dal 21/10/2026): vedi `Relations.kt` ---

    /** Scrive una cella e segna la riga come modificata, come `setCellValue`, dentro una transazione già aperta. */
    private suspend fun writeCell(rowId: String, columnId: String, value: String, now: Long) {
        val existing = dao.getCell(rowId, columnId)
        if (existing?.value == value) return
        if (existing != null) {
            dao.updateCell(existing.copy(value = value))
        } else {
            dao.insertCell(DatabaseCellEntity(rowId = rowId, columnId = columnId, value = value))
        }
        dao.touchRow(rowId, now)
    }

    /**
     * **Una cella Relation**, e se la Relation vale nei due sensi anche le
     * celle della colonna gemella nell'altro database: la riga aggiunta qui
     * ha questa fra le sue, quella tolta non più. Tutto in una transazione
     * sola, così le due tabelle non si vedono mai a metà.
     */
    suspend fun setRelation(rowId: String, column: DatabaseColumnEntity, value: String) = db.withTransaction {
        val now = System.currentTimeMillis()
        val before = relationIds(dao.getCell(rowId, column.id)?.value)
        val after = relationIds(value)
        writeCell(rowId, column.id, encodeRelationIds(after), now)
        val reverse = column.relationConfig()?.reverse ?: return@withTransaction
        // Una gemella cancellata nel frattempo, o diventata di un altro tipo,
        // non si tocca.
        if (dao.getColumnById(reverse)?.relationConfig()?.reverse != column.id) return@withTransaction
        val touched = (before + after).distinct().associateWith { other -> dao.getCell(other, reverse)?.value }
        reverseChanges(rowId, before, after) { other -> touched[other] }
            .forEach { (other, ids) -> writeCell(other, reverse, ids, now) }
    }

    /**
     * Una colonna Relation nuova. Nei due sensi ([twoWayName] non null) nasce
     * anche la gemella nell'altro database, in fondo alle sue colonne, col
     * nome dato; ognuna sa qual è l'altra.
     */
    suspend fun addRelationColumn(column: DatabaseColumnEntity, twoWayName: String?) = db.withTransaction {
        val config = column.relationConfig() ?: run {
            dao.insertColumn(column)
            return@withTransaction
        }
        if (twoWayName == null) {
            dao.insertColumn(column.copy(optionsJson = encodeRelationConfig(config.copy(reverse = null))))
            return@withTransaction
        }
        val twinId = UUID.randomUUID().toString()
        dao.insertColumn(column.copy(optionsJson = encodeRelationConfig(config.copy(reverse = twinId))))
        val order = (dao.getColumnsForPageOnce(config.database).maxOfOrNull { it.orderIndex } ?: -1) + 1
        dao.insertColumn(
            DatabaseColumnEntity(
                id = twinId,
                pageId = config.database,
                name = twoWayName,
                type = ColumnType.RELATION,
                optionsJson = encodeRelationConfig(RelationConfig(database = column.pageId, reverse = column.id)),
                orderIndex = order
            )
        )
    }

    /**
     * **"Edit property" di una Relation** (o "Change type" che ci arriva):
     * nome, database e "nei due sensi" insieme, in una transazione. Collegata
     * a un altro database, la gemella di prima se ne va: puntava al vecchio.
     */
    suspend fun updateRelationColumn(column: DatabaseColumnEntity, twoWayName: String?) = db.withTransaction {
        val config = column.relationConfig() ?: run {
            dao.updateColumn(column)
            return@withTransaction
        }
        val before = dao.getColumnById(column.id)?.relationConfig()
        val reverse = before?.reverse?.takeIf { before.database == config.database }
        val reverseBefore = before?.reverse
        if (reverseBefore != null && reverse == null) {
            dao.getColumnById(reverseBefore)?.takeIf { it.relationConfig()?.reverse == column.id }?.let { dao.deleteColumn(it) }
        }
        val updated = column.copy(optionsJson = encodeRelationConfig(config.copy(reverse = reverse)))
        dao.updateColumn(updated)
        setRelationTwoWay(updated, twoWayName)
    }

    /**
     * Accende o spegne "nei due sensi" su una Relation che c'è già. Accesa,
     * la gemella nasce già piena: ogni riga di là ha le righe di qua che la
     * collegano. Spenta, la gemella se ne va con le sue celle. Già accesa,
     * la gemella prende il nome dato.
     */
    suspend fun setRelationTwoWay(column: DatabaseColumnEntity, twoWayName: String?) = db.withTransaction {
        val config = column.relationConfig() ?: return@withTransaction
        val current = dao.getColumnById(column.id) ?: return@withTransaction
        if (twoWayName == null) {
            config.reverse?.let { twin -> dao.getColumnById(twin)?.let { dao.deleteColumn(it) } }
            dao.updateColumn(current.copy(optionsJson = encodeRelationConfig(config.copy(reverse = null))))
            return@withTransaction
        }
        config.reverse?.let { dao.getColumnById(it) }?.let { twin ->
            if (twin.name != twoWayName) dao.updateColumn(twin.copy(name = twoWayName))
            return@withTransaction
        }
        val twinId = UUID.randomUUID().toString()
        val order = (dao.getColumnsForPageOnce(config.database).maxOfOrNull { it.orderIndex } ?: -1) + 1
        dao.insertColumn(
            DatabaseColumnEntity(
                id = twinId,
                pageId = config.database,
                name = twoWayName,
                type = ColumnType.RELATION,
                optionsJson = encodeRelationConfig(RelationConfig(database = column.pageId, reverse = column.id)),
                orderIndex = order
            )
        )
        dao.updateColumn(current.copy(optionsJson = encodeRelationConfig(config.copy(reverse = twinId))))
        // Le celle della gemella, dalle relazioni che ci sono già.
        val back = LinkedHashMap<String, MutableList<String>>()
        dao.getAllCellsForPageOnce(column.pageId)
            .filter { it.columnId == column.id }
            .forEach { cell -> relationIds(cell.value).forEach { other -> back.getOrPut(other) { mutableListOf() } += cell.rowId } }
        val now = System.currentTimeMillis()
        back.forEach { (other, ids) -> writeCell(other, twinId, encodeRelationIds(ids), now) }
    }

    /**
     * Cancella una colonna. Una Relation nei due sensi si porta via la
     * gemella: senza l'altra metà non avrebbe più niente da tenere allineato.
     */
    suspend fun deleteColumnWithTwin(column: DatabaseColumnEntity) = db.withTransaction {
        column.relationConfig()?.reverse?.let { twin ->
            dao.getColumnById(twin)
                ?.takeIf { it.relationConfig()?.reverse == column.id }
                ?.let { dao.deleteColumn(it) }
        }
        dao.deleteColumn(column)
    }

    /**
     * **"Activate all" e "Deactivate all"** di una colonna Checkbox (dal
     * 29/09/2026): spunta o toglie la spunta a tutte le righe date, in una
     * transazione sola, così la tabella si ridisegna una volta. Le caselle che
     * sono già così non si toccano, e la loro riga non risulta modificata
     * adesso: una cella mai toccata vale già "non spuntata".
     */
    suspend fun setAllChecked(rowIds: List<String>, columnId: String, checked: Boolean) = db.withTransaction {
        val now = System.currentTimeMillis()
        rowIds.forEach { rowId ->
            val existing = dao.getCell(rowId, columnId)
            if ((existing?.value == "true") == checked) return@forEach
            if (existing != null) {
                dao.updateCell(existing.copy(value = checked.toString()))
            } else {
                dao.insertCell(DatabaseCellEntity(rowId = rowId, columnId = columnId, value = checked.toString()))
            }
            dao.touchRow(rowId, now)
        }
    }

    private companion object {
        val NOTIFY_SUFFIX = MULTI_VALUE_SEPARATOR + DATE_NOTIFY_FLAG
    }
}
