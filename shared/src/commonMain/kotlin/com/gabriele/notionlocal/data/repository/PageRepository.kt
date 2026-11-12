package com.gabriele.notionlocal.data.repository

import com.gabriele.notionlocal.data.symbolField
import com.gabriele.notionlocal.data.withTransaction
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.IconChoice
import com.gabriele.notionlocal.data.iconField
import com.gabriele.notionlocal.data.imageField
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.data.textStatsOf
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.copiedLinkOptions
import com.gabriele.notionlocal.data.entity.encodeRelationIds
import com.gabriele.notionlocal.data.entity.relationConfig
import com.gabriele.notionlocal.data.entity.relationIds
import com.gabriele.notionlocal.data.entity.encodeColumnWidths
import com.gabriele.notionlocal.data.entity.encodeSortRules
import com.gabriele.notionlocal.data.entity.encodeGroupRulesMapped
import com.gabriele.notionlocal.data.entity.parseSortRules
import com.gabriele.notionlocal.data.entity.encodeFitColumns
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.entity.parseFitColumns
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.data.entity.PersonalWordEntity
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.data.entity.splitLines
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.attrsForNewLine
import com.gabriele.notionlocal.data.entity.firstEmptyLine
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.entity.MULTILINE_TYPES
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.ink.remapAnchor
import com.gabriele.notionlocal.data.security.AppLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.gabriele.notionlocal.data.entity.PageVersionEntity
import com.gabriele.notionlocal.data.entity.PageVersionInfo
import com.gabriele.notionlocal.data.entity.VersionReason
import com.gabriele.notionlocal.data.export.ExportPage
import com.gabriele.notionlocal.data.entity.PAGE_LINK_PREFIX
import com.gabriele.notionlocal.data.entity.mentionedPageIds
import com.gabriele.notionlocal.data.entity.pageLink
import com.gabriele.notionlocal.data.entity.refreshPageMentions
import com.gabriele.notionlocal.data.export.subpageIds
import com.gabriele.notionlocal.data.history.VersionCodec
import com.gabriele.notionlocal.data.history.VersionContent
import com.gabriele.notionlocal.data.history.VersionRetention
import com.gabriele.notionlocal.data.history.toEntity
import com.gabriele.notionlocal.data.notify.ReminderAlarms
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import com.gabriele.notionlocal.data.sync.SyncedBlocks
import com.gabriele.notionlocal.data.sync.SyncedLine

/**
 * Punto unico di accesso ai dati di pagine, blocchi e celle delle
 * tabelle semplici. I ViewModel parlano con il Repository, mai
 * direttamente con i DAO — così se un domani cambiamo come sono
 * salvati i dati, la UI non deve saperlo.
 */
/** Quante pagine al massimo risale `PageRepository.ancestorsOf`. */
private const val MAX_ANCESTORS = 16

class PageRepository(private val db: AppDatabase) {

    /** Il database dell'app, per chi lavora fra pagine e righe insieme: i bottoni (`ButtonRunner`). */
    val database: AppDatabase get() = db

    private val pageDao = db.pageDao()
    private val blockDao = db.blockDao()
    private val tableCellDao = db.tableCellDao()
    private val pageEditDao = db.pageEditDao()

    /**
     * Per le scritture che devono sopravvivere alla schermata che le ha
     * chieste — oggi solo la cronologia delle modifiche. Il repository
     * vive quanto l'app, quindi questo scope non è "per sempre": è
     * lungo quanto il processo, e non c'è niente da annullare.
     */
    private val historyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Una versione alla volta, per tutta l'app: due salvataggi insieme farebbero due versioni uguali. */
    private val versionLock get() = VERSION_LOCK

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        val VERSION_LOCK = Mutex()

        /** Il titolo che una nota al volo prende dalla sua prima riga, al massimo. */
        const val QUICK_NOTE_TITLE_MAX = 80

        /** Oltre, una versione non si salva: vedi `saveVersion`. */
        const val MAX_VERSION_BYTES = 1_500_000

        /** Quanto testo della riga che menziona una pagina si vede nei suoi backlink. */
        const val BACKLINK_SNIPPET_MAX = 140

        /** Quante pagine propone il menu "@". */
        const val MENTION_CANDIDATES = 8

        /** Quanti livelli di sottopagine si esportano al massimo: un albero infinito non si scrive. */
        const val EXPORT_MAX_DEPTH = 12
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val DEFAULT_PAGE_EMOJI = "📄"
        // Quanto testo mostrare intorno alla parola trovata: meno prima,
        // di più dopo, perché si legge da sinistra a destra.
        const val SNIPPET_BEFORE = 30
        const val SNIPPET_AFTER = 70

        /** Davanti a un pezzo di testo trovato in una foto, nei risultati della ricerca. */
        const val PHOTO_MARK = "🖼 "

        /** Le righe che la prima pagina scansionata prende, se sono vuote: le altre restano e le pagine vanno sotto. */
        val SCAN_REPLACES = setOf(BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM)
    }

    fun getRootPages(): Flow<List<PageEntity>> = pageDao.getRootPages()

    // --- Template (dal 26/09/2026, vedi `PageEntity.isTemplate`) ---

    /** I template, per la voce "Templates" della barra laterale e per le pagine nuove. */
    fun observeTemplates(): Flow<List<PageEntity>> = pageDao.observeTemplates()

    /** Un template nuovo, vuoto e senza nome. Restituisce il suo id, per aprirlo e scriverlo. */
    suspend fun createTemplate(): String {
        val template = PageEntity(title = "", isTemplate = true)
        pageDao.insert(template)
        return template.id
    }

    /** La riga di cui la pagina è la pagina (`isRowPage`), seguita: null per le altre. */
    fun observeRowOfPage(pageId: String): Flow<DatabaseRowEntity?> =
        db.databaseDao().observeRowLinkingTo(pageId)

    /**
     * **Scrive in una pagina tutto quello che c'è in un template**: i
     * blocchi, coi loro figli, le celle delle tabelle e le sottopagine,
     * che si copiano anche loro (come fa "Duplicate": due collegamenti
     * alla stessa sottopagina non sarebbero una copia, e scrivendo nella
     * pagina si cambierebbe il template).
     *
     * Se la pagina ha solo righe vuote — è il caso per cui si sceglie un
     * template, appena creata — quelle se ne vanno e il template prende il
     * loro posto; altrimenti il template si aggiunge in fondo. Il titolo, le
     * proprietà e l'icona della pagina non si toccano.
     */
    suspend fun applyTemplate(
        templateId: String,
        pageId: String,
        copyImage: suspend (String) -> String?
    ): Boolean {
        if (templateId == pageId) return false
        pageDao.getById(templateId) ?: return false
        return db.withTransaction {
            val existing = blockDao.getBlocksForPageOnce(pageId)
            val onlyEmptyLines = existing.all { block ->
                block.type == BlockType.PARAGRAPH && block.parentBlockId == null &&
                    runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }
                        .getOrNull()?.plainText().isNullOrEmpty()
            }
            val startAt = if (onlyEmptyLines) {
                existing.forEach { blockDao.delete(it) }
                0
            } else {
                (existing.filter { it.parentBlockId == null }.maxOfOrNull { it.orderIndex } ?: -1) + 1
            }

            val copies = mutableMapOf<String, String>()
            // La pagina stessa è già "copiata": se il template la richiamasse
            // (un collegamento verso la pagina in cui lo si usa), il
            // collegamento punterebbe a lei invece di copiarla.
            copies[pageId] = pageId
            val blocks = inDocumentOrder(blockDao.getBlocksForPageOnce(templateId))
            val newBlockIds = blocks.associate { it.id to UUID.randomUUID().toString() }
            val copied = blocks.map { block ->
                block.copy(
                    id = newBlockIds.getValue(block.id),
                    pageId = pageId,
                    parentBlockId = block.parentBlockId?.let { newBlockIds[it] },
                    orderIndex = if (block.parentBlockId == null) startAt + block.orderIndex else block.orderIndex,
                    linkedPageId = block.linkedPageId?.let { copyLinkedPage(it, copies, copyImage) },
                    attrsJson = copiedAttrs(block, copyImage)
                )
            }
            blockDao.insertAll(copied)
            blocks.filter { it.type == BlockType.TABLE }.forEach { table ->
                tableCellDao.getCellsForBlockOnce(table.id).forEach { cell ->
                    tableCellDao.insertCell(
                        cell.copy(id = UUID.randomUUID().toString(), blockId = newBlockIds.getValue(table.id))
                    )
                }
            }
            true
        }
    }

    fun getChildPages(parentId: String): Flow<List<PageEntity>> =
        pageDao.getChildPages(parentId)

    suspend fun getPage(pageId: String): PageEntity? = pageDao.getById(pageId)

    /**
     * Quanto testo ha scritto l'utente in una pagina: la voce "X words"
     * del menu dei tre puntini. Le regole del conteggio stanno in
     * `TextStats`; qui si decide **che cosa** si conta.
     *
     * Si conta il testo dei blocchi — paragrafi, titoli, elenchi, caselle,
     * toggle **e quello che c'è dentro ai toggle**, anche chiusi — e
     * quello delle celle delle tabelle semplici. Resta fuori tutto quello
     * che l'utente non ha scritto in questa pagina, come ha chiesto:
     *  - **i database**, righe e celle comprese: sono un'altra cosa, anche
     *    quando stanno dentro la pagina;
     *  - i blocchi che rimandano a un'altra pagina o a un database: il
     *    nome che mostrano è quello dell'altra pagina;
     *  - **il titolo della pagina**: non è nel corpo del testo, e spesso
     *    non l'ha scritto nessuno — le pagine nuove nascono con un nome
     *    messo dall'app ("Senza titolo", "Untitled").
     *
     * Una "riga" è un blocco con del testo, come ha chiesto l'utente
     * ("in realtà quanti blocchi"): i blocchi vuoti non contano, e una
     * tabella conta una volta sola, qualunque sia il numero di celle.
     * Delle tabelle si guardano solo le celle che si vedono: rimpicciolita
     * una tabella, le celle rimaste fuori restano salvate ma non si
     * leggono più.
     */
    suspend fun textStats(pageId: String): TextStats {
        var total = TextStats()
        for (block in blockDao.getBlocksForPageOnce(pageId)) {
            when (block.type) {
                BlockType.TABLE -> {
                    val cells = tableCellDao.getCellsForBlockOnce(block.id).filter {
                        it.rowIndex < block.tableRows &&
                            it.colIndex < block.tableCols &&
                            it.text.isNotBlank()
                    }
                    if (cells.isEmpty()) continue
                    total += cells.fold(TextStats()) { sum, cell -> sum + textStatsOf(cell.text) }
                        .copy(lines = 1)
                }
                BlockType.PAGE_LINK, BlockType.DATABASE_LINK, BlockType.DIVIDER -> Unit
                else -> {
                    val text = runCatching {
                        json.decodeFromString<List<RichTextSpan>>(block.textJson)
                    }.getOrNull()?.plainText().orEmpty()
                    if (text.isBlank()) continue
                    total += textStatsOf(text).copy(lines = 1)
                }
            }
        }
        return total
    }

    /** La pagina seguita nel tempo: vedi `PageDao.observeById`. */
    fun observePage(pageId: String): Flow<PageEntity?> = pageDao.observeById(pageId)

    fun observePages(pageIds: List<String>): Flow<List<PageEntity>> = pageDao.observeByIds(pageIds)

    /** Le pagine e i database messi fra i preferiti, tranne quelli nel cestino. */
    fun observeFavorites(): Flow<List<PageEntity>> = pageDao.observeFavorites()

    /**
     * Mette o toglie una pagina dai preferiti, segnando quando.
     *
     * La pagina radice non si può mettere: è quella da cui si parte
     * sempre, e un collegamento a lei non porterebbe da nessuna parte.
     * La UI non offre nemmeno il pulsante, ma il divieto sta anche qui:
     * una regola che vive in un posto solo prima o poi viene aggirata
     * da una strada nuova.
     */
    suspend fun setFavorite(pageId: String, favorite: Boolean) {
        if (pageId == PageEntity.ROOT_PAGE_ID) return
        pageDao.setFavorite(pageId, favorite, if (favorite) System.currentTimeMillis() else null)
    }

    /** L'icona di una pagina: un'emoji, un'immagine o niente (il 📄 di riserva). */
    suspend fun setIcon(pageId: String, choice: IconChoice) =
        pageDao.setIcon(pageId, choice.iconField, choice.imageField, choice.symbolField)

    /** Blocca o sblocca il **contenuto** di una pagina. */
    suspend fun setLocked(pageId: String, locked: Boolean) =
        pageDao.setLocked(pageId, locked)

    /** Come è inquadrata la copertina nelle schede della galleria: vedi `PageEntity.cardCoverScale`. */
    suspend fun setCardCoverTransform(pageId: String, scale: Float, offsetX: Float, offsetY: Float) =
        pageDao.setCardCoverTransform(pageId, scale, offsetX, offsetY)

    /** Blocca o sblocca **l'impaginazione** di un database (vista, ordine, filtro, colonne). */
    suspend fun setViewLocked(pageId: String, locked: Boolean) =
        pageDao.setViewLocked(pageId, locked)

    /**
     * Butta una pagina nel cestino.
     *
     * La pagina **non viene cancellata**: resta nel database con tutto
     * il suo contenuto e si segna solo quando è stata buttata. Quello
     * che sparisce davvero sono i blocchi che la richiamavano nelle
     * altre pagine — è lì che vive l'albero di navigazione, quindi
     * togliere quelli è ciò che la fa sparire da dov'era.
     *
     * Da lì la si ritrova nel Cestino della barra laterale, che la
     * ripristina o la cancella per davvero.
     */
    suspend fun moveToTrash(pageId: String): PageChange.Trashed? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        return db.withTransaction {
            val links = blockDao.getBlocksLinkingTo(pageId)
            pageDao.setTrashedAt(pageId, System.currentTimeMillis())
            links.forEach { blockDao.delete(it) }
            // Restituito per Annulla: dove stavano i collegamenti tolti.
            PageChange.Trashed(pageId, links)
        }
    }

    // --- Cestino ---

    /** Le pagine nel cestino, dall'ultima buttata. */
    fun observeTrash(): Flow<List<PageEntity>> = pageDao.observeTrash()

    /**
     * Tira fuori una pagina dal cestino e la rimette **in fondo al menu
     * principale**: il posto da cui era stata tolta non c'è più (il suo
     * collegamento è stato cancellato buttandola), e in fondo alla pagina
     * da cui si parte sempre la si ritrova di sicuro.
     *
     * Le pagine di una riga di database fanno eccezione: il loro posto è
     * la riga, che le ha sempre tenute, e un collegamento in più nel menu
     * sarebbe un doppione. Se però la riga non c'è più (le pagine rimaste
     * da righe cancellate, vedi `tidyOrphanRowPages`), la pagina torna una
     * pagina normale, nel menu principale: altrimenti non la si troverebbe.
     */
    suspend fun restoreFromTrash(pageId: String) {
        val page = pageDao.getById(pageId) ?: return
        db.withTransaction {
            pageDao.setTrashedAt(pageId, null)
            val rowGone = page.isRowPage && db.databaseDao().getRowsLinkingTo(pageId).isEmpty()
            if (rowGone) pageDao.setRowPage(pageId, false)
            // Un template torna fra i template, non nel menu principale.
            if ((!page.isRowPage || rowGone) && !page.isTemplate && blockDao.getBlocksLinkingTo(pageId).isEmpty()) {
                appendLink(PageEntity.ROOT_PAGE_ID, pageId, page.isDatabase)
            }
        }
    }

    /**
     * **La pagina di una riga appena cancellata se ne va con lei** (dal
     * 08/10/2026, chiesto dall'utente): è quello che dice la conferma,
     * "Delete this page? … This can't be undone". Prima restava nel
     * database, senza più un posto, e la ricerca la trovava ancora. Se
     * un'altra riga la apre ancora (una vista collegata), resta.
     *
     * Restituisce i file da cancellare, come `deletePermanently`.
     */
    suspend fun deleteRowPage(pageId: String): List<String> {
        if (db.databaseDao().getRowsLinkingTo(pageId).isNotEmpty()) return emptyList()
        if (pageDao.getById(pageId)?.isRowPage != true) return emptyList()
        return deletePermanently(pageId)
    }

    /**
     * **Le pagine rimaste da righe cancellate prima del 08/10/2026**: si
     * fa all'avvio. Non si cancellano: vanno **nel cestino**, dove si
     * possono guardare e ripristinare (tornano pagine normali, vedi
     * `restoreFromTrash`) e da dove se ne vanno con la regola del cestino.
     * Quelle che un'altra pagina richiama ancora diventano pagine normali
     * lì dove sono. Restituisce quante ne sono andate nel cestino.
     */
    suspend fun tidyOrphanRowPages(): Int = db.withTransaction {
        val now = System.currentTimeMillis()
        var trashed = 0
        for (page in pageDao.orphanRowPages()) {
            if (blockDao.getBlocksLinkingTo(page.id).isNotEmpty()) {
                pageDao.setRowPage(page.id, false)
            } else {
                pageDao.setTrashedAt(page.id, now)
                trashed++
            }
        }
        trashed
    }

    /**
     * Cancella una pagina **per davvero**, con le pagine che contiene.
     *
     * "Contiene" vuol dire le pagine richiamate dai suoi collegamenti e, se
     * è un database, quelle aperte dalle sue righe — e poi quelle dentro
     * di loro, giù fino in fondo. Ma **solo se non sono richiamate anche
     * da fuori**: una sottopagina collegata pure da un'altra parte non è
     * sua soltanto, e cancellandola sparirebbe anche dall'altra.
     *
     * Restituisce i nomi dei file (icone e copertine, e dal 06/10/2026 i
     * file dei blocchi: immagini, video, audio, file) delle
     * pagine cancellate, che chi chiama toglie dalla cartella: il
     * repository non sa dov'è.
     */
    suspend fun deletePermanently(pageId: String): List<String> {
        if (pageId == PageEntity.ROOT_PAGE_ID) return emptyList()
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val doomed = linkedSetOf(pageId)
            val queue = ArrayDeque(listOf(pageId))
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val page = pageDao.getById(current) ?: continue
                val children = buildList {
                    blockDao.getBlocksForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                    if (page.isDatabase) {
                        databaseDao.getRowsForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                    }
                }.distinct()
                for (child in children) {
                    if (child in doomed || child == PageEntity.ROOT_PAGE_ID) continue
                    val linkedFromOutside = blockDao.getBlocksLinkingTo(child).any { it.pageId !in doomed }
                    val openedFromOutside = databaseDao.getRowsLinkingTo(child).any { it.pageId !in doomed }
                    if (!linkedFromOutside && !openedFromOutside) {
                        doomed += child
                        queue += child
                    }
                }
            }
            // Le viste in più dei database che se ne vanno, con loro.
            doomed.toList().forEach { id -> pageDao.getViewsOnce(id).forEach { doomed += it.id } }
            val pages = pageDao.getByIds(doomed.toList())
            // I file dei blocchi (immagini, video, file, icone dei riquadri):
            // letti prima, perché i blocchi se ne vanno con le loro pagine.
            val blockFiles = doomed.flatMap { id -> blockDao.getBlocksForPageOnce(id).flatMap { it.attrs.files } }
            // Se la pagina non era nel cestino, qualcuno la richiama
            // ancora: quei collegamenti porterebbero a una pagina che non
            // c'è più.
            doomed.forEach { id -> blockDao.getBlocksLinkingTo(id).forEach { blockDao.delete(it) } }
            pages.forEach { pageDao.delete(it) }
            // Le copie di un blocco sincronizzato hanno gli stessi file (dal
            // 03/11/2026): uno ancora usato da una copia in un'altra pagina resta.
            val gone = blockFiles.distinct().filter { blockDao.countWithAttrs(SyncedBlocks.patternOf(it)) == 0 }
            if (gone.isNotEmpty()) db.imageTextDao().delete(gone)
            pages.flatMap { listOfNotNull(it.iconImage, it.coverImage) } + gone
        }
    }

    /**
     * Toglie per sempre dal cestino quello che la regola scelta dice di
     * togliere: le pagine buttate da più di trenta giorni, o tutte.
     * Si chiama all'avvio dell'app. Restituisce i file immagine da
     * cancellare, come `deletePermanently`.
     */
    suspend fun purgeTrash(emptyAll: Boolean, retentionDays: Int): List<String> {
        val ids = if (emptyAll) {
            pageDao.getAllTrashedIds()
        } else {
            pageDao.getTrashedBefore(System.currentTimeMillis() - retentionDays * DAY_MILLIS)
        }
        return ids.flatMap { deletePermanently(it) }
    }

    /** Svuota il cestino adesso. */
    suspend fun emptyTrash(): List<String> = pageDao.getAllTrashedIds().flatMap { deletePermanently(it) }

    // --- L'albero delle pagine (barra laterale) ---

    /**
     * Un nodo dell'albero: una pagina, un database, o la riga di un
     * database che non è ancora mai stata aperta come pagina (e quindi
     * una pagina sua non ce l'ha: nasce la prima volta che la si tocca).
     */
    data class PageTreeNode(
        val pageId: String?,
        val rowId: String?,
        val title: String,
        val emoji: String,
        val iconImage: String?,
        val isDatabase: Boolean,
        val hasChildren: Boolean,
        /** L'icona di Material, se ne ha una (vedi `PageEntity.iconSymbol`). */
        val iconSymbol: String? = null,
        /** Un database semplice: le sue righe non hanno pagine, e dentro non ci si sposta niente. */
        val isSimpleDatabase: Boolean = false
    ) {
        /** Chi è, per ricordarsi se è aperto: la pagina, o la riga se la pagina non c'è. */
        val key: String get() = pageId ?: "row:$rowId"
    }

    suspend fun treeRoot(): PageTreeNode? =
        pageDao.getById(PageEntity.ROOT_PAGE_ID)?.let { nodeFor(it) }

    /**
     * I figli di una pagina nell'albero, nell'ordine in cui compaiono.
     *
     * L'albero non sta in una colonna "genitore": vive nei collegamenti
     * dentro il contenuto delle pagine, e per i database nelle loro righe.
     * Quindi i figli di una pagina sono i suoi collegamenti, **nell'ordine
     * del testo** — anche quelli dentro un toggle, al punto in cui stanno —
     * e i figli di un database sono le sue righe.
     */
    suspend fun treeChildren(pageId: String): List<PageTreeNode> {
        val page = pageDao.getById(pageId) ?: return emptyList()
        // Bloccata con la password e non aperta: cosa contiene non si vede.
        if (page.isProtected && !AppLock.isUnlocked(page.id)) return emptyList()
        // Le righe di un database semplice non sono pagine: nell'albero
        // il database è una foglia, e le righe si guardano aprendolo.
        if (page.isSimpleDatabase) return emptyList()
        if (page.isDatabase) {
            return db.databaseDao().getRowsForPageOnce(pageId).mapNotNull { row ->
                val linked = row.linkedPageId?.let { pageDao.getById(it) }
                if (linked?.trashedAt != null) return@mapNotNull null
                if (linked != null) {
                    nodeFor(linked).copy(title = row.title.ifBlank { linked.title })
                } else {
                    PageTreeNode(
                        pageId = null,
                        rowId = row.id,
                        title = row.title,
                        emoji = DEFAULT_PAGE_EMOJI,
                        iconImage = null,
                        isDatabase = false,
                        hasChildren = false
                    )
                }
            }
        }
        val blocks = blockDao.getBlocksForPageOnce(pageId)
        return inDocumentOrder(blocks)
            .filter { it.type == BlockType.PAGE_LINK || it.type == BlockType.DATABASE_LINK }
            .mapNotNull { it.linkedPageId }
            .distinct()
            // Le viste collegate non sono pagine a sé: sono un altro modo di
            // guardare un database che nell'albero c'è già, dove sta.
            .mapNotNull { id -> pageDao.getById(id)?.takeIf { it.trashedAt == null && it.sourceDatabaseId == null } }
            .map { nodeFor(it) }
    }

    /**
     * **Il percorso di una pagina**, per la barra in alto del PC (dal
     * 11/11/2026, come il percorso in cima alle pagine di Notion): le pagine
     * che la contengono, dalla più esterna, senza lei. Come l'albero della
     * barra laterale (`treeChildren`) si risale dai collegamenti: una pagina
     * sta dentro quella che ha un suo collegamento (il primo, se sono più
     * d'uno), la pagina di una riga sta dentro il suo database. Si ferma
     * alla pagina principale, a una pagina nel cestino o a un giro chiuso.
     */
    suspend fun ancestorsOf(pageId: String): List<PageEntity> {
        val path = ArrayList<PageEntity>()
        val seen = hashSetOf(pageId)
        var current = pageDao.getById(pageId) ?: return emptyList()
        while (current.id != PageEntity.ROOT_PAGE_ID && path.size < MAX_ANCESTORS) {
            val parentId = if (current.isRowPage) {
                db.databaseDao().getRowsLinkingTo(current.id).firstOrNull()?.pageId
            } else {
                blockDao.getBlocksLinkingTo(current.id)
                    .filter { it.type == BlockType.PAGE_LINK || it.type == BlockType.DATABASE_LINK }
                    .map { it.pageId }
                    .firstOrNull { it !in seen }
            } ?: break
            if (!seen.add(parentId)) break
            val parent = pageDao.getById(parentId)?.takeIf { it.trashedAt == null } ?: break
            path += parent
            current = parent
        }
        return path.reversed()
    }

    /** Le pagine fra cui scegliere quella da aprire all'avvio: tutte, tranne il cestino e la principale. */
    suspend fun startupCandidates(): List<PageEntity> = pageDao.getAllLivePages().filter { it.viewHostId == null }

    /**
     * Crea la pagina di una riga che non l'ha ancora, come fa il pulsante OPEN.
     * Pagina e riga cambiano insieme (dal 08/10/2026): in mezzo la pagina
     * sarebbe di una riga che non la apre, e `tidyOrphanRowPages` la
     * scambierebbe per una rimasta da una riga cancellata.
     */
    suspend fun ensureRowPage(rowId: String): String? = db.withTransaction { ensureRowPageNow(rowId) }

    private suspend fun ensureRowPageNow(rowId: String): String? {
        val databaseDao = db.databaseDao()
        val row = databaseDao.getRowById(rowId) ?: return null
        row.linkedPageId?.let { return it }
        // **Le righe di un database semplice non diventano mai pagine**
        // (`PageEntity.isSimpleDatabase`). Le schermate non lo chiedono
        // già da sé, ma questo è l'unico punto da cui una pagina di riga
        // nasce: il divieto sta qui, così nessuna strada nuova — un
        // menu, una ricerca, un "Duplicate" — può aggirarlo per sbaglio.
        if (pageDao.getById(row.pageId)?.isSimpleDatabase == true) return null
        val page = PageEntity(title = row.title.ifBlank { "Untitled" }, isRowPage = true)
        pageDao.insert(page)
        databaseDao.updateRow(row.copy(linkedPageId = page.id))
        return page.id
    }

    private suspend fun nodeFor(page: PageEntity): PageTreeNode = PageTreeNode(
        pageId = page.id,
        rowId = null,
        title = page.title,
        emoji = page.icon,
        iconImage = page.iconImage,
        iconSymbol = page.iconSymbol,
        isDatabase = page.isDatabase,
        isSimpleDatabase = page.isSimpleDatabase,
        hasChildren = if (page.isSimpleDatabase || (page.isProtected && !AppLock.isUnlocked(page.id))) {
            false
        } else if (page.isDatabase) {
            db.databaseDao().countRows(page.id) > 0
        } else {
            blockDao.countLinksInPage(page.id) > 0
        }
    )

    /** I blocchi come si leggono: in fila, e i figli di un toggle subito dopo di lui. */
    private fun inDocumentOrder(blocks: List<BlockEntity>): List<BlockEntity> {
        val byParent = blocks.groupBy { it.parentBlockId }
        val result = mutableListOf<BlockEntity>()
        fun visit(parentId: String?) {
            byParent[parentId].orEmpty().sortedBy { it.orderIndex }.forEach { block ->
                result += block
                visit(block.id)
            }
        }
        visit(null)
        return result
    }

    // --- Ricerca ---

    /**
     * Un risultato della ricerca. `snippet` è il pezzo di testo in cui
     * compare la parola, se la si è trovata nel contenuto; `matchStart` e
     * `matchLength` dicono dove sta dentro quel pezzo, per evidenziarla.
     */
    data class SearchHit(
        val pageId: String?,
        val rowId: String?,
        val isDatabase: Boolean,
        val title: String,
        val emoji: String,
        val iconImage: String?,
        val snippet: String?,
        val matchStart: Int,
        val matchLength: Int,
        val inTrash: Boolean,
        val iconSymbol: String? = null
    )

    /**
     * Cerca nei titoli e, se richiesto, nel contenuto: il testo dei
     * blocchi, le celle delle tabelle semplici e i nomi delle righe dei
     * database. Una pagina compare **una volta sola**, col primo pezzo di
     * testo in cui la parola si trova.
     *
     * Le pagine nel cestino restano fuori, a meno che non si sia chiesto
     * di cercare anche lì.
     */
    suspend fun search(query: String, inContent: Boolean, inTrash: Boolean): List<SearchHit> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val hits = LinkedHashMap<String, SearchHit>()

        // Le pagine dei quaderni non si cercano da qui: sono un mondo a parte
        // (vedi `NotebookEntity`).
        // Nemmeno i template: stanno nella loro voce della barra laterale.
        fun allowed(page: PageEntity) = (page.trashedAt == null || inTrash) &&
            page.notebookSectionId == null && !page.isTemplate

        fun hitFor(page: PageEntity, snippet: Snippet?) = SearchHit(
            pageId = page.id,
            rowId = null,
            isDatabase = page.isDatabase,
            title = page.title,
            emoji = page.icon,
            iconImage = page.iconImage,
            iconSymbol = page.iconSymbol,
            snippet = snippet?.text,
            matchStart = snippet?.start ?: 0,
            matchLength = snippet?.length ?: 0,
            inTrash = page.trashedAt != null
        )

        // Le viste collegate hanno il nome del loro database, e sarebbero
        // state un doppione di quello, che la ricerca trova già.
        pageDao.searchByTitle(needle).filter { allowed(it) && it.sourceDatabaseId == null }.forEach { page ->
            hits[page.id] = hitFor(page, null)
        }

        if (inContent) {
            for (block in blockDao.searchBlocks(needle)) {
                val text = runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }
                    .getOrNull()?.plainText() ?: continue
                val snippet = snippetAround(text, needle) ?: continue
                val existing = hits[block.pageId]
                if (existing?.snippet != null) continue
                val page = pageDao.getById(block.pageId)?.takeIf { allowed(it) } ?: continue
                hits[page.id] = hitFor(page, snippet)
            }
            for (cell in tableCellDao.searchCells(needle)) {
                val snippet = snippetAround(cell.text, needle) ?: continue
                val block = blockDao.getById(cell.blockId) ?: continue
                if (hits[block.pageId]?.snippet != null) continue
                val page = pageDao.getById(block.pageId)?.takeIf { allowed(it) } ?: continue
                hits[page.id] = hitFor(page, snippet)
            }
            // **Il testo delle foto** (dal 03/11/2026): quello che c'è scritto
            // in un'immagine o in un documento scansionato, letto da `ImageTexts`.
            for (entry in db.imageTextDao().search(needle)) {
                val snippet = snippetAround(entry.text.replace('\n', ' '), needle) ?: continue
                for (block in blockDao.getBlocksWithAttrs(SyncedBlocks.patternOf(entry.file))) {
                    if (block.type != BlockType.IMAGE || block.attrs.file != entry.file) continue
                    if (hits[block.pageId]?.snippet != null) continue
                    val page = pageDao.getById(block.pageId)?.takeIf { allowed(it) } ?: continue
                    hits[page.id] = hitFor(page, snippet.copy(text = PHOTO_MARK + snippet.text, start = snippet.start + PHOTO_MARK.length))
                }
            }
            for (row in db.databaseDao().searchRows(needle)) {
                val database = pageDao.getById(row.pageId)?.takeIf { allowed(it) } ?: continue
                // Una riga di un database semplice non ha una pagina da
                // aprire: il risultato porta **al database**, e sotto il
                // nome della riga dice in quale. `rowId` resta, perché
                // due righe dello stesso database sono due risultati.
                if (database.isSimpleDatabase) {
                    hits["row:${row.id}"] = SearchHit(
                        pageId = database.id,
                        rowId = row.id,
                        isDatabase = true,
                        title = row.title,
                        emoji = database.icon,
                        iconImage = database.iconImage,
                        iconSymbol = database.iconSymbol,
                        snippet = database.title,
                        matchStart = 0,
                        matchLength = 0,
                        inTrash = database.trashedAt != null
                    )
                    continue
                }
                val linked = row.linkedPageId?.let { pageDao.getById(it) }
                if (linked != null) {
                    // Il nome della riga è il titolo della sua pagina: se
                    // c'è, l'ha già trovata la ricerca nei titoli.
                    if (allowed(linked) && hits[linked.id] == null) hits[linked.id] = hitFor(linked, null)
                    continue
                }
                hits["row:${row.id}"] = SearchHit(
                    pageId = null,
                    rowId = row.id,
                    isDatabase = false,
                    title = row.title,
                    emoji = DEFAULT_PAGE_EMOJI,
                    iconImage = null,
                    snippet = database.title,
                    matchStart = 0,
                    matchLength = 0,
                    inTrash = database.trashedAt != null
                )
            }
        }
        // **Le pagine bloccate con la password** (dal 26/09/2026): del loro
        // contenuto non si mostra niente. Di una pagina bloccata resta il
        // titolo, se è lui che corrisponde — si vede comunque nella barra
        // laterale; quello che sta dentro (sottopagine, righe) sparisce,
        // come sparisce dalla barra.
        val closedBy = HashMap<String, String?>()
        suspend fun closedBy(id: String): String? = closedBy.getOrPut(id) {
            lockingPageOf(id)?.takeIf { !AppLock.isUnlocked(it) }
        }
        return hits.entries.mapNotNull { (key, hit) ->
            val containerId = if (key.startsWith("row:")) {
                db.databaseDao().getRowById(key.removePrefix("row:"))?.pageId
            } else {
                hit.pageId
            } ?: return@mapNotNull hit
            val lockedBy = closedBy(containerId) ?: return@mapNotNull hit
            when {
                key.startsWith("row:") -> null
                lockedBy == containerId && hit.title.contains(needle, ignoreCase = true) ->
                    hit.copy(snippet = null, matchStart = 0, matchLength = 0)
                else -> null
            }
        }
    }

    private data class Snippet(val text: String, val start: Int, val length: Int)

    /**
     * Il pezzo di testo intorno alla parola trovata: un po' prima, di più
     * dopo, coi puntini dove si è tagliato. Null se nel testo vero la
     * parola non c'è (la query guarda il JSON, che ha anche i nomi dei
     * campi).
     */
    private fun snippetAround(text: String, needle: String): Snippet? {
        val index = text.indexOf(needle, ignoreCase = true)
        if (index < 0) return null
        val from = (index - SNIPPET_BEFORE).coerceAtLeast(0)
        val to = (index + needle.length + SNIPPET_AFTER).coerceAtMost(text.length)
        val prefix = if (from > 0) "…" else ""
        val suffix = if (to < text.length) "…" else ""
        val body = text.substring(from, to).replace('\n', ' ')
        return Snippet(prefix + body + suffix, prefix.length + (index - from), needle.length)
    }

    /**
     * Quello che "Move to" non può offrire come destinazione per una
     * pagina: **lei stessa e tutto quello che contiene**, giù fino in
     * fondo — le pagine richiamate dai suoi collegamenti, le pagine delle
     * righe se è un database, e così via — più le righe mai aperte dei
     * database là dentro, che si scelgono per id di riga e non di pagina.
     *
     * Spostare una pagina dentro una sua sottopagina farebbe un giro
     * chiuso: la sottopagina sta dentro la pagina, la pagina dentro la
     * sottopagina, e nessuna delle due si raggiunge più dal menu
     * principale. Prima l'elenco di "Move to" non lo impediva.
     */
    data class MoveExclusions(
        val pageIds: Set<String>,
        val rowIds: Set<String>,
        /**
         * Se adesso il database si vede **aperto dentro una pagina** (un
         * blocco database, non un collegamento a pagina): "Move as inline"
         * parte acceso (dal 08/10/2026), così spostato si vede com'era.
         */
        val shownInline: Boolean = false
    )

    suspend fun moveExclusions(pageId: String): MoveExclusions {
        val pages = pagesInside(pageId)
        val databaseDao = db.databaseDao()
        val rows = pages
            .mapNotNull { pageDao.getById(it)?.takeIf { page -> page.isDatabase } }
            .flatMap { database -> databaseDao.getRowsForPageOnce(database.id).map { it.id } }
            .toSet()
        val inline = blockDao.getBlocksLinkingTo(pageId).any { it.type == BlockType.DATABASE_LINK }
        return MoveExclusions(pages, rows, inline)
    }

    /** Una pagina e tutte quelle dentro di lei (vedi `moveExclusions`). La principale mai: non sta dentro niente. */
    private suspend fun pagesInside(pageId: String): Set<String> {
        val databaseDao = db.databaseDao()
        val found = linkedSetOf(pageId)
        val queue = ArrayDeque(listOf(pageId))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val page = pageDao.getById(current) ?: continue
            val children = buildList {
                blockDao.getBlocksForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                if (page.isDatabase) {
                    databaseDao.getRowsForPageOnce(current).mapNotNullTo(this) { it.linkedPageId }
                }
            }
            for (child in children) {
                if (child != PageEntity.ROOT_PAGE_ID && found.add(child)) queue += child
            }
        }
        return found
    }

    /**
     * "Move to": la pagina lascia il posto in cui stava e finisce **in
     * fondo** alla pagina scelta — anche se la si sceglie uguale a quella
     * in cui sta già: "in fondo" è la regola, sempre. Restituisce false se
     * lo spostamento non si può fare (vedi sotto) e niente è cambiato.
     *
     * - La destinazione è una pagina, o una **riga** di database: in quel
     *   caso si finisce nella pagina della riga, che se non è mai stata
     *   aperta nasce adesso. Dentro un database no: il suo contenuto sono
     *   righe, non blocchi. Né dentro la pagina stessa o una sua
     *   sottopagina (`moveExclusions`).
     * - **Un database spostato arriva come collegamento a pagina**
     *   (chiesto dall'utente il 24/09/2026; prima si ricreava sempre come
     *   database aperto), **oppure aperto dentro la pagina** se si accende
     *   "Move as inline" (`asInline`, dal 08/10/2026, chiesto dall'utente:
     *   la scelta in fondo alla finestra di "Move to"). Dopo, si cambia con
     *   "Turn into database inline" / "Turn into page".
     * - **La pagina di una riga esce dal suo database**, come su Notion:
     *   la riga si toglie, con i valori delle sue proprietà, e la pagina
     *   diventa una pagina come le altre, con nome e contenuto. Prima
     *   restava anche nel database, cioè in due posti insieme.
     */
    suspend fun movePageTo(pageId: String, destination: PageTreeNode, asInline: Boolean = false): PageChange.Moved? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        val inside = pagesInside(pageId)
        val destinationPageId = when {
            destination.pageId != null -> destination.pageId
            destination.rowId != null -> {
                val row = db.databaseDao().getRowById(destination.rowId) ?: return null
                if (row.pageId in inside) return null
                ensureRowPage(destination.rowId) ?: return null
            }
            else -> return null
        }
        return moveInto(pageId, destinationPageId, inside, asInline)
    }

    /**
     * Lo spostamento vero e proprio, con la destinazione già decisa.
     * Restituisce, per Annulla, tutto quello che serve a rimettere la
     * pagina dov'era: i collegamenti tolti, e la riga con i valori delle
     * sue proprietà se era la pagina di una riga.
     */
    private suspend fun moveInto(pageId: String, destinationPageId: String, inside: Set<String>, asInline: Boolean = false): PageChange.Moved? {
        if (destinationPageId in inside) return null
        val page = pageDao.getById(pageId) ?: return null
        val target = pageDao.getById(destinationPageId) ?: return null
        if (target.isDatabase || target.trashedAt != null) return null
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val links = blockDao.getBlocksLinkingTo(pageId)
            links.forEach { blockDao.delete(it) }
            val rows = if (page.isRowPage) databaseDao.getRowsLinkingTo(pageId) else emptyList()
            val cells = rows.flatMap { databaseDao.getCellsForRowOnce(it.id) }
            if (page.isRowPage) {
                rows.forEach { databaseDao.deleteRow(it) }
                pageDao.setRowPage(pageId, false)
            }
            // Un database arriva aperto dentro la pagina solo se lo si è chiesto.
            val newLinkId = appendLink(destinationPageId, pageId, isDatabase = asInline && page.isDatabase)
            PageChange.Moved(pageId, destinationPageId, links, rows, cells, newLinkId)
        }
    }

    /**
     * **"Move to database"** (dal 05/10/2026, chiesto dall'utente): la
     * pagina lascia il posto in cui stava e diventa **una pagina nuova del
     * database scelto**, in fondo, col suo contenuto e il suo nome.
     *
     * - **Una pagina** diventa la pagina della riga nuova (`isRowPage`):
     *   la si apre dalla riga, e le proprietà del database le compaiono in
     *   cima, vuote.
     * - **Un database** (complesso) non può essere lui la pagina di una
     *   riga — le righe si aprono come pagine di testo —: nasce una pagina
     *   della riga col suo nome e la sua icona, e **dentro, il database
     *   aperto**. È lui, non una copia.
     * - La pagina di una riga lascia il suo database, come con "Move to".
     *
     * **Il nome della pagina nuova** (`rowTitle`, dal 05/10/2026, chiesto
     * dall'utente): lo si sceglie prima di spostare. Per una pagina è il suo
     * nome nuovo (la pagina è lei); per un database è il nome della pagina
     * che lo contiene, e il database tiene il suo. Vuoto o null = quello di
     * adesso.
     *
     * Non si può: verso un database semplice (le sue righe non sono
     * pagine), verso uno che sta dentro la pagina stessa (`pagesInside`),
     * o nel cestino. Una vista collegata vale come il database che mostra.
     * Null se non si fa, e niente è cambiato.
     */
    suspend fun moveIntoDatabase(
        pageId: String,
        databaseId: String,
        rowTitle: String? = null,
        copyImage: suspend (String) -> String? = imageCopier
    ): PageChange.MovedToDatabase? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        val page = pageDao.getById(pageId) ?: return null
        val chosen = pageDao.getById(databaseId)?.takeIf { it.isDatabase } ?: return null
        val target = (chosen.sourceDatabaseId?.let { pageDao.getById(it) } ?: chosen)
            .takeIf { it.isDatabase && !it.isSimpleDatabase && it.trashedAt == null } ?: return null
        val inside = pagesInside(pageId)
        if (target.id in inside || chosen.id in inside) return null
        val databaseDao = db.databaseDao()
        // L'icona fatta di un'immagine: la pagina nuova ne ha una copia sua
        // (due pagine non possono avere lo stesso file).
        val wrapperIcon = if (page.isDatabase) page.iconImage?.let { copyImage(it) } else null
        return db.withTransaction {
            val links = blockDao.getBlocksLinkingTo(pageId)
            links.forEach { blockDao.delete(it) }
            val rows = if (page.isRowPage) databaseDao.getRowsLinkingTo(pageId) else emptyList()
            val cells = rows.flatMap { databaseDao.getCellsForRowOnce(it.id) }
            rows.forEach { databaseDao.deleteRow(it) }
            val now = System.currentTimeMillis()
            val name = rowTitle?.trim()?.takeIf { it.isNotEmpty() } ?: page.title
            // Il nome di prima, per Annulla, se la pagina spostata lo cambia.
            val oldTitle = page.title.takeIf { !page.isDatabase && name != it }
            if (oldTitle != null) pageDao.setTitle(pageId, name, now)
            val rowPageId = if (page.isDatabase) {
                val wrapper = PageEntity(
                    title = name,
                    icon = page.icon,
                    iconImage = wrapperIcon,
                    iconSymbol = page.iconSymbol,
                    isRowPage = true,
                    createdAt = now,
                    updatedAt = now
                )
                pageDao.insert(wrapper)
                appendLink(wrapper.id, pageId, isDatabase = true)
                if (page.isRowPage) pageDao.setRowPage(pageId, false)
                wrapper.id
            } else {
                pageDao.setRowPage(pageId, true)
                pageId
            }
            val order = (databaseDao.getRowsForPageOnce(target.id).maxOfOrNull { it.orderIndex } ?: -1) + 1
            val row = DatabaseRowEntity(
                pageId = target.id,
                title = name.takeUnless { it == "Untitled" }.orEmpty(),
                linkedPageId = rowPageId,
                orderIndex = order,
                createdAt = now,
                updatedAt = now
            )
            databaseDao.insertRow(row)
            PageChange.MovedToDatabase(
                pageId = pageId,
                databaseId = target.id,
                oldLinks = links,
                oldRows = rows,
                oldCells = cells,
                newRowId = row.id,
                wrapperPageId = rowPageId.takeIf { it != pageId },
                rowTitle = rowTitle,
                oldTitle = oldTitle
            )
        }
    }

    // --- Annulla e Ripristina per quello che si fa alle pagine ---

    /**
     * Un cambiamento fatto **alle pagine** — non ai blocchi di quella
     * aperta — che Annulla sa disfare e Ripristina rifare: "Move to",
     * "Duplicate" e "Move to trash", dal menu dei tre puntini o dal menu
     * del blocco.
     *
     * Le foto di Annulla sono i blocchi di una pagina sola, e questi
     * cambiamenti toccano altre pagine (quella che si sposta, quella in
     * cui arriva, la copia); per questo hanno un passo a parte, che si
     * porta dietro quello che serve a tornare indietro. Chiesto
     * dall'utente il 24/09/2026.
     */
    sealed interface PageChange {
        /** Spostata: i collegamenti e le righe che aveva prima, e il collegamento nuovo. */
        data class Moved(
            val pageId: String,
            val destinationPageId: String,
            val oldLinks: List<BlockEntity>,
            val oldRows: List<DatabaseRowEntity>,
            val oldCells: List<DatabaseCellEntity>,
            val newLinkId: String
        ) : PageChange

        /** Duplicata: la copia, e il posto in cui sta — un collegamento, o una riga nuova del database. */
        data class Duplicated(
            val copyId: String,
            val links: List<BlockEntity>,
            val rows: List<DatabaseRowEntity>,
            val cells: List<DatabaseCellEntity>
        ) : PageChange

        /** Buttata nel cestino: i collegamenti che la mostravano. */
        data class Trashed(val pageId: String, val oldLinks: List<BlockEntity>) : PageChange

        /**
         * Spostata in un database ("Move to database", dal 05/10/2026): dov'era
         * prima, la riga nuova, e la pagina nata per contenerla se era un
         * database.
         */
        data class MovedToDatabase(
            val pageId: String,
            val databaseId: String,
            val oldLinks: List<BlockEntity>,
            val oldRows: List<DatabaseRowEntity>,
            val oldCells: List<DatabaseCellEntity>,
            val newRowId: String,
            val wrapperPageId: String?,
            /** Il nome scelto per la pagina nuova (per Ripristina). */
            val rowTitle: String? = null,
            /** Il nome che aveva la pagina spostata, se il nome scelto lo ha cambiato (per Annulla). */
            val oldTitle: String? = null
        ) : PageChange
    }

    /** Dove sta la copia appena fatta da "Duplicate", per poterla disfare. */
    suspend fun describeCopy(copyId: String): PageChange.Duplicated {
        val databaseDao = db.databaseDao()
        val rows = databaseDao.getRowsLinkingTo(copyId)
        return PageChange.Duplicated(
            copyId = copyId,
            links = blockDao.getBlocksLinkingTo(copyId),
            rows = rows,
            cells = rows.flatMap { databaseDao.getCellsForRowOnce(it.id) }
        )
    }

    /**
     * Annulla un cambiamento. Restituisce quello che serve a Ripristina
     * (lo stesso cambiamento, aggiornato a com'è adesso), o null se non
     * c'è più niente da annullare — la pagina nel frattempo è stata
     * cancellata per sempre.
     *
     * - **Spostata** → torna dov'era: al posto del suo collegamento di
     *   prima (dentro il toggle, se era lì), o nella sua riga, con le
     *   proprietà che aveva.
     * - **Duplicata** → la copia va **nel cestino**, come su Notion quando
     *   si toglie una pagina: se nel frattempo ci si è scritto dentro,
     *   niente è perso. Se era una riga nuova del database, la riga si
     *   toglie (e la copia nel cestino diventa una pagina normale, che
     *   ripristinata dal cestino va in fondo al menu principale).
     * - **Buttata** → esce dal cestino e torna al suo posto.
     */
    suspend fun undoPageChange(change: PageChange): PageChange? = db.withTransaction {
        val databaseDao = db.databaseDao()
        when (change) {
            is PageChange.Moved -> {
                pageDao.getById(change.pageId) ?: return@withTransaction null
                blockDao.getBlocksLinkingTo(change.pageId).forEach { blockDao.delete(it) }
                change.oldRows.forEach { row ->
                    if (pageDao.getById(row.pageId) != null) databaseDao.insertRow(row)
                }
                change.oldCells.forEach { cell ->
                    if (databaseDao.getRowById(cell.rowId) != null) databaseDao.insertCell(cell)
                }
                val backInRow = databaseDao.getRowsLinkingTo(change.pageId).isNotEmpty()
                if (backInRow) pageDao.setRowPage(change.pageId, true)
                putLinksBack(change.pageId, change.oldLinks, fallbackToMainMenu = !backInRow)
                change
            }
            is PageChange.Duplicated -> {
                pageDao.getById(change.copyId) ?: return@withTransaction null
                val now = describeCopy(change.copyId)
                now.rows.forEach { databaseDao.deleteRow(it) }
                if (now.rows.isNotEmpty()) pageDao.setRowPage(change.copyId, false)
                now.links.forEach { blockDao.delete(it) }
                pageDao.setTrashedAt(change.copyId, System.currentTimeMillis())
                now
            }
            is PageChange.Trashed -> {
                val page = pageDao.getById(change.pageId) ?: return@withTransaction null
                pageDao.setTrashedAt(change.pageId, null)
                val inRow = page.isRowPage && databaseDao.getRowsLinkingTo(change.pageId).isNotEmpty()
                putLinksBack(change.pageId, change.oldLinks, fallbackToMainMenu = !inRow)
                change
            }
            // Torna dov'era: la riga nuova se ne va. La pagina nata per un
            // database perde il collegamento al database (che torna al suo
            // posto) e, se dentro non c'è altro, se ne va; se nel frattempo
            // ci si è scritto, va nel cestino e niente è perso.
            is PageChange.MovedToDatabase -> {
                pageDao.getById(change.pageId) ?: return@withTransaction null
                databaseDao.getRowById(change.newRowId)?.let { databaseDao.deleteRow(it) }
                val wrapper = change.wrapperPageId?.let { pageDao.getById(it) }
                if (wrapper != null) {
                    blockDao.getBlocksLinkingTo(change.pageId)
                        .filter { it.pageId == wrapper.id }
                        .forEach { blockDao.delete(it) }
                    if (blockDao.getBlocksForPageOnce(wrapper.id).isEmpty()) {
                        pageDao.delete(wrapper)
                    } else {
                        pageDao.setRowPage(wrapper.id, false)
                        pageDao.setTrashedAt(wrapper.id, System.currentTimeMillis())
                    }
                } else {
                    pageDao.setRowPage(change.pageId, false)
                }
                change.oldTitle?.let { pageDao.setTitle(change.pageId, it, System.currentTimeMillis()) }
                change.oldRows.forEach { row ->
                    if (pageDao.getById(row.pageId) != null) databaseDao.insertRow(row)
                }
                change.oldCells.forEach { cell ->
                    if (databaseDao.getRowById(cell.rowId) != null) databaseDao.insertCell(cell)
                }
                val backInRow = databaseDao.getRowsLinkingTo(change.pageId).isNotEmpty()
                if (backInRow) pageDao.setRowPage(change.pageId, true)
                putLinksBack(change.pageId, change.oldLinks, fallbackToMainMenu = !backInRow)
                change
            }
        }
    }

    /** Ripristina un cambiamento annullato. Come `undoPageChange`: null se non si può più. */
    suspend fun redoPageChange(change: PageChange): PageChange? = when (change) {
        is PageChange.Moved -> {
            pageDao.getById(change.pageId)?.let {
                moveInto(change.pageId, change.destinationPageId, pagesInside(change.pageId))
            }
        }
        is PageChange.Duplicated -> db.withTransaction {
            pageDao.getById(change.copyId) ?: return@withTransaction null
            val databaseDao = db.databaseDao()
            pageDao.setTrashedAt(change.copyId, null)
            change.rows.forEach { row ->
                if (pageDao.getById(row.pageId) != null) databaseDao.insertRow(row)
            }
            change.cells.forEach { cell ->
                if (databaseDao.getRowById(cell.rowId) != null) databaseDao.insertCell(cell)
            }
            val inRow = databaseDao.getRowsLinkingTo(change.copyId).isNotEmpty()
            if (inRow) pageDao.setRowPage(change.copyId, true)
            putLinksBack(change.copyId, change.links, fallbackToMainMenu = !inRow)
            change
        }
        is PageChange.Trashed -> moveToTrash(change.pageId)
        is PageChange.MovedToDatabase -> moveIntoDatabase(change.pageId, change.databaseId, change.rowTitle)
    }

    /**
     * Per "Move to database" (e il suo Ripristina): la copia dell'icona
     * fatta di un'immagine, per la pagina nata per un database. Il
     * repository non sa dov'è la cartella delle immagini: la imposta chi ce
     * l'ha (`ViewModelFactory`); senza, la pagina nasce con l'emoji.
     */
    var imageCopier: suspend (String) -> String? = { null }

    /**
     * Rimette i collegamenti a una pagina dov'erano: stessa pagina, stesso
     * toggle, stesso posto fra i fratelli (quelli da lì in giù scalano di
     * uno, così nessuno si ritrova col suo stesso numero d'ordine). Se il
     * toggle non c'è più si mette in fondo alla pagina; se non c'è più
     * nemmeno la pagina si salta. Se alla fine la pagina non è richiamata
     * da nessuna parte, e `fallbackToMainMenu`, va in fondo al menu
     * principale — come quando la si ripristina dal cestino — invece di
     * restare irraggiungibile.
     */
    private suspend fun putLinksBack(pageId: String, links: List<BlockEntity>, fallbackToMainMenu: Boolean) {
        var placed = false
        for (link in links) {
            if (pageDao.getById(link.pageId) == null) continue
            if (blockDao.getById(link.id) != null) {
                placed = true
                continue
            }
            val parentExists = link.parentBlockId?.let { blockDao.getById(it) != null } ?: true
            val block = if (parentExists) {
                link
            } else {
                link.copy(parentBlockId = null, orderIndex = (blockDao.lastOrderIndex(link.pageId) ?: -1) + 1)
            }
            blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex, 1)
            blockDao.insert(block)
            placed = true
        }
        if (!placed && fallbackToMainMenu && blockDao.getBlocksLinkingTo(pageId).isEmpty()) {
            val page = pageDao.getById(pageId) ?: return
            appendLink(PageEntity.ROOT_PAGE_ID, pageId, page.isDatabase)
        }
    }

    /** Dove mettere la copia fatta da "Duplicate". */
    sealed interface DuplicateTarget {
        /**
         * Accanto all'originale: il collegamento subito sotto il suo, o,
         * per la pagina di una riga, una riga nuova subito sotto la sua.
         */
        data object NextToOriginal : DuplicateTarget

        /** In fondo a una pagina. Il menu principale è la pagina radice. */
        data class IntoPage(val pageId: String) : DuplicateTarget

        /** In fondo alla pagina di una riga di database, che se non è mai stata aperta nasce adesso. */
        data class IntoRow(val rowId: String) : DuplicateTarget
    }

    /**
     * Fa una copia di una pagina, con tutto quello che contiene, e la mette
     * dove si è scelto. Restituisce l'id della copia, o null se la pagina, o
     * quella in cui metterla, non c'è più.
     *
     * **Si copia tutto, sottopagine comprese**, come su Notion: le pagine
     * richiamate dai collegamenti, quelle delle righe se è un database, e
     * giù fino in fondo. Prima la copia puntava alle stesse sottopagine
     * dell'originale, e allora non era una copia: scrivendo in una
     * sottopagina della copia cambiava anche l'originale, rinominare la
     * pagina di una riga rinominava la riga in tutti e due i database, e
     * buttarne una nel cestino la toglieva da entrambi.
     *
     * Non si copiano i preferiti e il cestino, che sono scelte fatte su
     * quella pagina lì e non su cosa contiene. Le immagini sì, ognuna col
     * suo file (`copyImage`, che sa dov'è la cartella): due pagine con lo
     * stesso file perderebbero l'immagine insieme appena una la cambia.
     *
     * Solo la copia di partenza prende `titleSuffix` nel titolo; le pagine
     * dentro di lei tengono il loro nome.
     */
    suspend fun duplicatePage(
        pageId: String,
        target: DuplicateTarget,
        titleSuffix: String,
        /**
         * Il collegamento accanto a cui mettere la copia, per
         * `NextToOriginal`: quello su cui si è aperto il menu del blocco.
         * Null dal menu dei tre puntini, dove si usa il primo che si trova
         * — una pagina di solito ne ha uno solo, ma se ne ha due la copia
         * deve nascere sotto quello toccato, non sotto l'altro.
         */
        besideLinkBlockId: String? = null,
        copyImage: suspend (String) -> String?
    ): String? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return null
        val original = pageDao.getById(pageId) ?: return null
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val destinationPageId = when (target) {
                DuplicateTarget.NextToOriginal -> null
                is DuplicateTarget.IntoPage -> target.pageId
                is DuplicateTarget.IntoRow -> ensureRowPage(target.rowId) ?: return@withTransaction null
            }
            // Dentro un database non si mette un collegamento: il suo
            // contenuto sono righe, non blocchi.
            if (destinationPageId != null && pageDao.getById(destinationPageId)?.isDatabase != false) {
                return@withTransaction null
            }

            // Dove sta l'originale, letto prima di copiare. La pagina di
            // una riga copiata accanto a sé resta una riga dello stesso
            // database; messa altrove diventa una pagina come le altre.
            val sourceRow = if (destinationPageId == null && original.isRowPage) {
                databaseDao.getRowsLinkingTo(pageId).firstOrNull()
            } else {
                null
            }
            val sourceLink = if (destinationPageId == null && sourceRow == null) {
                besideLinkBlockId
                    ?.let { blockDao.getById(it) }
                    ?.takeIf { it.linkedPageId == pageId }
                    ?: blockDao.getBlocksLinkingTo(pageId).firstOrNull()
            } else {
                null
            }

            val copyTitle = "${original.title} ($titleSuffix)".trimStart()
            val copyId = copyPageDeep(pageId, mutableMapOf(), copyImage) { page ->
                page.copy(title = copyTitle, isRowPage = sourceRow != null)
            } ?: return@withTransaction null

            val now = System.currentTimeMillis()
            when {
                sourceRow != null -> {
                    databaseDao.shiftRowOrder(sourceRow.pageId, sourceRow.orderIndex + 1, 1)
                    val rowId = UUID.randomUUID().toString()
                    databaseDao.insertRow(
                        sourceRow.copy(
                            id = rowId,
                            title = copyTitle,
                            linkedPageId = copyId,
                            orderIndex = sourceRow.orderIndex + 1,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                    // Le proprietà vengono con lei: una riga duplicata
                    // senza i suoi valori sarebbe solo una riga nuova.
                    // Un'immagine ("Image") ha il suo file: la copia ne ha uno suo,
                    // o cambiandola in una riga sparirebbe anche dall'altra.
                    val sourceColumns = databaseDao.getColumnsForPageOnce(sourceRow.pageId)
                    val imageColumns = sourceColumns.filter { it.type == ColumnType.IMAGE }.mapTo(HashSet()) { it.id }
                    val sourceCells = databaseDao.getCellsForRowOnce(sourceRow.id)
                    sourceCells.forEach { cell ->
                        val value = if (cell.columnId in imageColumns && cell.value.isNotEmpty()) {
                            copyImage(cell.value) ?: cell.value
                        } else {
                            cell.value
                        }
                        databaseDao.insertCell(cell.copy(id = UUID.randomUUID().toString(), rowId = rowId, value = value))
                    }
                    // Una "Relation" nei due sensi (dal 21/10/2026): le righe
                    // collegate all'originale hanno anche la copia fra le loro.
                    sourceColumns.forEach { column ->
                        val reverse = column.relationConfig()?.reverse ?: return@forEach
                        if (databaseDao.getColumnById(reverse)?.relationConfig()?.reverse != column.id) return@forEach
                        relationIds(sourceCells.find { it.columnId == column.id }?.value).forEach { other ->
                            val cell = databaseDao.getCell(other, reverse)
                            val ids = relationIds(cell?.value)
                            if (rowId in ids) return@forEach
                            if (cell != null) {
                                databaseDao.updateCell(cell.copy(value = encodeRelationIds(ids + rowId)))
                            } else {
                                databaseDao.insertCell(DatabaseCellEntity(rowId = other, columnId = reverse, value = rowId))
                            }
                        }
                    }
                }
                sourceLink != null -> {
                    blockDao.shiftOrderIndexes(
                        sourceLink.pageId,
                        sourceLink.parentBlockId,
                        sourceLink.orderIndex + 1,
                        1
                    )
                    blockDao.insert(
                        sourceLink.copy(
                            id = UUID.randomUUID().toString(),
                            linkedPageId = copyId,
                            orderIndex = sourceLink.orderIndex + 1
                        )
                    )
                }
                // Scelta una pagina, o un originale che non sta da nessuna
                // parte: in fondo, dove la si ritrova di sicuro.
                else -> appendLink(destinationPageId ?: PageEntity.ROOT_PAGE_ID, copyId, original.isDatabase)
            }
            copyId
        }
    }

    /**
     * "Duplicate" dal menu di un blocco: una copia della riga subito sotto
     * di lei, con lo stesso tipo, testo, colori, rientro e spunta.
     *
     * Un toggle si copia **coi suoi figli**, e una tabella con le sue
     * celle, che stanno in una tabella a parte. Se dentro un toggle c'è il
     * collegamento a una sottopagina o un database, anche quelli si
     * copiano, come fa "Duplicate" di una pagina: due collegamenti alla
     * stessa sottopagina non sarebbero una copia, scrivere in una
     * cambierebbe anche l'altra.
     *
     * Restituisce l'id della copia, o null se il blocco non c'è più.
     */
    suspend fun duplicateBlock(blockId: String, copyImage: suspend (String) -> String?): String? {
        val original = blockDao.getById(blockId) ?: return null
        return db.withTransaction {
            blockDao.shiftOrderIndexes(
                original.pageId,
                original.parentBlockId,
                original.orderIndex + 1,
                1
            )
            copyBlockTree(
                block = original,
                parentBlockId = original.parentBlockId,
                orderIndex = original.orderIndex + 1,
                copies = mutableMapOf(),
                copyImage = copyImage
            )
        }
    }

    /**
     * Le voci in più della copia di un blocco, coi **suoi** file (dal
     * 06/10/2026): l'immagine, il video, il file, l'icona del riquadro.
     * Come per le icone delle pagine, due blocchi non devono condividere lo
     * stesso file: cancellando per sempre la pagina di uno, il file se ne va.
     */
    private suspend fun copiedAttrs(block: BlockEntity, copyImage: suspend (String) -> String?): String {
        val attrs = block.attrs
        if (attrs.files.isEmpty()) return block.attrsJson
        return BlockAttrs.encode(
            attrs.copy(
                file = attrs.file?.let { copyImage(it) ?: it },
                iconImage = attrs.iconImage?.let { copyImage(it) ?: it },
                image = attrs.image?.let { copyImage(it) ?: it }
            )
        )
    }

    /** Un blocco e, sotto, i suoi figli: vedi `duplicateBlock`. Dentro una transazione. */
    private suspend fun copyBlockTree(
        block: BlockEntity,
        parentBlockId: String?,
        orderIndex: Int,
        copies: MutableMap<String, String>,
        copyImage: suspend (String) -> String?
    ): String {
        val copyId = UUID.randomUUID().toString()
        blockDao.insert(
            block.copy(
                id = copyId,
                parentBlockId = parentBlockId,
                orderIndex = orderIndex,
                linkedPageId = block.linkedPageId?.let { copyLinkedPage(it, copies, copyImage) },
                attrsJson = copiedAttrs(block, copyImage)
            )
        )
        if (block.type == BlockType.TABLE) {
            tableCellDao.getCellsForBlockOnce(block.id).forEach { cell ->
                tableCellDao.insertCell(cell.copy(id = UUID.randomUUID().toString(), blockId = copyId))
            }
        }
        blockDao.getChildren(block.id).sortedBy { it.orderIndex }.forEach { child ->
            copyBlockTree(child, copyId, child.orderIndex, copies, copyImage)
        }
        return copyId
    }

    /**
     * Copia una pagina e quello che contiene, giù fino in fondo. Va
     * chiamata dentro una transazione.
     *
     * `copies` tiene originale → copia. Una pagina richiamata due volte si
     * copia una volta sola, e una che richiama una pagina sopra di lei
     * (basta un collegamento all'indietro) punta alla copia di quella
     * invece di ricominciare all'infinito. `adjust` ritocca la copia di
     * partenza prima di salvarla.
     */
    private suspend fun copyPageDeep(
        pageId: String,
        copies: MutableMap<String, String>,
        copyImage: suspend (String) -> String?,
        adjust: (PageEntity) -> PageEntity = { it }
    ): String? {
        copies[pageId]?.let { return it }
        val original = pageDao.getById(pageId) ?: return null
        val databaseDao = db.databaseDao()
        val copyId = UUID.randomUUID().toString()
        copies[pageId] = copyId
        val now = System.currentTimeMillis()

        // Le colonne cambiano id, e sono richiamate dalle celle e dalla
        // pagina stessa: raggruppamento, calendario, ordine e filtro. Senza
        // tradurre anche quelli la copia si apriva col raggruppamento o il
        // filtro spariti, perché puntavano alle colonne dell'originale.
        val columns = if (original.isDatabase) databaseDao.getColumnsForPageOnce(pageId) else emptyList()
        val newColumnIds = columns.associate { it.id to UUID.randomUUID().toString() }
        fun column(id: String?) = id?.let { newColumnIds[it] ?: it }

        pageDao.insert(
            adjust(
                original.copy(
                    id = copyId,
                    iconImage = original.iconImage?.let { copyImage(it) },
                    coverImage = original.coverImage?.let { copyImage(it) },
                    // I raggruppamenti (dal 28/10/2026): ogni regola sulla sua proprietà copiata.
                    tableGroupRulesJson = encodeGroupRulesMapped(original.tableGroupRulesJson) { column(it) ?: it },
                    boardGroupRulesJson = encodeGroupRulesMapped(original.boardGroupRulesJson) { column(it) ?: it },
                    calendarDateColumnId = column(original.calendarDateColumnId),
                    sortRulesJson = original.sortRulesJson?.let { json ->
                        encodeSortRules(parseSortRules(json).map { it.copy(column = column(it.column) ?: it.column) })
                    },
                    filterColumnId = column(original.filterColumnId),
                    // Anche le larghezze delle colonne sono per id.
                    columnWidths = encodeColumnWidths(
                        parseColumnWidths(original.columnWidths).mapKeys { (key, _) -> column(key) ?: key }
                    ),
                    fitColumns = encodeFitColumns(parseFitColumns(original.fitColumns).map { column(it) ?: it }.toSet()),
                    // Le viste in più non si copiano: la copia mostra la sua.
                    activeViewId = null,
                    isFavorite = false,
                    favoritedAt = null,
                    trashedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )

        // I blocchi, genitori prima dei figli: `parentBlockId` deve
        // trovare il suo blocco già salvato, e gli id vecchi vanno
        // tradotti nei nuovi, altrimenti i figli di un toggle della copia
        // resterebbero appesi a quello dell'originale. Le sottopagine si
        // copiano prima di salvare il collegamento che le richiama.
        val blocks = inDocumentOrder(blockDao.getBlocksForPageOnce(pageId))
        val newBlockIds = blocks.associate { it.id to UUID.randomUUID().toString() }
        val copiedBlocks = blocks.map { block ->
            block.copy(
                id = newBlockIds.getValue(block.id),
                pageId = copyId,
                parentBlockId = block.parentBlockId?.let { newBlockIds[it] },
                linkedPageId = block.linkedPageId?.let { copyLinkedPage(it, copies, copyImage) },
                attrsJson = copiedAttrs(block, copyImage)
            )
        }
        blockDao.insertAll(copiedBlocks)

        // Le tabelle semplici tengono il testo in una tabella a parte, per
        // blocco: prima la copia le aveva vuote.
        blocks.filter { it.type == BlockType.TABLE }.forEach { table ->
            tableCellDao.getCellsForBlockOnce(table.id).forEach { cell ->
                tableCellDao.insertCell(
                    cell.copy(id = UUID.randomUUID().toString(), blockId = newBlockIds.getValue(table.id))
                )
            }
        }

        // I disegni a mano vengono con la pagina, attaccati ai blocchi
        // della copia: agli originali non li vedrebbe nessuno.
        val inkDao = db.inkDao()
        val strokes = inkDao.getForPageOnce(pageId)
        if (strokes.isNotEmpty()) {
            inkDao.insertAll(
                strokes.map { stroke ->
                    stroke.copy(
                        id = UUID.randomUUID().toString(),
                        pageId = copyId,
                        anchor = remapAnchor(stroke.anchor, newBlockIds)
                    )
                }
            )
        }

        if (original.isDatabase) {
            columns.forEach { column ->
                databaseDao.insertColumn(
                    column.copy(
                        id = newColumnIds.getValue(column.id),
                        pageId = copyId,
                        // "Relation" e "Rollup" che puntano dentro il database puntano dentro la copia.
                        optionsJson = copiedLinkOptions(column, pageId, copyId, newColumnIds)
                    )
                )
            }
            val rows = databaseDao.getRowsForPageOnce(pageId)
            val newRowIds = rows.associate { it.id to UUID.randomUUID().toString() }
            rows.forEach { row ->
                databaseDao.insertRow(
                    row.copy(
                        id = newRowIds.getValue(row.id),
                        pageId = copyId,
                        // Una pagina di riga buttata nel cestino resta
                        // dov'è: la riga copiata ne avrà una nuova la
                        // prima volta che la si apre.
                        linkedPageId = row.linkedPageId
                            ?.takeIf { pageDao.getById(it)?.trashedAt == null }
                            ?.let { copyPageDeep(it, copies, copyImage) },
                        // La sotto-riga della copia sta sotto la riga copiata, non sotto l'originale.
                        parentRowId = row.parentRowId?.let { newRowIds[it] },
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            val pageLinkColumns = columns.filter { it.type == ColumnType.PAGE_LINK }.mapTo(HashSet()) { it.id }
            val imageColumns = columns.filter { it.type == ColumnType.IMAGE }.mapTo(HashSet()) { it.id }
            val selfRelations = columns.filter { it.relationConfig()?.database == pageId }.mapTo(HashSet()) { it.id }
            databaseDao.getAllCellsForPageOnce(pageId).forEach { cell ->
                val rowId = newRowIds[cell.rowId] ?: return@forEach
                val columnId = newColumnIds[cell.columnId] ?: return@forEach
                // "Page link" verso una pagina copiata insieme (di solito la
                // pagina di un'altra riga): la copia punta alla copia.
                val value = when {
                    cell.columnId in pageLinkColumns -> copies[cell.value] ?: cell.value
                    // Una Relation di un database con sé stesso: fra le righe della copia.
                    cell.columnId in selfRelations -> encodeRelationIds(relationIds(cell.value).map { newRowIds[it] ?: it })
                    // "Image": ogni copia col suo file (vedi sopra, riga duplicata).
                    cell.columnId in imageColumns && cell.value.isNotEmpty() -> copyImage(cell.value) ?: cell.value
                    else -> cell.value
                }
                databaseDao.insertCell(cell.copy(id = UUID.randomUUID().toString(), rowId = rowId, columnId = columnId, value = value))
            }
        }
        return copyId
    }

    /**
     * La pagina a cui punterà un collegamento della copia: la copia della
     * sottopagina. La pagina principale e quelle nel cestino non si
     * copiano — la prima c'è una volta sola, le altre sono state buttate —
     * e il collegamento resta a loro, com'era nell'originale.
     */
    private suspend fun copyLinkedPage(
        pageId: String,
        copies: MutableMap<String, String>,
        copyImage: suspend (String) -> String?
    ): String? {
        if (pageId == PageEntity.ROOT_PAGE_ID) return pageId
        val page = pageDao.getById(pageId) ?: return null
        if (page.trashedAt != null) return pageId
        return copyPageDeep(pageId, copies, copyImage)
    }

    /** Un collegamento a una pagina in fondo a un'altra, fuori da ogni toggle. */
    private suspend fun appendLink(pageId: String, linkedPageId: String, isDatabase: Boolean): String {
        val link = BlockEntity(
            pageId = pageId,
            type = if (isDatabase) BlockType.DATABASE_LINK else BlockType.PAGE_LINK,
            linkedPageId = linkedPageId,
            orderIndex = (blockDao.lastOrderIndex(pageId) ?: -1) + 1
        )
        blockDao.insert(link)
        return link.id
    }

    /**
     * Una pagina fra cui scegliere dove mettere la copia, e dove sta: il
     * database per la pagina di una riga, la pagina che la contiene per le
     * altre. Serve a distinguere due pagine con lo stesso nome — e dopo un
     * "Duplicate" ce ne sono sempre due.
     */
    data class DestinationHit(val node: PageTreeNode, val place: String?)

    /**
     * Le pagine il cui titolo contiene la parola cercata, fra cui scegliere
     * dove mettere una copia: quelle di testo e quelle delle righe di tutti
     * i database, anche le righe mai aperte (la loro pagina nascerà se la
     * si sceglie). I database no, e niente dal cestino.
     *
     * Nemmeno le pagine che non stanno da nessuna parte, senza un
     * collegamento né una riga che le apra: si trovano solo cercandole, e
     * una copia messa lì dentro sparirebbe con loro.
     */
    suspend fun searchDestinations(query: String): List<DestinationHit> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        val databaseDao = db.databaseDao()
        val hits = mutableListOf<DestinationHit>()
        for (page in pageDao.searchByTitle(needle)) {
            if (page.isDatabase || page.trashedAt != null) continue
            val container = if (page.isRowPage) {
                databaseDao.getRowsLinkingTo(page.id).firstOrNull()?.let { pageDao.getById(it.pageId) }
            } else {
                blockDao.getBlocksLinkingTo(page.id).firstOrNull()?.let { pageDao.getById(it.pageId) }
            }
            val isRoot = page.id == PageEntity.ROOT_PAGE_ID
            if (!isRoot && (container == null || container.trashedAt != null)) continue
            hits += DestinationHit(nodeFor(page), container?.title)
        }
        // Il nome di una riga già aperta è il titolo della sua pagina,
        // che la ricerca qui sopra ha già trovato.
        for (row in databaseDao.searchRows(needle)) {
            if (row.linkedPageId != null) continue
            val database = pageDao.getById(row.pageId)?.takeIf { it.trashedAt == null } ?: continue
            // Copiare "dentro" una riga vuol dire dentro la sua pagina, e
            // una riga di un database semplice una pagina non ce l'ha.
            if (database.isSimpleDatabase) continue
            hits += DestinationHit(
                PageTreeNode(
                    pageId = null,
                    rowId = row.id,
                    title = row.title,
                    emoji = DEFAULT_PAGE_EMOJI,
                    iconImage = null,
                    isDatabase = false,
                    hasChildren = false
                ),
                database.title
            )
        }
        return hits
    }

    /**
     * **I database in cui spostare una pagina** ("Move to database", dal
     * 05/10/2026), cercandoli per nome: quelli veri — non le viste in più,
     * non le viste collegate, che mostrano un altro database — e non i
     * database semplici, le cui righe non sono pagine. Niente dal cestino,
     * e niente che non stia da nessuna parte. Sotto, la pagina in cui sta.
     */
    suspend fun searchDatabaseDestinations(query: String): List<DestinationHit> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return pageDao.searchByTitle(needle)
            .filter {
                it.isDatabase && !it.isSimpleDatabase && it.viewHostId == null &&
                    it.sourceDatabaseId == null && it.trashedAt == null
            }
            .mapNotNull { database ->
                val container = blockDao.getBlocksLinkingTo(database.id).firstOrNull()
                    ?.let { pageDao.getById(it.pageId) }
                    ?.takeIf { it.trashedAt == null }
                    ?: return@mapNotNull null
                DestinationHit(nodeFor(database), container.title)
            }
    }

    // --- Cronologia delle modifiche (la voce "Updates") ---

    fun observeEdits(pageId: String): Flow<List<PageEditEntity>> =
        pageEditDao.observeForPage(pageId)

    suspend fun recordEdit(edit: PageEditEntity) = pageEditDao.insert(edit)

    /**
     * Scrive una voce di cronologia **dopo una pausa**, e su uno scope
     * che non muore con chi l'ha chiesta.
     *
     * Le due cose servono insieme. La pausa: quello che si è appena
     * scritto viaggia verso il database e torna indietro, e leggendo
     * subito si prenderebbe il testo di un attimo prima — la cronologia
     * racconterebbe una correzione troncata all'ultima lettera. Lo
     * scope a sé: l'ultima sessione di scrittura si chiude proprio
     * mentre si esce dalla pagina, e lo scope del ViewModel a quel
     * punto è già stato annullato, quindi quella riga non verrebbe
     * scritta mai — che è esattamente la modifica che si vuole
     * ritrovare.
     *
     * `build` restituisce null quando non c'è niente da registrare.
     */
    fun recordEditAfterSettling(delayMillis: Long, build: suspend () -> PageEditEntity?) {
        historyScope.launch {
            delay(delayMillis)
            build()?.let { pageEditDao.insert(it) }
        }
    }

    suspend fun clearEdits(pageId: String) = pageEditDao.clearForPage(pageId)

    suspend fun createPage(page: PageEntity) = pageDao.insert(page)

    suspend fun updatePage(page: PageEntity) {
        val now = System.currentTimeMillis()
        pageDao.update(page.copy(updatedAt = now))
        // Se questa pagina è la pagina di una riga di database, il suo
        // titolo È il nome della riga: rinominandola qui dentro va
        // aggiornata anche la tabella, altrimenti tornando indietro si
        // ritrova il nome vecchio. Sulle pagine normali non corrisponde
        // nessuna riga e la scrittura non tocca niente.
        db.databaseDao().setRowTitleByLinkedPage(page.id, page.title, now)
    }

    suspend fun deletePage(page: PageEntity) = db.withTransaction {
        pageDao.deleteViewsOf(page.id)
        pageDao.delete(page)
    }

    fun getBlocks(pageId: String): Flow<List<BlockEntity>> =
        blockDao.getBlocksForPage(pageId)

    suspend fun saveBlock(block: BlockEntity) = blockDao.insert(block)

    /** Salva il blocco solo se nel database non c'è ancora (vedi `PageEditorViewModel.placeholder`). */
    suspend fun saveBlockIfAbsent(block: BlockEntity) = blockDao.insertIfAbsent(block)

    /**
     * "Fai posto da qui in poi": sposta avanti di `by` la numerazione
     * dei blocchi fratelli da `fromIndex` compreso. Vedi
     * `BlockDao.shiftOrderIndexes`.
     */
    data class BlockShift(
        val pageId: String,
        val parentBlockId: String?,
        val fromIndex: Int,
        val by: Int
    )

    /**
     * Applica **in un colpo solo** un cambiamento che tocca più
     * blocchi: fare posto, salvarne alcuni, cancellarne altri.
     *
     * Tutto dentro una transazione, e non è un dettaglio di
     * prestazioni: Room avvisa chi osserva la tabella **ad ogni
     * scrittura**, quindi salvare venti blocchi uno per uno faceva
     * ridisegnare la pagina venti volte di fila. In mezzo a quella
     * tempesta il campo di testo su cui si sta scrivendo viene
     * ricomposto di continuo, il fuoco non riesce a passare al blocco
     * nuovo e i caratteri successivi finiscono in quello vecchio — è
     * il bug per cui premendo Invio e scrivendo in fretta si otteneva
     * tutto attaccato in una riga sola. Con la transazione l'avviso è
     * uno, e la pagina si ridisegna una volta.
     */
    suspend fun applyBlockChanges(
        shift: BlockShift? = null,
        saved: List<BlockEntity> = emptyList(),
        deleted: List<BlockEntity> = emptyList(),
        /** Da creare **solo se mancano**: la riga vuota di una pagina nuova (vedi `saveBlockIfAbsent`). */
        ensured: List<BlockEntity> = emptyList(),
        /**
         * Il **solo testo** di blocchi che ci sono già, per id. Tipo, spunta,
         * rientro e il resto restano quelli del database: un cambio di tipo
         * arrivato un istante prima (il menu "/" salva il testo e poi cambia
         * il tipo) non viene riscritto con la copia di prima.
         */
        texts: Map<String, String> = emptyMap()
    ) = db.withTransaction {
        if (shift != null && shift.by != 0) {
            blockDao.shiftOrderIndexes(
                shift.pageId,
                shift.parentBlockId,
                shift.fromIndex,
                shift.by
            )
        }
        ensured.forEach { blockDao.insertIfAbsent(it) }
        if (saved.isNotEmpty()) blockDao.insertAll(saved)
        texts.forEach { (blockId, textJson) -> blockDao.setText(blockId, textJson) }
        deleted.forEach { blockDao.delete(it) }
    }

    /** I blocchi di una pagina come sono **adesso** nel database, letti una volta. */
    suspend fun getBlocksOnce(pageId: String): List<BlockEntity> = blockDao.getBlocksForPageOnce(pageId)

    /**
     * **Sposta un blocco di un posto** fra i suoi fratelli, su (`up`) o giù:
     * i pulsanti con le frecce della barra. Restituisce l'id del fratello
     * scavalcato, o null se non c'era dove andare.
     *
     * Prima si scambiavano due blocchi **interi** presi dalla copia della
     * schermata, con due salvataggi separati: se il testo appena scritto o
     * la riga appena creata con l'Invio non erano ancora nel database, lo
     * scambio rimetteva il testo di prima e le posizioni di prima (trovato
     * dall'utente il 28/09/2026: "sembra che cancelli una riga in basso").
     * Qui le posizioni si leggono dal database dentro la transazione e si
     * cambia **solo l'indice**.
     *
     * I fratelli nell'ordine della pagina (`getBlocksForPageOnce`, la stessa
     * lettura della schermata). Due con lo stesso indice non si potrebbero
     * scambiare: in quel caso prima si rimettono in fila, 0, 1, 2…
     */
    suspend fun moveBlock(blockId: String, up: Boolean): String? = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction null
        val siblings = blockDao.getBlocksForPageOnce(block.pageId).filter { it.parentBlockId == block.parentBlockId }
        val from = siblings.indexOfFirst { it.id == blockId }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to !in siblings.indices) return@withTransaction null
        val passed = siblings[to]
        val order = siblings.toMutableList().apply { add(to, removeAt(from)) }
        val slots = siblings.map { it.orderIndex }.let { if (it.distinct().size == it.size) it else it.indices.toList() }
        order.forEachIndexed { i, sibling ->
            if (sibling.orderIndex != slots[i]) blockDao.setOrderIndex(sibling.id, slots[i])
        }
        passed.id
    }

    /**
     * **"Move to line…"** (dal 20/10/2026, chiesto dall'utente: "concedimi di
     * spostare le immagini dove voglio"): il blocco va subito sotto `afterId`,
     * fra i fratelli di quella riga (dentro lo stesso toggle, se ci sta), o in
     * cima alla pagina se `afterId` è null. Le righe che ha dentro vengono con
     * lui. Il vecchio posto si chiude, il nuovo si apre spostando avanti di uno
     * quelli dopo. Falso se il blocco o la riga non ci sono più, o se la riga
     * sta dentro il blocco stesso.
     */
    suspend fun moveBlockAfter(blockId: String, afterId: String?): Boolean = moveBlockNextTo(blockId, afterId, after = true)

    /**
     * **Il blocco subito prima o subito dopo [targetId]** ([after]), fra i
     * fratelli di quella riga: dentro lo stesso toggle o la stessa colonna,
     * se ci sta (dal 12/11/2026, per trascinare dentro e fuori dalle colonne).
     * Con [targetId] null va in cima alla pagina. Le righe che ha dentro
     * vengono con lui; la colonna che lascia, se resta vuota, si sistema
     * (`tidyColumn`). Falso se il blocco o la riga non ci sono più, se la riga
     * sta dentro il blocco stesso, o se un gruppo di colonne finirebbe dentro
     * una colonna.
     */
    suspend fun moveBlockNextTo(blockId: String, targetId: String?, after: Boolean): Boolean = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction false
        if (targetId == blockId) return@withTransaction false
        val all = blockDao.getBlocksForPageOnce(block.pageId)
        val byId = all.associateBy { it.id }
        if (targetId != null) {
            val target = byId[targetId] ?: return@withTransaction false
            if (isInside(target, blockId, byId)) return@withTransaction false
            if (block.type == BlockType.COLUMN_LIST && byId[target.parentBlockId]?.type == BlockType.COLUMN) return@withTransaction false
        }
        val oldParent = block.parentBlockId
        blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex + 1, -1)
        // La riga si rilegge: chiuso il buco, il suo posto può essere cambiato.
        val target = targetId?.let { blockDao.getById(it) }
        val parent = target?.parentBlockId
        val at = when {
            target == null -> 0
            after -> target.orderIndex + 1
            else -> target.orderIndex
        }
        blockDao.shiftOrderIndexes(block.pageId, parent, at, 1)
        blockDao.setParentAndOrder(blockId, parent, at)
        if (oldParent != parent) tidyColumn(oldParent)
        true
    }

    /**
     * **Il blocco accanto a [targetId], a destra o a sinistra ([right])**,
     * come su Notion (dal 12/11/2026, chiesto dall'utente con le foto):
     * - accanto a una riga qualsiasi della pagina, lei e il blocco diventano
     *   un gruppo di due colonne, al posto della riga;
     * - accanto a una riga dentro una colonna (o alla colonna), il blocco
     *   diventa una colonna in più, subito a destra o a sinistra di quella;
     * - accanto a un gruppo di colonne, una colonna in più in fondo o in cima.
     * La colonna che il blocco lascia si sistema (`tidyColumn`). Falso se non
     * si può: il blocco su se stesso o dentro di sé, o un gruppo di colonne
     * (niente colonne dentro le colonne).
     */
    suspend fun moveBlockBeside(blockId: String, targetId: String, right: Boolean): Boolean = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction false
        if (targetId == blockId || block.type == BlockType.COLUMN_LIST || block.type == BlockType.COLUMN) return@withTransaction false
        val all = blockDao.getBlocksForPageOnce(block.pageId)
        val byId = all.associateBy { it.id }
        val first = byId[targetId] ?: return@withTransaction false
        if (isInside(first, blockId, byId)) return@withTransaction false
        val oldParent = block.parentBlockId
        blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex + 1, -1)
        val target = blockDao.getById(targetId) ?: return@withTransaction false
        val holder = target.parentBlockId?.let { blockDao.getById(it) }
        suspend fun newColumn(listId: String, at: Int): String {
            blockDao.shiftOrderIndexes(block.pageId, listId, at, 1)
            return BlockEntity(pageId = block.pageId, parentBlockId = listId, type = BlockType.COLUMN, orderIndex = at)
                .also { blockDao.insert(it) }.id
        }
        when {
            target.type == BlockType.COLUMN_LIST -> {
                val at = if (right) (blockDao.getChildren(target.id).maxOfOrNull { it.orderIndex } ?: -1) + 1 else 0
                blockDao.setParentAndOrder(blockId, newColumn(target.id, at), 0)
            }
            target.type == BlockType.COLUMN || holder?.type == BlockType.COLUMN -> {
                val column = if (target.type == BlockType.COLUMN) target else holder!!
                val listId = column.parentBlockId ?: return@withTransaction false
                blockDao.setParentAndOrder(blockId, newColumn(listId, column.orderIndex + if (right) 1 else 0), 0)
            }
            else -> {
                // Il gruppo prende il posto della riga, che va in una delle due colonne.
                val list = BlockEntity(pageId = block.pageId, parentBlockId = target.parentBlockId, type = BlockType.COLUMN_LIST, orderIndex = target.orderIndex)
                blockDao.insert(list)
                val left = BlockEntity(pageId = block.pageId, parentBlockId = list.id, type = BlockType.COLUMN, orderIndex = 0).also { blockDao.insert(it) }
                val rightColumn = BlockEntity(pageId = block.pageId, parentBlockId = list.id, type = BlockType.COLUMN, orderIndex = 1).also { blockDao.insert(it) }
                blockDao.setParentAndOrder(if (right) target.id else blockId, left.id, 0)
                blockDao.setParentAndOrder(if (right) blockId else target.id, rightColumn.id, 0)
            }
        }
        tidyColumn(oldParent)
        true
    }

    /** Vero se [block] sta dentro [ancestorId] (o è lui), risalendo i genitori. */
    private fun isInside(block: BlockEntity, ancestorId: String, byId: Map<String, BlockEntity>): Boolean {
        var current: BlockEntity? = block
        val seen = HashSet<String>()
        while (current != null && seen.add(current.id)) {
            if (current.id == ancestorId) return true
            current = current.parentBlockId?.let { byId[it] }
        }
        return false
    }

    /**
     * **Una colonna rimasta vuota se ne va, e un gruppo con una colonna sola
     * torna righe normali** (dal 12/11/2026, come su Notion): le righe di
     * quell'ultima colonna prendono il posto del gruppo, nell'ordine. Niente
     * se [columnId] non è una colonna.
     */
    private suspend fun tidyColumn(columnId: String?) {
        val column = columnId?.let { blockDao.getById(it) }?.takeIf { it.type == BlockType.COLUMN } ?: return
        val listId = column.parentBlockId ?: return
        if (blockDao.getChildren(column.id).isEmpty()) {
            blockDao.delete(column)
            blockDao.shiftOrderIndexes(column.pageId, listId, column.orderIndex + 1, -1)
        }
        val list = blockDao.getById(listId)?.takeIf { it.type == BlockType.COLUMN_LIST } ?: return
        val columns = blockDao.getChildren(list.id)
        if (columns.size > 1) return
        val last = columns.singleOrNull()
        if (last != null && last.type != BlockType.COLUMN) return
        val lines = last?.let { blockDao.getChildren(it.id) }.orEmpty()
        // Il gruppo ha un posto solo: le righe ne prendono tante quante sono.
        blockDao.shiftOrderIndexes(list.pageId, list.parentBlockId, list.orderIndex + 1, lines.size - 1)
        lines.forEachIndexed { i, line -> blockDao.setParentAndOrder(line.id, list.parentBlockId, list.orderIndex + i) }
        columns.forEach { blockDao.delete(it) }
        blockDao.delete(list)
    }

    /**
     * **Dove va quello che arriva dal Web Clipper** (dal 20/10/2026): al posto
     * della prima riga vuota della pagina, dall'alto (`firstEmptyLine`), e se
     * non ce n'è in fondo. Il primo dei `blocks` prende il posto della riga
     * vuota (la foto, il segnalibro, o il titolo di "Text and Images"), gli
     * altri vengono sotto di lui, in ordine, allo stesso livello; le tabelle
     * con le loro celle. Restituisce l'id del primo blocco, o null se non ce
     * n'erano.
     */
    suspend fun insertAtFirstEmptyLine(pageId: String, blocks: List<NewBlock>): String? =
        db.withTransaction {
            if (blocks.isEmpty()) return@withTransaction null
            val existing = blockDao.getBlocksForPageOnce(pageId)
            val empty = existing.firstEmptyLine { block ->
                (runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrNull() ?: emptyList())
                    .joinToString("") { it.text }
            }
            fun entityFor(spec: NewBlock, base: BlockEntity): BlockEntity = base.copy(
                type = spec.type,
                textJson = json.encodeToString(spec.spans),
                attrsJson = BlockAttrs.encode(spec.attrs),
                indentLevel = spec.indentLevel,
                tableRows = spec.cells?.size?.coerceAtLeast(1) ?: base.tableRows,
                tableCols = spec.cells?.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: base.tableCols
            )
            val first = blocks.first()
            val placed = if (empty != null) {
                entityFor(first, empty).also { blockDao.update(it) }
            } else {
                entityFor(first, BlockEntity(pageId = pageId, orderIndex = (blockDao.lastOrderIndex(pageId) ?: -1) + 1))
                    .also { blockDao.insert(it) }
            }
            val rest = blocks.drop(1)
            if (rest.isNotEmpty()) {
                blockDao.shiftOrderIndexes(pageId, placed.parentBlockId, placed.orderIndex + 1, rest.size)
            }
            val created = listOf(first to placed) + rest.mapIndexed { i, spec ->
                spec to entityFor(spec, BlockEntity(pageId = pageId, parentBlockId = placed.parentBlockId, orderIndex = placed.orderIndex + 1 + i))
                    .also { blockDao.insert(it) }
            }
            created.forEach { (spec, entity) ->
                spec.cells?.forEachIndexed { r, row ->
                    row.forEachIndexed { c, text -> if (text.isNotEmpty()) setTableCellText(entity.id, r, c, text) }
                }
            }
            placed.id
        }

    // --- I bottoni (dal 27/10/2026): vedi `data/buttons/Buttons.kt` ---

    /**
     * Il database vero di [databaseId] (quello di cui è una vista, se è una
     * vista collegata), o null se non c'è più.
     */
    suspend fun realDatabase(databaseId: String): PageEntity? {
        val page = pageDao.getById(databaseId) ?: return null
        return (page.sourceDatabaseId?.let { pageDao.getById(it) } ?: page).takeIf { it.isDatabase && it.trashedAt == null }
    }

    /** Le proprietà del database vero di [databaseId], nel loro ordine: quelle che un bottone può cambiare. */
    suspend fun databaseColumns(databaseId: String): List<DatabaseColumnEntity> {
        val real = realDatabase(databaseId) ?: return emptyList()
        return db.databaseDao().getColumnsForPageOnce(real.id).sortedBy { it.orderIndex }
    }

    /** Un blocco seguito nel tempo: il bottone vero di una copia collegata. */
    fun observeBlock(blockId: String): Flow<BlockEntity?> = blockDao.observeById(blockId)

    /**
     * La riga di cui [pageId] è la pagina, o null: per un bottone che sta lì,
     * "This page" è lei.
     */
    suspend fun rowOfPage(pageId: String): DatabaseRowEntity? =
        db.databaseDao().getRowsLinkingTo(pageId).firstOrNull()

    /** Un bottone da scegliere in "Link to button": il blocco, il suo nome e la pagina in cui sta. */
    data class ButtonChoice(val blockId: String, val config: ButtonConfig, val page: PageEntity)

    /**
     * I bottoni veri (non le copie collegate) di tutte le pagine fuori dal
     * cestino, tranne quelli dentro una pagina protetta e chiusa: per nome
     * della pagina, poi nell'ordine della pagina.
     */
    suspend fun buttonChoices(): List<ButtonChoice> {
        val pages = HashMap<String, PageEntity?>()
        return blockDao.getButtonBlocks().mapNotNull { block ->
            val attrs = block.attrs
            if (attrs.buttonLink != null) return@mapNotNull null
            val page = pages.getOrPut(block.pageId) { pageDao.getById(block.pageId) } ?: return@mapNotNull null
            if (page.trashedAt != null || (page.isProtected && !AppLock.isUnlocked(page.id))) return@mapNotNull null
            ButtonChoice(block.id, attrs.button ?: ButtonConfig(), page) to block.orderIndex
        }.sortedWith(compareBy({ it.first.page.title.lowercase() }, { it.second })).map { it.first }
    }

    /**
     * **Una copia collegata di un bottone** in fondo alla pagina [pageId]: se
     * [sourceBlockId] è a sua volta una copia, si collega al bottone vero.
     * Restituisce l'id del blocco nuovo, o null se il bottone non c'è più.
     */
    suspend fun addLinkedButton(sourceBlockId: String, pageId: String): String? {
        val source = blockDao.getById(sourceBlockId) ?: return null
        val realId = source.attrs.buttonLink ?: source.id
        return insertBlocksAt(
            pageId, null, InsertPlace.BOTTOM_OF_PAGE,
            listOf(NewBlock(type = BlockType.BUTTON, attrs = BlockAttrs(buttonLink = realId)))
        ).firstOrNull()
    }

    // --- Registrare l'audio e scansionare i documenti (dal 03/11/2026) ---

    /**
     * **Una registrazione finita** (vedi `AudioRecorder`): il file va nel
     * blocco audio da cui è partita, se è ancora vuoto; se nel frattempo il
     * blocco è sparito o ha preso un altro file, in un blocco audio nuovo
     * subito sotto, o in fondo alla pagina. Falso se non c'è più nemmeno la
     * pagina: il file allora non serve a nessuno.
     */
    suspend fun putRecording(blockId: String, pageId: String, attrs: BlockAttrs): Boolean {
        val block = blockDao.getById(blockId)
        if (block != null && block.type == BlockType.AUDIO && block.attrs.file == null && block.attrs.url == null) {
            blockDao.setTypeAndAttrs(blockId, BlockType.AUDIO, BlockAttrs.encode(attrs))
            return true
        }
        if (pageDao.getById(pageId) == null) return false
        return insertBlocksAt(
            pageId, block?.id?.takeIf { block.pageId == pageId }, InsertPlace.BELOW_BUTTON,
            listOf(NewBlock(type = BlockType.AUDIO, attrs = attrs))
        ).isNotEmpty()
    }

    /**
     * **Le pagine di un documento scansionato**, una foto ciascuna, al posto
     * della riga [blockId] se è vuota, se no subito sotto di lei. [files]
     * sono già nell'app, coi loro nomi ([names]). Restituisce gli id dei
     * blocchi delle foto, in ordine.
     */
    suspend fun insertScannedPages(blockId: String, files: List<String>, names: List<String?>): List<String> {
        if (files.isEmpty()) return emptyList()
        val block = blockDao.getById(blockId) ?: return emptyList()
        val photos = files.mapIndexed { i, file -> BlockAttrs(file = file, name = names.getOrNull(i)) }
        val empty = block.type in SCAN_REPLACES && block.textJson.let { text ->
            runCatching { json.decodeFromString<List<RichTextSpan>>(text) }.getOrNull()?.plainText().isNullOrBlank()
        }
        return db.withTransaction {
            val first = if (empty) {
                blockDao.setTypeAndAttrs(block.id, BlockType.IMAGE, BlockAttrs.encode(photos.first()))
                blockDao.setText(block.id, "[]")
                listOf(block.id)
            } else {
                emptyList()
            }
            val rest = (if (empty) photos.drop(1) else photos).map { NewBlock(type = BlockType.IMAGE, attrs = it) }
            first + insertBlocksAt(block.pageId, block.id, InsertPlace.BELOW_BUTTON, rest)
        }
    }

    // --- I blocchi sincronizzati (dal 03/11/2026): vedi `data/sync/SyncedBlocks.kt` ---

    /** Il contenuto di [root] com'è nel database, con le celle delle tabelle. */
    private suspend fun syncedLinesOf(root: BlockEntity): List<SyncedLine> =
        SyncedBlocks.subtree(root.id, blockDao.getBlocksForPageOnce(root.pageId)).map { (depth, block) ->
            val cells = if (block.type == BlockType.TABLE) tableCellDao.getCellsForBlockOnce(block.id) else emptyList()
            SyncedBlocks.lineOf(depth, block, cells)
        }

    /** Le copie del gruppo [syncId], in qualunque pagina. */
    private suspend fun syncedCopiesOf(syncId: String): List<BlockEntity> =
        blockDao.getSyncedBlocks(SyncedBlocks.patternOf(syncId)).filter { it.attrs.syncId == syncId }

    /**
     * **"Create two/three/four columns"** (dal 12/11/2026, chieste
     * dall'utente, solo sul PC, come su Notion): [count] colonne affiancate,
     * ognuna con una riga vuota. Una riga di testo vuota e senza righe dentro
     * diventa lei il gruppo di colonne; una con del testo resta com'è, e le
     * colonne vanno subito sotto, allo stesso livello. Restituisce l'id della
     * riga della prima colonna, per il cursore, o null se il blocco non c'è più.
     */
    suspend fun makeColumns(blockId: String, count: Int): String? = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction null
        val text = runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrNull().orEmpty().joinToString("") { it.text }
        val inPlace = text.isBlank() && block.type in COLUMN_SPOT_TYPES && blockDao.getChildren(block.id).isEmpty()
        val listId = if (inPlace) {
            blockDao.update(
                block.copy(
                    type = BlockType.COLUMN_LIST,
                    textJson = "[]",
                    isChecked = false,
                    numberStartsAt = null,
                    notifyAt = null,
                    linkedPageId = null,
                    indentLevel = 0,
                    attrsJson = "{}"
                )
            )
            block.id
        } else {
            blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex + 1, 1)
            BlockEntity(pageId = block.pageId, parentBlockId = block.parentBlockId, type = BlockType.COLUMN_LIST, orderIndex = block.orderIndex + 1)
                .also { blockDao.insert(it) }.id
        }
        val empty = json.encodeToString(listOf(RichTextSpan(text = "")))
        (0 until count).map { i ->
            val column = BlockEntity(pageId = block.pageId, parentBlockId = listId, type = BlockType.COLUMN, orderIndex = i)
            blockDao.insert(column)
            BlockEntity(pageId = block.pageId, parentBlockId = column.id, textJson = empty, orderIndex = 0).also { blockDao.insert(it) }.id
        }.first()
    }

    /** Le righe che, vuote, diventano loro stesse le colonne (vedi `makeColumns`): quelle di testo. */
    private val COLUMN_SPOT_TYPES = setOf(
        BlockType.PARAGRAPH, BlockType.BULLET_LIST_ITEM, BlockType.NUMBERED_LIST_ITEM,
        BlockType.CHECKBOX, BlockType.TOGGLE, BlockType.CALLOUT, BlockType.QUOTE
    )

    /**
     * **"Synced block" dei menu "/" e "Turn into"**: la riga diventa un
     * blocco sincronizzato, per ora solo qui, e quello che c'era scritto la
     * sua prima riga (i figli di un toggle dopo di lei). Restituisce l'id di
     * quella riga, per il cursore, o null se il blocco non c'è più.
     */
    suspend fun makeSynced(blockId: String): String? = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction null
        val keepsType = block.type in SyncedBlocks.FIRST_LINE_TYPES
        val firstId = UUID.randomUUID().toString()
        blockDao.shiftOrderIndexes(block.pageId, block.id, 0, 1)
        blockDao.update(
            block.copy(
                type = BlockType.SYNCED,
                textJson = "[]",
                isChecked = false,
                isExpanded = true,
                numberStartsAt = null,
                notifyAt = null,
                linkedPageId = null,
                attrsJson = BlockAttrs.encode(BlockAttrs(syncId = UUID.randomUUID().toString()))
            )
        )
        blockDao.insert(
            BlockEntity(
                id = firstId,
                pageId = block.pageId,
                parentBlockId = block.id,
                type = if (keepsType) block.type else BlockType.PARAGRAPH,
                textJson = block.textJson.takeIf { it != "[]" } ?: json.encodeToString(listOf(RichTextSpan(text = ""))),
                isChecked = keepsType && block.isChecked,
                numberStartsAt = block.numberStartsAt.takeIf { keepsType },
                attrsJson = if (keepsType) block.attrsJson else "{}",
                orderIndex = 0
            )
        )
        firstId
    }

    /**
     * **"Sync to another page"**: una copia del blocco sincronizzato
     * [sourceBlockId] in fondo alla pagina [pageId], con le stesse righe.
     * Restituisce l'id della copia, o null se il blocco non c'è più.
     */
    suspend fun addSyncedCopy(sourceBlockId: String, pageId: String): String? {
        val source = blockDao.getById(sourceBlockId)?.takeIf { it.type == BlockType.SYNCED } ?: return null
        val syncId = source.attrs.syncId ?: return null
        val copyId = insertBlocksAt(
            pageId, null, InsertPlace.BOTTOM_OF_PAGE,
            listOf(NewBlock(type = BlockType.SYNCED, attrs = BlockAttrs(syncId = syncId)))
        ).firstOrNull() ?: return null
        db.withTransaction {
            val copy = blockDao.getById(copyId) ?: return@withTransaction
            writeSyncedContent(copy, syncedLinesOf(blockDao.getById(sourceBlockId) ?: source))
        }
        return copyId
    }

    /**
     * **Il contenuto della copia [sourceBlockId] nelle altre copie**, dopo
     * che è cambiato: quelle già uguali non si toccano. Restituisce quante
     * ne ha riscritte. Vedi `SyncedBlocks`.
     */
    suspend fun pushSynced(sourceBlockId: String): Int = db.withTransaction {
        val source = blockDao.getById(sourceBlockId)?.takeIf { it.type == BlockType.SYNCED }
            ?: return@withTransaction 0
        val syncId = source.attrs.syncId ?: return@withTransaction 0
        val lines = syncedLinesOf(source)
        val key = SyncedBlocks.keyOf(lines)
        syncedCopiesOf(syncId).count { copy ->
            copy.id != source.id && writeSyncedContent(copy, lines).also { written ->
                if (written) SyncedBlocks.expect(copy.id, key)
            }
        }
    }

    /**
     * Le righe di [target] diventano [lines]. Con la stessa forma (tante
     * righe, ognuna allo stesso livello e dello stesso tipo) si riscrivono
     * al loro posto, con gli stessi id: un campo aperto su una di loro resta
     * suo. Se no si rifanno da capo. La casa di una sottopagina resta casa
     * se la pagina c'è ancora. Falso se era già uguale. Dentro una transazione.
     */
    private suspend fun writeSyncedContent(target: BlockEntity, lines: List<SyncedLine>): Boolean {
        val old = SyncedBlocks.subtree(target.id, blockDao.getBlocksForPageOnce(target.pageId))
        val oldLines = old.map { (depth, block) ->
            val cells = if (block.type == BlockType.TABLE) tableCellDao.getCellsForBlockOnce(block.id) else emptyList()
            SyncedBlocks.lineOf(depth, block, cells)
        }
        if (oldLines == lines) return false
        val sameShape = oldLines.size == lines.size &&
            oldLines.indices.all { oldLines[it].depth == lines[it].depth && oldLines[it].type == lines[it].type }
        if (sameShape) {
            old.forEachIndexed { i, (_, block) ->
                val line = lines[i]
                if (oldLines[i] == line) return@forEachIndexed
                val home = block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK
                if (home && BlockAttrs.parse(line.attrsJson).pageId == block.linkedPageId) {
                    blockDao.setIndentLevel(block.id, line.indentLevel)
                } else {
                    blockDao.update(syncedEntityOf(line, block.id, block.pageId, block.parentBlockId, block.orderIndex))
                }
                if (line.type == BlockType.TABLE && line.cells != oldLines[i].cells) writeSyncedCells(block.id, line)
            }
        } else {
            val homes = old.map { it.second }
                .filter { it.type == BlockType.PAGE_LINK || it.type == BlockType.DATABASE_LINK }
                .filter { it.linkedPageId != null }
                .associateBy { it.linkedPageId }
                .toMutableMap()
            blockDao.getChildren(target.id).forEach { blockDao.delete(it) }
            val parents = mutableListOf<String>()
            val nextOrder = mutableMapOf<String, Int>()
            for (line in lines) {
                while (parents.size > line.depth) parents.removeAt(parents.lastIndex)
                val parentId = parents.lastOrNull() ?: target.id
                val order = nextOrder[parentId] ?: 0
                nextOrder[parentId] = order + 1
                val id = UUID.randomUUID().toString()
                val home = if (line.type == BlockType.PAGE_MENTION) {
                    BlockAttrs.parse(line.attrsJson).pageId?.let { homes.remove(it) }
                } else {
                    null
                }
                blockDao.insert(
                    home?.copy(id = id, parentBlockId = parentId, orderIndex = order, indentLevel = line.indentLevel)
                        ?: syncedEntityOf(line, id, target.pageId, parentId, order)
                )
                if (line.type == BlockType.TABLE) writeSyncedCells(id, line)
                parents += id
            }
        }
        return true
    }

    private fun syncedEntityOf(line: SyncedLine, id: String, pageId: String, parentId: String?, order: Int) =
        BlockEntity(
            id = id,
            pageId = pageId,
            parentBlockId = parentId,
            type = line.type,
            textJson = line.textJson,
            isChecked = line.isChecked,
            isExpanded = line.isExpanded,
            tableRows = line.tableRows,
            tableCols = line.tableCols,
            numberStartsAt = line.numberStartsAt,
            indentLevel = line.indentLevel,
            orderIndex = order,
            notifyAt = line.notifyAt,
            attrsJson = line.attrsJson
        )

    private suspend fun writeSyncedCells(tableId: String, line: SyncedLine) {
        tableCellDao.deleteCellsForBlock(tableId)
        line.cells.forEach { (row, col, text) ->
            tableCellDao.insertCell(TableCellEntity(blockId = tableId, rowIndex = row, colIndex = col, text = text))
        }
    }

    /**
     * **"Unsync"**: questa copia torna righe normali, al posto del blocco; le
     * altre copie restano sincronizzate fra loro. Falso se non c'è più.
     */
    suspend fun unsync(blockId: String): Boolean = db.withTransaction {
        val block = blockDao.getById(blockId)?.takeIf { it.type == BlockType.SYNCED }
            ?: return@withTransaction false
        val children = blockDao.getChildren(block.id).sortedBy { it.orderIndex }
        if (children.size > 1) {
            blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex + 1, children.size - 1)
        }
        children.forEachIndexed { i, child ->
            blockDao.setParentAndOrder(child.id, block.parentBlockId, block.orderIndex + i)
        }
        blockDao.delete(block)
        true
    }

    /** In quante pagine, fuori dal cestino, c'è una copia del gruppo [syncId]. */
    suspend fun syncedPageCount(syncId: String): Int {
        val pageIds = syncedCopiesOf(syncId).map { it.pageId }.distinct()
        if (pageIds.isEmpty()) return 0
        return pageDao.getByIds(pageIds).count { it.trashedAt == null }
    }

    /**
     * **"Insert blocks" di un bottone** (dal 27/10/2026): [blocks] in fila,
     * sopra o sotto il blocco [anchorBlockId] e fra i suoi fratelli, o fra
     * quelli di primo livello in cima o in fondo alla pagina. Si fa spazio
     * spostando avanti quelli dopo, come per l'Invio. Restituisce gli id dei
     * blocchi nuovi, in ordine.
     */
    suspend fun insertBlocksAt(
        pageId: String,
        anchorBlockId: String?,
        place: InsertPlace,
        blocks: List<NewBlock>
    ): List<String> = db.withTransaction {
        if (blocks.isEmpty()) return@withTransaction emptyList()
        val anchor = anchorBlockId?.let { blockDao.getById(it) }?.takeIf { it.pageId == pageId }
        val roots = blockDao.getBlocksForPageOnce(pageId).filter { it.parentBlockId == null }
        val (parent, at) = when {
            place == InsertPlace.ABOVE_BUTTON && anchor != null -> anchor.parentBlockId to anchor.orderIndex
            place == InsertPlace.BELOW_BUTTON && anchor != null -> anchor.parentBlockId to anchor.orderIndex + 1
            place == InsertPlace.TOP_OF_PAGE -> null to (roots.minOfOrNull { it.orderIndex } ?: 0)
            else -> null to ((roots.maxOfOrNull { it.orderIndex } ?: -1) + 1)
        }
        blockDao.shiftOrderIndexes(pageId, parent, at, blocks.size)
        blocks.mapIndexed { i, spec ->
            val entity = BlockEntity(
                pageId = pageId,
                parentBlockId = parent,
                type = spec.type,
                textJson = json.encodeToString(spec.spans),
                attrsJson = BlockAttrs.encode(spec.attrs),
                indentLevel = spec.indentLevel,
                orderIndex = at + i
            )
            blockDao.insert(entity)
            entity.id
        }
    }

    /**
     * **Una riga vuota sopra o sotto un blocco** (`below`), fra i suoi
     * fratelli: "Add above" e "Add below" del menu dei sei puntini (dal
     * 29/09/2026). Come per l'Invio, il posto si legge dal database dentro la
     * transazione e si fa spazio spostando avanti di uno quelli dopo.
     * La riga nuova è un paragrafo col rientro del blocco. Falso se il
     * blocco non c'è più.
     */
    suspend fun insertLineNextTo(blockId: String, newBlockId: String, below: Boolean, textJson: String): Boolean =
        db.withTransaction {
            val block = blockDao.getById(blockId) ?: return@withTransaction false
            val at = if (below) block.orderIndex + 1 else block.orderIndex
            blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, at, 1)
            blockDao.insert(
                BlockEntity(
                    id = newBlockId,
                    pageId = block.pageId,
                    parentBlockId = block.parentBlockId,
                    textJson = textJson,
                    indentLevel = block.indentLevel,
                    orderIndex = at
                )
            )
            true
        }

    /**
     * Divide un blocco mettendo la parte prima del cursore in un blocco
     * **nuovo sopra**, e lasciando a quello di partenza la parte dopo.
     *
     * Il punto di tutto è che il blocco di partenza resta lo stesso, con
     * il suo campo di testo e la tastiera attaccata (vedi il commento in
     * `splitBlockAt`). Sta qui e non nel ViewModel perché l'indice va
     * **riletto dal database dentro la transazione**: andando a capo in
     * fretta la copia che ha in mano la schermata è di un a-capo fa, e
     * usare quell'indice creava due righe con lo stesso posto.
     *
     * Restituisce false se il blocco non c'era più e non ha diviso niente:
     * il ViewModel la riga nuova l'ha già mostrata, e deve saperlo per
     * toglierla.
     */
    suspend fun splitBlockAbove(
        blockId: String,
        newBlockId: String,
        newBlockType: BlockType,
        textBeforeJson: String,
        textAfterJson: String
    ): Boolean = db.withTransaction {
        val current = blockDao.getById(blockId) ?: return@withTransaction false
        blockDao.shiftOrderIndexes(
            current.pageId,
            current.parentBlockId,
            current.orderIndex,
            1
        )
        blockDao.insertAll(
            listOf(
                BlockEntity(
                    id = newBlockId,
                    pageId = current.pageId,
                    parentBlockId = current.parentBlockId,
                    orderIndex = current.orderIndex,
                    type = newBlockType,
                    textJson = textBeforeJson,
                    // La spunta e il "riparti da N" appartengono alla
                    // riga di sopra, che continua quella che c'era.
                    isChecked = current.isChecked,
                    numberStartsAt = current.numberStartsAt,
                    indentLevel = current.indentLevel,
                    // Il posto di un elenco, se la riga lo tiene: vedi `BlockAttrs.listIndent`.
                    attrsJson = current.attrsForNewLine()
                ),
                current.copy(
                    orderIndex = current.orderIndex + 1,
                    textJson = textAfterJson,
                    // Una riga appena nata non è ancora fatta.
                    isChecked = false,
                    numberStartsAt = null
                )
            )
        )
        true
    }

    /**
     * Un blocco diventa divisore e sotto nasce una riga vuota, dove si
     * continua a scrivere. Del blocco si cambia **solo il tipo**: prima
     * veniva risalvato per intero dalla copia della schermata, che dal
     * menu "/" aveva ancora dentro il comando appena tolto — il divisore
     * si teneva addosso il suo "/d", invisibile ma nel database.
     */
    suspend fun turnIntoDividerWithLineBelow(
        blockId: String,
        newBlockId: String,
        /**
         * Il testo che scende nella riga nuova, da "Turn into" nel menu del
         * blocco: il divisore non lo mostra, e lì sarebbe sparito alla
         * vista. Null dal menu "/", dove la riga è vuota.
         */
        carriedTextJson: String? = null
    ) =
        db.withTransaction {
            val current = blockDao.getById(blockId) ?: return@withTransaction
            blockDao.setType(current.id, BlockType.DIVIDER)
            blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
            blockDao.insert(
                BlockEntity(
                    id = newBlockId,
                    pageId = current.pageId,
                    parentBlockId = current.parentBlockId,
                    orderIndex = current.orderIndex + 1,
                    textJson = carriedTextJson ?: "[]"
                )
            )
            // **Il divisore resta senza testo**, sempre (dal 14/10/2026): un
            // divisore non lo mostra, e quello che gli restava addosso
            // finiva nella ricerca e nel conteggio delle parole. Fatto con
            // "---", sul telefono il divisore si teneva i suoi tre trattini,
            // anche se il campo li aveva già tolti: un salvataggio del testo
            // arrivato prima di questo cambio di tipo.
            blockDao.setText(current.id, json.encodeToString(listOf(RichTextSpan(text = ""))))
        }

    /**
     * Una riga nuova come **primo figlio** di un blocco (il contenuto di
     * un toggle): i figli che c'erano scalano di un posto.
     */
    suspend fun insertFirstChild(parent: BlockEntity, newBlockId: String, textJson: String) =
        db.withTransaction {
            blockDao.shiftOrderIndexes(parent.pageId, parent.id, 0, 1)
            blockDao.insert(
                BlockEntity(
                    id = newBlockId,
                    pageId = parent.pageId,
                    parentBlockId = parent.id,
                    orderIndex = 0,
                    textJson = textJson
                )
            )
        }

    /**
     * Una riga nuova **subito sotto** un blocco, allo stesso livello: i
     * fratelli che seguono scalano di un posto. Rilegge il blocco dal
     * database invece di fidarsi della copia in mano a chi chiama, per
     * la stessa ragione di `splitBlockAbove`: con due Invio ravvicinati
     * quella copia ha già la posizione vecchia.
     *
     * Restituisce false se il blocco non c'era più, come `splitBlockAbove`.
     */
    suspend fun insertSiblingBelow(
        block: BlockEntity,
        newBlockId: String,
        type: BlockType,
        textJson: String
    ): Boolean = db.withTransaction {
        val current = blockDao.getById(block.id) ?: return@withTransaction false
        blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
        blockDao.insert(
            BlockEntity(
                id = newBlockId,
                pageId = current.pageId,
                parentBlockId = current.parentBlockId,
                orderIndex = current.orderIndex + 1,
                type = type,
                textJson = textJson,
                // Un toggle nuovo nasce chiuso come quello da cui viene:
                // si stava lavorando a toggle chiusi, non c'è motivo di
                // aprirne uno di colpo.
                isExpanded = current.isExpanded
            )
        )
        true
    }

    /**
     * Cambia tipo a un blocco che ha dei figli — un toggle che torna
     * testo — portando i figli **fuori**, subito sotto di lui e allo
     * stesso livello, nell'ordine in cui erano.
     *
     * I fratelli che seguivano il blocco scalano di tanti posti quanti
     * sono i figli, così nessuno si ritrova con lo stesso numero d'ordine
     * di un altro. Tutto in una transazione: a metà strada la pagina
     * avrebbe righe col genitore sbagliato.
     */
    suspend fun unnestAndConvert(blockId: String, newType: BlockType) = db.withTransaction {
        unnestChildren(blockId)
        blockDao.setType(blockId, newType)
    }

    /**
     * Solo la prima metà di `unnestAndConvert`: i figli escono, il tipo
     * resta. Serve prima delle trasformazioni che non sono un semplice
     * cambio di tipo — in pagina, in database, in tabella, in divisore —
     * dove i figli di un toggle sarebbero rimasti appesi a un blocco che
     * non li mostra, spariti alla vista.
     */
    suspend fun unnestChildren(blockId: String) = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction
        val children = blockDao.getChildren(blockId)
        if (children.isNotEmpty()) {
            blockDao.shiftOrderIndexes(
                block.pageId,
                block.parentBlockId,
                block.orderIndex + 1,
                children.size
            )
            children.forEachIndexed { i, child ->
                blockDao.update(
                    child.copy(
                        parentBlockId = block.parentBlockId,
                        orderIndex = block.orderIndex + 1 + i
                    )
                )
            }
        }
    }

    suspend fun deleteBlock(block: BlockEntity) = blockDao.delete(block)

    /**
     * **Cambia le voci in più di un blocco** (`BlockAttrs`) leggendole dal
     * database, dentro la transazione: due ritocchi di fila — l'icona e poi
     * il colore di un riquadro — non si cancellano a vicenda come farebbero
     * partendo dalla copia della schermata.
     */
    suspend fun updateBlockAttrs(blockId: String, change: (BlockAttrs) -> BlockAttrs) = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction
        val updated = change(block.attrs)
        if (updated != block.attrs) blockDao.setAttrs(blockId, BlockAttrs.encode(updated))
    }

    /**
     * **"Database - Full page"** (dal 06/10/2026): la riga diventa il
     * collegamento a un database nuovo, che si apre a pagina intera invece di
     * stare dentro la pagina — come un database dopo "Turn into page". Il
     * nome è `title` (il testo della riga, da "Turn into") o "Untitled".
     * Restituisce l'id del database.
     */
    suspend fun makeFullPageDatabase(blockId: String, title: String?): String = db.withTransaction {
        unnestChildren(blockId)
        val database = PageEntity(title = title?.takeIf { it.isNotBlank() } ?: "Untitled", isDatabase = true)
        pageDao.insert(database)
        blockDao.setTypeAndLinkClearingText(blockId, BlockType.PAGE_LINK, database.id)
        database.id
    }

    /**
     * **Sposta un blocco in fondo a un'altra pagina** (dal 06/10/2026, per
     * l'immagine: "spostarla da qualche altra parte, di default sempre in
     * fondo alla pagina di destinazione"). I blocchi rimasti si chiudono sul
     * buco che lascia. Falso se il blocco non c'è più.
     */
    suspend fun moveBlockToPage(blockId: String, destinationPageId: String): Boolean = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction false
        val last = blockDao.lastOrderIndex(destinationPageId) ?: -1
        blockDao.moveToPage(blockId, destinationPageId, last + 1)
        if (block.pageId != destinationPageId) {
            blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex + 1, -1)
        }
        true
    }

    /** Una riga diventa un blocco senza testo (immagine, video, link a una pagina…): vedi `BlockDao.setTypeAndAttrs`. */
    suspend fun setBlockTypeAndAttrs(blockId: String, type: BlockType, attrs: BlockAttrs) = db.withTransaction {
        unnestChildren(blockId)
        blockDao.setTypeAndAttrs(blockId, type, BlockAttrs.encode(attrs))
    }

    /**
     * **Un blocco scritto su più righe cambia tipo** (dal 06/10/2026): un
     * riquadro, una citazione o del codice che diventano testo, elenco,
     * casella o toggle.
     *
     * Quei tipi tengono tutte le loro righe in un blocco solo, separate da
     * a-capo; gli altri ne tengono una per blocco, e il testo condiviso conta
     * le righe dagli a-capo. Un blocco di testo con dentro tre righe ne
     * mostrerebbe tre e ne salverebbe una: qui la prima resta nel blocco,
     * col suo nuovo tipo, e le altre diventano blocchi subito sotto, ognuna
     * con la sua formattazione. Per un toggle le righe in più sono testo:
     * tre toggle uno sotto l'altro non sono quello che si voleva.
     */
    suspend fun convertMultiline(blockId: String, newType: BlockType) = db.withTransaction {
        if (newType != BlockType.TOGGLE) unnestChildren(blockId)
        val block = blockDao.getById(blockId) ?: return@withTransaction
        blockDao.setType(blockId, newType)
        if (newType in MULTILINE_TYPES) return@withTransaction
        val lines = (runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrNull() ?: emptyList())
            .splitLines()
        if (lines.size <= 1) return@withTransaction
        blockDao.setText(blockId, json.encodeToString(lines.first()))
        blockDao.shiftOrderIndexes(block.pageId, block.parentBlockId, block.orderIndex + 1, lines.size - 1)
        val restType = if (newType == BlockType.TOGGLE) BlockType.PARAGRAPH else newType
        lines.drop(1).forEachIndexed { i, line ->
            blockDao.insert(
                BlockEntity(
                    pageId = block.pageId,
                    parentBlockId = block.parentBlockId,
                    type = restType,
                    textJson = json.encodeToString(line),
                    indentLevel = block.indentLevel,
                    orderIndex = block.orderIndex + 1 + i
                )
            )
        }
    }

    /** Cambia solo da che numero riparte un elemento di elenco, senza riscrivere il testo. */
    suspend fun setNumberStart(blockId: String, startsAt: Int?) =
        blockDao.setNumberStart(blockId, startsAt)

    /** Cambia solo il testo di un blocco, senza toccarne posto e stato. */
    suspend fun setBlockText(blockId: String, textJson: String) =
        blockDao.setText(blockId, textJson)

    /**
     * "Turn into page": il database dentro una pagina smette di essere
     * mostrato lì e diventa un collegamento, che si apre a schermo
     * intero. Il database non viene toccato — righe, colonne, viste,
     * filtri restano tutti — cambia solo **il blocco che lo mostra**.
     */
    suspend fun turnDatabaseBlockIntoPage(blockId: String) =
        blockDao.setType(blockId, BlockType.PAGE_LINK)

    /**
     * "Turn into database": il contrario. Ogni collegamento a pagina che
     * richiama questo database torna a mostrarlo dentro la pagina.
     */
    suspend fun turnPageLinksIntoDatabase(databasePageId: String) =
        blockDao.retypeLinks(databasePageId, BlockType.PAGE_LINK, BlockType.DATABASE_LINK)

    /** Se da qualche parte questo database è richiamato come pagina, e quindi si può rimettere dentro. */
    suspend fun isShownAsPage(databasePageId: String): Boolean =
        blockDao.countLinks(databasePageId, BlockType.PAGE_LINK) > 0

    /**
     * "Turn into database" dal menu di un blocco: **quel** collegamento
     * torna a mostrare il database dentro la pagina. A differenza della
     * voce dei tre puntini (`turnPageLinksIntoDatabase`) gli altri
     * collegamenti allo stesso database, se ce ne sono, restano come sono:
     * si è scelto un blocco, non il database.
     */
    suspend fun turnPageLinkIntoDatabase(blockId: String) =
        blockDao.setType(blockId, BlockType.DATABASE_LINK)

    /**
     * "Rename" dal menu di un blocco: cambia solo il titolo. Se la pagina
     * è quella di una riga, cambia anche il nome della riga, come quando
     * la si rinomina da dentro (vedi `updatePage`).
     */
    suspend fun renamePage(pageId: String, title: String) {
        val now = System.currentTimeMillis()
        db.withTransaction {
            pageDao.setTitle(pageId, title, now)
            db.databaseDao().setRowTitleByLinkedPage(pageId, title, now)
        }
    }

    /**
     * Quante pagine delle righe di un database hanno qualcosa dentro —
     * del testo, un blocco che non sia una riga vuota, un'icona, una
     * copertina. È quello che "Turn into simple database" butterebbe via
     * per sempre: una pagina di riga mai scritta non conta, perde solo il
     * nome, che resta alla riga.
     */
    suspend fun countRowPagesWithContent(databasePageId: String): Int =
        db.databaseDao().getRowsForPageOnce(databasePageId)
            .mapNotNull { it.linkedPageId }
            .distinct()
            .count { rowPageId ->
                val page = pageDao.getById(rowPageId) ?: return@count false
                page.iconImage != null || page.coverImage != null ||
                    blockDao.getBlocksForPageOnce(rowPageId).any { block ->
                        block.type != BlockType.PARAGRAPH || runCatching {
                            json.decodeFromString<List<RichTextSpan>>(block.textJson)
                        }.getOrNull()?.plainText().orEmpty().isNotBlank()
                    }
            }

    /**
     * "Turn into simple database": le righe tornano solo testo, e **le
     * loro pagine si cancellano per sempre**, con quello che c'è dentro —
     * non nel cestino: una pagina di riga nel cestino non avrebbe più una
     * riga a cui tornare, perché le righe di un database semplice non ne
     * hanno. Nome e proprietà delle righe restano.
     *
     * Restituisce i file immagine da togliere dalla cartella, come
     * `deletePermanently`.
     */
    suspend fun turnIntoSimpleDatabase(databasePageId: String): List<String> {
        val databaseDao = db.databaseDao()
        return db.withTransaction {
            val rowPages = databaseDao.getRowsForPageOnce(databasePageId)
                .mapNotNull { it.linkedPageId }
                .distinct()
            val files = rowPages.flatMap { deletePermanently(it) }
            databaseDao.unlinkRowPages(databasePageId)
            pageDao.setSimpleDatabase(databasePageId, true)
            files
        }
    }

    // --- Viste collegate ("Linked view of data source") ---

    /** Un database che una vista collegata può mostrare, e dove sta, per riconoscerlo fra due omonimi. */
    data class LinkableDatabase(val page: PageEntity, val place: String?)

    /**
     * I database fra cui scegliere quello da mostrare in una vista
     * collegata, ognuno con la pagina in cui sta (o null se sta nel menu
     * principale, o in nessun posto).
     */
    suspend fun linkableDatabases(): List<LinkableDatabase> =
        pageDao.getLinkableDatabases().map { database ->
            val container = blockDao.getBlocksLinkingTo(database.id)
                .firstNotNullOfOrNull { link -> pageDao.getById(link.pageId)?.takeIf { it.trashedAt == null } }
            LinkableDatabase(database, container?.takeIf { it.id != PageEntity.ROOT_PAGE_ID }?.title)
        }

    /**
     * Crea una vista collegata del database `sourceId` e ne restituisce
     * l'id, o null se il database non c'è più.
     *
     * **Nasce identica all'originale**, come chiesto: stesso layout,
     * filtro, ordinamento, raggruppamento, galleria, calendario, e le
     * stesse proprietà nascoste — poi si cambia quello che si vuole, qui
     * soltanto. Il nome è quello del database (serve dove la vista si vede
     * come pagina: preferiti, cestino); dentro la pagina si legge sempre
     * quello vivo dell'origine.
     */
    suspend fun createLinkedView(sourceId: String): String? {
        val source = pageDao.getById(sourceId)?.takeIf { it.isDatabase } ?: return null
        // Una vista di una vista mostra il database vero.
        val realSource = source.sourceDatabaseId?.let { pageDao.getById(it) } ?: source
        val hidden = db.databaseDao().getColumnsForPageOnce(realSource.id).filter { it.hidden }.map { it.id }
        val now = System.currentTimeMillis()
        val view = realSource.copy(
            id = UUID.randomUUID().toString(),
            parentId = null,
            icon = DEFAULT_PAGE_EMOJI,
            iconImage = null,
            coverImage = null,
            isSimpleDatabase = false,
            sourceDatabaseId = realSource.id,
            viewHiddenColumnIds = hidden.joinToString(",").ifEmpty { null },
            // Le viste in più del database restano sue.
            activeViewId = null,
            isRowPage = false,
            isFavorite = false,
            favoritedAt = null,
            isLocked = false,
            isViewLocked = false,
            trashedAt = null,
            createdAt = now,
            updatedAt = now
        )
        pageDao.insert(view)
        return view.id
    }

    // --- Le viste di un database (dal 29/09/2026) ---

    fun observeViews(hostId: String): Flow<List<PageEntity>> = pageDao.observeViews(hostId)

    /**
     * **Una vista nuova** del database `hostId`, col suo nome e il suo
     * layout, e la si mette davanti. È fatta come una vista collegata: i dati
     * sono quelli del database vero (per una vista collegata, del suo
     * originale), le impostazioni tutte sue e **da zero**, come su Notion —
     * niente filtro, ordinamento o raggruppamento, tutte le proprietà in
     * vista. Restituisce l'id, o null se il database non c'è più.
     *
     * **Con le righe di un altro database** ([sourceId], dal 21/10/2026, come
     * le "data sources" di Notion): la vista sta qui ma mostra quelle. Senza
     * un nome suo prende quello dell'altro database.
     */
    suspend fun createView(hostId: String, name: String?, layout: DatabaseLayout, sourceId: String? = null): String? = db.withTransaction {
        val host = pageDao.getById(hostId)?.takeIf { it.isDatabase } ?: return@withTransaction null
        val own = host.sourceDatabaseId ?: host.id
        val source = sourceId?.takeIf { it != own }?.let { id ->
            pageDao.getById(id)?.takeIf { it.isDatabase && it.sourceDatabaseId == null && it.trashedAt == null }
                ?: return@withTransaction null
        }
        val order = (pageDao.getViewsOnce(hostId).maxOfOrNull { it.orderIndex } ?: -1) + 1
        val now = System.currentTimeMillis()
        val view = PageEntity(
            title = host.title,
            isDatabase = true,
            sourceDatabaseId = source?.id ?: own,
            viewHostId = host.id,
            viewName = name?.trim()?.ifEmpty { null } ?: source?.title?.ifBlank { null },
            databaseLayout = layout,
            orderIndex = order,
            createdAt = now,
            updatedAt = now
        )
        pageDao.insert(view)
        pageDao.setActiveView(host.id, view.id)
        view.id
    }

    /** Il nome della proprietà principale di un database: vedi `PageEntity.nameColumnTitle`. */
    suspend fun setNameColumnTitle(databaseId: String, title: String?) =
        pageDao.setNameColumnTitle(databaseId, title)

    /** "Hide icon" della proprietà principale di un database: vedi `PageEntity.hideNameIcon`. */
    suspend fun setHideNameIcon(databaseId: String, hidden: Boolean) = pageDao.setHideNameIcon(databaseId, hidden)

    /** Quale vista di un database si vede: null (o il database stesso) = la sua. */
    suspend fun setActiveView(hostId: String, viewId: String?) =
        pageDao.setActiveView(hostId, viewId?.takeIf { it != hostId })

    suspend fun renameView(pageId: String, name: String?) =
        pageDao.setViewName(pageId, name?.trim()?.ifEmpty { null })

    /**
     * **Togliere la vista di partenza** (dal 08/10/2026, chiesto
     * dall'utente: in "Foreign Languages" si poteva togliere la List e non
     * la Table). La vista di partenza è il database stesso, che non può
     * andarsene: prende allora **le impostazioni della vista che viene dopo**
     * (layout, nome, filtro, ordinamento, gruppi, colonne nascoste e
     * larghezze, calcoli, grafico…), e quella se ne va. Per chi guarda, la
     * prima vista è sparita e la seconda è diventata la prima, come su
     * Notion. Non si toglie l'ultima vista rimasta: restituisce false.
     *
     * Prende la prima vista **con le stesse righe** (dal 21/10/2026): una
     * vista che mostra un altro database non può diventare il database. Se
     * non ce n'è, la vista di partenza resta: false.
     */
    suspend fun deleteStartingView(hostId: String): Boolean = db.withTransaction {
        val host = pageDao.getById(hostId)?.takeIf { it.isDatabase } ?: return@withTransaction false
        val own = host.sourceDatabaseId ?: host.id
        val next = pageDao.getViewsOnce(hostId).firstOrNull { it.sourceDatabaseId == own } ?: return@withTransaction false
        val databaseDao = db.databaseDao()
        // Le colonne nascoste: in una vista sono una lista sua; nel database
        // vero sono le colonne stesse. In una vista collegata, la sua lista.
        val hidden = next.viewHiddenColumnIds?.split(',')?.filter { it.isNotBlank() }?.toSet().orEmpty()
        if (host.sourceDatabaseId == null) {
            databaseDao.getColumnsForPageOnce(host.id).forEach { column ->
                val hide = column.id in hidden
                if (column.hidden != hide) databaseDao.updateColumn(column.copy(hidden = hide))
            }
        }
        pageDao.update(
            host.copy(
                databaseLayout = next.databaseLayout,
                viewName = next.viewName,
                // I raggruppamenti della vista dopo (dal 28/10/2026 un elenco, coi loro
                // "Hide empty groups" e "Date by").
                tableGroupRulesJson = next.tableGroupRulesJson,
                boardGroupRulesJson = next.boardGroupRulesJson,
                columnWidths = next.columnWidths,
                fitColumns = next.fitColumns,
                cellOverflow = next.cellOverflow,
                freezeFirstColumn = next.freezeFirstColumn,
                freezeFirstRow = next.freezeFirstRow,
                colorBoardCards = next.colorBoardCards,
                boardHideNew = next.boardHideNew,
                calendarDateColumnId = next.calendarDateColumnId,
                calendarWeekView = next.calendarWeekView,
                calendarMode = next.calendarMode,
                timelineZoom = next.timelineZoom,
                galleryCardPreview = next.galleryCardPreview,
                galleryCardSize = next.galleryCardSize,
                sortRulesJson = next.sortRulesJson,
                filterColumnId = next.filterColumnId,
                filterValue = next.filterValue,
                viewCalculations = next.viewCalculations,
                chartSettings = next.chartSettings,
                isViewLocked = next.isViewLocked,
                viewHiddenColumnIds = if (host.sourceDatabaseId == null) host.viewHiddenColumnIds else next.viewHiddenColumnIds,
                // Se davanti c'era quella che se ne va, ora è il database a mostrarla.
                activeViewId = host.activeViewId?.takeIf { it != next.id },
                updatedAt = System.currentTimeMillis()
            )
        )
        pageDao.delete(next)
        true
    }

    /** Toglie una vista in più; se era davanti, torna quella del database. */
    suspend fun deleteView(viewId: String) = db.withTransaction {
        val view = pageDao.getById(viewId)?.takeIf { it.viewHostId != null } ?: return@withTransaction
        val host = view.viewHostId?.let { pageDao.getById(it) }
        if (host?.activeViewId == viewId) pageDao.setActiveView(host.id, null)
        pageDao.delete(view)
    }

    /** Vedi `BlockDao.setTypeAndLink`. */
    suspend fun setBlockTypeAndLink(blockId: String, type: BlockType, linkedPageId: String) =
        blockDao.setTypeAndLink(blockId, type, linkedPageId)

    /** Le proprietà nascoste di una vista collegata, solo sue. */
    suspend fun setViewHiddenColumns(viewPageId: String, columnIds: Set<String>) =
        pageDao.setViewHiddenColumns(viewPageId, columnIds.joinToString(",").ifEmpty { null })

    /**
     * Una riga nuova sotto un blocco, che richiama una pagina: serve a
     * "Turn into → Linked view" su una riga che ha del testo, che così
     * resta dov'è invece di sparire dentro la vista.
     */
    suspend fun insertLinkBelow(blockId: String, linkedPageId: String, type: BlockType): String? =
        db.withTransaction {
            val current = blockDao.getById(blockId) ?: return@withTransaction null
            blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
            val link = BlockEntity(
                pageId = current.pageId,
                parentBlockId = current.parentBlockId,
                type = type,
                linkedPageId = linkedPageId,
                orderIndex = current.orderIndex + 1
            )
            blockDao.insert(link)
            link.id
        }

    /**
     * "Turn into complex database": le righe tornano a essere pagine.
     * Ognuna nasce la prima volta che la si apre, come in ogni altro
     * database (`ensureRowPage`), e da subito compare nella barra laterale
     * sotto il database.
     */
    suspend fun turnIntoComplexDatabase(databasePageId: String) =
        pageDao.setSimpleDatabase(databasePageId, false)

    /** Apre o chiude un toggle, senza riscriverne il testo. */
    suspend fun setBlockExpanded(blockId: String, expanded: Boolean) =
        blockDao.setExpanded(blockId, expanded)

    /**
     * Cambia solo il tipo di un blocco, senza riscriverne il testo. Il posto
     * di un elenco che il blocco teneva (`BlockAttrs.listIndent`) se ne va:
     * diventato altro e poi di nuovo testo, non deve tornare rientrato.
     */
    suspend fun setBlockType(blockId: String, type: BlockType) = db.withTransaction {
        blockDao.setType(blockId, type)
        dropListIndent(blockId)
    }

    /** Toglie a un blocco il posto dell'elenco, se lo teneva. */
    private suspend fun dropListIndent(blockId: String) {
        val block = blockDao.getById(blockId) ?: return
        val attrs = BlockAttrs.parse(block.attrsJson)
        if (attrs.listIndent != null) blockDao.setAttrs(blockId, BlockAttrs.encode(attrs.copy(listIndent = null)))
    }

    /**
     * **Il primo backspace a inizio di un elemento di elenco** (dal
     * 18/10/2026, come su Word): diventa testo, ma tiene il posto
     * dell'elenco — il numero o il pallino non ci sono più e il testo resta
     * allineato agli altri. Vedi `BlockAttrs.listIndent`.
     */
    suspend fun makeListText(blockId: String, listType: BlockType) = db.withTransaction {
        val block = blockDao.getById(blockId) ?: return@withTransaction
        blockDao.setType(blockId, BlockType.PARAGRAPH)
        val attrs = BlockAttrs.parse(block.attrsJson).copy(listIndent = listType.name)
        blockDao.setAttrs(blockId, BlockAttrs.encode(attrs))
    }

    /** Il secondo backspace: il testo lascia il posto dell'elenco e va a sinistra. */
    suspend fun releaseListIndent(blockId: String) = db.withTransaction { dropListIndent(blockId) }

    /**
     * **Più righe cambiano tipo insieme** (dal 14/10/2026, il "+" su del testo
     * selezionato): in una transazione sola, così la pagina le vede cambiare
     * tutte nello stesso istante e non una dopo l'altra. Come per una riga
     * sola, i figli di un toggle che smette di esserlo escono subito sotto
     * (`unnestChildren`), e un toggle nuovo nasce aperto.
     */
    suspend fun setBlockTypes(blockIds: List<String>, type: BlockType) = db.withTransaction {
        for (id in blockIds) {
            val block = blockDao.getById(id) ?: continue
            if (block.type == type) continue
            if (type != BlockType.TOGGLE) unnestChildren(id)
            blockDao.setType(id, type)
            dropListIndent(id)
            if (type == BlockType.TOGGLE) blockDao.setExpanded(id, true)
        }
    }

    /**
     * **Blocca o sblocca una pagina con la password.** Un database bloccato
     * che si vede dentro una pagina ci diventa un collegamento, come con
     * "Turn into page", richiesto così dall'utente: altrimenti le sue righe
     * resterebbero lì sotto gli occhi di tutti. Sbloccandolo resta un
     * collegamento; per rimetterlo dentro c'è "Turn into database".
     */
    suspend fun setProtected(pageId: String, protected: Boolean) {
        pageDao.setProtected(pageId, protected)
        if (protected && pageDao.getById(pageId)?.isDatabase == true) {
            blockDao.retypeLinks(pageId, BlockType.DATABASE_LINK, BlockType.PAGE_LINK)
        }
        AppLock.protectionChanged()
    }

    /** Pagine e sezioni dei quaderni bloccate con la password. */
    suspend fun countProtectedPages(): Int = pageDao.countProtected() + db.notebookDao().countProtectedSections()

    /**
     * **La pagina bloccata che chiude questa**: lei stessa, o una che la
     * contiene — risalendo i collegamenti, dal database di una riga alla
     * pagina che richiama il database, fino in cima; e per una vista
     * collegata, il database che mostra. Null se nessuna è bloccata.
     */
    suspend fun lockingPageOf(pageId: String): String? {
        val databaseDao = db.databaseDao()
        val seen = HashSet<String>()
        var frontier = listOf(pageId)
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<String>()
            for (id in frontier) {
                if (!seen.add(id)) continue
                val page = pageDao.getById(id)
                if (page == null) {
                    // **Una sezione di un quaderno**, protetta con la password
                    // come in OneNote: chiude le sue pagine.
                    if (db.notebookDao().getSection(id)?.isProtected == true) return id
                    continue
                }
                if (page.isProtected) return id
                page.notebookSectionId?.let { next += it }
                blockDao.getBlocksLinkingTo(id).forEach { next += it.pageId }
                databaseDao.getRowsLinkingTo(id).forEach { next += it.pageId }
                page.sourceDatabaseId?.let { next += it }
            }
            frontier = next
        }
        return null
    }

    /** Se adesso questa pagina non si può leggere: bloccata (lei o chi la contiene) e non aperta. */
    suspend fun isLockedNow(pageId: String): Boolean =
        lockingPageOf(pageId)?.let { !AppLock.isUnlocked(it) } ?: false

    /**
     * **La riga diventa una notifica** (dal 25/09/2026): tipo e ora
     * insieme. Il testo solo se chi chiama ne porta uno — la riga
     * "/notification 22.10.2026 17:41 Buttare l'immondizia" lascia il solo
     * "Buttare l'immondizia" — e con `newBlockId` sotto nasce una riga
     * vuota, come dopo un Invio. Letta dal database dentro la transazione,
     * come il divisore: la copia della schermata può essere di un attimo fa.
     */
    suspend fun turnIntoNotification(blockId: String, at: Long, textJson: String?, newBlockId: String?) =
        db.withTransaction {
            val current = blockDao.getById(blockId) ?: return@withTransaction
            blockDao.setNotification(current.id, at)
            if (textJson != null) blockDao.setText(current.id, textJson)
            if (newBlockId != null) {
                blockDao.shiftOrderIndexes(current.pageId, current.parentBlockId, current.orderIndex + 1, 1)
                blockDao.insert(
                    BlockEntity(
                        id = newBlockId,
                        pageId = current.pageId,
                        parentBlockId = current.parentBlockId,
                        orderIndex = current.orderIndex + 1,
                        textJson = "[]"
                    )
                )
            }
        }

    /** L'ora di una riga "/notification", cambiata dalla sua data. */
    suspend fun setNotifyAt(blockId: String, at: Long) = blockDao.setNotifyAt(blockId, at)

    /** Cambia solo il rientro di un blocco, senza riscriverne il testo. */
    suspend fun setIndentLevel(blockId: String, level: Int) =
        blockDao.setIndentLevel(blockId, level)

    /**
     * I collegamenti di una foto di Annulla che **non devono tornare**:
     * quelli a una pagina che non c'è più, che è nel cestino, o che nel
     * frattempo è stata spostata in un'altra pagina ("Move to").
     *
     * Le foto sono i blocchi di tutta la pagina, scattate prima; quello
     * che è successo dopo alle pagine collegate non lo sanno. Rimettere il
     * collegamento a una pagina spostata la faceva stare in due posti;
     * rimetterlo a una pagina **cancellata** — un database eliminato dalle
     * sue impostazioni, poi Annulla — era peggio: il blocco puntava a una
     * pagina inesistente, il database lo rifiutava (chiave esterna) e
     * l'app si chiudeva. Trovato il 24/09/2026 leggendo il codice.
     */
    private suspend fun withoutStaleLinks(pageId: String, blocks: List<BlockEntity>): List<BlockEntity> {
        val stale = mutableSetOf<String>()
        val checked = blocks.map { block ->
            val linkedId = block.linkedPageId ?: return@map block
            val linked = pageDao.getById(linkedId)
            val isLink = block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK
            if (!isLink) {
                // Un blocco che è stato un collegamento e poi è tornato
                // testo può tenersi l'id della pagina: se quella non c'è
                // più, l'id va tolto per la stessa chiave esterna.
                return@map if (linked == null) block.copy(linkedPageId = null) else block
            }
            val movedAway = blockDao.getBlocksLinkingTo(linkedId).any { it.pageId != pageId }
            if (linked == null || linked.trashedAt != null || movedAway) stale += block.id
            block
        }
        if (stale.isEmpty()) return checked
        // Un collegamento non ha figli, ma per sicurezza non si lascia
        // nessun blocco appeso a uno tolto: la chiave esterna del
        // genitore lo rifiuterebbe allo stesso modo.
        var result = checked.filterNot { it.id in stale }
        while (true) {
            val ids = result.map { it.id }.toSet()
            val orphaned = result.filter { it.parentBlockId != null && it.parentBlockId !in ids }
            if (orphaned.isEmpty()) return result
            result = result - orphaned.toSet()
        }
    }

    /**
     * Rimette la pagina com'era in una foto dei suoi blocchi: è quello che
     * fanno Annulla e Ripristina.
     *
     * **Le celle delle tabelle si salvano da parte e si rimettono.** I
     * blocchi si cancellano tutti e si riscrivono, e le celle sono legate
     * al loro blocco con `CASCADE`: cancellando la tabella il database
     * portava via anche il suo testo. Un Annulla qualsiasi — anche di una
     * lettera scritta tre righe più su — svuotava ogni tabella della
     * pagina. Trovato il 24/09/2026 leggendo il codice. Le celle tornano
     * solo per le tabelle che ci sono ancora nella foto; il loro testo
     * resta quello di adesso, perché Annulla non lo segue (vedi
     * `PageEditorViewModel`, "Annulla / Ripristina").
     *
     * **Si reinseriscono i genitori prima dei figli** (`inDocumentOrder`).
     * La foto arriva ordinata solo per `orderIndex`, che per i figli di un
     * toggle riparte da 0: "Riga dentro il toggle" (0) veniva prima del suo
     * toggle (9), la chiave esterna verso il genitore non ancora rimesso la
     * rifiutava e l'app si chiudeva — con qualunque Annulla, in qualunque
     * pagina con un toggle non vuoto che non fosse il primo blocco. C'era
     * già prima delle sessioni cloud; trovato sul telefono il 24/09/2026.
     */
    suspend fun replaceAllBlocks(pageId: String, snapshot: List<BlockEntity>) {
        db.withTransaction {
            val blocks = withoutStaleLinks(pageId, snapshot)
            val keptIds = blocks.map { it.id }.toSet()
            val cells = tableCellDao.getCellsForPageOnce(pageId).filter { it.blockId in keptIds }
            blockDao.deleteAllForPage(pageId)
            blockDao.insertAll(inDocumentOrder(blocks))
            cells.forEach { tableCellDao.insertCell(it) }
        }
    }

    // --- "Page history": le versioni delle pagine (dal 02/11/2026) ---
    //
    // Chiesto dall'utente: "Updates ti mostra cosa è cambiato, ma non riporta
    // una pagina a com'era ieri". Una versione è la pagina intera in un
    // momento (vedi `PageVersionEntity`); le salva l'editor aprendo la pagina,
    // ogni dieci minuti mentre è aperta e uscendo, solo se è cambiata.

    private val versionDao = db.pageVersionDao()

    fun observeVersions(pageId: String): Flow<List<PageVersionInfo>> = versionDao.observeForPage(pageId)

    /** Il contenuto di una versione, per vederla prima di rimetterla. */
    suspend fun versionContent(versionId: String): VersionContent? {
        val bytes = versionDao.contentOf(versionId) ?: return null
        return withContext(Dispatchers.Default) { runCatching { VersionCodec.decode(bytes) }.getOrNull() }
    }

    /** I titoli di adesso delle pagine collegate in una versione: la versione ha solo i loro id. */
    suspend fun titlesOf(pageIds: Collection<String>): Map<String, String> =
        pageIds.distinct().mapNotNull { id -> pageDao.getById(id)?.let { id to it.title } }.toMap()

    /** Un blocco, dovunque stia. */
    suspend fun blockOf(blockId: String): BlockEntity? = blockDao.getById(blockId)

    /** Le viste in più di un database (vedi `PageEntity.viewHostId`). */
    suspend fun viewsOf(hostId: String): List<PageEntity> = pageDao.getViewsOnce(hostId)

    /** I tratti disegnati a mano su una pagina. */
    suspend fun strokesOf(pageId: String): List<InkStrokeEntity> = db.inkDao().getForPageOnce(pageId)

    /** Le proprietà di un database. */
    suspend fun columnsOf(databaseId: String): List<DatabaseColumnEntity> = db.databaseDao().getColumnsForPageOnce(databaseId)

    /** Una pagina che ne richiama un'altra, e la riga in cui lo fa (null per un "Link to page"). */
    data class Backlink(val page: PageEntity, val snippet: String?)

    /**
     * **I backlink di una pagina** (dal 03/11/2026, chiesto dall'utente: "In
     * fondo a ogni pagina vedresti quali altre pagine la richiamano"): le
     * pagine che la menzionano con "@" nel testo o con un "Link to page",
     * una volta sola ciascuna, le toccate per ultime prima. Non la pagina
     * stessa, né quelle nel cestino. Il collegamento di una sottopagina nella
     * pagina che la contiene non conta: è dove sta, non un richiamo.
     */
    suspend fun backlinksOf(pageId: String): List<Backlink> {
        val blocks = blockDao.getBlocksMentioning("%${pageLink(pageId)}%", "%\"$pageId\"%")
        val snippets = LinkedHashMap<String, String?>()
        for (block in blocks) {
            if (block.pageId == pageId) continue
            if (block.type == BlockType.PAGE_MENTION) {
                if (block.attrs.pageId != pageId) continue
                if (block.pageId !in snippets) snippets[block.pageId] = null
            } else {
                val spans = runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrNull() ?: continue
                if (pageId !in mentionedPageIds(spans)) continue
                if (snippets[block.pageId] == null) {
                    snippets[block.pageId] = spans.plainText().replace('\n', ' ').trim().take(BACKLINK_SNIPPET_MAX)
                }
            }
        }
        return snippets.mapNotNull { (id, snippet) ->
            pageDao.getById(id)?.takeIf { it.trashedAt == null }?.let { Backlink(it, snippet) }
        }.sortedByDescending { it.page.updatedAt }
    }

    /**
     * **Le pagine da menzionare con "@"** (dal 03/11/2026): quelle col titolo
     * che somiglia a [query] — prima chi comincia così, poi chi ha una parola
     * che comincia così, poi le altre — o, senza niente scritto, le toccate
     * per ultime. Non [excludeId] (la pagina in cui si scrive), non il
     * cestino, non i template né le viste in più di un database.
     */
    suspend fun mentionCandidates(query: String, excludeId: String?, limit: Int = MENTION_CANDIDATES): List<PageEntity> {
        val needle = query.trim()
        // Riordinate anche qui: nelle prove (Robolectric) il database non le dava in ordine.
        val pages = if (needle.isEmpty()) pageDao.recentlyUpdated(limit * 3).sortedByDescending { it.updatedAt } else pageDao.searchByTitle(needle)
        val usable = pages.filter {
            it.trashedAt == null && it.id != excludeId && !it.isTemplate && it.viewHostId == null && it.sourceDatabaseId == null
        }
        if (needle.isEmpty()) return usable.take(limit)
        fun rank(title: String): Int = when {
            title.startsWith(needle, ignoreCase = true) -> 0
            title.split(' ', '-', '–').any { it.startsWith(needle, ignoreCase = true) } -> 1
            else -> 2
        }
        return usable.sortedWith(compareBy<PageEntity>({ rank(it.title) }, { -it.updatedAt })).take(limit)
    }

    /**
     * **I titoli delle menzioni rimessi in pari** nella pagina [pageId]: una
     * pagina menzionata e poi rinominata si vede col nome nuovo (dal
     * 03/11/2026). Quante righe sono cambiate.
     */
    suspend fun refreshMentionTitles(pageId: String, untitled: String): Int {
        val withMentions = blockDao.getBlocksForPageOnce(pageId).mapNotNull { block ->
            if (!block.textJson.contains(PAGE_LINK_PREFIX)) return@mapNotNull null
            val spans = runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrNull() ?: return@mapNotNull null
            block to spans
        }
        if (withMentions.isEmpty()) return 0
        val titles = titlesOf(withMentions.flatMap { mentionedPageIds(it.second) })
        var changed = 0
        for ((block, spans) in withMentions) {
            val fresh = refreshPageMentions(spans, titles, untitled) ?: continue
            blockDao.setText(block.id, json.encodeToString(fresh))
            changed++
        }
        return changed
    }

    /**
     * **La pagina da esportare** (vedi `data/export`): le righe, le celle, i
     * titoli delle pagine a cui portano e, con [withSubpages], le sottopagine —
     * solo le pagine figlie, non i link a pagine che stanno altrove. Null se
     * la pagina non c'è.
     */
    suspend fun exportPage(pageId: String, withSubpages: Boolean): ExportPage? =
        exportPage(pageId, withSubpages, HashSet(), 0)

    private suspend fun exportPage(pageId: String, withSubpages: Boolean, seen: MutableSet<String>, depth: Int): ExportPage? {
        if (!seen.add(pageId)) return null
        val page = pageDao.getById(pageId) ?: return null
        val blocks = blockDao.getBlocksForPageOnce(pageId)
        val linked = blocks.mapNotNull { it.linkedPageId ?: it.attrs.pageId }
        val exported = ExportPage(page, blocks, tableCellDao.getCellsForPageOnce(pageId), titlesOf(linked))
        if (!withSubpages || depth >= EXPORT_MAX_DEPTH) return exported
        val subpages = subpageIds(exported).mapNotNull { id ->
            pageDao.getById(id)?.takeIf { it.parentId == pageId && !it.isDatabase }?.let { exportPage(id, true, seen, depth + 1) }
        }
        return exported.copy(subpages = subpages)
    }

    /**
     * **Salva com'è adesso la pagina**, se è diversa dall'ultima versione;
     * poi toglie quelle che non si tengono più (`VersionRetention`). Vero se
     * ne è nata una.
     *
     * Non per i database (le loro righe non sono blocchi) né per una pagina
     * senza niente dentro. Una pagina enorme, oltre [MAX_VERSION_BYTES]
     * compressi, non si salva: il database non la rileggerebbe in un pezzo.
     */
    suspend fun saveVersion(
        pageId: String,
        reason: VersionReason,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = runCatching { AppSettings.zoneId }.getOrDefault(ZoneId.systemDefault())
    ): Boolean = versionLock.withLock {
        val content = db.withTransaction {
            val page = pageDao.getById(pageId)
            if (page == null || page.isDatabase) return@withTransaction null
            val blocks = blockDao.getBlocksForPageOnce(pageId)
            val ink = db.inkDao().getForPageOnce(pageId)
            if (blocks.isEmpty() && ink.isEmpty()) return@withTransaction null
            VersionContent.of(page.title, blocks, tableCellDao.getCellsForPageOnce(pageId), ink)
        } ?: return@withLock false
        val (hash, bytes) = withContext(Dispatchers.Default) { VersionCodec.hash(content) to VersionCodec.encode(content) }
        if (versionDao.latestHash(pageId) == hash || bytes.size > MAX_VERSION_BYTES) return@withLock false
        versionDao.insert(
            PageVersionEntity(
                pageId = pageId, at = now, reason = reason, title = content.title,
                lines = content.lines, words = content.words, hash = hash, content = bytes
            )
        )
        val drop = VersionRetention.toDrop(versionDao.stampsFor(pageId), now, zone)
        if (drop.isNotEmpty()) versionDao.deleteByIds(drop)
        true
    }

    /**
     * La stessa cosa, ma fuori dalla schermata che la chiede: uscendo da una
     * pagina il suo ViewModel se ne va, e il salvataggio deve finire lo
     * stesso. Aspetta [delayMillis] perché le ultime scritture arrivino.
     * Una versione che non si riesce a salvare non chiude l'app: è una rete
     * in più, non deve diventare un problema in più.
     */
    fun saveVersionLater(pageId: String, reason: VersionReason, delayMillis: Long = 0) {
        historyScope.launch {
            delay(delayMillis)
            runCatching { saveVersion(pageId, reason) }
        }
    }

    /**
     * **Rimette la pagina com'era nella versione** [versionId]. Prima salva
     * com'è adesso (`VersionReason.BEFORE_RESTORE`), così il ripristino si
     * può disfare rimettendo quella.
     *
     * Tornano il titolo, i blocchi con i loro id (i collegamenti a un blocco
     * restano buoni), il testo delle tabelle e i disegni. Come per Annulla
     * (`withoutStaleLinks`) non tornano i collegamenti a pagine cancellate,
     * nel cestino o spostate altrove. **Le sottopagine e i database nati
     * dopo la versione non si perdono**: il loro collegamento resta, in
     * fondo alla pagina. Falso se la versione non c'è o non si legge.
     */
    suspend fun restoreVersion(pageId: String, versionId: String): Boolean {
        val content = versionContent(versionId) ?: return false
        saveVersion(pageId, VersionReason.BEFORE_RESTORE)
        val restored = versionLock.withLock {
            db.withTransaction {
                if (pageDao.getById(pageId) == null) return@withTransaction false
                val blocks = withoutStaleLinks(pageId, content.blocks.map { it.toEntity(pageId) })
                val restoredLinks = blocks.mapNotNullTo(HashSet()) { it.linkedPageId }
                val lastTop = blocks.filter { it.parentBlockId == null }.maxOfOrNull { it.orderIndex } ?: -1
                val newerLinks = blockDao.getBlocksForPageOnce(pageId)
                    .filter { (it.type == BlockType.PAGE_LINK || it.type == BlockType.DATABASE_LINK) && it.linkedPageId != null }
                    .filter { it.linkedPageId !in restoredLinks && blocks.none { b -> b.id == it.id } }
                    .sortedBy { it.orderIndex }
                    .mapIndexed { i, link -> link.copy(parentBlockId = null, orderIndex = lastTop + 1 + i, indentLevel = 0) }
                val all = blocks + newerLinks
                val ids = all.mapTo(HashSet()) { it.id }
                blockDao.deleteAllForPage(pageId)
                blockDao.insertAll(inDocumentOrder(all))
                content.cells.filter { it.blockId in ids }.forEach { tableCellDao.insertCell(it.toEntity()) }
                db.inkDao().deleteForPage(pageId)
                if (content.ink.isNotEmpty()) db.inkDao().insertAll(content.ink.map { it.toEntity(pageId) })
                pageDao.setTitle(pageId, content.title, System.currentTimeMillis())
                true
            }
        }
        // I promemoria dei blocchi rimessi tornano, quelli dei blocchi tolti se ne vanno.
        if (restored) ReminderAlarms.reschedule()
        return restored
    }

    // --- "Condividi con notE" e le note al volo (dal 02/11/2026) ---
    //
    // Chiesto dall'utente: "da Chrome, dalla Galleria o da WhatsApp non puoi
    // mandare niente a notE", e "un widget [...] o un tasto nelle impostazioni
    // rapide, per scrivere una nota in due secondi". Vedi `ShareActivity` e
    // `QuickNotes`.

    /** Una cosa arrivata da un'altra app, pronta per diventare un blocco. */
    data class IncomingBlock(val type: BlockType, val text: String = "", val attrs: BlockAttrs? = null)

    /**
     * **La pagina in cui finisce quello che arriva da fuori**: [chosen], se c'è
     * ancora (non nel cestino, non un database); altrimenti una pagina nuova
     * "Inbox" (📥, col nome [inboxTitle]) in fondo al menu principale. Chi
     * chiama ricorda l'id che torna.
     */
    suspend fun inboxPageId(chosen: String?, inboxTitle: String): String = db.withTransaction {
        val page = chosen?.let { pageDao.getById(it) }
        if (page != null && page.trashedAt == null && !page.isDatabase) return@withTransaction page.id
        val inbox = PageEntity(title = inboxTitle, icon = "📥")
        pageDao.insert(inbox)
        appendLink(PageEntity.ROOT_PAGE_ID, inbox.id, false)
        inbox.id
    }

    /**
     * **Salva quello che è arrivato**: in una pagina nuova col titolo dato, il
     * cui collegamento va in fondo a [destinationId] ([asNewPage]), o in fondo
     * alla pagina [destinationId] stessa. Restituisce la pagina in cui sono
     * finiti i blocchi.
     */
    suspend fun saveIncoming(destinationId: String, title: String, blocks: List<IncomingBlock>, asNewPage: Boolean): String =
        db.withTransaction {
            val target = if (asNewPage) {
                val page = PageEntity(title = title.trim())
                pageDao.insert(page)
                appendLink(destinationId, page.id, false)
                page.id
            } else {
                destinationId
            }
            var order = (blockDao.lastOrderIndex(target) ?: -1) + 1
            blockDao.insertAll(
                blocks.map { block ->
                    BlockEntity(
                        pageId = target,
                        type = block.type,
                        textJson = json.encodeToString(listOf(RichTextSpan(text = block.text))),
                        attrsJson = block.attrs?.let { BlockAttrs.encode(it) } ?: "{}",
                        orderIndex = order++
                    )
                }
            )
            target
        }

    /**
     * **Una nota al volo**: una pagina nuova senza titolo, col collegamento in
     * fondo a [destinationId] e una riga vuota dove scrivere. Restituisce la
     * pagina e la riga.
     */
    suspend fun createQuickNote(destinationId: String): Pair<String, String> = db.withTransaction {
        val page = PageEntity(title = "")
        pageDao.insert(page)
        appendLink(destinationId, page.id, false)
        val line = BlockEntity(pageId = page.id, orderIndex = 0)
        blockDao.insert(line)
        page.id to line.id
    }

    /**
     * **Lasciando una nota al volo**: se non c'è scritto niente se ne va
     * (aperta per sbaglio dal widget non deve lasciare una pagina vuota);
     * senza titolo prende la sua prima riga, come un foglietto. Restituisce i
     * file da cancellare, come `deletePermanently`.
     */
    suspend fun finishQuickNote(pageId: String): List<String> {
        val page = pageDao.getById(pageId) ?: return emptyList()
        if (page.title.isNotBlank()) return emptyList()
        val blocks = blockDao.getBlocksForPageOnce(pageId)
        val firstLine = blocks.asSequence()
            .filter { it.parentBlockId == null }
            .sortedBy { it.orderIndex }
            .map { block ->
                (runCatching { json.decodeFromString<List<RichTextSpan>>(block.textJson) }.getOrNull().orEmpty()).plainText()
                    .lineSequence().firstOrNull { it.isNotBlank() }?.trim()
            }
            .firstOrNull { !it.isNullOrEmpty() }
        val somethingElse = blocks.any { it.type != BlockType.PARAGRAPH } || db.inkDao().getForPageOnce(pageId).isNotEmpty()
        return when {
            firstLine != null -> {
                pageDao.setTitle(pageId, firstLine.take(QUICK_NOTE_TITLE_MAX), System.currentTimeMillis())
                emptyList()
            }
            somethingElse -> emptyList()
            else -> deletePermanently(pageId)
        }
    }

    // --- Celle delle tabelle semplici (blocchi TABLE) ---

    fun getTableCells(blockId: String): Flow<List<TableCellEntity>> =
        tableCellDao.getCellsForBlock(blockId)

    suspend fun setTableCellText(blockId: String, row: Int, col: Int, text: String) {
        val existing = tableCellDao.getCell(blockId, row, col)
        if (existing != null) {
            tableCellDao.insertCell(existing.copy(text = text))
        } else {
            tableCellDao.insertCell(
                TableCellEntity(blockId = blockId, rowIndex = row, colIndex = col, text = text)
            )
        }
    }

    // --- Il correttore (dal 08/10/2026) ---

    private val personalWordDao = db.personalWordDao()

    /** Il dizionario dell'utente, in ordine alfabetico. */
    fun observePersonalWords(): Flow<List<String>> = personalWordDao.observeAll().map { list -> list.map { it.word } }

    suspend fun addPersonalWord(word: String) {
        val w = word.trim()
        if (w.isNotEmpty()) personalWordDao.insert(PersonalWordEntity(w))
    }

    suspend fun removePersonalWord(word: String) = personalWordDao.delete(word)

    /** Il correttore di una pagina: vedi `PageEntity.proofingMode`. */
    suspend fun setPageProofing(pageId: String, mode: Int, language: String?) = pageDao.setProofing(pageId, mode, language)

    /** Le righe lunghe di una pagina: vedi `PageEntity.fullLineMode`. */
    suspend fun setPageFullLine(pageId: String, mode: Int) = pageDao.setFullLineMode(pageId, mode)

    suspend fun setPageIgnoredWords(pageId: String, words: Set<String>) =
        pageDao.setProofingIgnored(pageId, words.sorted().joinToString("\n").ifEmpty { null })

    /** Il "Paragraph" di una pagina: vedi `PageParagraph`. */
    suspend fun setPageParagraph(pageId: String, paragraph: PageParagraph) =
        pageDao.setParagraph(pageId, PageParagraph.encode(paragraph))

    /**
     * Le voci di più blocchi insieme, **in una transazione sola**: chi
     * guarda la tabella viene avvisato una volta, non una per riga (vedi
     * "Strade già tentate" nel README). Tocca solo `attrsJson`: il testo,
     * che intanto si può star scrivendo, resta quello del database.
     */
    suspend fun updateBlocksAttrs(changes: Map<String, (BlockAttrs) -> BlockAttrs>) = db.withTransaction {
        for ((blockId, change) in changes) {
            val block = blockDao.getById(blockId) ?: continue
            val updated = change(block.attrs)
            if (updated != block.attrs) blockDao.setAttrs(blockId, BlockAttrs.encode(updated))
        }
    }
}
