package com.gabriele.notionlocal.ui.navigation

import kotlin.math.abs
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.key
import androidx.compose.foundation.gestures.detectDragGestures
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
class PcTabs private constructor(
    tabs: List<PcTab>,
    activeId: String,
    /** La scheda attiva quando si è aperta la finestra: solo lei fa le cose dell'avvio (null in una finestra staccata). */
    val launchId: String?,
    persist: Boolean
) {
    val tabs = mutableStateListOf<PcTab>().apply { addAll(tabs) }
    var activeId by mutableStateOf(activeId)
        private set

    /**
     * **Queste schede si ritrovano alla volta dopo** (dal 13/11/2026): quelle
     * della finestra principale sì, quelle di una finestra staccata no; chiusa
     * la principale, le prende la finestra che resta (vedi `Main.kt` del PC).
     */
    var persist: Boolean = persist
        set(value) {
            field = value
            if (value) save()
        }

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

    /**
     * **Sposta una scheda al posto [to]** (dal 13/11/2026, chiesto dall'utente:
     * trascinandola, come nei browser). Le fissate solo fra le fissate, le altre
     * solo fra le altre: false se il posto è dell'altro gruppo.
     */
    fun move(id: String, to: Int): Boolean {
        val from = tabs.indexOfFirst { it.id == id }
        if (from < 0 || to !in tabs.indices || to == from || tabs[to].pinned != tabs[from].pinned) return false
        tabs.add(to, tabs.removeAt(from))
        save()
        return true
    }

    /**
     * **Toglie una scheda per portarla in una finestra nuova** (dal 13/11/2026):
     * non una fissata, e non l'ultima (con una sola scheda la barra non c'è).
     */
    fun detach(id: String): PcTab? {
        val at = tabs.indexOfFirst { it.id == id }
        if (at < 0 || tabs[at].pinned || tabs.size < 2) return null
        val tab = tabs.removeAt(at)
        if (id == activeId) activeId = tabs[(at - 1).coerceAtLeast(0)].id
        save()
        return tab
    }

    /** La pagina dove è arrivata la scheda: per il nome nella barra e per la volta dopo. */
    fun updateRoute(id: String, route: String) {
        val at = tabs.indexOfFirst { it.id == id }
        if (at < 0 || tabs[at].route == route) return
        tabs[at] = tabs[at].copy(route = route)
        save()
    }

    private fun save() {
        if (persist) AppSettings.pcTabs = json.encodeToString(SavedTabs(tabs.toList(), activeId))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun newId() = Random.nextLong().toULong().toString(36)

        /** Le schede della volta prima, o una sola sul menu principale. */
        fun load(): PcTabs {
            val saved = AppSettings.pcTabs?.let { runCatching { json.decodeFromString<SavedTabs>(it) }.getOrNull() }
            if (saved == null || saved.tabs.isEmpty()) {
                val first = PcTab(newId())
                return PcTabs(listOf(first), first.id, launchId = first.id, persist = true)
            }
            val ordered = saved.tabs.sortedByDescending { it.pinned }
            val active = saved.activeId.takeIf { id -> ordered.any { it.id == id } } ?: ordered.first().id
            return PcTabs(ordered, active, launchId = active, persist = true)
        }

        /** Una finestra staccata, con la sola [tab]: niente avvio, e non si ricorda (vedi [persist]). */
        fun detached(tab: PcTab): PcTabs = PcTabs(listOf(tab), tab.id, launchId = null, persist = false)
    }
}

/**
 * **La finestra del PC** di una barra di schede (dal 13/11/2026): una scheda
 * trascinata fuori dalla barra si apre in una finestra nuova, come nei browser
 * e in Esplora file. Null dove non ci sono finestre (telefono, test).
 */
fun interface PcWindowActions {
    /** Apre [tab] in una finestra nuova, dove il mouse l'ha lasciata ([pointerInWindow], px di questa finestra). */
    fun openInNewWindow(tab: PcTab, pointerInWindow: Offset)
}

val LocalPcWindow = staticCompositionLocalOf<PcWindowActions?> { null }

/** La scheda che si sta trascinando: di quanto si è spostata dal suo posto, e se è fuori dalla barra. */
private class TabDrag {
    var id by mutableStateOf<String?>(null)
    var dx by mutableFloatStateOf(0f)
    var dy by mutableFloatStateOf(0f)
    var detached by mutableStateOf(false)
    /** Dove sta il mouse, in px della finestra: lì si apre la finestra nuova. */
    var pointer = Offset.Zero
    /** Le larghezze delle schede: con loro e l'ordine si sa dove sta ognuna, anche prima che la barra si rifaccia. */
    val widths = HashMap<String, Int>()
    val coordinates = HashMap<String, LayoutCoordinates>()

    fun stop() {
        id = null
        dx = 0f
        dy = 0f
        detached = false
    }
}

/**
 * **La barra delle schede**, in cima alla finestra del PC: le fissate a
 * sinistra (solo l'icona), poi le altre col nome e la ×, e il + per una nuova
 * sul menu principale. Col destro su una scheda: fissarla o sbloccarla, chiuderla.
 *
 * **Trascinandole** (dal 13/11/2026, chiesto dall'utente): una scheda si sposta
 * fra le altre, le fissate solo fra le fissate; portata fuori dalla barra (giù
 * o su) e lasciata, si apre in una finestra nuova. Le fissate non si staccano.
 */
@Composable
fun PcTabBar(tabs: PcTabs, repository: PageRepository) {
    val window = LocalPcWindow.current
    val drag = remember(tabs) { TabDrag() }
    val density = LocalDensity.current
    val gap = with(density) { TAB_GAP.toPx() }
    val tearDistance = with(density) { TEAR_OFF_DISTANCE.toPx() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TAB_BAR_HEIGHT)
            .background(SidebarBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(TAB_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.tabs.forEach { tab ->
            key(tab.id) {
                val dragged = drag.id == tab.id
                TabItem(
                    tab,
                    active = tab.id == tabs.activeId,
                    tabs = tabs,
                    repository = repository,
                    modifier = Modifier
                        .onGloballyPositioned {
                            drag.widths[tab.id] = it.size.width
                            drag.coordinates[tab.id] = it
                        }
                        .pointerInput(tab.id, window) {
                            detectDragGestures(
                                onDragStart = { drag.id = tab.id },
                                onDragCancel = { drag.stop() },
                                onDragEnd = {
                                    if (drag.detached) {
                                        val pointer = drag.pointer
                                        tabs.detach(tab.id)?.let { window?.openInNewWindow(it, pointer) }
                                    }
                                    drag.stop()
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    drag.dx += amount.x
                                    drag.dy += amount.y
                                    drag.coordinates[tab.id]?.takeIf { it.isAttached }?.let { drag.pointer = it.localToWindow(change.position) }
                                    drag.detached = window != null && !tab.pinned && tabs.tabs.size > 1 && abs(drag.dy) > tearDistance
                                    if (!drag.detached) drag.dx += tabShift(tabs, tab, drag, gap)
                                }
                            )
                        }
                        .zIndex(if (dragged) 1f else 0f)
                        .graphicsLayer {
                            if (dragged) {
                                translationX = drag.dx
                                translationY = if (drag.detached) drag.dy else 0f
                                alpha = if (drag.detached) 0.7f else 1f
                            }
                        }
                )
            }
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

/**
 * Se la scheda trascinata ha passato la metà di quella accanto (dello stesso
 * gruppo), le scambia di posto, e dice di quanto correggere lo spostamento
 * perché resti sotto il mouse; 0 se non cambia niente.
 */
private fun tabShift(tabs: PcTabs, tab: PcTab, drag: TabDrag, gap: Float): Float {
    val index = tabs.tabs.indexOfFirst { it.id == tab.id }
    if (index < 0) return 0f
    fun width(i: Int) = (drag.widths[tabs.tabs[i].id] ?: 0).toFloat()
    fun left(i: Int) = (0 until i).sumOf { width(it).toDouble() }.toFloat() + i * gap
    val center = left(index) + width(index) / 2f + drag.dx
    for (other in listOf(index + 1, index - 1)) {
        if (other !in tabs.tabs.indices) continue
        val passed = width(other)
        val otherCenter = left(other) + passed / 2f
        val crossed = if (other > index) center > otherCenter else center < otherCenter
        // Passata quella accanto, il suo posto è più in là della sua larghezza (e dello spazio fra le due).
        if (crossed && tabs.move(tab.id, other)) return if (other > index) -(passed + gap) else passed + gap
    }
    return 0f
}

@Composable
private fun TabItem(tab: PcTab, active: Boolean, tabs: PcTabs, repository: PageRepository, modifier: Modifier = Modifier) {
    val pageId = remember(tab.route) { if (tab.route == Routes.HOME) PageEntity.ROOT_PAGE_ID else Routes.pageIdOf(tab.route) }
    val page by remember(pageId) { pageId?.let { repository.observePage(it) } ?: flowOf(null) }.collectAsState(null)
    val title = page?.title?.ifBlank { Strings.untitled } ?: tabLabelOf(tab.route)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var menu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
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
private val TAB_GAP = 4.dp

/** Quanto sotto (o sopra) la barra va portata una scheda perché si stacchi in una finestra nuova. */
private val TEAR_OFF_DISTANCE = 40.dp
