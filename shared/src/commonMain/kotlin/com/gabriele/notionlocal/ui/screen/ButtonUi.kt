package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.PlatformContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.buttons.ButtonConfig
import com.gabriele.notionlocal.data.buttons.ButtonHost
import com.gabriele.notionlocal.data.buttons.ButtonOutcome
import com.gabriele.notionlocal.data.buttons.ButtonPlace
import com.gabriele.notionlocal.data.buttons.ButtonRunner
import com.gabriele.notionlocal.data.buttons.InsertPlace
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.NewBlock
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.ButtonStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.openUrl
import com.gabriele.notionlocal.data.notify.postButtonNotification
import com.gabriele.notionlocal.data.applicationContextOf

const val BUTTON_PILL_TAG = "buttonPill"

/** La domanda di "Show confirmation" in attesa della risposta. */
class ButtonConfirm(val message: String, val answer: CompletableDeferred<Boolean>)

/**
 * **Chi fa partire i bottoni di una schermata** (dal 27/10/2026): esegue le
 * azioni con `ButtonRunner`, fa le domande di "Show confirmation"
 * (`ButtonRunDialogs`), apre le pagine e i link, e alla fine dice con un
 * avviso quello che non si vede: i problemi, le righe nate o cambiate in un
 * altro database. Un bottone che sta ancora lavorando non riparte.
 */
@Stable
class ButtonRunController(
    private val runner: ButtonRunner,
    private val scope: CoroutineScope,
    private val appContext: PlatformContext,
    private val onOpenPage: (String, Boolean) -> Unit,
    private val onInsert: (suspend (String, String?, InsertPlace, List<NewBlock>) -> Boolean?)?
) {
    var confirming by mutableStateOf<ButtonConfirm?>(null)
        private set
    private var running by mutableStateOf<Set<String>>(emptySet())

    fun isRunning(key: String): Boolean = key in running

    private val host = object : ButtonHost {
        override suspend fun confirm(message: String): Boolean {
            val answer = CompletableDeferred<Boolean>()
            confirming = ButtonConfirm(message, answer)
            return try {
                answer.await()
            } finally {
                confirming = null
            }
        }

        override suspend fun insertBlocks(pageId: String, anchorBlockId: String?, place: InsertPlace, blocks: List<NewBlock>): Boolean? =
            onInsert?.invoke(pageId, anchorBlockId, place, blocks)

        override fun openPage(pageId: String, isDatabase: Boolean) = onOpenPage(pageId, isDatabase)

        override fun openUrl(url: String) {
            openUrl(appContext, url)
        }

        override fun notify(title: String, text: String): Boolean = postButtonNotification(appContext, title, text)
    }

    /** Esegue le azioni di [config]; [key] distingue il bottone toccato (il blocco, o riga e colonna). */
    fun run(key: String, config: ButtonConfig, place: ButtonPlace) {
        if (key in running || !config.isSet) return
        running = running + key
        scope.launch {
            try {
                report(runner.run(config, place, host, zone = AppSettings.zoneId))
            } finally {
                running = running - key
            }
        }
    }

    private fun report(outcome: ButtonOutcome) {
        val lines = outcome.problems.distinct().map { ButtonStrings.problem(it) } +
            listOfNotNull(
                outcome.createdRows.takeIf { it > 0 }?.let { ButtonStrings.pagesAdded(it) },
                outcome.changedRows.takeIf { it > 0 }?.let { ButtonStrings.pagesEdited(it) }
            )
        if (lines.isNotEmpty()) showMessage(appContext, lines.joinToString("\n"), long = true)
    }
}

val LocalButtonRunController = compositionLocalOf<ButtonRunController?> { null }

@Composable
fun rememberButtonRunController(
    pages: PageRepository,
    onOpenPage: (String, Boolean) -> Unit,
    onInsert: (suspend (String, String?, InsertPlace, List<NewBlock>) -> Boolean?)? = null
): ButtonRunController {
    val scope = rememberCoroutineScope()
    val context = LocalPlatformContext.current
    val open by rememberUpdatedState(onOpenPage)
    return remember(pages) {
        // Le immagini di un template si copiano per la pagina nuova (vedi `ButtonStep.AddPage.templateId`).
        val store = PageImageStore(applicationContextOf(context))
        ButtonRunController(ButtonRunner(pages) { store.copy(it) }, scope, applicationContextOf(context), { id, db -> open(id, db) }, onInsert)
    }
}

/** La domanda di "Show confirmation", finché un bottone la sta facendo. */
@Composable
fun ButtonRunDialogs(controller: ButtonRunController) {
    val asking = controller.confirming ?: return
    AlertDialog(
        onDismissRequest = { asking.answer.complete(false) },
        containerColor = DarkSheet,
        text = { Text(asking.message.ifBlank { ButtonStrings.areYouSure }, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = { asking.answer.complete(true) }) { Text(ButtonStrings.continueLabel) } },
        dismissButton = { TextButton(onClick = { asking.answer.complete(false) }) { Text(Strings.cancel) } }
    )
}

/**
 * **Il bottone**: una pastiglia grigia con l'icona e il nome, come su Notion.
 * [config] null: il bottone vero di una copia collegata non c'è più. [linked]
 * mette la freccetta delle copie collegate; [running] lo spegne un poco
 * finché lavora.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ButtonPill(
    config: ButtonConfig?,
    linked: Boolean,
    running: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier
            .testTag(BUTTON_PILL_TAG)
            .clip(RoundedCornerShape(6.dp))
            .background(NotionGray900)
            .onRightClick(onLongClick)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 6.dp)
            .alpha(if (running) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (config != null && config.hasIcon) {
            PageIconView(
                emoji = config.icon ?: "",
                iconImage = config.iconImage,
                isDatabase = false,
                size = if (compact) 16.dp else 18.dp,
                iconSymbol = config.iconSymbol
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        Text(
            text = when {
                config == null -> ButtonStrings.originalDeleted
                else -> config.name.ifBlank { ButtonStrings.newButton }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (config == null) NotionGray400 else NotionWhite,
            textDecoration = if (config == null) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (linked) {
            Spacer(modifier = Modifier.size(6.dp))
            Icon(Icons.Filled.NorthEast, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(12.dp))
        }
    }
}

/** Il bottone vero di una copia collegata, una volta letto: null dentro vuol dire che non c'è più. */
private class LinkedSource(val block: BlockEntity?)

/**
 * **Il blocco "Button"** in una pagina (dal 27/10/2026). Toccato esegue le
 * sue azioni; se non ne ha ancora, apre la finestra per sceglierle. Tenuto
 * premuto apre il menu del blocco, dove c'è "Edit button". Una copia
 * collegata usa nome e azioni del bottone vero, e cambiandole da qui cambia
 * quelle.
 */
@Composable
fun ButtonBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val controller = LocalButtonRunController.current
    val context = LocalPlatformContext.current
    val focus = LocalFocusManager.current
    val locked = LocalPageLocked.current
    val attrs = block.attrs
    val linkId = attrs.buttonLink
    val source by remember(linkId) {
        if (linkId == null) flowOf(LinkedSource(null)) else viewModel.observeBlock(linkId).map { LinkedSource(it) }
    }.collectAsStateWithLifecycle(initialValue = null)
    val sourceBlock = source?.block?.takeIf { it.type == BlockType.BUTTON }
    // Ancora da leggere: niente, per non far vedere "Deleted button" per un attimo.
    if (linkId != null && source == null) return
    val config: ButtonConfig? = if (linkId == null) attrs.button ?: ButtonConfig() else sourceBlock?.attrs?.button ?: sourceBlock?.let { ButtonConfig() }
    val realId = if (linkId == null) block.id else sourceBlock?.id

    // "This page": la riga di cui questa pagina è la pagina, se lo è.
    val row by produceState<com.gabriele.notionlocal.data.entity.DatabaseRowEntity?>(null, block.pageId) {
        value = viewModel.rowOfPage(block.pageId)
    }
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ButtonPill(
            config = config,
            linked = linkId != null,
            running = controller?.isRunning(block.id) == true,
            onClick = {
                focus.clearFocus()
                when {
                    config == null -> showMessage(context, ButtonStrings.originalDeletedNote)
                    !config.isSet -> if (!locked) viewModel.requestButtonEdit(block.id)
                    else -> controller?.run(block.id, config, ButtonPlace(pageId = block.pageId, blockId = block.id, rowId = row?.id))
                }
            },
            onLongClick = onLongPress
        )
    }

}

/**
 * **La finestra delle azioni di un blocco "Button"**, finché
 * `PageEditorViewModel.buttonEditRequest` la chiede: sta fuori dall'elenco
 * dei blocchi, perché un blocco che scorre fuori dallo schermo esce
 * dall'elenco, e con lui si chiuderebbe la finestra. Per una copia collegata
 * si cambia il bottone vero.
 */
@Composable
fun ButtonEditHost(viewModel: PageEditorViewModel) {
    val blockId by viewModel.buttonEditRequest.collectAsStateWithLifecycle()
    val id = blockId ?: return
    val block by remember(id) { viewModel.observeBlock(id).map { LinkedSource(it) } }.collectAsStateWithLifecycle(initialValue = null)
    val loaded = block ?: return
    val shown = loaded.block?.takeIf { it.type == BlockType.BUTTON }
    if (shown == null) {
        // Il bottone non c'è più (cancellato, o un Annulla): niente da mostrare.
        LaunchedEffect(id) { viewModel.consumeButtonEditRequest() }
        return
    }
    val linkId = shown.attrs.buttonLink
    val source by remember(linkId) {
        if (linkId == null) flowOf(LinkedSource(shown)) else viewModel.observeBlock(linkId).map { LinkedSource(it) }
    }.collectAsStateWithLifecycle(initialValue = null)
    val real = source?.let { it.block?.takeIf { b -> b.type == BlockType.BUTTON } } ?: run {
        if (source != null) LaunchedEffect(id) { viewModel.consumeButtonEditRequest() }
        return
    }
    // "This page": la riga di cui la pagina del bottone toccato è la pagina.
    val row by produceState<com.gabriele.notionlocal.data.entity.DatabaseRowEntity?>(null, shown.pageId) {
        value = viewModel.rowOfPage(shown.pageId)
    }
    key(id) {
        ButtonEditorSheet(
            initial = real.attrs.button ?: ButtonConfig(),
            inDatabase = false,
            thisPageDatabaseId = row?.pageId,
            linked = linkId != null,
            onDismiss = { viewModel.consumeButtonEditRequest() },
            onSave = { viewModel.saveButton(real.id, it) }
        )
    }
}

/**
 * Chi apre la finestra delle azioni di una proprietà Button: toccando un
 * bottone che non ne ha ancora si sceglie cosa deve fare, come per il
 * blocco. Null dove non si può (il database bloccato, la pagina di una riga).
 */
val LocalButtonColumnEditor = compositionLocalOf<((com.gabriele.notionlocal.data.entity.DatabaseColumnEntity) -> Unit)?> { null }

/**
 * **La cella di una proprietà Button** (dal 27/10/2026): il bottone di
 * questa riga. Le azioni sono quelle della proprietà, uguali per tutte le
 * righe; "This page" è la riga. La scritta, se non se ne è scelta una, è il
 * nome della proprietà.
 */
@Composable
fun ButtonCell(
    column: com.gabriele.notionlocal.data.entity.DatabaseColumnEntity,
    row: com.gabriele.notionlocal.data.entity.DatabaseRowEntity
) {
    val controller = LocalButtonRunController.current
    val editColumn = LocalButtonColumnEditor.current
    val config = remember(column.optionsJson) { ButtonConfig.parse(column.optionsJson) }
    val shown = config.copy(name = config.name.ifBlank { com.gabriele.notionlocal.ui.i18n.DbStrings.columnName(column.name) })
    val key = "${row.id}:${column.id}"
    val focus = LocalFocusManager.current
    ButtonPill(
        config = shown,
        linked = false,
        running = controller?.isRunning(key) == true,
        onClick = {
            focus.clearFocus()
            if (!config.isSet) {
                editColumn?.invoke(column)
            } else {
                controller?.run(key, config, ButtonPlace(pageId = row.linkedPageId, rowId = row.id))
            }
        },
        onLongClick = null,
        compact = true
    )
}

/**
 * **"Link to button"**: i bottoni di tutte le pagine, col nome della pagina
 * sotto, per sceglierne uno di cui mettere qui una copia collegata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkToButtonSheet(
    loadChoices: suspend () -> List<PageRepository.ButtonChoice>,
    onDismiss: () -> Unit,
    onPick: (PageRepository.ButtonChoice) -> Unit
) {
    val choices by produceState<List<PageRepository.ButtonChoice>?>(null) { value = loadChoices() }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).navigationBarsPadding()) {
            Text(
                ButtonStrings.linkToButton,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                ButtonStrings.linkToButtonHint,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            )
            val list = choices
            when {
                list == null -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                list.isEmpty() -> Text(
                    ButtonStrings.noButtonsYet,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.blockId }) { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(choice) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ButtonPill(config = choice.config, linked = false, running = false, onClick = { onPick(choice) }, onLongClick = null)
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                Strings.inPlace(choice.page.title.ifBlank { Strings.untitled }),
                                color = NotionGray400,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
