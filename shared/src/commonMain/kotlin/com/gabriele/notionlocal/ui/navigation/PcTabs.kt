package com.gabriele.notionlocal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.screen.PageIconView
import com.gabriele.notionlocal.ui.screen.onRightClick
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Una scheda del PC: la pagina (la rotta) dove sta, e se è fissata a sinistra. */
@Serializable
data class PcTab(val id: String, val route: String = Routes.HOME, val pinned: Boolean = false)

@Serializable
private data class SavedTabs(val tabs: List<PcTab>, val activeId: String)

/**
 * **Le schede del PC** (dal 12/11/2026, chiesto dall'utente "come funzionano i
 * browser"): ognuna con la sua pagina e la sua cronologia (indietro), una sola
 * disegnata alla volta. Le fissate stanno a sinistra, ridotte all'icona della
 * pagina, come in Firefox. Si salvano in `AppSettings.pcTabs` e si riaprono
 * alla volta dopo, ognuna sulla sua pagina.
 */
class PcTabs private constructor(tabs: List<PcTab>, activeId: String) {
    val tabs = mutableStateListOf<PcTab>().apply { addAll(tabs) }
    var activeId by mutableStateOf(activeId)
        private set

    /** La scheda attiva quando si è aperta la finestra: solo lei fa le cose dell'avvio. */
    val launchId: String = activeId

    val active: PcTab get() = tabs.firstOrNull { it.id == activeId } ?: tabs.first()

    fun select(id: String) {
        if (tabs.any { it.id == id }) {
            activeId = id
            save()
        }
    }

    /** Una scheda nuova su [route], accanto a quella attiva (o in fondo), e la si apre. */
    fun open(route: String) {
        val tab = PcTab(newId(), route)
        val at = tabs.indexOfFirst { it.id == activeId }
        val after = if (at < 0 || tabs[at].pinned) tabs.size else at + 1
        tabs.add(after, tab)
        activeId = tab.id
        save()
    }

    /** Chiusa l'ultima, ne resta una sul menu principale, come un browser con una scheda vuota. */
    // shortcut: i ViewModel delle pagine di una scheda chiusa restano in memoria fino alla chiusura
    // dell'app (stanno nel NavControllerViewModel della finestra); da liberare se le schede diventano tante.
    fun close(id: String) {
        val at = tabs.indexOfFirst { it.id == id }
        if (at < 0) return
        if (tabs.size == 1) {
            tabs[0] = PcTab(newId())
            activeId = tabs[0].id
        } else {
            tabs.removeAt(at)
            if (id == activeId) activeId = tabs[(at - 1).coerceAtLeast(0)].id
        }
        save()
    }

    /** Fissata va in fondo alle fissate; sbloccata, in testa alle altre. */
    fun setPinned(id: String, pinned: Boolean) {
        val at = tabs.indexOfFirst { it.id == id }
        if (at < 0) return
        val tab = tabs.removeAt(at).copy(pinned = pinned)
        val lastPinned = tabs.indexOfLast { it.pinned }
        tabs.add(lastPinned + 1, tab)
        save()
    }

    /** La pagina dove è arrivata la scheda: per il nome nella barra e per la volta dopo. */
    fun updateRoute(id: String, route: String) {
        val at = tabs.indexOfFirst { it.id == id }
        if (at < 0 || tabs[at].route == route) return
        tabs[at] = tabs[at].copy(route = route)
        save()
    }

    private fun save() {
        AppSettings.pcTabs = json.encodeToString(SavedTabs(tabs.toList(), activeId))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun newId() = Random.nextLong().toULong().toString(36)

        /** Le schede della volta prima, o una sola sul menu principale. */
        fun load(): PcTabs {
            val saved = AppSettings.pcTabs?.let { runCatching { json.decodeFromString<SavedTabs>(it) }.getOrNull() }
            if (saved == null || saved.tabs.isEmpty()) {
                val first = PcTab(newId())
                return PcTabs(listOf(first), first.id)
            }
            val ordered = saved.tabs.sortedByDescending { it.pinned }
            val active = saved.activeId.takeIf { id -> ordered.any { it.id == id } } ?: ordered.first().id
            return PcTabs(ordered, active)
        }
    }
}

/**
 * **La barra delle schede**, in cima alla finestra del PC: le fissate a
 * sinistra (solo l'icona), poi le altre col nome e la ×, e il + per una nuova
 * sul menu principale. Col destro su una scheda: fissarla o sbloccarla, chiuderla.
 */
@Composable
fun PcTabBar(tabs: PcTabs, repository: PageRepository) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAB_BAR_HEIGHT)
            .background(SidebarBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.tabs.forEach { tab ->
            TabItem(tab, active = tab.id == tabs.activeId, tabs = tabs, repository = repository)
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { tabs.open(Routes.HOME) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = Strings.newTab, tint = NotionGray400, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TabItem(tab: PcTab, active: Boolean, tabs: PcTabs, repository: PageRepository) {
    val pageId = remember(tab.route) { if (tab.route == Routes.HOME) PageEntity.ROOT_PAGE_ID else Routes.pageIdOf(tab.route) }
    val page by remember(pageId) { pageId?.let { repository.observePage(it) } ?: flowOf(null) }.collectAsState(null)
    val title = page?.title?.ifBlank { Strings.untitled } ?: tabLabelOf(tab.route)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .then(if (tab.pinned) Modifier.size(32.dp, 26.dp) else Modifier.widthIn(min = 80.dp, max = 200.dp).height(26.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) DarkBackground else if (hovered) DarkBackground.copy(alpha = 0.5f) else SidebarBackground)
                .hoverable(interaction)
                .onRightClick { menu = true }
                .clickable { tabs.select(tab.id) }
                .padding(horizontal = if (tab.pinned) 0.dp else 8.dp),
            horizontalArrangement = if (tab.pinned) Arrangement.Center else Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageIconView(
                emoji = page?.icon ?: "📄",
                iconImage = page?.iconImage,
                isDatabase = page?.isDatabase == true,
                size = 14.dp,
                iconSymbol = page?.iconSymbol
            )
            if (!tab.pinned) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) NotionWhite else NotionGray400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.weight(1f))
                // La × si vede sulla scheda attiva o col mouse sopra, come nei browser.
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = active || hovered) { tabs.close(tab.id) },
                    contentAlignment = Alignment.Center
                ) {
                    if (active || hovered) {
                        Icon(Icons.Filled.Close, contentDescription = Strings.closeTab, tint = NotionGray400, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (tab.pinned) Strings.unpinTab else Strings.pinTab) },
                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = {
                    menu = false
                    tabs.setPinned(tab.id, !tab.pinned)
                }
            )
            DropdownMenuItem(
                text = { Text(Strings.closeTab) },
                leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = {
                    menu = false
                    tabs.close(tab.id)
                }
            )
        }
    }
}

/** Il nome di una scheda che non sta su una pagina (la ricerca, Settings…). */
private fun tabLabelOf(route: String): String = when (route) {
    Routes.SEARCH -> Strings.search
    Routes.SETTINGS -> Strings.settings
    Routes.TRASH -> Strings.trash
    Routes.FAVORITES -> Strings.favoritePages
    else -> Strings.untitled
}

private val TAB_BAR_HEIGHT = 34.dp
