package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.chosenEmoji
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.i18n.LockStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.gabriele.notionlocal.platform.LocalPlatformContext

/** La pagina richiamata, una volta letta: null dentro vuol dire che non c'è più. */
private class MentionedPage(val page: PageEntity?)

/**
 * **"Link to page"** (dal 06/10/2026, chiesto dall'utente): il rimando a una
 * pagina che sta da un'altra parte del Workspace, come su Notion. Si vede
 * come il collegamento a una sottopagina — icona e nome, sempre quelli di
 * adesso — con in più la **freccetta** sull'icona, che dice "è altrove".
 * Toccandolo si apre la pagina; tenendolo premuto si apre il menu del blocco.
 *
 * Se la pagina è nel cestino, o cancellata per sempre, lo dice e non apre
 * niente.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageMentionBlockContent(
    block: BlockEntity,
    viewModel: PageEditorViewModel,
    onOpenPage: (PageEntity) -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val pageId = block.attrs.pageId
    val loaded by remember(pageId) {
        if (pageId == null) flowOf(MentionedPage(null)) else viewModel.observePageInfo(pageId).map { MentionedPage(it) }
    }.collectAsStateWithLifecycle(initialValue = null)
    val page = loaded?.page
    val gone = loaded != null && (page == null || page.trashedAt != null)
    val context = LocalPlatformContext.current
    val store = remember(context) { PageImageStore(context) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onRightClick(onLongPress)
            .combinedClickable(
                enabled = page != null || onLongPress != null,
                onClick = { if (page != null && page.trashedAt == null) onOpenPage(page) },
                onLongClick = onLongPress
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            val image = page?.iconImage
            when {
                image != null -> PageImage(
                    fileName = image,
                    store = store,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                )
                page?.iconSymbol != null -> SymbolIcon(page.iconSymbol, 22.dp)
                page?.isDatabase == true && page.chosenEmoji == null -> Icon(Icons.Filled.TableChart, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(20.dp))
                else -> Text(page?.icon ?: "📄", style = MaterialTheme.typography.bodyLarge)
            }
            // La freccetta in basso a destra: un rimando, non una sottopagina.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(11.dp)
                    .background(DarkSheet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.NorthEast, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(9.dp))
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = when {
                loaded == null -> ""
                page == null -> BlockStrings.deletedPage
                else -> page.title.ifBlank { Strings.untitled }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (gone) NotionGray400 else NotionWhite,
            textDecoration = if (gone) TextDecoration.LineThrough else TextDecoration.Underline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (page?.trashedAt != null) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(BlockStrings.inTrash, style = MaterialTheme.typography.labelMedium, color = NotionGray400)
        }
        if (page?.isProtected == true) {
            Spacer(modifier = Modifier.size(6.dp))
            Icon(Icons.Filled.Lock, contentDescription = LockStrings.passwordLock, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}
