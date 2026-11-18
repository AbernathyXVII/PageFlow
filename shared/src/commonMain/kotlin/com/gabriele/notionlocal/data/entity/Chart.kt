package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------------------------------------------------------------------------
// **La vista Chart dei database** (dal 28/09/2026, come su Notion): le righe
// diventano un grafico — barre verticali o orizzontali, una linea, o una
// ciambella. Qui ci sono le impostazioni e i conti; il disegno sta in
// `ui/screen/DatabaseChart.kt`.
// ---------------------------------------------------------------------------

/** I quattro grafici, nell'ordine della finestrella per sceglierli. */
@Serializable
enum class ChartType { VERTICAL, HORIZONTAL, LINE, DONUT }

/**
 * Come si fa un numero solo da tutte le righe di un gruppo, quando quello
 * che si mostra è una proprietà Number. `VALUES` somma i valori (con le
 * righe per nome, è il valore di ogni riga); `DISTINCT` conta i valori
 * diversi.
 */
@Serializable
enum class ChartAggregation { VALUES, SUM, AVERAGE, MIN, MAX, MEDIAN, DISTINCT }

/** "Sort by": per nome del gruppo (A→Z, Z→A) o per il numero (basso→alto, alto→basso). */
@Serializable
enum class ChartSort { LABEL_ASC, LABEL_DESC, VALUE_ASC, VALUE_DESC }

/** "Height" in "More style options". */
@Serializable
enum class ChartHeight { SMALL, MEDIUM, LARGE, EXTRA_LARGE }

/** Una linea di riferimento: una riga tratteggiata a un certo valore, con un nome facoltativo. */
@Serializable
data class ChartReferenceLine(val value: Double, val label: String = "")

/**
 * Le impostazioni di un grafico, salvate nella vista (`PageEntity.chartSettings`):
 * una vista collegata ha le sue, come il resto delle impostazioni di vista.
 *
 * `groupColumnId` è "What to show" dell'asse dei gruppi (null = il nome
 * delle righe); `valueColumnId` quello dei numeri (null = "Count", quante
 * righe ci sono in ogni gruppo).
 */
@Serializable
data class ChartSettings(
    val type: ChartType = ChartType.VERTICAL,
    val groupColumnId: String? = null,
    val valueColumnId: String? = null,
    val aggregation: ChartAggregation = ChartAggregation.VALUES,
    val sort: ChartSort = ChartSort.LABEL_ASC,
    val omitZero: Boolean = false,
    /** Null = quelle della proprietà ("Default"). */
    val decimals: Int? = null,
    val cumulative: Boolean = false,
    /** Null = "Auto". */
    val rangeMin: Double? = null,
    val rangeMax: Double? = null,
    val referenceLines: List<ChartReferenceLine> = emptyList(),
    /** Null = "Auto": un colore per gruppo. Altrimenti un colore solo, in esadecimale. */
    val color: String? = null,
    val height: ChartHeight = ChartHeight.MEDIUM,
    val gridLines: Boolean = true,
    val dataLabels: Boolean = true,
    /** Solo per la linea: curva invece che spezzata. */
    val smoothLine: Boolean = false,
    /** La legenda: per la linea (il nome della serie) e per la ciambella (i gruppi). */
    val showLegend: Boolean = true,
    /** Solo per la ciambella: il totale al centro. */
    val showTotal: Boolean = true
)

private val chartJson = Json { ignoreUnknownKeys = true; encodeDefaults = false }

/** Le impostazioni salvate; quelle di base se non ce ne sono o non si leggono. */
fun parseChartSettings(raw: String?): ChartSettings =
    raw?.takeIf { it.isNotBlank() }?.let { runCatching { chartJson.decodeFromString<ChartSettings>(it) }.getOrNull() }
        ?: ChartSettings()

/** Le impostazioni tutte di base non si salvano: la pagina resta com'era (null). */
fun encodeChartSettings(settings: ChartSettings): String? =
    if (settings == ChartSettings()) null else chartJson.encodeToString(settings)

/**
 * Una barra (o un punto, o una fetta). `key` ordina i gruppi "per nome":
 * le date per data, i numeri per numero, il resto per testo, coi numeri
 * dentro i nomi in ordine naturale ("2" prima di "10").
 */
data class ChartPoint(
    val label: String,
    val value: Double,
    val key: ChartKey,
    /**
     * Il colore del gruppo: il suo posto nell'ordine per nome. Come su
     * Notion, riordinando o togliendo gli zeri ognuno tiene il suo colore.
     */
    val colorIndex: Int = 0
)

sealed class ChartKey : Comparable<ChartKey> {
    data class Number(val value: Double) : ChartKey()
    data class Text(val value: String) : ChartKey()

    override fun compareTo(other: ChartKey): Int = when {
        this is Number && other is Number -> value.compareTo(other.value)
        this is Text && other is Text -> naturalCompare(value, other.value)
        this is Number -> -1
        else -> 1
    }
}

/** Le etichette dei gruppi che non hanno un testo loro. */
data class ChartLabels(
    val untitled: String,
    val empty: String,
    val checked: String,
    val unchecked: String,
    /** Una data di un gruppo per giorno, nel formato della colonna. */
    val day: (LocalDate) -> String
)

/**
 * **Le righe diventano punti del grafico.** Si raggruppano per "What to
 * show" dei gruppi (il nome: una riga, un gruppo), si fa il numero di ogni
 * gruppo, si tolgono gli zeri se richiesto, si ordina e, con "Cumulative",
 * ogni punto somma quelli prima.
 */
fun chartPoints(
    rows: List<DatabaseRowEntity>,
    columns: List<DatabaseColumnEntity>,
    cellValues: Map<Pair<String, String>, String>,
    settings: ChartSettings,
    labels: ChartLabels,
    zone: ZoneId = ZoneId.systemDefault(),
    /** I database collegati dalle Relation: i nomi delle righe di un gruppo per Relation. */
    related: Map<String, DatabaseSnapshot> = emptyMap()
): List<ChartPoint> {
    val groupColumn = settings.groupColumnId?.let { id -> columns.find { it.id == id } }
    val valueColumn = settings.valueColumnId?.let { id -> columns.find { it.id == id && it.type == ColumnType.NUMBER } }

    // Gruppo -> righe, nell'ordine in cui si incontrano.
    data class Group(val label: String, val key: ChartKey, val rows: MutableList<DatabaseRowEntity> = mutableListOf())
    val groups = LinkedHashMap<String, Group>()
    fun add(id: String, label: String, key: ChartKey, row: DatabaseRowEntity) {
        groups.getOrPut(id) { Group(label, key) }.rows += row
    }
    fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    rows.forEach { row ->
        if (groupColumn == null) {
            // Per nome: ogni riga è un gruppo a sé, anche con due nomi uguali.
            val title = row.title.ifBlank { labels.untitled }
            add(row.id, title, ChartKey.Text(title), row)
            return@forEach
        }
        val raw = cellValues[row.id to groupColumn.id].orEmpty()
        when (groupColumn.type) {
            ColumnType.MULTI_SELECT, ColumnType.PEOPLE -> {
                val tags = raw.split(MULTI_VALUE_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
                if (tags.isEmpty()) add("", labels.empty, ChartKey.Text("￿"), row)
                tags.forEach { add(it, it, ChartKey.Text(it), row) }
            }
            // "Relation" (dal 21/10/2026): come i tag, una barra per ogni riga collegata.
            ColumnType.RELATION -> {
                val target = groupColumn.relationConfig()?.let { related[it.database] }
                val linked = target?.let { snapshot -> relationIds(raw).mapNotNull { snapshot.rowsById[it] } }.orEmpty()
                if (linked.isEmpty()) add("", labels.empty, ChartKey.Text("￿"), row)
                linked.forEach { other ->
                    val title = other.title.ifBlank { labels.untitled }
                    add(other.id, title, ChartKey.Text(title), row)
                }
            }
            ColumnType.CHECKBOX -> {
                val checked = raw == "true"
                add(checked.toString(), if (checked) labels.checked else labels.unchecked, ChartKey.Number(if (checked) 1.0 else 0.0), row)
            }
            ColumnType.DATE -> {
                val day = parseDateRange(raw)?.start?.let { dayOf(it) }
                if (day == null) add("", labels.empty, ChartKey.Number(Double.MAX_VALUE), row)
                else add(day.toString(), labels.day(day), ChartKey.Number(day.toEpochDay().toDouble()), row)
            }
            ColumnType.CREATED_TIME, ColumnType.LAST_EDITED_TIME -> {
                val millis = if (groupColumn.type == ColumnType.CREATED_TIME) row.createdAt else row.updatedAt ?: row.createdAt
                val day = dayOf(millis)
                add(day.toString(), labels.day(day), ChartKey.Number(day.toEpochDay().toDouble()), row)
            }
            ColumnType.NUMBER -> {
                val number = raw.trim().toDoubleOrNull()
                if (number == null) add("", labels.empty, ChartKey.Number(Double.MAX_VALUE), row)
                else add(raw.trim(), raw.trim(), ChartKey.Number(number), row)
            }
            ColumnType.COLOR -> {
                val text = parseColorCell(raw).text.trim()
                if (text.isEmpty()) add("", labels.empty, ChartKey.Text("￿"), row) else add(text, text, ChartKey.Text(text), row)
            }
            else -> {
                val text = raw.trim()
                if (text.isEmpty()) add("", labels.empty, ChartKey.Text("￿"), row) else add(text, text, ChartKey.Text(text), row)
            }
        }
    }

    var points = groups.values.map { group ->
        ChartPoint(group.label, valueOf(group.rows, valueColumn, cellValues, settings.aggregation), group.key)
    }
    val byName = points.indices.sortedWith(compareBy { points[it].key })
    val rank = IntArray(points.size).also { r -> byName.forEachIndexed { place, i -> r[i] = place } }
    points = points.mapIndexed { i, point -> point.copy(colorIndex = rank[i]) }
    if (settings.omitZero) points = points.filter { it.value != 0.0 }
    points = when (settings.sort) {
        ChartSort.LABEL_ASC -> points.sortedWith(compareBy { it.key })
        ChartSort.LABEL_DESC -> points.sortedWith(compareByDescending { it.key })
        ChartSort.VALUE_ASC -> points.sortedBy { it.value }
        ChartSort.VALUE_DESC -> points.sortedByDescending { it.value }
    }
    if (settings.cumulative) {
        var total = 0.0
        points = points.map { point -> total += point.value; point.copy(value = total) }
    }
    return points
}

/** Il numero di un gruppo: quante righe, o la proprietà Number fatta come chiede `aggregation`. */
private fun valueOf(
    rows: List<DatabaseRowEntity>,
    valueColumn: DatabaseColumnEntity?,
    cellValues: Map<Pair<String, String>, String>,
    aggregation: ChartAggregation
): Double {
    if (valueColumn == null) return rows.size.toDouble()
    val raw = rows.map { cellValues[it.id to valueColumn.id].orEmpty().trim() }.filter { it.isNotEmpty() }
    val numbers = raw.mapNotNull { it.toBigDecimalOrNull() }
    if (numbers.isEmpty()) return 0.0
    return when (aggregation) {
        ChartAggregation.VALUES, ChartAggregation.SUM -> numbers.fold(BigDecimal.ZERO, BigDecimal::add).toDouble()
        ChartAggregation.AVERAGE -> numbers.fold(BigDecimal.ZERO, BigDecimal::add).toDouble() / numbers.size
        ChartAggregation.MIN -> numbers.min().toDouble()
        ChartAggregation.MAX -> numbers.max().toDouble()
        ChartAggregation.MEDIAN -> {
            val sorted = numbers.map { it.toDouble() }.sorted()
            val mid = sorted.size / 2
            if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
        }
        ChartAggregation.DISTINCT -> numbers.map { it.stripTrailingZeros() }.toSet().size.toDouble()
    }
}

/**
 * Le tacche dell'asse dei numeri: da `min` a `max` con un passo "tondo"
 * (1, 2, 2,5 o 5 per una potenza di dieci), circa `count` intervalli. Come
 * su Notion, un massimo di 400 dà 0, 120, 240, 360, 480.
 */
fun chartTicks(min: Double, max: Double, count: Int = 4, integers: Boolean = false): List<Double> {
    val low = minOf(min, 0.0)
    // Un po' d'aria sopra la barra più alta, come Notion: 400 arriva a 480.
    val top = if (max > 0) max * 1.1 else max
    // Lo zero sempre dentro, anche se i numeri sono tutti sotto.
    val high = maxOf(top, 0.0).let { if (it <= low) low + 1 else it }
    val rough = (high - low) / count
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rough)))
    val nice = listOf(1.0, 1.2, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0).map { it * magnitude }.first { it >= rough }
    // Contando le pagine non si va di 0,3 in 0,3.
    val step = if (integers) maxOf(nice, 1.0) else nice
    val first = Math.floor(low / step) * step
    val ticks = mutableListOf<Double>()
    var tick = first
    while (tick < high + step * 0.5 && ticks.size <= count + 2) {
        ticks += tick
        if (tick >= high) break
        tick += step
    }
    if (ticks.last() < high) ticks += ticks.last() + step
    return ticks
}

/** L'asse dei numeri: dove comincia, dove finisce e le sue tacche. */
data class ChartAxis(val low: Double, val high: Double, val ticks: List<Double>)

/**
 * L'asse dei numeri di un grafico. Da solo sceglie tacche tonde con un po'
 * d'aria sopra; con un intervallo scelto ("Range") divide quello in quattro,
 * così nessuna tacca finisce fuori dal disegno.
 */
fun chartAxis(points: List<ChartPoint>, settings: ChartSettings): ChartAxis {
    val values = points.map { it.value } + settings.referenceLines.map { it.value }
    val auto = chartTicks(
        min = values.minOrNull() ?: 0.0,
        max = values.maxOrNull() ?: 0.0,
        integers = values.all { it == Math.floor(it) }
    )
    val low = settings.rangeMin ?: auto.first()
    val high = settings.rangeMax ?: auto.last()
    if (settings.rangeMin == null && settings.rangeMax == null || high <= low) return ChartAxis(auto.first(), auto.last(), auto)
    return ChartAxis(low, high, (0..4).map { low + (high - low) * it / 4 })
}

/** "1K", "1.5M": il totale al centro della ciambella, come su Notion. */
fun compactNumber(value: Double): String {
    val abs = Math.abs(value)
    val (divisor, suffix) = when {
        abs >= 1e9 -> 1e9 to "B"
        abs >= 1e6 -> 1e6 to "M"
        abs >= 1e3 -> 1e3 to "K"
        else -> 1.0 to ""
    }
    val scaled = value / divisor
    val text = if (suffix.isEmpty()) {
        BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    } else {
        BigDecimal(scaled).setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
    return text + suffix
}

/** Il confronto "naturale": i numeri dentro i testi contano come numeri ("Pagina 2" prima di "Pagina 10"). */
fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            val startA = i
            val startB = j
            while (i < a.length && a[i].isDigit()) i++
            while (j < b.length && b[j].isDigit()) j++
            val na = a.substring(startA, i).trimStart('0')
            val nb = b.substring(startB, j).trimStart('0')
            if (na.length != nb.length) return na.length - nb.length
            val c = na.compareTo(nb)
            if (c != 0) return c
        } else {
            val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
            if (c != 0) return c
            i++
            j++
        }
    }
    return (a.length - i) - (b.length - j)
}

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
