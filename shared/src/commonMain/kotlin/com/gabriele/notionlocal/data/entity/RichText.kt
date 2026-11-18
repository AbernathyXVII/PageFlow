package com.gabriele.notionlocal.data.entity

import kotlinx.serialization.Serializable

/**
 * Un singolo "span" di testo con formattazione. Un blocco (BlockEntity)
 * contiene una lista di questi span serializzata in JSON: questo è ciò
 * che permette "questa parola in grassetto, questa in corsivo" dentro
 * lo stesso paragrafo — esattamente come funziona in Notion.
 *
 * Esempio: "Ciao **mondo**" diventa:
 *   [RichTextSpan("Ciao "), RichTextSpan("mondo", bold=true)]
 */
@Serializable
data class RichTextSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    /**
     * Testo coperto, come su Discord e Telegram: si vede una fascia
     * grigia al posto delle lettere finché non lo si tocca.
     *
     * Ha un valore predefinito come tutti gli altri, quindi il testo
     * già salvato — che questa chiave non ce l'ha — continua a
     * leggersi senza migrazione: manca il campo, vale `false`.
     */
    val spoiler: Boolean = false,
    /**
     * Colore del testo e colore dietro al testo, scritti come
     * esadecimale con il cancelletto (`#RRGGBB`). `null` vuol dire "il
     * colore di sempre": il bianco del tema per il testo, niente per lo
     * sfondo.
     *
     * Una stringa e non un intero perché è la stessa forma in cui i
     * colori si copiano da fuori — è quello che l'utente scrive nel
     * riquadro "Hex" — e perché nel JSON del testo si legge a occhio.
     */
    val color: String? = null,
    val background: String? = null,
    /**
     * **Pedice** (H₂O) e **apice** (x²): più piccolo, sotto o sopra la
     * riga. Si escludono a vicenda — vedi `withSubscript` e
     * `withSuperscript` — come in Word. Aggiunti il 24/09/2026; come
     * `spoiler`, il testo salvato prima non ha la chiave e vale `false`.
     */
    val subscript: Boolean = false,
    val superscript: Boolean = false,
    /**
     * **Font e corpo di questo pezzo di testo** (dal 25/09/2026), scelti
     * dalla barra Aa sul testo selezionato, come in Word e OneNote. Il font
     * è il nome di un `PageFont` ("GARAMOND"); un nome che l'app non
     * conosce più si legge col font di sistema invece di far chiudere
     * l'app. Il corpo è in punti, da 5 a 72. Null = quello di sempre (il
     * font di sistema, 16). Prima c'erano un font e un corpo **per pagina
     * intera**: l'utente voleva poterli cambiare pezzo per pezzo, e la
     * migrazione 30→31 li ha spostati sul testo delle pagine che li usavano.
     */
    val font: String? = null,
    val size: Int? = null,
    /**
     * **Il collegamento di questo pezzo di testo** (dal 17/10/2026, chiesto
     * dall'utente): l'indirizzo che si apre toccandolo, messo dall'icona
     * del collegamento nella barra sopra la tastiera. Null = testo normale.
     * Come gli altri campi, il testo salvato prima non ha la chiave e vale
     * null.
     */
    val link: String? = null
)

/** Accende o spegne il pedice; accenderlo spegne l'apice, perché un pezzo di testo non può stare sotto e sopra insieme. */
fun RichTextSpan.withSubscript(on: Boolean): RichTextSpan = copy(subscript = on, superscript = superscript && !on)

/** Accende o spegne l'apice; accenderlo spegne il pedice. */
fun RichTextSpan.withSuperscript(on: Boolean): RichTextSpan = copy(superscript = on, subscript = subscript && !on)

/** Il testo semplice risultante da una lista di span, concatenati in ordine. */
fun List<RichTextSpan>.plainText(): String = joinToString("") { it.text }

/**
 * Unisce gli span attaccati che hanno **la stessa identica
 * formattazione**: `["Ci"]["a"]["o"]` diventa `["Ciao"]`.
 *
 * Senza, ogni tasto battuto lascia il suo pezzetto separato — la
 * modifica si applica solo al tratto cambiato, e quel tratto resta uno
 * span a sé — e il JSON di un paragrafo cresce di una voce per lettera
 * scritta. Non è sbagliato da leggere, ma è un file che si gonfia per
 * niente e che non si riesce più a guardare a occhio quando qualcosa
 * va storto.
 */
private fun List<RichTextSpan>.merged(): List<RichTextSpan> {
    if (size < 2) return this
    val result = mutableListOf<RichTextSpan>()
    for (span in this) {
        val last = result.lastOrNull()
        // Confronto a testo vuoto: così a contare è solo la
        // formattazione, qualunque campo le si aggiunga in futuro.
        if (last != null && last.copy(text = "") == span.copy(text = "")) {
            result[result.size - 1] = last.copy(text = last.text + span.text)
        } else {
            result.add(span)
        }
    }
    return result
}

/**
 * Restituisce lo span (o meglio, la sua formattazione) a cui appartiene
 * la posizione data — usato per far ereditare al testo appena digitato
 * la formattazione del punto in cui viene inserito (scrivere in mezzo a
 * una parola in grassetto resta grassetto, come in qualsiasi editor).
 */
private fun formatAtPosition(spans: List<RichTextSpan>, position: Int, replacing: Boolean = false): RichTextSpan {
    var pos = 0
    var previous: RichTextSpan? = null
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        if (position < spanEnd) {
            // **Un collegamento non si allunga dai bordi**, come su Notion:
            // scrivendo subito prima il testo nuovo non diventa parte del
            // link. Dentro sì (si corregge il testo del link), e anche
            // quando si sostituisce un pezzo che comincia lì.
            val atLinkStart = position == spanStart && !replacing &&
                span.link != null && previous?.link != span.link
            return if (atLinkStart) span.copy(link = null) else span
        }
        previous = span
        pos = spanEnd
    }
    // **In fondo al testo lo spoiler non si eredita.** Per tutto il
    // resto continuare a scrivere continua anche la formattazione, ed è
    // quello che ci si aspetta da qualsiasi editor. Un testo coperto no:
    // finito di scriverlo il cursore resta subito dopo, e la parola
    // seguente diventerebbe invisibile senza che nessuno l'abbia chiesto.
    // Per allungare uno spoiler si seleziona e si tocca il pulsante.
    // Lo stesso per il collegamento: finito di scriverlo, la parola dopo
    // non ci finisce dentro.
    return spans.lastOrNull()?.copy(spoiler = false, link = null) ?: RichTextSpan(text = "")
}

/**
 * Applica alla lista di span la stessa modifica subita dal testo grezzo,
 * passando da oldText a newText — sostituisce solo la porzione
 * effettivamente cambiata (calcolata confrontando prefisso e suffisso
 * comuni tra vecchio e nuovo testo), lasciando invariato — e con la sua
 * formattazione intatta — tutto il resto. Il testo inserito eredita la
 * formattazione del punto in cui viene inserito.
 */
fun applyTextEdit(spans: List<RichTextSpan>, oldText: String, newText: String): List<RichTextSpan> {
    if (oldText == newText) return spans

    var prefixLen = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) prefixLen++

    var suffixLen = 0
    val maxSuffix = minLen - prefixLen
    while (suffixLen < maxSuffix &&
        oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
    ) suffixLen++

    val deleteStart = prefixLen
    val deleteEnd = oldText.length - suffixLen
    val insertText = newText.substring(prefixLen, newText.length - suffixLen)

    return spliceSpans(spans, deleteStart, deleteEnd, insertText)
}

/**
 * Il testo inserito con la formattazione [format] del punto in cui entra,
 * **tranne il corpo per quello che va a finire in una riga nuova** (dal
 * 27/10/2026, chiesto dall'utente: "se dovessi modificare il font size di un
 * blocco, per esempio un titolo a 50, e poi vado a capo per scrivere altro,
 * fai sì che il font size venga resettato a 16 per tutti i nuovi blocchi").
 * Dopo il primo a-capo il testo sta in righe nate adesso, e il corpo scelto
 * per la riga di sopra non le segue; il resto della formattazione sì, come
 * prima. Succedeva scrivendo subito dopo l'Invio, prima che la riga nuova
 * fosse nel database: Invio e lettere arrivavano insieme, un pezzo solo
 * inserito in fondo al titolo. Un Invio in mezzo al titolo non tocca niente:
 * la metà che scende è testo che c'era già, e tiene il suo corpo.
 */
private fun insertedSpans(format: RichTextSpan, insertText: String): List<RichTextSpan> {
    val firstBreak = insertText.indexOf('\n')
    if (firstBreak < 0 || format.size == null) return listOf(format.copy(text = insertText))
    return listOf(
        format.copy(text = insertText.substring(0, firstBreak + 1)),
        format.copy(text = insertText.substring(firstBreak + 1), size = null)
    )
}

/**
 * Sostituisce l'intervallo [deleteStart, deleteEnd) del testo con
 * insertText, spezzando gli span ai bordi dove necessario. Il testo
 * inserito eredita la formattazione dello span che copre deleteStart.
 */
fun spliceSpans(spans: List<RichTextSpan>, deleteStart: Int, deleteEnd: Int, insertText: String): List<RichTextSpan> {
    val inheritedFormat = if (insertText.isNotEmpty()) formatAtPosition(spans, deleteStart, replacing = deleteEnd > deleteStart) else null

    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    var inserted = false
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        when {
            spanEnd <= deleteStart -> result.add(span)
            spanStart >= deleteEnd -> {
                if (!inserted && inheritedFormat != null) {
                    result.addAll(insertedSpans(inheritedFormat, insertText))
                    inserted = true
                }
                result.add(span)
            }
            else -> {
                if (spanStart < deleteStart) {
                    result.add(span.copy(text = span.text.substring(0, deleteStart - spanStart)))
                }
                if (!inserted && inheritedFormat != null) {
                    result.addAll(insertedSpans(inheritedFormat, insertText))
                    inserted = true
                }
                if (spanEnd > deleteEnd) {
                    result.add(span.copy(text = span.text.substring(deleteEnd - spanStart)))
                }
            }
        }
    }
    if (!inserted && inheritedFormat != null) {
        result.addAll(insertedSpans(inheritedFormat, insertText))
    }
    return result.filter { it.text.isNotEmpty() }.merged()
        .ifEmpty { listOf(RichTextSpan(text = "")) }
}

/**
 * Applica (o toglie) una formattazione a un intervallo di caratteri
 * dentro una lista di span, spezzando gli span esattamente ai bordi
 * della selezione. Se anche un solo carattere nell'intervallo non ha
 * già quella formattazione, l'azione la ATTIVA per tutto l'intervallo;
 * se ce l'hanno già tutti, la TOGLIE — lo standard in ogni editor
 * (Word, Google Docs, Notion).
 */
fun toggleFormatInRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    getFlag: (RichTextSpan) -> Boolean,
    setFlag: (RichTextSpan, Boolean) -> RichTextSpan
): List<RichTextSpan> {
    if (start >= end) return spans

    var pos = 0
    var allHaveFlag = true
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd
        val overlapStart = maxOf(spanStart, start)
        val overlapEnd = minOf(spanEnd, end)
        if (overlapStart < overlapEnd && !getFlag(span)) {
            allHaveFlag = false
        }
    }
    val newValue = !allHaveFlag

    val result = mutableListOf<RichTextSpan>()
    pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }

        if (spanStart < start) {
            result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        }
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        val innerText = span.text.substring(innerStart - spanStart, innerEnd - spanStart)
        result.add(setFlag(span.copy(text = innerText), newValue))
        if (spanEnd > end) {
            result.add(span.copy(text = span.text.substring(end - spanStart)))
        }
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/**
 * Come `toggleFormatInRange`, ma **accende e basta**: non guarda cosa
 * c'era prima. Serve quando la formattazione non è una scelta dell'utente
 * su una selezione ma la conseguenza di quello che ha scritto — `||...||`
 * vuol dire "coprilo", non "invertilo", e se per caso quel pezzo era già
 * coperto la versione a interruttore lo scoprirebbe.
 */
fun setFormatInRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    setFlag: (RichTextSpan, Boolean) -> RichTextSpan
): List<RichTextSpan> {
    if (start >= end) return spans

    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }
        if (spanStart < start) {
            result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        }
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        result.add(
            setFlag(span.copy(text = span.text.substring(innerStart - spanStart, innerEnd - spanStart)), true)
        )
        if (spanEnd > end) {
            result.add(span.copy(text = span.text.substring(end - spanStart)))
        }
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/**
 * Cambia il colore del testo, o quello dietro al testo, su un
 * intervallo di caratteri.
 *
 * Non è un interruttore come grassetto e corsivo: un colore si
 * **imposta**, non si inverte, e `null` lo toglie riportando il pezzo
 * al colore di sempre. Per il resto spezza gli span ai bordi della
 * selezione esattamente come `toggleFormatInRange`.
 */
fun setColorInRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    background: Boolean,
    hex: String?
): List<RichTextSpan> {
    if (start >= end) return spans

    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd

        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }
        if (spanStart < start) {
            result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        }
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        val inner = span.copy(text = span.text.substring(innerStart - spanStart, innerEnd - spanStart))
        result.add(if (background) inner.copy(background = hex) else inner.copy(color = hex))
        if (spanEnd > end) {
            result.add(span.copy(text = span.text.substring(end - spanStart)))
        }
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/**
 * Cambia qualcosa di ogni span nell'intervallo [start, end), spezzando gli
 * span ai bordi della selezione come `setColorInRange`. È il pezzo comune
 * a font e corpo, che come il colore si **impostano** e non si invertono.
 */
private fun transformRange(
    spans: List<RichTextSpan>,
    start: Int,
    end: Int,
    transform: (RichTextSpan) -> RichTextSpan
): List<RichTextSpan> {
    if (start >= end) return spans
    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd
        if (spanEnd <= start || spanStart >= end) {
            result.add(span)
            continue
        }
        if (spanStart < start) result.add(span.copy(text = span.text.substring(0, start - spanStart)))
        val innerStart = maxOf(spanStart, start)
        val innerEnd = minOf(spanEnd, end)
        result.add(transform(span.copy(text = span.text.substring(innerStart - spanStart, innerEnd - spanStart))))
        if (spanEnd > end) result.add(span.copy(text = span.text.substring(end - spanStart)))
    }
    return result.filter { it.text.isNotEmpty() }.merged()
}

/** Il font di un intervallo; null lo riporta a quello di sistema. */
fun setFontInRange(spans: List<RichTextSpan>, start: Int, end: Int, font: String?): List<RichTextSpan> =
    transformRange(spans, start, end) { it.copy(font = font) }

/** Il corpo di un intervallo, in punti; null (o 16, quello di sempre) lo riporta al normale. */
fun setSizeInRange(spans: List<RichTextSpan>, start: Int, end: Int, size: Int?, defaultSize: Int): List<RichTextSpan> =
    transformRange(spans, start, end) { it.copy(size = size?.takeIf { s -> s != defaultSize }) }

/** Il collegamento di un intervallo; null lo toglie. */
fun setLinkInRange(spans: List<RichTextSpan>, start: Int, end: Int, link: String?): List<RichTextSpan> =
    transformRange(spans, start, end) { it.copy(link = link) }

/** Un collegamento su un pezzo di testo: da [start] a [end], all'indirizzo [url]. */
data class LinkRange(val start: Int, val end: Int, val url: String)

/**
 * **I collegamenti arrivati con un incolla** (dal 17/10/2026): ognuno sul
 * suo pezzo di testo. Quelli che cadono fuori dal testo (anche solo in
 * parte) si accorciano o si saltano, invece di sbagliare posto.
 */
fun applyLinks(spans: List<RichTextSpan>, links: List<LinkRange>): List<RichTextSpan> {
    if (links.isEmpty()) return spans
    val length = spans.plainText().length
    return links.fold(spans) { current, link ->
        val start = link.start.coerceAtLeast(0)
        val end = link.end.coerceAtMost(length)
        if (start < end && link.url.isNotBlank()) setLinkInRange(current, start, end, link.url) else current
    }
}

/** Un pezzo di testo incollato con il suo stile: grassetto, corsivo, sottolineato, barrato. */
data class StyleRange(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    /**
     * **La grandezza della fonte** (dal 20/10/2026, "Keep Source
     * Formatting"), già in punti di notE (vedi `RichTextSpan.size`); null =
     * quella normale.
     */
    val size: Int? = null,
    /** **I colori della fonte** (dal 17/11/2026), `#RRGGBB` come [RichTextSpan.color]; null = quelli di sempre. */
    val color: String? = null,
    val background: String? = null
)

/**
 * **Come va messo nel testo quello che si incolla** (dal 18/10/2026): i
 * pezzi [plain] perdono la formattazione che avrebbero preso da dove si
 * incolla (come "Keep Text Only" e "Keep Source Formatting" di Word), poi
 * prendono gli stili [styles] e i collegamenti [links] di chi li ha copiati.
 * Tutte le posizioni sono nel testo intero del campo.
 */
data class PasteFormat(
    val links: List<LinkRange> = emptyList(),
    val styles: List<StyleRange> = emptyList(),
    val plain: List<IntRange> = emptyList(),
    /** Le righe copiate intere da notE, col loro tipo: vedi [PastedLine]. */
    val lines: List<PastedLine> = emptyList()
) {
    val isEmpty: Boolean get() = links.isEmpty() && styles.isEmpty() && plain.isEmpty() && lines.isEmpty()

    /** Le stesse posizioni spostate di [delta] (in un pezzo del testo che comincia più avanti). */
    fun shifted(delta: Int): PasteFormat = PasteFormat(
        links = links.map { it.copy(start = it.start + delta, end = it.end + delta) },
        styles = styles.map { it.copy(start = it.start + delta, end = it.end + delta) },
        plain = plain.map { (it.first + delta)..(it.last + delta) },
        lines = lines.map { it.copy(start = it.start + delta, end = it.end + delta) }
    )

    /** La riga incollata che va **esattamente** da [start] a [end], se c'è. */
    fun lineAt(start: Int, end: Int): PastedLine? = lines.firstOrNull { it.start == start && it.end == end }
}

/**
 * **Una riga incollata col suo tipo** (dal 19/10/2026, chiesto dall'utente:
 * copiando delle caselle da spuntare si incollava testo normale). Copiata
 * intera da notE, una casella (spuntata o no), un elemento di un elenco
 * puntato o numerato tornano tali, col loro rientro. Posizioni nel testo
 * incollato, senza l'a-capo.
 */
data class PastedLine(
    val start: Int,
    val end: Int,
    val type: BlockType,
    val checked: Boolean = false,
    val indent: Int = 0,
    /**
     * **Una tabella copiata** (dal 20/10/2026, da OneNote o da una pagina
     * web): le celle, riga per riga. La riga incollata diventa una tabella
     * semplice di notE con queste celle; il suo testo è solo un segnaposto.
     */
    val table: List<List<String>>? = null,
    /**
     * **Un'immagine copiata** (dal 18/11/2026, da OneNote): da dove la si
     * prende (vedi `pastableImage`) e, dopo `savePastedImage`, il nome del
     * file nell'app ("" se non si è potuta leggere). Anche qui il testo della
     * riga è un segnaposto.
     */
    val image: String? = null
)

/**
 * Una riga copiata intera, col suo tipo (vedi [PastedLine]): negli appunti ce
 * n'è una per ogni riga del testo copiato, null per una presa solo in parte.
 */
data class CopiedLine(val type: BlockType, val checked: Boolean = false, val indent: Int = 0)

/** Mette nel testo quello che dice [format]: prima i pezzi senza formattazione, poi gli stili, poi i link. */
fun applyPaste(spans: List<RichTextSpan>, format: PasteFormat): List<RichTextSpan> {
    if (format.isEmpty) return spans
    val length = spans.plainText().length
    fun inside(start: Int, end: Int): IntRange? {
        val from = start.coerceAtLeast(0)
        val to = end.coerceAtMost(length)
        return if (from < to) from until to else null
    }
    var result = format.plain.fold(spans) { current, range ->
        inside(range.first, range.last + 1)?.let { r -> transformRange(current, r.first, r.last + 1) { RichTextSpan(text = it.text) } } ?: current
    }
    result = format.styles.fold(result) { current, style ->
        inside(style.start, style.end)?.let { r ->
            transformRange(current, r.first, r.last + 1) {
                it.copy(
                    bold = it.bold || style.bold,
                    italic = it.italic || style.italic,
                    underline = it.underline || style.underline,
                    strikethrough = it.strikethrough || style.strikethrough,
                    size = style.size ?: it.size,
                    color = style.color ?: it.color,
                    background = style.background ?: it.background
                )
            }
        } ?: current
    }
    return applyLinks(result, format.links)
}

/** Il pezzo [start, end) del testo, con la sua formattazione: per copiare. */
fun sliceSpans(spans: List<RichTextSpan>, start: Int, end: Int): List<RichTextSpan> {
    val result = mutableListOf<RichTextSpan>()
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        val spanEnd = pos + span.text.length
        pos = spanEnd
        val from = maxOf(spanStart, start)
        val to = minOf(spanEnd, end)
        if (from < to) result.add(span.copy(text = span.text.substring(from - spanStart, to - spanStart)))
    }
    return result
}

/** Il primo collegamento nell'intervallo [start, end), o null se non ce n'è. */
fun linkInRange(spans: List<RichTextSpan>, start: Int, end: Int): String? {
    var pos = 0
    for (span in spans) {
        val spanStart = pos
        pos += span.text.length
        if (span.link != null && spanStart < end && pos > start) return span.link
    }
    return null
}

/**
 * **Il collegamento in cui sta il cursore**, da dove a dove: quello della
 * lettera subito prima del cursore o, se quella non ne ha, della lettera
 * subito dopo. Tutti i pezzi attaccati con lo stesso indirizzo contano come
 * uno (un link con una parola in grassetto è sempre un link solo). Null se
 * il cursore non tocca nessun collegamento.
 */
fun linkRunAt(spans: List<RichTextSpan>, caret: Int): IntRange? {
    val anchor = if (spanAt(spans, caret - 1)?.link != null) caret - 1 else caret
    val link = spanAt(spans, anchor)?.link ?: return null
    val starts = IntArray(spans.size)
    var pos = 0
    spans.forEachIndexed { i, span ->
        starts[i] = pos
        pos += span.text.length
    }
    val index = spans.indices.first { anchor >= starts[it] && anchor < starts[it] + spans[it].text.length }
    var first = index
    while (first > 0 && spans[first - 1].link == link) first--
    var last = index
    while (last < spans.lastIndex && spans[last + 1].link == link) last++
    return starts[first] until starts[last] + spans[last].text.length
}

private val BARE_LINK_START = Regex("^(https?://|www\\.)", RegexOption.IGNORE_CASE)

/**
 * **Un sito scritto come testo semplice** (dal 27/10/2026), senza un
 * collegamento: la parola fra due spazi che contiene il tratto
 * [[start], [end]) e comincia con "http://", "https://" o "www.". La
 * punteggiatura attaccata ai bordi non conta: le virgolette e la parentesi
 * prima, il punto, la virgola e la parentesi chiusa dopo — tranne quella
 * che chiude una parentesi dell'indirizzo stesso, come in
 * ".../wiki/Bulbasaur_(Pokémon)". Null se lì non c'è un sito.
 */
fun bareLinkAt(text: String, start: Int, end: Int): IntRange? {
    if (start < 0 || end > text.length || start > end) return null
    if ((start until end).any { text[it].isWhitespace() }) return null
    var from = start
    while (from > 0 && !text[from - 1].isWhitespace()) from--
    var to = end
    while (to < text.length && !text[to].isWhitespace()) to++
    while (from < to && text[from] in "([{<\"'«") from++
    while (to > from) {
        val last = text[to - 1]
        val closesItsOwn = last == ')' && text.substring(from, to).count { it == '(' } >= text.substring(from, to).count { it == ')' }
        if (last in ".,;:!?]}>\"'»" || (last == ')' && !closesItsOwn)) to-- else break
    }
    val word = text.substring(from.coerceAtMost(to), to)
    val prefix = BARE_LINK_START.find(word) ?: return null
    // Dopo "https://" o "www." ci vuole almeno un nome con un punto.
    if (!word.substring(prefix.value.length).contains('.')) return null
    return from until to
}

/** Lo span che contiene il carattere in posizione `index`, o null fuori dal testo. */
fun spanAt(spans: List<RichTextSpan>, index: Int): RichTextSpan? {
    if (index < 0) return null
    var pos = 0
    for (span in spans) {
        val spanEnd = pos + span.text.length
        if (index < spanEnd) return span
        pos = spanEnd
    }
    return null
}

/**
 * Spezza una lista di span sui ritorni a capo: una lista per riga.
 *
 * Il testo scorrevole è un campo solo ma tanti blocchi, uno per riga, e
 * ogni blocco tiene i propri span. Per far sopravvivere la formattazione
 * a una modifica che attraversa più righe conviene ragionare una volta
 * sola su tutto il pezzo e poi rimetterlo in righe: è quello che fa
 * questa, insieme a `joinLines`.
 */
fun List<RichTextSpan>.splitLines(): List<List<RichTextSpan>> {
    val lines = mutableListOf<List<RichTextSpan>>()
    var current = mutableListOf<RichTextSpan>()
    for (span in this) {
        val pieces = span.text.split('\n')
        for ((i, piece) in pieces.withIndex()) {
            if (i > 0) {
                lines.add(current)
                current = mutableListOf()
            }
            if (piece.isNotEmpty()) current.add(span.copy(text = piece))
        }
    }
    lines.add(current)
    return lines.map { it.ifEmpty { listOf(RichTextSpan(text = "")) } }
}

/**
 * Il contrario di `splitLines`: rimette insieme le righe con un a-capo
 * fra una e l'altra. L'a-capo è uno span **senza formattazione**, così è
 * anche il confine oltre il quale la formattazione non si propaga.
 */
fun List<List<RichTextSpan>>.joinLines(): List<RichTextSpan> {
    val result = mutableListOf<RichTextSpan>()
    for ((i, line) in this.withIndex()) {
        if (i > 0) result.add(RichTextSpan(text = "\n"))
        result.addAll(line.filter { it.text.isNotEmpty() })
    }
    return result.ifEmpty { listOf(RichTextSpan(text = "")) }
}
