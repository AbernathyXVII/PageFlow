package com.gabriele.notionlocal.data.buttons

import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.MULTI_VALUE_SEPARATOR
import com.gabriele.notionlocal.data.entity.SelectOption
import com.gabriele.notionlocal.data.entity.isTag
import com.gabriele.notionlocal.data.entity.encodeDateRange
import com.gabriele.notionlocal.data.entity.parseDateRange
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * **I bottoni** (dal 27/10/2026, chiesti dall'utente: "una proprietà/funzione
 * che mi consente di triggerare una qualsiasi azione", come i Buttons di
 * Notion e le Routines di Samsung e dell'iPhone).
 *
 * Un bottone è un nome, un'emoji e **una fila di azioni** che partono tutte,
 * in ordine, a ogni tocco. Ce ne sono di due tipi, come su Notion:
 * - il blocco "Button" in una pagina (`BlockType.BUTTON`), con le azioni in
 *   `BlockAttrs.button`;
 * - la proprietà "Button" di un database (`ColumnType.BUTTON`), un bottone su
 *   ogni riga, con le azioni nell'`optionsJson` della colonna: lì "This page"
 *   è la riga del bottone toccato.
 *
 * Un bottone ancora senza azioni, toccato, chiede cosa deve fare: "quando
 * attivo un bottone per la prima volta, mi chiederà che cosa voglio fare".
 * Le azioni le esegue `ButtonRunner`.
 */
@Serializable
data class ButtonConfig(
    /** Il nome scritto sul bottone; vuoto, si legge "New button". */
    val name: String = "",
    /** L'emoji accanto al nome; null, nessuna. */
    val icon: String? = null,
    /** Al posto dell'emoji, un'immagine caricata (il nome del file, `PageImageStore`). */
    val iconImage: String? = null,
    /** Al posto dell'emoji, un'icona di Material, "Home|#E03E3E" (vedi `IconSymbol`). */
    val iconSymbol: String? = null,
    val steps: List<ButtonStep> = emptyList()
) {
    /** Ha già qualcosa da fare: toccandolo parte, se no chiede cosa fare. */
    val isSet: Boolean get() = steps.isNotEmpty()

    val hasIcon: Boolean get() = icon != null || iconImage != null || iconSymbol != null

    /** L'icona scelta nel selettore delle icone, come per le pagine. */
    fun withIcon(choice: IconChoice): ButtonConfig = when (choice) {
        is IconChoice.Emoji -> copy(icon = choice.emoji, iconImage = null, iconSymbol = null)
        is IconChoice.Image -> copy(icon = null, iconImage = choice.fileName, iconSymbol = null)
        is IconChoice.Symbol -> copy(icon = null, iconImage = null, iconSymbol = choice.symbol.encoded)
        IconChoice.None -> copy(icon = null, iconImage = null, iconSymbol = null)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /**
         * Le azioni salvate in una colonna Button. Un JSON rovinato, o quello
         * di un altro tipo ("[]" delle opzioni), vale come un bottone vuoto
         * invece di far chiudere l'app.
         */
        fun parse(text: String?): ButtonConfig =
            text?.takeIf { it.trimStart().startsWith("{") }
                ?.let { runCatching { json.decodeFromString<ButtonConfig>(it) }.getOrNull() }
                ?: ButtonConfig()

        fun encode(config: ButtonConfig): String = json.encodeToString(config)
    }
}

fun newButtonId(): String = UUID.randomUUID().toString()

/** Le azioni di un bottone, come i passi di Notion ("New action"). */
@Serializable
sealed class ButtonStep {
    abstract val id: String

    /** **"Insert blocks"**: dei blocchi già scritti, messi sopra o sotto il bottone, o in cima o in fondo alla pagina. */
    @Serializable
    @SerialName("insert_blocks")
    data class InsertBlocks(
        override val id: String = newButtonId(),
        val blocks: List<ButtonBlock> = listOf(ButtonBlock()),
        val place: InsertPlace = InsertPlace.BELOW_BUTTON
    ) : ButtonStep()

    /** **"Add page to"**: una riga nuova in un database, col nome e i valori scelti. */
    @Serializable
    @SerialName("add_page")
    data class AddPage(
        override val id: String = newButtonId(),
        val databaseId: String? = null,
        val title: String = "",
        val edits: List<PropertyEdit> = emptyList(),
        /** Finite le azioni, si apre la pagina della riga nuova. */
        val openAfter: Boolean = false,
        /**
         * **Il template della pagina nuova** (dal 03/11/2026, per le
         * automazioni: "ogni lunedì crea la pagina della settimana"): la sua
         * pagina nasce col contenuto del template. Null, vuota.
         */
        val templateId: String? = null
    ) : ButtonStep()

    /**
     * **"Edit pages in"**: cambia le proprietà di "This page" (la riga del
     * bottone, o quella di cui la pagina col bottone è la pagina) o delle
     * righe di un database, tutte o solo quelle che passano [filter].
     */
    @Serializable
    @SerialName("edit_pages")
    data class EditPages(
        override val id: String = newButtonId(),
        /** Null: "This page". */
        val databaseId: String? = null,
        val filter: ButtonFilter? = null,
        val edits: List<PropertyEdit> = emptyList()
    ) : ButtonStep()

    /** **"Show confirmation"**: una domanda; "Cancel" ferma le azioni che vengono dopo. */
    @Serializable
    @SerialName("confirm")
    data class Confirm(
        override val id: String = newButtonId(),
        val message: String = ""
    ) : ButtonStep()

    /** **"Open page or URL"**: alla fine delle azioni si apre una pagina dell'app, o un link. */
    @Serializable
    @SerialName("open")
    data class Open(
        override val id: String = newButtonId(),
        val pageId: String? = null,
        val url: String = ""
    ) : ButtonStep()

    /** **"Send notification"**: una notifica sul telefono, subito. */
    @Serializable
    @SerialName("notify")
    data class Notify(
        override val id: String = newButtonId(),
        val title: String = "",
        val text: String = ""
    ) : ButtonStep()
}

/** Dove vanno i blocchi di "Insert blocks". */
@Serializable
enum class InsertPlace { ABOVE_BUTTON, BELOW_BUTTON, TOP_OF_PAGE, BOTTOM_OF_PAGE }

/**
 * Un blocco di "Insert blocks": il tipo (il nome di un `BlockType`, fra
 * [BUTTON_BLOCK_TYPES]) e il testo. Un divisore il testo non ce l'ha.
 */
@Serializable
data class ButtonBlock(
    val id: String = newButtonId(),
    val type: String = "PARAGRAPH",
    val text: String = ""
)

/** I tipi di blocco che "Insert blocks" sa mettere, nell'ordine del menu. */
val BUTTON_BLOCK_TYPES = listOf(
    "PARAGRAPH", "BULLET_LIST_ITEM", "NUMBERED_LIST_ITEM", "CHECKBOX", "TOGGLE", "QUOTE", "CALLOUT", "DIVIDER"
)

/**
 * "Edit pages in": solo le righe in cui la proprietà [columnId] vale uno dei
 * [values] (la casella: "true" o "false"). Come il filtro dei database:
 * passa chi ne ha almeno uno.
 */
@Serializable
data class ButtonFilter(
    val columnId: String,
    val values: List<String> = emptyList()
)

/** Cosa fa al valore di una proprietà: vedi `PropertyEdit.applyTo`. */
@Serializable
enum class EditOp {
    /** Scrive [PropertyEdit.value]: un testo, un numero, il tag, i tag. */
    SET,
    /** Svuota la cella. */
    CLEAR,
    CHECK,
    UNCHECK,
    /** La casella al contrario di com'è. */
    TOGGLE,
    /** Un numero cresce di [PropertyEdit.value]; fra i tag, aggiunge quelli di [PropertyEdit.value]. */
    ADD,
    /** Un numero cala di [PropertyEdit.value]; fra i tag, toglie quelli di [PropertyEdit.value]. */
    REMOVE,
    /** La data: adesso, con l'ora. */
    NOW,
    /** La data: oggi, senza l'ora. */
    TODAY
}

/**
 * Il cambio di una proprietà: quale ([columnId], o [NAME_PROPERTY_ID] per il
 * nome della riga), cosa le fa ([op]) e con cosa ([value]). Più tag stanno in
 * [value] separati da `MULTI_VALUE_SEPARATOR`, come nelle celle.
 */
@Serializable
data class PropertyEdit(
    val id: String = newButtonId(),
    val columnId: String,
    val op: EditOp = EditOp.SET,
    val value: String = ""
)

/** La "proprietà" del nome di una riga, che non è una colonna. */
const val NAME_PROPERTY_ID = "__name__"

/** I tipi di proprietà che un bottone sa cambiare. */
val ColumnType.buttonEditable: Boolean
    get() = this == ColumnType.TEXT || this == ColumnType.NUMBER || this == ColumnType.CHECKBOX ||
        isTag || this == ColumnType.DATE ||
        this == ColumnType.URL || this == ColumnType.EMAIL || this == ColumnType.PHONE

/** Cosa si può fare a una proprietà di questo tipo, nell'ordine del menu; la prima è quella di partenza. */
fun ColumnType.editOps(): List<EditOp> = when (this) {
    ColumnType.CHECKBOX -> listOf(EditOp.CHECK, EditOp.UNCHECK, EditOp.TOGGLE)
    ColumnType.NUMBER -> listOf(EditOp.ADD, EditOp.REMOVE, EditOp.SET, EditOp.CLEAR)
    ColumnType.SELECT -> listOf(EditOp.SET, EditOp.CLEAR)
    ColumnType.MULTI_SELECT, ColumnType.PEOPLE -> listOf(EditOp.ADD, EditOp.REMOVE, EditOp.SET, EditOp.CLEAR)
    ColumnType.DATE -> listOf(EditOp.NOW, EditOp.TODAY, EditOp.CLEAR)
    else -> listOf(EditOp.SET, EditOp.CLEAR)
}

/** Se a [op] serve un valore scritto o scelto. */
fun EditOp.needsValue(): Boolean = this == EditOp.SET || this == EditOp.ADD || this == EditOp.REMOVE

/**
 * **Il valore nuovo di una cella**, dato quello di adesso ([current]); null
 * se il cambio non si può fare (un numero che non è un numero, un tag che la
 * proprietà non ha più) e la cella resta com'è. [now] è l'istante del tocco.
 */
fun PropertyEdit.applyTo(column: DatabaseColumnEntity, current: String, now: Long, zone: ZoneId): String? {
    if (op == EditOp.CLEAR) return ""
    return when (column.type) {
        ColumnType.CHECKBOX -> when (op) {
            EditOp.CHECK -> "true"
            EditOp.UNCHECK -> "false"
            EditOp.TOGGLE -> if (current == "true") "false" else "true"
            else -> value.takeIf { it == "true" || it == "false" }
        }
        ColumnType.NUMBER -> {
            val amount = parseNumber(value) ?: return null
            when (op) {
                EditOp.SET -> amount.plainString()
                EditOp.ADD, EditOp.REMOVE -> {
                    // Una cella vuota parte da zero, come un contatore nuovo.
                    val start = if (current.isBlank()) BigDecimal.ZERO else parseNumber(current) ?: return null
                    (if (op == EditOp.ADD) start + amount else start - amount).plainString()
                }
                else -> null
            }
        }
        ColumnType.SELECT -> if (op == EditOp.SET) {
            value.takeIf { it.isNotBlank() && it in optionLabels(column) }
        } else {
            null
        }
        ColumnType.MULTI_SELECT, ColumnType.PEOPLE -> {
            val known = optionLabels(column)
            val chosen = splitTags(value).filter { it in known }
            if (chosen.isEmpty() && op != EditOp.SET) return null
            val had = splitTags(current)
            val result = when (op) {
                EditOp.SET -> chosen
                EditOp.ADD -> had + chosen.filterNot { it in had }
                EditOp.REMOVE -> had.filterNot { it in chosen }
                else -> return null
            }
            result.joinToString(MULTI_VALUE_SEPARATOR)
        }
        ColumnType.DATE -> {
            // La notifica della data, se c'era, resta: cambia solo il giorno.
            val old = parseDateRange(current)
            when (op) {
                EditOp.NOW -> encodeDateRange(now, hasTime = true, notify = old?.notify == true, reminders = old?.reminders ?: listOf(0))
                EditOp.TODAY -> encodeDateRange(
                    Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli(),
                    notify = old?.notify == true,
                    reminders = old?.reminders ?: listOf(0)
                )
                else -> null
            }
        }
        else -> if (op == EditOp.SET) value else null
    }
}

/** I tag di una cella, nell'ordine in cui ci sono. */
fun splitTags(value: String): List<String> =
    value.split(MULTI_VALUE_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

private val tagsJson = Json { ignoreUnknownKeys = true }

/** I tag di una proprietà Select o Multi-select; nessuno se il JSON non si legge. */
fun DatabaseColumnEntity.selectOptions(): List<SelectOption> =
    runCatching { tagsJson.decodeFromString<List<SelectOption>>(optionsJson) }.getOrDefault(emptyList())

private fun optionLabels(column: DatabaseColumnEntity): Set<String> = column.selectOptions().mapTo(HashSet()) { it.label }

/** Un numero com'è scritto in una cella: col punto, o con la virgola dei decimali. */
fun parseNumber(text: String): BigDecimal? =
    text.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }

/** Senza zeri in fondo e senza notazione scientifica: "5", "2.5", "-0.1". */
private fun BigDecimal.plainString(): String =
    stripTrailingZeros().toPlainString().let { if (it == "-0") "0" else it }

/**
 * Se una riga passa il filtro di "Edit pages in": [cell] è il valore della
 * sua cella (il nome, per [NAME_PROPERTY_ID]), [column] la proprietà, null per
 * il nome. Senza valori da cercare non passa niente: un filtro a metà non
 * deve diventare "tutte le righe".
 */
fun ButtonFilter.matches(column: DatabaseColumnEntity?, cell: String): Boolean {
    val wanted = values.map { it.trim() }.filter { it.isNotEmpty() }
    if (wanted.isEmpty()) return false
    return when (column?.type) {
        // Una casella mai toccata non ha la cella: vale "non spuntata".
        ColumnType.CHECKBOX -> (if (cell == "true") "true" else "false") in wanted
        ColumnType.SELECT, ColumnType.MULTI_SELECT, ColumnType.PEOPLE -> splitTags(cell).any { it in wanted }
        ColumnType.NUMBER -> {
            val number = parseNumber(cell) ?: return false
            wanted.any { w -> parseNumber(w)?.let { it.compareTo(number) == 0 } == true }
        }
        else -> wanted.any { it.equals(cell.trim(), ignoreCase = true) }
    }
}
