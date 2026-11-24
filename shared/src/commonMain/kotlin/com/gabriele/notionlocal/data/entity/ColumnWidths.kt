package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Le larghezze delle colonne della tabella** (dal 29/09/2026, chieste
 * dall'utente "come in Excel"): ogni vista ha le sue, come su Notion.
 * `PageEntity.columnWidths` le tiene in dp per chiave — l'id della colonna,
 * o [NAME_COLUMN_KEY] per Name; una colonna senza la sua ha la larghezza di
 * sempre. `PageEntity.fitColumns` dice quali si allargano da sole quanto il
 * loro contenuto ("Fit to content"): finché è acceso la larghezza salvata
 * resta da parte, e spegnendolo torna quella.
 */
const val NAME_COLUMN_KEY = "__name__"

/** Più stretta di così una colonna non si legge più: resta l'icona o poco più. */
const val MIN_COLUMN_WIDTH_DP = 48

/** Un limite solo di sicurezza: un testo lunghissimo non fa una tabella di chilometri. */
const val MAX_COLUMN_WIDTH_DP = 1200

private val widthsJson = Json { ignoreUnknownKeys = true }

fun parseColumnWidths(raw: String?): Map<String, Int> =
    raw?.let { runCatching { widthsJson.decodeFromString<Map<String, Int>>(it) }.getOrNull() }.orEmpty()

fun encodeColumnWidths(widths: Map<String, Int>): String? =
    widths.takeIf { it.isNotEmpty() }?.let { widthsJson.encodeToString<Map<String, Int>>(it.toSortedMap()) }

fun parseFitColumns(raw: String?): Set<String> =
    raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet().orEmpty()

fun encodeFitColumns(keys: Set<String>): String? =
    keys.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")

fun clampColumnWidth(widthDp: Int, min: Int = MIN_COLUMN_WIDTH_DP): Int = widthDp.coerceIn(min, MAX_COLUMN_WIDTH_DP)

/**
 * **Le colonne in cui si scrive** (dal 30/09/2026, chiesto dall'utente) non
 * vanno sotto questa larghezza: testo, numero, collegamento, email,
 * telefono, cella colorata. Più strette non ci starebbe nemmeno una parola
 * coi tre puntini.
 */
const val MIN_WRITABLE_COLUMN_WIDTH_DP = 80

/** Name un po' di più: accanto al nome c'è OPEN. */
const val MIN_NAME_COLUMN_WIDTH_DP = 120

private val WRITABLE_COLUMN_TYPES = setOf(
    ColumnType.TEXT,
    ColumnType.NUMBER,
    ColumnType.URL,
    ColumnType.EMAIL,
    ColumnType.PHONE,
    ColumnType.COLOR
)

/**
 * **Quanto si può stringere una colonna trascinandola: fin quasi a niente** (dal 18/10/2026, chiesto
 * dall'utente: «rimpicciolire la lunghezza delle colonne quanto voglio, anche fino al massimo tutto verso
 * sinistra»), ma non sotto [MIN_REACHABLE_COLUMN_WIDTH_DP]. Prima ogni colonna aveva un minimo (Name 120,
 * quelle in cui si scrive 80, le altre 48); restano per "Fit to content" (vedi `fitMinColumnWidth`).
 */
fun minColumnWidth(key: String, type: ColumnType? = null): Int = MIN_REACHABLE_COLUMN_WIDTH_DP

/**
 * **La colonna più stretta che si tocca ancora** (dal 24/11/2026, trovato dall'utente sul telefono:
 * stretta fino a niente, il suo titolo non si toccava più, e con lui il menu per riallargarla). Vale anche
 * per le misure salvate prima, più strette: si disegnano larghe così.
 */
const val MIN_REACHABLE_COLUMN_WIDTH_DP = 32

/** Più stretta di così "Fit to content" non fa una colonna: si legge ancora qualcosa. */
fun fitMinColumnWidth(key: String, type: ColumnType? = null): Int = when {
    key == NAME_COLUMN_KEY -> MIN_NAME_COLUMN_WIDTH_DP
    type in WRITABLE_COLUMN_TYPES -> MIN_WRITABLE_COLUMN_WIDTH_DP
    // "Image" nasce quadrata a 44: il minimo non la allarga.
    else -> minOf(MIN_COLUMN_WIDTH_DP, defaultColumnWidth(key, type))
}

/** La larghezza di sempre: 150, Name 200. */
const val DEFAULT_COLUMN_WIDTH_DP = 150
const val DEFAULT_NAME_COLUMN_WIDTH_DP = 200

/**
 * Trascinando fin quasi alla larghezza di sempre (a 4 dp o meno) ci si
 * ferma lì: col dito non si ritroverebbe mai il punto preciso.
 */
const val SNAP_TO_DEFAULT_DP = 4

/** Una colonna "Image" è un quadrato: larga quanto è alta una riga. */
const val DEFAULT_IMAGE_COLUMN_WIDTH_DP = 44

/**
 * Una colonna "Duration" nasce più larga: ci sta tutta "30 years, 7 months,
 * 17 days, 21 hours and 22 minutes", che in 150 si fermerebbe a "30 years, 7…".
 */
const val DEFAULT_DURATION_COLUMN_WIDTH_DP = 320

fun defaultColumnWidth(key: String, type: ColumnType? = null): Int = when {
    key == NAME_COLUMN_KEY -> DEFAULT_NAME_COLUMN_WIDTH_DP
    type == ColumnType.IMAGE -> DEFAULT_IMAGE_COLUMN_WIDTH_DP
    type == ColumnType.DURATION -> DEFAULT_DURATION_COLUMN_WIDTH_DP
    else -> DEFAULT_COLUMN_WIDTH_DP
}

/** Dentro i limiti (`min`: vedi [minColumnWidth]), e agganciata alla larghezza di sempre se ci è vicina. */
fun snapColumnWidth(key: String, widthDp: Int, default: Int = defaultColumnWidth(key), min: Int = minColumnWidth(key)): Int {
    return if (kotlin.math.abs(widthDp - default) <= SNAP_TO_DEFAULT_DP) default else clampColumnWidth(widthDp, min)
}
