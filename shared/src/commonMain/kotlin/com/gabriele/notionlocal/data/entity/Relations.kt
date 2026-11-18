package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * **"Relation" e "Rollup"** (dal 21/10/2026, chiesti dall'utente, come su
 * Notion).
 *
 * Una **Relation** collega le righe di un database a quelle di un altro (o
 * dello stesso): la cella tiene gli id delle righe collegate, separati da
 * `MULTI_VALUE_SEPARATOR` come i tag di una selezione multipla. La sua
 * configurazione (`RelationConfig`) sta in `DatabaseColumnEntity.optionsJson`,
 * come le opzioni dei tag: niente colonne nuove nel database.
 *
 * Una Relation può valere **nei due sensi**: allora nell'altro database c'è
 * una colonna gemella, anche lei una Relation, che punta indietro, e le due
 * celle si tengono allineate (`PageRepository` non c'entra: vedi
 * `DatabaseRepository.setRelation`).
 *
 * Un **Rollup** non salva niente: guarda le righe collegate da una Relation
 * di questo database, ne legge una proprietà e ci fa un calcolo
 * (`RollupCalc`). Si calcola ogni volta che le righe cambiano.
 */

private val configJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

/** La Relation di una colonna: a quale database, e se ha la gemella dall'altra parte. */
@Serializable
data class RelationConfig(
    /** L'id del database collegato (la pagina col database, non una sua vista). */
    val database: String,
    /** L'id della colonna gemella nell'altro database, se la Relation vale nei due sensi. */
    val reverse: String? = null
) {
    val twoWay: Boolean get() = reverse != null
}

/** Il Rollup di una colonna: quale Relation, quale proprietà delle righe collegate, quale calcolo. */
@Serializable
data class RollupConfig(
    /** L'id della colonna Relation di questo database. */
    val relation: String,
    /** L'id della proprietà nel database collegato, o `SORT_BY_NAME` per il nome delle righe. */
    val property: String = SORT_BY_NAME,
    val calc: RollupCalc = RollupCalc.SHOW_ORIGINAL
)

/**
 * I calcoli di un Rollup, quelli di Notion. Ognuno dice se dà un numero,
 * una data o un testo (vedi `RollupValue`).
 */
enum class RollupCalc {
    SHOW_ORIGINAL,
    SHOW_UNIQUE,
    COUNT_ALL,
    COUNT_VALUES,
    COUNT_UNIQUE,
    COUNT_EMPTY,
    COUNT_NOT_EMPTY,
    PERCENT_EMPTY,
    PERCENT_NOT_EMPTY,
    SUM,
    AVERAGE,
    MEDIAN,
    MIN,
    MAX,
    RANGE,
    EARLIEST_DATE,
    LATEST_DATE,
    DATE_RANGE,
    CHECKED,
    UNCHECKED,
    PERCENT_CHECKED,
    PERCENT_UNCHECKED;

    companion object {
        fun of(name: String?): RollupCalc = entries.find { it.name == name } ?: SHOW_ORIGINAL
    }
}

/** I calcoli che hanno senso per una proprietà di questo tipo (null = il nome delle righe). */
fun rollupCalcsFor(type: ColumnType?): List<RollupCalc> {
    val common = listOf(
        RollupCalc.SHOW_ORIGINAL, RollupCalc.SHOW_UNIQUE,
        RollupCalc.COUNT_ALL, RollupCalc.COUNT_VALUES, RollupCalc.COUNT_UNIQUE,
        RollupCalc.COUNT_EMPTY, RollupCalc.COUNT_NOT_EMPTY,
        RollupCalc.PERCENT_EMPTY, RollupCalc.PERCENT_NOT_EMPTY
    )
    return when (type) {
        ColumnType.NUMBER -> common + listOf(
            RollupCalc.SUM, RollupCalc.AVERAGE, RollupCalc.MEDIAN,
            RollupCalc.MIN, RollupCalc.MAX, RollupCalc.RANGE
        )
        ColumnType.DATE, ColumnType.CREATED_TIME, ColumnType.LAST_EDITED_TIME ->
            common + listOf(RollupCalc.EARLIEST_DATE, RollupCalc.LATEST_DATE, RollupCalc.DATE_RANGE)
        ColumnType.CHECKBOX -> listOf(
            RollupCalc.SHOW_ORIGINAL, RollupCalc.COUNT_ALL,
            RollupCalc.CHECKED, RollupCalc.UNCHECKED, RollupCalc.PERCENT_CHECKED, RollupCalc.PERCENT_UNCHECKED
        )
        else -> common
    }
}

fun DatabaseColumnEntity.relationConfig(): RelationConfig? =
    if (type != ColumnType.RELATION) null else runCatching { configJson.decodeFromString<RelationConfig>(optionsJson) }.getOrNull()

fun DatabaseColumnEntity.rollupConfig(): RollupConfig? =
    if (type != ColumnType.ROLLUP) null else runCatching { configJson.decodeFromString<RollupConfig>(optionsJson) }.getOrNull()

fun encodeRelationConfig(config: RelationConfig): String = configJson.encodeToString(config)

fun encodeRollupConfig(config: RollupConfig): String = configJson.encodeToString(config)

/** Le righe collegate da una cella Relation, nell'ordine in cui sono state scelte, senza doppioni. */
fun relationIds(value: String?): List<String> =
    value.orEmpty().split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }.distinct()

fun encodeRelationIds(ids: List<String>): String = ids.distinct().joinToString(MULTI_VALUE_SEPARATOR)

/**
 * Un database com'è adesso, per leggere le righe collegate: le sue colonne,
 * le sue righe e le sue celle.
 */
data class DatabaseSnapshot(
    val columns: List<DatabaseColumnEntity>,
    val rows: List<DatabaseRowEntity>,
    val cells: Map<Pair<String, String>, String>,
    /** La pagina del database: il suo nome, e se è semplice. Null se non c'è più. */
    val page: PageEntity? = null
) {
    val rowsById: Map<String, DatabaseRowEntity> by lazy { rows.associateBy { it.id } }
}

/**
 * Il risultato di un Rollup: il testo da mostrare e, se è un numero o una
 * data, il valore con cui si ordina.
 */
data class RollupValue(
    val text: String,
    val number: Double? = null,
    /** Per i calcoli sulle date: l'istante, in millisecondi. */
    val date: Long? = null
) {
    val isEmpty: Boolean get() = text.isEmpty()

    companion object {
        val EMPTY = RollupValue("")
    }
}

/** Come si scrive quello che il Rollup legge: un numero, una data, un titolo. Lo dà la schermata. */
interface RollupFormatter {
    fun number(value: Double): String
    fun date(millis: Long): String
    /** Una data con l'ora (per le formule): senza, si scrive come una data. */
    fun dateTime(millis: Long): String = date(millis)
    /** Il valore di una cella così come si legge (un tag, il testo di una cella colorata…). */
    fun cell(column: DatabaseColumnEntity, raw: String): String
}

/**
 * Il modo di scrivere numeri e date dei Rollup che vale nell'app: la
 * schermata ci mette il suo (`AppRollupFormatter`, con le scelte di
 * Settings) la prima volta che mostra un database.
 */
object RollupFormatting {
    @Volatile
    var formatter: RollupFormatter = PlainRollupFormatter
}

/** Numeri e date senza le scelte di Settings: per i test, e finché la schermata non dà il suo. */
object PlainRollupFormatter : RollupFormatter {
    override fun number(value: Double): String =
        if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()

    override fun date(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    override fun dateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0).toString().replace('T', ' ')

    override fun cell(column: DatabaseColumnEntity, raw: String): String = when (column.type) {
        ColumnType.CHECKBOX -> if (raw == "true") "✓" else ""
        ColumnType.COLOR -> parseColorCell(raw).text
        ColumnType.DATE -> raw.substringBefore(MULTI_VALUE_SEPARATOR).toLongOrNull()?.let(::date) ?: raw
        else -> raw
    }
}

/**
 * **Il calcolo di un Rollup** per una riga: `relationValue` è la sua cella
 * Relation, `related` il database collegato, `hop` dà gli altri database
 * quando la proprietà letta è anche lei una Relation (il gruppo di una
 * canzone, letto da una vittoria).
 */
fun computeRollup(
    config: RollupConfig,
    relationValue: String?,
    related: DatabaseSnapshot?,
    hop: (String) -> DatabaseSnapshot?,
    format: RollupFormatter,
    zone: ZoneId = ZoneId.systemDefault()
): RollupValue {
    related ?: return RollupValue.EMPTY
    val rows = relationIds(relationValue).mapNotNull { related.rowsById[it] }
    val column = related.columns.find { it.id == config.property }
    // Quello che c'è in ogni riga collegata, già leggibile: una riga può
    // dare più valori (i tag, le righe di un'altra Relation).
    val readable: List<List<String>> = rows.map { row ->
        when {
            config.property == SORT_BY_NAME || column == null -> listOf(row.title)
            column.type == ColumnType.RELATION -> {
                val next = column.relationConfig()?.let { hop(it.database) }
                relationIds(related.cells[row.id to column.id]).mapNotNull { id -> next?.rowsById?.get(id)?.title }
            }
            column.type.isMultiTag -> relationIds(related.cells[row.id to column.id])
            column.type == ColumnType.CREATED_TIME -> listOf(format.date(row.createdAt))
            column.type == ColumnType.LAST_EDITED_TIME -> listOf(format.date(row.updatedAt ?: row.createdAt))
            else -> related.cells[row.id to column.id]
                ?.takeIf { it.isNotEmpty() }
                ?.let { listOf(format.cell(column, it)) }
                .orEmpty()
        }
    }
    val all = readable.size
    val filled = readable.count { it.isNotEmpty() }
    fun count(n: Int) = RollupValue(n.toString(), n.toDouble())
    fun percent(part: Int, whole: Int): RollupValue {
        if (whole == 0) return RollupValue.EMPTY
        val p = part * 100.0 / whole
        return RollupValue(format.number(Math.round(p * 10) / 10.0) + "%", p)
    }
    val numbers: List<Double> by lazy {
        if (column?.type != ColumnType.NUMBER) emptyList()
        else rows.mapNotNull { related.cells[it.id to column.id]?.toDoubleOrNull() }
    }
    val dates: List<Long> by lazy {
        when (column?.type) {
            ColumnType.DATE -> rows.mapNotNull { row ->
                related.cells[row.id to column.id]?.substringBefore(MULTI_VALUE_SEPARATOR)?.toLongOrNull()
            }
            ColumnType.CREATED_TIME -> rows.map { it.createdAt }
            ColumnType.LAST_EDITED_TIME -> rows.map { it.updatedAt ?: it.createdAt }
            else -> emptyList()
        }
    }
    val checked: Int by lazy {
        if (column?.type != ColumnType.CHECKBOX) 0 else rows.count { related.cells[it.id to column.id] == "true" }
    }
    fun number(value: Double) = RollupValue(format.number(value), value)

    return when (config.calc) {
        RollupCalc.SHOW_ORIGINAL -> {
            val values = readable.flatten().filter { it.isNotEmpty() }
            RollupValue(values.joinToString(", "))
        }
        RollupCalc.SHOW_UNIQUE -> RollupValue(readable.flatten().filter { it.isNotEmpty() }.distinct().joinToString(", "))
        RollupCalc.COUNT_ALL -> count(all)
        RollupCalc.COUNT_VALUES -> count(readable.sumOf { it.size })
        RollupCalc.COUNT_UNIQUE -> count(readable.flatten().distinct().size)
        RollupCalc.COUNT_EMPTY -> count(all - filled)
        RollupCalc.COUNT_NOT_EMPTY -> count(filled)
        RollupCalc.PERCENT_EMPTY -> percent(all - filled, all)
        RollupCalc.PERCENT_NOT_EMPTY -> percent(filled, all)
        RollupCalc.SUM -> if (numbers.isEmpty()) RollupValue.EMPTY else number(numbers.sum())
        RollupCalc.AVERAGE -> if (numbers.isEmpty()) RollupValue.EMPTY else number(numbers.average())
        RollupCalc.MEDIAN -> if (numbers.isEmpty()) RollupValue.EMPTY else number(median(numbers))
        RollupCalc.MIN -> numbers.minOrNull()?.let(::number) ?: RollupValue.EMPTY
        RollupCalc.MAX -> numbers.maxOrNull()?.let(::number) ?: RollupValue.EMPTY
        RollupCalc.RANGE -> if (numbers.isEmpty()) RollupValue.EMPTY else number(numbers.max() - numbers.min())
        RollupCalc.EARLIEST_DATE -> dates.minOrNull()?.let { RollupValue(format.date(it), it.toDouble(), it) } ?: RollupValue.EMPTY
        RollupCalc.LATEST_DATE -> dates.maxOrNull()?.let { RollupValue(format.date(it), it.toDouble(), it) } ?: RollupValue.EMPTY
        RollupCalc.DATE_RANGE -> if (dates.isEmpty()) RollupValue.EMPTY else {
            val first = Instant.ofEpochMilli(dates.min()).atZone(zone).toLocalDate()
            val last = Instant.ofEpochMilli(dates.max()).atZone(zone).toLocalDate()
            count(ChronoUnit.DAYS.between(first, last).toInt())
        }
        RollupCalc.CHECKED -> count(checked)
        RollupCalc.UNCHECKED -> count(all - checked)
        RollupCalc.PERCENT_CHECKED -> percent(checked, all)
        RollupCalc.PERCENT_UNCHECKED -> percent(all - checked, all)
    }
}

/**
 * **I Rollup di tutte le righe** di un database: (riga, colonna) → valore,
 * solo per le celle che hanno qualcosa da mostrare.
 */
fun computeRollups(
    columns: List<DatabaseColumnEntity>,
    rows: List<DatabaseRowEntity>,
    cells: Map<Pair<String, String>, String>,
    related: Map<String, DatabaseSnapshot>,
    format: RollupFormatter
): Map<Pair<String, String>, RollupValue> {
    val rollups = columns.filter { it.type == ColumnType.ROLLUP }
    if (rollups.isEmpty()) return emptyMap()
    val result = HashMap<Pair<String, String>, RollupValue>()
    for (column in rollups) {
        val config = column.rollupConfig() ?: continue
        val relation = columns.find { it.id == config.relation }?.relationConfig() ?: continue
        val target = related[relation.database] ?: continue
        for (row in rows) {
            val value = computeRollup(config, cells[row.id to config.relation], target, { related[it] }, format)
            if (!value.isEmpty) result[row.id to column.id] = value
        }
    }
    return result
}

/**
 * I database che servono, oltre a quelli collegati da `columns`: quelli a
 * due passi, collegati da una Relation che un Rollup legge.
 */
fun secondHopDatabases(columns: List<DatabaseColumnEntity>, related: Map<String, DatabaseSnapshot>): Set<String> {
    val ids = LinkedHashSet<String>()
    for (column in columns) {
        val config = column.rollupConfig() ?: continue
        val relation = columns.find { it.id == config.relation }?.relationConfig() ?: continue
        val property = related[relation.database]?.columns?.find { it.id == config.property } ?: continue
        property.relationConfig()?.let { ids += it.database }
    }
    return ids - related.keys
}

/**
 * **Una Relation o un Rollup copiati insieme al loro database** (`originalDb`
 * → `copyDb`): quello che puntava dentro il database punta dentro la copia
 * ([newColumnIds]: colonna originale → colonna della copia). Una Relation
 * verso un altro database resta verso quello, ma in un senso solo: la
 * gemella di là è dell'originale.
 */
fun copiedLinkOptions(
    column: DatabaseColumnEntity,
    originalDb: String,
    copyDb: String,
    newColumnIds: Map<String, String>
): String {
    column.relationConfig()?.let { config ->
        val copied = if (config.database == originalDb) {
            RelationConfig(database = copyDb, reverse = config.reverse?.let { newColumnIds[it] })
        } else {
            RelationConfig(database = config.database)
        }
        return encodeRelationConfig(copied)
    }
    column.rollupConfig()?.let { config ->
        // La proprietà letta è di questo database solo se la Relation punta qui: le altre non sono in `newColumnIds`.
        return encodeRollupConfig(
            config.copy(
                relation = newColumnIds[config.relation] ?: config.relation,
                property = newColumnIds[config.property] ?: config.property
            )
        )
    }
    return column.optionsJson
}

/** I nomi delle righe collegate da una cella Relation, nell'ordine scelto (quelle che non ci sono più si saltano). */
fun relationTitles(value: String?, target: DatabaseSnapshot?): List<String> =
    target?.let { snapshot -> relationIds(value).mapNotNull { snapshot.rowsById[it]?.title } }.orEmpty()

private fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
}

/**
 * **Le due celle di una Relation nei due sensi**: dopo che la riga
 * `rowId` è passata da `before` a `after` nella colonna di qua, che cosa
 * cambia nella colonna gemella di là. Ogni riga aggiunta deve avere `rowId`
 * fra le sue, ogni riga tolta non deve averlo più.
 */
fun reverseChanges(
    rowId: String,
    before: List<String>,
    after: List<String>,
    reverseCells: (String) -> String?
): Map<String, String> {
    val added = after.filterNot { it in before }
    val removed = before.filterNot { it in after }
    val changes = LinkedHashMap<String, String>()
    for (other in added) {
        val ids = relationIds(reverseCells(other))
        if (rowId !in ids) changes[other] = encodeRelationIds(ids + rowId)
    }
    for (other in removed) {
        val ids = relationIds(reverseCells(other))
        if (rowId in ids) changes[other] = encodeRelationIds(ids - rowId)
    }
    return changes
}
