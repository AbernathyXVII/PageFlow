package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.BlockFiles
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockAttrs
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.MediaRequestKind
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import androidx.compose.material3.OutlinedButton
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.FileKind
import com.gabriele.notionlocal.platform.rememberFileImporter
import com.gabriele.notionlocal.platform.rememberSaveCopy
import com.gabriele.notionlocal.platform.urlLastPathSegment

/**
 * **"Upload" o "Link"** (dal 06/10/2026), la finestrella delle foto di
 * Notion per video, audio e file: o si sceglie un file dal telefono, o si
 * incolla un indirizzo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSourceSheet(
    title: String,
    uploadLabel: String,
    linkButton: String,
    linkHint: String?,
    onUpload: () -> Unit,
    onLink: (String) -> Unit,
    onDismiss: () -> Unit,
    /** Un secondo bottone sotto "Upload": "Record audio" per l'audio (dal 03/11/2026). */
    extraLabel: String? = null,
    onExtra: (() -> Unit)? = null
) {
    var linkTab by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = NotionWhite, modifier = Modifier.padding(bottom = 12.dp))
            Row {
                listOf(false to BlockStrings.uploadTab, true to BlockStrings.linkTab).forEach { (isLink, label) ->
                    val selected = linkTab == isLink
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { linkTab = isLink }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) NotionWhite else NotionGray400)
                        Spacer(modifier = Modifier.size(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .background(if (selected) NotionWhite else Color.Transparent)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (!linkTab) {
                Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) { Text(uploadLabel) }
                if (extraLabel != null && onExtra != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                    OutlinedButton(onClick = onExtra, modifier = Modifier.fillMaxWidth()) { Text(extraLabel) }
                }
            } else {
                val submit = {
                    val url = normalizedLink(link)
                    if (url == null) wrong = true else onLink(url)
                }
                AutoCaps {
                    OutlinedTextField(
                        value = link,
                        onValueChange = {
                            link = it
                            wrong = false
                        },
                        singleLine = true,
                        isError = wrong,
                        placeholder = { Text(BlockStrings.pasteLink) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (wrong) {
                    Text(
                        BlockStrings.notALink,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Button(onClick = submit, enabled = link.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(linkButton) }
                if (linkHint != null) {
                    Text(linkHint, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Le cose che un blocco-file fa da sé, chieste dal suo menu: salvare una
 * copia dove si sceglie, aprirlo con un'altra app o nel browser.
 */
@Stable
class FileActions(val saveCopy: () -> Unit, val openElsewhere: () -> Unit)

@Composable
fun rememberFileActions(attrs: BlockAttrs, store: PageImageStore): FileActions {
    val context = LocalPlatformContext.current
    val saveCopy = rememberSaveCopy(store, attrs.mime) { saved ->
        showMessage(context, if (saved) BlockStrings.fileSaved else BlockStrings.saveFailed)
    }
    return FileActions(
        saveCopy = {
            attrs.file?.let { file ->
                val name = attrs.name ?: ("PageFlow" + (BlockFiles.extensionFor(attrs.mime)?.let { ".$it" } ?: ""))
                saveCopy(file, name)
            }
        },
        openElsewhere = {
            val file = attrs.file
            val url = attrs.url
            val opened = when {
                file != null -> BlockFiles.openWithAnotherApp(context, store, file, attrs.mime)
                url != null -> BlockFiles.openUrl(context, url)
                else -> true
            }
            if (!opened) showMessage(context, BlockStrings.noAppToOpen)
        }
    )
}

/** La riga sotto un video da un link: il nome del sito e i tre puntini del menu del blocco. */
@Composable
private fun EmbedFooter(url: String, onMenu: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
        Text(linkHost(url), style = MaterialTheme.typography.labelMedium, color = NotionGray400, modifier = Modifier.weight(1f))
        if (onMenu != null) {
            IconButton(onClick = onMenu, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = EditorStrings.blockOptions, tint = NotionGray400)
            }
        }
    }
}

/**
 * Le richieste del menu dei sei puntini (vedi `PageEditorViewModel.mediaRequest`)
 * per un blocco di video, audio o file: le fa lui, che resta aperto.
 */
@Composable
fun HandleFileRequests(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    actions: FileActions,
    /** "Record audio" dai menu "/" e "+" (dal 03/11/2026): solo il blocco audio lo sa fare. */
    onRecord: () -> Unit = {},
    onPick: () -> Unit
) {
    val request by viewModel.mediaRequest.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        val asked = request?.takeIf { it.blockId == block.id } ?: return@LaunchedEffect
        viewModel.consumeMediaRequest()
        when (asked.kind) {
            MediaRequestKind.PICK -> if (!locked) onPick()
            MediaRequestKind.RECORD -> if (!locked) onRecord()
            MediaRequestKind.DOWNLOAD -> actions.saveCopy()
            MediaRequestKind.OPEN_ELSEWHERE -> actions.openElsewhere()
            MediaRequestKind.VIEW, MediaRequestKind.RESIZE, MediaRequestKind.FULL_RESIZE -> Unit
        }
    }
}

/**
 * Il file scelto dal telefono, copiato nell'app e messo nel blocco; se non
 * si riesce a leggerlo, lo dice.
 */
@Composable
fun rememberImport(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    store: PageImageStore,
    kind: FileKind,
    onBusy: (Boolean) -> Unit
): () -> Unit {
    val context = LocalPlatformContext.current
    return rememberFileImporter(store, kind, onStart = { onBusy(true) }) { imported ->
        if (imported != null) {
            viewModel.updateBlockAttrs(block.id) { imported }
        } else {
            showMessage(context, BlockStrings.fileReadFailed)
        }
        onBusy(false)
    }
}

/**
 * **Il blocco "Video"** (dal 06/10/2026, chiesto dall'utente): un video
 * caricato dal telefono o preso da un link.
 *
 * - **Un file, o il link a un video** (.mp4…): si guarda qui, col lettore
 *   dell'app — play, pausa, la barra col pallino per andare avanti e indietro;
 * - **YouTube e Vimeo**: nel loro lettore, dentro la pagina;
 * - dal **menu del blocco** (tenendolo premuto, o i tre puntini sotto un
 *   video da un link): aprirlo nel browser o nell'app di YouTube, o il file
 *   con un altro lettore; scaricarlo; sostituirlo; copiarlo; toglierlo.
 */
@Composable
fun VideoBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    var busy by remember(block.id) { mutableStateOf(false) }
    var choosing by remember(block.id) { mutableStateOf(false) }
    val actions = rememberFileActions(attrs, store)
    val import = rememberImport(block, viewModel, store, FileKind.VIDEO) { busy = it }
    HandleFileRequests(block, viewModel, locked, actions) { choosing = true }

    val file = attrs.file
    val url = attrs.url
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        when {
            file != null -> InlineVideoPlayer(MediaSource.LocalFile(store.fileFor(file)), onLongPress = onLongPress)
            url != null -> {
                val youTube = youTubeId(url)
                val vimeo = vimeoId(url)
                val embed = when {
                    youTube != null -> "https://www.youtube-nocookie.com/embed/$youTube?playsinline=1&rel=0"
                    vimeo != null -> "https://player.vimeo.com/video/$vimeo"
                    else -> null
                }
                Column {
                    if (embed != null) EmbeddedVideoPlayer(embed) else InlineVideoPlayer(MediaSource.Link(url), onLongPress = onLongPress)
                    EmbedFooter(url, onLongPress)
                }
            }
            else -> MediaPlaceholder(
                icon = Icons.Outlined.OndemandVideo,
                text = BlockStrings.addVideo,
                busy = busy,
                onClick = if (locked) null else { { choosing = true } },
                onLongPress = onLongPress
            )
        }
    }

    if (choosing) {
        MediaSourceSheet(
            title = EditorStrings.blockType("Video"),
            uploadLabel = BlockStrings.chooseVideo,
            linkButton = BlockStrings.embedVideo,
            linkHint = BlockStrings.videoLinkHint,
            onUpload = {
                choosing = false
                import()
            },
            onLink = { link ->
                choosing = false
                viewModel.updateBlockAttrs(block.id) { BlockAttrs(url = link) }
            },
            onDismiss = { choosing = false }
        )
    }
}

/**
 * **Il blocco "Audio"** (dal 06/10/2026, chiesto dall'utente): un file audio
 * caricato dal telefono o preso da un link, che si ascolta qui — play,
 * pausa, la barra col pallino (settima foto). Tenendolo premuto: aprirlo con
 * un'altra app o nel browser, scaricarlo, sostituirlo, copiarlo, toglierlo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    var busy by remember(block.id) { mutableStateOf(false) }
    var choosing by remember(block.id) { mutableStateOf(false) }
    val actions = rememberFileActions(attrs, store)
    val import = rememberImport(block, viewModel, store, FileKind.AUDIO) { busy = it }
    // **Registrare da qui** (dal 03/11/2026): vedi `AudioRecorder`.
    val recording by AudioRecorder.current.collectAsStateWithLifecycle()
    val record = rememberRecordStarter(block)
    HandleFileRequests(block, viewModel, locked, actions, onRecord = record) { choosing = true }

    val file = attrs.file
    val url = attrs.url
    val recordingHere = recording?.takeIf { it.blockId == block.id }
    if (recordingHere != null) {
        RecordingBar(
            recording = recordingHere,
            onStop = { AudioRecorder.stop(context) },
            onDiscard = { AudioRecorder.stop(context, keep = false) },
            modifier = modifier
        )
    } else if (file == null && url == null) {
        MediaPlaceholder(
            icon = Icons.Outlined.Audiotrack,
            text = BlockStrings.addAudio,
            busy = busy,
            onClick = if (locked) null else { { choosing = true } },
            onLongPress = onLongPress,
            modifier = modifier
        )
    } else {
        val source = if (file != null) MediaSource.LocalFile(store.fileFor(file)) else MediaSource.Link(url.orEmpty())
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            InlineAudioPlayer(source, title = attrs.name ?: url?.let { linkHost(it) } ?: EditorStrings.blockType("Audio"))
        }
    }

    if (choosing) {
        MediaSourceSheet(
            title = EditorStrings.blockType("Audio"),
            uploadLabel = BlockStrings.chooseFile,
            linkButton = BlockStrings.embedAudio,
            linkHint = null,
            onUpload = {
                choosing = false
                import()
            },
            onLink = { link ->
                choosing = false
                viewModel.updateBlockAttrs(block.id) { BlockAttrs(url = link, name = urlLastPathSegment(link)) }
            },
            onDismiss = { choosing = false },
            extraLabel = CaptureStrings.recordAudio,
            onExtra = {
                choosing = false
                record()
            }
        )
    }
}
