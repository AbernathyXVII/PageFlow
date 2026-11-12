package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.TrashPolicy
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.TrashViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/**
 * Il cestino: le pagine buttate con "Move to trash".
 *
 * Toccandone una la si apre, in sola lettura, con in cima la barra per
 * ripristinarla o cancellarla per sempre. Qui sopra si sceglie cosa
 * succede da sole alle pagine buttate: restano trenta giorni, o se ne
 * vanno al prossimo avvio dell'app.
 */
@Composable
fun TrashScreen(
    factory: ViewModelFactory,
    onBack: () -> Unit,
    onOpenPage: (PageEntity) -> Unit,
    viewModel: TrashViewModel = viewModel(factory = factory)
) {
    val trash by viewModel.trash.collectAsStateWithLifecycle()
    var confirmEmpty by remember { mutableStateOf(false) }
    val policy = AppSettings.trashPolicy

    Scaffold(
        topBar = {
            WorkspaceTopBar(title = Strings.trash, onBack = onBack) {
                IconButton(onClick = { confirmEmpty = true }, enabled = trash.isNotEmpty()) {
                    Icon(
                        Icons.Filled.DeleteForever,
                        contentDescription = Strings.emptyTrash,
                        tint = if (trash.isNotEmpty()) NotionWhite else NotionGray400
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                RadioItem(
                    title = Strings.keepFor30Days,
                    detail = Strings.keepFor30DaysDetail,
                    selected = policy == TrashPolicy.KEEP_30_DAYS,
                    onClick = { AppSettings.trashPolicy = TrashPolicy.KEEP_30_DAYS }
                )
                RadioItem(
                    title = Strings.emptyAutomatically,
                    detail = Strings.emptyAutomaticallyDetail,
                    selected = policy == TrashPolicy.EMPTY_AUTOMATICALLY,
                    onClick = { AppSettings.trashPolicy = TrashPolicy.EMPTY_AUTOMATICALLY }
                )
                HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (trash.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            Strings.trashIsEmpty,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            items(trash, key = { it.id }) { page ->
                TrashRow(page = page, policy = policy, onClick = { onOpenPage(page) })
            }
        }
    }

    if (confirmEmpty) {
        FitAlertDialog(
            onDismissRequest = { confirmEmpty = false },
            containerColor = DarkSheet,
            title = { Text(Strings.emptyTrashTitle, color = NotionWhite) },
            text = { Text(Strings.emptyTrashText, color = NotionGray400) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEmpty = false
                    viewModel.emptyTrash()
                }) { Text(Strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) { Text(Strings.cancel) }
            }
        )
    }
}

/** Quanti giorni restano a una pagina buttata in quel momento, con la regola dei trenta giorni. */
fun trashDaysLeft(trashedAt: Long): Int {
    val elapsed = System.currentTimeMillis() - trashedAt
    val left = AppSettings.TRASH_RETENTION_DAYS - (elapsed / (24L * 60 * 60 * 1000)).toInt()
    return left.coerceAtLeast(1)
}

@Composable
private fun TrashRow(page: PageEntity, policy: TrashPolicy, onClick: () -> Unit) {
    val trashedAt = page.trashedAt ?: return
    val fate = when (policy) {
        TrashPolicy.KEEP_30_DAYS -> Strings.daysLeft(trashDaysLeft(trashedAt))
        TrashPolicy.EMPTY_AUTOMATICALLY -> Strings.deletedAtNextStart
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, size = 24.dp, iconSymbol = page.iconSymbol)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                page.title.ifBlank { Strings.untitled },
                color = NotionWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${Strings.deletedOn(Formats.dateTime(trashedAt))} · $fate",
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
