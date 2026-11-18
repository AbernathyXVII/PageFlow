package com.gabriele.notionlocal.ui.screen

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.repository.PageRepository.IncomingBlock
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.share.Incoming
import com.gabriele.notionlocal.data.share.hostOf
import com.gabriele.notionlocal.data.share.splitSharedText
import com.gabriele.notionlocal.ui.i18n.ShareStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.MoveToInbox
import com.gabriele.notionlocal.data.ocr.ImageTexts
import com.gabriele.notionlocal.data.saveFromGallery
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext

// ---------------------------------------------------------------------------
// **"Condividi con notE"** (dal 02/11/2026, chiesto dall'utente): la finestra
// che sale sopra Chrome, la Galleria o WhatsApp dopo aver scelto notE in
// "Condividi". Il titolo si può cambiare; sotto si vede cosa arriva (il link,
// il testo, quante foto e file), dove va ("Save to", la pagina scelta o
// Inbox) e se diventa una pagina nuova o finisce in fondo a quella pagina.
// "Save" salva e si torna all'app di prima; "Save and open" apre notE lì.
// ---------------------------------------------------------------------------

const val SHARE_SHEET_TAG = "shareSheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    incoming: Incoming,
    factory: ViewModelFactory,
    onClose: () -> Unit,
    /** Salvato con "Save and open": la pagina da aprire in notE. */
    onOpen: (String) -> Unit
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val repository = factory.pageRepository
    val shared = remember(incoming) { splitSharedText(incoming.text, incoming.subject) }
    val imageCount = remember(incoming) { incoming.files.count { mimeOf(context, it, incoming.mime).startsWith("image/") } }
    val fileCount = incoming.files.size - imageCount
    var title by remember(incoming) {
        mutableStateOf(shared.title.ifBlank { incoming.files.firstOrNull()?.let { BlockFiles.displayName(context, it) }.orEmpty() })
    }
    var asNewPage by remember { mutableStateOf(true) }
    var destinationTitle by remember { mutableStateOf(ShareStrings.inbox) }
    var picking by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // **Lo stato dello schermo si scrive solo dal suo thread** (in tutta la
    // finestra): dopo il database si riprende dove capita, e uno stato scritto
    // da lì lasciava Compose in sospeso per sempre (nei test, i test dopo
    // questo non finivano più). Il database va su IO, il resto su Main.
    LaunchedEffect(AppSettings.sharePageId) {
        val chosen = AppSettings.sharePageId
        withContext(Dispatchers.Main.immediate) {
            destinationTitle = withContext(Dispatchers.IO) {
                chosen?.let { repository.getPage(it) }
                    ?.takeIf { it.trashedAt == null && !it.isDatabase }
                    ?.title?.ifBlank { null }
            } ?: ShareStrings.inbox
        }
    }

    // **Prima la finestra scende, poi l'attività si chiude.** Chiudendo
    // l'attività con la finestra ancora su, la finestra restava appesa (nei
    // test teneva occupato lo schermo per sempre). `after` è cosa fare quando
    // la finestra non c'è più: tornare all'app di prima, o aprire notE.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var after by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun closeThen(action: () -> Unit) {
        scope.launch(Dispatchers.Main.immediate) {
            runCatching { sheetState.hide() }
            after = action
        }
    }
    after?.let { action ->
        LaunchedEffect(action) { action() }
        return
    }

    fun save(open: Boolean) {
        saving = true
        val chosen = AppSettings.sharePageId
        val lines = if (asNewPage) shared.lines else shared.allLines
        val newPage = asNewPage
        val pageTitle = title
        scope.launch(Dispatchers.Main.immediate) {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val destination = repository.inboxPageId(chosen, ShareStrings.inbox)
                    val blocks = incomingBlocks(context, incoming, lines, shared.link)
                    val pageId = repository.saveIncoming(destination, pageTitle, blocks, newPage)
                    val where = repository.getPage(destination)?.title?.ifBlank { null } ?: ShareStrings.inbox
                    Triple(destination, pageId, where)
                }.getOrNull()
            }
            saving = false
            if (saved == null) {
                showMessage(context, ShareStrings.saveFailed)
            } else {
                AppSettings.sharePageId = saved.first
                // Il testo delle foto arrivate, per la ricerca (dal 03/11/2026).
                ImageTexts.request(delayMs = 1_000)
                showMessage(context, ShareStrings.savedIn(saved.third))
                closeThen { if (open) onOpen(saved.second) else onClose() }
            }
        }
    }

    NoteBottomSheet(
        onDismissRequest = { if (!saving) after = onClose },
        sheetState = sheetState,
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SHARE_SHEET_TAG)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(ShareStrings.saveToNote, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(12.dp))
            if (asNewPage) {
                // La maiuscola a inizio frase anche qui, come in ogni campo dell'app (vedi `AutoCaps`).
                AutoCaps {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.replace("\n", " ") },
                        label = { Text(ShareStrings.title) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
            }
            // Cosa arriva.
            shared.link?.let { PreviewRow(Icons.Filled.Link, hostOf(it) ?: it, it) }
            val lines = if (asNewPage) shared.lines else shared.allLines
            if (lines.any { it.isNotBlank() }) {
                PreviewRow(Icons.Filled.Subject, lines.filter { it.isNotBlank() }.take(3).joinToString("\n"), null)
            }
            if (imageCount > 0) PreviewRow(Icons.Filled.Image, ShareStrings.images(imageCount), null)
            if (fileCount > 0) PreviewRow(Icons.Filled.AttachFile, ShareStrings.files(fileCount), null)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            // Dove va.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !saving) { picking = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ShareStrings.saveTo, color = NotionGray400, modifier = Modifier.padding(end = 12.dp))
                Text(
                    destinationTitle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = NotionGray400)
            }
            ChoiceLine(ShareStrings.asNewPage, asNewPage) { asNewPage = true }
            ChoiceLine(ShareStrings.atTheEnd, !asNewPage) { asNewPage = false }

            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { save(open = true) }, enabled = !saving, modifier = Modifier.weight(1f)) {
                    Text(ShareStrings.saveAndOpen)
                }
                Button(onClick = { save(open = false) }, enabled = !saving, modifier = Modifier.weight(1f)) {
                    if (saving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(ShareStrings.save)
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
    }

    if (picking) {
        DestinationPickerSheet(
            factory = factory,
            title = ShareStrings.saveTo,
            hint = ShareStrings.pickerHint,
            viewModelKey = "share",
            onClear = if (AppSettings.sharePageId != null) {
                {
                    picking = false
                    AppSettings.sharePageId = null
                }
            } else {
                null
            },
            clearLabel = ShareStrings.inboxNote,
            clearIcon = Icons.Filled.MoveToInbox,
            clearIsDestructive = false,
            onDismiss = { picking = false },
            onPick = { node ->
                picking = false
                scope.launch(Dispatchers.Main.immediate) {
                    val id = node.pageId ?: withContext(Dispatchers.IO) { node.rowId?.let { repository.ensureRowPage(it) } } ?: return@launch
                    AppSettings.sharePageId = id
                }
            }
        )
    }
}

@Composable
private fun PreviewRow(icon: ImageVector, text: String, detail: String?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = NotionGray400, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ChoiceLine(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun mimeOf(context: Context, uri: Uri, declared: String?): String =
    runCatching { context.contentResolver.getType(uri) }.getOrNull()
        ?: declared?.takeIf { !it.endsWith("/*") }
        ?: BlockFiles.mimeType(context, uri, BlockFiles.displayName(context, uri))
        ?: declared.orEmpty()

/**
 * **I blocchi di quello che arriva**: le righe di testo, il segnalibro del
 * link (l'anteprima la scarica la pagina la prima volta che si apre), le
 * foto come immagini, video e audio coi loro blocchi, il resto come file. I
 * file si copiano dentro l'app adesso: il permesso di leggerli finisce con
 * questa finestra. Uno che non si riesce a leggere si salta.
 */
private suspend fun incomingBlocks(context: Context, incoming: Incoming, lines: List<String>, link: String?): List<IncomingBlock> {
    val store = PageImageStore(context)
    val blocks = mutableListOf<IncomingBlock>()
    lines.forEach { blocks += IncomingBlock(BlockType.PARAGRAPH, it) }
    link?.let { blocks += IncomingBlock(BlockType.BOOKMARK, attrs = BlockAttrs(url = it)) }
    for (uri in incoming.files) {
        val mime = mimeOf(context, uri, incoming.mime)
        val block = when {
            mime.startsWith("image/") -> store.saveFromGallery(uri).getOrNull()?.let { file ->
                IncomingBlock(BlockType.IMAGE, attrs = BlockAttrs(file = file, name = BlockFiles.displayName(context, uri)))
            }
            else -> importFile(context, store, uri)?.let { attrs ->
                val type = when {
                    mime.startsWith("video/") -> BlockType.VIDEO
                    mime.startsWith("audio/") -> BlockType.AUDIO
                    else -> BlockType.FILE
                }
                IncomingBlock(type, attrs = attrs)
            }
        }
        if (block != null) blocks += block
    }
    return blocks
}
