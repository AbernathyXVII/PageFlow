package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.isPc
import com.gabriele.notionlocal.ui.theme.pcOr
import com.gabriele.notionlocal.data.replacedImage
import com.gabriele.notionlocal.data.hasChosenIcon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.NorthEast
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatLineSpacing
import com.gabriele.notionlocal.ui.i18n.ParagraphStrings
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.rememberCoroutineScope
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.MULTILINE_TYPES
import com.gabriele.notionlocal.data.entity.MEDIA_TYPES
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.FavoriteStar
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.LinkOff
import com.gabriele.notionlocal.ui.i18n.SyncStrings
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.data.applicationContextOf

/**
 * **Il menu di un blocco**, come quello che su Notion si apre dai sei
 * puntini accanto a ogni riga. Ci si arriva in tre modi:
 *
 * - dall'icona coi sei puntini nella barra sopra la tastiera, per la riga
 *   in cui sta il cursore;
 * - tenendo premuto il collegamento a una pagina (o a un database) nel
 *   testo, dove il cursore non può entrare;
 * - dai sei puntini nella riga degli strumenti di un database dentro la
 *   pagina, per lo stesso motivo.
 *
 * Le voci dipendono da cosa è il blocco, come chiesto dall'utente il
 * 24/09/2026:
 *
 * - **una riga di testo** (paragrafo, elenchi, casella, toggle): Turn
 *   into, Color, Duplicate. Icona, preferiti, spostare e cestino
 *   riguardano le pagine — "per i semplici campi di testo non mi
 *   servono" — e qui non ci sono; nemmeno Rename, perché una riga di
 *   testo non ha un nome da cambiare: il suo nome è il testo stesso;
 * - **una pagina**: Turn into, Color, Edit icon, Add to favorites,
 *   Rename, Duplicate, Move to, Move to trash;
 * - **un database**: in alto Turn into page, Turn into simple database,
 *   Turn into complex database, Lock database, Open as page; sotto le
 *   voci della pagina.
 *
 * - **un divisore** (dal 05/10/2026, tenendolo premuto): solo "Add above",
 *   "Add below" e "Delete" — le altre voci non hanno senso per una riga
 *   che non ha testo, colore né nome. "Delete" lo toglie subito; Annulla
 *   lo rimette.
 *
 * In cima a tutti, "Add above" e "Add below" (dal 29/09/2026, chiesto
 * dall'utente): una riga vuota sopra o sotto il blocco, col cursore dentro.
 * Sopra ancora, "Move up" e "Move down" (dal 14/10/2026, chiesto
 * dall'utente) per i blocchi in cui il cursore non entra, e che quindi non
 * hanno le frecce della barra sopra la tastiera: pagine e database, poi
 * (lo stesso giorno) immagini, video, audio, file, segnalibri, "Link to
 * page" e divisori.
 *
 * Una voce che per quel blocco non è ancora costruita si vede **grigia**
 * (Turn into per pagine e database, Color per i database), come nei menu
 * "/" e "+": dice che esiste e che arriverà.
 *
 * Il menu è uno solo e le finestre che apre (colori, icona, nome, dove
 * duplicare, dove spostare, conferme) prendono il suo posto: stanno tutte
 * qui, e chi lo apre deve solo dire su quale blocco.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockActionsHost(
    blockId: String,
    blocks: List<BlockEntity>,
    viewModel: PageEditorViewModel,
    factory: ViewModelFactory,
    imageStore: PageImageStore,
    /** Cosa fa una voce di "Turn into": decide la schermata, come per i menu "/" e "+". */
    onTurnInto: (BlockEntity, SlashEntry) -> Unit,
    onNavigateToDatabase: (String) -> Unit,
    /** "Open" di un "Link to page" (dal 06/10/2026). */
    onNavigateToPage: (String) -> Unit = {},
    /** "Paragraph" di questa riga (dal 21/10/2026): lo apre la schermata. Null = la voce non c'è. */
    onParagraph: ((BlockEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val block = blocks.find { it.id == blockId }
    if (block == null) {
        // Il blocco non c'è più (cancellato mentre il menu si apriva, o da
        // un Annulla): niente da mostrare.
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val context = LocalPlatformContext.current

    val linkedId = block.linkedPageId?.takeIf {
        block.type == BlockType.PAGE_LINK || block.type == BlockType.DATABASE_LINK
    }
    // La pagina collegata **seguita nel tempo**: preferiti, lucchetto e
    // nome cambiano sotto gli occhi appena li si tocca. `Loaded` distingue
    // "non ancora letta" (null) da "non esiste più" (pagina null).
    val loaded by remember(linkedId) {
        if (linkedId == null) {
            flowOf(Loaded(null))
        } else {
            viewModel.observePageInfo(linkedId).map { Loaded(it) }
        }
    }.collectAsStateWithLifecycle(initialValue = null)
    val state = loaded ?: return
    val page = state.page
    if (linkedId != null && page == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    // **Vista collegata**: le voci che riguardano il database — semplice o
    // complesso, blocco, nome, icona — valgono per il database di origine,
    // che è quello che la vista mostra; duplicare, spostare, buttare e i
    // preferiti riguardano la vista, cioè questo blocco.
    val sourceId = page?.sourceDatabaseId
    val sourcePage by remember(sourceId) {
        if (sourceId == null) flowOf(null) else viewModel.observePageInfo(sourceId)
    }.collectAsStateWithLifecycle(initialValue = null)
    val dataPage = if (sourceId != null) sourcePage else page

    var step by remember { mutableStateOf(BlockMenuStep.MENU) }
    val menuScope = rememberCoroutineScope()
    var moveExclusions by remember { mutableStateOf<PageRepository.MoveExclusions?>(null) }
    var rowPagesToLose by remember { mutableIntStateOf(0) }

    when (step) {
        BlockMenuStep.MENU, BlockMenuStep.TURN_INTO -> NoteBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSheet
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                // Il suo posto fra i fratelli, per spegnere "Move up" in
                // cima e "Move down" in fondo: lo stesso conto delle frecce.
                val siblings = blocks.filter { it.parentBlockId == block.parentBlockId }
                val position = siblings.indexOfFirst { it.id == block.id }
                val onMoveUp: (() -> Unit)? = if (position > 0) {
                    {
                        onDismiss()
                        viewModel.moveBlockUp(block)
                    }
                } else {
                    null
                }
                val onMoveDown: (() -> Unit)? = if (position >= 0 && position < siblings.lastIndex) {
                    {
                        onDismiss()
                        viewModel.moveBlockDown(block)
                    }
                } else {
                    null
                }
                val onMoveToLine = { step = BlockMenuStep.MOVE_TO_LINE }
                if (step == BlockMenuStep.TURN_INTO) {
                    TurnIntoList(
                        block = block,
                        onBack = { step = BlockMenuStep.MENU },
                        onPick = { entry ->
                            onDismiss()
                            // Il tipo che la riga ha già: si chiude e basta,
                            // senza una modifica vuota nella cronologia.
                            if ((entry.action as? SlashAction.Type)?.type != block.type) {
                                onTurnInto(block, entry)
                            }
                        }
                    )
                } else if (block.type == BlockType.DIVIDER) {
                    DividerBlockMenu(
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        onDelete = {
                            onDismiss()
                            viewModel.deleteBlock(block)
                        },
                        onMoveToLine = onMoveToLine
                    )
                } else if (block.type in MEDIA_TYPES) {
                    // **Immagini, video, audio, file e segnalibri** (dal
                    // 06/10/2026): le cose da fare le fa il blocco (vedi
                    // `PageEditorViewModel.mediaRequest`), perché questo menu
                    // si chiude prima che la galleria o il "Salva in…" rispondano.
                    val media = block.attrs
                    fun ask(kind: MediaRequestKind) {
                        onDismiss()
                        viewModel.requestMedia(block.id, kind)
                    }
                    ObjectBlockMenu(
                        title = media.name ?: media.url?.let { linkHost(it) } ?: EditorStrings.blockType(mediaTypeLabel(block.type)),
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        actions = when {
                            block.type == BlockType.IMAGE && media.file != null -> listOf(
                                // Le maniglie si vedono solo da qui (dal 20/10/2026, chiesto dall'utente).
                                ObjectAction(Icons.Filled.PhotoSizeSelectLarge, BlockStrings.resizeImage) { ask(MediaRequestKind.RESIZE) },
                                ObjectAction(Icons.Filled.CropFree, BlockStrings.fullResize) { ask(MediaRequestKind.FULL_RESIZE) },
                                ObjectAction(Icons.Filled.OpenInFull, BlockStrings.view) { ask(MediaRequestKind.VIEW) },
                                ObjectAction(Icons.Filled.Download, BlockStrings.download) { ask(MediaRequestKind.DOWNLOAD) },
                                ObjectAction(Icons.Filled.Autorenew, BlockStrings.replace) { ask(MediaRequestKind.PICK) },
                                ObjectAction(Icons.Filled.DriveFileMove, Strings.moveTo) { step = BlockMenuStep.MOVE_BLOCK }
                            )
                            media.file != null -> listOf(
                                ObjectAction(Icons.Filled.OpenInNew, BlockStrings.openWith) { ask(MediaRequestKind.OPEN_ELSEWHERE) },
                                ObjectAction(Icons.Filled.Download, BlockStrings.download) { ask(MediaRequestKind.DOWNLOAD) },
                                ObjectAction(Icons.Filled.Autorenew, BlockStrings.replace) { ask(MediaRequestKind.PICK) }
                            )
                            media.url != null -> listOf(
                                ObjectAction(Icons.Filled.OpenInNew, BlockStrings.openInBrowser) { ask(MediaRequestKind.OPEN_ELSEWHERE) },
                                ObjectAction(Icons.Filled.Autorenew, BlockStrings.replace) { ask(MediaRequestKind.PICK) }
                            )
                            else -> listOf(ObjectAction(Icons.Filled.Upload, EditorStrings.upload) { ask(MediaRequestKind.PICK) })
                        },
                        onDuplicate = {
                            onDismiss()
                            viewModel.duplicateBlock(block.id, imageStore)
                        },
                        onDelete = {
                            onDismiss()
                            viewModel.deleteBlock(block)
                        },
                        onMoveToLine = onMoveToLine
                    )
                } else if (block.type == BlockType.SYNCED) {
                    // **Un blocco sincronizzato** (dal 03/11/2026): metterne una
                    // copia in un'altra pagina, o farne di nuovo righe normali.
                    // Niente "Move to" un'altra pagina: le righe dentro restano
                    // nella loro (si fa una copia lì e si toglie questa).
                    ObjectBlockMenu(
                        title = SyncStrings.syncedBlock,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        actions = listOf(
                            ObjectAction(Icons.Filled.Sync, SyncStrings.syncToPage) { step = BlockMenuStep.SYNC_TO_PAGE },
                            ObjectAction(Icons.Filled.LinkOff, SyncStrings.unsync) {
                                onDismiss()
                                viewModel.unsyncBlock(block)
                            }
                        ),
                        onDuplicate = {
                            onDismiss()
                            viewModel.duplicateBlock(block.id, imageStore)
                        },
                        onDelete = {
                            onDismiss()
                            viewModel.deleteBlock(block)
                        },
                        onMoveToLine = onMoveToLine
                    )
                } else if (block.type == BlockType.BUTTON) {
                    // **Un bottone** (dal 27/10/2026): cambiarne le azioni,
                    // metterne una copia collegata altrove, e per una copia
                    // tornare al bottone vero.
                    val linkId = block.attrs.buttonLink
                    val source by remember(linkId) {
                        if (linkId == null) flowOf(null) else viewModel.observeBlock(linkId)
                    }.collectAsStateWithLifecycle(initialValue = null)
                    val config = if (linkId == null) block.attrs.button else source?.attrs?.button
                    ObjectBlockMenu(
                        title = config?.name?.ifBlank { null } ?: ButtonStrings.newButton,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        actions = listOfNotNull(
                            ObjectAction(Icons.Filled.Edit, ButtonStrings.editButton) {
                                onDismiss()
                                viewModel.requestButtonEdit(block.id)
                            }.takeIf { linkId == null || source != null },
                            ObjectAction(Icons.Filled.AddLink, ButtonStrings.linkInAnotherPage) { step = BlockMenuStep.LINK_BUTTON }
                                .takeIf { linkId == null || source != null },
                            source?.let { original ->
                                ObjectAction(Icons.Filled.NorthEast, ButtonStrings.goToOriginal) {
                                    onDismiss()
                                    if (original.pageId == block.pageId) viewModel.highlightBlock(original.id) else onNavigateToPage(original.pageId)
                                }
                            },
                            ObjectAction(Icons.Filled.DriveFileMove, Strings.moveTo) { step = BlockMenuStep.MOVE_BLOCK }
                        ),
                        onDuplicate = {
                            onDismiss()
                            viewModel.duplicateBlock(block.id, imageStore)
                        },
                        onDelete = {
                            onDismiss()
                            viewModel.deleteBlock(block)
                        },
                        onMoveToLine = onMoveToLine
                    )
                } else if (block.type == BlockType.PAGE_MENTION) {
                    // **"Link to page"** (dal 06/10/2026): aprire la pagina,
                    // sceglierne un'altra, copiare o togliere il rimando — la
                    // pagina richiamata non si tocca.
                    val mentionId = block.attrs.pageId
                    val mentioned by remember(mentionId) {
                        if (mentionId == null) flowOf(null) else viewModel.observePageInfo(mentionId)
                    }.collectAsStateWithLifecycle(initialValue = null)
                    val target = mentioned
                    ObjectBlockMenu(
                        title = target?.title?.ifBlank { Strings.untitled } ?: EditorStrings.blockType("Link to page"),
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        actions = listOfNotNull(
                            target?.takeIf { it.trashedAt == null }?.let { p ->
                                ObjectAction(Icons.Filled.OpenInNew, BlockStrings.openPage) {
                                    onDismiss()
                                    if (p.isDatabase) onNavigateToDatabase(p.id) else onNavigateToPage(p.id)
                                }
                            },
                            ObjectAction(Icons.Filled.Link, BlockStrings.changePage) { step = BlockMenuStep.PICK_MENTION }
                        ),
                        onDuplicate = {
                            onDismiss()
                            viewModel.duplicateBlock(block.id, imageStore)
                        },
                        onDelete = {
                            onDismiss()
                            viewModel.deleteBlock(block)
                        },
                        onMoveToLine = onMoveToLine
                    )
                } else if (page == null) {
                    TextBlockMenu(
                        title = viewModel.plainTextOf(block).lineSequence().firstOrNull().orEmpty()
                            .ifBlank { EditorStrings.blockType(blockTypeLabel(block.type)) },
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        onTurnInto = { step = BlockMenuStep.TURN_INTO },
                        // Il codice ha i colori del suo linguaggio: niente "Color".
                        onColor = if (block.type == BlockType.CODE) null else { { step = BlockMenuStep.COLOR } },
                        onDuplicate = {
                            onDismiss()
                            viewModel.duplicateBlock(block.id, imageStore)
                        },
                        // Il riquadro ha in più l'icona e lo sfondo (dal
                        // 06/10/2026); riquadro, citazione e codice si
                        // tolgono anche da qui, come scatole a sé.
                        onEditIcon = if (block.type == BlockType.CALLOUT) {
                            { step = BlockMenuStep.CALLOUT_ICON }
                        } else {
                            null
                        },
                        onBackground = if (block.type == BlockType.CALLOUT) {
                            { step = BlockMenuStep.CALLOUT_COLOR }
                        } else {
                            null
                        },
                        onDelete = if (block.type in MULTILINE_TYPES) {
                            {
                                onDismiss()
                                viewModel.deleteBlock(block)
                            }
                        } else {
                            null
                        },
                        pinned = block.attrs.linkTarget,
                        onTogglePin = {
                            onDismiss()
                            viewModel.setLinkTarget(block, !block.attrs.linkTarget)
                        },
                        onParagraph = onParagraph?.let { open ->
                            {
                                onDismiss()
                                open(block)
                            }
                        }
                    )
                } else {
                    PageBlockMenu(
                        block = block,
                        page = page,
                        data = dataPage ?: page,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onAddLine = { below ->
                            onDismiss()
                            viewModel.addLineNextTo(block, below)
                        },
                        onTurnIntoPage = {
                            onDismiss()
                            // Come dalle impostazioni del database: il
                            // blocco diventa un collegamento, e si resta
                            // nella pagina.
                            viewModel.turnDatabaseIntoPage(block)
                        },
                        onTurnIntoDatabase = {
                            onDismiss()
                            viewModel.turnLinkIntoDatabase(block)
                        },
                        onTurnIntoSimple = {
                            // Prima si guarda se c'è qualcosa da perdere: se
                            // nessuna pagina di riga ha contenuto non c'è
                            // niente da chiedere.
                            val data = dataPage ?: page
                            viewModel.countRowPagesWithContent(data.id) { count ->
                                if (count == 0) {
                                    onDismiss()
                                    viewModel.turnIntoSimpleDatabase(data.id, imageStore)
                                } else {
                                    rowPagesToLose = count
                                    step = BlockMenuStep.CONFIRM_SIMPLE
                                }
                            }
                        },
                        onTurnIntoComplex = {
                            onDismiss()
                            viewModel.turnIntoComplexDatabase((dataPage ?: page).id)
                        },
                        onToggleLock = {
                            val data = dataPage ?: page
                            viewModel.setLockedFor(data.id, !data.isLocked)
                        },
                        onOpenAsPage = {
                            onDismiss()
                            onNavigateToDatabase(page.id)
                        },
                        onColor = { step = BlockMenuStep.COLOR },
                        onEditIcon = { step = BlockMenuStep.EDIT_ICON },
                        onToggleFavorite = {
                            onDismiss()
                            viewModel.setFavoriteFor(page.id, !page.isFavorite)
                        },
                        onRename = { step = BlockMenuStep.RENAME },
                        onDuplicate = { step = BlockMenuStep.DUPLICATE },
                        onMoveTo = {
                            viewModel.loadMoveExclusionsFor(page.id) {
                                moveExclusions = it
                                step = BlockMenuStep.MOVE_TO
                            }
                        },
                        onMoveToDatabase = {
                            viewModel.loadMoveExclusionsFor(page.id) {
                                moveExclusions = it
                                step = BlockMenuStep.MOVE_TO_DATABASE
                            }
                        },
                        onMoveToTrash = { step = BlockMenuStep.TRASH },
                        onMoveToLine = onMoveToLine
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
        }

        BlockMenuStep.MOVE_TO_LINE -> {
            val lines = remember(block.id) { viewModel.moveTargets(block) }
            MoveToLineSheet(
                title = EditorStrings.moveToLine,
                lines = lines,
                labelOf = { line -> lineLabel(line.block, viewModel) },
                onPick = { afterId ->
                    onDismiss()
                    viewModel.moveBlockAfter(block, afterId)
                },
                onDismiss = onDismiss
            )
        }

        BlockMenuStep.COLOR -> {
            // Si parte dal colore che la riga ha già, se ne ha uno: è da lì
            // che di solito si vuole correggere.
            val firstSpan = viewModel.spansOf(block).firstOrNull()
            var onBackground by remember { mutableStateOf(false) }
            ColorPickerSheet(
                initialHex = if (onBackground) firstSpan?.background else firstSpan?.color,
                background = onBackground,
                onDismiss = onDismiss,
                onSwitchTarget = { onBackground = it },
                onPick = { hex ->
                    onDismiss()
                    viewModel.colorWholeBlock(block.id, onBackground, hex)
                }
            )
        }

        BlockMenuStep.MOVE_BLOCK -> {
            // "Move to" di un'immagine (dal 06/10/2026): in fondo alla pagina scelta.
            val appContext = applicationContextOf(context)
            DestinationPickerSheet(
                factory = factory,
                title = Strings.moveTo,
                hint = BlockStrings.moveBlockHint,
                viewModelKey = "move-block",
                onDismiss = onDismiss,
                onPick = { node ->
                    onDismiss()
                    menuScope.launch {
                        val pageId = node.pageId ?: node.rowId?.let { factory.pageRepository.ensureRowPage(it) }
                        if (pageId != null) {
                            viewModel.moveBlockToPage(block, pageId) {
                                showMessage(appContext, Strings.movedInto(node.placeName()))
                            }
                        }
                    }
                }
            )
        }

        BlockMenuStep.LINK_BUTTON -> {
            // "Link in another page" (dal 27/10/2026): la copia collegata va in fondo alla pagina scelta.
            val appContext = applicationContextOf(context)
            DestinationPickerSheet(
                factory = factory,
                title = ButtonStrings.linkInAnotherPage,
                hint = ButtonStrings.linkInAnotherPageHint,
                viewModelKey = "link-button",
                onDismiss = onDismiss,
                onPick = { node ->
                    onDismiss()
                    menuScope.launch {
                        val pageId = node.pageId ?: node.rowId?.let { factory.pageRepository.ensureRowPage(it) }
                        if (pageId != null) {
                            viewModel.linkButtonIn(block, pageId) {
                                showMessage(appContext, ButtonStrings.linkedTo)
                            }
                        }
                    }
                }
            )
        }

        BlockMenuStep.SYNC_TO_PAGE -> {
            // "Sync to another page" (dal 03/11/2026): la copia va in fondo alla pagina scelta.
            val appContext = applicationContextOf(context)
            DestinationPickerSheet(
                factory = factory,
                title = SyncStrings.syncToPage,
                hint = SyncStrings.syncToPageHint,
                viewModelKey = "sync-block",
                onDismiss = onDismiss,
                onPick = { node ->
                    onDismiss()
                    menuScope.launch {
                        val pageId = node.pageId ?: node.rowId?.let { factory.pageRepository.ensureRowPage(it) }
                        if (pageId != null) {
                            viewModel.syncToPage(block, pageId) {
                                showMessage(appContext, SyncStrings.synced)
                            }
                        }
                    }
                }
            )
        }

        BlockMenuStep.PICK_MENTION -> DestinationPickerSheet(
            factory = factory,
            title = EditorStrings.blockType("Link to page"),
            hint = BlockStrings.linkToPageHint,
            viewModelKey = "link-to-page",
            onDismiss = onDismiss,
            onPick = { node ->
                onDismiss()
                menuScope.launch {
                    val pageId = node.pageId ?: node.rowId?.let { factory.pageRepository.ensureRowPage(it) }
                    if (pageId != null) viewModel.convertToPageMention(block, pageId)
                }
            }
        )

        BlockMenuStep.CALLOUT_ICON -> CalloutIconSheet(
            store = imageStore,
            hasIcon = block.attrs.hasCalloutIcon,
            onPick = { choice -> viewModel.updateBlockAttrs(block.id) { it.withIcon(choice) } },
            onDismiss = onDismiss
        )

        BlockMenuStep.CALLOUT_COLOR -> ColorPickerSheet(
            initialHex = block.attrs.color,
            background = true,
            onDismiss = onDismiss,
            onSwitchTarget = {},
            onPick = { hex ->
                onDismiss()
                // Senza colore torna il grigio di sempre.
                viewModel.updateBlockAttrs(block.id) { it.copy(color = hex) }
            },
            showTargetSwitch = false,
            previewLabel = EditorStrings.backgroundColor
        )

        BlockMenuStep.EDIT_ICON -> if (dataPage != null) {
            // L'icona che la vista mostra è quella del database di origine.
            IconPickerSheet(
                store = imageStore,
                hasIcon = dataPage.hasChosenIcon,
                onPick = { choice ->
                    viewModel.setIconFor(dataPage.id, choice)
                    replacedImage(dataPage.iconImage, choice)
                },
                onDismiss = onDismiss
            )
        }

        BlockMenuStep.RENAME -> if (dataPage != null) {
            // Anche il nome: una vista collegata mostra quello del database.
            RenamePageDialog(
                current = dataPage.title,
                onDismiss = onDismiss,
                onConfirm = { title ->
                    onDismiss()
                    viewModel.renamePage(dataPage.id, title)
                }
            )
        }

        BlockMenuStep.DUPLICATE -> if (page != null) {
            DuplicateFlow(
                page = page,
                factory = factory,
                onDismiss = onDismiss,
                onChosen = { target, placeName ->
                    onDismiss()
                    // Si resta qui: accanto all'originale la copia si vede
                    // già, sotto il collegamento toccato; messa altrove, lo
                    // dice l'avviso.
                    viewModel.duplicateLinkedPage(
                        pageId = page.id,
                        besideBlockId = block.id,
                        target = target,
                        titleSuffix = Strings.copySuffix,
                        imageStore = imageStore
                    ) {
                        placeName?.let {
                            showMessage(context, Strings.duplicatedInto(it))
                        }
                    }
                }
            )
        }

        BlockMenuStep.MOVE_TO -> {
            val exclusions = moveExclusions
            if (page != null && exclusions != null) {
                // Lo stesso albero con la ricerca dei tre puntini: vedi
                // `MovePagePicker`. Si resta qui; l'avviso dice dove è
                // finita la pagina, che da qui sparisce.
                MovePagePicker(
                    page = page,
                    exclusions = exclusions,
                    factory = factory,
                    onDismiss = onDismiss,
                    onPick = { destination, inline ->
                        onDismiss()
                        viewModel.moveLinkedPageTo(page.id, destination, inline)
                        showMessage(context, Strings.movedInto(destination.placeName()))
                    }
                )
            }
        }

        BlockMenuStep.MOVE_TO_DATABASE -> {
            val exclusions = moveExclusions
            if (page != null && exclusions != null) {
                // "Move to database" (dal 05/10/2026): come "Move to", ma la
                // pagina diventa una pagina del database scelto.
                MoveToDatabasePicker(
                    page = page,
                    exclusions = exclusions,
                    factory = factory,
                    onDismiss = onDismiss,
                    onPick = { destination, rowTitle ->
                        onDismiss()
                        viewModel.moveLinkedPageToDatabase(page.id, destination, rowTitle)
                        showMessage(context, Strings.movedInto(destination.placeName()))
                    }
                )
            }
        }

        BlockMenuStep.TRASH -> if (page != null) {
            MoveToTrashDialog(
                pageTitle = page.title,
                onConfirm = {
                    onDismiss()
                    viewModel.moveLinkedPageToTrash(page.id)
                },
                onDismiss = onDismiss
            )
        }

        BlockMenuStep.CONFIRM_SIMPLE -> if (page != null) {
            // Chiesto una volta sola e solo quando c'è davvero qualcosa da
            // perdere: l'utente ha detto che ci sta attento lui, ma un
            // numero davanti agli occhi costa un tocco e salva da uno
            // sbaglio che non si può annullare.
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(EditorStrings.simpleDatabaseLossTitle) },
                text = { Text(EditorStrings.simpleDatabaseLossText(rowPagesToLose)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDismiss()
                        viewModel.turnIntoSimpleDatabase((dataPage ?: page).id, imageStore)
                    }) {
                        Text(EditorStrings.turnIntoSimpleDatabase, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
            )
        }
    }
}

/** La pagina collegata, una volta letta: null dentro vuol dire che non esiste più. */
private class Loaded(val page: PageEntity?)

/** Le schermate del menu del blocco: il menu, la lista di "Turn into", e le finestre che si aprono dalle voci. */
private enum class BlockMenuStep {
    MENU, TURN_INTO, COLOR, EDIT_ICON, RENAME, DUPLICATE, MOVE_TO, MOVE_TO_DATABASE, TRASH, CONFIRM_SIMPLE,
    /** L'icona e lo sfondo di un riquadro (dal 06/10/2026). */
    CALLOUT_ICON, CALLOUT_COLOR,
    /** La pagina di un "Link to page" (dal 06/10/2026). */
    PICK_MENTION,
    /** Dove spostare un'immagine (dal 06/10/2026). */
    MOVE_BLOCK,
    /** "Move to line…": sotto quale riga della pagina (dal 20/10/2026). */
    MOVE_TO_LINE,
    /** "Link in another page" di un bottone: in quale pagina va la copia collegata (dal 27/10/2026). */
    LINK_BUTTON,
    /** "Sync to another page" di un blocco sincronizzato: in quale pagina va la copia (dal 03/11/2026). */
    SYNC_TO_PAGE
}

/**
 * Come si chiama una riga in "Move to line…": il suo testo (la prima riga),
 * il nome del file o del sito per immagini e segnalibri, e per il resto il
 * tipo ("Divider", "Table"…).
 */
private fun lineLabel(block: BlockEntity, viewModel: PageEditorViewModel): String {
    val text = viewModel.plainTextOf(block).lineSequence().firstOrNull()?.trim().orEmpty()
    return when {
        text.isNotEmpty() -> text.take(120)
        block.type in MEDIA_TYPES -> block.attrs.name ?: block.attrs.url?.let { linkHost(it) }
            ?: EditorStrings.blockType(mediaTypeLabel(block.type))
        block.type == BlockType.DIVIDER -> EditorStrings.blockType("Divider")
        block.type == BlockType.PARAGRAPH -> EditorStrings.untitledLine
        else -> EditorStrings.blockType(blockTypeLabel(block.type))
    }
}

/** Il nome inglese di un blocco-file, com'è nel catalogo dei menu "/" e "+". */
private fun mediaTypeLabel(type: BlockType): String = when (type) {
    BlockType.IMAGE -> "Image"
    BlockType.VIDEO -> "Video"
    BlockType.AUDIO -> "Audio"
    BlockType.FILE -> "File"
    else -> "Web bookmark"
}

/** Una voce del menu di un blocco che non è testo: aprire, cambiare, scaricare… (dal 06/10/2026). */
class ObjectAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * **Il menu di un blocco che non è testo** (dal 06/10/2026): un link a una
 * pagina, un'immagine, un video, un audio, un file, un segnalibro. Le righe
 * sopra e sotto come per tutti; poi quello che si fa con lui (`actions`);
 * in fondo copiarlo e toglierlo. Niente "Turn into" né "Color": non ha
 * testo da trasformare o colorare.
 */
@Composable
private fun ObjectBlockMenu(
    title: String,
    /** "Move up" e "Move down": null quando il blocco è già il primo o l'ultimo. */
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onAddLine: (Boolean) -> Unit,
    actions: List<ObjectAction>,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveToLine: (() -> Unit)? = null
) {
    MenuTitle(title)
    MoveGroup(onMoveUp, onMoveDown, onMoveToLine)
    AddLineGroup(onAddLine)
    if (actions.isNotEmpty()) {
        OptionsGroup {
            actions.forEachIndexed { index, action ->
                if (index > 0) HorizontalDivider()
                OptionRow(icon = action.icon, label = action.label, onClick = action.onClick)
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
    }
    OptionsGroup {
        OptionRow(icon = Icons.Filled.ContentCopy, label = Strings.duplicate, onClick = onDuplicate)
        HorizontalDivider()
        OptionRow(
            icon = Icons.Filled.Delete,
            label = Strings.delete,
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )
    }
}

/** Il nome inglese del tipo di un blocco, com'è scritto nel catalogo dei menu (`SLASH_ENTRIES`). */
private fun blockTypeLabel(type: BlockType): String =
    SLASH_ENTRIES.firstOrNull { (it.action as? SlashAction.Type)?.type == type }?.label ?: "Text"

@Composable
private fun MenuTitle(text: String) {
    // Sul PC piccolo e grigio, come "Database · Calendar" in cima al menu di Notion.
    Text(
        text = text,
        style = if (isPc) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
        fontWeight = if (isPc) FontWeight.Medium else FontWeight.Bold,
        color = if (isPc) MaterialTheme.colorScheme.onSurfaceVariant else androidx.compose.ui.graphics.Color.Unspecified,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = pcOr(6.dp, 0.dp), bottom = pcOr(6.dp, 12.dp))
    )
}

/** La freccia che dice "questa voce apre un'altra schermata", come "Turn into ›" su Notion. */
@Composable
private fun Chevron() {
    Icon(
        Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

/** Il segno di spunta della scelta attuale: il tipo della riga, o il genere del database. */
@Composable
private fun CurrentMark() {
    Icon(
        Icons.Filled.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

/**
 * **"Move up" e "Move down"** (dal 14/10/2026, chiesto dall'utente): il blocco
 * scambia il posto con quello sopra o sotto, come con le frecce della barra
 * sopra la tastiera (`PageEditorViewModel.moveBlockUp`), e la pagina lo porta
 * in vista. Null = già il primo (o l'ultimo): la voce si vede grigia.
 */
@Composable
private fun MoveGroup(
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    /** "Move to line…" (dal 20/10/2026): sotto una riga qualsiasi della pagina. Null = la voce non c'è. */
    onMoveToLine: (() -> Unit)? = null
) {
    OptionsGroup {
        OptionRow(
            icon = Icons.Filled.ArrowUpward,
            label = EditorStrings.moveUp,
            enabled = onMoveUp != null,
            onClick = { onMoveUp?.invoke() }
        )
        HorizontalDivider()
        OptionRow(
            icon = Icons.Filled.ArrowDownward,
            label = EditorStrings.moveDown,
            enabled = onMoveDown != null,
            onClick = { onMoveDown?.invoke() }
        )
        if (onMoveToLine != null) {
            HorizontalDivider()
            OptionRow(
                icon = Icons.Filled.SwapVert,
                label = EditorStrings.moveToLine,
                trailing = { Chevron() },
                onClick = onMoveToLine
            )
        }
    }
    Spacer(modifier = Modifier.size(12.dp))
}

/** "Add above" e "Add below": una riga vuota sopra (false) o sotto (true) il blocco. */
@Composable
private fun AddLineGroup(onAddLine: (Boolean) -> Unit) {
    OptionsGroup {
        OptionRow(icon = Icons.Filled.VerticalAlignTop, label = EditorStrings.addAbove, onClick = { onAddLine(false) })
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.VerticalAlignBottom, label = EditorStrings.addBelow, onClick = { onAddLine(true) })
    }
    Spacer(modifier = Modifier.size(12.dp))
}

/** Il menu di un divisore: spostarlo, le righe sopra e sotto, e toglierlo (dal 05/10/2026). */
@Composable
private fun DividerBlockMenu(
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onAddLine: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMoveToLine: (() -> Unit)? = null
) {
    // Il nome scritto a mano: nel catalogo dei menu "/" il divisore non è un
    // "tipo" di riga (`SlashAction.Type`), e `blockTypeLabel` direbbe "Text".
    MenuTitle(EditorStrings.blockType("Divider"))
    MoveGroup(onMoveUp, onMoveDown, onMoveToLine)
    AddLineGroup(onAddLine)
    OptionsGroup {
        OptionRow(
            icon = Icons.Filled.Delete,
            label = Strings.delete,
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )
    }
}

/** Il menu per una riga di testo: le righe sopra e sotto, poi tre voci, come ha chiesto l'utente. */
@Composable
private fun TextBlockMenu(
    title: String,
    onAddLine: (Boolean) -> Unit,
    onTurnInto: () -> Unit,
    /** Null per il codice, che ha i colori del suo linguaggio. */
    onColor: (() -> Unit)?,
    onDuplicate: () -> Unit,
    /** Il riquadro: la sua icona. Null per gli altri. */
    onEditIcon: (() -> Unit)? = null,
    /** Il riquadro: il suo sfondo. Null per gli altri. */
    onBackground: (() -> Unit)? = null,
    /** Riquadro, citazione e codice: toglierli. Null per le righe di testo. */
    onDelete: (() -> Unit)? = null,
    /** Se la riga è un punto di arrivo dei collegamenti (vedi `BlockLinks.kt`). */
    pinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    /** "Paragraph" (dal 21/10/2026): lo spazio fra i blocchi e dopo il marcatore di questa riga. */
    onParagraph: (() -> Unit)? = null
) {
    MenuTitle(title)
    AddLineGroup(onAddLine)
    OptionsGroup {
        OptionRow(
            icon = if (pinned) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            label = if (pinned) EditorStrings.unpinLinkTarget else EditorStrings.pinAsLinkTarget,
            onClick = onTogglePin
        )
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.Autorenew, label = EditorStrings.turnInto, trailing = { Chevron() }, onClick = onTurnInto)
        if (onColor != null) {
            HorizontalDivider()
            OptionRow(icon = Icons.Filled.Brush, label = EditorStrings.color, trailing = { Chevron() }, onClick = onColor)
        }
        if (onEditIcon != null) {
            HorizontalDivider()
            OptionRow(icon = Icons.Filled.EmojiEmotions, label = EditorStrings.editIcon, trailing = { Chevron() }, onClick = onEditIcon)
        }
        if (onBackground != null) {
            HorizontalDivider()
            OptionRow(icon = Icons.Filled.FormatColorFill, label = EditorStrings.backgroundColor, trailing = { Chevron() }, onClick = onBackground)
        }
        if (onParagraph != null) {
            HorizontalDivider()
            OptionRow(icon = Icons.Filled.FormatLineSpacing, label = ParagraphStrings.paragraph, trailing = { Chevron() }, onClick = onParagraph)
        }
    }
    Spacer(modifier = Modifier.size(12.dp))
    OptionsGroup {
        OptionRow(icon = Icons.Filled.ContentCopy, label = Strings.duplicate, onClick = onDuplicate)
        if (onDelete != null) {
            HorizontalDivider()
            OptionRow(
                icon = Icons.Filled.Delete,
                label = Strings.delete,
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }
    }
}

/**
 * Il menu per una pagina o un database. I gruppi sono quelli di Notion:
 * trasformare e colorare, poi l'icona e i preferiti, poi nome, copia e
 * posto. Per un database, sopra a tutto, le voci del database.
 */
@Composable
private fun PageBlockMenu(
    block: BlockEntity,
    page: PageEntity,
    /** Chi ha i dati: il database di origine per una vista collegata, altrimenti `page`. */
    data: PageEntity,
    /** "Move up" e "Move down": null quando il blocco è già il primo o l'ultimo. */
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onAddLine: (Boolean) -> Unit,
    onTurnIntoPage: () -> Unit,
    onTurnIntoDatabase: () -> Unit,
    onTurnIntoSimple: () -> Unit,
    onTurnIntoComplex: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenAsPage: () -> Unit,
    onColor: () -> Unit,
    onEditIcon: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onMoveToDatabase: () -> Unit,
    onMoveToTrash: () -> Unit,
    onMoveToLine: (() -> Unit)? = null
) {
    val linked = page.sourceDatabaseId != null
    // Una vista collegata si presenta come "↗ Nome del database", come nel
    // suo titolo dentro la pagina.
    MenuTitle((if (linked) "↗ " else "") + data.title.ifBlank { Strings.untitled })
    MoveGroup(onMoveUp, onMoveDown, onMoveToLine)
    AddLineGroup(onAddLine)

    if (page.isDatabase) {
        OptionsGroup {
            // Dentro la pagina il database si può mandare a schermo
            // intero; già collegato come pagina, la voce al suo posto è il
            // contrario, rimetterlo dentro. Mostrare "Turn into page" su
            // qualcosa che è già una pagina sarebbe una voce che non fa
            // niente. Per una vista collegata dentro la pagina nessuna delle
            // due: una vista è fatta per stare lì, e come pagina a sé c'è
            // già "Open as page".
            if (block.type == BlockType.DATABASE_LINK) {
                if (!linked) {
                    OptionRow(icon = Icons.Filled.Description, label = Strings.turnIntoPage, onClick = onTurnIntoPage)
                    HorizontalDivider()
                }
            } else {
                OptionRow(icon = Icons.Filled.TableChart, label = Strings.turnIntoDatabase, onClick = onTurnIntoDatabase)
                HorizontalDivider()
            }
            // Le due voci ci sono sempre, e quella che il database è già si
            // vede spenta con la spunta: dice com'è adesso senza bisogno di
            // un'altra riga.
            OptionRow(
                icon = Icons.Filled.GridOn,
                label = EditorStrings.turnIntoSimpleDatabase,
                enabled = !data.isSimpleDatabase,
                trailing = if (data.isSimpleDatabase) {
                    { CurrentMark() }
                } else {
                    null
                },
                onClick = onTurnIntoSimple
            )
            HorizontalDivider()
            OptionRow(
                icon = Icons.Filled.TableChart,
                label = EditorStrings.turnIntoComplexDatabase,
                enabled = data.isSimpleDatabase,
                trailing = if (!data.isSimpleDatabase) {
                    { CurrentMark() }
                } else {
                    null
                },
                onClick = onTurnIntoComplex
            )
            HorizontalDivider()
            OptionRow(
                icon = if (data.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                label = EditorStrings.lockDatabase,
                trailing = { Switch(checked = data.isLocked, onCheckedChange = { onToggleLock() }) },
                onClick = onToggleLock
            )
            HorizontalDivider()
            OptionRow(icon = Icons.Filled.OpenInFull, label = EditorStrings.openAsPage, onClick = onOpenAsPage)
        }
        Spacer(modifier = Modifier.size(12.dp))
    }

    // **Per un database niente "Turn into" né "Color"** (dal 03/11/2026,
    // chiesto dall'utente: "Togli Turn into e Color dalle impostazioni dei
    // database"): erano tutte e due grigie, senza niente da fare.
    if (!page.isDatabase) {
        OptionsGroup {
            // Trasformare una pagina in un'altra cosa non è ancora
            // costruito: la voce c'è, grigia, come nei menu "/" e "+".
            OptionRow(icon = Icons.Filled.Autorenew, label = EditorStrings.turnInto, enabled = false, onClick = {})
            HorizontalDivider()
            // Il colore di una pagina è quello del suo nome nel collegamento.
            OptionRow(
                icon = Icons.Filled.Brush,
                label = EditorStrings.color,
                enabled = block.type == BlockType.PAGE_LINK,
                trailing = if (block.type == BlockType.PAGE_LINK) {
                    { Chevron() }
                } else {
                    null
                },
                onClick = onColor
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
    }
    OptionsGroup {
        OptionRow(icon = Icons.Filled.EmojiEmotions, label = EditorStrings.editIcon, onClick = onEditIcon)
        HorizontalDivider()
        OptionRow(
            icon = if (page.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            label = if (page.isFavorite) EditorStrings.removeFromFavorites else EditorStrings.addToFavorites,
            iconTint = if (page.isFavorite) FavoriteStar else null,
            onClick = onToggleFavorite
        )
    }
    Spacer(modifier = Modifier.size(12.dp))
    OptionsGroup {
        OptionRow(icon = Icons.Filled.Edit, label = EditorStrings.rename, onClick = onRename)
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.ContentCopy, label = Strings.duplicate, onClick = onDuplicate)
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.DriveFileMove, label = Strings.moveTo, onClick = onMoveTo)
        HorizontalDivider()
        OptionRow(icon = Icons.Filled.TableRows, label = Strings.moveToDatabase, onClick = onMoveToDatabase)
        HorizontalDivider()
        OptionRow(
            icon = Icons.Filled.Delete,
            label = Strings.moveToTrash,
            tint = MaterialTheme.colorScheme.error,
            onClick = onMoveToTrash
        )
    }
}

/**
 * "Turn into": **le stesse voci dei menu "/" e "+"**, nello stesso ordine
 * (`SLASH_ENTRIES`), con quelle non ancora costruite grigie. Il tipo che
 * la riga ha già porta la spunta.
 */
@Composable
private fun TurnIntoList(
    block: BlockEntity,
    onBack: () -> Unit,
    onPick: (SlashEntry) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back)
        }
        Text(
            text = EditorStrings.turnInto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    // "Link to page", le immagini e gli altri file non ci sono: il testo
    // della riga andrebbe perso (dal 06/10/2026).
    val entries = slashEntries().filter {
        it.action != SlashAction.LinkToPage && it.action !is SlashAction.Media && it.action != SlashAction.LinkButton &&
            it.action != SlashAction.Record && it.action != SlashAction.Scan
    }
    OptionsGroup {
        entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider()
            val current = (entry.action as? SlashAction.Type)?.type == block.type
            OptionRow(
                icon = entry.icon,
                label = EditorStrings.blockType(entry.label),
                enabled = entry.enabled,
                trailing = if (current) {
                    { CurrentMark() }
                } else {
                    null
                },
                onClick = { onPick(entry) }
            )
        }
    }
}

/** Il nuovo nome di una pagina o di un database, da "Rename". */
@Composable
private fun RenamePageDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Tutto il nome selezionato: di solito lo si riscrive da capo, e per
    // correggerlo basta un tocco dentro.
    var value by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val confirm = { onConfirm(value.text.replace("\n", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(EditorStrings.rename) },
        text = {
            AutoCaps {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
                    singleLine = true,
                    placeholder = { Text(Strings.untitled) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = { TextButton(onClick = confirm) { Text(Strings.done) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}
