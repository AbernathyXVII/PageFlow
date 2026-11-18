package com.gabriele.notionlocal.data.entity

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * **Come si scrive un numero** in una colonna Number (dal 26/09/2026):
 * semplice, in percentuale o in una delle valute, nell'ordine scelto
 * dall'utente. `symbol` è il segno della valuta; `after` = il segno va dopo
 * il numero (100 kr, 100 zł, 100 ₽), altrimenti prima ($100).
 */
enum class NumberFormatKind(val label: String, val symbol: String = "", val after: Boolean = false) {
    NUMBER("Number"),
    PERCENT("Percent"),
    USD("US Dollar (USD)", "$"),
    EUR("Euro (EUR)", "€"),
    GBP("Pound (GBP)", "£"),
    CHF("Swiss Franc (CHF)", "CHF "),
    CNY("Yuan (CNY)", "¥"),
    JPY("Yen (JPY)", "¥"),
    KRW("Won (KRW)", "₩"),
    AUD("Australian Dollar (AUD)", "A$"),
    CAD("Canadian Dollar (CAD)", "CA$"),
    SGD("Singapore Dollar (SGD)", "S$"),
    RUB("Ruble (RUB)", " ₽", after = true),
    INR("Rupee (INR)", "₹"),
    HKD("Hong Kong Dollar (HKD)", "HK$"),
    NZD("New Zealand Dollar (NZD)", "NZ$"),
    SEK("Swedish Krone (SEK)", " kr", after = true),
    NOK("Norwegian Krone (NOK)", " kr", after = true),
    MXN("Mexican Peso (MXN)", "MX$"),
    TWD("Taiwan Dollar (TWD)", "NT$"),
    DKK("Danish Krone (DKK)", " kr.", after = true),
    PLN("Złoty (PLN)", " zł", after = true);

    val isCurrency: Boolean get() = symbol.isNotEmpty()

    companion object {
        /** Letto dal database: un nome sconosciuto vale "Number", invece di far chiudere l'app. */
        fun of(name: String?): NumberFormatKind = entries.find { it.name == name } ?: NUMBER
    }
}

/** Come si mostra un numero nella cella: il numero, una barra, un anello. */
enum class NumberDisplay {
    NUMBER,
    BAR,
    RING;

    companion object {
        fun of(name: String?): NumberDisplay = entries.find { it.name == name } ?: NUMBER
    }
}

/** Il verde di Notion per barre e anelli, finché non se ne sceglie un altro. */
const val DEFAULT_NUMBER_COLOR = "#4DAB6F"

val DatabaseColumnEntity.numberFormatKind: NumberFormatKind get() = NumberFormatKind.of(numberFormat)
val DatabaseColumnEntity.numberDisplayKind: NumberDisplay get() = NumberDisplay.of(numberDisplay)

/**
 * Il numero scritto nella cella con le **cifre decimali scelte**, prima
 * del formato: "45.76" con una cifra diventa "45.7" — la cifra in più si
 * nasconde, non si arrotonda e non si cancella (il valore salvato resta
 * quello scritto); con tre diventa "45.760". Null = come l'ha scritto
 * l'utente. Null anche se non è un numero.
 */
fun numberWithDecimals(raw: String, decimals: Int?): String? {
    val value = raw.trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNullSafe() ?: return null
    val shown = if (decimals == null) value else value.setScale(decimals.coerceIn(0, 5), RoundingMode.DOWN)
    return shown.toPlainString()
}

/** Il segno della valuta o della percentuale intorno al numero già scritto coi separatori. */
fun NumberFormatKind.decorate(grouped: String): String {
    val negative = grouped.startsWith("-")
    val digits = grouped.removePrefix("-")
    val sign = if (negative) "-" else ""
    return when {
        this == NumberFormatKind.PERCENT -> "$sign$digits%"
        !isCurrency -> grouped
        after -> "$sign$digits$symbol"
        else -> "$sign$symbol$digits"
    }
}

/**
 * Quanto è piena la barra o l'anello: il numero diviso 100, fra vuoto e
 * pieno ("Divide by" di Notion, fisso a 100: l'utente non lo vuole
 * scegliere). 72 → tre quarti scarsi.
 */
fun numberFill(raw: String): Float {
    val value = raw.trim().toBigDecimalOrNullSafe() ?: return 0f
    return (value.toFloat() / 100f).coerceIn(0f, 1f)
}

private fun String.toBigDecimalOrNullSafe(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

// --- I calcoli sotto la tabella ("Calculate") ---

/**
 * **Un calcolo in fondo a una colonna** (dal 26/09/2026), come "Calculate"
 * di Notion. `group` dice sotto quale voce del menu sta.
 */
enum class Calculation(val group: CalculationGroup) {
    COUNT_ALL(CalculationGroup.COUNT),
    COUNT_VALUES(CalculationGroup.COUNT),
    COUNT_UNIQUE(CalculationGroup.COUNT),
    COUNT_EMPTY(CalculationGroup.COUNT),
    COUNT_NOT_EMPTY(CalculationGroup.COUNT),
    CHECKED(CalculationGroup.COUNT),
    UNCHECKED(CalculationGroup.COUNT),
    PERCENT_EMPTY(CalculationGroup.PERCENT),
    PERCENT_NOT_EMPTY(CalculationGroup.PERCENT),
    EARLIEST_DATE(CalculationGroup.DATE),
    LATEST_DATE(CalculationGroup.DATE),
    DATE_RANGE(CalculationGroup.DATE),
    SUM(CalculationGroup.MORE),
    AVERAGE(CalculationGroup.MORE),
    MEDIAN(CalculationGroup.MORE),
    MIN(CalculationGroup.MORE),
    MAX(CalculationGroup.MORE),
    RANGE(CalculationGroup.MORE);

    companion object {
        fun of(name: String?): Calculation? = entries.find { it.name == name }
    }
}

enum class CalculationGroup { COUNT, PERCENT, DATE, MORE }

/**
 * I calcoli che una colonna può avere, per gruppo. Count e Percent per
 * tutte; per le caselle Count diventa "Count all / Checked / Unchecked";
 * le date hanno anche "Date", i numeri "More options". `type` null = la
 * colonna del nome.
 */
fun calculationsFor(type: ColumnType?): Map<CalculationGroup, List<Calculation>> = buildMap {
    put(
        CalculationGroup.COUNT,
        if (type == ColumnType.CHECKBOX) {
            listOf(Calculation.COUNT_ALL, Calculation.CHECKED, Calculation.UNCHECKED)
        } else {
            listOf(
                Calculation.COUNT_ALL, Calculation.COUNT_VALUES, Calculation.COUNT_UNIQUE,
                Calculation.COUNT_EMPTY, Calculation.COUNT_NOT_EMPTY
            )
        }
    )
    put(CalculationGroup.PERCENT, listOf(Calculation.PERCENT_EMPTY, Calculation.PERCENT_NOT_EMPTY))
    // Una formula (dal 28/10/2026) può dare date o numeri: ha tutti e due.
    if (type == ColumnType.DATE || type == ColumnType.FORMULA) {
        put(CalculationGroup.DATE, listOf(Calculation.EARLIEST_DATE, Calculation.LATEST_DATE, Calculation.DATE_RANGE))
    }
    if (type == ColumnType.NUMBER || type == ColumnType.FORMULA) {
        put(
            CalculationGroup.MORE,
            listOf(Calculation.SUM, Calculation.AVERAGE, Calculation.MEDIAN, Calculation.MIN, Calculation.MAX, Calculation.RANGE)
        )
    }
}

/** La chiave della colonna del nome nei calcoli di una vista: non è una proprietà, non ha un id. */
const val NAME_CALCULATION_KEY = "__name__"

/** I calcoli di una vista, "colonna=CALCOLO;…", come li salva `PageEntity.viewCalculations`. */
fun parseViewCalculations(text: String?): Map<String, Calculation> =
    text.orEmpty().split(";").mapNotNull { part ->
        val i = part.indexOf('=')
        if (i <= 0) return@mapNotNull null
        val calc = Calculation.of(part.substring(i + 1)) ?: return@mapNotNull null
        part.substring(0, i) to calc
    }.toMap()

fun encodeViewCalculations(
    calcs: Map<String, Calculation>,
    alignments: Map<String, CellAlignment> = emptyMap(),
    subRows: Set<String> = emptySet()
): String? = (
    calcs.entries.map { "${it.key}=${it.value.name}" } +
        alignments.entries.map { "$ALIGN_PREFIX${it.key}=${it.value.name}" } +
        subRows.sorted().map { "$SUB_ROWS_PREFIX$it=1" }
    ).joinToString(";").ifEmpty { null }

/**
 * **I calcoli che contano anche le sotto-righe** (dal 29/09/2026, "Include
 * sub rows" nella finestra di "Calculate"): nella stessa stringa, con le
 * chiavi "subrows:colonna". Chi manca conta solo le righe principali.
 */
fun parseCalculationSubRows(text: String?): Set<String> =
    text.orEmpty().split(";").mapNotNull { part ->
        if (!part.startsWith(SUB_ROWS_PREFIX)) return@mapNotNull null
        val i = part.indexOf('=')
        if (i <= SUB_ROWS_PREFIX.length) return@mapNotNull null
        part.substring(SUB_ROWS_PREFIX.length, i)
    }.toSet()

private const val SUB_ROWS_PREFIX = "subrows:"

/**
 * **Dove sta il risultato di ogni calcolo** sotto la tabella (dal
 * 26/09/2026): nella stessa stringa dei calcoli, con le chiavi
 * "align:colonna". Chi manca sta a destra, come su Notion.
 */
fun parseCalculationAlignments(text: String?): Map<String, CellAlignment> =
    text.orEmpty().split(";").mapNotNull { part ->
        if (!part.startsWith(ALIGN_PREFIX)) return@mapNotNull null
        val i = part.indexOf('=')
        if (i <= ALIGN_PREFIX.length) return@mapNotNull null
        val align = CellAlignment.of(part.substring(i + 1)) ?: return@mapNotNull null
        part.substring(ALIGN_PREFIX.length, i) to align
    }.toMap()

private const val ALIGN_PREFIX = "align:"

/**
 * Il risultato di un calcolo, prima di essere scritto: un numero di pagine,
 * una percentuale, un numero della colonna, una data o un numero di giorni.
 */
sealed class CalculationResult {
    data class Count(val n: Int) : CalculationResult()
    data class Percent(val value: Double) : CalculationResult()
    data class Number(val value: BigDecimal) : CalculationResult()
    data class Date(val epochMillis: Long) : CalculationResult()
    data class Days(val days: Long) : CalculationResult()
    object Nothing : CalculationResult()
}

/**
 * **Il calcolo sulle righe mostrate.** `values` sono i valori grezzi della
 * colonna, uno per riga (vuoto se la cella è vuota); `startOfDate` legge
 * l'inizio di una data. Per una selezione multipla i valori sono le
 * etichette separate da `MULTI_VALUE_SEPARATOR`: "Count values" le conta
 * tutte, "Count unique values" una volta sola ciascuna.
 */
fun calculate(
    calculation: Calculation,
    type: ColumnType?,
    values: List<String>,
    startOfDate: (String) -> Long? = { null }
): CalculationResult {
    fun isEmpty(v: String) = if (type == ColumnType.CHECKBOX) v != "true" else v.isBlank()
    fun pieces(v: String): List<String> =
        // Una Relation (dal 21/10/2026) conta come i tag: ogni riga collegata è un valore.
        if (type?.isMultiTag == true || type == ColumnType.RELATION) {
            v.split(MULTI_VALUE_SEPARATOR).filter { it.isNotBlank() }
        } else {
            listOf(v)
        }
    val numbers by lazy { values.mapNotNull { it.trim().takeIf { t -> t.isNotEmpty() }?.let { t -> runCatching { BigDecimal(t) }.getOrNull() } } }
    val dates by lazy { values.mapNotNull { v -> v.takeIf { it.isNotBlank() }?.let(startOfDate) } }
    val all = values.size
    val empty = values.count(::isEmpty)
    return when (calculation) {
        Calculation.COUNT_ALL -> CalculationResult.Count(all)
        Calculation.COUNT_VALUES -> CalculationResult.Count(values.filterNot(::isEmpty).sumOf { pieces(it).size })
        Calculation.COUNT_UNIQUE -> CalculationResult.Count(values.filterNot(::isEmpty).flatMap(::pieces).map { it.trim() }.toSet().size)
        Calculation.COUNT_EMPTY, Calculation.UNCHECKED -> CalculationResult.Count(empty)
        Calculation.COUNT_NOT_EMPTY, Calculation.CHECKED -> CalculationResult.Count(all - empty)
        Calculation.PERCENT_EMPTY -> if (all == 0) CalculationResult.Nothing else CalculationResult.Percent(empty * 100.0 / all)
        Calculation.PERCENT_NOT_EMPTY -> if (all == 0) CalculationResult.Nothing else CalculationResult.Percent((all - empty) * 100.0 / all)
        Calculation.EARLIEST_DATE -> dates.minOrNull()?.let { CalculationResult.Date(it) } ?: CalculationResult.Nothing
        Calculation.LATEST_DATE -> dates.maxOrNull()?.let { CalculationResult.Date(it) } ?: CalculationResult.Nothing
        Calculation.DATE_RANGE -> if (dates.isEmpty()) {
            CalculationResult.Nothing
        } else {
            CalculationResult.Days((dates.max() - dates.min()) / 86_400_000L)
        }
        Calculation.SUM -> CalculationResult.Number(numbers.fold(BigDecimal.ZERO, BigDecimal::add))
        Calculation.AVERAGE -> if (numbers.isEmpty()) {
            CalculationResult.Nothing
        } else {
            CalculationResult.Number(
                numbers.fold(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal(numbers.size), 2, RoundingMode.HALF_UP).stripTrailingZeros()
            )
        }
        Calculation.MEDIAN -> if (numbers.isEmpty()) {
            CalculationResult.Nothing
        } else {
            val sorted = numbers.sorted()
            val mid = sorted.size / 2
            CalculationResult.Number(
                if (sorted.size % 2 == 1) {
                    sorted[mid]
                } else {
                    (sorted[mid - 1] + sorted[mid]).divide(BigDecimal(2), 2, RoundingMode.HALF_UP).stripTrailingZeros()
                }
            )
        }
        Calculation.MIN -> numbers.minOrNull()?.let { CalculationResult.Number(it) } ?: CalculationResult.Nothing
        Calculation.MAX -> numbers.maxOrNull()?.let { CalculationResult.Number(it) } ?: CalculationResult.Nothing
        Calculation.RANGE -> if (numbers.isEmpty()) {
            CalculationResult.Nothing
        } else {
            CalculationResult.Number(numbers.max() - numbers.min())
        }
    }
}

/** Le scelte di "Edit property" per una colonna Number, tutte insieme. */
data class NumberSettings(
    val format: NumberFormatKind = NumberFormatKind.NUMBER,
    val decimals: Int? = null,
    val display: NumberDisplay = NumberDisplay.NUMBER,
    val color: String? = null,
    val showNumber: Boolean = true,
    val ringCentered: Boolean = false
)

fun DatabaseColumnEntity.numberSettings(): NumberSettings =
    NumberSettings(numberFormatKind, decimalPlaces, numberDisplayKind, numberColor, showNumber, ringCentered)

fun DatabaseColumnEntity.withNumberSettings(settings: NumberSettings): DatabaseColumnEntity = copy(
    numberFormat = settings.format.takeIf { it != NumberFormatKind.NUMBER }?.name,
    decimalPlaces = settings.decimals,
    numberDisplay = settings.display.takeIf { it != NumberDisplay.NUMBER }?.name,
    numberColor = settings.color,
    showNumber = settings.showNumber,
    ringCentered = settings.ringCentered
)

// --- Le colonne Date: formato della data e dell'ora (dal 26/09/2026) ---

/** Come si scrive l'ora di una data: a 24 ore (21:30), a 12 ore (9:30 PM), o per niente. */
enum class TimeFormatKind {
    H24,
    H12,
    HIDDEN;

    companion object {
        /** Null o un nome sconosciuto: a 24 ore, com'era prima. */
        fun of(name: String?): TimeFormatKind = entries.find { it.name == name } ?: H24
    }
}

// Le scelte di "Edit property" di una colonna Date (`DateSettings`: il formato
// della data e quello dell'ora) non ci sono più: dal 16/10/2026 sono
// "Date format" e "Time format" in Settings, uguali per tutta l'app.
