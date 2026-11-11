package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.unit.Dp
import com.gabriele.notionlocal.ui.theme.pcOr
import com.gabriele.notionlocal.platform.isPc
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.TemplateStrings
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotebookBand
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.theme.SidebarBackground

/**
 * Cosa fare quando si tocca una voce della barra laterale. Un oggetto solo
 * invece di dieci parametri in fila: chi apre la barra (la navigazione)
 * li passa tutti insieme, e un ordine sbagliato fra dieci lambda uguali
 * non lo segnalerebbe nessuno.
 */
/**
 * **La barra laterale è fissata accanto alla pagina** (sul PC, vedi
 * `AppSettings.sidebarPinned`): allora le pagine non mostrano il pulsante
 * per aprirla.
 */
val LocalSidebarPinned = compositionLocalOf { false }

class SidebarActions(
    val onMainMenu: () -> Unit,
    val onFavorites: () -> Unit,
    val onOpenNode: (PageTreeNode) -> Unit,
    val onToggleNode: (PageTreeNode) -> Unit,
    val onSearch: () -> Unit,
    /** "Templates", sotto "Search" (dal 26/09/2026). */
    val onTemplates: () -> Unit,
    val onTrash: () -> Unit,
    val onSettings: () -> Unit,
    /** "Backup" (dal 28/10/2026). */
    val onBackup: () -> Unit = {},
    /** "Connections": la sincronizzazione Wi‑Fi (dal 10/11/2026; prima la voce era grigia). */
    val onConnections: () -> Unit = {}
)

/**
 * La barra laterale, nell'ordine chiesto: il menu principale in cima, e
 * subito sotto i preferiti e l'albero delle pagine (dal 11/11/2026, chiesto
 * dall'utente; prima stavano sotto i widget), poi i quaderni, i widget e le
 * voci di servizio. Scorre tutta insieme: con l'albero aperto le voci sotto
 * scendono, come nella barra di Notion.
 *
 * Le voci ancora da costruire ci sono già, grigie e ferme: dicono cosa
 * arriverà senza far finta di funzionare.
 */
@Composable
fun SidebarContent(
    root: PageTreeNode?,
    children: Map<String, List<PageTreeNode>>,
    expanded: Set<String>,
    currentPageId: String?,
    actions: SidebarActions,
    /** La barra è aperta: solo allora gli orologi dei widget si aggiornano ogni secondo. */
    widgetsActive: Boolean = true,
    /** La voce "Notebooks" coi quaderni: vedi `NotebooksSidebarSection`. */
    notebooks: @Composable () -> Unit = {},
    /** A destra di "Main menu": sul PC il pulsante per fissarla o richiuderla (vedi `AppNavHost`). */
    header: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SidebarBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // 1. Il menu principale, **in cima a tutto** (chiesto dall'utente
        // l'11/11/2026), e sul PC accanto il pulsante per fissare la barra.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            SidebarItem(Icons.Filled.Home, Strings.mainMenu, onClick = actions.onMainMenu, modifier = Modifier.weight(1f))
            header()
        }
        HorizontalDivider(
            color = NotionGray900,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // 2. Pagine preferite, subito sotto il menu principale (chiesto dall'utente l'11/11/2026).
        SidebarItem(Icons.Filled.Star, Strings.favoritePages, onClick = actions.onFavorites)
        // E un divisore prima delle pagine (chiesto dall'utente l'11/11/2026).
        HorizontalDivider(
            color = NotionGray900,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // 3. L'albero delle pagine, a partire da quella principale.
        SidebarSectionTitle(Strings.pages)
        if (root != null) {
            flattenTree(root, children, expanded).forEach { row ->
                TreeRow(
                    row = row,
                    isCurrent = row.node.pageId != null && row.node.pageId == currentPageId,
                    // Sulla pagina principale niente fondo grigio (chiesto dall'utente l'11/11/2026).
                    currentBackground = row.depth > 0,
                    isExpanded = row.node.pageId in expanded,
                    onOpen = { actions.onOpenNode(row.node) },
                    onToggle = { actions.onToggleNode(row.node) }
                )
                val id = row.node.pageId
                if (id != null && id in expanded && row.node.hasChildren && children[id]?.isEmpty() == true) {
                    Text(
                        Strings.noPagesInside,
                        color = NotionGray400,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(
                            start = TREE_START + TREE_INDENT * (row.depth + 1) + TREE_ARROW + 4.dp,
                            top = 4.dp,
                            bottom = 4.dp
                        )
                    )
                }
            }
        }
        // Un divisore sotto l'albero: senza, l'ultima pagina e quello che segue
        // sembravano la stessa lista (richiesta dell'utente del 25/09/2026).
        HorizontalDivider(
            color = NotionGray900,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 4. I quaderni (dal 26/09/2026, prima era una fascia grigia ferma):
        // la stessa fascia di un altro colore, staccata dal resto, e sotto
        // i quaderni come in OneNote. Un mondo a parte dal menu principale.
        // Sopra i widget: chiesto dall'utente il 28/09/2026.
        notebooks()
        // Un divisore sotto i quaderni (chiesto dall'utente il 26/09/2026):
        // senza, l'ultima riga dei quaderni e quello che segue sembravano
        // la stessa lista.
        HorizontalDivider(
            color = NotionGray900,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 5. Widget: fusi orari, avanzamento, contatore, pomodoro, con la
        // freccetta per nasconderli tutti. Vedi `WidgetsSection`.
        WidgetsSection(active = widgetsActive)
        // Lo stesso divisore sotto i widget, prima delle voci di servizio.
        HorizontalDivider(
            color = NotionGray900,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 6. Le voci di servizio.
        SidebarItem(Icons.Filled.Search, Strings.search, onClick = actions.onSearch)
        // "Startup window" sta in Settings, sopra "Title position" (dal
        // 03/11/2026, chiesto dall'utente).
        SidebarItem(Icons.Outlined.Description, TemplateStrings.templates, onClick = actions.onTemplates)
        SidebarItem(Icons.Filled.Backup, Strings.backup, onClick = actions.onBackup)
        // "Notifications" non c'è più (dal 05/10/2026, chiesto dall'utente):
        // le sue scelte stanno in Settings, vedi `NotificationSettingsSection`.
        SidebarItem(Icons.Filled.Delete, Strings.trash, onClick = actions.onTrash)
        // "Import" ed "Export" grigi non ci sono più (dal 03/11/2026, chiesto
        // dall'utente): l'export si fa dai tre puntini di una pagina o di un
        // database, vedi `PageExportSheet`.
        SidebarItem(Icons.Filled.Devices, Strings.connections, onClick = actions.onConnections)
        SidebarItem(Icons.Filled.Settings, Strings.settings, onClick = actions.onSettings)
    }
}

// Sul PC più piccole, come in Notion (vedi `pcOr`).
private val ITEM_ICON: Dp get() = pcOr(18.dp, 22.dp)
val TREE_START = 12.dp
val TREE_INDENT = 16.dp
val TREE_ARROW: Dp get() = pcOr(20.dp, 24.dp)
private const val DISABLED_ROW_ALPHA = 0.35f

/** Il grigio delle voci spente: lo stesso ovunque nella barra. */
private val DisabledTint: Color get() = NotionGray400.copy(alpha = 0.55f)

@Composable
private fun SidebarSectionTitle(text: String) {
    Text(
        text = text,
        color = NotionGray400,
        // Sul PC piccole come "Recents" e "Private" di Notion (dal 11/11/2026, vedi `PC_TEXT_SCALE`).
        style = if (isPc) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = pcOr(14.dp, 20.dp), end = 20.dp, top = pcOr(12.dp, 18.dp), bottom = pcOr(4.dp, 6.dp))
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tint = if (enabled) NotionWhite else DisabledTint
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = pcOr(14.dp, 20.dp), vertical = pcOr(5.dp, 11.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(ITEM_ICON))
        Spacer(modifier = Modifier.width(pcOr(10.dp, 16.dp)))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Una riga dell'albero già messa in fila, con quanto è rientrata.
 *
 * Questa, `flattenTree` e `TreeRow` servono anche alla scelta della pagina
 * in cui duplicarne un'altra (`DuplicateSheet`): lo stesso albero, così
 * le pagine si ritrovano dove si è abituati a vederle.
 */
class FlatTreeRow(val node: PageTreeNode, val depth: Int)

/**
 * L'albero messo in fila, rami aperti compresi.
 *
 * Una pagina che richiama una delle pagine sopra di lei (succede: basta
 * un collegamento all'indietro) si mostra ma non si riapre, altrimenti la
 * fila non finirebbe mai.
 */
fun flattenTree(
    root: PageTreeNode,
    children: Map<String, List<PageTreeNode>>,
    expanded: Set<String>
): List<FlatTreeRow> {
    val rows = mutableListOf<FlatTreeRow>()
    fun visit(node: PageTreeNode, depth: Int, ancestors: Set<String>) {
        rows += FlatTreeRow(node, depth)
        val id = node.pageId ?: return
        if (id !in expanded || id in ancestors) return
        children[id]?.forEach { visit(it, depth + 1, ancestors + id) }
    }
    visit(root, 0, emptySet())
    return rows
}

/**
 * Una riga dell'albero: il triangolino (solo se dentro c'è qualcosa, come
 * nelle cartelle di Esplora file), l'icona della pagina e il titolo.
 * Il triangolino apre e chiude; il resto della riga porta alla pagina.
 */
@Composable
fun TreeRow(
    row: FlatTreeRow,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    /**
     * Spenta: si vede, grigia, ma non si tocca e non si apre. Serve a
     * "Move to", dove la pagina da spostare resta al suo posto nell'albero
     * — ritrovarla aiuta a orientarsi — ma non può essere la destinazione,
     * e nemmeno quello che ha dentro.
     */
    enabled: Boolean = true,
    /** Il fondo grigio della pagina aperta (sulla pagina principale no, vedi `SidebarContent`). */
    currentBackground: Boolean = true
) {
    val node = row.node
    val canExpand = enabled && node.hasChildren
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isCurrent && currentBackground) DarkSurfaceVariant else Color.Transparent)
            .clickable(enabled = enabled, onClick = onOpen)
            .padding(start = TREE_START - 8.dp + TREE_INDENT * row.depth, end = 8.dp)
            .height(pcOr(28.dp, 36.dp))
            .alpha(if (enabled) 1f else DISABLED_ROW_ALPHA),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(TREE_ARROW)
                .clip(RoundedCornerShape(4.dp))
                .clickable(enabled = canExpand, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (canExpand) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ArrowDropDown else Icons.Filled.ArrowRight,
                    contentDescription = if (isExpanded) Strings.collapse else Strings.expand,
                    tint = NotionGray400
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        PageIconView(emoji = node.emoji, iconImage = node.iconImage, isDatabase = node.isDatabase, size = pcOr(16.dp, 18.dp), iconSymbol = node.iconSymbol)
        Spacer(modifier = Modifier.width(pcOr(6.dp, 8.dp)))
        Text(
            text = node.title.ifBlank { Strings.untitled },
            color = NotionWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Serve ai `WindowInsets` vuoti del foglio della barra: i margini li mette già il contenuto. */
val NoInsets = WindowInsets(0, 0, 0, 0)
