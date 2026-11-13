package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.withEmojiFont
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite

/** Le due schede della Library. */
enum class LibraryTab { RECENTS, FAVORITES }

/**
 * **La Library** (dal 13/11/2026, chiesta dall'utente con la foto di Notion):
 * le pagine aperte di recente su questo dispositivo ("Recents", vedi
 * `AppSettings.recentVisits`) e le preferite ("Favorites"), con il nome, dove
 * stanno, l'ultima modifica e l'ultima visita. Ci si arriva da "View all"
 * sotto "Recents" nella barra laterale. Larga (il PC) è una tabella come in
 * Notion; stretta (il telefono) ogni pagina è una riga, con sotto dove sta e
 * quando la si è vista.
 */
@Composable
fun LibraryScreen(
    repository: PageRepository,
    onBack: () -> Unit,
    onOpenPage: (PageEntity) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.RECENTS) }
    val visits = AppSettings.recentVisits
    val visitedAt = remember(visits) { visits.associate { it.pageId to it.at } }
    val ids = remember(visits) { visits.map { it.pageId } }
    val recent by remember(ids) { repository.observePages(ids) }.collectAsStateWithLifecycle(emptyList())
    val favorites by remember(repository) { repository.observeFavorites() }.collectAsStateWithLifecycle(emptyList())
    val shown = when (tab) {
        LibraryTab.RECENTS -> recent.filter { it.trashedAt == null && it.viewHostId == null }.sortedByDescending { visitedAt[it.id] ?: 0L }
        LibraryTab.FAVORITES -> favorites.sortedByDescending { it.favoritedAt ?: Long.MIN_VALUE }
    }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.library, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 640.dp
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LibraryTabChip(Icons.Filled.Schedule, Strings.recents, tab == LibraryTab.RECENTS) { tab = LibraryTab.RECENTS }
                    LibraryTabChip(Icons.Filled.StarBorder, Strings.favorites, tab == LibraryTab.FAVORITES) { tab = LibraryTab.FAVORITES }
                }
                if (shown.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (tab == LibraryTab.RECENTS) Strings.noRecents else Strings.noFavorites,
                            color = NotionGray400,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (wide) item(key = "header") { LibraryHeader() }
                        items(shown, key = { it.id }) { page ->
                            if (wide) {
                                LibraryTableRow(page, visitedAt[page.id], repository) { onOpenPage(page) }
                            } else {
                                LibraryListRow(page, visitedAt[page.id], repository) { onOpenPage(page) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Una scheda in cima, come le pillole di Notion: piena quella scelta. */
@Composable
private fun LibraryTabChip(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) NotionWhite else NotionGray400
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) DarkSurfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = color, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Le parti delle colonne della tabella: nome, dove sta, ultima modifica, ultima visita. */
private val LIBRARY_COLUMNS = listOf(0.4f, 0.26f, 0.17f, 0.17f)

@Composable
private fun LibraryHeader() {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            listOf(Strings.pageName, Strings.pageSource, Strings.lastEditedTime, Strings.lastVisitedTime).forEachIndexed { i, label ->
                Text(label, color = NotionGray400, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(LIBRARY_COLUMNS[i]))
            }
        }
        HorizontalDivider(color = NotionGray900)
    }
}

/** Dove sta una pagina: quella che la contiene (la pagina principale non sta da nessuna parte). */
@Composable
private fun sourceOf(page: PageEntity, repository: PageRepository): PageEntity? {
    val source by produceState<PageEntity?>(null, page.id) { value = repository.ancestorsOf(page.id).lastOrNull() }
    return source
}

@Composable
private fun PageLabel(page: PageEntity?, modifier: Modifier = Modifier, bold: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (page == null) {
            Text("—", color = NotionGray400, style = MaterialTheme.typography.bodyMedium)
            return@Row
        }
        PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, size = 18.dp, iconSymbol = page.iconSymbol)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            page.title.ifBlank { Strings.untitled }.withEmojiFont(),
            color = NotionWhite,
            style = if (bold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LibraryTableRow(page: PageEntity, visited: Long?, repository: PageRepository, onClick: () -> Unit) {
    val source = sourceOf(page, repository)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageLabel(page, Modifier.weight(LIBRARY_COLUMNS[0]).padding(end = 12.dp), bold = true)
            PageLabel(source, Modifier.weight(LIBRARY_COLUMNS[1]).padding(end = 12.dp))
            Text(timeAgo(page.updatedAt), color = NotionWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(LIBRARY_COLUMNS[2]))
            Text(visited?.let { timeAgo(it) } ?: "—", color = NotionWhite, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(LIBRARY_COLUMNS[3]))
        }
        HorizontalDivider(color = NotionGray900)
    }
}

@Composable
private fun LibraryListRow(page: PageEntity, visited: Long?, repository: PageRepository, onClick: () -> Unit) {
    val source = sourceOf(page, repository)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIconView(emoji = page.icon, iconImage = page.iconImage, isDatabase = page.isDatabase, size = 24.dp, iconSymbol = page.iconSymbol)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                page.title.ifBlank { Strings.untitled }.withEmojiFont(),
                color = NotionWhite,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOfNotNull(source?.title?.ifBlank { Strings.untitled }, visited?.let { timeAgo(it) }).joinToString(" · "),
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * **Quanto tempo fa**, come le date di Notion nella Library: "Just now", "5m
 * ago", "3h ago", "2d ago", "2w ago", e da quattro settimane la data.
 */
fun timeAgo(millis: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = (now - millis).coerceAtLeast(0) / 60_000
    return when {
        minutes < 1 -> Strings.justNow
        minutes < 60 -> Strings.minutesAgo(minutes)
        minutes < 60 * 24 -> Strings.hoursAgo(minutes / 60)
        minutes < 60 * 24 * 7 -> Strings.daysAgo(minutes / (60 * 24))
        minutes < 60 * 24 * 28 -> Strings.weeksAgo(minutes / (60 * 24 * 7))
        else -> Formats.date(millis)
    }
}
