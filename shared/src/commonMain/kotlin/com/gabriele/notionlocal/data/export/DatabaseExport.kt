package com.gabriele.notionlocal.data.export

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.DatabaseSnapshot
import com.gabriele.notionlocal.data.entity.LinkLabel
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.RollupFormatting
import com.gabriele.notionlocal.data.entity.durationFormatOf
import com.gabriele.notionlocal.data.entity.durationOfCell
import com.gabriele.notionlocal.data.entity.durationUnits
import com.gabriele.notionlocal.data.entity.parseDateRange
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationTitles
import com.gabriele.notionlocal.data.entity.tableKeys
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.viewmodel.DatabaseTableState
import com.gabriele.notionlocal.viewmodel.DatabaseViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId

// ---------------------------------------------------------------------------
// **I database nell'export** (dal 03/11/2026, chiesto dall'utente: "il
// database sarà esportato come lo fa Notion e, se esso è un database
// complesso, allora creerà PDF che sono le pagine contenute al suo interno
// che hanno testo scritto o altro"). Si legge con lo stesso ViewModel della
// schermata, così filtri, ordine, formule, Rollup e relazioni sono quelli che
// si vedono nell'app; le celle si scrivono come nella tabella.
// ---------------------------------------------------------------------------

/** Un database letto: come esportarlo, e le pagine delle sue righe (null dove una riga non ne ha). */
data class DatabaseRead(val database: ExportDatabase, val rowPageIds: List<String?>)

/** Chi legge i database per l'export: [DatabaseReader] nell'app, uno finto nelle prove. */
interface DatabaseSource {
    suspend fun read(databaseId: String): DatabaseRead?
}

class DatabaseReader(private val factory: ViewModelFactory, private val pages: PageRepository) : DatabaseSource {

    /**
     * Il database [databaseId] nella vista che mostra (quella scelta fra le sue
     * viste, o lui stesso). Null se non c'è. Il ViewModel lavora sul thread
     * dello schermo: si aspetta che abbia finito di calcolare, poi si chiude.
     */
    @OptIn(FlowPreview::class)
    override suspend fun read(databaseId: String): DatabaseRead? {
        val host = pages.getPage(databaseId)?.takeIf { it.isDatabase } ?: return null
        val views = pages.viewsOf(host.id)
        val viewId = host.activeViewId?.takeIf { id -> views.any { it.id == id } } ?: host.id
        return withContext(Dispatchers.Main) {
            val store = ViewModelStore()
            try {
                val viewModel = ViewModelProvider.create(store, factory)[DatabaseViewModel::class]
                viewModel.load(viewId, light = true)
                withTimeoutOrNull(LOAD_TIMEOUT_MS) { viewModel.page.first { it != null } }
                // Il calcolo della tabella arriva a pezzi (prima le righe, poi la
                // vista col suo ordine): si prende quando è fermo da un po'.
                val state = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                    viewModel.tableState.debounce(SETTLE_MS).first { it.columns.isNotEmpty() || it.rows.isNotEmpty() }
                } ?: viewModel.tableState.value
                val relatedIds = state.columns.mapNotNullTo(HashSet()) { it.relationConfig()?.database }
                val related = if (relatedIds.isEmpty()) emptyMap() else {
                    withTimeoutOrNull(LOAD_TIMEOUT_MS) { viewModel.related.first { it.keys.containsAll(relatedIds) } } ?: viewModel.related.value
                }
                val linked = if (state.columns.none { it.type == ColumnType.PAGE_LINK }) emptyMap() else {
                    withTimeoutOrNull(LOAD_TIMEOUT_MS) { viewModel.linkedPages.debounce(SETTLE_MS).first { it.isNotEmpty() } } ?: viewModel.linkedPages.value
                }
                val data = viewModel.dataPage.value ?: host
                databaseRead(host, data, viewModel.page.value ?: host, state, related, linked)
            } finally {
                store.clear()
            }
        }
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 6_000L
        const val SETTLE_MS = 400L
    }
}

/**
 * Da quello che mostra la vista all'export: le colonne che si vedono
 * nell'ordine della tabella (Name al suo posto), le righe com'è la vista; per
 * il CSV tutte le colonne.
 */
fun databaseRead(
    host: PageEntity,
    data: PageEntity,
    view: PageEntity,
    state: DatabaseTableState,
    related: Map<String, DatabaseSnapshot>,
    linkedPages: Map<String, PageEntity>
): DatabaseRead {
    val byId = state.columns.associateBy { it.id }
    val nameTitle = DbStrings.nameColumn(data.nameColumnTitle ?: host.nameColumnTitle)
    val order = tableKeys(state.columns, view.nameColumnOrder)
    val shown = order.filter { it == NAME_COLUMN_KEY || byId[it]?.hidden == false }
    fun header(key: String) = if (key == NAME_COLUMN_KEY) nameTitle else byId[key]?.name.orEmpty()
    fun value(row: DatabaseRowEntity, key: String): String {
        if (key == NAME_COLUMN_KEY) return row.title
        val column = byId[key] ?: return ""
        val raw = state.cellValues[row.id to key].orEmpty()
        return exportCellText(column, row, raw, related, linkedPages, state.linkLabels[row.id to key])
    }
    val rows = state.rows.map { row ->
        ExportDatabaseRow(
            title = row.title,
            cells = shown.map { value(row, it) },
            csvCells = order.map { value(row, it) }
        )
    }
    val pageIds = state.rows.map { row -> row.linkedPageId.takeIf { !data.isSimpleDatabase } }
    return DatabaseRead(
        ExportDatabase(
            title = host.title,
            headers = shown.map(::header),
            rows = rows,
            csvHeaders = order.map(::header),
            nameIndex = shown.indexOf(NAME_COLUMN_KEY).coerceAtLeast(0)
        ),
        pageIds
    )
}

/**
 * **Il testo di una cella**, come si legge nella tabella: le date con il
 * formato di Settings (e la fine, se c'è), le durate a parole, i nomi delle
 * righe collegate al posto dei loro id, la casella spuntata come ✓.
 */
fun exportCellText(
    column: DatabaseColumnEntity,
    row: DatabaseRowEntity,
    raw: String,
    related: Map<String, DatabaseSnapshot>,
    linkedPages: Map<String, PageEntity>,
    label: LinkLabel?
): String {
    val formatter = RollupFormatting.formatter
    return when (column.type) {
        ColumnType.RELATION -> relationTitles(raw, column.relationConfig()?.let { related[it.database] }).joinToString(", ")
        ColumnType.PAGE_LINK -> if (raw.isEmpty()) "" else linkedPages[raw]?.title.orEmpty()
        ColumnType.DATE -> parseDateRange(raw)?.let { range ->
            fun one(millis: Long) = if (range.hasTime) formatter.dateTime(millis) else formatter.date(millis)
            one(range.start) + (range.end?.let { " → " + one(it) } ?: "")
        } ?: ""
        ColumnType.DURATION -> durationOfCell(raw, ZoneId.systemDefault())
            ?.let { DbStrings.durationText(durationUnits(it, durationFormatOf(column.durationFormat))) }
            .orEmpty()
        ColumnType.CREATED_TIME -> formatter.dateTime(row.createdAt)
        ColumnType.LAST_EDITED_TIME -> formatter.dateTime(row.updatedAt ?: row.createdAt)
        ColumnType.URL -> label?.text?.takeIf { it.isNotBlank() && it != raw }?.let { "$it ($raw)" } ?: raw
        ColumnType.IMAGE -> if (raw.isBlank()) "" else "🖼"
        ColumnType.BUTTON -> ""
        ColumnType.SELECT -> raw
        ColumnType.MULTI_SELECT -> raw.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.joinToString(", ")
        else -> formatter.cell(column, raw)
    }
}

/** **Il CSV di un database**, come quello di Notion: la prima riga i nomi delle colonne, i valori fra virgolette dove serve. */
fun csvOf(database: ExportDatabase): String {
    fun field(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
    val lines = listOf(database.csvHeaders) + database.rows.map { it.csvCells }
    // Con il BOM davanti, Excel lo apre con gli accenti giusti.
    return "﻿" + lines.joinToString("\r\n") { line -> line.joinToString(",") { field(it) } } + "\r\n"
}
