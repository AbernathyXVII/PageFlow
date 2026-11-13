package com.gabriele.notionlocal.viewmodel

import com.gabriele.notionlocal.data.symbolField
import com.gabriele.notionlocal.data.replacedImage
import com.gabriele.notionlocal.data.imageField
import com.gabriele.notionlocal.data.iconField
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.LinkTarget
import com.gabriele.notionlocal.data.entity.linkTargetsOf
import com.gabriele.notionlocal.data.entity.PageLine
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.entity.moveTargetsFor
import com.gabriele.notionlocal.data.entity.LIST_INDENT_TYPES
import com.gabriele.notionlocal.data.entity.MEDIA_TYPES
import com.gabriele.notionlocal.data.entity.MULTILINE_TYPES
import com.gabriele.notionlocal.data.entity.attrsForNewLine
import com.gabriele.notionlocal.data.entity.heldListType
import com.gabriele.notionlocal.ui.screen.bulletMarkerSizeFor
import com.gabriele.notionlocal.ui.screen.runGroupKey
import com.gabriele.notionlocal.data.entity.MARKER_LIST_TYPES
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.entity.ParagraphLook
import com.gabriele.notionlocal.data.entity.paragraph
import com.gabriele.notionlocal.data.entity.paragraphAttrs
import com.gabriele.notionlocal.data.entity.spacingStartsOf
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageVersionInfo
import com.gabriele.notionlocal.data.entity.VersionReason
import com.gabriele.notionlocal.data.history.VersionContent
import com.gabriele.notionlocal.data.share.QuickNotes
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageFont
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.LinkRange
import com.gabriele.notionlocal.data.entity.PasteFormat
import com.gabriele.notionlocal.data.entity.PastedLine
import com.gabriele.notionlocal.data.entity.CopiedLine
import com.gabriele.notionlocal.data.entity.applyPaste
import com.gabriele.notionlocal.data.entity.sliceSpans
import com.gabriele.notionlocal.data.entity.applyTextEdit
import com.gabriele.notionlocal.data.entity.bareLinkAt
import com.gabriele.notionlocal.data.entity.joinLines
import com.gabriele.notionlocal.data.entity.linkInRange
import com.gabriele.notionlocal.data.entity.linkRunAt
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.setColorInRange
import com.gabriele.notionlocal.data.entity.setFontInRange
import com.gabriele.notionlocal.data.entity.setFormatInRange
import com.gabriele.notionlocal.data.entity.setLinkInRange
import com.gabriele.notionlocal.data.entity.setSizeInRange
import com.gabriele.notionlocal.data.entity.spanAt
import com.gabriele.notionlocal.data.entity.spliceSpans
import com.gabriele.notionlocal.data.entity.splitLines
import com.gabriele.notionlocal.data.entity.toggleFormatInRange
import com.gabriele.notionlocal.data.entity.withSubscript
import com.gabriele.notionlocal.data.entity.withSuperscript
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.data.notify.ReminderAlarms
import com.gabriele.notionlocal.data.notify.NotificationCommand
import com.gabriele.notionlocal.data.notify.NotificationCommands
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.theme.DEFAULT_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MAX_PAGE_FONT_SIZE
import com.gabriele.notionlocal.ui.theme.MIN_PAGE_FONT_SIZE
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.gabriele.notionlocal.data.entity.ignoredWords
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import com.gabriele.notionlocal.data.export.ExportFormat
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.data.entity.movedDateLink
import com.gabriele.notionlocal.data.entity.replaceLinkedText
import java.time.LocalDate
import com.gabriele.notionlocal.data.sync.SyncedWatcher
import com.gabriele.notionlocal.data.ocr.PhotoTextRequests
import com.gabriele.notionlocal.ui.i18n.CaptureStrings

/** I tipi di formattazione inline selezionabili dalla barra "Aa". */
enum class FormatType { BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, SPOILER, SUBSCRIPT, SUPERSCRIPT }

/**
 * Gestisce il contenuto di una singola pagina di testo: titolo, icona,
 * l'albero di blocchi (paragrafi, titoli, liste, checkbox, toggle,
 * tabelle, link a pagina/database...), il focus tra i blocchi, e la
 * cronologia annulla/ripristina.
 */
/**
 * Cosa ha fatto `splitBlockAt`. Serve a chi chiama, non per curiosità:
 * `BECAME_PARAGRAPH` vuol dire che il blocco ha **cambiato specie**, da
 * riga a sé a testo scorrevole, e che quindi il campo di testo da cui
 * arriva la chiamata sta per essere distrutto. Chi lo sa deve staccare
 * la tastiera prima che succeda — vedi il commento in `BlockRow`.
 *
 * `SPLIT_ABOVE` vuol dire l'opposto: la riga nuova è stata messa
 * **sopra** e il campo che chiama resta vivo, col fuoco, a tenere il
 * testo dopo il cursore. Deve quindi mostrare quello, non la prima
 * riga, col cursore all'inizio.
 */
enum class SplitOutcome { SPLIT, SPLIT_ABOVE, BECAME_PARAGRAPH, NOTHING }

/**
 * Cosa ha fatto il backspace a inizio riga. `MERGED_INTO_ISLAND` vuol
 * dire che questa riga è finita dentro il blocco di sopra e quindi non
 * esiste più: il campo che ha chiamato sta per essere distrutto.
 * `REMOVED_CLOSE_KEYBOARD` (dal 05/10/2026): la riga se n'è andata e non
 * c'è nessun'altra riga di testo vicina su cui spostare il cursore — sopra
 * c'è un divisore, che non si può scrivere —: il campo deve lasciare il
 * fuoco, e la tastiera si chiude.
 */
enum class BackspaceOutcome { NOTHING, STEP_UNDONE, MERGED_INTO_ISLAND, REMOVED_CLOSE_KEYBOARD }

class PageEditorViewModel(private val repository: PageRepository) : ViewModel() {

    companion object {
        /** Ogni quanto, con la pagina aperta, si salva una versione se è cambiata. */
        const val VERSION_EVERY_MS = 10L * 60 * 1000

        /** Uscendo, quanto aspettare le ultime scritture prima di salvare la versione. */
        const val VERSION_AFTER_LEAVING_MS = 1_500L

        // Piccolo ritardo prima di spostare il focus su un blocco diverso
        // da quello su cui l'utente sta scrivendo. Mitigazione
        // sperimentale per un bug documentato della tastiera Samsung con
        // Jetpack Compose: se il campo di testo attivo viene sostituito
        // "da sotto" troppo a ridosso di un carattere appena digitato
        // (mentre la tastiera sta ancora finalizzando quel carattere),
        // quel carattere può finire duplicato nel campo vecchio invece
        // che nel nuovo. Non è garantito risolva del tutto — la causa
        // vive nell'implementazione della tastiera stessa, non nel
        // nostro codice — ma dà alla tastiera un momento in più per
        // "assestarsi" prima che il focus si sposti.
        private const val IME_SETTLE_DELAY_MS = 60L

        // Quanto aspettare che una riga prenda il fuoco prima di rinunciare
        // a togliere quella che lo sta cedendo. Di solito basta un
        // fotogramma; mezzo secondo è largo apposta.
        private const val FOCUS_HANDOFF_TIMEOUT_MS = 500L

        /** Quanto resta "preso" un divisore dopo il primo backspace sotto di lui. */
        const val ARMED_DIVIDER_MS = 3_000L

        // Tetto ai passaggi di annulla ricordati, per non far crescere
        // la memoria senza limite su una sessione di modifica lunga.
        private const val MAX_UNDO_STEPS = 50

        // Livello massimo di rientro per i blocchi di testo scorrevole.
        //
        // Dieci e non cinque perché i segni degli elenchi puntati fanno
        // un giro di otto (vedi `BULLET_MARKERS`): con cinque livelli il
        // giro non si sarebbe mai potuto vedere finire, né tantomeno
        // ricominciare.
        private const val MAX_INDENT_LEVEL = 10

        // Quanto si aspetta, chiusa una sessione di scrittura, prima di
        // leggere il testo "dopo" da mettere nella cronologia: il tempo
        // che l'ultima battuta faccia il giro fino al database e torni
        // indietro. Vedi `PageRepository.recordEditAfterSettling`.
        private const val EDIT_SETTLE_MS = 400L

        // I tipi di riga che una copia porta con sé: vedi `selectedLines`.
        private val COPIED_LINE_TYPES = setOf(BlockType.CHECKBOX, BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM)

        // Le righe che, incollandoci sopra una riga copiata, ne prendono il tipo.
        private val RETYPABLE_ON_PASTE = COPIED_LINE_TYPES + BlockType.PARAGRAPH

        // Il testo di una riga vuota, come lo scrive il resto dell'app:
        // uno span senza lettere. Serve a svuotare una riga il cui testo è
        // andato altrove (nel nome di una pagina, nella prima cella di una
        // tabella).
        private const val EMPTY_TEXT_JSON = """[{"text":""}]"""

        // **Quanto è grande un pezzo di testo** (vedi `chunkMembers`): oltre
        // queste righe o queste lettere ne comincia un altro. Un pezzo più
        // grande si rifà più lentamente a ogni tasto; uno più piccolo
        // lascia selezionare meno righe insieme.
        const val CHUNK_MAX_LINES = 50

        // Oltre queste righe il campo col fuoco non prende altri pezzi, anche
        // se si vedono: scriverci costerebbe troppo (vedi `keepVisibleTogether`).
        const val FOCUSED_CHUNK_CAP = 200

        // Dopo quanto dall'ultimo tasto si può toccare il campo in cui si scrive.
        const val TYPING_QUIET_MS = 2_000L
        const val CHUNK_MAX_CHARS = 3_500

        // Quanto si aspetta, smesso di scrivere, prima di ridividere in
        // pezzi: il tempo che l'ultima battuta arrivi al database.
        private const val RECHUNK_DELAY_MS = 1_200L
    }

    /**
     * Le divisioni di blocco si mettono in fila.
     *
     * Ognuna rilegge dal database l'indice del blocco e sposta di uno
     * tutto quello che sta sotto: due che girano insieme leggono lo
     * stesso indice e finiscono per occupare lo stesso posto. È successo
     * davvero, andando a capo in fretta.
     */
    private val structuralEdits = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

    private var currentPageId: String? = null

    private val _page = MutableStateFlow<PageEntity?>(null)
    val page: StateFlow<PageEntity?> = _page

    private var blocksFlowJob: Job? = null
    private var pageFlowJob: Job? = null
    private var editsFlowJob: Job? = null

    private val _blocks = MutableStateFlow<List<BlockEntity>>(emptyList())
    val blocks: StateFlow<List<BlockEntity>> = _blocks

    // --- Le righe nuove si vedono subito, non al ritorno dal database ---
    //
    // **Andando a capo dentro un toggle la riga appena scritta spariva
    // per un fotogramma.** Il campo col cursore passa all'istante al testo
    // dopo il cursore, ma la riga nuova sopra, col testo di prima, arrivava
    // solo quando il database aveva finito di scriverla e la rimandava
    // indietro: 29 ms misurati, tre o quattro fotogrammi. Registrando lo
    // schermo si vede la lettera appena battuta scomparire e ricomparire
    // una riga più su, e il cursore scendere di colpo. Il testo condiviso
    // non ha il problema perché lì le righe sono tutte dentro un campo solo.
    //
    // Qui la riga nuova entra nella lista **nello stesso istante** in cui
    // il campo cambia testo, e il database la conferma dopo.
    //
    // Non basta però aggiungerla una volta: una lettura del database
    // partita un attimo prima dell'Invio può arrivare dopo, senza la riga,
    // e toglierla di nuovo — e se quella riga ha già il cursore, togliere
    // la sua casella di scrittura fa chiudere l'app. Quindi la modifica
    // resta in sospeso e si **riapplica a ogni lettura** che non contiene
    // ancora la riga nuova; la prima che la contiene la chiude.

    private class PendingInsert(
        val newBlockId: String,
        val applyTo: (List<BlockEntity>) -> List<BlockEntity>
    )

    private val pendingInserts = mutableListOf<PendingInsert>()
    private var lastLoadedBlocks: List<BlockEntity> = emptyList()

    /**
     * **La riga vuota di una pagina senza righe.** Si mostra per non
     * lasciare una pagina bianca senza cursore (vedi `publishBlocks`), ma
     * nel database non c'è finché non la si scrive.
     *
     * Ha **sempre lo stesso id** finché si resta sulla pagina. Prima ne
     * nasceva una nuova, con un id nuovo, a ogni lettura del database, e il
     * campo di testo — che si riconosce dall'id della sua prima riga — con
     * lei. E va salvata **insieme** alla prima modifica, anche quando la
     * modifica non la tocca (vedi `placeholderToSave` in `updateRun`):
     * con l'Invio su una pagina vuota si salvava la sola riga nuova, la riga
     * vuota spariva, il campo rinasceva e il cursore restava a nessuno —
     * tastiera aperta, barra sparita, e quello che si scriveva non andava da
     * nessuna parte. Trovato sul telefono il 25/09/2026.
     */
    private var placeholder: BlockEntity? = null

    /** I blocchi sincronizzati della pagina, cambiati qui, alle altre copie (dal 03/11/2026). */
    private val syncedWatcher = SyncedWatcher { id -> repository.pushSynced(id) }

    private fun placeholderFor(pageId: String): BlockEntity =
        placeholder?.takeIf { it.pageId == pageId }
            ?: BlockEntity(pageId = pageId, orderIndex = 0).also { placeholder = it }

    /**
     * **Le modifiche di una colonna sola passano da qui** (dal 26/09/2026).
     *
     * Cambiare il tipo, il testo, il rientro, il numero di partenza, l'ora
     * della notifica… scrive nel database *solo quella colonna* di un blocco
     * che c'è già. La riga vuota di una pagina nuova (`placeholder`) invece
     * non c'è ancora: la modifica non trovava niente e andava persa. Così
     * "To-do list" dal "+" sulla prima riga di un template appena creato non
     * faceva la casella, e quello che si scriveva dopo restava testo normale
     * (trovato dall'utente). Qui prima si salva quella riga, **solo se
     * manca**: se nel frattempo è stata scritta, resta com'è.
     */
    private val blocksDb = object {
        private suspend fun saved(blockId: String) {
            placeholder?.takeIf { it.id == blockId }?.let { repository.saveBlockIfAbsent(it) }
        }

        suspend fun setBlockText(blockId: String, textJson: String) {
            saved(blockId); repository.setBlockText(blockId, textJson)
        }

        suspend fun setBlockType(blockId: String, type: BlockType) {
            saved(blockId); repository.setBlockType(blockId, type)
        }

        suspend fun setBlockTypes(blockIds: List<String>, type: BlockType) {
            blockIds.forEach { saved(it) }; repository.setBlockTypes(blockIds, type)
        }

        suspend fun makeListText(blockId: String, listType: BlockType) {
            saved(blockId); repository.makeListText(blockId, listType)
        }

        suspend fun releaseListIndent(blockId: String) {
            saved(blockId); repository.releaseListIndent(blockId)
        }

        suspend fun setBlockExpanded(blockId: String, expanded: Boolean) {
            saved(blockId); repository.setBlockExpanded(blockId, expanded)
        }

        suspend fun setNumberStart(blockId: String, startsAt: Int?) {
            saved(blockId); repository.setNumberStart(blockId, startsAt)
        }

        suspend fun setNotifyAt(blockId: String, at: Long) {
            saved(blockId); repository.setNotifyAt(blockId, at)
        }

        suspend fun setIndentLevel(blockId: String, level: Int) {
            saved(blockId); repository.setIndentLevel(blockId, level)
        }

        suspend fun setBlockTypeAndLink(blockId: String, type: BlockType, linkedPageId: String) {
            saved(blockId); repository.setBlockTypeAndLink(blockId, type, linkedPageId)
        }

        suspend fun unnestAndConvert(blockId: String, newType: BlockType) {
            saved(blockId); repository.unnestAndConvert(blockId, newType)
        }

        suspend fun setTypeAndAttrs(blockId: String, type: BlockType, attrs: BlockAttrs) {
            saved(blockId); repository.setBlockTypeAndAttrs(blockId, type, attrs)
        }

        suspend fun makeFullPageDatabase(blockId: String, title: String?): String {
            saved(blockId); return repository.makeFullPageDatabase(blockId, title)
        }

        suspend fun setTableCellText(blockId: String, row: Int, col: Int, text: String) {
            saved(blockId); repository.setTableCellText(blockId, row, col, text)
        }

        suspend fun makeSynced(blockId: String): String? {
            saved(blockId); return repository.makeSynced(blockId)
        }

        suspend fun makeColumns(blockId: String, count: Int): String? {
            saved(blockId); return repository.makeColumns(blockId, count)
        }
    }

    /** Mostra subito una riga nuova che il database deve ancora scrivere. */
    private fun showBeforeSaved(newBlockId: String, applyTo: (List<BlockEntity>) -> List<BlockEntity>) {
        pendingInserts += PendingInsert(newBlockId, applyTo)
        _blocks.value = applyTo(_blocks.value)
    }

    /** Il database non l'ha scritta: la riga mostrata in anticipo se ne va. */
    private fun forgetPendingInsert(newBlockId: String) {
        if (pendingInserts.removeAll { it.newBlockId == newBlockId }) publishBlocks(lastLoadedBlocks)
    }

    private fun publishBlocks(loaded: List<BlockEntity>) {
        pendingInserts.removeAll { pending -> loaded.any { it.id == pending.newBlockId } }
        var shown = loaded
        for (pending in pendingInserts) shown = pending.applyTo(shown)
        // Se la pagina non ha ancora nessun blocco (appena creata),
        // partiamo con un paragrafo vuoto pronto per scrivere,
        // così l'utente non vede una schermata completamente bianca
        // senza cursore.
        val published = shown.ifEmpty {
            listOf(placeholderFor(currentPageId ?: return))
        }
        // I pezzi della pagina appena aperta si fanno prima di mostrarla,
        // così il primo disegno non è già un campo enorme; dopo, solo
        // quando non si scrive (vedi `chunkMembers`).
        if (chunkedPageId != currentPageId) {
            chunkedPageId = currentPageId
            rechunk(published)
        } else if (!editing) {
            scheduleRechunk()
        }
        _blocks.value = published
    }

    // --- I pezzi delle pagine lunghe (dal 18/10/2026) ---

    /**
     * **Le righe di testo di fila si dividono in pezzi**, ognuno un campo di
     * testo a sé (dal 18/10/2026). Erano un campo solo, e in una pagina di
     * 689 righe ogni tasto lo rifaceva tutto: la tastiera restava indietro,
     * cancellare era lentissimo, scorrere a tastiera aperta scattava
     * (chiesto dall'utente: "l'app deve reggere tante scritte"). Scrivendo
     * si rifà solo il pezzo in cui si scrive.
     *
     * **I confini non si spostano mai sotto il campo in cui si scrive**:
     * campo e database devono parlare delle stesse righe (vedi
     * `resolveRun`). Per questo ogni pezzo ricorda le sue righe, nell'ordine,
     * e comincia dalla prima che c'è ancora: cancellata o unita a quella
     * sopra la sua prima riga, il pezzo continua dalla seconda. Una riga
     * nuova messa in cima al pezzo (Invio a inizio della prima riga) diventa
     * lei la prima (`writeRunChange`). Le righe scritte dentro allungano il
     * pezzo; lo si ridivide solo quando non si scrive (`rechunk`).
     *
     * Il primo pezzo di un gruppo di righe non è qui: comincia dove
     * comincia il gruppo, e la sua chiave resta quella di sempre.
     */
    private var chunkMembers: Map<String, List<String>> = emptyMap()
    private val _chunkVersion = MutableStateFlow(0)

    /** Cambia quando cambiano i pezzi senza che cambino le righe. */
    val chunkVersion: StateFlow<Int> = _chunkVersion
    private var chunkedPageId: String? = null
    private var editing = false
    private var rechunkJob: Job? = null

    /**
     * Dove comincia ogni pezzo fra queste righe, che sono quelle in cima
     * alla pagina e nel loro ordine: id della prima riga → chiave del pezzo.
     * La stessa risposta per la schermata e per `resolveRun`, data la stessa
     * lista: anche quando la schermata è indietro di un tasto.
     */
    fun chunkStartsOf(blocks: List<BlockEntity>): Map<String, String> {
        val members = chunkMembers
        if (members.isEmpty()) return emptyMap()
        val present = HashSet<String>(blocks.size * 2)
        for (b in blocks) present += b.id
        val starts = HashMap<String, String>()
        for ((key, ids) in members) {
            val first = ids.firstOrNull { it in present } ?: continue
            if (first !in starts) starts[first] = key
        }
        return starts
    }

    /** La schermata dice se si sta scrivendo: i pezzi si ridividono solo quando no. */
    fun setEditing(active: Boolean) {
        editing = active
        if (active) rechunkJob?.cancel() else scheduleRechunk()
    }

    private fun scheduleRechunk() {
        rechunkJob?.cancel()
        rechunkJob = viewModelScope.launch {
            delay(RECHUNK_DELAY_MS)
            if (!editing) rechunk(_blocks.value)
        }
    }

    /**
     * Ridivide in pezzi: quelli che ci sono restano dove sono, e un pezzo
     * troppo grande (`CHUNK_MAX_LINES`, `CHUNK_MAX_CHARS`) se ne fa un altro
     * dopo. Le righe ricordate da ogni pezzo diventano quelle di adesso.
     */
    private fun rechunk(all: List<BlockEntity>) {
        val top = all.filter { it.parentBlockId == null }
        val starts = chunkStartsOf(top)
        val next = LinkedHashMap<String, List<String>>()
        var key: String? = null
        var members = ArrayList<String>()
        var lines = 0
        var chars = 0
        var inRun = false
        var previousId: String? = null
        var previousLook: LineLook? = null
        // Come appare l'ultima riga di testo sopra l'isola che ha chiuso il gruppo: vedi sotto.
        var lookAboveIsland: LineLook? = null
        // Il segno della prima riga del pezzo: vedi `markerSizeOf`.
        var pieceMarker = 0f
        fun close() {
            key?.let { next[it] = members }
            key = null
            members = ArrayList()
        }
        for (block in top) {
            if (!isFlowingTextType(block.type)) {
                close()
                inRun = false
                previousId = null
                if (previousLook != null) lookAboveIsland = previousLook
                previousLook = null
                continue
            }
            val length = plainTextOf(block).length + 1
            // Una riga che appare diversa da quella sopra comincia un pezzo,
            // anche sullo schermo (vedi `lineLookOf`).
            val look = lineLookOf(block)
            val lookChanged = previousLook != null && look != previousLook
            previousLook = look
            // Fra due righe sullo schermo un confine non si mette (vedi `keepVisibleTogether`).
            val onScreen = previousId in visibleLineIds && block.id in visibleLineIds
            previousId = block.id
            if (!inRun) {
                // Il primo pezzo del gruppo: comincia col gruppo.
                inRun = true
                lines = 0
                chars = 0
                pieceMarker = markerSizeOf(block)
                // **Un gruppo che comincia diverso dalla riga sopra l'isola ha già un
                // pezzo suo** (dal 10/11/2026, trovato dall'utente sul PC): tolta l'isola
                // (un divisore cancellato) i due gruppi diventano uno, e senza un confine
                // le righe normali finivano nel campo di un titolo grande, con la sua
                // altezza di riga, finché si scriveva (vedi [lineLookOf]).
                if (lookAboveIsland != null && look != lookAboveIsland) {
                    key = starts[block.id]?.takeUnless { it.startsWith("run:") } ?: ("chunk:" + block.id)
                }
            } else if (lookChanged || !onScreen) {
                val existing = starts[block.id]
                val due = existing != null || lines >= CHUNK_MAX_LINES || chars + length > CHUNK_MAX_CHARS
                // Un pezzo comincia con lo stesso segno della prima riga di
                // quello sopra (vedi `markerSizeOf`); se non arriva, al doppio
                // della misura lo si taglia lo stesso.
                val sameMarker = markerSizeOf(block) == pieceMarker
                val tooBig = lines >= 2 * CHUNK_MAX_LINES || chars + length > 2 * CHUNK_MAX_CHARS
                if (lookChanged || (due && sameMarker) || tooBig) {
                    close()
                    // La chiave del gruppo scritta per esteso serviva solo al
                    // campo col cursore (vedi `keepVisibleTogether`): senza
                    // cursore il pezzo ne prende una sua, e il primo torna
                    // quello del gruppo.
                    key = existing?.takeUnless { it.startsWith("run:") } ?: ("chunk:" + block.id)
                    lines = 0
                    chars = 0
                    pieceMarker = markerSizeOf(block)
                }
            }
            if (key != null) members += block.id
            lines++
            chars += length
        }
        close()
        if (next != chunkMembers) {
            chunkMembers = next
            _chunkVersion.value++
        }
    }

    /** Come appare una riga nel suo campo: vedi [lineLookOf]. */
    private data class LineLook(val checkbox: Boolean, val largestSize: Int)

    /**
     * **Come appare una riga** (dal 27/10/2026): se è una casella da spuntare,
     * e il corpo più grande scelto per lei con la barra Aa (mai meno di quello
     * normale). Il campo di testo ha un'altezza di riga sola per tutte le sue
     * righe, e la prende da qui (`CHECKBOX_LINE_HEIGHT` e `lineScaleFor` nella
     * schermata): due righe che appaiono diverse non stanno quindi nello stesso
     * pezzo ([rechunk]), e non ci finiscono unendo i pezzi sullo schermo
     * ([keepVisibleTogether]).
     *
     * Trovato dall'utente in "Fast notes": i titoli col corpo a 20 ("Anki field
     * codes", "Creepy things", "Emergency") allungavano tutte le righe del loro
     * pezzo da 50, quelle sotto avevano più spazio di quelle in cima, e
     * scorrendo lo spazio cambiava da solo, perché i pezzi sullo schermo si
     * univano e si ridividevano portando le righe dentro e fuori dal campo
     * allungato.
     */
    private fun lineLookOf(block: BlockEntity): LineLook = LineLook(
        checkbox = block.type == BlockType.CHECKBOX,
        largestSize = maxOf(DEFAULT_PAGE_FONT_SIZE, spansOf(block).maxOfOrNull { it.size ?: DEFAULT_PAGE_FONT_SIZE } ?: DEFAULT_PAGE_FONT_SIZE)
    )

    /**
     * **Il corpo del segno dell'elenco puntato davanti alla riga**, anche
     * quando è nascosto perché la riga ne tiene solo il posto; 0 senza segno
     * (dal 27/10/2026).
     *
     * Compose mette il testo dentro le righe di un campo guardando **solo la
     * prima**: se lì c'è un pallino, scritto più grande del testo (a 20,
     * `bulletMarkerSizeFor`), il testo di tutto il campo scende di qualche
     * pixel. Misurato in "Fast notes": la riga di base a 60 pixel dalla cima
     * della riga nel pezzo che cominciava con "◦ BACK", a 49 in quello sopra.
     * Quando i due pezzi si univano il testo risaliva: lo spazio fra le righe
     * cambiava da solo. Per questo un pezzo comincia solo con una riga che ha
     * lo stesso segno della prima del pezzo sopra ([rechunk]), e due pezzi che
     * cominciano diversi non si uniscono ([keepVisibleTogether]).
     */
    private fun markerSizeOf(block: BlockEntity): Float =
        if (block.type == BlockType.BULLET_LIST_ITEM || block.heldListType == BlockType.BULLET_LIST_ITEM) {
            bulletMarkerSizeFor(block.indentLevel.coerceAtLeast(0)).value
        } else {
            0f
        }

    /**
     * Le righe nuove di un pezzo entrano nella sua lista: messe in cima a
     * quello che comincia con `firstId` (`afterId` null) ne diventano
     * l'inizio, altrimenti vanno dopo `afterId`. Così, cancellata una riga,
     * il pezzo sa sempre qual è la successiva.
     */
    private fun noteNewChunkLines(firstId: String, afterId: String?, newIds: List<String>) {
        if (newIds.isEmpty() || chunkMembers.isEmpty()) return
        if (afterId == null) {
            val key = chunkStartsOf(_blocks.value.filter { it.parentBlockId == null })[firstId] ?: return
            chunkMembers = chunkMembers + (key to (newIds + chunkMembers.getValue(key)))
            return
        }
        val (key, ids) = chunkMembers.entries.firstOrNull { afterId in it.value } ?: return
        val at = ids.indexOf(afterId) + 1
        chunkMembers = chunkMembers + (key to (ids.take(at) + newIds + ids.drop(at)))
    }

    /**
     * **La selezione arrivata in fondo a un pezzo va avanti** (dal 19/10/2026,
     * trovato dall'utente in Permanent notes: tenendo premuto e trascinando in
     * giù, la selezione si fermava a "Kor", che era l'ultima riga di un pezzo).
     * Ogni pezzo è un campo a sé (vedi [chunkMembers]) e un campo non seleziona
     * oltre le sue righe: mentre il dito trascina sul testo e la selezione
     * arriva all'ultima riga, il pezzo che viene dopo entra in questo. Le
     * posizioni del campo non cambiano (le righe arrivano in fondo), quindi il
     * gesto in corso continua. Di solito non serve: le righe sullo schermo sono
     * già un campo solo ([keepVisibleTogether]).
     *
     * @return se dopo `lastLineId` c'era un pezzo da unire.
     */
    fun joinChunkAfter(lastLineId: String): Boolean {
        val top = _blocks.value.filter { it.parentBlockId == null }
        val at = top.indexOfFirst { it.id == lastLineId }
        if (at < 0) return false
        val next = top.getOrNull(at + 1) ?: return false
        // **Mai una riga che appare diversa** (dal 10/11/2026, trovato dall'utente sul PC): il
        // campo ha un'altezza di riga sola (vedi [lineLookOf]), e selezionando la fine di un
        // titolo grande tutte le righe sotto prendevano lo spazio del titolo.
        if (lineLookOf(next) != lineLookOf(top[at])) return false
        val nextKey = chunkStartsOf(top)[next.id] ?: return false
        val nextIds = chunkMembers[nextKey] ?: return false
        val ownKey = chunkMembers.entries.firstOrNull { lastLineId in it.value }?.key
        var joined = chunkMembers - nextKey
        if (ownKey != null) joined = joined + (ownKey to (joined.getValue(ownKey) + nextIds))
        chunkMembers = joined
        _chunkVersion.value++
        return true
    }

    /**
     * Le righe sullo schermo l'ultima volta che la pagina si è fermata: fra due
     * di loro non c'è un confine di pezzo (vedi [keepVisibleTogether]), e
     * [rechunk] non ne mette.
     */
    private var visibleLineIds: Set<String> = emptySet()

    /** Quando si è scritto l'ultima volta in questa pagina (ms): vedi [keepVisibleTogether]. */
    private var lastTypedAt = 0L

    /**
     * **Le righe sullo schermo stanno in un campo solo** (dal 19/10/2026, per
     * la selezione che si fermava al confine fra due pezzi). Un campo non
     * seleziona oltre le sue righe, e unire i pezzi mentre si seleziona non va:
     * col dito su una maniglia, il campo che cambia testo la perde e la lente
     * resta bloccata; a selezione fatta, Compose non rimette più la barretta
     * Copy/Cut. Allora i pezzi si uniscono **prima**: quando la pagina si
     * ferma, due pezzi che si vedono tutti e due diventano uno, e la selezione
     * va su e giù per tutto lo schermo senza trovare confini.
     *
     * Il campo che resta è quello di sopra (le righe di sotto gli arrivano in
     * fondo); se però il campo col fuoco è quello di sotto resta lui, e le
     * righe di sopra gli vanno in cima: un campo col fuoco non si butta via.
     * Lui non si tocca mentre si scrive (la tastiera ricomincerebbe la parola)
     * né oltre [FOCUSED_CHUNK_CAP] righe, che scrivere costerebbe troppo.
     * I pezzi fuori dallo schermo tornano piccoli quando non si scrive
     * ([rechunk]).
     *
     * @param visibleIds le righe di primo livello sullo schermo, in ordine.
     * @param focusedId la riga col fuoco, se ce n'è una.
     * @return vero se il campo col fuoco è stato lasciato stare perché ci si
     * stava scrivendo: la schermata riprova quando si smette.
     */
    fun keepVisibleTogether(visibleIds: Collection<String>, focusedId: String?): Boolean {
        visibleLineIds = visibleIds.toHashSet()
        val joining = visibleLineIds.size >= 2 && chunkMembers.isNotEmpty()
        if (!joining && focusedId == null) return false
        val typing = System.currentTimeMillis() - lastTypedAt < TYPING_QUIET_MS
        val top = _blocks.value.filter { it.parentBlockId == null }
        val starts = chunkStartsOf(top)
        val byId = top.associateBy { it.id }

        // I pezzi della pagina, gruppo per gruppo: chiave (null per il primo
        // di un gruppo, che ha quella del gruppo) e righe.
        class Piece(val key: String?, val ids: MutableList<String>)
        class Group(val key: String, val pieces: MutableList<Piece>)
        val groups = mutableListOf<Group>()
        var group: Group? = null
        var islandAbove: BlockEntity? = null
        for (block in top) {
            if (!isFlowingTextType(block.type)) {
                group = null
                islandAbove = block
                continue
            }
            val current = group ?: Group(runGroupKey(islandAbove, block), mutableListOf()).also {
                groups += it
                group = it
            }
            val key = starts[block.id]
            if (current.pieces.isEmpty() || key != null) current.pieces += Piece(key, mutableListOf(block.id)) else current.pieces.last().ids += block.id
        }

        var changed = false
        var skippedForTyping = false
        // **Il campo col cursore cambia da un lato solo per volta** (dal
        // 27/10/2026): sa rimettere il cursore sulle stesse lettere quando delle
        // righe gli arrivano o gli escono sopra, oppure sotto, non tutte e due
        // insieme. Lasciato uscire il titolo di sopra e unito subito il pezzo di
        // sotto, il cursore restava allo stesso numero e finiva qualche riga più
        // giù (provato sul telefono: le lettere scritte dopo finivano in "1. Primo
        // numero"). Il resto si fa al giro dopo, che arriva perché i pezzi sono
        // cambiati.
        var focusedChanged = false

        // **Il campo col cursore tiene solo le righe che appaiono come quella
        // col cursore** (dal 27/10/2026, chiesto dall'utente: "se dovessi
        // modificare il font size di un blocco, per esempio un titolo a 50, e poi
        // vado a capo per scrivere altro..."). Le righe scritte sotto un titolo
        // grande restavano nel suo campo finché la tastiera era aperta, alte come
        // lui e col cursore alto come lui: sembravano scritte a 50, e invece erano
        // a 16. Come per l'unione qui sotto, si aspetta che non si scriva da un
        // po' (la tastiera ricomincerebbe la parola); poi le righe diverse escono
        // dal campo, sopra e sotto, e il campo resta lui — con la sua chiave, per
        // esteso se era quella del gruppo — quindi il cursore non si muove.
        if (focusedId != null) {
            for (g in groups) {
                val at = g.pieces.indexOfFirst { focusedId in it.ids }
                if (at < 0) continue
                val piece = g.pieces[at]
                val look = lineLookOf(byId.getValue(focusedId))
                val f = piece.ids.indexOf(focusedId)
                var from = f
                while (from > 0 && lineLookOf(byId.getValue(piece.ids[from - 1])) == look) from--
                var to = f
                while (to < piece.ids.size - 1 && lineLookOf(byId.getValue(piece.ids[to + 1])) == look) to++
                if (from == 0 && to == piece.ids.size - 1) break
                if (typing) {
                    skippedForTyping = true
                    break
                }
                // Un lato per volta, prima quello di sopra: il campo sa rimettere
                // il cursore quando le righe escono da un lato solo. L'altro
                // al giro dopo, che arriva perché i pezzi sono cambiati.
                g.pieces.removeAt(at)
                g.pieces.addAll(
                    at,
                    if (from > 0) {
                        listOf(
                            Piece("look:" + piece.ids.first(), piece.ids.subList(0, from).toMutableList()),
                            Piece(piece.key ?: g.key, piece.ids.subList(from, piece.ids.size).toMutableList())
                        )
                    } else {
                        listOf(
                            Piece(piece.key, piece.ids.subList(0, to + 1).toMutableList()),
                            Piece("chunk:" + piece.ids[to + 1], piece.ids.subList(to + 1, piece.ids.size).toMutableList())
                        )
                    }
                )
                changed = true
                focusedChanged = true
                break
            }
        }

        for (pieces in groups.takeIf { joining }.orEmpty().map { it.pieces }) {
            var i = 1
            while (i < pieces.size) {
                val above = pieces[i - 1]
                val below = pieces[i]
                if (above.ids.last() !in visibleLineIds || below.ids.first() !in visibleLineIds) {
                    i++
                    continue
                }
                // Righe che appaiono diverse restano in campi diversi (vedi
                // `lineLookOf`), e un pezzo che comincia con un segno diverso da
                // quello sopra non gli si unisce: il suo testo si sposterebbe
                // (vedi `markerSizeOf`).
                if (lineLookOf(byId.getValue(above.ids.last())) != lineLookOf(byId.getValue(below.ids.first())) ||
                    markerSizeOf(byId.getValue(above.ids.first())) != markerSizeOf(byId.getValue(below.ids.first()))
                ) {
                    i++
                    continue
                }
                val focusBelow = focusedId != null && focusedId in below.ids
                val focusAbove = focusedId != null && focusedId in above.ids
                if (focusBelow || focusAbove) {
                    if (typing) {
                        skippedForTyping = true
                        i++
                        continue
                    }
                    if (above.ids.size + below.ids.size > FOCUSED_CHUNK_CAP || focusedChanged) {
                        i++
                        continue
                    }
                    focusedChanged = true
                }
                if (focusBelow) {
                    below.ids.addAll(0, above.ids)
                    pieces.removeAt(i - 1)
                } else {
                    above.ids.addAll(below.ids)
                    pieces.removeAt(i)
                }
                changed = true
            }
        }
        if (changed) {
            chunkMembers = buildMap {
                for (g in groups) for (piece in g.pieces) piece.key?.let { put(it, piece.ids.toList()) }
            }
            _chunkVersion.value++
            if (!editing) scheduleRechunk()
        }
        return skippedForTyping
    }

    /** `splitBlockAbove` del repository, fatto sulla lista in memoria. */
    private fun splitAboveInList(
        list: List<BlockEntity>,
        blockId: String,
        newBlockId: String,
        newBlockType: BlockType,
        textBeforeJson: String,
        textAfterJson: String
    ): List<BlockEntity> {
        val current = list.find { it.id == blockId } ?: return list
        val shifted = list.map { b ->
            when {
                b.id == current.id -> b.copy(
                    orderIndex = current.orderIndex + 1,
                    textJson = textAfterJson,
                    isChecked = false,
                    numberStartsAt = null
                )
                b.parentBlockId == current.parentBlockId && b.orderIndex >= current.orderIndex ->
                    b.copy(orderIndex = b.orderIndex + 1)
                else -> b
            }
        }
        val above = BlockEntity(
            id = newBlockId,
            pageId = current.pageId,
            parentBlockId = current.parentBlockId,
            orderIndex = current.orderIndex,
            type = newBlockType,
            textJson = textBeforeJson,
            isChecked = current.isChecked,
            numberStartsAt = current.numberStartsAt,
            indentLevel = current.indentLevel,
            attrsJson = current.attrsForNewLine()
        )
        return (shifted + above).sortedBy { it.orderIndex }
    }

    /** `insertFirstChild` del repository, fatto sulla lista in memoria. */
    private fun firstChildInList(
        list: List<BlockEntity>,
        parentId: String,
        newBlockId: String,
        textJson: String
    ): List<BlockEntity> {
        val parent = list.find { it.id == parentId } ?: return list
        val shifted = list.map { b ->
            if (b.parentBlockId == parent.id) b.copy(orderIndex = b.orderIndex + 1) else b
        }
        val child = BlockEntity(
            id = newBlockId,
            pageId = parent.pageId,
            parentBlockId = parent.id,
            orderIndex = 0,
            textJson = textJson
        )
        return (shifted + child).sortedBy { it.orderIndex }
    }

    /** `insertSiblingBelow` del repository, fatto sulla lista in memoria. */
    private fun siblingBelowInList(
        list: List<BlockEntity>,
        blockId: String,
        newBlockId: String,
        type: BlockType,
        textJson: String
    ): List<BlockEntity> {
        val current = list.find { it.id == blockId } ?: return list
        val shifted = list.map { b ->
            if (b.parentBlockId == current.parentBlockId && b.orderIndex >= current.orderIndex + 1) {
                b.copy(orderIndex = b.orderIndex + 1)
            } else b
        }
        val sibling = BlockEntity(
            id = newBlockId,
            pageId = current.pageId,
            parentBlockId = current.parentBlockId,
            orderIndex = current.orderIndex + 1,
            type = type,
            textJson = textJson,
            isExpanded = current.isExpanded
        )
        return (shifted + sibling).sortedBy { it.orderIndex }
    }

    // Blocco su cui la UI deve richiedere il focus adesso — impostato da
    // splitBlockAt/splitBlockAtMultiple/addChildBlock/focusLastBlock/
    // deleteBlockAndFocusPrevious, consumato una volta applicato.
    private val _focusRequestBlockId = MutableStateFlow<String?>(null)
    val focusRequestBlockId: StateFlow<String?> = _focusRequestBlockId

    // Dove mettere il cursore dentro il blocco che sta per ricevere il
    // fuoco, quando non basta "dall'inizio" o "in fondo": per ora lo
    // usa la fusione di una riga dentro la casella sopra, che vuole il
    // cursore nel punto esatto in cui i due testi si sono uniti.
    private val _pendingCaret = MutableStateFlow<Pair<String, Int>?>(null)
    val pendingCaret: StateFlow<Pair<String, Int>?> = _pendingCaret

    fun consumePendingCaret() {
        _pendingCaret.value = null
    }

    // Richiesta "applica questa formattazione alla selezione corrente
    // del blocco X" — impostata dalla barra Aa, consumata dal BlockRow
    // del blocco interessato una volta applicata. Stesso schema di
    // _focusRequestBlockId: chi mostra la barra (PageEditorScreen) non
    // conosce la posizione esatta della selezione (vive dentro il
    // singolo BlockRow), quindi manda solo "quale blocco, quale
    // formattazione" e lascia che sia il blocco giusto ad applicarla
    // alla propria selezione attuale.
    private val _formatRequest = MutableStateFlow<Pair<String, FormatType>?>(null)
    val formatRequest: StateFlow<Pair<String, FormatType>?> = _formatRequest

    fun requestFormat(blockId: String, type: FormatType) {
        _formatRequest.value = blockId to type
    }

    fun consumeFormatRequest() {
        _formatRequest.value = null
    }

    // --- Il pennello: quale testo colorare ---
    //
    // **La selezione va messa da parte prima di aprire la finestra.**
    // Il pennello apre un pannello che sale dal basso, e il campo di
    // scrittura, perdendo il fuoco, chiude la selezione riducendola a
    // un cursore: al momento di applicare il colore non ci sarebbe più
    // niente di selezionato. I comandi della barra Aa non hanno questo
    // problema perché non aprono niente e agiscono all'istante.
    //
    // Quindi i campi dicono qui, mentre si usa, dove sta la selezione;
    // il pennello ne fa una copia nell'istante in cui lo si tocca —
    // prima che il fuoco se ne vada — e il colore si applica a quella.

    data class SelectionSnapshot(val blockIds: List<String>, val start: Int, val end: Int)

    private var liveSelection: SelectionSnapshot? = null
    private var capturedSelection: SelectionSnapshot? = null

    /** Posizioni **nel testo dei blocchi**, senza l'a-capo nascosto del campo. */
    fun reportSelection(blockIds: List<String>, start: Int, end: Int) {
        val selection = SelectionSnapshot(blockIds, start, end)
        liveSelection = selection
        _selectionTypography.value = typographyAt(selection)
        val linked = linkSelection(selection)
        _selectionLink.value = SelectionLink(
            hasText = start < end,
            link = linked?.let { linkOf(it) },
            onSite = linked != null && start == end
        )
    }

    /**
     * **Il testo selezionato con la sua formattazione** (dal 18/10/2026): per
     * copiarlo con lo stile. Le righe sono unite dall'a-capo. Null senza
     * testo selezionato.
     */
    fun selectedRichText(): List<RichTextSpan>? {
        val selection = liveSelection ?: return null
        if (selection.start >= selection.end) return null
        val all = _blocks.value
        val blocks = selection.blockIds.mapNotNull { id -> all.find { it.id == id } }
        if (blocks.isEmpty()) return null
        return sliceSpans(blocks.map { spansOf(it) }.joinLines(), selection.start, selection.end)
    }

    /**
     * **Le righe della selezione col loro tipo** (dal 19/10/2026): una voce per
     * ogni riga toccata dalla selezione, come le righe del testo copiato. Una
     * riga presa intera dice se è una casella (e se è spuntata), un elemento
     * di elenco o testo, e quanto è rientrata; una presa solo in parte è null.
     * Per copiarle col loro tipo: vedi `LinkClipboard`.
     */
    fun selectedLines(): List<CopiedLine?>? {
        val selection = liveSelection ?: return null
        if (selection.start >= selection.end) return null
        val all = _blocks.value
        val blocks = selection.blockIds.mapNotNull { id -> all.find { it.id == id } }
        if (blocks.isEmpty()) return null
        val result = mutableListOf<CopiedLine?>()
        var lineStart = 0
        for (block in blocks) {
            val lineEnd = lineStart + plainTextOf(block).length
            if (lineEnd >= selection.start && lineStart <= selection.end) {
                // Una riga vuota è presa intera solo se la selezione la attraversa.
                val whole = selection.start <= lineStart && selection.end >= lineEnd &&
                    (lineEnd > lineStart || (selection.start < lineStart && selection.end > lineEnd))
                result += if (whole) {
                    CopiedLine(
                        type = if (block.type in COPIED_LINE_TYPES) block.type else BlockType.PARAGRAPH,
                        checked = block.type == BlockType.CHECKBOX && block.isChecked,
                        indent = block.indentLevel
                    )
                } else {
                    null
                }
            }
            lineStart = lineEnd + 1
        }
        return result
    }

    /**
     * La selezione di adesso, messa da parte: prima di aprire il pannello
     * del colore o la finestrella del corpo del testo, che portano via il
     * fuoco (e con lui la selezione). Anche il menu dei font la usa.
     */
    fun captureSelection() {
        capturedSelection = liveSelection
    }

    /**
     * **Le righe toccate dalla selezione messa da parte** (dal 14/10/2026),
     * nell'ordine della pagina: quelle di cui è selezionato almeno un
     * carattere, e quelle vuote che stanno dentro la selezione. Una
     * selezione che finisce proprio all'inizio di una riga non la prende:
     * si è selezionato fino all'a-capo prima, come trascinando fino in fondo
     * a una riga. Senza testo selezionato nessuna: vale la riga del cursore.
     */
    fun capturedSelectionBlocks(): List<BlockEntity> {
        val selection = capturedSelection?.takeIf { it.start < it.end } ?: return emptyList()
        val all = _blocks.value
        val touched = mutableListOf<BlockEntity>()
        var pos = 0
        for (id in selection.blockIds) {
            val block = all.find { it.id == id } ?: continue
            val length = spansOf(block).plainText().length
            val inside = if (length == 0) {
                pos >= selection.start && pos < selection.end
            } else {
                selection.start < pos + length && selection.end > pos
            }
            if (inside) touched += block
            pos += length + 1 // +1 per l'a-capo che separa i blocchi
        }
        return touched
    }

    /** Vero se c'è davvero del testo selezionato da colorare. */
    fun hasCapturedSelection(): Boolean =
        capturedSelection?.let { it.start < it.end } == true

    fun applyCapturedColor(background: Boolean, hex: String?) {
        val selection = capturedSelection ?: return
        applyColorToRun(selection.blockIds, selection.start, selection.end, background, hex)
    }

    // --- Font e corpo del testo selezionato (barra Aa) ---
    //
    // Dal 25/09/2026 valgono **per il testo selezionato**, come il colore,
    // e stanno negli span (`RichTextSpan.font`, `size`); prima valevano per
    // tutta la pagina. Le due caselle della barra mostrano quelli del punto
    // in cui si è: il primo carattere della selezione, o quello subito
    // prima del cursore — che è anche quello che il testo scritto lì
    // eredita.

    data class SelectionTypography(val font: PageFont?, val size: Int?)

    private val _selectionTypography = MutableStateFlow(SelectionTypography(null, null))
    val selectionTypography: StateFlow<SelectionTypography> = _selectionTypography

    private fun typographyAt(selection: SelectionSnapshot): SelectionTypography {
        val all = _blocks.value
        val position = if (selection.end > selection.start) selection.start else selection.start - 1
        var pos = 0
        for (id in selection.blockIds) {
            val block = all.find { it.id == id } ?: continue
            val spans = spansOf(block)
            val length = spans.plainText().length
            if (position < pos + length || id == selection.blockIds.last()) {
                val span = spanAt(spans, position - pos) ?: return SelectionTypography(null, null)
                val font = span.font?.let { name -> runCatching { PageFont.valueOf(name) }.getOrNull() }
                return SelectionTypography(font, span.size)
            }
            pos += length + 1 // +1 per l'a-capo che separa i blocchi
        }
        return SelectionTypography(null, null)
    }

    // --- Il collegamento del testo selezionato (dal 17/10/2026) ---
    //
    // L'icona del collegamento nella barra sopra la tastiera compare quando
    // c'è del testo selezionato, o quando il cursore sta in un testo che ha
    // già un collegamento (per cambiarlo o toglierlo). Come per il pennello,
    // la selezione si mette da parte prima di aprire la finestrella.

    /**
     * `hasText` = c'è del testo selezionato; `link` = il collegamento che c'è
     * già lì, se c'è; `onSite` = il solo cursore sta in un collegamento o in
     * un sito scritto come testo (dal 27/10/2026, vedi `linkSelection`).
     */
    data class SelectionLink(val hasText: Boolean, val link: String?, val onSite: Boolean = false)

    private val _selectionLink = MutableStateFlow(SelectionLink(hasText = false, link = null))
    val selectionLink: StateFlow<SelectionLink> = _selectionLink

    /**
     * Il testo a cui va il collegamento: quello selezionato o, con il solo
     * cursore dentro un collegamento, tutto quel collegamento. Null se non
     * c'è né l'uno né l'altro.
     *
     * **Dal 27/10/2026 si prende il link intero** anche quando ne è
     * selezionato solo un pezzo — tenendo premuto su un link si seleziona
     * una parola sola — e lo stesso per **un sito scritto come testo
     * semplice** ("https://…" o "www.…", vedi `bareLinkAt`): quello che si
     * vede di un link si cambia tutto insieme (vedi `LinkDialog`).
     */
    private fun linkSelection(selection: SelectionSnapshot): SelectionSnapshot? {
        val all = _blocks.value
        var pos = 0
        for (id in selection.blockIds) {
            val block = all.find { it.id == id } ?: continue
            val spans = spansOf(block)
            val length = spans.plainText().length
            if (selection.start <= pos + length) {
                // Una selezione su più righe resta com'è.
                if (selection.end > pos + length) return selection.takeIf { it.start < it.end }
                val from = selection.start - pos
                val to = selection.end - pos
                val run = if (from < to) {
                    linkRunAt(spans, from + 1)?.takeIf { from >= it.first && to <= it.last + 1 }
                } else {
                    linkRunAt(spans, from)
                } ?: bareLinkAt(spans.plainText(), from, to)?.takeIf { linkInRange(spans, it.first, it.last + 1) == null }
                if (run != null) return selection.copy(start = pos + run.first, end = pos + run.last + 1)
                return selection.takeIf { it.start < it.end }
            }
            pos += length + 1 // +1 per l'a-capo che separa i blocchi
        }
        return null
    }

    /**
     * Il testo del link messo da parte, com'è scritto, per la finestrella: è
     * lui che "Show" cambia. Null se prende più righe (lì si cambia solo
     * l'indirizzo).
     */
    fun capturedLinkText(): String? {
        val selection = capturedSelection?.let { linkSelection(it) } ?: return null
        val (block, from, to) = singleBlockRange(selection) ?: return null
        return spansOf(block).plainText().substring(from, to)
    }

    /** Il blocco in cui sta tutta la selezione, con le posizioni dentro di lui; null se ne prende più d'uno. */
    private fun singleBlockRange(selection: SelectionSnapshot): Triple<BlockEntity, Int, Int>? {
        if (selection.start >= selection.end) return null
        val all = _blocks.value
        var pos = 0
        for (id in selection.blockIds) {
            val block = all.find { it.id == id } ?: continue
            val length = spansOf(block).plainText().length
            if (selection.start <= pos + length) {
                if (selection.end > pos + length) return null
                return Triple(block, selection.start - pos, selection.end - pos)
            }
            pos += length + 1
        }
        return null
    }

    /** Il primo collegamento dentro la selezione, o null. */
    private fun linkOf(selection: SelectionSnapshot): String? {
        val all = _blocks.value
        var pos = 0
        for (id in selection.blockIds) {
            val block = all.find { it.id == id } ?: continue
            val spans = spansOf(block)
            val length = spans.plainText().length
            val from = (selection.start - pos).coerceAtLeast(0)
            val to = (selection.end - pos).coerceAtMost(length)
            if (from < to) linkInRange(spans, from, to)?.let { return it }
            pos += length + 1
        }
        return null
    }

    /** Il collegamento che c'è già nel testo messo da parte, per riempire la finestrella. */
    fun capturedLink(): String? = capturedSelection?.let { linkSelection(it) }?.let { linkOf(it) }

    /**
     * Mette il collegamento [url] al testo messo da parte, o lo toglie con
     * null. Falso se non c'era testo selezionato né un collegamento sotto il
     * cursore.
     */
    fun applyLinkToCapturedSelection(url: String?, shown: String? = null): Boolean {
        val selection = capturedSelection?.let { linkSelection(it) } ?: return false
        if (url != null && shown != null) return replaceLinkText(selection, url, shown)
        transformRun(selection) { spans, from, to -> setLinkInRange(spans, from, to, url) }
        _selectionLink.value = _selectionLink.value.copy(link = url)
        return true
    }

    /**
     * **Quello che si vede del link** (dal 27/10/2026, chiesto dall'utente:
     * "il link per intero", "il titolo della pagina web" o "una scritta che
     * io scriverò manualmente"): il testo del link diventa [shown], con la
     * formattazione che aveva (grassetto, colore…) e il collegamento [url].
     * Il campo col cursore prende il testo nuovo come per Annulla
     * (`externalChangeTick`), e il cursore va in fondo al link.
     */
    private fun replaceLinkText(selection: SelectionSnapshot, url: String, shown: String): Boolean {
        val (block, from, to) = singleBlockRange(selection) ?: return false
        val text = shown.replace('\n', ' ')
        val spans = spansOf(block)
        val newSpans = if (spans.plainText().substring(from, to) == text) {
            setLinkInRange(spans, from, to, url)
        } else {
            setLinkInRange(spliceSpans(spans, from, to, text), from, from + text.length, url)
        }
        snapshotForStructuralChange()
        lastTextEditSnapshotBlockId = null
        val updated = block.copy(textJson = json.encodeToString(newSpans))
        _externalChangeTick.value += 1
        _linkTextChanges.tryEmit(LinkTextChange(block.id, newSpans, from + text.length))
        viewModelScope.launch { repository.saveBlock(updated) }
        _selectionLink.value = _selectionLink.value.copy(link = url)
        return true
    }

    /** Il testo nuovo di un link e dove va il cursore (in fondo al link), per i campi: vedi `replaceLinkText`. */
    data class LinkTextChange(val blockId: String, val spans: List<RichTextSpan>, val caret: Int)

    private val _linkTextChanges = MutableSharedFlow<LinkTextChange>(extraBufferCapacity = 4)
    val linkTextChanges: SharedFlow<LinkTextChange> = _linkTextChanges

    /** Il font sul testo messo da parte; falso se non c'era testo selezionato. */
    fun applyFontToCapturedSelection(font: PageFont?): Boolean {
        val selection = capturedSelection?.takeIf { it.start < it.end } ?: return false
        transformRun(selection) { spans, from, to -> setFontInRange(spans, from, to, font?.name) }
        _selectionTypography.value = _selectionTypography.value.copy(font = font)
        return true
    }

    /** Il corpo sul testo messo da parte (5-72; 16 torna al normale); falso se non c'era testo selezionato. */
    fun applySizeToCapturedSelection(size: Int): Boolean {
        val selection = capturedSelection?.takeIf { it.start < it.end } ?: return false
        val clamped = size.coerceIn(MIN_PAGE_FONT_SIZE, MAX_PAGE_FONT_SIZE)
        transformRun(selection) { spans, from, to -> setSizeInRange(spans, from, to, clamped, DEFAULT_PAGE_FONT_SIZE) }
        _selectionTypography.value = _selectionTypography.value.copy(size = clamped.takeIf { it != DEFAULT_PAGE_FONT_SIZE })
        return true
    }

    /**
     * **La formattazione messa da fuori a un blocco** (dal 30/09/2026): il
     * font, il corpo e il colore scelti dalla barra Aa e dal pennello, col
     * blocco e i suoi span nuovi. I campi dei blocchi a sé (il titolo di un
     * toggle, una casella da spuntare…) tengono i loro span mentre si
     * scrive, e quelli salvati li rileggono solo senza il fuoco: la
     * finestrella del corpo il fuoco non lo toglie, e il titolo restava
     * com'era — poi, alla lettera dopo, il campo riscriveva i suoi span e il
     * corpo nuovo spariva ("Non mi modifica la grandezza del font di un
     * titolo del toggle"). Con questo il campo li prende subito.
     */
    private val _externalSpans = MutableSharedFlow<Pair<String, List<RichTextSpan>>>(extraBufferCapacity = 16)
    val externalSpans: SharedFlow<Pair<String, List<RichTextSpan>>> = _externalSpans

    /**
     * Applica una modifica degli span a una selezione che può attraversare
     * più blocchi, come `applyColorToRun`: ogni blocco riceve la sua parte.
     */
    private fun transformRun(
        selection: SelectionSnapshot,
        transform: (List<RichTextSpan>, Int, Int) -> List<RichTextSpan>
    ) {
        val all = _blocks.value
        val runBlocks = selection.blockIds.mapNotNull { id -> all.find { it.id == id } }
        if (runBlocks.isEmpty()) return
        snapshotForStructuralChange()
        var pos = 0
        val updates = mutableListOf<BlockEntity>()
        for (block in runBlocks) {
            val text = plainTextOf(block)
            val blockStart = pos
            val blockEnd = pos + text.length
            pos = blockEnd + 1
            val overlapStart = maxOf(blockStart, selection.start) - blockStart
            val overlapEnd = minOf(blockEnd, selection.end) - blockStart
            if (overlapStart >= overlapEnd) continue
            val newSpans = transform(spansOf(block), overlapStart, overlapEnd)
            updates.add(block.copy(textJson = json.encodeToString(newSpans)))
            _externalSpans.tryEmit(block.id to newSpans)
        }
        viewModelScope.launch { updates.forEach { repository.saveBlock(it) } }
    }

    /**
     * Come `applyFormatToRun`: la selezione dell'utente è una sola ma
     * può attraversare più blocchi, e ognuno tiene i propri span.
     */
    fun applyColorToRun(
        runBlockIds: List<String>,
        selStart: Int,
        selEnd: Int,
        background: Boolean,
        hex: String?
    ) {
        if (selStart >= selEnd) return
        val all = _blocks.value
        val runBlocks = runBlockIds.mapNotNull { id -> all.find { it.id == id } }
        if (runBlocks.isEmpty()) return

        snapshotForStructuralChange()

        var pos = 0
        val updates = mutableListOf<BlockEntity>()
        for (block in runBlocks) {
            val text = plainTextOf(block)
            val blockStart = pos
            val blockEnd = pos + text.length
            pos = blockEnd + 1 // +1 per l'a-capo che separa i blocchi

            val overlapStart = maxOf(blockStart, selStart) - blockStart
            val overlapEnd = minOf(blockEnd, selEnd) - blockStart
            if (overlapStart >= overlapEnd) continue

            val newSpans = setColorInRange(
                spansOf(block),
                overlapStart,
                overlapEnd,
                background,
                hex
            )
            updates.add(block.copy(textJson = json.encodeToString(newSpans)))
            _externalSpans.tryEmit(block.id to newSpans)
        }

        viewModelScope.launch { updates.forEach { repository.saveBlock(it) } }
    }

    // --- Annulla / Ripristina ---
    //
    // Un intero "scatto" della lista blocchi della pagina viene salvato
    // prima di ogni modifica strutturale (cambio tipo, divisione,
    // indentazione, spostamento, eliminazione...) e prima di ogni nuova
    // "sessione" di digitazione (la prima lettera scritta in un blocco
    // diverso da quello dell'ultimo scatto salvato) — non ad ogni
    // singolo tasto premuto, altrimenti Annulla toglierebbe una lettera
    // alla volta invece di annullare "quello che hai appena scritto" in
    // un colpo solo, come in un editor vero. Copre i blocchi (testo,
    // tipo, struttura); non copre il contenuto delle celle di tabelle o
    // database, che vivono in tabelle del database separate.
    //
    // **Due tipi di passo.** Oltre alle foto dei blocchi ci sono i
    // cambiamenti fatti alle pagine — "Move to", "Duplicate", "Move to
    // trash" — che toccano altre pagine e non si disfano rimettendo i
    // blocchi di questa: ognuno si porta dietro quello che serve a
    // tornare indietro (`PageRepository.PageChange`). Il passo di pagina
    // è modificabile perché Ripristina lo rifà con id nuovi (il
    // collegamento creato di nuovo), e l'Annulla successivo deve
    // conoscerli.
    private sealed interface UndoStep
    private class BlocksStep(val blocks: List<BlockEntity>) : UndoStep
    private class PageStep(var change: PageRepository.PageChange) : UndoStep

    private val undoStack = ArrayDeque<UndoStep>()
    private val redoStack = ArrayDeque<UndoStep>()
    private var lastTextEditSnapshotBlockId: String? = null

    // Annulla e Ripristina si fanno **uno alla volta**, nell'ordine in cui
    // sono stati toccati: un passo di pagina lavora sul database per un
    // attimo, e due tocchi veloci non devono sovrapporsi.
    private val historyLock = Mutex()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun updateUndoRedoAvailability() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun pushUndoSnapshot() {
        pushStep(BlocksStep(_blocks.value))
    }

    private fun pushStep(step: UndoStep) {
        undoStack.addLast(step)
        if (undoStack.size > MAX_UNDO_STEPS) undoStack.removeFirst()
        redoStack.clear()
        updateUndoRedoAvailability()
    }

    /**
     * Mette in cronologia un cambiamento fatto alle pagine. Chiude la
     * sessione di scrittura come ogni modifica di struttura: la prossima
     * lettera scritta sarà un passo a sé.
     */
    private fun pushPageChange(change: PageRepository.PageChange) {
        closeEditSession()
        lastTextEditSnapshotBlockId = null
        pushStep(PageStep(change))
    }

    /**
     * **Il passo lasciato dai tre puntini.** "Move to", "Duplicate" e
     * "Move to trash" dal menu dei tre puntini lasciano la pagina su cui
     * si fanno — si torna indietro, o si entra nella copia — e il loro
     * Annulla deve stare nella pagina che si vede dopo. Quella lo prende
     * qui, appena compare (vedi `PendingPageUndo`).
     */
    fun adoptPendingPageChange() {
        PendingPageUndo.take()?.let { pushPageChange(it) }
    }

    // Chiesto dopo aver annullato la "Duplicate" della pagina aperta
    // stando dentro la copia: la copia è andata nel cestino, e restarci
    // davanti non ha senso. La schermata torna indietro.
    private val _leavePage = MutableStateFlow(false)
    val leavePage: StateFlow<Boolean> = _leavePage
    fun consumeLeavePage() {
        _leavePage.value = false
    }

    /** Da chiamare prima di una modifica di testo/formattazione (updateBlockText, updateBlockSpans). */
    private fun snapshotForTextEdit(blockId: String) {
        beginEditSession(blockId)
        if (lastTextEditSnapshotBlockId != blockId) {
            pushUndoSnapshot()
            lastTextEditSnapshotBlockId = blockId
        }
    }

    /** Da chiamare prima di qualsiasi altra modifica (tipo, struttura, posizione, eliminazione...). */
    private fun snapshotForStructuralChange() {
        closeEditSession()
        pushUndoSnapshot()
        lastTextEditSnapshotBlockId = null
    }

    // --- Cronologia delle modifiche (la voce "Updates") ---
    //
    // **Una voce per sessione di scrittura, non per tasto premuto.**
    // Finché si continua a scrivere nello stesso blocco è una modifica
    // sola; si chiude quando il cursore passa a un altro blocco, quando
    // cambia la struttura della pagina o quando si esce dalla pagina.
    // È lo stesso confine che l'Annulla usa già, ed è quello giusto
    // anche qui: "Esmepoi → Esempio" è una correzione, non sette.

    private data class EditSession(val blockId: String, val before: String)

    private var editSession: EditSession? = null

    /** Il testo semplice di un blocco, o del titolo se l'id è quello riservato. */
    private fun textOfEditable(blockId: String): String =
        if (blockId == PageEditEntity.TITLE_BLOCK_ID) {
            _page.value?.title.orEmpty()
        } else {
            _blocks.value.find { it.id == blockId }?.let { plainTextOf(it) }.orEmpty()
        }

    private fun beginEditSession(blockId: String) {
        if (editSession?.blockId == blockId) return
        closeEditSession()
        editSession = EditSession(blockId, textOfEditable(blockId))
    }

    /**
     * Chiude la sessione aperta e, se qualcosa è cambiato davvero, ne
     * scrive la voce.
     *
     * **L'attesa prima di leggere il "dopo" è voluta.** Quello che si è
     * appena scritto viaggia verso il database e torna indietro dal
     * flusso dei blocchi: leggendo subito si prenderebbe il testo di un
     * attimo prima, e la cronologia racconterebbe una correzione
     * troncata all'ultima lettera. Qui un ritardo non costa niente —
     * nessuno sta aspettando questa riga — mentre sbagliarla la
     * renderebbe inservibile.
     */
    fun closeEditSession() {
        val session = editSession ?: return
        editSession = null
        val pageId = currentPageId ?: return
        // **Non su `viewModelScope`.** L'ultima sessione si chiude
        // proprio mentre si esce dalla pagina, e uscendo quello scope
        // viene annullato: la riga non verrebbe mai scritta. Vedi
        // `PageRepository.recordEditAfterSettling`.
        repository.recordEditAfterSettling(EDIT_SETTLE_MS) {
            val after = textOfEditable(session.blockId)
            if (after == session.before) {
                null
            } else {
                PageEditEntity(
                    pageId = pageId,
                    blockId = session.blockId,
                    before = session.before,
                    after = after
                )
            }
        }
    }

    /** Le modifiche fatte a questa pagina, dalla più recente. */
    private val _edits = MutableStateFlow<List<PageEditEntity>>(emptyList())
    val edits: StateFlow<List<PageEditEntity>> = _edits

    override fun onCleared() {
        closeEditSession()
        // Uscendo, com'è rimasta la pagina: dopo un attimo, perché le ultime
        // scritture arrivino (il ViewModel se ne va, il salvataggio no).
        currentPageId?.let { repository.saveVersionLater(it, VersionReason.CLOSED, VERSION_AFTER_LEAVING_MS) }
        super.onCleared()
    }

    // --- "Page history": le versioni della pagina (dal 02/11/2026) ---
    //
    // Chiesto dall'utente ("Cronologia con ripristino"): una rete di
    // sicurezza accanto al backup. Una versione nasce aprendo la pagina,
    // ogni [VERSION_EVERY_MS] mentre è aperta e uscendo (anche dall'app),
    // solo se la pagina è cambiata: vedi `PageRepository.saveVersion`.

    private val _versions = MutableStateFlow<List<PageVersionInfo>>(emptyList())
    val versions: StateFlow<List<PageVersionInfo>> = _versions
    private var versionsFlowJob: Job? = null
    private var versionTimerJob: Job? = null

    /** L'app va in secondo piano: com'è adesso, come uscendo dalla pagina. */
    fun saveVersionOnLeave() {
        currentPageId?.let { repository.saveVersionLater(it, VersionReason.CLOSED, VERSION_AFTER_LEAVING_MS) }
    }

    // -----------------------------------------------------------------------
    // **Menzioni "@" e backlink** (dal 03/11/2026, chiesto dall'utente:
    // "Collegamenti all'indietro (backlink), come Obsidian e Notion. In fondo
    // a ogni pagina vedresti quali altre pagine la richiamano. Insieme, le
    // menzioni @ dentro il testo per citare una pagina o una data al volo").
    // Vedi `PageMentions.kt`.
    // -----------------------------------------------------------------------

    private val _backlinks = MutableStateFlow<List<PageRepository.Backlink>>(emptyList())
    val backlinks: StateFlow<List<PageRepository.Backlink>> = _backlinks

    /** Rilegge chi richiama questa pagina: aprendola, e tornandoci da un'altra. */
    fun loadBacklinks() {
        val pageId = currentPageId ?: return
        viewModelScope.launch {
            val found = runCatching { withContext(Dispatchers.IO) { repository.backlinksOf(pageId) } }.getOrNull() ?: return@launch
            if (currentPageId == pageId) _backlinks.value = found
        }
    }

    /** Le pagine da proporre nel menu "@" per [query]. */
    suspend fun mentionCandidates(query: String): List<PageEntity> =
        runCatching { withContext(Dispatchers.IO) { repository.mentionCandidates(query, currentPageId) } }.getOrDefault(emptyList())

    /**
     * **Una data menzionata, cambiata** toccandola: il testo diventa [text] e
     * il collegamento il giorno [date], con lo stesso segno (`movedDateLink`).
     * Come per il testo di un link, il campo la riprende come un Annulla.
     */
    fun changeDateMention(link: String, date: LocalDate, text: String) {
        val block = _blocks.value.firstOrNull { b -> spansOf(b).any { it.link == link } } ?: return
        val spans = spansOf(block)
        val newSpans = replaceLinkedText(spans, link, text, movedDateLink(link, date)) ?: return
        val caret = spans.takeWhile { it.link != link }.sumOf { it.text.length } + text.length
        snapshotForStructuralChange()
        lastTextEditSnapshotBlockId = null
        _externalChangeTick.value += 1
        _linkTextChanges.tryEmit(LinkTextChange(block.id, newSpans, caret))
        val updated = block.copy(textJson = json.encodeToString(newSpans))
        viewModelScope.launch { runCatching { repository.saveBlock(updated) } }
    }

    /**
     * La pagina aperta e le sue repository, per chi lavora sulla pagina fuori
     * da qui: l'esportazione in un file (sul telefono, `PageEditorExport.kt`).
     */
    val openPageId: String? get() = currentPageId
    val pages: PageRepository get() = repository

    /** Una versione da vedere: il contenuto e i titoli di adesso delle pagine collegate. */
    suspend fun versionPreview(versionId: String): VersionPreview? = runCatching {
        withContext(Dispatchers.IO) {
            repository.versionContent(versionId)?.let { content ->
                VersionPreview(content, repository.titlesOf(content.blocks.mapNotNull { it.linkedPageId }))
            }
        }
    }.getOrNull()

    /**
     * **Rimette la pagina com'era nella versione** (vedi
     * `PageRepository.restoreVersion`, che prima salva com'è adesso).
     * Annulla e Ripeti ricominciano da qui: le loro foto parlano della pagina
     * di prima. [onDone] riceve se è andata.
     */
    fun restoreVersion(versionId: String, onDone: (Boolean) -> Unit) {
        val pageId = currentPageId ?: return
        closeEditSession()
        viewModelScope.launch {
            val restored = historyLock.withLock {
                pendingInserts.clear()
                lastTextEditSnapshotBlockId = null
                // Il contenuto che arriva dal database va preso anche sopra
                // una modifica in sospeso, come dopo un Annulla.
                _externalChangeTick.value += 1
                // Leggere, comprimere e riscrivere la pagina: lontano dallo schermo.
                withContext(Dispatchers.IO) { runCatching { repository.restoreVersion(pageId, versionId) }.getOrDefault(false) }
            }
            if (restored) {
                undoStack.clear()
                redoStack.clear()
                updateUndoRedoAvailability()
            }
            onDone(restored)
        }
    }

    // Incrementato ad ogni annulla/ripristina. La UI lo osserva per
    // sapere che il prossimo contenuto in arrivo dal database va
    // accettato anche se ha una propria modifica in sospeso — è un
    // cambiamento voluto proveniente da fuori, non il database rimasto
    // indietro.
    private val _externalChangeTick = MutableStateFlow(0)
    val externalChangeTick: StateFlow<Int> = _externalChangeTick

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        // Annullare vuol dire tornare alla foto di prima: una riga nuova
        // ancora in sospeso non deve restare appiccicata sopra.
        pendingInserts.clear()
        lastTextEditSnapshotBlockId = null
        _externalChangeTick.value += 1
        when (previous) {
            is BlocksStep -> {
                redoStack.addLast(BlocksStep(_blocks.value))
                updateUndoRedoAvailability()
                viewModelScope.launch { historyLock.withLock { applySnapshot(previous.blocks) } }
            }
            is PageStep -> {
                redoStack.addLast(previous)
                updateUndoRedoAvailability()
                viewModelScope.launch {
                    historyLock.withLock {
                        val undone = repository.undoPageChange(previous.change)
                        if (undone == null) {
                            // La pagina non c'è più (cancellata per sempre
                            // nel frattempo): niente da rifare.
                            redoStack.remove(previous)
                            updateUndoRedoAvailability()
                        } else {
                            previous.change = undone
                            // Annullata la copia dentro cui si sta: fuori.
                            if (undone is PageRepository.PageChange.Duplicated && undone.copyId == currentPageId) {
                                _leavePage.value = true
                            }
                        }
                    }
                }
            }
        }
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        pendingInserts.clear()
        lastTextEditSnapshotBlockId = null
        _externalChangeTick.value += 1
        when (next) {
            is BlocksStep -> {
                undoStack.addLast(BlocksStep(_blocks.value))
                updateUndoRedoAvailability()
                viewModelScope.launch { historyLock.withLock { applySnapshot(next.blocks) } }
            }
            is PageStep -> {
                undoStack.addLast(next)
                updateUndoRedoAvailability()
                viewModelScope.launch {
                    historyLock.withLock {
                        val redone = repository.redoPageChange(next.change)
                        if (redone == null) {
                            undoStack.remove(next)
                            updateUndoRedoAvailability()
                        } else {
                            next.change = redone
                        }
                    }
                }
            }
        }
    }

    private suspend fun applySnapshot(snapshot: List<BlockEntity>) {
        val pageId = currentPageId ?: return
        repository.replaceAllBlocks(pageId, snapshot)
    }

    fun load(pageId: String) {
        if (currentPageId == pageId) return
        // Un'altra pagina nello stesso ViewModel: quella di prima com'è rimasta.
        currentPageId?.let { repository.saveVersionLater(it, VersionReason.CLOSED) }
        currentPageId = pageId
        visibleLineIds = emptySet()
        undoStack.clear()
        redoStack.clear()
        lastTextEditSnapshotBlockId = null
        updateUndoRedoAvailability()

        pageFlowJob?.cancel()
        pageFlowJob = viewModelScope.launch {
            if (repository.getPage(pageId) == null && pageId == PageEntity.ROOT_PAGE_ID) {
                // Primo avvio dell'app: la pagina radice (quella che
                // prima era la schermata Home separata) non esiste
                // ancora — la creiamo al volo con l'ID fisso, così
                // esiste sempre esattamente una volta.
                repository.createPage(
                    PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space", icon = "🏠")
                )
            }
            // Seguita nel tempo, non letta una volta sola: la stessa
            // pagina può stare aperta in due punti della pila di
            // navigazione, e le modifiche qui sotto salvano la pagina
            // intera. Chi resta indietro riscriverebbe il titolo (o
            // l'icona, o la copertina) vecchio sopra a quello nuovo.
            repository.observePage(pageId)
                .distinctUntilChanged()
                .collect { fromDb -> if (fromDb != null) _page.value = fromDb }
        }

        editsFlowJob?.cancel()
        editsFlowJob = viewModelScope.launch {
            repository.observeEdits(pageId).collect { _edits.value = it }
        }

        // Le menzioni "@" col titolo di adesso delle pagine che citano (dal 03/11/2026).
        _backlinks.value = emptyList()
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.refreshMentionTitles(pageId, Strings.untitled) } }
        }

        // "Page history": com'era aprendola, poi ogni dieci minuti se cambia.
        versionsFlowJob?.cancel()
        versionsFlowJob = viewModelScope.launch {
            repository.observeVersions(pageId).collect { _versions.value = it }
        }
        repository.saveVersionLater(pageId, VersionReason.OPENED)
        versionTimerJob?.cancel()
        versionTimerJob = viewModelScope.launch {
            while (true) {
                delay(VERSION_EVERY_MS)
                withContext(Dispatchers.IO) { runCatching { repository.saveVersion(pageId, VersionReason.WHILE_OPEN) } }
            }
        }

        blocksFlowJob?.cancel()
        pendingInserts.clear()
        placeholder = null
        syncedWatcher.reset()
        blocksFlowJob = viewModelScope.launch {
            repository.getBlocks(pageId).collect { loaded ->
                lastLoadedBlocks = loaded
                publishBlocks(loaded)
                syncedWatcher.onLoaded(loaded)
                // Una nota al volo appena nata (dal 02/11/2026): il cursore sulla
                // sua prima riga, per scrivere subito. Vedi `QuickNotes`.
                QuickNotes.takeFocus(pageId) { id -> loaded.any { it.id == id } }?.let { _focusRequestBlockId.value = it }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        val current = _page.value ?: return
        // Il titolo non è un blocco ma si modifica come loro, quindi
        // entra nella cronologia con l'id riservato.
        beginEditSession(PageEditEntity.TITLE_BLOCK_ID)
        val updated = current.copy(title = newTitle)
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
    }

    // --- Le voci del menu "..." della pagina ---

    /** Blocca o sblocca il contenuto: da bloccata la pagina si legge ma non si scrive. */
    fun toggleLocked() {
        val current = _page.value ?: return
        val wanted = !current.isLocked
        _page.value = current.copy(isLocked = wanted)
        viewModelScope.launch { repository.setLocked(current.id, wanted) }
    }

    /**
     * Blocca o sblocca la pagina con la password (vedi `AppLock`). Bloccata,
     * si richiude subito: il cancello davanti alla pagina chiede di
     * sbloccarla, così si vede che il blocco c'è.
     */
    fun setPasswordLocked(wanted: Boolean) {
        val current = _page.value ?: return
        if (current.id == PageEntity.ROOT_PAGE_ID) return
        _page.value = current.copy(isProtected = wanted)
        if (wanted) AppLock.lock(current.id)
        viewModelScope.launch { repository.setProtected(current.id, wanted) }
    }

    /** Blocca o sblocca l'impaginazione di un database (vista, ordine, filtro, colonne). */
    fun toggleViewLocked() {
        val current = _page.value ?: return
        val wanted = !current.isViewLocked
        _page.value = current.copy(isViewLocked = wanted)
        viewModelScope.launch { repository.setViewLocked(current.id, wanted) }
    }

    /**
     * Fa una copia della pagina, sottopagine comprese, e la mette dove si
     * è scelto. `onDone` riceve l'id della copia, per poterci andare subito.
     */
    fun duplicatePage(
        target: PageRepository.DuplicateTarget,
        titleSuffix: String,
        imageStore: PageImageStore,
        onDone: (String) -> Unit
    ) {
        val current = _page.value ?: return
        viewModelScope.launch {
            repository.duplicatePage(current.id, target, titleSuffix) { imageStore.copy(it) }?.let { copyId ->
                // Si entra nella copia: il suo Annulla lo prende lei.
                PendingPageUndo.offer(repository.describeCopy(copyId))
                onDone(copyId)
            }
        }
    }

    /** Dove questa pagina non si può spostare: vedi `PageRepository.moveExclusions`. */
    fun loadMoveExclusions(onReady: (PageRepository.MoveExclusions) -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch { onReady(repository.moveExclusions(current.id)) }
    }

    /** "Move to" dai tre puntini. `onDone` arriva solo se lo spostamento c'è stato. */
    fun movePageTo(destination: PageRepository.PageTreeNode, onDone: () -> Unit) {
        val current = _page.value ?: return
        viewModelScope.launch {
            repository.movePageTo(current.id, destination)?.let { change ->
                // Si torna indietro: l'Annulla lo prende la pagina che si vede dopo.
                PendingPageUndo.offer(change)
                onDone()
            }
        }
    }

    /**
     * **"Move to database"** dai tre puntini (dal 05/10/2026): la pagina
     * diventa una pagina nuova del database scelto, col suo contenuto
     * (vedi `PageRepository.moveIntoDatabase`). Come "Move to": si torna
     * indietro, e l'Annulla lo prende la pagina che si vede dopo.
     */
    fun movePageToDatabase(destination: PageRepository.PageTreeNode, rowTitle: String, onDone: () -> Unit) {
        val current = _page.value ?: return
        val databaseId = destination.pageId ?: return
        viewModelScope.launch {
            repository.moveIntoDatabase(current.id, databaseId, rowTitle)?.let { change ->
                PendingPageUndo.offer(change)
                onDone()
            }
        }
    }

    /**
     * Butta la pagina nel cestino. La pagina resta nel database: quello
     * che sparisce è il collegamento che la mostrava. `onDone` serve a
     * tornare indietro, perché restare su una pagina che non è più
     * raggiungibile da nessuna parte non avrebbe senso.
     */
    fun moveToTrash(onDone: () -> Unit) {
        val current = _page.value ?: return
        closeEditSession()
        viewModelScope.launch {
            repository.moveToTrash(current.id)?.let { PendingPageUndo.offer(it) }
            onDone()
        }
    }

    /** Dalla barra del cestino: la pagina torna in fondo al menu principale. */
    fun restoreFromTrash() {
        val current = _page.value ?: return
        viewModelScope.launch { repository.restoreFromTrash(current.id) }
    }

    /**
     * Dalla barra del cestino: la pagina se ne va per sempre, con le sue
     * immagini. `onDone` arriva a cancellazione finita — chi chiama torna
     * indietro, e questa pagina da lì in poi non esiste più.
     */
    fun deletePermanently(imageStore: PageImageStore, onDone: () -> Unit) {
        val current = _page.value ?: return
        closeEditSession()
        viewModelScope.launch {
            repository.deletePermanently(current.id).forEach { imageStore.delete(it) }
            onDone()
        }
    }

    /**
     * Il conteggio del testo della pagina, per la voce "X words" del menu
     * dei tre puntini. Letto dal database ogni volta che si apre il menu:
     * il testo si salva a ogni tasto, quindi lì è già tutto.
     */
    fun loadTextStats(onReady: (TextStats) -> Unit) {
        val pageId = currentPageId ?: return
        viewModelScope.launch { onReady(repository.textStats(pageId)) }
    }

    /**
     * Mette o toglie questa pagina dai preferiti.
     *
     * Scrive **solo quella colonna**: la stessa pagina può stare aperta
     * in due schermate, e riscriverla intera da qui rimetterebbe
     * indietro un titolo cambiato nell'altra. Il valore in mano alla UI
     * si aggiorna subito, così la stellina non aspetta il giro dal
     * database.
     */
    fun toggleFavorite() {
        val current = _page.value ?: return
        if (current.id == PageEntity.ROOT_PAGE_ID) return
        val wanted = !current.isFavorite
        _page.value = current.copy(isFavorite = wanted)
        viewModelScope.launch { repository.setFavorite(current.id, wanted) }
    }

    fun updateIcon(newIcon: String) {
        val current = _page.value ?: return
        val updated = current.copy(icon = newIcon)
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
    }

    /**
     * **L'icona della pagina** scelta nel selettore: un'emoji, un'immagine o
     * niente (torna il 📄 di riserva). Il nome del file dell'immagine di
     * prima viene restituito a chi chiama, che è l'unico a sapere dove
     * stanno i file e può cancellarlo: lasciarlo lì vorrebbe dire riempire
     * la cartella di immagini irraggiungibili.
     */
    // --- Il correttore (dal 08/10/2026): vedi `ProofingUi.kt` ---

    /** Il dizionario dell'utente: vale in tutte le pagine e in tutte le lingue. */
    val personalWords: Flow<List<String>> = repository.observePersonalWords()

    fun addToDictionary(word: String) {
        viewModelScope.launch { repository.addPersonalWord(word) }
    }

    /** "Proofing" dai tre puntini: acceso, spento o come Settings, e la lingua (null = quella di Settings). */
    fun setProofing(mode: Int, language: String?) {
        val current = _page.value ?: return
        _page.value = current.copy(proofingMode = mode, proofingLanguage = language)
        viewModelScope.launch { repository.setPageProofing(current.id, mode, language) }
    }

    /** "Full or page line" dai tre puntini: come Settings, righe intere o righe a capo (vedi `PageEntity.fullLineMode`). */
    fun setFullLineMode(mode: Int) {
        val current = _page.value ?: return
        _page.value = current.copy(fullLineMode = mode)
        viewModelScope.launch { repository.setPageFullLine(current.id, mode) }
    }

    // --- "Paragraph" (dal 21/10/2026): vedi `ParagraphLook` ---

    /** Le misure di questa pagina: quelle sue, o quelle di Settings. */
    fun paragraphLook(page: PageEntity? = _page.value): ParagraphLook = ParagraphLook(
        page = page?.paragraph ?: PageParagraph(),
        appSpacing = AppSettings.blockSpacing,
        appGaps = AppSettings.listGaps
    )

    /**
     * Dove comincia un gruppo di righe con uno spazio suo (vedi
     * `spacingStartsOf`): la stessa risposta per la schermata, che lo
     * disegna come un campo a sé, e per `resolveRun`, che deve sapere quali
     * righe ha in mano quel campo.
     */
    fun spacingStartsOf(blocks: List<BlockEntity>): Map<String, String> {
        val look = paragraphLook()
        if (look.isPlain && blocks.none { it.paragraphAttrs?.spacing != null }) return emptyMap()
        return spacingStartsOf(blocks, look, ::isFlowingTextType)
    }

    /** "Paragraph" dai tre puntini: le misure di questa pagina (le parti null seguono Settings). */
    fun setPageParagraph(paragraph: PageParagraph) {
        val current = _page.value ?: return
        _page.value = current.copy(paragraphJson = PageParagraph.encode(paragraph))
        viewModelScope.launch { repository.setPageParagraph(current.id, paragraph) }
    }

    /**
     * **"Paragraph" di un gruppo di righe**: lo spazio fra i blocchi
     * ([spacing], null = come la pagina) e quello dopo il marcatore di ogni
     * elenco ([gapFor] per tipo, null = come la pagina). Si annulla come le
     * altre modifiche, e tocca solo le voci dei blocchi, non il testo.
     */
    fun setLinesParagraph(lines: List<BlockEntity>, spacing: Int?, gapFor: (BlockType) -> Int?) {
        if (lines.isEmpty()) return
        snapshotForStructuralChange()
        val changes = lines.associate { line ->
            val listType = line.type.takeIf { it in MARKER_LIST_TYPES } ?: line.heldListType
            val gap = listType?.let(gapFor)
            line.id to { attrs: BlockAttrs -> attrs.copy(spacing = spacing, markerGap = gap) }
        }
        viewModelScope.launch { repository.updateBlocksAttrs(changes) }
    }

    /** "Ignore": la parola non si segna più, solo in questa pagina. */
    fun ignoreWord(word: String) = setIgnored { it + word }

    /** "Check again": la parola ignorata torna a segnarsi. */
    fun stopIgnoring(word: String) = setIgnored { it - word }

    private fun setIgnored(change: (Set<String>) -> Set<String>) {
        val current = _page.value ?: return
        val words = change(current.ignoredWords)
        _page.value = current.copy(proofingIgnored = words.sorted().joinToString("\n").ifEmpty { null })
        viewModelScope.launch { repository.setPageIgnoredWords(current.id, words) }
    }

    fun setIcon(choice: IconChoice): String? {
        val current = _page.value ?: return null
        val updated = current.copy(icon = choice.iconField, iconImage = choice.imageField, iconSymbol = choice.symbolField)
        _page.value = updated
        viewModelScope.launch { repository.setIcon(current.id, choice) }
        return replacedImage(current.iconImage, choice)
    }

    /**
     * La copertina, o null per toglierla. Vedi `setIcon`.
     *
     * L'inquadratura torna a zero: era stata scelta guardando l'altra
     * immagine, e su una nuova darebbe un ritaglio deciso a caso. Anche
     * quella delle schede della galleria, per la stessa ragione.
     */
    fun setCoverImage(fileName: String?): String? {
        val current = _page.value ?: return null
        val previous = current.coverImage
        val updated = current.copy(
            coverImage = fileName,
            coverScale = 1f,
            coverOffsetX = 0f,
            coverOffsetY = 0f,
            cardCoverScale = null,
            cardCoverOffsetX = null,
            cardCoverOffsetY = null
        )
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
        return previous?.takeIf { it != fileName }
    }

    /** Come è inquadrata la copertina: ingrandimento e spostamento. */
    fun setCoverTransform(scale: Float, offsetX: Float, offsetY: Float) {
        val current = _page.value ?: return
        val updated = current.copy(
            coverScale = scale,
            coverOffsetX = offsetX,
            coverOffsetY = offsetY
        )
        _page.value = updated
        viewModelScope.launch { repository.updatePage(updated) }
    }

    /** Aggiorna il testo semplice di un blocco (senza formattazione inline). */
    fun updateBlockText(block: BlockEntity, newText: String) {
        updateBlockSpans(block, listOf(RichTextSpan(text = newText)))
    }

    /**
     * Aggiorna gli span (testo + formattazione inline) di un blocco.
     *
     * Scrive **solo la colonna del testo**, non l'entità intera. La
     * copia del blocco che ha in mano la schermata può essere di un
     * istante fa: risalvandola per intero si rimandavano indietro anche
     * il posto e la spunta di allora, e dopo un a-capo quel posto è
     * quello che nel frattempo ha preso la riga nuova.
     */
    fun updateBlockSpans(block: BlockEntity, spans: List<RichTextSpan>) {
        snapshotForTextEdit(block.id)
        val textJson = json.encodeToString(spans)
        viewModelScope.launch { blocksDb.setBlockText(block.id, textJson) }
    }

    /**
     * Cambia il tipo di un blocco **scrivendo solo quella colonna**.
     *
     * Prima salvava il blocco intero, e il blocco che arriva qui è la
     * copia che la UI aveva in mano quando si è toccato il menu. Col
     * menu "/" questo voleva dire: si scrive `/toggle`, il campo toglie
     * la barra e il comando dal testo e lo salva, e subito dopo il cambio
     * di tipo **riscriveva il testo vecchio**, barra compresa. Era la "/"
     * che restava appesa dentro ogni toggle creato così. Scrivendo solo
     * il tipo, il testo resta quello che il campo ha appena salvato.
     */
    fun updateBlockType(block: BlockEntity, newType: BlockType) {
        snapshotForStructuralChange()
        val liftChildren = newType != BlockType.TOGGLE && hasChildren(block.id)
        // Un riquadro, una citazione o del codice su più righe che diventano
        // testo: le righe diventano blocchi (vedi `convertMultiline`).
        val current = _blocks.value.find { it.id == block.id } ?: block
        if (current.type in MULTILINE_TYPES && newType !in MULTILINE_TYPES) {
            viewModelScope.launch {
                structuralEdits.withLock { repository.convertMultiline(block.id, newType) }
                if (newType == BlockType.TOGGLE) {
                    blocksDb.setBlockExpanded(block.id, true)
                    delay(IME_SETTLE_DELAY_MS)
                    _focusRequestBlockId.value = block.id
                }
            }
            return
        }
        viewModelScope.launch {
            // **Un toggle che smette di esserlo lascia uscire i suoi figli.**
            // Solo un toggle mostra le righe che ha dentro: cambiandogli
            // tipo dal "+" (o da "Turn into") restavano appese a una riga
            // che non le fa vedere, e sparivano. Escono subito sotto, come
            // quando il toggle si toglie col backspace.
            if (liftChildren) {
                blocksDb.unnestAndConvert(block.id, newType)
            } else {
                blocksDb.setBlockType(block.id, newType)
            }
            // **Un toggle nuovo nasce aperto.** Lo stato aperto/chiuso sta
            // nel blocco e ci resta anche quando il blocco cambia tipo:
            // una riga che era stata un toggle chiuso, rifatta toggle,
            // rinasceva chiusa — e l'Invio nel titolo, invece di scendere
            // dentro, creava un secondo toggle sotto. Chi crea un toggle
            // ci vuole scrivere dentro subito.
            if (newType == BlockType.TOGGLE && block.type != BlockType.TOGGLE) {
                blocksDb.setBlockExpanded(block.id, true)
            }
            // **Diventato toggle, si continua a scriverne il titolo.** Il
            // toggle è una riga a sé, fuori dal testo condiviso: il campo
            // in cui si stava scrivendo non è più il suo, e senza questa
            // richiesta la tastiera si chiudeva e bisognava ritoccare la
            // riga per cominciare. Su Notion invece si scrive subito.
            // Lo stesso per il riquadro e la citazione (dal 06/10/2026).
            val focusable = newType == BlockType.TOGGLE || newType in MULTILINE_TYPES
            if (focusable && block.type != newType) {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = block.id
            }
        }
    }

    /**
     * **"+" su più righe selezionate** (dal 14/10/2026, chiesto dall'utente):
     * diventano tutte `newType`, ognuna per conto suo e nell'ordine in cui
     * sono — tre paragrafi selezionati fanno tre caselle, o un elenco 1, 2, 3
     * — qualunque font e corpo abbia il testo, che resta com'era. Annulla le
     * rimette com'erano in un passo solo. Con una riga sola è
     * [updateBlockType].
     *
     * Le righe che si selezionano insieme stanno nello stesso testo
     * condiviso (paragrafi, elenchi, caselle), quindi nessuna è scritta su
     * più righe come un riquadro: non c'è niente da dividere. Diventando
     * toggle, riquadro, citazione o codice escono dal testo condiviso, e si
     * continua a scrivere nell'ultima, come con una riga sola.
     */
    fun updateBlockTypes(blocks: List<BlockEntity>, newType: BlockType) {
        if (blocks.size <= 1) {
            blocks.firstOrNull()?.let { updateBlockType(it, newType) }
            return
        }
        snapshotForStructuralChange()
        viewModelScope.launch {
            blocksDb.setBlockTypes(blocks.map { it.id }, newType)
            val focusable = newType == BlockType.TOGGLE || newType in MULTILINE_TYPES
            if (focusable) {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = blocks.last().id
            }
        }
    }

    /**
     * Invio nel titolo di un toggle.
     *
     * Non è una divisione come per un paragrafo, e trattarlo così era
     * il guaio: la riga nuova nasceva **sopra** col testo prima del
     * cursore — cioè col titolo — e il toggle restava vuoto sotto. Il
     * titolo "usciva" dal toggle a ogni Invio.
     *
     * Come su Notion: a toggle **aperto** la riga nuova è il **primo
     * figlio**, dentro; a toggle **chiuso** è un **altro toggle** subito
     * sotto, perché dentro non si vedrebbe. Il testo dopo il cursore, se
     * c'è, passa alla riga nuova; il titolo tiene quello prima.
     */
    fun enterInToggle(toggle: BlockEntity, titleSpans: List<RichTextSpan>, textAfterCursor: String) {
        val current = _blocks.value.find { it.id == toggle.id } ?: toggle
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()
        val newSpansJson = json.encodeToString(listOf(RichTextSpan(text = textAfterCursor)))
        val titleJson = json.encodeToString(titleSpans)
        // La riga nuova si vede subito, come per l'Invio dentro il toggle:
        // vedi `showBeforeSaved`. Il titolo va aggiornato insieme, perché
        // una lettura in ritardo lo riporterebbe col testo di prima
        // dell'Invio.
        showBeforeSaved(newBlockId) { list ->
            val titled = list.map { if (it.id == current.id) it.copy(textJson = titleJson) else it }
            if (current.isExpanded) {
                firstChildInList(titled, current.id, newBlockId, newSpansJson)
            } else {
                siblingBelowInList(titled, current.id, newBlockId, BlockType.TOGGLE, newSpansJson)
            }
        }
        viewModelScope.launch {
            val inserted = structuralEdits.withLock {
                blocksDb.setBlockText(current.id, titleJson)
                if (current.isExpanded) {
                    repository.insertFirstChild(current, newBlockId, newSpansJson)
                    true
                } else {
                    repository.insertSiblingBelow(current, newBlockId, BlockType.TOGGLE, newSpansJson)
                }
            }
            if (!inserted) {
                forgetPendingInsert(newBlockId)
                return@launch
            }
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * "Turn into page" dalle impostazioni di un database dentro questa
     * pagina, o dal menu del blocco: il blocco diventa un collegamento, e
     * si resta qui. Fino al 17/10/2026 il database si apriva anche a
     * schermo intero; l'utente ha chiesto di no: «la trasforma in una
     * pagina e nient'altro, rimango lì dove sono».
     */
    fun turnDatabaseIntoPage(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.turnDatabaseBlockIntoPage(block.id)
        }
    }

    /** Il primo figlio di un toggle aperto e vuoto, dal segnaposto "Empty toggle". */
    fun addFirstChild(toggle: BlockEntity) {
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()
        val emptyJson = json.encodeToString(listOf(RichTextSpan(text = "")))
        showBeforeSaved(newBlockId) { list -> firstChildInList(list, toggle.id, newBlockId, emptyJson) }
        viewModelScope.launch {
            structuralEdits.withLock {
                repository.insertFirstChild(toggle, newBlockId, emptyJson)
            }
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    fun toggleCheckbox(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(block.copy(isChecked = !block.isChecked))
        }
    }

    /**
     * Le quattro cose che si possono fare a un elemento di elenco
     * numerato tenendo premuto sul suo numero, come su OneNote.
     *
     * Prendono l'id e non il blocco di proposito. Chi chiama — un dito
     * sullo schermo — può avere in mano una copia vecchia dell'entità,
     * catturata quando la zona toccabile è stata creata: scrivere
     * quella riporterebbe indietro anche il testo, cancellando ciò che
     * nel frattempo è stato digitato. Per lo stesso motivo i
     * salvataggi toccano **solo la colonna che cambia**.
     */
    fun beginNewListHere(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { blocksDb.setNumberStart(blockId, 1) }
    }

    /** Toglie il "riparti da qui": il conteggio riprende da quello di prima. */
    fun continuePreviousList(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { blocksDb.setNumberStart(blockId, null) }
    }

    /** Toglie il numero: la riga resta, ma come paragrafo normale. */
    fun removeNumber(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { blocksDb.setBlockType(blockId, BlockType.PARAGRAPH) }
    }

    /**
     * La vista di un database appena creato dal menu "/". "Calendar
     * view" deve dare un calendario, non una tabella da cambiare a
     * mano: è l'unica differenza fra quelle voci.
     */
    fun setDatabaseLayout(databasePageId: String, layout: DatabaseLayout) {
        viewModelScope.launch {
            val page = repository.getPage(databasePageId) ?: return@launch
            repository.updatePage(page.copy(databaseLayout = layout))
        }
    }

    /** Da numero a pallino, testo compreso. */
    fun changeToBullet(blockId: String) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch { blocksDb.setBlockType(blockId, BlockType.BULLET_LIST_ITEM) }
    }

    /**
     * **A che numero è arrivato** un elemento di elenco numerato dentro
     * il suo livello: il primo del livello è 1, il secondo 2, e così
     * via. Non l'etichetta da mostrare — quella dipende dal livello e la
     * compone la schermata, che sa come si scrivono i numeri romani, le
     * lettere e "First".
     *
     * Ogni livello conta per conto suo e **riparte da capo** sotto ogni
     * elemento del livello sopra, come su OneNote: `indentLevel` decide
     * la profondità, quindi spostare un elemento a destra o a sinistra
     * lo rinumera da solo, e con lui tutti quelli che seguono.
     *
     * Il conteggio scorre i fratelli dello stesso gruppo in ordine,
     * tenendo un contatore per livello: scendendo di un rientro se ne
     * apre uno nuovo, risalendo quelli più profondi vengono dimenticati
     * (è così che il primo figlio riparte da uno sotto ogni padre).
     * `numberStartsAt` impone il valore all'elemento che lo porta. I
     * blocchi di altro tipo intercalati (un paragrafo in mezzo
     * all'elenco) non interrompono il conteggio — come in Notion.
     */
    /**
     * **I numeri di tutti gli elenchi numerati, in un giro solo** (dal
     * 18/10/2026): id → numero, con le stesse regole di
     * [numberedListOrdinal]. Chiederlo riga per riga rileggeva tutta la
     * pagina per ogni numero, e in una pagina lunga a ogni tasto.
     */
    fun numberedOrdinals(allBlocks: List<BlockEntity>): Map<String, Int> {
        val result = HashMap<String, Int>()
        allBlocks
            .filter { it.type == BlockType.NUMBERED_LIST_ITEM }
            .groupBy { it.parentBlockId }
            .forEach { (_, siblings) ->
                val counters = mutableListOf<Int>()
                for (sibling in siblings.sortedBy { it.orderIndex }) {
                    val level = sibling.indentLevel.coerceIn(0, MAX_INDENT_LEVEL)
                    while (counters.size < level) counters.add(1)
                    if (counters.size > level) {
                        while (counters.size > level + 1) counters.removeAt(counters.size - 1)
                        counters[level] = sibling.numberStartsAt ?: (counters[level] + 1)
                    } else {
                        counters.add(sibling.numberStartsAt ?: 1)
                    }
                    result[sibling.id] = counters[level]
                }
            }
        return result
    }

    fun numberedListOrdinal(allBlocks: List<BlockEntity>, block: BlockEntity): Int {
        val siblings = allBlocks
            .filter { it.parentBlockId == block.parentBlockId && it.type == BlockType.NUMBERED_LIST_ITEM }
            .sortedBy { it.orderIndex }

        val counters = mutableListOf<Int>()
        for (sibling in siblings) {
            val level = sibling.indentLevel.coerceIn(0, MAX_INDENT_LEVEL)
            // Se qualcuno salta un livello (due rientri in un colpo),
            // riempiamo i livelli intermedi con 1: meglio contare da uno
            // che lasciare un buco.
            while (counters.size < level) counters.add(1)
            if (counters.size > level) {
                while (counters.size > level + 1) counters.removeAt(counters.size - 1)
                counters[level] = sibling.numberStartsAt ?: (counters[level] + 1)
            } else {
                counters.add(sibling.numberStartsAt ?: 1)
            }

            if (sibling.id == block.id) return counters[level]
        }
        return 1
    }

    fun toggleExpanded(block: BlockEntity) {
        // Espandere/comprimere un toggle è solo uno stato di
        // visualizzazione, non contenuto — non entra nella cronologia
        // annulla/ripristina.
        //
        // **Solo la colonna dello stato**, e letta dall'ultima versione
        // in memoria: il blocco che arriva qui è quello che la freccia
        // aveva in mano quando è stata disegnata, e salvarlo intero
        // riporterebbe indietro il titolo scritto nel frattempo.
        val current = _blocks.value.find { it.id == block.id } ?: block
        viewModelScope.launch {
            blocksDb.setBlockExpanded(current.id, !current.isExpanded)
        }
    }

    /**
     * Gestisce la pressione di Invio dentro un blocco. Se il blocco è un
     * elemento di lista (puntata, numerata, checkbox) vuoto, Invio esce
     * dalla lista trasformandolo in un paragrafo — come in qualsiasi
     * editor di note. Altrimenti, divide gli span nel punto del cursore:
     * la parte prima (con la sua formattazione intatta) resta in questo
     * blocco, la parte dopo il cursore diventa il testo iniziale
     * (semplice, senza formattazione) di un nuovo blocco creato subito
     * sotto, che eredita lo stesso tipo e lo stesso parentBlockId se si
     * tratta di una lista.
     */
    fun splitBlockAt(
        block: BlockEntity,
        spansBeforeCursor: List<RichTextSpan>,
        textAfterCursor: String,
        // Il testo dopo il cursore già formattato (un incolla coi link); se
        // non torna con [textAfterCursor] non si usa.
        spansAfterCursor: List<RichTextSpan>? = null
    ): SplitOutcome {
        val afterCursor = spansAfterCursor?.takeIf { it.plainText() == textAfterCursor }
            ?: listOf(RichTextSpan(text = textAfterCursor))
        val isListType = block.type == BlockType.BULLET_LIST_ITEM ||
            block.type == BlockType.NUMBERED_LIST_ITEM ||
            block.type == BlockType.CHECKBOX

        val textBeforeCursor = spansBeforeCursor.plainText()

        // **Cintura di sicurezza contro la perdita di testo.**
        //
        // "Invio su una riga vuota esce dalla lista" si decide guardando
        // quello che il campo dice di avere in mano. Se per un attimo
        // quella copia è sbagliata — e con la tastiera vera, che tiene
        // le parole in composizione, succede — una riga piena verrebbe
        // scambiata per vuota e **riscritta a zero**: il testo sparisce
        // e non va nemmeno a capo. È il difetto segnalato il 22/09/2026.
        //
        // Prima di svuotare si chiede quindi al database, che è l'unico
        // a sapere davvero cosa c'è scritto: se lì c'è del testo, non si
        // svuota niente e si divide normalmente.
        val fieldThinksEmpty = textBeforeCursor.isBlank() && textAfterCursor.isBlank()
        if (fieldThinksEmpty && plainTextOf(block).isNotBlank()) {
            // Il campo si crede vuoto ma nel database c'è del testo: è
            // la sua copia a essere indietro. Non si tocca niente e si
            // dice a chi chiama di rimettere il campo com'è davvero.
            return SplitOutcome.NOTHING
        }
        if (isListType && fieldThinksEmpty) {
            snapshotForStructuralChange()
            val spans = listOf(RichTextSpan(text = ""))
            viewModelScope.launch {
                repository.saveBlock(
                    block.copy(type = BlockType.PARAGRAPH, textJson = json.encodeToString(spans))
                )
                // Il blocco cambia specie: da "isola" (una riga tutta
                // sua) a testo scorrevole (dentro il campo condiviso
                // coi vicini). Per la UI sono due posti diversi, quindi
                // il campo di prima viene distrutto e con lui se ne va
                // il cursore — e la tastiera si chiude in faccia a chi
                // stava scrivendo. Lo richiamiamo qui.
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = block.id
            }
            return SplitOutcome.BECAME_PARAGRAPH
        }

        snapshotForStructuralChange()
        val pageId = currentPageId ?: return SplitOutcome.NOTHING
        val newBlockType = if (isListType) block.type else BlockType.PARAGRAPH

        // **La riga nuova nasce sopra, non sotto.**
        //
        // Sembra al contrario ed è il punto di tutta la faccenda. Ogni
        // isola — una casella da spuntare, per esempio — è un campo di
        // testo a sé. Creando la riga nuova sotto bisogna spostarci il
        // cursore, e spostare il cursore fra due campi **stacca e
        // riattacca la tastiera**: nel frattempo i tasti già premuti
        // finiscono nel campo vecchio, la maiuscola automatica si
        // rimette a zero (si vede la lettera passare da minuscola a
        // maiuscola) e il campo appena abbandonato viene distrutto
        // mentre la tastiera gli sta ancora chiedendo dov'è il cursore,
        // che è il modo in cui l'app si chiudeva di colpo.
        //
        // Mettendo invece la riga nuova **sopra** col testo prima del
        // cursore, e lasciando a questo blocco — che il fuoco ce l'ha
        // già — il testo dopo il cursore, sullo schermo non cambia
        // niente ma **il fuoco non si muove**: nessun cambio di campo,
        // nessuna tastiera staccata, niente da distruggere.
        //
        // Vale solo per i blocchi senza figli: se ne avessero,
        // resterebbero attaccati a questo blocco, che dopo la divisione
        // è la riga *di sotto*, e finirebbero sotto la riga sbagliata.
        val hasChildren = _blocks.value.any { it.parentBlockId == block.id }
        // Non per la riga-notifica: la riga di sopra sarebbe un paragrafo
        // col testo della notifica, e la notifica resterebbe vuota sotto.
        // Lì la riga nuova nasce sotto, com'era per tutte le isole.
        if (!hasChildren && block.type != BlockType.NOTIFICATION) {
            val newBlockId = java.util.UUID.randomUUID().toString()
            val textBeforeJson = json.encodeToString(spansBeforeCursor)
            val textAfterJson = json.encodeToString(afterCursor)
            // La riga di sopra si vede nello stesso fotogramma in cui il
            // campo passa al testo dopo il cursore: vedi `showBeforeSaved`.
            showBeforeSaved(newBlockId) { list ->
                splitAboveInList(list, block.id, newBlockId, newBlockType, textBeforeJson, textAfterJson)
            }
            viewModelScope.launch {
                // In coda alle divisioni già in corso, una per volta:
                // due a-capo ravvicinati che calcolano gli indici
                // insieme si pestano i piedi.
                val split = structuralEdits.withLock {
                    repository.splitBlockAbove(
                        blockId = block.id,
                        newBlockId = newBlockId,
                        newBlockType = newBlockType,
                        textBeforeJson = textBeforeJson,
                        textAfterJson = textAfterJson
                    )
                }
                if (!split) forgetPendingInsert(newBlockId)
            }
            return SplitOutcome.SPLIT_ABOVE
        }

        val newIndex = block.orderIndex + 1
        val newBlockId = java.util.UUID.randomUUID().toString()

        viewModelScope.launch {
            val afterSpans = afterCursor
            repository.applyBlockChanges(
                shift = PageRepository.BlockShift(pageId, block.parentBlockId, newIndex, 1),
                saved = listOf(
                    block.copy(textJson = json.encodeToString(spansBeforeCursor)),
                    BlockEntity(
                        id = newBlockId,
                        pageId = pageId,
                        parentBlockId = block.parentBlockId,
                        orderIndex = newIndex,
                        type = newBlockType,
                        textJson = json.encodeToString(afterSpans)
                    )
                )
            )

            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
        return SplitOutcome.SPLIT
    }

    /**
     * Come splitBlockAt, ma per quando il testo arrivato in un colpo solo
     * (tipicamente un incolla) contiene più righe: la prima resta nel
     * blocco corrente, ognuna delle successive diventa un nuovo blocco
     * fratello — tutte ereditando lo stesso tipo se il blocco di partenza
     * era una lista. Incollare tre righe dentro un elenco puntato crea
     * tre punti separati, non un blocco unico con newline dentro.
     */
    fun splitBlockAtMultiple(
        currentBlock: BlockEntity,
        lines: List<String>,
        // Le righe già formattate (un incolla coi link): una per riga, e
        // ognuna si usa solo se torna col suo testo.
        lineSpans: List<List<RichTextSpan>>? = null,
        // Il tipo delle righe incollate, una voce per riga (vedi `PastedLine`):
        // le righe nuove lo prendono, la prima resta questo blocco.
        lineKinds: List<PastedLine?>? = null
    ) {
        fun spansFor(index: Int): List<RichTextSpan> =
            lineSpans?.getOrNull(index)?.takeIf { it.plainText() == lines[index] }
                ?: listOf(RichTextSpan(text = lines[index]))
        if (lines.size <= 1) {
            updateBlockText(currentBlock, lines.firstOrNull() ?: "")
            return
        }

        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        val inheritedType = when (currentBlock.type) {
            BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.CHECKBOX ->
                currentBlock.type
            else -> BlockType.PARAGRAPH
        }
        val newBlockIds = lines.drop(1).map { java.util.UUID.randomUUID().toString() }

        viewModelScope.launch {
            val startIndex = currentBlock.orderIndex + 1
            val firstSpans = spansFor(0)
            repository.applyBlockChanges(
                shift = PageRepository.BlockShift(
                    pageId,
                    currentBlock.parentBlockId,
                    startIndex,
                    lines.size - 1
                ),
                saved = listOf(retypedFirst(currentBlock, lineKinds?.getOrNull(0), lineKinds).copy(textJson = json.encodeToString(firstSpans))) +
                    lines.drop(1).mapIndexed { i, _ ->
                        val kind = lineKinds?.getOrNull(i + 1)
                        val minIndent = lineKinds?.filterNotNull()?.minOfOrNull { it.indent } ?: 0
                        BlockEntity(
                            id = newBlockIds[i],
                            pageId = pageId,
                            parentBlockId = currentBlock.parentBlockId,
                            type = kind?.type ?: inheritedType,
                            isChecked = kind?.checked ?: false,
                            indentLevel = kind?.let { (currentBlock.indentLevel + it.indent - minIndent).coerceIn(0, MAX_INDENT_LEVEL) }
                                ?: 0,
                            textJson = json.encodeToString(spansFor(i + 1)),
                            orderIndex = startIndex + i
                        )
                    }
            )

            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockIds.last()
        }
    }

    /**
     * La riga in cui si incolla, se è tutta e solo una riga copiata col suo
     * tipo (vedi `PastedLine`): lo prende, come nel testo normale. Non il
     * titolo di un toggle, né un riquadro o una citazione, che restano quello
     * che sono.
     */
    private fun retypedFirst(block: BlockEntity, kind: PastedLine?, all: List<PastedLine?>?): BlockEntity {
        if (kind == null || block.type !in RETYPABLE_ON_PASTE) return block
        val minIndent = all?.filterNotNull()?.minOfOrNull { it.indent } ?: 0
        return block.copy(
            type = kind.type,
            isChecked = kind.checked,
            indentLevel = (block.indentLevel + kind.indent - minIndent).coerceIn(0, MAX_INDENT_LEVEL)
        )
    }

    /** Aggiunge un blocco figlio in coda dentro un toggle (es. il primo, se è ancora vuoto). */
    fun addChildBlock(parentBlock: BlockEntity, initialText: String = "") {
        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        val siblingCount = _blocks.value.count { it.parentBlockId == parentBlock.id }
        val newBlockId = java.util.UUID.randomUUID().toString()

        viewModelScope.launch {
            val spans = listOf(RichTextSpan(text = initialText))
            repository.saveBlock(
                BlockEntity(
                    id = newBlockId,
                    pageId = pageId,
                    parentBlockId = parentBlock.id,
                    orderIndex = siblingCount,
                    textJson = json.encodeToString(spans)
                )
            )
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * Trasforma un blocco in divisore E crea subito un paragrafo vuoto
     * appena dopo, mettendoci il focus — un divisore non ha campo di
     * testo, quindi senza questo passaggio in più il blocco appena
     * convertito (specie se era l'ultimo della pagina) lascerebbe
     * l'utente senza nulla su cui continuare a scrivere.
     */
    /**
     * `keepText`, da "Turn into" nel menu del blocco: la riga aveva del
     * testo, e quel testo scende nella riga nuova sotto il divisore invece
     * di restare nascosto dentro il divisore, che non lo mostra. Dal menu
     * "/" la riga è vuota (il comando si è appena tolto) e non serve.
     */
    // --- Le righe "/notification" (dal 25/09/2026) ---

    /** La riga-notifica di cui aprire subito data e ora: appena creata dal menu "/". */
    private val _notificationPickerFor = MutableStateFlow<String?>(null)
    val notificationPickerFor: StateFlow<String?> = _notificationPickerFor

    fun consumeNotificationPicker() {
        _notificationPickerFor.value = null
    }

    /**
     * **La riga diventa una notifica.** Due strade:
     *
     *  - scritta per intero, "/notification 22.10.2026 17:41 Buttare
     *    l'immondizia" e Invio (`command`): resta il solo testo, e sotto
     *    nasce una riga vuota dove si continua a scrivere, come dopo ogni
     *    Invio;
     *  - scelta "Notification" dal menu "/" o da "Turn into" (`command`
     *    null): il testo resta com'è, l'ora è alle prossime 9:00, e si apre
     *    subito la finestra per sceglierla.
     *
     * Un blocco con dei figli li lascia uscire, come diventando divisore:
     * una notifica non ne mostra.
     */
    fun makeNotification(block: BlockEntity, command: NotificationCommand?) {
        snapshotForStructuralChange()
        val zone = AppSettings.zoneId
        val at = (command?.at ?: NotificationCommands.nextDefault(LocalDateTime.now(zone)))
            .atZone(zone).toInstant().toEpochMilli()
        val textJson = command?.let { json.encodeToString(listOf(RichTextSpan(text = it.text))) }
        val newBlockId = if (command != null) java.util.UUID.randomUUID().toString() else null
        val liftChildren = hasChildren(block.id)
        viewModelScope.launch {
            structuralEdits.withLock {
                if (liftChildren) repository.unnestChildren(block.id)
                repository.turnIntoNotification(block.id, at, textJson, newBlockId)
            }
            ReminderAlarms.reschedule()
            if (newBlockId != null) {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = newBlockId
            } else {
                _notificationPickerFor.value = block.id
            }
        }
    }

    /** Scelte nella finestra data e ora della riga-notifica. */
    fun setNotificationTime(block: BlockEntity, at: Long) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            blocksDb.setNotifyAt(block.id, at)
            ReminderAlarms.reschedule()
        }
    }

    fun convertToDividerAndFocusNext(block: BlockEntity, keepText: Boolean = false) {
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()
        val carried = if (keepText && plainTextOf(block).isNotEmpty()) block.textJson else null
        val liftChildren = hasChildren(block.id)

        viewModelScope.launch {
            // In coda alle altre modifiche di struttura, e senza riscrivere
            // il testo: vedi `turnIntoDividerWithLineBelow`.
            structuralEdits.withLock {
                if (liftChildren) repository.unnestChildren(block.id)
                repository.turnIntoDividerWithLineBelow(block.id, newBlockId, carried)
            }
            // **Il campo da cui viene la riga prende quello che c'è adesso**
            // (corretto il 27/10/2026, trovato dall'utente con "---" in mezzo a
            // un elenco): diventata divisore, la riga esce dal suo campo con
            // quelle sotto. Il campo aspettava ancora indietro il testo senza i
            // trattini; sul telefono il database lo salta e arriva già diviso,
            // e il campo, convinto di avere una modifica in viaggio, restava
            // com'era — coi trattini, i numeri di prima e le righe sotto il
            // divisore viste due volte. Come per Annulla (`externalChangeTick`).
            _externalChangeTick.value += 1
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * Indenta un blocco: diventa figlio del fratello immediatamente
     * precedente, in coda ai suoi figli attuali. Se non c'è un fratello
     * precedente (il blocco è già il primo del suo gruppo), non fa nulla
     * — non c'è nulla sotto cui annidarlo.
     */
    /**
     * Indenta un blocco. Per i tipi di testo scorrevole (paragrafo,
     * titoli, elenchi), è solo un numero di livello sul blocco stesso —
     * come Tab in Word, non una vera nidificazione — dato che questi
     * tipi ora vivono in un campo di testo condiviso con i vicini, non
     * più in righe strutturalmente separate. Per le "isole" (oggi solo
     * TOGGLE) resta il vecchio comportamento: diventa figlio del
     * fratello immediatamente precedente.
     */
    fun indentBlock(block: BlockEntity) {
        if (isFlowingTextType(block.type)) {
            snapshotForStructuralChange()
            viewModelScope.launch {
                repository.saveBlock(block.copy(indentLevel = (block.indentLevel + 1).coerceAtMost(MAX_INDENT_LEVEL)))
            }
            return
        }

        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val currentPos = siblings.indexOfFirst { it.id == block.id }
        if (currentPos <= 0) return
        val newParent = siblings[currentPos - 1]

        snapshotForStructuralChange()
        val newParentChildren = _blocks.value.filter { it.parentBlockId == newParent.id }
        val newOrderIndex = (newParentChildren.maxOfOrNull { it.orderIndex } ?: -1) + 1

        viewModelScope.launch {
            repository.saveBlock(block.copy(parentBlockId = newParent.id, orderIndex = newOrderIndex))
            // Indentare sposta il blocco da "cima pagina" a "dentro un
            // altro blocco" — posizioni strutturalmente diverse nella UI,
            // quindi Compose ricrea il campo di testo da zero perdendo il
            // focus, e con esso la tastiera si chiude da sola. Richiedere
            // di nuovo il focus qui lo riporta su.
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = block.id
        }
    }

    /** Come indentBlock ma al contrario. Per i tipi di testo scorrevole, decrementa il livello (minimo 0). */
    fun outdentBlock(block: BlockEntity) {
        if (isFlowingTextType(block.type)) {
            if (block.indentLevel <= 0) return
            snapshotForStructuralChange()
            viewModelScope.launch {
                repository.saveBlock(block.copy(indentLevel = block.indentLevel - 1))
            }
            return
        }

        val currentParentId = block.parentBlockId ?: return
        val currentParent = _blocks.value.find { it.id == currentParentId } ?: return
        // Da una colonna non si esce col rientro: finirebbe fra le colonne, dove non si vede.
        if (currentParent.type == BlockType.COLUMN) return
        val newParentId = currentParent.parentBlockId
        val insertAtIndex = currentParent.orderIndex + 1

        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        viewModelScope.launch {
            // Stessa transazione unica della divisione: vedi
            // `BlockDao.shiftOrderIndexes`. Qui il blocco che si sposta
            // è "nuovo" per il gruppo che lo accoglie, quindi entra fra
            // i blocchi da inserire e non fra quelli da aggiornare.
            repository.applyBlockChanges(
                shift = PageRepository.BlockShift(pageId, newParentId, insertAtIndex, 1),
                saved = listOf(
                    block.copy(parentBlockId = newParentId, orderIndex = insertAtIndex)
                )
            )
            // Stesso motivo di indentBlock: disindentare sposta il
            // blocco tra due posizioni strutturalmente diverse nella UI,
            // il campo di testo viene ricreato senza focus e la tastiera
            // si chiude da sola. Richiediamo di nuovo il focus qui.
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = block.id
        }
    }

    /** Scambia la posizione di un blocco con il fratello immediatamente precedente. */
    fun moveBlockUp(block: BlockEntity) = moveBlock(block, up = true)

    /** Scambia la posizione di un blocco con il fratello immediatamente successivo. */
    fun moveBlockDown(block: BlockEntity) = moveBlock(block, up = false)

    /**
     * **Le frecce della barra** (rifatte il 28/09/2026, dopo due bug trovati
     * dall'utente: "sembra che cancelli una riga in basso", e sopra un
     * database "sembra che non sposta niente").
     *
     * - Lo spostamento aspetta in fila le scritture di prima
     *   (`structuralEdits`, come l'Invio) e cambia nel database **solo le
     *   posizioni**: vedi `PageRepository.moveBlock`.
     * - Il campo di testo condiviso deve prendere il nuovo ordine anche se
     *   ha una sua scrittura in viaggio (`externalChangeTick`, come per
     *   Annulla): altrimenti restava col vecchio, e la lettera dopo finiva
     *   nella riga sbagliata, cancellandone il testo.
     * - **Il cursore segue la riga**, nello stesso punto. Prima restava
     *   all'altezza di prima, cioè sulla riga scambiata: sopra un database la
     *   riga saliva ma il cursore restava sotto, e il tocco seguente spostava
     *   un'altra riga. Il fuoco si chiede quando la lista mostrata ha già il
     *   nuovo ordine, se no lo prenderebbe il campo di prima.
     */
    private fun moveBlock(block: BlockEntity, up: Boolean) {
        val siblings = _blocks.value.filter { it.parentBlockId == block.parentBlockId }
        val position = siblings.indexOfFirst { it.id == block.id }
        if (position < 0 || (up && position == 0) || (!up && position == siblings.lastIndex)) return
        val caret = caretInLine(block.id)

        snapshotForStructuralChange()
        viewModelScope.launch {
            val passedId = structuralEdits.withLock { repository.moveBlock(block.id, up) } ?: return@launch
            _externalChangeTick.value += 1
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                _blocks.first { shown ->
                    val at = shown.indexOfFirst { it.id == block.id }
                    val passedAt = shown.indexOfFirst { it.id == passedId }
                    at >= 0 && passedAt >= 0 && (at < passedAt) == up
                }
            }
            if (caret != null) _pendingCaret.value = block.id to caret
            _focusRequestBlockId.value = block.id
            _movedBlock.value = MovedBlock(block.id, System.nanoTime() / 1_000_000)
        }
    }

    /**
     * **La riga appena spostata con le frecce, che si illumina un attimo**
     * (dal 29/09/2026). Una riga vuota spostata non si vede: fra righe
     * vuote sembrava che non succedesse niente, e passando un divisore che
     * fosse il divisore a salire mangiandosi una riga (l'utente). Il
     * momento distingue due spostamenti di fila della stessa riga.
     */
    data class MovedBlock(val blockId: String, val at: Long)

    private val _movedBlock = MutableStateFlow<MovedBlock?>(null)
    val movedBlock: StateFlow<MovedBlock?> = _movedBlock

    /**
     * **"Add above" e "Add below"** del menu dei sei puntini (dal 29/09/2026,
     * chiesto dall'utente): una riga vuota sopra o sotto qualunque blocco —
     * testo, pagina, database — e il cursore dentro, pronto a scrivere. In
     * fila dopo le scritture di prima, come l'Invio; il fuoco si chiede
     * quando la riga c'è già nella lista mostrata, se no nessun campo la
     * troverebbe.
     */
    fun addLineNextTo(block: BlockEntity, below: Boolean) {
        snapshotForStructuralChange()
        val newBlockId = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            val added = structuralEdits.withLock {
                // La riga vuota di una pagina nuova non è ancora nel database.
                placeholder?.takeIf { it.id == block.id }?.let { repository.saveBlockIfAbsent(it) }
                repository.insertLineNextTo(block.id, newBlockId, below, EMPTY_TEXT_JSON)
            }
            if (!added) return@launch
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                _blocks.first { shown -> shown.any { it.id == newBlockId } }
            }
            _focusRequestBlockId.value = newBlockId
        }
    }

    /**
     * **Invio su una riga vuota in fondo a un riquadro o a una citazione**
     * (dal 06/10/2026): si esce. La riga vuota se ne va (`kept` è il testo
     * senza di lei) e sotto nasce una riga di testo col cursore dentro. Gli
     * altri Invio vanno a capo dentro il blocco, che si allunga.
     */
    fun leaveMultilineBlock(block: BlockEntity, kept: List<RichTextSpan>) {
        updateBlockSpans(block, kept)
        addLineNextTo(block, below = true)
    }

    /**
     * Cambia le voci in più di un blocco (`BlockAttrs`): l'icona o il colore
     * di un riquadro, la lingua del codice, il file di un'immagine… Si può
     * annullare come ogni altra modifica.
     */
    fun updateBlockAttrs(blockId: String, change: (BlockAttrs) -> BlockAttrs) {
        snapshotForStructuralChange()
        viewModelScope.launch { repository.updateBlockAttrs(blockId, change) }
    }

    /**
     * **Il pin di una riga**, dal menu dei sei puntini (dal 20/10/2026): la
     * riga diventa un punto di arrivo dei collegamenti dentro la pagina (vedi
     * `BlockLinks.kt`). Si può annullare.
     */
    fun setLinkTarget(block: BlockEntity, pinned: Boolean) {
        updateBlockAttrs(block.id) { it.copy(linkTarget = pinned) }
    }

    /** Le righe a cui si può collegare del testo, nell'ordine della pagina: vedi `linkTargetsOf`. */
    fun linkTargets(): List<LinkTarget> = linkTargetsOf(_blocks.value) { spansOf(it) }

    /** La riga arrivata con un collegamento si illumina un attimo, come una riga spostata. */
    fun highlightBlock(blockId: String) {
        _movedBlock.value = MovedBlock(blockId, System.nanoTime() / 1_000_000)
    }

    /**
     * **Il Web Clipper** (dal 20/10/2026): la foto di un sito (o il suo
     * segnalibro) al posto della prima riga vuota della pagina, e se non ce
     * n'è in fondo (vedi `PageRepository.insertWebClip`); sotto, le righe di
     * `captions`. Si può annullare. Quando la pagina lo mostra, `onPlaced`
     * riceve l'id del blocco, e il blocco si illumina.
     */
    fun insertWebClip(type: BlockType, attrs: BlockAttrs, captions: List<List<RichTextSpan>>, onPlaced: (String) -> Unit = {}) {
        insertWebClipBlocks(listOf(NewBlock(type = type, attrs = attrs)) + captions.map { NewBlock(spans = it) }, onPlaced)
    }

    /**
     * Come `insertWebClip`, per più blocchi in fila: "Text and Images" (dal
     * 20/10/2026), il titolo, la nota, "Clipped from" e poi l'articolo, con le
     * sue immagini. Tutto in un passo di Annulla.
     */
    fun insertWebClipBlocks(blocks: List<NewBlock>, onPlaced: (String) -> Unit = {}) {
        val pageId = currentPageId ?: return
        if (blocks.isEmpty()) return
        val firstType = blocks.first().type
        snapshotForStructuralChange()
        viewModelScope.launch {
            val placedId = structuralEdits.withLock { repository.insertAtFirstEmptyLine(pageId, blocks) } ?: return@launch
            _externalChangeTick.value += 1
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS * 4) {
                _blocks.first { shown -> shown.any { it.id == placedId && it.type == firstType } }
            }
            _movedBlock.value = MovedBlock(placedId, System.nanoTime() / 1_000_000)
            _revealBlock.value = placedId
            onPlaced(placedId)
        }
    }

    private val _revealBlock = MutableStateFlow<String?>(null)

    /** Un blocco appena arrivato da portare in vista (la foto del Web Clipper): la pagina scorre fin lì, poi `consumeReveal`. */
    val revealBlock: StateFlow<String?> = _revealBlock

    fun consumeReveal() {
        _revealBlock.value = null
    }

    /** I posti dove "Move to line…" può mettere un blocco: vedi `moveTargetsFor`. */
    fun moveTargets(block: BlockEntity): List<PageLine> = _blocks.value.moveTargetsFor(block.id)

    /**
     * **"Move to line…"** (dal 20/10/2026): il blocco va subito sotto la riga
     * `afterId`, o in cima alla pagina con null (vedi
     * `PageRepository.moveBlockAfter`). Si può annullare; arrivato, si illumina.
     */
    fun moveBlockAfter(block: BlockEntity, afterId: String?) = moveBlockBy(block) { repository.moveBlockAfter(block.id, afterId) }

    /** Trascinato coi sei puntini (dal 12/11/2026): subito prima o dopo [targetId], anche dentro una colonna (vedi `PageRepository.moveBlockNextTo`). */
    fun moveBlockNextTo(block: BlockEntity, targetId: String, after: Boolean) =
        moveBlockBy(block) { repository.moveBlockNextTo(block.id, targetId, after) }

    /** Trascinato coi sei puntini sul bordo di [targetId]: le colonne (vedi `PageRepository.moveBlockBeside`). */
    fun moveBlockBeside(block: BlockEntity, targetId: String, right: Boolean) =
        moveBlockBy(block) { repository.moveBlockBeside(block.id, targetId, right) }

    /**
     * **I file trascinati da fuori** (dal 13/11/2026): vedi
     * `PageRepository.insertDroppedBlocks`. Un passo di Annulla; arrivati, il
     * primo si illumina.
     */
    fun insertDroppedBlocks(blocks: List<NewBlock>, targetId: String?, beside: Boolean, after: Boolean) {
        val pageId = currentPageId ?: return
        if (blocks.isEmpty()) return
        snapshotForStructuralChange()
        viewModelScope.launch {
            val first = structuralEdits.withLock { repository.insertDroppedBlocks(pageId, blocks, targetId, beside, after) } ?: return@launch
            _externalChangeTick.value += 1
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS * 4) { _blocks.first { shown -> shown.any { it.id == first } } }
            _movedBlock.value = MovedBlock(first, System.nanoTime() / 1_000_000)
        }
    }

    /**
     * **La larghezza delle colonne** trascinata col mouse (dal 13/11/2026,
     * vedi `ColumnEdges.kt`): la parte di ogni colonna, in un passo di Annulla.
     */
    fun setColumnWeights(weights: List<Pair<String, Float>>) {
        snapshotForStructuralChange()
        viewModelScope.launch { weights.forEach { (id, part) -> repository.updateBlockAttrs(id) { it.copy(width = part) } } }
    }

    /** Uno spostamento ([move]) che si può annullare: arrivato, il blocco si illumina. */
    private fun moveBlockBy(block: BlockEntity, move: suspend () -> Boolean) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            val moved = structuralEdits.withLock { move() }
            if (!moved) return@launch
            _externalChangeTick.value += 1
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                _blocks.first { shown ->
                    val now = shown.find { it.id == block.id }
                    now != null && (now.parentBlockId != block.parentBlockId || now.orderIndex != block.orderIndex)
                }
            }
            _movedBlock.value = MovedBlock(block.id, System.nanoTime() / 1_000_000)
        }
    }

    /**
     * Come `updateBlockAttrs`, ma **senza un passo di Annulla**: per quello
     * che l'app aggiunge da sola, come l'anteprima di un segnalibro arrivata
     * dalla rete. Annullando si tornerebbe al segnalibro "spoglio" di un
     * attimo prima, che nessuno ha scritto.
     */
    fun fillBlockAttrs(blockId: String, change: (BlockAttrs) -> BlockAttrs) {
        viewModelScope.launch { repository.updateBlockAttrs(blockId, change) }
    }

    /** Dove sta il cursore dentro la riga `blockId`, se è lì che si sta scrivendo. */
    private fun caretInLine(blockId: String): Int? {
        val selection = liveSelection ?: return null
        val line = selection.blockIds.indexOf(blockId)
        if (line < 0) return null
        val byId = _blocks.value.associateBy { it.id }
        val lineStart = selection.blockIds.take(line).sumOf { id -> (byId[id]?.let { plainTextOf(it).length } ?: 0) + 1 }
        return (selection.end - lineStart).coerceAtLeast(0)
    }

    fun deleteBlock(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            // In coda alle scritture di prima; poi il campo di testo da cui la riga
            // se ne va prende le righe nuove anche con una scrittura sua in viaggio.
            structuralEdits.withLock { repository.deleteBlock(block) }
            _externalChangeTick.value += 1
        }
    }

    // --- Tutta la pagina selezionata, come su Notion (dal 20/10/2026) ---

    /** Una riga della pagina copiata intera: il testo col suo stile, e il suo tipo se è una riga da ritrovare uguale. */
    data class PageCopyLine(val spans: List<RichTextSpan>, val kind: CopiedLine?)

    /** I blocchi che "Cut" e "Delete" della pagina intera lasciano: sottopagine e database vivono anche altrove. */
    private val keptByPageDelete = setOf(BlockType.PAGE_LINK, BlockType.DATABASE_LINK)

    /**
     * **Tutta la pagina da copiare**, nell'ordine in cui si legge: ogni riga
     * col suo stile, e per le righe di testo, gli elenchi e le caselle anche
     * il tipo e il rientro, così incollate in notE tornano uguali. Riquadri,
     * citazioni e codice danno una riga per ognuna delle loro; una tabella
     * una riga per riga, con le celle separate da una tabulazione (come la
     * copia un foglio di calcolo); un divisore "---". Sottopagine e database
     * non hanno testo da copiare.
     *
     * Con [only] (dal 10/11/2026, i blocchi selezionati col mouse) solo quei
     * blocchi di primo livello, con quello che contengono.
     */
    suspend fun pageCopyLines(only: Set<String>? = null): List<PageCopyLine> {
        val children = _blocks.value.groupBy { it.parentBlockId }.mapValues { (_, list) -> list.sortedBy { it.orderIndex } }
        val lines = mutableListOf<PageCopyLine>()
        suspend fun visit(parentId: String?, depth: Int) {
            for (block in children[parentId].orEmpty()) {
                if (parentId == null && only != null && block.id !in only) continue
                when (block.type) {
                    BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM, BlockType.CHECKBOX ->
                        lines += PageCopyLine(spansOf(block), CopiedLine(block.type, block.isChecked, (block.indentLevel + depth).coerceIn(0, 10)))
                    BlockType.TABLE -> {
                        val cells = repository.getTableCells(block.id).first().associate { (it.rowIndex to it.colIndex) to it.text }
                        for (r in 0 until block.tableRows) {
                            val row = (0 until block.tableCols).joinToString("\t") { c -> cells[r to c].orEmpty().replace('\n', ' ') }
                            lines += PageCopyLine(listOf(RichTextSpan(text = row)), null)
                        }
                    }
                    BlockType.DIVIDER -> lines += PageCopyLine(listOf(RichTextSpan(text = "---")), null)
                    BlockType.PAGE_LINK, BlockType.DATABASE_LINK -> Unit
                    // Le colonne (dal 12/11/2026) sono cornici: le loro righe, una colonna dopo l'altra, senza rientro.
                    BlockType.COLUMN_LIST, BlockType.COLUMN -> {
                        visit(block.id, depth)
                        continue
                    }
                    in MEDIA_TYPES -> {
                        val media = BlockAttrs.parse(block.attrsJson)
                        (media.url ?: media.name)?.let { text -> lines += PageCopyLine(listOf(RichTextSpan(text = text)), null) }
                    }
                    else -> spansOf(block).splitLines().forEach { lines += PageCopyLine(it, null) }
                }
                visit(block.id, depth + 1)
            }
        }
        visit(null, 0)
        return lines
    }

    /**
     * **"Cut" e "Delete" della pagina intera**: tutto quello che c'è nella
     * pagina se ne va, tranne le sottopagine e i database (e i toggle che li
     * contengono), che si tolgono uno alla volta dal loro menu. Si può annullare.
     * Con [only], solo quei blocchi di primo livello e quello che contengono.
     */
    fun deletePageContent(only: Set<String>? = null) {
        val byId = _blocks.value.associateBy { it.id }
        fun topOf(block: BlockEntity): String {
            var current = block
            while (true) current = byId[current.parentBlockId ?: return current.id] ?: return current.id
        }
        val all = if (only == null) _blocks.value else _blocks.value.filter { topOf(it) in only }
        // Chi contiene una sottopagina o un database resta, per non portarseli via.
        val holders = HashSet<String>()
        for (block in all) {
            if (block.type !in keptByPageDelete) continue
            var parent = block.parentBlockId
            while (parent != null && holders.add(parent)) parent = byId[parent]?.parentBlockId
        }
        val doomed = all.filter { it.type !in keptByPageDelete && it.id !in holders }
        if (doomed.isEmpty()) return
        snapshotForStructuralChange()
        viewModelScope.launch { structuralEdits.withLock { repository.applyBlockChanges(deleted = doomed) } }
    }

    /**
     * Elimina un database mostrato dentro la pagina: sia il blocco sia
     * la pagina-database che contiene, con le sue colonne e le sue
     * righe (se ne occupa il database, a cascata).
     *
     * Qui si cancella tutto, a differenza di un collegamento a pagina,
     * dove togliere il blocco lascia la pagina al suo posto. La
     * differenza è che lì il blocco è un rimando a qualcosa che vive
     * per conto suo, mentre un database incorporato **è** quel
     * contenuto: lasciarne i dati vorrebbe dire lasciarli in un posto
     * da cui non si può più arrivare.
     */
    fun deleteDatabaseBlock(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            block.linkedPageId?.let { linkedId ->
                repository.getPage(linkedId)?.let { repository.deletePage(it) }
            }
            repository.deleteBlock(block)
        }
    }

    /**
     * Elimina un blocco e sposta il focus sul fratello precedente, se
     * esiste — chiamato dal pulsante "elimina blocco vuoto" nella barra
     * (prima intercettava il tasto Backspace direttamente sul campo di
     * testo, ma su alcune tastiere — Samsung inclusa — quell'intercettazione
     * disturbava la connessione con l'input).
     */
    fun deleteBlockAndFocusPrevious(block: BlockEntity) {
        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val currentPos = siblings.indexOfFirst { it.id == block.id }
        // Solo tra i fratelli PRIMA di questa posizione, e solo quelli
        // con un campo di testo vero (non divisori/tabelle/link, che
        // non hanno nulla su cui il focus possa atterrare).
        val previous = siblings
            .subList(0, currentPos.coerceAtLeast(0))
            .lastOrNull { isFocusableBlockType(it.type) }

        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.deleteBlock(block)
            if (previous != null) {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = previous.id
            }
        }
    }

    /**
     * Porta il cursore in fondo alla pagina — chiamato toccando lo
     * spazio vuoto sotto l'ultimo blocco, così tutta l'area dal titolo
     * in giù è scrivibile e non solo i confini esatti dei blocchi.
     *
     * Conta l'**ultimo** blocco, non l'ultimo scrivibile: se la pagina
     * finisce con qualcosa su cui il cursore non può atterrare (un
     * database, un divisore, una tabella, un collegamento) va creato un
     * paragrafo nuovo **dopo** di lui. Cercare l'ultimo scrivibile
     * riportava invece a scrivere sopra quell'elemento, che è l'opposto
     * di quello che si intende toccando sotto.
     */
    fun focusLastBlock() {
        // **Con righe vuote in fondo, sulla prima di loro** (dal 30/09/2026):
        // in una pagina vuota o nuova è la prima riga della pagina, non
        // l'ultima con tutto il vuoto sopra ("non ci sia uno spazio vuoto
        // sopra inutilissimo"). Vedi `trailingBlankLines`.
        trailingBlankLines().firstOrNull()?.let { first ->
            viewModelScope.launch {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = first.id
            }
            return
        }

        val topLevel = _blocks.value
            .filter { it.parentBlockId == null }
            .sortedBy { it.orderIndex }
        val last = topLevel.lastOrNull()

        // Sotto un riquadro, una citazione o del codice si scrive su una riga
        // nuova, come su Notion: rientrarci dentro non è quello che si
        // intende toccando sotto.
        if (last != null && isFocusableBlockType(last.type) && last.type !in MULTILINE_TYPES) {
            viewModelScope.launch {
                delay(IME_SETTLE_DELAY_MS)
                _focusRequestBlockId.value = last.id
            }
            return
        }

        // L'ultimo blocco non accetta il cursore (un database, un
        // divisore, una tabella, un collegamento): ne creiamo uno nuovo
        // dopo di lui, che è dove si intende scrivere toccando sotto.
        snapshotForStructuralChange()
        val pageId = currentPageId ?: return
        val newIndex = (topLevel.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val newBlockId = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.saveBlock(BlockEntity(id = newBlockId, pageId = pageId, orderIndex = newIndex))
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = newBlockId
        }
    }

    fun consumeFocusRequest() {
        _focusRequestBlockId.value = null
    }

    /**
     * **Le righe vuote in fondo alla pagina**: i paragrafi senza testo (e
     * senza niente dentro) dopo l'ultimo blocco che ha qualcosa. In una
     * pagina vuota o nuova sono tutte le sue righe. `localText` dà il testo
     * delle righe che il campo ha appena cambiato e il database non ancora.
     */
    fun trailingBlankLines(localText: Map<String, String> = emptyMap()): List<BlockEntity> {
        val all = _blocks.value
        return all
            .filter { it.parentBlockId == null }
            .sortedBy { it.orderIndex }
            .takeLastWhile { block ->
                block.type == BlockType.PARAGRAPH &&
                    (localText[block.id] ?: plainTextOf(block)).isEmpty() &&
                    all.none { it.parentBlockId == block.id }
            }
    }

    /**
     * **Un tocco su una riga vuota in fondo alla pagina** (dal 30/09/2026,
     * "in un campo vuoto o nuovo, se premo in un punto a caso della pagina
     * il text cursor mi vada al primissimo blocco"): il cursore va sulla
     * prima delle righe vuote in fondo, così sopra non resta del vuoto. È
     * l'indice di quella riga fra `runBlockIds` (le righe del campo, coi
     * loro testi di adesso in `lines`), o null se il tocco va bene dov'è:
     * sulla prima di loro, su una riga col testo, o su una riga vuota in
     * mezzo al testo, che è uno spazio voluto.
     */
    fun trailingBlankRedirect(runBlockIds: List<String>, lines: List<String>, lineIndex: Int): Int? {
        val tapped = runBlockIds.getOrNull(lineIndex) ?: return null
        val localText = if (lines.size == runBlockIds.size) runBlockIds.zip(lines).toMap() else emptyMap()
        val trailing = trailingBlankLines(localText)
        val first = trailing.firstOrNull() ?: return null
        if (tapped == first.id || trailing.none { it.id == tapped }) return null
        return runBlockIds.indexOf(first.id).takeIf { it >= 0 }
    }

    /**
     * I tipi di blocco senza un campo di testo editabile — richiedere il
     * focus su uno di questi manderebbe in crash la UI (nessun
     * FocusRequester collegato). Usato da focusLastBlock e
     * deleteBlockAndFocusPrevious per scegliere sempre un blocco valido.
     */
    private fun isFocusableBlockType(type: BlockType): Boolean =
        type != BlockType.DIVIDER &&
            type != BlockType.SYNCED &&
            type != BlockType.COLUMN_LIST &&
            type != BlockType.COLUMN &&
            type != BlockType.TABLE &&
            type != BlockType.PAGE_LINK &&
            type != BlockType.DATABASE_LINK &&
            type != BlockType.PAGE_MENTION &&
            type != BlockType.BUTTON &&
            type !in MEDIA_TYPES

    /**
     * I tipi "di testo scorrevole": vivono uniti in un unico campo di
     * testo condiviso con i vicini consecutivi dello stesso gruppo,
     * invece che ciascuno nel proprio campo separato — è quello che
     * rende selezione e backspace-tra-blocchi naturali. Tutti gli altri
     * tipi restano "isole" separate, invariate.
     */
    /**
     * I tipi che vivono dentro il campo di testo condiviso.
     *
     * **Deve restare uguale a `FLOWING_TYPES` nella schermata.** Sono
     * due elenchi perché uno decide cosa disegnare e l'altro cosa
     * salvare, ma se divergono il campo mostra righe che il ViewModel
     * non riconosce come sue e le modifiche spariscono in silenzio: è
     * successo mettendoci le caselle da spuntare e aggiornando solo
     * l'elenco della schermata.
     */
    fun isFlowingTextType(type: BlockType): Boolean =
        type == BlockType.PARAGRAPH ||
            type == BlockType.BULLET_LIST_ITEM ||
            type == BlockType.NUMBERED_LIST_ITEM ||
            type == BlockType.CHECKBOX

    /**
     * Applica a un gruppo di blocchi di testo scorrevole (mostrati
     * uniti in un unico campo nell'editor) la stessa modifica subita
     * dal testo combinato, passando dalle righe attuali a newLines.
     * Confronta quante righe iniziali e finali sono rimaste identiche
     * per isolare la porzione davvero cambiata (di solito pochissime
     * righe, anche in un gruppo lungo), poi:
     *  - stesso numero di righe coinvolte quanto blocchi: aggiorna
     *    solo il testo di ciascuno (digitazione normale)
     *  - più righe che blocchi: le righe in eccesso diventano nuovi
     *    blocchi (Invio premuto, o incolla multi-riga)
     *  - meno righe che blocchi: i blocchi in eccesso vengono
     *    eliminati, il loro contenuto è già confluito nelle righe
     *    superstiti (backspace che unisce due righe, o una selezione
     *    a cavallo di più blocchi cancellata) — è esattamente questo
     *    terzo caso a far funzionare backspace-tra-blocchi in modo
     *    nativo, senza bisogno di intercettare nessun tasto: è solo
     *    una conseguenza di come il testo è cambiato, rilevata allo
     *    stesso modo affidabile con cui rileviamo qualsiasi modifica.
     */
    /**
     * Applica una formattazione (grassetto/corsivo/...) a un intervallo
     * del campo di testo unito, traducendolo nei corrispondenti
     * intervalli sui singoli blocchi che lo compongono: la selezione
     * dell'utente è una sola, ma può attraversare più blocchi, ognuno
     * dei quali conserva i propri span separatamente.
     */
    fun applyFormatToRun(runBlockIds: List<String>, selStart: Int, selEnd: Int, type: FormatType) {
        if (selStart >= selEnd) return
        val all = _blocks.value
        val runBlocks = runBlockIds.mapNotNull { id -> all.find { it.id == id } }
        if (runBlocks.isEmpty()) return

        snapshotForStructuralChange()

        var pos = 0
        val updates = mutableListOf<BlockEntity>()
        for (block in runBlocks) {
            val text = plainTextOf(block)
            val blockStart = pos
            val blockEnd = pos + text.length
            pos = blockEnd + 1 // +1 per il newline che separa i blocchi

            val overlapStart = maxOf(blockStart, selStart) - blockStart
            val overlapEnd = minOf(blockEnd, selEnd) - blockStart
            if (overlapStart >= overlapEnd) continue

            val spans = spansOf(block)
            val newSpans = when (type) {
                FormatType.BOLD -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.bold }) { s, v -> s.copy(bold = v) }
                FormatType.ITALIC -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.italic }) { s, v -> s.copy(italic = v) }
                FormatType.UNDERLINE -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.underline }) { s, v -> s.copy(underline = v) }
                FormatType.STRIKETHROUGH -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.strikethrough }) { s, v -> s.copy(strikethrough = v) }
                FormatType.SPOILER -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.spoiler }) { s, v -> s.copy(spoiler = v) }
                FormatType.SUBSCRIPT -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.subscript }) { s, v -> s.withSubscript(v) }
                FormatType.SUPERSCRIPT -> toggleFormatInRange(spans, overlapStart, overlapEnd, { it.superscript }) { s, v -> s.withSuperscript(v) }
            }
            updates.add(block.copy(textJson = json.encodeToString(newSpans)))
        }

        viewModelScope.launch {
            updates.forEach { repository.saveBlock(it) }
        }
    }

    /**
     * Il gruppo di testo scorrevole reale a cui appartengono gli id
     * indicati. La lista di id arriva dalla UI, che può essere indietro
     * di un istante rispetto allo stato reale (es. si scrive subito
     * dopo un Invio, prima che la ricomposizione abbia aggiornato il
     * gruppo). Invece di fidarcene — che faceva sparire la modifica in
     * silenzio, ed è la ragione per cui l'Invio e la continuazione
     * delle liste sembravano "saltare" — ricostruiamo il gruppo dallo
     * stato corrente: la sequenza consecutiva di blocchi di testo
     * scorrevole che contiene il primo blocco ancora esistente tra
     * quelli indicati.
     */
    private fun resolveRun(runBlockIds: List<String>, all: List<BlockEntity> = _blocks.value): List<BlockEntity>? {
        val byId = all.associateBy { it.id }
        val anchor = runBlockIds.firstNotNullOfOrNull { id -> byId[id] } ?: return null
        val siblings = all
            .filter { it.parentBlockId == anchor.parentBlockId }
            .sortedBy { it.orderIndex }
        val anchorPos = siblings.indexOfFirst { it.id == anchor.id }
        if (anchorPos < 0) return null
        // **Il gruppo si ferma ai confini dei pezzi** (dal 18/10/2026), gli
        // stessi che vede la schermata: vedi `chunkMembers`.
        // E a quelli dei gruppi con uno spazio fra i blocchi tutto loro
        // (dal 21/10/2026): anche quelli sono un campo a sé.
        val starts = if (anchor.parentBlockId == null) {
            chunkStartsOf(siblings).keys + spacingStartsOf(siblings).keys
        } else {
            emptySet()
        }

        var start = anchorPos
        while (start > 0 && siblings[start].id !in starts && isFlowingTextType(siblings[start - 1].type)) start--
        var endExclusive = anchorPos
        while (endExclusive < siblings.size &&
            isFlowingTextType(siblings[endExclusive].type) &&
            (endExclusive == anchorPos || siblings[endExclusive].id !in starts)
        ) endExclusive++
        return siblings.subList(start, endExclusive).ifEmpty { null }
    }

    /**
     * Backspace premuto a inizio di una riga del testo unito. Invece di
     * fondere subito la riga con quella sopra, il gesto la "smonta" un
     * gradino alla volta, come in Notion:
     *  1. se è un elemento di elenco, ne toglie il marcatore — e il testo
     *     **resta dov'era**, allineato agli altri elementi (dal 18/10/2026,
     *     come su Word, vedi `BlockAttrs.listIndent`); un altro backspace lo
     *     porta a sinistra;
     *  2. altrimenti, se è rientrata, le toglie un rientro — e si può
     *     continuare, un livello per volta, fino al margine;
     *  3. quando non c'è più niente da togliere restituisce false, e la
     *     fusione con la riga sopra avviene normalmente.
     *
     * Restituisce true quando ha fatto qualcosa: è il segnale al campo
     * che la fusione non deve avvenire e che il testo va rimesso com'era.
     * La decisione sta qui e non nella UI perché solo qui il gruppo di
     * blocchi è quello aggiornato (vedi resolveRun). I salvataggi
     * toccano solo la colonna che cambia: scrivere l'intera entità
     * riporterebbe indietro anche il testo, se la copia in memoria
     * fosse più vecchia di quello che si sta scrivendo.
     */
    fun backspaceAtLineStart(runBlockIds: List<String>, lineIndex: Int): Boolean =
        backspaceAtLineStartDetailed(runBlockIds, lineIndex) != BackspaceOutcome.NOTHING

    /**
     * Come sopra, ma dice **cosa** ha fatto.
     *
     * A chi chiama serve distinguere un gradino tolto (il campo resta
     * dov'è) da una fusione con l'isola di sopra: lì questa riga sparisce
     * e con lei il campo di testo che sta chiamando. Un campo distrutto
     * mentre ha il fuoco fa chiudere l'app, quindi prima il fuoco va
     * posato altrove — vedi "il posteggio della tastiera".
     */
    fun backspaceAtLineStartDetailed(
        runBlockIds: List<String>,
        lineIndex: Int
    ): BackspaceOutcome {
        val block = resolveRun(runBlockIds)?.getOrNull(lineIndex)
            ?: return BackspaceOutcome.NOTHING

        when {
            // Il numero o il pallino se ne vanno, il testo resta al suo posto.
            block.type in LIST_INDENT_TYPES -> {
                snapshotForStructuralChange()
                viewModelScope.launch {
                    blocksDb.makeListText(block.id, block.type)
                }
            }
            // Il backspace dopo: il testo lascia il posto dell'elenco.
            block.heldListType != null -> {
                snapshotForStructuralChange()
                viewModelScope.launch {
                    blocksDb.releaseListIndent(block.id)
                }
            }
            // Backspace a inizio di una casella toglie il quadratino e
            // lascia il testo, come per gli altri marcatori. Prima era
            // un ramo a parte (`backspaceAtIslandStart`) perché la
            // casella viveva fuori dal campo condiviso.
            block.type == BlockType.CHECKBOX -> {
                snapshotForStructuralChange()
                viewModelScope.launch {
                    blocksDb.setBlockType(block.id, BlockType.PARAGRAPH)
                }
            }
            block.indentLevel > 0 -> {
                snapshotForStructuralChange()
                viewModelScope.launch {
                    blocksDb.setIndentLevel(block.id, block.indentLevel - 1)
                }
            }
            // Prima riga del gruppo, e sopra c'è un blocco che vive per
            // conto suo (una casella da spuntare, un toggle): la fusione
            // normale non lo raggiunge, perché quella sa muoversi solo
            // dentro il campo di testo condiviso. Senza questo ramo il
            // backspace lì non faceva niente — sembrava di essere in
            // cima alla pagina mentre sopra c'era ancora roba.
            lineIndex == 0 -> return mergeIntoIslandAbove(block)
            else -> return BackspaceOutcome.NOTHING
        }
        return BackspaceOutcome.STEP_UNDONE
    }

    /**
     * Fonde `block` (la prima riga di un gruppo di testo) con il blocco
     * isola che ha sopra: il testo si attacca in coda a quello
     * dell'isola, e la riga sparisce.
     *
     * Dentro le caselle da spuntare il testo si può scrivere ma non si
     * poteva fondere: è la stessa separazione che impedisce la
     * selezione fra un blocco e l'altro. Qui almeno il backspace
     * attraversa il confine, così si può cancellare all'indietro senza
     * trovare muri.
     */
    private fun mergeIntoIslandAbove(block: BlockEntity): BackspaceOutcome {
        val siblings = _blocks.value
            .filter { it.parentBlockId == block.parentBlockId }
            .sortedBy { it.orderIndex }
        val position = siblings.indexOfFirst { it.id == block.id }
        if (position <= 0) return BackspaceOutcome.NOTHING
        val above = siblings[position - 1]
        if (above.type == BlockType.DIVIDER) return backspaceUnderDivider(above, block, siblings.getOrNull(position + 1))
        // Le isole "contenitore" (tabelle, link, database) non hanno un
        // testo in cui fondersi, e dentro hanno roba da non buttare: lì
        // non si fa niente, come prima.
        // Riquadro e citazione (dal 06/10/2026) si fondono come il toggle:
        // la riga va in fondo al loro testo.
        // **E l'ultima riga del pezzo di sopra** (dal 18/10/2026): è testo
        // come questa, solo in un altro campo (vedi `chunkMembers`). Questo
        // pezzo continua dalla sua seconda riga.
        if (above.type != BlockType.CHECKBOX && above.type != BlockType.TOGGLE &&
            above.type != BlockType.CALLOUT && above.type != BlockType.QUOTE &&
            !isFlowingTextType(above.type)
        ) {
            return BackspaceOutcome.NOTHING
        }

        val aboveSpans = spansOf(above)
        val joinAt = aboveSpans.plainText().length
        val merged = aboveSpans + spansOf(block)

        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.applyBlockChanges(
                saved = listOf(above.copy(textJson = json.encodeToString(merged))),
                deleted = listOf(block)
            )
            // Il cursore va dove i due testi si sono uniti, non in
            // fondo: è lì che l'utente stava cancellando. Senza attesa,
            // per lo stesso motivo di `backspaceAtIslandStart`: la riga
            // che si fonde sparisce e la tastiera va riagganciata
            // all'altra prima che si chiuda.
            _pendingCaret.value = above.id to joinAt
            _focusRequestBlockId.value = above.id
        }
        return BackspaceOutcome.MERGED_INTO_ISLAND
    }

    // --- Il backspace sotto un divisore (corretto il 29/09/2026) ---

    private val _armedDivider = MutableStateFlow<String?>(null)

    /**
     * Il divisore "preso" dal backspace, che si accende: il prossimo
     * backspace a inizio della riga sotto lo toglie. Si spegne da solo dopo
     * `ARMED_DIVIDER_MS`.
     */
    val armedDivider: StateFlow<String?> = _armedDivider
    private var armedDividerJob: Job? = null

    /**
     * **Backspace a inizio della riga subito sotto un divisore.**
     *
     * Il 29/09/2026 il backspace lì toglieva il divisore ("sotto un divider
     * non posso cancellare nulla"), ma l'utente voleva cancellare **le righe
     * vuote** fra il divisore e il testo: arrivato all'ultima, spariva il
     * divisore e la riga vuota restava ("cancellando lo spazio vuoto fino al
     * divider me lo cancella"). Ora:
     *  - **riga vuota**: se ne va lei, il divisore resta, e il cursore va
     *    all'inizio della riga che sale al suo posto. **Se sotto non c'è una
     *    riga di testo** (la riga vuota era l'ultima, o sotto c'è una
     *    tabella, un database…) se ne va lo stesso, e la tastiera si chiude:
     *    il divisore non si può scrivere (dal 05/10/2026, chiesto
     *    dall'utente; prima il backspace lì non faceva niente);
     *  - **riga col testo**: come su Notion, il primo backspace "prende" il
     *    divisore (si accende), il secondo lo toglie. Così continuando a
     *    premere non sparisce senza avvisare.
     */
    private fun backspaceUnderDivider(divider: BlockEntity, line: BlockEntity, next: BlockEntity?): BackspaceOutcome {
        if (spansOf(line).plainText().isEmpty()) {
            // Senza una riga di testo sotto non c'è dove mettere il cursore:
            // la riga se ne va lo stesso, e chi ha chiamato lascia il fuoco
            // (la tastiera si chiude). Il divisore resta.
            if (next == null || !isFlowingTextType(next.type)) {
                disarmDivider()
                snapshotForStructuralChange()
                viewModelScope.launch {
                    structuralEdits.withLock { repository.applyBlockChanges(deleted = listOf(line)) }
                }
                return BackspaceOutcome.REMOVED_CLOSE_KEYBOARD
            }
            disarmDivider()
            snapshotForStructuralChange()
            viewModelScope.launch {
                structuralEdits.withLock { repository.applyBlockChanges(deleted = listOf(line)) }
                withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                    _blocks.first { shown -> shown.none { it.id == line.id } }
                }
                _pendingCaret.value = next.id to 0
                _focusRequestBlockId.value = next.id
            }
            return BackspaceOutcome.MERGED_INTO_ISLAND
        }
        if (_armedDivider.value == divider.id) {
            disarmDivider()
            removeDividerAbove(divider, line)
            return BackspaceOutcome.MERGED_INTO_ISLAND
        }
        _armedDivider.value = divider.id
        armedDividerJob?.cancel()
        armedDividerJob = viewModelScope.launch {
            delay(ARMED_DIVIDER_MS)
            _armedDivider.compareAndSet(divider.id, null)
        }
        return BackspaceOutcome.STEP_UNDONE
    }

    private fun disarmDivider() {
        armedDividerJob?.cancel()
        _armedDivider.value = null
    }

    /**
     * Toglie il divisore sopra `line`. La riga di solito cambia campo di
     * testo (il suo gruppo si unisce a quello sopra il divisore): chi
     * chiama posa prima il fuoco sul campo invisibile, e qui il cursore
     * torna sulla riga, all'inizio, quando la lista mostrata è già quella
     * senza divisore.
     */
    private fun removeDividerAbove(divider: BlockEntity, line: BlockEntity): Boolean {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { repository.applyBlockChanges(deleted = listOf(divider)) }
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                _blocks.first { shown -> shown.none { it.id == divider.id } }
            }
            _pendingCaret.value = line.id to 0
            _focusRequestBlockId.value = line.id
        }
        return true
    }

    /**
     * Backspace a inizio di una casella da spuntare: toglie la casella
     * e lascia il testo, che diventa una riga normale — lo stesso
     * gradino che il testo unito fa già per gli elenchi. Da lì in poi
     * la riga è testo come gli altri e si fonde con quella sopra.
     *
     * Restituisce true se il campo che ha chiamato **sta per essere
     * distrutto senza un altro posto dove mandare il fuoco**, e quindi
     * deve posarlo sul campo invisibile. La riga vuota dentro un toggle
     * restituisce false anche quando sparisce: lì il fuoco va sulla riga
     * di sopra prima che la riga venga tolta.
     */
    fun backspaceAtIslandStart(block: BlockEntity): Boolean {
        if (block.type == BlockType.TOGGLE) return unmakeToggle(block)
        if (block.type in MULTILINE_TYPES) return unmakeMultiline(block)
        // Un elemento di elenco dentro un toggle (dal 18/10/2026): come nel
        // testo unito, prima se ne va il marcatore e il testo resta al suo
        // posto, poi va a sinistra. La riga resta la stessa casella di
        // scrittura, quindi il fuoco non si sposta.
        val current = _blocks.value.find { it.id == block.id } ?: block
        if (current.type in LIST_INDENT_TYPES || current.heldListType != null) {
            snapshotForStructuralChange()
            viewModelScope.launch {
                if (current.type in LIST_INDENT_TYPES) {
                    blocksDb.makeListText(current.id, current.type)
                } else {
                    blocksDb.releaseListIndent(current.id)
                }
            }
            return false
        }
        // La riga-notifica, col backspace a inizio riga, torna testo come
        // la casella da spuntare: la notifica se ne va, il testo resta.
        if (block.type != BlockType.CHECKBOX && block.type != BlockType.NOTIFICATION) {
            return removeEmptyNestedLine(block)
        }
        snapshotForStructuralChange()
        viewModelScope.launch {
            // Solo la colonna del tipo: il testo resta quello che c'è
            // adesso nel database, non la copia che ha in mano la UI.
            blocksDb.setBlockType(block.id, BlockType.PARAGRAPH)
            // **Senza attesa.** Qui il campo muore per forza: il blocco
            // passa da riga a sé a testo scorrevole, che è un'altra
            // composable. L'unica cosa che si può fare è far riprendere
            // il fuoco al campo nuovo **prima** che la tastiera faccia
            // in tempo a chiudersi — l'animazione di chiusura dura
            // circa due decimi di secondo, quindi ogni millisecondo di
            // attesa qui è un pezzo di tastiera che sparisce in faccia.
            _focusRequestBlockId.value = block.id
        }
        return true
    }

    /**
     * Backspace a inizio del titolo di un toggle: **il toggle torna testo
     * normale**, col suo titolo, e le righe che conteneva escono e
     * restano subito sotto, allo stesso livello. Come su Notion.
     *
     * Prima non succedeva niente, e per un toggle col titolo scritto non
     * c'era nessun modo di toglierlo: il cestino della barra compare solo
     * sulle righe vuote. Così invece si fa com'è naturale — un backspace
     * lo trasforma, un altro lo unisce alla riga sopra.
     *
     * Le righe interne **non si cancellano** di proposito: un backspace
     * che si porta via mezza pagina nascosta dentro un toggle chiuso
     * sarebbe il modo peggiore di perdere del testo.
     */
    private fun unmakeMultiline(block: BlockEntity): Boolean {
        // **Backspace a inizio di un riquadro o di una citazione** (dal
        // 06/10/2026): torna testo normale, come su Notion, con tutte le sue
        // righe. Un altro backspace la unisce alla riga sopra.
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { repository.convertMultiline(block.id, BlockType.PARAGRAPH) }
            _pendingCaret.value = block.id to 0
            _focusRequestBlockId.value = block.id
        }
        return true
    }

    private fun unmakeToggle(block: BlockEntity): Boolean {
        val current = _blocks.value.find { it.id == block.id } ?: return false
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                blocksDb.unnestAndConvert(current.id, BlockType.PARAGRAPH)
            }
            // Il cursore resta **a inizio riga**, dov'era quando si è
            // premuto: così il backspace successivo unisce la riga a
            // quella sopra, invece di mangiare l'ultima lettera del
            // titolo come farebbe col cursore in fondo.
            _pendingCaret.value = current.id to 0
            // Senza attesa, per la stessa ragione della casella da
            // spuntare qui sopra: il campo di prima è già morto, e ogni
            // istante perso è un pezzo di tastiera che si chiude.
            _focusRequestBlockId.value = current.id
        }
        return true
    }

    /**
     * Backspace a inizio di una riga **vuota dentro un toggle**: la riga
     * sparisce e il cursore sale su quella di sopra — o sul titolo del
     * toggle, se era la prima.
     *
     * Prima non succedeva niente: le righe dentro un toggle sono campi a
     * sé, fuori dal testo condiviso, e il backspace a inizio riga lì
     * veniva ignorato. Una riga vuota creata per sbaglio non si riusciva
     * più a togliere.
     *
     * Solo se è vuota: con del testo dentro bisognerebbe fonderlo con la
     * riga sopra, che è un altro campo, ed è un lavoro a parte. Restituisce
     * sempre false: il campo non deve posare il fuoco da nessuna parte,
     * perché glielo prende direttamente la riga di sopra.
     */
    private fun removeEmptyNestedLine(block: BlockEntity): Boolean {
        val parentId = block.parentBlockId ?: return false
        if (block.type == BlockType.TOGGLE) return false
        val current = _blocks.value.find { it.id == block.id } ?: return false
        if (plainTextOf(current).isNotEmpty()) return false

        val previousSibling = _blocks.value
            .filter { it.parentBlockId == parentId && it.orderIndex < current.orderIndex }
            .filter { isFocusableBlockType(it.type) }
            .maxByOrNull { it.orderIndex }
        // La prima riga di un blocco sincronizzato o di una colonna: sopra non
        // c'è un campo dove andare, il blocco si toglie dal suo menu.
        val parentType = _blocks.value.find { it.id == parentId }?.type
        if (previousSibling == null && (parentType == BlockType.SYNCED || parentType == BlockType.COLUMN)) return false
        val focusTarget = previousSibling?.id ?: parentId
        // La tastiera a volte manda lo stesso backspace due volte, a due
        // millisecondi di distanza (si vede nel registro): la seconda
        // troverebbe la riga ancora lì e la toglierebbe un'altra volta.
        if (!removingNestedLines.add(current.id)) return false

        // **Prima il cursore sale, poi la riga sparisce.**
        //
        // Prima era il contrario: il fuoco si posava sul campo invisibile,
        // la riga veniva cancellata, e 60 ms dopo il cursore andava sulla
        // riga di sopra. Per tutto quel tempo nessuna riga aveva il fuoco,
        // e la barra sopra la tastiera — che si vede solo se c'è una riga
        // a fuoco — **spariva e tornava**: misurato, cento millisecondi,
        // quattro fotogrammi del video senza barra. Era lo scatto più forte
        // del solito che si vedeva salendo col backspace.
        //
        // Adesso il cursore passa direttamente da questa riga a quella di
        // sopra, in un colpo solo: nessun istante senza fuoco, nessun campo
        // intermedio. La riga si cancella **dopo**, quando non ha più il
        // cursore — distruggere il campo che ce l'ha fa chiudere l'app.
        snapshotForStructuralChange()
        val target = _blocks.value.find { it.id == focusTarget }
        // In fondo alla riga di sopra, dove un backspace deve portare.
        _pendingCaret.value = focusTarget to (target?.let { plainTextOf(it).length } ?: 0)
        _focusRequestBlockId.value = focusTarget
        viewModelScope.launch {
            try {
                // Il `true` in fondo non è decorativo: la richiesta servita
                // diventa `null`, e restituendo quella `withTimeoutOrNull`
                // non si distinguerebbe più dal tempo scaduto — la riga
                // non veniva mai tolta.
                val moved = withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS) {
                    focusRequestBlockId.first { it != focusTarget }
                    true
                } ?: false
                if (!moved) {
                    // La riga di sopra non ha preso il fuoco: questa lo ha
                    // ancora, e cancellarla adesso sarebbe il crash. Resta
                    // dov'è; un altro backspace riproverà.
                    if (_focusRequestBlockId.value == focusTarget) _focusRequestBlockId.value = null
                    if (_pendingCaret.value?.first == focusTarget) _pendingCaret.value = null
                    return@launch
                }
                repository.deleteBlock(current)
            } finally {
                removingNestedLines.remove(current.id)
            }
        }
        // Il campo resta vivo finché il fuoco non se n'è andato: non deve
        // posarlo sul campo invisibile. Vedi `backspaceAtIslandStart`.
        return false
    }

    /** Le righe dentro un toggle che un backspace sta già togliendo. */
    private val removingNestedLines = mutableSetOf<String>()

    /**
     * L'utente ha appena scritto "5." (o "1.", o qualsiasi numero) come
     * unica cosa in un blocco: quel blocco diventa un elemento di elenco
     * numerato che parte da quel numero, e il "5." scritto a mano sparisce
     * perché da lì in poi lo disegna la lista.
     *
     * Vale solo sui paragrafi: su un blocco che è già un elenco, o su un
     * titolo, non si converte niente. Restituisce true se ha convertito,
     * così il campo sa che deve togliere il testo digitato.
     */
    fun startNumberedListAtLine(runBlockIds: List<String>, lineIndex: Int, startNumber: Int): Boolean {
        val block = resolveRun(runBlockIds)?.getOrNull(lineIndex) ?: return false
        return startNumberedList(block, startNumber)
    }

    /**
     * **"---" in una riga di testo** (dal 14/10/2026, chiesto dall'utente, come
     * su Notion): la riga del testo condiviso che deve diventare un divisore,
     * o null se non è un paragrafo — su un elenco o una casella i trattini
     * restano scritti, come "5.". A trasformarla ci pensa poi la stessa voce
     * "Divider" del menu "/".
     */
    fun dividerShortcutBlock(runBlockIds: List<String>, lineIndex: Int): BlockEntity? =
        resolveRun(runBlockIds)?.getOrNull(lineIndex)?.takeIf { it.type == BlockType.PARAGRAPH }

    /**
     * Lo stesso "5." per una riga che ha un campo suo: le righe dentro un
     * toggle. Fino al 25/09/2026 lì non scattava — la regola c'era solo
     * nel testo condiviso — e l'utente ha trovato che dentro un toggle
     * "le Numbered list non vengono attivate".
     */
    fun startNumberedListInBlock(blockId: String, startNumber: Int): Boolean {
        val block = _blocks.value.find { it.id == blockId } ?: return false
        return startNumberedList(block, startNumber)
    }

    private fun startNumberedList(block: BlockEntity, startNumber: Int): Boolean {
        if (block.type != BlockType.PARAGRAPH) return false
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(
                block.copy(
                    type = BlockType.NUMBERED_LIST_ITEM,
                    numberStartsAt = startNumber,
                    textJson = json.encodeToString(listOf(RichTextSpan(text = "")))
                )
            )
        }
        return true
    }

    /**
     * L'utente ha appena chiuso un `||qualcosa||` su una riga del testo
     * unito: le quattro barre spariscono — erano un comando, non roba
     * scritta — e quello che stava in mezzo resta, coperto.
     *
     * `lineText` è la riga **già ripulita** dalle barre, cioè quella che
     * il campo mostra da adesso; `spoilerStart`/`spoilerEnd` sono le
     * posizioni del testo da coprire dentro quella riga. Passarla già
     * fatta evita di rifare qui lo stesso conto che ha fatto il campo e
     * di rischiare che i due non coincidano.
     */
    fun applySpoilerAtLine(
        runBlockIds: List<String>,
        lineIndex: Int,
        lineText: String,
        spoilerStart: Int,
        spoilerEnd: Int
    ): Boolean {
        val block = resolveRun(runBlockIds)?.getOrNull(lineIndex) ?: return false
        snapshotForTextEdit(block.id)
        val spans = setFormatInRange(
            applyTextEdit(spansOf(block), plainTextOf(block), lineText),
            spoilerStart,
            spoilerEnd
        ) { s, _ -> s.copy(spoiler = true) }
        viewModelScope.launch {
            // Solo la colonna del testo: qui non cambia nient'altro del
            // blocco, e riscriverlo tutto rischierebbe di riportare
            // indietro un tipo o un rientro cambiati nel frattempo.
            blocksDb.setBlockText(block.id, json.encodeToString(spans))
        }
        return true
    }

    /**
     * `linesBefore` è quello che c'era scritto **nel campo** un attimo
     * prima, cioè quello che l'utente vedeva. Non è un doppione dei
     * testi salvati: il salvataggio va a giro per il database e può
     * essere indietro di qualche decina di millisecondi, e in quella
     * finestra i due non coincidono. Il confronto serve a capire
     * *quale riga è cambiata*, quindi va fatto su quello che l'utente
     * aveva davanti: con i testi salvati, una riga appena scritta ma
     * non ancora salvata risultava diversa, il confronto si
     * disallineava di una posizione, e la riga nuova nata dall'Invio
     * ereditava il tipo dalla riga sbagliata — è il motivo per cui
     * "0." creava la lista ma poi l'Invio dava un paragrafo invece di
     * continuarla. Se il campo e i blocchi non hanno nemmeno lo stesso
     * numero di righe, la UI è indietro di una struttura intera e non
     * ci si può fidare: lì si torna ai testi salvati.
     *
     * **Il confronto si fa col database di adesso, una modifica alla
     * volta** (dal 26/09/2026). Prima si faceva subito, coi blocchi che il
     * ViewModel aveva in mano: premendo Invio in fretta il secondo arrivava
     * prima che il database avesse scritto il primo, le righe del campo
     * erano una più dei blocchi, si tornava ai testi salvati e da lì
     * sembravano nate **due** righe — il database ne aggiungeva una di
     * troppo. Da quel momento campo e blocchi non si corrispondevano più, e
     * le righe con font diversi, pedici e apici si vedevano senza (trovato
     * dall'utente: "scattano"). Adesso ogni modifica aspetta la precedente
     * (`structuralEdits`) e rilegge i blocchi dal database: lì la
     * precedente c'è già, e il confronto con `linesBefore` è esatto.
     */
    fun updateRun(
        runBlockIds: List<String>,
        newLines: List<String>,
        linesBefore: List<String>? = null,
        caretLine: Int? = null,
        // I link di un incolla, nelle posizioni del testo intero (righe unite
        // con l'a-capo): vedi `PasteLinks.kt`.
        links: List<LinkRange> = emptyList(),
        // Il resto di un incolla (dal 18/10/2026): stili e pezzi da ripulire.
        paste: PasteFormat = PasteFormat()
    ) {
        // Si sta scrivendo: il campo col fuoco per un po' non si tocca (vedi `keepVisibleTogether`).
        if (newLines != linesBefore) lastTypedAt = System.currentTimeMillis()
        // Qui, subito, solo quello che serve ad Annulla e alla cronologia:
        // guardano la modifica così come l'ha fatta l'utente.
        val shownRun = runOfField(runBlockIds, linesBefore) ?: resolveRun(runBlockIds) ?: return
        val shownOld = if (linesBefore != null && linesBefore.size == shownRun.size) {
            linesBefore
        } else {
            shownRun.map { plainTextOf(it) }
        }
        if (shownOld == newLines) return
        val (shownPrefix, shownSuffix) = runDiff(shownOld, newLines, caretLine)
        val shownMiddle = shownRun.subList(shownPrefix, shownOld.size - shownSuffix)
        val shownNewMiddle = newLines.size - shownSuffix - shownPrefix
        if (shownMiddle.isEmpty() && shownNewMiddle == 0) return
        if (shownMiddle.size == 1 && shownNewMiddle == 1) {
            snapshotForTextEdit(shownMiddle[0].id)
        } else {
            snapshotForStructuralChange()
        }

        val pageId = currentPageId ?: return
        viewModelScope.launch {
            structuralEdits.withLock {
                val fresh = repository.getBlocksOnce(pageId)
                // La riga vuota di una pagina nuova non è ancora nel database.
                val unsaved = placeholder?.takeIf { p -> p.pageId == pageId && fresh.none { it.id == p.id } }
                val all = fresh + listOfNotNull(unsaved)
                val runBlocks = runOfField(runBlockIds, linesBefore, all) ?: resolveRun(runBlockIds, all) ?: return@withLock
                writeRunChange(pageId, runBlocks, unsaved, newLines, linesBefore, caretLine, paste.copy(links = paste.links + links))
            }
        }
    }

    /**
     * **Le righe del campo, quando i pezzi sono cambiati sotto di lui** (dal
     * 26/10/2026, trovato dall'utente in "Fast notes": cambiando solo il corpo
     * di "Anki field codes", 34 righe sotto erano ricomparse doppie, tutte
     * come punti elenco). Le righe sullo schermo stanno in un campo solo
     * (`keepVisibleTogether`) e, quando non si scrive, si ridividono in pezzi
     * da 50 (`rechunk`): per un momento il campo ha ancora le righe di prima,
     * mentre [resolveRun] guarda i confini di adesso e trova solo le prime 50.
     * Le altre del campo sembravano scritte in quel momento, e diventavano
     * righe nuove col tipo della cinquantesima ("English meaning", un punto
     * elenco); o, col campo più corto del pezzo, sparivano.
     *
     * Allora, se le righe che il campo dice di avere ci sono ancora, una dopo
     * l'altra e con lo stesso testo che aveva lui, il gruppo è il suo: campo e
     * database parlano delle stesse righe. Altrimenti (Invio premuto prima
     * che la schermata si aggiorni, una riga sparita) decide [resolveRun].
     */
    private fun runOfField(
        runBlockIds: List<String>,
        linesBefore: List<String>?,
        all: List<BlockEntity> = _blocks.value
    ): List<BlockEntity>? {
        if (linesBefore == null || runBlockIds.isEmpty() || runBlockIds.size != linesBefore.size) return null
        val byId = all.associateBy { it.id }
        val blocks = runBlockIds.map { id -> byId[id] ?: return null }
        val parentId = blocks.first().parentBlockId
        if (blocks.any { it.parentBlockId != parentId || !isFlowingTextType(it.type) }) return null
        val siblings = all.filter { it.parentBlockId == parentId }.sortedBy { it.orderIndex }
        val first = siblings.indexOfFirst { it.id == runBlockIds.first() }
        if (first < 0 || first + blocks.size > siblings.size) return null
        if (siblings.subList(first, first + blocks.size).map { it.id } != runBlockIds) return null
        if (blocks.map { plainTextOf(it) } != linesBefore) return null
        return blocks
    }

    /**
     * Quante righe in cima (`first`) e in fondo (`second`) sono rimaste
     * uguali fra `oldTexts` e `newLines`: in mezzo c'è la modifica.
     */
    private fun runDiff(oldTexts: List<String>, newLines: List<String>, caretLine: Int?): Pair<Int, Int> {
        var prefixCount = 0
        val minCount = minOf(oldTexts.size, newLines.size)
        while (prefixCount < minCount && oldTexts[prefixCount] == newLines[prefixCount]) prefixCount++
        // **La modifica non può essere dopo il cursore.** Una riga
        // vuota inserita in mezzo ad altre righe vuote è identica a
        // tutte le sue vicine: il confronto da solo non sa quale sia
        // la nuova, e finiva per attribuirla all'ultima riga del
        // gruppo. Lì il "vicino da cui ereditare" era l'ultimo blocco
        // invece della riga su cui si stava scrivendo, e l'Invio in
        // fondo a un elenco dava un paragrafo: la lista si fermava.
        // Il cursore dice senza ambiguità dove è successo.
        if (caretLine != null) {
            prefixCount = prefixCount.coerceAtMost(caretLine.coerceIn(0, minCount))
        }

        var suffixCount = 0
        // **E quando le righe calano, la riga del cursore è fra quelle
        // riscritte.** Col backspace a inizio della seconda di quattro
        // righe vuote, il confronto dal fondo le trovava tutte uguali e
        // toglieva la **prima**: per il testo è lo stesso, ma il campo
        // condiviso era legato al suo primo blocco (la chiave nell'elenco;
        // oggi non più, vedi `groupIntoRenderItems`), e tolto quello
        // rinasceva da capo — la tastiera spariva.
        // Trovato dall'utente il 25/09/2026. Tenendo dentro la riga del
        // cursore si riscrive quella e si toglie la successiva, che è
        // quella davvero cancellata. Solo quando le righe calano:
        // all'Invio a inizio riga il blocco che scende deve restare lui.
        val maxSuffix = if (caretLine != null && newLines.size < oldTexts.size) {
            minOf(minCount - prefixCount, (newLines.size - 1 - caretLine).coerceAtLeast(0))
        } else {
            minCount - prefixCount
        }
        while (suffixCount < maxSuffix &&
            oldTexts[oldTexts.size - 1 - suffixCount] == newLines[newLines.size - 1 - suffixCount]
        ) suffixCount++
        return prefixCount to suffixCount
    }

    /** La scrittura di `updateRun`, coi blocchi letti adesso dal database. */
    private suspend fun writeRunChange(
        pageId: String,
        runBlocks: List<BlockEntity>,
        unsaved: BlockEntity?,
        newLines: List<String>,
        linesBefore: List<String>?,
        caretLine: Int?,
        paste: PasteFormat = PasteFormat()
    ) {
        val stored = runBlocks.map { plainTextOf(it) }
        if (stored == newLines) return
        val oldTexts = if (linesBefore != null && linesBefore.size == runBlocks.size) {
            linesBefore
        } else {
            stored
        }
        if (oldTexts == newLines) return
        val (prefixCount, suffixCount) = runDiff(oldTexts, newLines, caretLine)

        val oldMiddleStart = prefixCount
        val oldMiddleEnd = oldTexts.size - suffixCount
        val oldMiddleBlocks = runBlocks.subList(oldMiddleStart, oldMiddleEnd)
        val newMiddleLines = newLines.subList(prefixCount, newLines.size - suffixCount)

        if (oldMiddleBlocks.isEmpty() && newMiddleLines.isEmpty()) return

        val parentBlockId = runBlocks.first().parentBlockId

        // Il blocco da cui i nuovi ereditano tipo e rientro è sempre
        // l'ultimo della zona modificata; se la zona è vuota (puro
        // inserimento, es. Invio a fine riga senza toccare le righe
        // esistenti), è quello immediatamente prima del punto in cui si
        // inserisce.
        val referenceBlock = oldMiddleBlocks.lastOrNull()
            ?: runBlocks.getOrNull(oldMiddleStart - 1)
        val indentLevel = referenceBlock?.indentLevel ?: 0
        // I titoli non si propagano: premere Invio dopo un titolo dà un
        // paragrafo normale, come in qualsiasi editor. Gli elenchi
        // invece sì, così continuare una lista crea altri elementi.
        val inheritedType = when (referenceBlock?.type) {
            BlockType.BULLET_LIST_ITEM,
            BlockType.NUMBERED_LIST_ITEM,
            // Andando a capo su una casella se ne ottiene un'altra, come
            // su Notion: è una lista di cose da fare, non una riga sola.
            // La nuova nasce **non spuntata**, ovviamente.
            BlockType.CHECKBOX -> referenceBlock.type
            else -> BlockType.PARAGRAPH
        }
        // Andando a capo da una riga che tiene il posto di un elenco, anche
        // la nuova lo tiene: resta allineata, come su Word.
        val inheritedAttrs = referenceBlock?.attrsForNewLine() ?: "{}"

        // **La formattazione sopravvive alla scrittura.**
        //
        // Prima ogni riga toccata veniva riscritta come un unico span
        // senza formato: bastava aggiungere una lettera a un paragrafo
        // perché il grassetto di una parola lontana sparisse. Con gli
        // spoiler sarebbe stato ancora più evidente — si copre una
        // parola, si continua a scrivere sulla stessa riga e il velo
        // cade da solo.
        //
        // Qui la zona modificata si tratta come **un testo solo**,
        // a-capo compresi: la stessa modifica si applica agli span con
        // `applyTextEdit`, che tocca soltanto il pezzo davvero
        // cambiato, e poi si rimette in righe. Vale anche quando il
        // cambiamento attraversa più righe: andando a capo in mezzo a
        // una frase, la metà che scende tiene il suo formato.
        val oldMiddleSpans = oldMiddleBlocks.map { spansOf(it) }.joinLines()
        val oldMiddleText = oldMiddleBlocks.joinToString("\n") { plainTextOf(it) }
        // Quello che porta un incolla (link, stili) va sulla zona modificata:
        // le posizioni si contano da dove comincia.
        val middleOffset = newLines.take(prefixCount).sumOf { it.length + 1 }
        val middlePaste = paste.shifted(-middleOffset)
        val newMiddleSpans = applyPaste(
            applyTextEdit(
                oldMiddleSpans,
                oldMiddleText,
                newMiddleLines.joinToString("\n")
            ),
            middlePaste
        ).splitLines()

        // **Le righe incollate col loro tipo** (dal 19/10/2026): una riga che è
        // tutta e solo una riga copiata intera da notE prende il suo tipo
        // (casella, spuntata o no, elenco puntato o numerato) e il suo
        // rientro, contato da quello della riga in cui si incolla. Vedi
        // `PastedLine`. Le altre restano come sempre.
        val lineKinds: List<PastedLine?> = run {
            var at = 0
            newMiddleLines.map { line ->
                val kind = middlePaste.lineAt(at, at + line.length)
                at += line.length + 1
                kind
            }
        }
        val minPastedIndent = lineKinds.filterNotNull().minOfOrNull { it.indent } ?: 0
        fun pastedIndent(kind: PastedLine): Int =
            (indentLevel + kind.indent - minPastedIndent).coerceIn(0, MAX_INDENT_LEVEL)

        /** Una riga che c'era già e diventa quella incollata, col suo tipo; null se resta com'è. */
        fun retyped(block: BlockEntity, index: Int, textJson: String): BlockEntity? {
            val kind = lineKinds.getOrNull(index) ?: return null
            // Una tabella incollata (dal 20/10/2026): il testo della riga era
            // solo un segnaposto, le celle arrivano dopo (`writePastedCells`).
            val table = kind.table
            return block.copy(
                type = kind.type,
                isChecked = kind.checked,
                indentLevel = pastedIndent(kind),
                textJson = if (table != null) EMPTY_TEXT_JSON else textJson,
                tableRows = table?.size ?: block.tableRows,
                tableCols = table?.maxOfOrNull { it.size } ?: block.tableCols,
                attrsJson = BlockAttrs.encode(BlockAttrs.parse(block.attrsJson).copy(listIndent = null))
            )
        }

        /** Le celle delle tabelle incollate, quando le tabelle ci sono già: una riga per blocco. */
        suspend fun writePastedCells(blocks: List<Pair<String, PastedLine?>>) {
            for ((blockId, kind) in blocks) {
                val table = kind?.table ?: continue
                table.forEachIndexed { r, row ->
                    row.forEachIndexed { c, text -> if (text.isNotEmpty()) repository.setTableCellText(blockId, r, c, text) }
                }
            }
        }

        // Rete di sicurezza: se per qualsiasi motivo il risultato non
        // corrisponde più alla riga che deve descrivere, si scrive la
        // riga in chiaro. Meglio perdere un grassetto che scrivere nel
        // database un testo diverso da quello che l'utente vede.
        fun spansForLine(index: Int, line: String): List<RichTextSpan> =
            newMiddleSpans.getOrNull(index)?.takeIf { it.plainText() == line }
                ?: listOf(RichTextSpan(text = line))

        // La riga vuota di una pagina appena aperta va scritta anche se la
        // modifica non la tocca (l'Invio aggiunge solo quella sotto): vedi
        // `placeholder`. Nella stessa transazione, così non c'è un attimo
        // in cui il database ha la riga nuova e non quella di prima. Se è
        // lei a cambiare testo, prima la si crea e poi le si scrive il testo.
        val placeholderToSave = unsaved?.takeIf { p -> runBlocks.any { it.id == p.id } }

        // Una transazione sola per tutto il cambiamento, righe
        // riscritte e righe nuove insieme: vedi
        // `PageRepository.applyBlockChanges`. Scritte una alla volta
        // facevano ridisegnare la pagina una volta per riga, proprio
        // mentre l'utente stava scrivendo. Delle righe che c'erano già si
        // riscrive **solo il testo** (`texts`).
        run {
            if (newMiddleLines.size >= oldMiddleBlocks.size) {
                val retypedBlocks = oldMiddleBlocks.mapIndexedNotNull { i, block ->
                    retyped(block, i, json.encodeToString(spansForLine(i, newMiddleLines[i])))
                }
                val retypedIds = retypedBlocks.mapTo(HashSet()) { it.id }
                val rewritten = oldMiddleBlocks.mapIndexed { i, block ->
                    block.id to json.encodeToString(spansForLine(i, newMiddleLines[i]))
                }.filter { it.first !in retypedIds }.toMap()
                val extraLines = newMiddleLines.drop(oldMiddleBlocks.size)
                // Senza un blocco prima (Invio a inizio della prima riga)
                // le righe nuove vanno al posto di quella prima riga, non
                // a zero: zero è la cima della **pagina**, e se sopra il
                // gruppo c'è un toggle o un link a una pagina la riga
                // vuota ci saltava sopra.
                val shiftFromIndex = referenceBlock?.let { it.orderIndex + 1 }
                    ?: runBlocks.getOrNull(oldMiddleStart)?.orderIndex
                    ?: 0
                val newBlocks = extraLines.mapIndexed { i, line ->
                    val kind = lineKinds.getOrNull(oldMiddleBlocks.size + i)
                    val table = kind?.table
                    BlockEntity(
                        pageId = pageId,
                        parentBlockId = parentBlockId,
                        type = kind?.type ?: inheritedType,
                        isChecked = kind?.checked ?: false,
                        indentLevel = kind?.let { pastedIndent(it) } ?: indentLevel,
                        textJson = if (table != null) {
                            EMPTY_TEXT_JSON
                        } else {
                            json.encodeToString(spansForLine(oldMiddleBlocks.size + i, line))
                        },
                        tableRows = table?.size ?: 2,
                        tableCols = table?.maxOfOrNull { it.size } ?: 2,
                        orderIndex = shiftFromIndex + i,
                        attrsJson = if (kind != null) "{}" else inheritedAttrs
                    )
                }
                // Le righe nuove restano nel pezzo in cui si scrive (vedi
                // `chunkMembers`); messe in cima, ne diventano l'inizio.
                if (parentBlockId == null) {
                    noteNewChunkLines(runBlocks.first().id, referenceBlock?.id, newBlocks.map { it.id })
                }
                repository.applyBlockChanges(
                    shift = if (extraLines.isEmpty()) {
                        null
                    } else {
                        PageRepository.BlockShift(
                            pageId,
                            parentBlockId,
                            shiftFromIndex,
                            extraLines.size
                        )
                    },
                    ensured = listOfNotNull(placeholderToSave),
                    texts = rewritten,
                    saved = retypedBlocks + newBlocks
                )
                writePastedCells(
                    oldMiddleBlocks.mapIndexed { i, block -> block.id to lineKinds.getOrNull(i) } +
                        newBlocks.mapIndexed { i, block -> block.id to lineKinds.getOrNull(oldMiddleBlocks.size + i) }
                )
            } else {
                val retypedBlocks = newMiddleLines.indices.mapNotNull { i ->
                    retyped(oldMiddleBlocks[i], i, json.encodeToString(spansForLine(i, newMiddleLines[i])))
                }
                val retypedIds = retypedBlocks.mapTo(HashSet()) { it.id }
                repository.applyBlockChanges(
                    ensured = listOfNotNull(placeholderToSave),
                    texts = newMiddleLines.mapIndexed { i, line ->
                        oldMiddleBlocks[i].id to json.encodeToString(spansForLine(i, line))
                    }.filter { it.first !in retypedIds }.toMap(),
                    saved = retypedBlocks,
                    deleted = oldMiddleBlocks.drop(newMiddleLines.size)
                )
                writePastedCells(newMiddleLines.indices.map { i -> oldMiddleBlocks[i].id to lineKinds.getOrNull(i) })
            }
        }
    }

    /** Se un blocco ha righe dentro di sé: in pratica, un toggle non vuoto. */
    private fun hasChildren(blockId: String): Boolean = _blocks.value.any { it.parentBlockId == blockId }

    /**
     * **Le righe già lette** (dal 18/10/2026): lo stesso JSON dà sempre gli
     * stessi span, e la pagina li chiede per ogni riga più volte a ogni
     * fotogramma. Le ultime usate restano qui; una riga cambiata ha un altro
     * JSON e si rilegge.
     */
    private val spansCache = object : LinkedHashMap<String, List<RichTextSpan>>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<RichTextSpan>>?) = size > 6_000
    }

    /** Gli span (testo + formattazione) di un blocco, decodificati dal JSON salvato. */
    fun spansOf(block: BlockEntity): List<RichTextSpan> {
        val textJson = block.textJson
        synchronized(spansCache) { spansCache[textJson]?.let { return it } }
        val spans = try {
            json.decodeFromString<List<RichTextSpan>>(textJson)
        } catch (e: Exception) {
            listOf(RichTextSpan(text = ""))
        }
        synchronized(spansCache) { spansCache[textJson] = spans }
        return spans
    }

    /** Estrae il testo semplice da un blocco, per mostrarlo nell'editor. */
    fun plainTextOf(block: BlockEntity): String = spansOf(block).plainText()

    // --- Tabelle semplici (BlockType.TABLE) ---

    fun tableCellsFlow(blockId: String): Flow<List<TableCellEntity>> =
        repository.getTableCells(blockId)

    fun setTableCell(blockId: String, row: Int, col: Int, text: String) {
        // Il contenuto delle celle vive in una tabella separata dai
        // blocchi: non è coperto dalla cronologia annulla/ripristina.
        viewModelScope.launch {
            blocksDb.setTableCellText(blockId, row, col, text)
            syncedWatcher.onCellChanged(blockId, lastLoadedBlocks)
        }
    }

    fun resizeTable(block: BlockEntity, newRows: Int, newCols: Int) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            repository.saveBlock(block.copy(tableRows = newRows, tableCols = newCols))
        }
    }

    // --- Blocchi link a pagina/database (BlockType.PAGE_LINK / DATABASE_LINK) ---

    /**
     * Converte un blocco in un link verso una pagina di testo, creandola
     * se il blocco non ne ha già una collegata (riselezionare "Page" su
     * un blocco già collegato non ne crea una seconda). onReady riceve
     * l'id della pagina per navigarci subito — scegliere "Page" dal
     * menu crea la sottopagina E ci entra, come in Notion.
     */
    /**
     * `title`, da "Turn into" nel menu del blocco: la riga aveva del testo,
     * e quel testo diventa il nome della pagina nuova invece di restare
     * nascosto dentro il collegamento, dove non lo vedrebbe più nessuno.
     */
    fun convertToPageLink(block: BlockEntity, title: String? = null, onReady: (String) -> Unit) {
        snapshotForStructuralChange()
        val liftChildren = hasChildren(block.id)
        viewModelScope.launch {
            // Vedi `updateBlockType`: i figli di un toggle escono prima.
            if (liftChildren) repository.unnestChildren(block.id)
            val targetPageId = block.linkedPageId ?: run {
                val newPage = PageEntity(title = title?.takeIf { it.isNotBlank() } ?: "Untitled", isDatabase = false)
                repository.createPage(newPage)
                newPage.id
            }
            if (block.type != BlockType.PAGE_LINK || block.linkedPageId != targetPageId) {
                repository.saveBlock(
                    block.copy(
                        type = BlockType.PAGE_LINK,
                        linkedPageId = targetPageId,
                        textJson = if (title != null) EMPTY_TEXT_JSON else block.textJson
                    )
                )
            }
            onReady(targetPageId)
        }
    }

    /** Come convertToPageLink, ma crea una pagina di tipo database. */
    /**
     * `simple` crea un **database semplice**, le cui righe non diventano
     * mai pagine (vedi `PageEntity.isSimpleDatabase`). Vale solo per un
     * database che nasce qui: uno che c'era già resta com'era.
     */
    fun convertToDatabaseLink(
        block: BlockEntity,
        simple: Boolean = false,
        /** Come in `convertToPageLink`: il testo della riga diventa il nome del database. */
        title: String? = null,
        onReady: (String) -> Unit
    ) {
        snapshotForStructuralChange()
        val liftChildren = hasChildren(block.id)
        viewModelScope.launch {
            if (liftChildren) repository.unnestChildren(block.id)
            val targetPageId = block.linkedPageId ?: run {
                val newPage = PageEntity(
                    title = title?.takeIf { it.isNotBlank() } ?: "Untitled",
                    isDatabase = true,
                    isSimpleDatabase = simple
                )
                repository.createPage(newPage)
                newPage.id
            }
            if (block.type != BlockType.DATABASE_LINK || block.linkedPageId != targetPageId) {
                repository.saveBlock(
                    block.copy(
                        type = BlockType.DATABASE_LINK,
                        linkedPageId = targetPageId,
                        textJson = if (title != null) EMPTY_TEXT_JSON else block.textJson
                    )
                )
            }
            onReady(targetPageId)
        }
    }

    /**
     * **"Database - Full page"** (dal 06/10/2026): un database nuovo che si
     * apre a pagina intera; qui resta il suo collegamento. `onReady` riceve
     * l'id per aprirlo subito, come fa "Page".
     */
    fun convertToFullPageDatabase(block: BlockEntity, title: String? = null, onReady: (String) -> Unit) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            val databaseId = structuralEdits.withLock { blocksDb.makeFullPageDatabase(block.id, title) }
            onReady(databaseId)
        }
    }

    /**
     * **"Link to page"** (dal 06/10/2026): la riga diventa un rimando a una
     * pagina che sta altrove. La pagina non si sposta e non entra
     * nell'albero di questa (vedi `BlockType.PAGE_MENTION`).
     */
    fun convertToPageMention(block: BlockEntity, pageId: String) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { blocksDb.setTypeAndAttrs(block.id, BlockType.PAGE_MENTION, BlockAttrs(pageId = pageId)) }
        }
    }

    // --- I bottoni (dal 27/10/2026): vedi `data/buttons/Buttons.kt` ---

    private val _buttonEditRequest = MutableStateFlow<String?>(null)

    /**
     * Il bottone di cui aprire la finestra delle azioni. La chiedono il menu
     * dei sei puntini ("Edit button") e i menu "/" e "+" per un bottone appena
     * nato, ma la apre **il blocco**, come per le immagini (`mediaRequest`).
     */
    val buttonEditRequest: StateFlow<String?> = _buttonEditRequest

    fun requestButtonEdit(blockId: String) {
        _buttonEditRequest.value = blockId
    }

    fun consumeButtonEditRequest() {
        _buttonEditRequest.value = null
    }

    /**
     * **"Button"** dai menu "/" e "+": la riga diventa un bottone ancora senza
     * azioni, e si apre subito la finestra per sceglierle. Con "Turn into" il
     * testo della riga diventa il nome ([name]).
     */
    fun convertToButton(block: BlockEntity, name: String = "") {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                val clean = name.lines().joinToString(" ").trim()
                blocksDb.setTypeAndAttrs(block.id, BlockType.BUTTON, BlockAttrs(button = ButtonConfig(name = clean)))
            }
            _buttonEditRequest.value = block.id
        }
    }

    // --- I blocchi sincronizzati (dal 03/11/2026): vedi `data/sync/SyncedBlocks.kt` ---

    /** Cambia quando le copie di un blocco sincronizzato possono essere cambiate: la cornice le riconta. */
    private val _syncedChanges = MutableStateFlow(0)
    val syncedChanges: StateFlow<Int> = _syncedChanges

    /** **"Synced block"** dai menu "/", "+" e "Turn into": il cursore va sulla sua prima riga. */
    fun makeSyncedBlock(block: BlockEntity) {
        // Uno dentro l'altro no: due gruppi si riscriverebbero a vicenda.
        if (insideSynced(block)) return
        snapshotForStructuralChange()
        viewModelScope.launch {
            val firstLine = structuralEdits.withLock { blocksDb.makeSynced(block.id) } ?: return@launch
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = firstLine
        }
    }

    /** **"Create two/three/four columns"** (dal 12/11/2026, solo sul PC): vedi `PageRepository.makeColumns`. Il cursore va nella prima colonna. */
    fun makeColumns(block: BlockEntity, count: Int) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            val firstLine = structuralEdits.withLock { blocksDb.makeColumns(block.id, count) } ?: return@launch
            // Il campo di testo da cui la riga esce deve prendere le righe nuove anche con
            // una sua scrittura in viaggio (quella del "/" tolto): senza, restava con quelle
            // di prima, la riga diventata colonne compresa ("le due colonne non si creano").
            _externalChangeTick.value += 1
            delay(IME_SETTLE_DELAY_MS)
            _focusRequestBlockId.value = firstLine
        }
    }

    private fun insideSynced(block: BlockEntity): Boolean {
        val byId = _blocks.value.associateBy { it.id }
        var parent = block.parentBlockId?.let { byId[it] }
        val visited = mutableSetOf<String>()
        while (parent != null && visited.add(parent.id)) {
            if (parent.type == BlockType.SYNCED) return true
            parent = parent.parentBlockId?.let { byId[it] }
        }
        return false
    }

    /** **"Sync to another page"**: una copia in fondo alla pagina [pageId], con le stesse righe. */
    fun syncToPage(block: BlockEntity, pageId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (repository.addSyncedCopy(block.id, pageId) != null) {
                _syncedChanges.value += 1
                onDone()
            }
        }
    }

    /** **"Unsync"**: questa copia torna righe normali; le altre restano sincronizzate fra loro. */
    fun unsyncBlock(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { repository.unsync(block.id) }
            _syncedChanges.value += 1
        }
    }

    /** In quante pagine c'è una copia del gruppo [syncId]. */
    suspend fun syncedPageCount(syncId: String): Int = withContext(Dispatchers.IO) { repository.syncedPageCount(syncId) }

    /** **"Link to button"**: la riga diventa la copia collegata del bottone [sourceBlockId]. */
    fun convertToLinkedButton(block: BlockEntity, sourceBlockId: String) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { blocksDb.setTypeAndAttrs(block.id, BlockType.BUTTON, BlockAttrs(buttonLink = sourceBlockId)) }
        }
    }

    /**
     * Nome, icona e azioni di un bottone. Il bottone vero di una copia
     * collegata può stare in un'altra pagina: lì l'Annulla di questa non
     * arriva, e si scrive e basta.
     */
    fun saveButton(blockId: String, config: ButtonConfig) {
        if (_blocks.value.any { it.id == blockId }) {
            updateBlockAttrs(blockId) { it.copy(button = config) }
        } else {
            viewModelScope.launch { repository.updateBlockAttrs(blockId) { it.copy(button = config) } }
        }
    }

    fun observeBlock(blockId: String): Flow<BlockEntity?> = repository.observeBlock(blockId)

    /** La riga di cui la pagina aperta è la pagina: "This page" per i suoi bottoni. */
    suspend fun rowOfPage(pageId: String): DatabaseRowEntity? = repository.rowOfPage(pageId)

    suspend fun buttonChoices(): List<PageRepository.ButtonChoice> = repository.buttonChoices()

    /** **"Link in another page"**: una copia collegata del bottone in fondo alla pagina [pageId]. */
    fun linkButtonIn(block: BlockEntity, pageId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (repository.addLinkedButton(block.id, pageId) != null) onDone()
        }
    }

    /**
     * **"Insert blocks" di un bottone nella pagina aperta**: si annulla in un
     * passo solo, i campi di testo si rileggono (`externalChangeTick`) e il
     * primo blocco nuovo si illumina. Null se i blocchi vanno in un'altra
     * pagina, che il motore scrive da sé.
     */
    suspend fun insertButtonBlocks(pageId: String, anchorBlockId: String?, place: InsertPlace, blocks: List<NewBlock>): Boolean? {
        if (pageId != currentPageId) return null
        snapshotForStructuralChange()
        val ids = structuralEdits.withLock { repository.insertBlocksAt(pageId, anchorBlockId, place, blocks) }
        val first = ids.firstOrNull() ?: return false
        _externalChangeTick.value += 1
        viewModelScope.launch {
            withTimeoutOrNull(FOCUS_HANDOFF_TIMEOUT_MS * 4) { _blocks.first { shown -> shown.any { it.id == first } } }
            _movedBlock.value = MovedBlock(first, System.nanoTime() / 1_000_000)
        }
        return true
    }

    // --- Immagini, video, audio e file (dal 06/10/2026) ---

    private val _mediaRequest = MutableStateFlow<MediaRequest?>(null)

    /**
     * **Una cosa da fare con un'immagine, un video, un audio o un file**:
     * sceglierlo, guardarlo a tutto schermo, scaricarlo, aprirlo altrove.
     *
     * La chiede il menu dei sei puntini, ma la fa **il blocco**: il menu si
     * chiude subito, e la galleria o la scelta di dove salvare, aperte da
     * lui, risponderebbero a nessuno. Anche il blocco appena nato dal menu
     * "/" chiede così di aprire la scelta del file, come fa Notion.
     */
    val mediaRequest: StateFlow<MediaRequest?> = _mediaRequest

    fun requestMedia(blockId: String, kind: MediaRequestKind) {
        _mediaRequest.value = MediaRequest(blockId, kind)
    }

    fun consumeMediaRequest() {
        _mediaRequest.value = null
    }

    /** La riga diventa un'immagine, un video, un audio, un file o un segnalibro ancora vuoti, e la scelta si apre. */
    /**
     * **"Record audio"** dai menu "/" e "+" (dal 03/11/2026): la riga diventa
     * un blocco audio vuoto che comincia subito a registrare (lo fa lui,
     * chiedendo il microfono se serve: vedi `AudioRecorder`).
     */
    fun convertToRecording(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { blocksDb.setTypeAndAttrs(block.id, BlockType.AUDIO, BlockAttrs()) }
            _mediaRequest.value = MediaRequest(block.id, MediaRequestKind.RECORD)
        }
    }

    /**
     * **Le pagine di un documento scansionato** (dal 03/11/2026): copiate
     * nell'app e messe come foto al posto della riga [blockId], se è vuota, o
     * sotto di lei. Si annullano in un passo; il loro testo si legge dopo, per
     * la ricerca (`ImageTexts`). [onFailed] se non se ne è salvata nessuna.
     *
     * [save] copia una pagina nell'app e ne dà il nome (sul telefono le pagine
     * sono `Uri` dello scanner: vedi `CaptureUi`); dal 04/11/2026, quando
     * l'editor è passato al codice comune.
     */
    fun <T> insertScannedPages(blockId: String, pages: List<T>, store: PageImageStore, save: suspend (T) -> String?, onFailed: () -> Unit) {
        viewModelScope.launch {
            val at = System.currentTimeMillis()
            val files = pages.mapNotNull { save(it) }
            if (files.isEmpty()) {
                onFailed()
                return@launch
            }
            val names = files.indices.map { CaptureStrings.scanName(at, it + 1, files.size, AppSettings.zoneId) }
            snapshotForStructuralChange()
            val ids = structuralEdits.withLock { repository.insertScannedPages(blockId, files, names) }
            if (ids.isEmpty()) {
                files.forEach { store.delete(it) }
                onFailed()
            } else {
                _externalChangeTick.value += 1
                PhotoTextRequests.request()
            }
        }
    }

    fun convertToMedia(block: BlockEntity, type: BlockType) {
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock { blocksDb.setTypeAndAttrs(block.id, type, BlockAttrs()) }
            _mediaRequest.value = MediaRequest(block.id, MediaRequestKind.PICK)
        }
    }

    /**
     * Sposta il blocco in fondo a un'altra pagina. **Senza Annulla**: il
     * blocco esce da questa pagina, e rimetterlo qui lascerebbe una copia
     * anche là. L'avviso dice dov'è finito.
     */
    fun moveBlockToPage(block: BlockEntity, destinationPageId: String, onDone: () -> Unit) {
        closeEditSession()
        viewModelScope.launch {
            if (structuralEdits.withLock { repository.moveBlockToPage(block.id, destinationPageId) }) onDone()
        }
    }

    // --- Il menu di un blocco (l'icona a sei puntini, o il dito tenuto su una pagina) ---

    /**
     * "Turn into Table" dal menu del blocco: il testo della riga va nella
     * prima cella, invece di restare nascosto dentro la tabella che non lo
     * mostra. Le celle sono testo semplice: grassetti e colori si perdono.
     */
    fun convertToTableKeepingText(block: BlockEntity) {
        val text = plainTextOf(block)
        val liftChildren = hasChildren(block.id)
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                if (liftChildren) repository.unnestChildren(block.id)
                blocksDb.setBlockType(block.id, BlockType.TABLE)
                if (text.isNotEmpty()) {
                    blocksDb.setTableCellText(block.id, 0, 0, text)
                    blocksDb.setBlockText(block.id, EMPTY_TEXT_JSON)
                }
            }
        }
    }

    /**
     * "Color" dal menu di un blocco: il colore va su **tutta** la riga, non
     * su una selezione come col pennello.
     *
     * Il collegamento a una pagina non ha testo suo — il nome è quello
     * della pagina — e il colore si tiene in uno span vuoto: la riga lo
     * legge da lì e lo dà al nome (vedi `PageLinkBlockContent`). È solo
     * di quel collegamento: la stessa pagina richiamata altrove resta
     * bianca, come su Notion.
     */
    fun colorWholeBlock(blockId: String, background: Boolean, hex: String?) {
        val block = _blocks.value.find { it.id == blockId } ?: return
        if (block.type == BlockType.PAGE_LINK) {
            val current = spansOf(block).firstOrNull() ?: RichTextSpan(text = "")
            val colored = if (background) {
                current.copy(text = "", background = hex)
            } else {
                current.copy(text = "", color = hex)
            }
            snapshotForStructuralChange()
            viewModelScope.launch { blocksDb.setBlockText(block.id, json.encodeToString(listOf(colored))) }
            return
        }
        applyColorToRun(listOf(block.id), 0, plainTextOf(block).length, background, hex)
    }

    /** "Duplicate" dal menu di un blocco: la copia nasce subito sotto. Vedi `PageRepository.duplicateBlock`. */
    fun duplicateBlock(blockId: String, imageStore: PageImageStore) {
        if (_blocks.value.none { it.id == blockId }) return
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                repository.duplicateBlock(blockId) { imageStore.copy(it) }
            }
        }
    }

    /**
     * La pagina a cui punta un collegamento, **seguita nel tempo**: rinominata
     * o cambiata d'icona dal menu del blocco, la riga del collegamento e il
     * menu stesso la mostrano subito così, senza dover riaprire la pagina.
     */
    fun observePageInfo(pageId: String): Flow<PageEntity?> = repository.observePage(pageId)

    // Le voci qui sotto agiscono sulla pagina **collegata**, non su quella
    // aperta: le gemelle del menu dei tre puntini (`toggleFavorite`,
    // `movePageTo`, `moveToTrash`...) lavorano sulla pagina aperta, e
    // prendono il suo id da sole.

    fun setFavoriteFor(pageId: String, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(pageId, favorite) }
    }

    fun renamePage(pageId: String, title: String) {
        viewModelScope.launch { repository.renamePage(pageId, title) }
    }

    /** L'icona della pagina collegata. Il file vecchio lo cancella chi chiama, come per `setIcon`. */
    fun setIconFor(pageId: String, choice: IconChoice) {
        viewModelScope.launch { repository.setIcon(pageId, choice) }
    }

    /** "Lock database" dal menu del blocco. */
    fun setLockedFor(pageId: String, locked: Boolean) {
        viewModelScope.launch { repository.setLocked(pageId, locked) }
    }

    fun loadMoveExclusionsFor(pageId: String, onReady: (PageRepository.MoveExclusions) -> Unit) {
        viewModelScope.launch { onReady(repository.moveExclusions(pageId)) }
    }

    /**
     * "Move to" dal menu del blocco: il collegamento sparisce da qui e ne
     * nasce uno in fondo alla pagina scelta. Annulla, da questa pagina, la
     * rimette al suo posto (`PageRepository.undoPageChange`).
     */
    fun moveLinkedPageTo(pageId: String, destination: PageRepository.PageTreeNode, asInline: Boolean = false) {
        viewModelScope.launch {
            historyLock.withLock {
                repository.movePageTo(pageId, destination, asInline)?.let { pushPageChange(it) }
            }
        }
    }

    /**
     * "Move to database" dal menu del blocco (dal 05/10/2026): il
     * collegamento sparisce da qui e la pagina diventa una pagina del
     * database scelto. Annulla, da questa pagina, la rimette al suo posto.
     */
    fun moveLinkedPageToDatabase(pageId: String, destination: PageRepository.PageTreeNode, rowTitle: String) {
        val databaseId = destination.pageId ?: return
        viewModelScope.launch {
            historyLock.withLock {
                repository.moveIntoDatabase(pageId, databaseId, rowTitle)?.let { pushPageChange(it) }
            }
        }
    }

    /** "Move to trash" dal menu del blocco. Annulla la tira fuori dal cestino e la rimette qui. */
    fun moveLinkedPageToTrash(pageId: String) {
        viewModelScope.launch {
            historyLock.withLock {
                repository.moveToTrash(pageId)?.let { pushPageChange(it) }
            }
        }
    }

    /**
     * "Duplicate" dal menu del blocco, per una pagina o un database: la
     * copia, "accanto all'originale", nasce sotto **questo** collegamento.
     * `onDone` riceve l'id della copia.
     */
    fun duplicateLinkedPage(
        pageId: String,
        besideBlockId: String,
        target: PageRepository.DuplicateTarget,
        titleSuffix: String,
        imageStore: PageImageStore,
        onDone: (String) -> Unit
    ) {
        viewModelScope.launch {
            historyLock.withLock {
                repository.duplicatePage(
                    pageId,
                    target,
                    titleSuffix,
                    copyImage = { imageStore.copy(it) },
                    besideLinkBlockId = besideBlockId
                )?.let { copyId ->
                    // Annulla, da qui, manda la copia nel cestino.
                    pushPageChange(repository.describeCopy(copyId))
                    onDone(copyId)
                }
            }
        }
    }

    // --- Template (dal 26/09/2026, vedi `PageEntity.isTemplate`) ---

    /**
     * Scrive nella pagina tutto quello che c'è nel template scelto (vedi
     * `PageRepository.applyTemplate`). Annulla lo toglie, come ogni
     * cambiamento della struttura della pagina.
     */
    fun applyTemplate(templateId: String, imageStore: PageImageStore) {
        val pageId = currentPageId ?: return
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                repository.applyTemplate(templateId, pageId) { imageStore.copy(it) }
            }
        }
    }

    /** Un template nuovo e vuoto; `onCreated` riceve il suo id, per aprirlo e scriverlo. */
    fun createTemplate(onCreated: (String) -> Unit) {
        viewModelScope.launch { onCreated(repository.createTemplate()) }
    }

    // --- Viste collegate ("Linked view of data source") ---

    /** I database fra cui scegliere quello da mostrare in una vista collegata. */
    fun loadLinkableDatabases(onReady: (List<PageRepository.LinkableDatabase>) -> Unit) {
        viewModelScope.launch { onReady(repository.linkableDatabases()) }
    }

    /**
     * La riga diventa una **vista collegata** del database `sourceId`: lo
     * stesso database, con impostazioni di vista sue (vedi
     * `PageRepository.createLinkedView`).
     *
     * `keepText`, da "Turn into" nel menu del blocco: se la riga ha del
     * testo, il testo resta dov'è e la vista nasce subito sotto — una
     * vista non ha un nome suo in cui metterlo (mostra quello del
     * database). Dai menu "/" e "+" la riga stessa diventa la vista, come
     * per gli altri database.
     */
    fun convertToLinkedView(block: BlockEntity, sourceId: String, keepText: Boolean) {
        val current = _blocks.value.find { it.id == block.id } ?: block
        val below = keepText && plainTextOf(current).isNotBlank()
        val liftChildren = !below && hasChildren(block.id)
        snapshotForStructuralChange()
        viewModelScope.launch {
            structuralEdits.withLock {
                val viewId = repository.createLinkedView(sourceId) ?: return@withLock
                if (below) {
                    repository.insertLinkBelow(block.id, viewId, BlockType.DATABASE_LINK)
                } else {
                    // Vedi `updateBlockType`: i figli di un toggle escono prima.
                    if (liftChildren) repository.unnestChildren(block.id)
                    blocksDb.setBlockTypeAndLink(block.id, BlockType.DATABASE_LINK, viewId)
                }
            }
        }
    }

    /** "Turn into database" dal menu di un collegamento a un database: torna dentro la pagina. */
    fun turnLinkIntoDatabase(block: BlockEntity) {
        snapshotForStructuralChange()
        viewModelScope.launch { repository.turnPageLinkIntoDatabase(block.id) }
    }

    /** Quante pagine di riga con del contenuto "Turn into simple database" cancellerebbe. */
    fun countRowPagesWithContent(databasePageId: String, onReady: (Int) -> Unit) {
        viewModelScope.launch { onReady(repository.countRowPagesWithContent(databasePageId)) }
    }

    /** Vedi `PageRepository.turnIntoSimpleDatabase`: le pagine delle righe se ne vanno per sempre. */
    fun turnIntoSimpleDatabase(databasePageId: String, imageStore: PageImageStore) {
        viewModelScope.launch {
            repository.turnIntoSimpleDatabase(databasePageId).forEach { imageStore.delete(it) }
        }
    }

    fun turnIntoComplexDatabase(databasePageId: String) {
        viewModelScope.launch { repository.turnIntoComplexDatabase(databasePageId) }
    }

    /**
     * Lettura una tantum (non un Flow osservato in continuo) del titolo
     * e icona della pagina collegata a un blocco PAGE_LINK/DATABASE_LINK
     * — sufficiente per mostrarli nella riga del blocco: si aggiornano
     * comunque ogni volta che si riapre la pagina che li contiene.
     */
    suspend fun getPageInfo(pageId: String): PageEntity? = repository.getPage(pageId)
}

/** Cosa fare con un blocco-file: vedi `PageEditorViewModel.mediaRequest`. */
/**
 * Cosa fare con un blocco-file. `RESIZE` e `FULL_RESIZE` (dal 20/10/2026, per le
 * immagini): le maniglie compaiono solo quando le si chiede dal menu.
 */
enum class MediaRequestKind { PICK, VIEW, DOWNLOAD, OPEN_ELSEWHERE, RESIZE, FULL_RESIZE, RECORD }

data class MediaRequest(val blockId: String, val kind: MediaRequestKind)

/** Una versione da vedere in "Page history": il contenuto e i titoli delle pagine collegate. */
data class VersionPreview(val content: VersionContent, val linkedTitles: Map<String, String>)
