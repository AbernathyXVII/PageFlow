package com.gabriele.notionlocal.data.entity

import com.gabriele.notionlocal.data.buttons.ButtonConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * **Quello che un blocco ha in più del testo** (dal 06/10/2026), salvato in
 * `BlockEntity.attrsJson`.
 *
 * Serve ai blocchi chiesti dall'utente quel giorno — riquadro, codice,
 * immagine, video, audio, file, segnalibro, link a una pagina — e ognuno
 * usa solo le voci che lo riguardano. Una colonna sola in JSON invece di
 * dieci colonne quasi sempre vuote: ogni voce ha un valore predefinito,
 * quindi aggiungerne una domani non chiede una migrazione, come per
 * `RichTextSpan`.
 *
 * I file stanno nella cartella delle immagini dell'app (`PageImageStore`),
 * per **nome** e non per percorso: lo stesso motivo per cui ci stanno le
 * icone e le copertine.
 */
@Serializable
data class BlockAttrs(
    /** Il file copiato dentro l'app: l'immagine, il video, l'audio o il file. */
    val file: String? = null,
    /** Un indirizzo: il video o l'audio presi da un link, il sito del segnalibro, il file da un link. */
    val url: String? = null,
    /** Il nome del file com'era sul telefono; per il segnalibro, il titolo del sito. */
    val name: String? = null,
    /** Il tipo del file ("video/mp4", "application/pdf"…): serve per aprirlo con l'app giusta. */
    val mime: String? = null,
    /** Quanto pesa il file, in byte. */
    val size: Long? = null,
    /** L'immagine: quanto è larga, in frazione della larghezza della pagina (da 0,2 a 1). */
    val width: Float? = null,
    /**
     * L'immagine, dopo "Full resize" (dal 20/10/2026): dove comincia da
     * sinistra, in frazione della larghezza della pagina; null = al centro.
     */
    val left: Float? = null,
    /**
     * L'immagine, dopo "Full resize": quanto è alta rispetto a quanto è larga
     * (il riquadro, che l'immagine riempie); null = la sua forma.
     */
    val aspect: Float? = null,
    /** Il codice: il linguaggio scelto, col nome com'è nell'elenco (`CODE_LANGUAGES`). */
    val language: String? = null,
    /** Il riquadro: l'emoji dell'icona; null = 💡, "" = nessuna icona. */
    val icon: String? = null,
    /** Il riquadro: l'immagine messa al posto dell'emoji. */
    val iconImage: String? = null,
    /** Il riquadro: un'icona di Material, "Home" o "Home|#E03E3E" (vedi `IconSymbol`). */
    val symbol: String? = null,
    /** Il riquadro: il colore dello sfondo, "#RRGGBB"; null = il grigio di sempre. */
    val color: String? = null,
    /** "Link to page": la pagina richiamata. */
    val pageId: String? = null,
    /** Il segnalibro: la descrizione del sito. */
    val description: String? = null,
    /** Il segnalibro: l'immagine di anteprima del sito, scaricata nell'app. */
    val image: String? = null,
    /**
     * **Testo che tiene il posto di un elenco** (dal 18/10/2026, chiesto
     * dall'utente, come su Word): il tipo dell'elenco da cui viene
     * ("BULLET_LIST_ITEM" o "NUMBERED_LIST_ITEM"). Il primo backspace a
     * inizio di un elemento toglie il numero o il pallino ma il testo resta
     * dov'era, allineato agli altri; il secondo lo porta a sinistra. Conta
     * solo su un paragrafo, e cambiando tipo se ne va (vedi
     * `PageRepository.setBlockType`).
     */
    val listIndent: String? = null,
    /**
     * **Un punto di arrivo dei collegamenti** (dal 20/10/2026, chiesto
     * dall'utente, come l'indice di un libro su Kindle): segnato dal menu dei
     * sei puntini, compare in "In this page" nella finestra del collegamento.
     * Vedi `BlockLinks.kt`.
     */
    val linkTarget: Boolean = false,
    /**
     * **"Paragraph" di un gruppo di righe** (dal 21/10/2026, chiesto
     * dall'utente, come su Word): lo spazio in più fra questo blocco e quelli
     * vicini, in punti; null = quello della pagina. Vedi `ParagraphLook`.
     */
    val spacing: Int? = null,
    /**
     * Lo spazio fra il numero, il pallino o la casella e il primo carattere,
     * in punti; null = quello della pagina. Vedi `ParagraphLook`.
     */
    val markerGap: Int? = null,
    /**
     * **Il bottone** (dal 27/10/2026): nome, emoji e azioni. Vedi
     * `data/buttons/Buttons.kt`.
     */
    val button: ButtonConfig? = null,
    /**
     * **La copia collegata di un bottone** (dal 27/10/2026, chiesto
     * dall'utente: "effettuare dei collegamenti ai bottoni se li voglio
     * spostare da qualche altra parte"): l'id del blocco del bottone vero, di
     * cui questo mostra il nome ed esegue le azioni. Cambiando le azioni da
     * una copia cambiano quelle del bottone vero, quindi di tutte.
     */
    val buttonLink: String? = null,
    /**
     * **Il gruppo di un blocco sincronizzato** (dal 03/11/2026): le copie con
     * lo stesso id hanno le stesse righe, in qualunque pagina stiano.
     */
    val syncId: String? = null,
    /**
     * **Lo stile di un divisore** (dal 18/11/2026, chiesto dall'utente, come in Word): null = la riga
     * sottile di sempre ("---"), poi [DividerStyle] per nome ("===" doppia, "___" spessa, "***" a
     * puntini, "~~~" a onda).
     */
    val divider: String? = null
) {
    /** L'elenco di cui questo paragrafo tiene il posto, o null. */
    val listIndentType: BlockType?
        get() = listIndent?.let { name -> LIST_INDENT_TYPES.find { it.name == name } }

    /** I file dell'app che questo blocco usa: vanno copiati con lui e cancellati con lui. */
    val files: List<String> get() = listOfNotNull(file, iconImage, image, button?.iconImage)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        /** Le voci di un blocco; un JSON rovinato vale come vuoto, invece di far chiudere l'app. */
        fun parse(attrsJson: String?): BlockAttrs =
            attrsJson?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<BlockAttrs>(it) }.getOrNull() }
                ?: BlockAttrs()

        fun encode(attrs: BlockAttrs): String = json.encodeToString(attrs)
    }
}

/** Le voci in più di un blocco: vedi `BlockAttrs`. */
val BlockEntity.attrs: BlockAttrs get() = BlockAttrs.parse(attrsJson)

/** Gli stili dei divisori, coi tre caratteri che scritti da soli in una riga li fanno (vedi `BlockAttrs.divider`). */
enum class DividerStyle(val shortcut: String) { SINGLE("---"), DOUBLE("==="), THICK("___"), DOTTED("***"), WAVY("~~~") }

/** Lo stile di questo divisore. */
val BlockEntity.dividerStyle: DividerStyle
    get() = attrs.divider?.let { name -> DividerStyle.entries.find { it.name == name } } ?: DividerStyle.SINGLE

/** Gli elenchi di cui un paragrafo può tenere il posto: vedi `BlockAttrs.listIndent`. */
val LIST_INDENT_TYPES = setOf(BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM)

/**
 * L'elenco di cui questo blocco tiene il posto, se è un paragrafo che lo
 * tiene; null per tutti gli altri.
 */
val BlockEntity.heldListType: BlockType?
    get() = if (type == BlockType.PARAGRAPH) attrs.listIndentType else null

/**
 * Le voci di una riga nuova nata andando a capo da questa: il posto
 * dell'elenco, se questa lo tiene (resta allineata anche lei, come su
 * Word), e il suo "Paragraph" (dal 21/10/2026: come su Word, il paragrafo
 * nuovo ha le misure di quello da cui nasce); nient'altro.
 */
fun BlockEntity.attrsForNewLine(): String {
    val paragraph = paragraphAttrs
    val kept = BlockAttrs(
        listIndent = heldListType?.name,
        spacing = paragraph?.spacing,
        markerGap = paragraph?.markerGap
    )
    return if (kept == BlockAttrs()) "{}" else BlockAttrs.encode(kept)
}

/**
 * Le voci del blocco, solo se ha un "Paragraph" suo; null altrimenti. Si
 * legge per ogni riga a ogni tasto: senza quelle due parole nel testo il
 * JSON non si apre nemmeno.
 */
val BlockEntity.paragraphAttrs: BlockAttrs?
    get() = if (attrsJson.contains("\"spacing\"") || attrsJson.contains("\"markerGap\"")) attrs else null

/** L'emoji di riserva di un riquadro, quella con cui nasce: la lampadina, come su Notion. */
const val DEFAULT_CALLOUT_ICON = "💡"

/**
 * I tipi che si scrivono **su più righe dentro un blocco solo**: l'Invio
 * va a capo dentro il riquadro, la citazione o il codice invece di fare
 * una riga nuova sotto. Diventando un tipo che sta su una riga, il loro
 * testo si divide in tante righe quante sono le sue (vedi
 * `PageRepository.convertMultiline`).
 */
val MULTILINE_TYPES = setOf(BlockType.CALLOUT, BlockType.QUOTE, BlockType.CODE)

/** I blocchi che mostrano un file o un indirizzo e non hanno testo da scrivere. */
val MEDIA_TYPES = setOf(BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.BOOKMARK)
