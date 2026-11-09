package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.LinkPreviews
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
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import java.util.Locale
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.FileKind
import com.gabriele.notionlocal.platform.urlLastPathSegment

/** Quanto pesa un file, da leggere: "820 B", "12 KB", "1,4 MB". */
fun formatFileSize(bytes: Long, locale: Locale = Locale.getDefault()): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (value >= 10) "%.0f %s".format(locale, value, units[unit]) else "%.1f %s".format(locale, value, units[unit])
}

/** L'icona che dice che tipo di file è. */
private fun fileIcon(mime: String?): ImageVector = when {
    mime == null -> Icons.Outlined.InsertDriveFile
    mime == "application/pdf" -> Icons.Outlined.PictureAsPdf
    mime.startsWith("image/") -> Icons.Outlined.Image
    mime.startsWith("video/") -> Icons.Outlined.Movie
    mime.startsWith("audio/") -> Icons.Outlined.AudioFile
    mime.startsWith("text/") || mime.contains("document") || mime.contains("msword") -> Icons.Outlined.Description
    mime.contains("zip") || mime.contains("compressed") || mime.contains("rar") || mime.contains("7z") -> Icons.Outlined.FolderZip
    else -> Icons.Outlined.InsertDriveFile
}

/**
 * **Il blocco "File"** (dal 06/10/2026, chiesto dall'utente): un file
 * qualsiasi caricato dal telefono (o il link a un file), con la sua icona,
 * il nome e il peso. **Toccandolo si apre** con l'app che lo sa aprire; la
 * freccia a destra lo **scarica** dove si sceglie ("ovunque io sia").
 * Tenendolo premuto: aprirlo, scaricarlo, sostituirlo, copiarlo, toglierlo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileBlockContent(
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
    val import = rememberImport(block, viewModel, store, FileKind.ANY) { busy = it }
    HandleFileRequests(block, viewModel, locked, actions) { choosing = true }

    val file = attrs.file
    val url = attrs.url
    if (file == null && url == null) {
        MediaPlaceholder(
            icon = Icons.Outlined.UploadFile,
            text = BlockStrings.addFile,
            busy = busy,
            onClick = if (locked) null else { { choosing = true } },
            onLongPress = onLongPress,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .combinedClickable(onClick = actions.openElsewhere, onLongClick = onLongPress)
                .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (file != null) fileIcon(attrs.mime) else Icons.Filled.Link,
                contentDescription = null,
                tint = NotionGray400,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(
                    attrs.name ?: url?.let { urlLastPathSegment(it) ?: linkHost(it) } ?: EditorStrings.blockType("File"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = NotionWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = if (file != null) attrs.size?.let { formatFileSize(it) } else url?.let { linkHost(it) }
                if (detail != null) {
                    Text(detail, style = MaterialTheme.typography.labelMedium, color = NotionGray400, maxLines = 1)
                }
            }
            if (file != null) {
                IconButton(onClick = actions.saveCopy) {
                    Icon(Icons.Filled.Download, contentDescription = BlockStrings.download, tint = NotionGray400)
                }
            }
        }
    }

    if (choosing) {
        MediaSourceSheet(
            title = EditorStrings.blockType("File"),
            uploadLabel = BlockStrings.chooseFile,
            linkButton = BlockStrings.embedLink,
            linkHint = null,
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
 * **Il blocco "Web bookmark"** (dal 06/10/2026, chiesto dall'utente): un link
 * a un sito, come una scheda — il titolo, la descrizione e l'immagine che il
 * sito dà per le anteprime, e l'indirizzo. **Toccandolo si apre nel browser**
 * predefinito (o nell'app del sito, se c'è). L'anteprima si legge dalla rete
 * una volta sola, appena il segnalibro nasce; senza rete resta l'indirizzo, e
 * ci si riprova alla prossima apertura della pagina.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    locked: Boolean,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }
    val attrs = remember(block.attrsJson) { block.attrs }
    var choosing by remember(block.id) { mutableStateOf(false) }
    val actions = rememberFileActions(attrs, store)
    HandleFileRequests(block, viewModel, locked, actions) { choosing = true }

    val url = attrs.url
    // L'anteprima, se non c'è ancora: una volta per apertura della pagina.
    // Solo il link fa da chiave: con altro, cambiarlo a metà strada
    // interromperebbe la lettura del sito.
    LaunchedEffect(block.id, url) {
        if (url == null || attrs.name != null || attrs.description != null || attrs.image != null) return@LaunchedEffect
        val preview = LinkPreviews.fetch(url) ?: return@LaunchedEffect
        val image = preview.imageUrl?.let { LinkPreviews.downloadImage(it) }?.let { store.saveImageBytes(it) }
        viewModel.fillBlockAttrs(block.id) { current ->
            // Il link è cambiato nel frattempo: l'anteprima non è più sua.
            if (current.url != url) current else current.copy(name = preview.title, description = preview.description, image = image)
        }
    }

    if (url == null) {
        MediaPlaceholder(
            icon = Icons.Outlined.BookmarkBorder,
            text = BlockStrings.addBookmark,
            busy = false,
            onClick = if (locked) null else { { choosing = true } },
            onLongPress = onLongPress,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                .combinedClickable(onClick = actions.openElsewhere, onLongClick = onLongPress),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                Text(
                    attrs.name ?: linkHost(url),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = NotionWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                attrs.description?.let {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = NotionGray400, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.size(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(url, style = MaterialTheme.typography.labelSmall, color = NotionGray400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            attrs.image?.let { image ->
                PageImage(
                    fileName = image,
                    store = store,
                    contentDescription = null,
                    modifier = Modifier.width(112.dp).height(92.dp)
                )
            }
        }
    }

    if (choosing) {
        BookmarkSheet(
            current = url.orEmpty(),
            onCreate = { link ->
                choosing = false
                // Un link nuovo: l'anteprima di prima non vale più.
                viewModel.updateBlockAttrs(block.id) { BlockAttrs(url = link) }
            },
            onDismiss = { choosing = false }
        )
    }
}

/** **"Create bookmark"**: il campo per incollare il link, come nella decima foto dell'utente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkSheet(current: String, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var link by remember { mutableStateOf(current) }
    var wrong by remember { mutableStateOf(false) }
    val submit = {
        val url = normalizedLink(link)
        if (url == null) wrong = true else onCreate(url)
    }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            Text(
                EditorStrings.blockType("Web bookmark"),
                style = MaterialTheme.typography.titleMedium,
                color = NotionWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
                Text(BlockStrings.notALink, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = submit, enabled = link.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(BlockStrings.createBookmark) }
            Text(BlockStrings.bookmarkHint, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
