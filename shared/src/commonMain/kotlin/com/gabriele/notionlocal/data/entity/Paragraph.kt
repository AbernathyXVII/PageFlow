package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **"Paragraph"** (dal 21/10/2026, chiesto dall'utente, come lo spacing del
 * paragrafo di Word): quanto spazio c'è fra un blocco e l'altro, e fra il
 * numero, il pallino o la casella di un elenco e il primo carattere.
 *
 * Tre livelli, e vince il più vicino al testo: un gruppo di righe
 * (`BlockAttrs.spacing` e `BlockAttrs.markerGap`), la pagina
 * (`PageEntity.paragraphJson`), Settings (`AppSettings`). Tutte le misure
 * sono in punti; per lo spazio dopo il marcatore null vuol dire "come
 * l'app l'ha sempre disegnato".
 */

/** Lo spazio in più fra un blocco e l'altro fra cui scegliere, in punti: 0 è com'era. */
val BLOCK_SPACING_STEPS = listOf(0, 2, 4, 6, 8, 10, 12, 16, 20, 24, 32)

/** Lo spazio dopo il marcatore fra cui scegliere, in punti: null è "Default", com'era. */
val MARKER_GAP_STEPS: List<Int?> = listOf(null, 0, 2, 4, 6, 8, 10, 12, 16, 20, 24, 32)

/** Gli elenchi che hanno un marcatore davanti al testo. */
val MARKER_LIST_TYPES = listOf(BlockType.NUMBERED_LIST_ITEM, BlockType.BULLET_LIST_ITEM, BlockType.CHECKBOX)

/**
 * Lo spazio dopo il marcatore degli elenchi: uno solo per tutti
 * ([same], chiesto dall'utente) o uno per numbered, bulleted e to-do.
 */
@Serializable
data class ListGaps(
    val same: Boolean = true,
    val all: Int? = null,
    val numbered: Int? = null,
    val bulleted: Int? = null,
    val todo: Int? = null
) {
    /** Lo spazio per un elenco di questo tipo; null = come l'app l'ha sempre disegnato. */
    fun of(type: BlockType): Int? = if (same) all else when (type) {
        BlockType.NUMBERED_LIST_ITEM -> numbered
        BlockType.BULLET_LIST_ITEM -> bulleted
        BlockType.CHECKBOX -> todo
        else -> null
    }

    /** Con lo spazio di un tipo cambiato; null come tipo = quello di tutti. */
    fun with(type: BlockType?, value: Int?): ListGaps = when (type) {
        null -> copy(all = value)
        BlockType.NUMBERED_LIST_ITEM -> copy(numbered = value)
        BlockType.BULLET_LIST_ITEM -> copy(bulleted = value)
        BlockType.CHECKBOX -> copy(todo = value)
        else -> this
    }

    /**
     * Uguale per tutti o no. Accendendolo, tutti prendono quello dei
     * numerati; spegnendolo, ognuno parte da quello di tutti: così il
     * testo non salta nell'istante in cui si tocca l'interruttore.
     */
    fun withSame(value: Boolean): ListGaps = when {
        value == same -> this
        value -> copy(same = true, all = numbered)
        else -> copy(same = false, numbered = all, bulleted = all, todo = all)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        fun parse(text: String?): ListGaps? =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString<ListGaps>(it) }.getOrNull() }

        fun encode(gaps: ListGaps): String = json.encodeToString(gaps)
    }
}

/**
 * Il "Paragraph" di una pagina, in `PageEntity.paragraphJson`. Ogni parte
 * null segue Settings.
 */
@Serializable
data class PageParagraph(
    val blockSpacing: Int? = null,
    val gaps: ListGaps? = null,
    /**
     * **"Full width"** (dal 12/11/2026, solo sul PC, chiesto dall'utente come
     * in Notion): `false` = il testo in una colonna al centro, null = largo
     * quanto lo spazio, com'era. Sta qui, con le altre misure della pagina,
     * per non cambiare il database.
     */
    val fullWidth: Boolean? = null
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        /** Un JSON rovinato vale come "tutto come Settings", invece di far chiudere l'app. */
        fun parse(text: String?): PageParagraph =
            text?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<PageParagraph>(it) }.getOrNull() }
                ?: PageParagraph()

        /** Null quando la pagina segue Settings in tutto: la colonna resta vuota. */
        fun encode(paragraph: PageParagraph): String? =
            if (paragraph == PageParagraph()) null else json.encodeToString(paragraph)
    }
}

/** Il "Paragraph" di questa pagina: vedi `PageParagraph`. */
val PageEntity.paragraph: PageParagraph get() = PageParagraph.parse(paragraphJson)

/**
 * **Le misure che valgono per ogni blocco di una pagina**: quelle del
 * blocco, se ne ha, poi quelle della pagina, poi quelle di Settings.
 */
data class ParagraphLook(
    val page: PageParagraph = PageParagraph(),
    val appSpacing: Int = 0,
    val appGaps: ListGaps = ListGaps()
) {
    /** Lo spazio in più sopra e sotto il blocco, in punti (metà per parte). */
    fun spacingOf(block: BlockEntity): Int =
        block.paragraphAttrs?.spacing ?: page.blockSpacing ?: appSpacing

    /** Lo spazio della pagina, senza guardare i blocchi. */
    val pageSpacing: Int get() = page.blockSpacing ?: appSpacing

    /** Gli spazi dopo i marcatori della pagina, senza guardare i blocchi. */
    val pageGaps: ListGaps get() = page.gaps ?: appGaps

    /**
     * Lo spazio dopo il marcatore di [block], che è (o tiene il posto di) un
     * elenco di tipo [type]; null = come l'app l'ha sempre disegnato.
     */
    fun gapOf(block: BlockEntity?, type: BlockType): Int? =
        block?.paragraphAttrs?.markerGap ?: pageGaps.of(type)

    /** Vero se nessun livello cambia niente: la pagina si disegna come prima. */
    val isPlain: Boolean
        // La larghezza della pagina non cambia gli spazi fra i blocchi.
        get() = page.copy(fullWidth = null) == PageParagraph() && appSpacing == 0 && appGaps.of(BlockType.NUMBERED_LIST_ITEM) == null &&
            appGaps.of(BlockType.BULLET_LIST_ITEM) == null && appGaps.of(BlockType.CHECKBOX) == null
}

/**
 * Dove cambia lo spazio fra i blocchi, fra queste righe in fila (quelle in
 * cima alla pagina, nel loro ordine): id della prima riga di testo che ha
 * uno spazio diverso da quella sopra → chiave del suo campo. Il campo di
 * testo condiviso ha una sola altezza di riga (vedi `CHECKBOX_LINE_HEIGHT`
 * nella schermata), quindi un gruppo con uno spazio suo è un campo a sé.
 *
 * La chiave è la riga **sopra** il confine: scrivendo nel gruppo, anche
 * con un Invio a inizio della sua prima riga, non cambia, e il campo col
 * cursore non si rifà.
 */
fun spacingStartsOf(
    blocks: List<BlockEntity>,
    look: ParagraphLook,
    isFlowing: (BlockType) -> Boolean
): Map<String, String> {
    var previous: BlockEntity? = null
    var previousSpacing = 0
    var starts: HashMap<String, String>? = null
    for (block in blocks) {
        if (!isFlowing(block.type)) {
            previous = null
            continue
        }
        val spacing = look.spacingOf(block)
        val above = previous
        if (above != null && spacing != previousSpacing) {
            if (starts == null) starts = HashMap()
            starts[block.id] = SPACING_KEY_PREFIX + above.id
        }
        previous = block
        previousSpacing = spacing
    }
    return starts ?: emptyMap()
}

/** Come comincia la chiave del campo di un gruppo di righe con uno spazio suo. */
const val SPACING_KEY_PREFIX = "space:"
