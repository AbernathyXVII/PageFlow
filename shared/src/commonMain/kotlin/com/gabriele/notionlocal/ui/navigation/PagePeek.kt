package com.gabriele.notionlocal.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.platform.BackHandler
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.screen.LockGate
import com.gabriele.notionlocal.ui.screen.OpenModeItems
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.PcBarButton
import com.gabriele.notionlocal.ui.screen.openModeIcon
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

/** Una pagina aperta in un peek: quale, e come (Side peek o Center peek). */
data class PagePeek(val pageId: String, val isDatabase: Boolean, val mode: OpenMode)

/** Chi fa le cose che dal peek escono fuori: cambiare modo, aprire a pagina intera, chiudere. */
class PeekActions(
    val close: () -> Unit,
    /** Un altro modo per la stessa pagina (anche pagina intera o una scheda nuova). */
    val changeMode: (OpenMode) -> Unit,
    /** Una pagina toccata dentro il peek: prende il suo posto. */
    val replace: (pageId: String, isDatabase: Boolean) -> Unit,
    /** Ricerca e cronologia, che vivono a pagina intera. */
    val openSearch: () -> Unit,
    val openUpdates: (String) -> Unit,
    /** Con un modo scelto da "Open as…" dentro il peek: lo decide chi sta fuori. */
    val openOutside: (pageId: String, isDatabase: Boolean, mode: OpenMode) -> Unit
)

/**
 * **Center peek** (dal 12/11/2026, solo sul PC, chiesto dall'utente con la foto
 * di Notion): la pagina in una finestra fissa al centro, sopra tutto, con lo
 * sfondo scurito. Si chiude con un clic fuori o con Esc.
 */
@Composable
fun CenterPeekLayer(peek: PagePeek, factory: ViewModelFactory, actions: PeekActions) {
    BackHandler(enabled = true) { actions.close() }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Un clic sul fondo, fuori dalla finestra, la chiude.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.close() }
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(vertical = CENTER_PEEK_MARGIN)
                .width(min(CENTER_PEEK_WIDTH, maxWidth - CENTER_PEEK_MARGIN * 2))
                .fillMaxHeight(),
            shape = RoundedCornerShape(10.dp),
            color = DarkBackground,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            PeekContent(peek, factory, actions)
        }
    }
}

/**
 * **Side peek** (dal 12/11/2026, solo sul PC): la pagina in un pannello a
 * destra, largo metà dello spazio della pagina finché non lo si allarga o
 * stringe dal bordo sinistro (`AppSettings.sidePeekPercent`). La pagina sotto
 * resta lì e si usa. Si chiude con » o con Esc.
 */
@Composable
fun SidePeekLayer(peek: PagePeek, factory: ViewModelFactory, actions: PeekActions) {
    BackHandler(enabled = true) { actions.close() }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val total = maxWidth
        var percent by remember { mutableFloatStateOf(AppSettings.sidePeekPercent.toFloat()) }
        val density = LocalDensity.current
        Row(modifier = Modifier.align(Alignment.CenterEnd).width(total * (percent / 100f)).fillMaxHeight()) {
            // Il bordo: trascinato a sinistra allarga il pannello, a destra lo stringe.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .horizontalResizeCursor()
                    .draggable(
                        state = rememberDraggableState { delta ->
                            val deltaDp = with(density) { delta.toDp() }
                            percent = (percent - deltaDp / total * 100f)
                                .coerceIn(AppSettings.SIDE_PEEK_MIN_PERCENT.toFloat(), AppSettings.SIDE_PEEK_MAX_PERCENT.toFloat())
                        },
                        orientation = Orientation.Horizontal,
                        onDragStopped = { AppSettings.sidePeekPercent = percent.toInt() }
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(NotionGray900))
            }
            Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground, shadowElevation = 12.dp) {
                PeekContent(peek, factory, actions)
            }
        }
    }
}

/** La pagina (o il database) dentro il peek, con i suoi ViewModel e il lucchetto delle pagine protette. */
@Composable
private fun PeekContent(peek: PagePeek, factory: ViewModelFactory, actions: PeekActions) {
    // ViewModel tutti suoi: chiuso il peek, o cambiata pagina, se ne vanno.
    val owner = remember(peek.pageId) { PeekStoreOwner() }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val current by rememberUpdatedState(actions)
    // Dentro il peek una pagina toccata ne prende il posto; con un modo scelto da "Open as…" decide chi sta fuori.
    val opener = remember {
        PageOpener { id, isDatabase, mode ->
            if (mode == null) current.replace(id, isDatabase) else current.openOutside(id, isDatabase, mode)
        }
    }
    val controls: @Composable RowScope.() -> Unit = { PeekControls(peek.mode, actions) }
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides owner,
        LocalPageOpener provides opener,
        LocalOpenMode provides peek.mode,
        LocalPeekControls provides controls
    ) {
        // Un'altra pagina nello stesso peek: tutto da capo, come aprendola.
        key(peek.pageId) {
        LockGate(peek.pageId, factory.pageRepository, fullScreen = true, isDatabase = peek.isDatabase, onBack = actions.close) {
            if (peek.isDatabase) {
                DatabaseViewScreen(
                    pageId = peek.pageId,
                    factory = factory,
                    onBack = actions.close,
                    onOpenRowPage = { id -> actions.replace(id, false) },
                    onOpenSearch = actions.openSearch,
                    onOpenUpdates = actions.openUpdates,
                    onOpenPage = { id -> actions.replace(id, false) },
                    onOpenDatabase = { id -> actions.replace(id, true) }
                )
            } else {
                PageEditorScreen(
                    pageId = peek.pageId,
                    factory = factory,
                    onBack = actions.close,
                    onOpenSearch = actions.openSearch,
                    onOpenUpdates = actions.openUpdates,
                    onNavigateToPage = { id -> actions.replace(id, false) },
                    onNavigateToDatabase = { id -> actions.replace(id, true) }
                )
            }
        }
        }
    }
}

/**
 * In alto a sinistra del peek, come in Notion: chiudi, apri a pagina intera, e
 * **il pulsante del modo** (chiesto dall'utente il 12/11/2026: "un'icona in
 * alto a sinistra che mi consente di scegliere la visualizzazione") con le
 * quattro scelte.
 */
@Composable
private fun RowScope.PeekControls(mode: OpenMode, actions: PeekActions) {
    PcBarButton(
        if (mode == OpenMode.SIDE_PEEK) Icons.Filled.KeyboardDoubleArrowRight else Icons.Filled.Close,
        EditorStrings.close,
        actions.close
    )
    PcBarButton(Icons.Filled.OpenInFull, Strings.openAsFullPage) { actions.changeMode(OpenMode.FULL_PAGE) }
    var menu by remember { mutableStateOf(false) }
    Box {
        PcBarButton(openModeIcon(mode), Strings.openAs) { menu = true }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            OpenModeItems(current = mode) { picked ->
                menu = false
                if (picked != mode) actions.changeMode(picked)
            }
        }
    }
}

private class PeekStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

private val CENTER_PEEK_WIDTH = 900.dp
private val CENTER_PEEK_MARGIN = 32.dp
