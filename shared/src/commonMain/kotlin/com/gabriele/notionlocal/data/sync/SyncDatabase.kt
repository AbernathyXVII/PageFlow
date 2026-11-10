package com.gabriele.notionlocal.data.sync

import androidx.room.PooledConnection
import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteStatement
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity

/** Com'è un pezzo su un lato: l'impronta, e quando è cambiato l'ultima volta (decide i conflitti). */
data class UnitState(val hash: String, val changedAt: Long)

/**
 * **Le righe dei registri**, che si aggiungono e non si sostituiscono: le
 * versioni della cronologia e "Updates" (id → pagina, dalle ultime
 * sincronizzazioni), il testo delle foto (per file) e le parole del
 * correttore.
 */
data class LogKeys(
    val versions: Map<String, String> = emptyMap(),
    val edits: Map<String, String> = emptyMap(),
    val imageTexts: Set<String> = emptySet(),
    val words: Set<String> = emptySet()
)

/**
 * **Il database visto dalla sincronizzazione** (dal 10/11/2026): i pezzi
 * (`SyncUnit`) con le loro impronte, i registri, e il modo di mettere qui
 * quelli arrivati dall'altro lato. Tutto con l'SQL, colonna per colonna per
 * nome: sul telefono le tabelle sono nate dalle migrazioni e le colonne
 * possono essere in un altro ordine.
 */
object SyncDatabase {

    /** Le tabelle dei pezzi, nell'ordine in cui si scrivono (prima chi è richiamato). */
    private val PAGE_TABLES = listOf("pages", "blocks", "table_cells", "ink_strokes", "database_columns", "automations")
    private val ROW_TABLES = listOf("database_rows", "database_cells")
    private val NOTEBOOK_TABLES = listOf("notebooks", "notebook_sections")
    private val LOG_TABLES = setOf("page_versions", "page_edits", "image_texts", "personal_words")

    /** I pezzi del database, o solo quelli di [wanted]. */
    suspend fun readUnits(db: AppDatabase, wanted: Set<String>? = null): Map<String, SyncUnit> {
        val units = HashMap<String, HashMap<String, MutableList<SyncRow>>>()
        fun add(unit: String, table: String, row: SyncRow) {
            if (wanted != null && unit !in wanted) return
            units.getOrPut(unit) { HashMap() }.getOrPut(table) { ArrayList() }.add(row)
        }
        db.rows("SELECT * FROM pages").forEach { add("p:${it["id"]}", "pages", it) }
        val pageOfBlock = HashMap<String, String>()
        db.rows("SELECT * FROM blocks").forEach {
            val pageId = it["pageId"] as String
            pageOfBlock[it["id"] as String] = pageId
            add("p:$pageId", "blocks", it)
        }
        db.rows("SELECT * FROM table_cells").forEach { row -> pageOfBlock[row["blockId"]]?.let { add("p:$it", "table_cells", row) } }
        db.rows("SELECT * FROM ink_strokes").forEach { add("p:${it["pageId"]}", "ink_strokes", it) }
        db.rows("SELECT * FROM database_columns").forEach { add("p:${it["pageId"]}", "database_columns", it) }
        db.rows("SELECT * FROM automations").forEach { add("p:${it["databaseId"]}", "automations", it) }
        db.rows("SELECT * FROM database_rows").forEach { add("r:${it["id"]}", "database_rows", it) }
        db.rows("SELECT * FROM database_cells").forEach { add("r:${it["rowId"]}", "database_cells", it) }
        db.rows("SELECT * FROM notebooks").forEach { add("n:${it["id"]}", "notebooks", it) }
        db.rows("SELECT * FROM notebook_sections").forEach { add("n:${it["notebookId"]}", "notebook_sections", it) }
        return units.mapValues { (id, tables) -> SyncUnit(id, tables) }
    }

    /**
     * Tutti i pezzi con l'impronta e l'ultima modifica: per una pagina la più
     * recente fra la pagina stessa, "Updates" e la cronologia (scrivere nei
     * blocchi non cambia `updatedAt`); per una riga la sua; per un quaderno
     * quando è nato.
     */
    suspend fun manifest(db: AppDatabase): Map<String, UnitState> = snapshot(db).first

    /** Come `manifest`, coi pezzi stessi accanto (per ricordare le impronte delle righe). */
    suspend fun snapshot(db: AppDatabase): Pair<Map<String, UnitState>, Map<String, SyncUnit>> {
        val lastEdit = db.latestByPage("page_edits")
        val lastVersion = db.latestByPage("page_versions")
        val units = readUnits(db)
        return units.mapValues { (id, unit) ->
            val key = id.substringAfter(':')
            val changedAt = when (id.substringBefore(':')) {
                "p" -> maxOf(
                    unit.tables["pages"]?.firstOrNull()?.get("updatedAt") as? Long ?: 0L,
                    lastEdit[key] ?: 0L,
                    lastVersion[key] ?: 0L
                )
                "r" -> unit.tables["database_rows"]?.firstOrNull()?.let { (it["updatedAt"] ?: it["createdAt"]) as? Long } ?: 0L
                else -> unit.tables["notebooks"]?.firstOrNull()?.get("createdAt") as? Long ?: 0L
            }
            UnitState(unit.hash, changedAt)
        } to units
    }

    private suspend fun AppDatabase.latestByPage(table: String): Map<String, Long> =
        rows("SELECT pageId, MAX(at) AS at FROM $table GROUP BY pageId").associate { it["pageId"] as String to (it["at"] as Long) }

    /** Le chiavi dei registri: versioni e "Updates" nati da [since] in poi, tutti i testi delle foto e le parole. */
    suspend fun logKeys(db: AppDatabase, since: Long): LogKeys = LogKeys(
        versions = db.rows("SELECT id, pageId FROM page_versions WHERE at >= ?", since).associate { it["id"] as String to it["pageId"] as String },
        edits = db.rows("SELECT id, pageId FROM page_edits WHERE at >= ?", since).associate { it["id"] as String to it["pageId"] as String },
        imageTexts = db.rows("SELECT file FROM image_texts").mapTo(HashSet()) { it["file"] as String },
        words = db.rows("SELECT word FROM personal_words").mapTo(HashSet()) { it["word"] as String }
    )

    /** Le righe dei registri chieste, tabella per tabella. */
    suspend fun readLogs(db: AppDatabase, keys: LogKeys): Map<String, List<SyncRow>> = mapOf(
        "page_versions" to keys.versions.keys.flatMap { db.rows("SELECT * FROM page_versions WHERE id = ?", it) },
        "page_edits" to keys.edits.keys.flatMap { db.rows("SELECT * FROM page_edits WHERE id = ?", it) },
        "image_texts" to keys.imageTexts.flatMap { db.rows("SELECT * FROM image_texts WHERE file = ?", it) },
        "personal_words" to keys.words.flatMap { db.rows("SELECT * FROM personal_words WHERE word = ?", it) }
    ).filterValues { it.isNotEmpty() }

    /**
     * **Mette qui quello che è arrivato**, tutto in una transazione: i pezzi
     * di [incoming] prendono il posto dei nostri (le righe che lì non ci sono
     * più se ne vanno), quelli di [toTrash] (cancellati dall'altra parte)
     * vanno nel cestino — le pagine; righe e quaderni non ce l'hanno e se ne
     * vanno — e i registri si aggiungono. Alla fine i collegamenti rimasti
     * senza l'altra metà (una pagina che qui non c'è) si tolgono come li
     * toglierebbe SQLite cancellandola.
     *
     * **Nessuna pagina resta senza posto**: se una pagina arrivata prende il
     * posto della nostra (un conflitto, per esempio il menu principale con
     * una pagina nuova di qua e una di là), i collegamenti che c'erano solo
     * qui spariscono con lei. Le pagine rimaste così senza collegamento, e
     * non nel cestino, tornano in fondo alla pagina che le richiamava (se no
     * al menu principale).
     */
    suspend fun apply(
        db: AppDatabase,
        incoming: Collection<SyncUnit>,
        toTrash: Collection<String> = emptyList(),
        logs: Map<String, List<SyncRow>> = emptyMap(),
        now: Long = System.currentTimeMillis()
    ) {
        val linkedBefore = db.linkers()
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                execSQL("PRAGMA defer_foreign_keys = TRUE")
                val columns = HashMap<String, Set<String>>()
                for (unit in incoming) applyUnit(unit, columns)
                for (unitId in toTrash) trash(unitId, now)
                for ((table, rows) in logs) {
                    // Solo le tabelle dei registri: il nome arriva dall'altro lato e finisce nell'SQL.
                    if (table !in LOG_TABLES) continue
                    for (row in rows) insert(table, row.filterKeys { it in columnsOf(table, columns) }, ignoreExisting = true)
                }
                repairLinks()
            }
        }
        relinkLostPages(db, linkedBefore)
        db.invalidationTracker.refreshAsync()
    }

    /** Pagina → le pagine che la richiamano con un collegamento. */
    private suspend fun AppDatabase.linkers(): Map<String, List<String>> =
        rows("SELECT linkedPageId, pageId FROM blocks WHERE linkedPageId IS NOT NULL")
            .groupBy({ it["linkedPageId"] as String }, { it["pageId"] as String })

    private suspend fun relinkLostPages(db: AppDatabase, linkedBefore: Map<String, List<String>>) {
        val linkedNow = db.linkers()
        for ((pageId, hosts) in linkedBefore) {
            if (pageId in linkedNow) continue
            val page = db.pageDao().getById(pageId) ?: continue
            if (page.trashedAt != null) continue
            val host = hosts.firstOrNull { db.pageDao().getById(it)?.trashedAt == null && it != pageId } ?: PageEntity.ROOT_PAGE_ID
            db.blockDao().insert(
                BlockEntity(
                    pageId = host,
                    type = if (page.isDatabase) BlockType.DATABASE_LINK else BlockType.PAGE_LINK,
                    linkedPageId = pageId,
                    orderIndex = (db.blockDao().lastOrderIndex(host) ?: -1) + 1
                )
            )
        }
    }

    private suspend fun PooledConnection.applyUnit(unit: SyncUnit, columns: HashMap<String, Set<String>>) {
        val key = unit.id.substringAfter(':')
        val tables = when (unit.id.substringBefore(':')) {
            "p" -> {
                dropStale(unit, "table_cells", "SELECT c.id FROM table_cells c JOIN blocks b ON b.id = c.blockId WHERE b.pageId = ?", key)
                dropStale(unit, "blocks", "SELECT id FROM blocks WHERE pageId = ?", key)
                dropStale(unit, "ink_strokes", "SELECT id FROM ink_strokes WHERE pageId = ?", key)
                dropStale(unit, "automations", "SELECT id FROM automations WHERE databaseId = ?", key)
                dropStale(unit, "database_columns", "SELECT id FROM database_columns WHERE pageId = ?", key)
                PAGE_TABLES
            }
            "r" -> {
                dropStale(unit, "database_cells", "SELECT id FROM database_cells WHERE rowId = ?", key)
                ROW_TABLES
            }
            else -> {
                dropStale(unit, "notebook_sections", "SELECT id FROM notebook_sections WHERE notebookId = ?", key)
                NOTEBOOK_TABLES
            }
        }
        for (table in tables) {
            for (row in unit.tables[table].orEmpty()) upsert(table, row.filterKeys { it in columnsOf(table, columns) })
        }
    }

    /** Toglie le righe di [table] che abbiamo qui ([localSql]) e che nel pezzo arrivato non ci sono più. */
    private suspend fun PooledConnection.dropStale(unit: SyncUnit, table: String, localSql: String, key: String) {
        val keep = unit.tables[table].orEmpty().mapTo(HashSet()) { it["id"] as String }
        val local = usePrepared(localSql) { st -> st.bindText(1, key); buildList { while (st.step()) add(st.getText(0)) } }
        for (id in local) if (id !in keep) run("DELETE FROM `$table` WHERE id = ?", listOf(id))
    }

    /** La riga come è arrivata: aggiornata se c'è già (senza toccare le colonne di questo lato), se no nuova. */
    private suspend fun PooledConnection.upsert(table: String, row: SyncRow) {
        val key = SyncUnit.keyColumn(table)
        val local = SyncUnit.LOCAL_ONLY[table].orEmpty()
        val set = row.keys.filter { it != key && it !in local }
        if (set.isNotEmpty()) {
            run("UPDATE `$table` SET ${set.joinToString { "`$it` = ?" }} WHERE `$key` = ?", set.map { row[it] } + listOf(row[key]))
            if (usePrepared("SELECT changes()") { st -> st.step(); st.getLong(0) } > 0) return
        }
        insert(table, row, ignoreExisting = false)
    }

    private suspend fun PooledConnection.insert(table: String, row: SyncRow, ignoreExisting: Boolean) {
        val or = if (ignoreExisting) "OR IGNORE " else ""
        run("INSERT ${or}INTO `$table` (${row.keys.joinToString { "`$it`" }}) VALUES (${row.keys.joinToString { "?" }})", row.values.toList())
    }

    private suspend fun PooledConnection.trash(unitId: String, now: Long) {
        val key = unitId.substringAfter(':')
        when (unitId.substringBefore(':')) {
            // Come `PageRepository.moveToTrash`: segnata, e via i collegamenti che la richiamano.
            "p" -> if (key != PageEntity.ROOT_PAGE_ID) {
                run("UPDATE pages SET trashedAt = ? WHERE id = ? AND trashedAt IS NULL", listOf(now, key))
                if (usePrepared("SELECT changes()") { st -> st.step(); st.getLong(0) } > 0) {
                    run("DELETE FROM blocks WHERE linkedPageId = ?", listOf(key))
                }
            }
            "r" -> run("DELETE FROM database_rows WHERE id = ?", listOf(key))
            else -> run("DELETE FROM notebooks WHERE id = ?", listOf(key))
        }
    }

    /** I collegamenti senza l'altra metà, come li sistemerebbe SQLite: "SET NULL", o via la riga. */
    private suspend fun PooledConnection.repairLinks() {
        for (round in 0 until MAX_REPAIR_ROUNDS) {
            val broken = usePrepared("PRAGMA foreign_key_check") { st ->
                buildList { while (st.step()) add(Triple(st.getText(0), st.getLong(1), st.getLong(3))) }
            }
            if (broken.isEmpty()) return
            for ((table, rowId, fkId) in broken) {
                val link = usePrepared("PRAGMA foreign_key_list(`$table`)") { st ->
                    var found: Pair<String, String>? = null
                    while (st.step()) if (st.getLong(0) == fkId) found = st.getText(3) to st.getText(6)
                    found
                } ?: continue
                if (link.second.equals("SET NULL", ignoreCase = true)) {
                    execSQL("UPDATE `$table` SET `${link.first}` = NULL WHERE rowid = $rowId")
                } else {
                    execSQL("DELETE FROM `$table` WHERE rowid = $rowId")
                }
            }
        }
    }

    private suspend fun PooledConnection.columnsOf(table: String, cache: HashMap<String, Set<String>>): Set<String> =
        cache[table] ?: usePrepared("PRAGMA table_info(`$table`)") { st -> buildSet { while (st.step()) add(st.getText(1)) } }
            .also { cache[table] = it }

    private suspend fun PooledConnection.run(sql: String, args: List<Any?>) {
        usePrepared(sql) { st ->
            bindAll(st, args)
            st.step()
        }
    }

    private const val MAX_REPAIR_ROUNDS = 6

    private fun bindAll(st: SQLiteStatement, args: List<Any?>) {
        args.forEachIndexed { i, value ->
            val at = i + 1
            when (value) {
                null -> st.bindNull(at)
                is Long -> st.bindLong(at, value)
                is Int -> st.bindLong(at, value.toLong())
                is Double -> st.bindDouble(at, value)
                is String -> st.bindText(at, value)
                is ByteArray -> st.bindBlob(at, value)
                is Boolean -> st.bindLong(at, if (value) 1 else 0)
                else -> error("valore che SQLite non tiene: ${value::class.simpleName}")
            }
        }
    }

    private suspend fun AppDatabase.rows(sql: String, vararg args: Any?): List<SyncRow> = useReaderConnection { connection ->
        connection.usePrepared(sql) { st ->
            bindAll(st, args.toList())
            val names = st.getColumnNames()
            buildList {
                while (st.step()) add(LinkedHashMap<String, Any?>().apply { names.forEachIndexed { i, name -> put(name, st.valueAt(i)) } })
            }
        }
    }

    private fun SQLiteStatement.valueAt(i: Int): Any? = when (getColumnType(i)) {
        SQLITE_DATA_NULL -> null
        SQLITE_DATA_INTEGER -> getLong(i)
        SQLITE_DATA_FLOAT -> getDouble(i)
        SQLITE_DATA_BLOB -> getBlob(i)
        else -> getText(i)
    }
}
