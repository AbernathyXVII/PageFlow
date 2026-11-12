package com.gabriele.notionlocal.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.ui.screen.SidebarPageActions
import com.gabriele.notionlocal.platform.isPc
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.gabriele.notionlocal.ui.theme.SidebarPinCircle
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import com.gabriele.notionlocal.platform.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import com.gabriele.notionlocal.ui.screen.LocalSidebarPinned
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import kotlin.math.roundToInt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.platform.LocalPlatformContext
import kotlinx.coroutines.flow.collectLatest
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.notify.NotificationTarget
import com.gabriele.notionlocal.data.settings.AnimationLevel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.share.QuickNotes
import com.gabriele.notionlocal.ui.i18n.ShareStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.data.settings.StartupMode
import com.gabriele.notionlocal.data.settings.TrashPolicy
import com.gabriele.notionlocal.ui.screen.BackupScreen
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.screen.LockGate
import com.gabriele.notionlocal.ui.screen.NotebookSectionScreen
import com.gabriele.notionlocal.ui.screen.NotebookScreen
import com.gabriele.notionlocal.ui.screen.PatchNoteScreen
import com.gabriele.notionlocal.ui.screen.PatchNotesScreen
import com.gabriele.notionlocal.ui.screen.NotebooksSidebarSection
import com.gabriele.notionlocal.ui.i18n.NotebookStrings
import com.gabriele.notionlocal.viewmodel.NotebooksViewModel
import androidx.compose.runtime.produceState
import com.gabriele.notionlocal.ui.screen.FavoritesScreen
import com.gabriele.notionlocal.ui.screen.NoInsets
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.ConnectionsScreen
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.ui.screen.SyncPairingDialog
import com.gabriele.notionlocal.ui.screen.syncSummary
import com.gabriele.notionlocal.data.sync.WifiSync
import com.gabriele.notionlocal.data.sync.syncSide
import com.gabriele.notionlocal.data.sync.syncWaitsForPhone
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.screen.SearchScreen
import com.gabriele.notionlocal.ui.screen.SidebarActions
import com.gabriele.notionlocal.ui.screen.SidebarContent
import com.gabriele.notionlocal.ui.screen.StartupScreen
import com.gabriele.notionlocal.ui.screen.TemplatesScreen
import com.gabriele.notionlocal.ui.screen.TrashScreen
import com.gabriele.notionlocal.ui.screen.UpdatesScreen
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import com.gabriele.notionlocal.viewmodel.SidebarViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import androidx.savedstate.read
import androidx.compose.runtime.DisposableEffect

/**
 * Grafo di navigazione dell'app. Non esiste più una schermata Home
 * separata: la rotta HOME apre semplicemente PageEditorScreen puntata
 * sulla pagina radice fissa (PageEntity.ROOT_PAGE_ID) — creata al volo
 * al primo avvio.
 *
 * **La barra laterale sta sopra a tutto**, non dentro una schermata: si
 * apre dal pulsante con le tre lineette di ogni pagina e scorre da
 * sinistra sopra quella aperta. Toccando una pagina nella barra ci si va
 * dritti, **senza passare dalle pagine che la contengono**: sotto resta
 * solo il menu principale, così "indietro" riporta lì e non a una fila di
 * pagine mai aperte davvero.
 */
@Composable
fun AppNavHost(
    factory: ViewModelFactory,
    /** Una nota al volo chiesta a questa finestra (vedi `QuickNotes`); di suo quella dei test. */
    quickNoteRequest: MutableState<Boolean> = QuickNotes.requested
) {
    if (!isPc) {
        AppTab(factory, quickNoteRequest, rememberNavController())
        return
    }
    // **Le schede del PC** (dal 12/11/2026, vedi `PcTabs`): la barra in cima,
    // e sotto la scheda attiva con la sua cronologia. Le altre tengono la loro
    // finché non le si riapre (`SaveableStateProvider`).
    val tabs = remember { PcTabs.load() }
    // Ctrl+T e Ctrl+W, come nei browser (dal 12/11/2026, vedi `AppShortcuts`).
    DisposableEffect(tabs) {
        AppShortcuts.newTab = { tabs.open(Routes.HOME) }
        AppShortcuts.closeTab = { tabs.close(tabs.activeId) }
        onDispose {
            AppShortcuts.newTab = null
            AppShortcuts.closeTab = null
        }
    }
    val holder = rememberSaveableStateHolder()
    Column(modifier = Modifier.fillMaxSize()) {
        // Con una scheda sola la barra non c'è (chiesto dall'utente il 12/11/2026): se ne apre
        // un'altra con Ctrl+T o con "Open as…" → New tab, e allora compare.
        if (tabs.tabs.size > 1) PcTabBar(tabs, factory.pageRepository)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val tab = tabs.active
            key(tab.id) {
                holder.SaveableStateProvider(tab.id) {
                    AppTab(
                        factory = factory,
                        quickNoteRequest = quickNoteRequest,
                        navController = rememberNavController(),
                        runStartup = tab.id == tabs.launchId,
                        startRoute = tab.route,
                        onRoute = { route -> tabs.updateRoute(tab.id, route) },
                        onOpenNewTab = { route -> tabs.open(route) }
                    )
                }
            }
        }
    }
}

/**
 * **Una scheda**: la barra laterale e le pagine, con la sua cronologia. Sul
 * telefono ce n'è una sola. [runStartup]: solo la prima scheda della finestra
 * fa le cose dell'avvio (cestino, pagina d'avvio); le altre si aprono su
 * [startRoute]. [onRoute] dice dove è arrivata, [onOpenNewTab] apre una pagina
 * in una scheda nuova (null = non ci sono schede).
 */
@Composable
private fun AppTab(
    factory: ViewModelFactory,
    quickNoteRequest: MutableState<Boolean>,
    navController: NavHostController,
    runStartup: Boolean = true,
    startRoute: String? = null,
    onRoute: ((String) -> Unit)? = null,
    onOpenNewTab: ((String) -> Unit)? = null
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sidebar: SidebarViewModel = viewModel(factory = factory)
    val treeRoot by sidebar.root.collectAsStateWithLifecycle()
    val treeChildren by sidebar.children.collectAsStateWithLifecycle()
    val treeExpanded by sidebar.expanded.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val animations = AppSettings.animations
    val notebooks: NotebooksViewModel = viewModel(factory = factory)

    // La pagina aperta adesso, per evidenziarla nell'albero.
    val currentPageId = when (backStackEntry?.destination?.route) {
        Routes.HOME -> PageEntity.ROOT_PAGE_ID
        Routes.PAGE_EDITOR, Routes.DATABASE_VIEW -> backStackEntry?.arguments?.read { getStringOrNull("pageId") }
        else -> null
    }
    // La sezione di un quaderno aperta adesso, o quella della pagina aperta:
    // la barra laterale la evidenzia fra i quaderni.
    val currentSectionId by produceState<String?>(null, backStackEntry) {
        value = when (backStackEntry?.destination?.route) {
            Routes.NOTEBOOK_SECTION -> backStackEntry?.arguments?.read { getStringOrNull("sectionId") }
            Routes.PAGE_EDITOR -> currentPageId?.let { factory.pageRepository.getPage(it)?.notebookSectionId }
            else -> null
        }
    }
    val currentNotebookId = backStackEntry?.takeIf { it.destination.route == Routes.NOTEBOOK }
        ?.arguments?.read { getStringOrNull("notebookId") }

    // Sul PC la barra si può fissare accanto alla pagina (vedi `AppSettings.sidebarPinned`).
    val pinned = canPinSidebar && AppSettings.sidebarPinned

    // Fissata, la barra è sempre in vista: l'albero si rilegge quando cambiano pagine e righe
    // (aperta e chiusa, lo rilegge `openSidebar`), poco dopo l'ultima modifica.
    if (pinned) {
        val context = LocalPlatformContext.current
        LaunchedEffect(Unit) {
            AppDatabase.getInstance(context).invalidationTracker.createFlow("pages", "database_rows", "database_cells").collectLatest {
                delay(PINNED_REFRESH_DELAY_MS)
                sidebar.refresh()
            }
        }
    }

    fun openSidebar() {
        if (pinned) return
        sidebar.refresh()
        scope.launch {
            if (animations == AnimationLevel.OFF) drawerState.snapTo(DrawerValue.Open) else drawerState.open()
        }
    }

    fun closeSidebar() {
        scope.launch {
            if (animations == AnimationLevel.OFF) drawerState.snapTo(DrawerValue.Closed) else drawerState.close()
        }
    }

    /** Dritti a una pagina, con sotto solo il menu principale. */
    fun jumpTo(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
        }
    }

    fun goHome() {
        navController.popBackStack(Routes.HOME, inclusive = false)
    }

    // --- Come si apre una pagina toccata (sul PC, dal 12/11/2026, vedi `PageOpener`) ---
    //
    // Un collegamento o una riga di un database: nel modo di Settings (di
    // partenza Center peek) o in quello scelto da "Open as…". Il peek è di
    // questa finestra: si chiude cambiando pagina.
    var peek by remember { mutableStateOf<PagePeek?>(null) }
    fun openWith(pageId: String, isDatabase: Boolean, mode: OpenMode) {
        when (mode) {
            OpenMode.SIDE_PEEK, OpenMode.CENTER_PEEK -> peek = PagePeek(pageId, isDatabase, mode)
            OpenMode.NEW_TAB -> {
                peek = null
                onOpenNewTab?.invoke(Routes.forPage(pageId, isDatabase)) ?: navController.navigate(Routes.forPage(pageId, isDatabase))
            }
            OpenMode.FULL_PAGE -> {
                peek = null
                navController.navigate(Routes.forPage(pageId, isDatabase))
            }
        }
    }
    val pageOpener: PageOpener? = if (isPc) {
        remember(navController) { PageOpener { id, isDatabase, mode -> openWith(id, isDatabase, mode ?: AppSettings.openPagesAs) } }
    } else {
        null
    }
    val peekActions = PeekActions(
        close = { peek = null },
        changeMode = { mode -> peek?.let { open -> openWith(open.pageId, open.isDatabase, mode) } },
        replace = { id, isDatabase -> peek = peek?.copy(pageId = id, isDatabase = isDatabase) },
        openSearch = {
            peek = null
            navController.navigate(Routes.SEARCH)
        },
        openUpdates = { id ->
            peek = null
            navController.navigate(Routes.updates(id))
        },
        openOutside = { id, isDatabase, mode -> openWith(id, isDatabase, mode) }
    )
    // Cambiata la pagina sotto, il peek se ne va.
    LaunchedEffect(backStackEntry?.id) { peek = null }

    // --- All'avvio: pulizia del cestino e pagina da cui partire ---
    //
    // Una volta sola per avvio vero: il flag sopravvive a una rotazione o a
    // un cambio di tema di sistema, che ricreano tutto questo da capo — e
    // senza, ogni volta si ricomincerebbe dalla pagina d'avvio.
    var startupHandled by rememberSaveable { mutableStateOf(false) }
    // Letta subito, prima che il registro dell'ultima pagina la sovrascriva
    // col menu principale da cui si parte.
    val lastRouteAtLaunch = remember { AppSettings.lastVisitedRoute }
    // Aperta toccando una notifica: si va lì, non alla pagina d'avvio.
    val launchedFromNotification = remember { NotificationTarget.pending.value != null }
    // Aperta per una nota al volo: si va lì, non alla pagina d'avvio.
    val launchedForQuickNote = remember { quickNoteRequest.value }
    LaunchedEffect(Unit) {
        if (startupHandled) return@LaunchedEffect
        startupHandled = true
        val repository = factory.pageRepository
        // Una scheda che non è la prima: la sua pagina, se c'è ancora, e niente avvio.
        if (!runStartup) {
            val route = startRoute?.takeIf { it != Routes.HOME } ?: return@LaunchedEffect
            val pageId = Routes.pageIdOf(route)
            val alive = pageId == null || repository.getPage(pageId)?.takeIf { it.trashedAt == null } != null
            // Dal thread dello schermo: dopo il database non ci si torna per forza (vedi la nota al volo).
            if (alive) withContext(Dispatchers.Main.immediate) { navController.navigate(route) }
            return@LaunchedEffect
        }
        repository.purgeTrash(
            emptyAll = AppSettings.trashPolicy == TrashPolicy.EMPTY_AUTOMATICALLY,
            retentionDays = AppSettings.TRASH_RETENTION_DAYS
        ).forEach { factory.imageStore.delete(it) }
        // Dopo la pulizia, così non se ne vanno subito: le pagine rimaste da
        // righe cancellate vanno nel cestino (vedi `tidyOrphanRowPages`).
        repository.tidyOrphanRowPages()

        if (launchedFromNotification || launchedForQuickNote) return@LaunchedEffect
        val targetId = when (AppSettings.startupMode) {
            StartupMode.HOME -> null
            StartupMode.LAST_VISITED -> lastRouteAtLaunch?.let { Routes.pageIdOf(it) }
            StartupMode.SPECIFIC_PAGE -> AppSettings.startupPageId
        }
        // Solo se la pagina c'è ancora e non sta nel cestino: altrimenti si
        // resta sul menu principale, che è meglio di una pagina vuota.
        val target = targetId
            ?.takeIf { it != PageEntity.ROOT_PAGE_ID }
            ?.let { repository.getPage(it) }
            ?.takeIf { it.trashedAt == null }
        if (target != null) navController.navigate(Routes.forPage(target.id, target.isDatabase))
    }

    // --- Toccata una notifica: la sua pagina ---
    //
    // Quella della riga di database per una data, quella con la riga
    // "/notification" per un promemoria. Una riga di un database semplice,
    // che non ha una pagina, apre il database con la sua scheda (dal
    // 30/09/2026, vedi `NotificationTarget.rowToShow`); una pagina sparita o
    // nel cestino non apre niente.
    val openRequest by NotificationTarget.pending
    LaunchedEffect(openRequest) {
        val request = openRequest ?: return@LaunchedEffect
        NotificationTarget.pending.value = null
        val repository = factory.pageRepository
        suspend fun routeOf(pageId: String): String? = when (pageId) {
            PageEntity.ROOT_PAGE_ID -> Routes.HOME
            else -> repository.getPage(pageId)?.takeIf { it.trashedAt == null }?.let { Routes.forPage(it.id, it.isDatabase) }
        }
        val rowPage = request.rowId?.let { rowId -> repository.ensureRowPage(rowId)?.let(Routes::pageEditor) }
        if (rowPage == null && request.rowId != null) NotificationTarget.rowToShow.value = request.rowId
        val route = rowPage
            ?: request.databaseId?.let { routeOf(it) }
            ?: request.pageId?.let { routeOf(it) }
            ?: return@LaunchedEffect
        closeSidebar()
        if (route == Routes.HOME) goHome() else jumpTo(route)
    }

    // --- Una nota al volo (dal 02/11/2026, vedi `QuickNotes`) ---
    //
    // Dal tasto delle impostazioni rapide, dal widget o dalla scorciatoia
    // dell'icona: una pagina nuova nella pagina di "Save to" (Inbox se non se
    // ne è scelta una), e dentro, col cursore sulla prima riga. Lasciata,
    // vuota se ne va e senza titolo prende la prima riga.
    // Ascoltata e non usata come chiave: rimettendola a "falso" un effetto con
    // la richiesta per chiave ripartiva da capo, e si fermava dopo aver creato
    // la nota ma prima di entrarci (provato sul telefono il 02/11/2026).
    LaunchedEffect(quickNoteRequest) {
        snapshotFlow { quickNoteRequest.value }.collect { requested ->
            if (!requested) return@collect
            quickNoteRequest.value = false
            val repository = factory.pageRepository
            val chosen = AppSettings.sharePageId
            // Il database su IO; lo stato e la navigazione dal thread dello schermo,
            // dove dopo il database non si torna per forza (vedi `ShareSheet`).
            withContext(Dispatchers.Main.immediate) {
                val (destination, note) = withContext(Dispatchers.IO) {
                    val destination = repository.inboxPageId(chosen, ShareStrings.inbox)
                    destination to repository.createQuickNote(destination)
                }
                AppSettings.sharePageId = destination
                QuickNotes.created(note.first, note.second)
                closeSidebar()
                jumpTo(Routes.pageEditor(note.first))
            }
        }
    }
    LaunchedEffect(navController) {
        navController.currentBackStack.collect { stack ->
            val open = stack.mapNotNullTo(HashSet()) { it.arguments?.read { getStringOrNull("pageId") } }
            for (pageId in QuickNotes.leftBehind(open)) {
                // Dopo un attimo: le ultime lettere scritte devono arrivare.
                scope.launch {
                    delay(QUICK_NOTE_SETTLE_MS)
                    runCatching { factory.pageRepository.finishQuickNote(pageId) }
                        .getOrNull()?.forEach { factory.imageStore.delete(it) }
                }
            }
        }
    }

    // Toccata la notifica di un timer che corre (dal 30/09/2026, vedi
    // `TimerNotifications`): la barra laterale, dove sta il suo widget.
    val openWidgets by NotificationTarget.openWidgets
    LaunchedEffect(openWidgets) {
        if (!openWidgets) return@LaunchedEffect
        NotificationTarget.openWidgets.value = false
        openSidebar()
    }

    // Toccato il promemoria dei backup (dal 28/10/2026): la pagina Backup.
    val openBackup by NotificationTarget.openBackup
    LaunchedEffect(openBackup) {
        if (!openBackup) return@LaunchedEffect
        NotificationTarget.openBackup.value = false
        navController.navigate(Routes.BACKUP) { launchSingleTop = true }
    }

    // L'ultima pagina aperta, per la finestra di avvio "Last visited page".
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            val id = entry.arguments?.read { getStringOrNull("pageId") }
            when (entry.destination.route) {
                Routes.HOME -> AppSettings.lastVisitedRoute = Routes.HOME
                Routes.PAGE_EDITOR -> id?.let { AppSettings.lastVisitedRoute = Routes.pageEditor(it) }
                Routes.DATABASE_VIEW -> id?.let { AppSettings.lastVisitedRoute = Routes.databaseView(it) }
            }
            // La scheda (sul PC) ricorda dove è arrivata: pagine e schermate senza argomenti.
            val route = when (val template = entry.destination.route) {
                Routes.PAGE_EDITOR -> id?.let(Routes::pageEditor)
                Routes.DATABASE_VIEW -> id?.let(Routes::databaseView)
                null -> null
                else -> template.takeIf { '{' !in it }
            }
            if (route != null) onRoute?.invoke(route)
        }
    }

    // **La barra riaperta da Android.** Se l'app viene chiusa con la barra
    // aperta, riaprendola la barra torna aperta da sola, senza passare da
    // `openSidebar`: l'albero delle pagine non veniva letto, e sotto
    // "Pages" non c'era niente. Visto sul telefono il 25/09/2026, provando
    // le notifiche ad app chiusa.
    LaunchedEffect(Unit) {
        if (drawerState.isOpen) sidebar.refresh()
    }

    BackHandler(enabled = drawerState.isOpen) { closeSidebar() }

    // Il codice della sincronizzazione Wi‑Fi, sopra qualunque schermata (vedi `SyncPrompts`).
    SyncPairingDialog()

    // Sul telefono, con un PC abbinato: il tasto "Sincronizza col telefono" del PC fa partire la
    // sincronizzazione anche da qui (vedi `WifiSync.listenForPc`), e com'è andata compare in basso.
    val syncContext = LocalPlatformContext.current
    LaunchedEffect(Unit) {
        if (syncWaitsForPhone) return@LaunchedEffect
        WifiSync.onPcRequestDone = { status ->
            if (status is WifiSync.Status.Done) scope.launch(Dispatchers.Main) { showMessage(syncContext, syncSummary(status.result), long = true) }
        }
        withContext(Dispatchers.IO) {
            val side = syncSide(syncContext)
            if (side.store.load().peers.isNotEmpty()) WifiSync.listenForPc(side)
        }
    }

    // Ctrl+F sul PC (vedi `AppShortcuts`): la ricerca, come dalla barra laterale.
    DisposableEffect(navController) {
        AppShortcuts.openSearch = { closeSidebar(); navController.navigate(Routes.SEARCH) { launchSingleTop = true } }
        // Ctrl+N: una pagina nuova in fondo a quella aperta (o al menu principale), e dentro col
        // cursore, come la nota al volo: lasciata vuota se ne va, senza titolo prende la prima riga.
        AppShortcuts.newPage = {
            val open = navController.currentBackStackEntry?.takeIf { it.destination.route == Routes.PAGE_EDITOR }
                ?.arguments?.read { getStringOrNull("pageId") }
            scope.launch(Dispatchers.Main.immediate) {
                val note = withContext(Dispatchers.IO) { factory.pageRepository.createQuickNote(open ?: PageEntity.ROOT_PAGE_ID) }
                QuickNotes.created(note.first, note.second)
                closeSidebar()
                navController.navigate(Routes.pageEditor(note.first))
            }
        }
        onDispose {
            AppShortcuts.openSearch = null
            AppShortcuts.newPage = null
        }
    }

    val actions = SidebarActions(
        onMainMenu = { closeSidebar(); goHome() },
        onFavorites = { closeSidebar(); navController.navigate(Routes.FAVORITES) { launchSingleTop = true } },
        onOpenNode = { node ->
            closeSidebar()
            val pageId = node.pageId
            val rowId = node.rowId
            when {
                pageId == PageEntity.ROOT_PAGE_ID -> goHome()
                pageId != null -> jumpTo(Routes.forPage(pageId, node.isDatabase))
                rowId != null -> sidebar.openRow(rowId) { id -> jumpTo(Routes.pageEditor(id)) }
            }
        },
        onToggleNode = { sidebar.toggle(it) },
        onSearch = { closeSidebar(); navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
        onTemplates = { closeSidebar(); navController.navigate(Routes.TEMPLATES) { launchSingleTop = true } },
        onTrash = { closeSidebar(); navController.navigate(Routes.TRASH) { launchSingleTop = true } },
        onSettings = { closeSidebar(); navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
        onBackup = { closeSidebar(); navController.navigate(Routes.BACKUP) { launchSingleTop = true } },
        onConnections = { closeSidebar(); navController.navigate(Routes.CONNECTIONS) { launchSingleTop = true } },
        // Sul PC "⋯" e "+" accanto alle pagine dell'albero (vedi `SidebarPageActions`).
        pageActions = if (isPc) SidebarPageActions(
            addInside = { parentId ->
                scope.launch(Dispatchers.Main.immediate) {
                    val note = withContext(Dispatchers.IO) { factory.pageRepository.createQuickNote(parentId) }
                    QuickNotes.created(note.first, note.second)
                    closeSidebar()
                    navController.navigate(Routes.pageEditor(note.first))
                }
            },
            isFavorite = { id -> withContext(Dispatchers.IO) { factory.pageRepository.getPage(id)?.isFavorite == true } },
            setFavorite = { id, favorite -> scope.launch(Dispatchers.IO) { factory.pageRepository.setFavorite(id, favorite) } },
            rename = { id, title -> scope.launch(Dispatchers.IO) { factory.pageRepository.renamePage(id, title) } },
            moveToTrash = { id ->
                scope.launch {
                    withContext(Dispatchers.IO) { factory.pageRepository.moveToTrash(id) }
                    // Era aperta: si torna al menu principale, come buttandola dal suo menu.
                    if (id == currentPageId) goHome()
                }
            }
        ) else null
    )

    // Il pulsante in cima alla barra, sul PC: fissarla accanto alla pagina, o richiuderla.
    val pinButton: @Composable () -> Unit = {
        if (canPinSidebar) {
            SidebarPinButton(pinned = pinned) {
                if (pinned) {
                    AppSettings.sidebarPinned = false
                } else {
                    // Prima si chiude il cassetto, poi la barra va accanto alla pagina: al contrario il
                    // cassetto restava "aperto" col velo invisibile, e il primo clic finiva lì.
                    scope.launch {
                        drawerState.snapTo(DrawerValue.Closed)
                        AppSettings.sidebarPinned = true
                    }
                }
            }
        }
    }

    val sidebarBody: @Composable (widgetsActive: Boolean) -> Unit = { widgetsActive ->
                SidebarContent(
                    root = treeRoot,
                    children = treeChildren,
                    expanded = treeExpanded,
                    currentPageId = currentPageId,
                    actions = actions,
                    widgetsActive = widgetsActive,
                    header = pinButton,
                    notebooks = {
                        NotebooksSidebarSection(
                            viewModel = notebooks,
                            store = factory.imageStore,
                            currentSectionId = currentSectionId,
                            currentNotebookId = currentNotebookId,
                            onOpenSection = { sectionId ->
                                closeSidebar()
                                jumpTo(Routes.notebookSection(sectionId))
                            },
                            onOpenNotebook = { notebookId ->
                                closeSidebar()
                                jumpTo(Routes.notebook(notebookId))
                            }
                        )
                    }
                )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Il trascinamento serve solo a chiuderla: aperta col dito dal bordo
        // sinistro litigherebbe col gesto "indietro" del telefono, che parte
        // proprio da lì.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            // Fissata, la barra sta accanto alla pagina e il cassetto resta vuoto, ma largo uguale:
            // largo zero, "chiuso" e "aperto" sarebbero lo stesso punto e potrebbe credersi aperto.
            if (pinned) {
                Box(modifier = Modifier.width(SIDEBAR_WIDTH))
            } else {
                ModalDrawerSheet(
                    drawerContainerColor = SidebarBackground,
                    windowInsets = NoInsets,
                    modifier = Modifier.width(SIDEBAR_WIDTH)
                ) {
                    // Aperta o mentre si apre: da chiusa la barra resta
                    // disegnata dietro alla pagina, e gli orologi non
                    // devono ridisegnarsi ogni secondo per nessuno.
                    sidebarBody(drawerState.isOpen || drawerState.targetValue == DrawerValue.Open)
                }
            }
        }
    ) {
        CompositionLocalProvider(LocalSidebarPinned provides pinned, LocalPageOpener provides pageOpener) {
            Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Fissata: la barra accanto alla pagina, col bordo che la allarga e la stringe.
                if (pinned) {
                    var width by remember { mutableFloatStateOf(AppSettings.sidebarWidth.toFloat()) }
                    Box(modifier = Modifier.width(width.dp).fillMaxHeight()) { sidebarBody(true) }
                    SidebarResizeHandle(
                        onDrag = { delta ->
                            width = (width + delta).coerceIn(AppSettings.SIDEBAR_MIN_WIDTH.toFloat(), AppSettings.SIDEBAR_MAX_WIDTH.toFloat())
                        },
                        onDragStopped = { AppSettings.sidebarWidth = width.roundToInt() }
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AppRoutes(
                        navController = navController,
                        factory = factory,
                        animations = animations,
                        onOpenSidebar = ::openSidebar,
                        onOpenRowPage = { rowId, then ->
                            scope.launch { factory.pageRepository.ensureRowPage(rowId)?.let(then) }
                        }
                    )
                    // Side peek: sopra la pagina, a destra; la barra laterale resta libera.
                    peek?.takeIf { it.mode == OpenMode.SIDE_PEEK }?.let { SidePeekLayer(it, factory, peekActions) }
                }
            }
            // Center peek: sopra tutta la finestra, barra laterale compresa.
            peek?.takeIf { it.mode == OpenMode.CENTER_PEEK }?.let { CenterPeekLayer(it, factory, peekActions) }
            }
        }
    }
}

private val SIDEBAR_WIDTH = 300.dp

private const val PINNED_REFRESH_DELAY_MS = 300L

/** In cima alla barra, sul PC: la puntina per fissarla accanto alla pagina; fissata, le due frecce per richiuderla. */
@Composable
private fun SidebarPinButton(pinned: Boolean, onClick: () -> Unit) {
    // **In un pallino, a destra di "Main menu"** (dal 11/11/2026, chiesto
    // dall'utente: "così non si confonde" con le icone delle voci), che si
    // vede solo col mouse sopra (chiesto dall'utente lo stesso giorno).
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .padding(end = 10.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(if (hovered) SidebarPinCircle else Color.Transparent)
            .hoverable(interaction)
    ) {
        Icon(
            if (pinned) Icons.Filled.KeyboardDoubleArrowLeft else Icons.Filled.PushPin,
            contentDescription = if (pinned) Strings.closeSidebar else Strings.keepSidebarOpen,
            tint = NotionGray400,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Il bordo destro della barra fissata: trascinato, la allarga o la stringe ([onDrag] in dp). */
@Composable
private fun SidebarResizeHandle(onDrag: (Float) -> Unit, onDragStopped: () -> Unit) {
    val density = LocalDensity.current
    val drag by rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .width(6.dp)
            .fillMaxHeight()
            .horizontalResizeCursor()
            .draggable(
                state = rememberDraggableState { delta -> drag(with(density) { delta.toDp().value }) },
                orientation = Orientation.Horizontal,
                onDragStopped = { onDragStopped() }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(NotionGray900))
    }
}

@Composable
private fun AppRoutes(
    navController: NavHostController,
    factory: ViewModelFactory,
    animations: AnimationLevel,
    onOpenSidebar: () -> Unit,
    onOpenRowPage: (rowId: String, then: (String) -> Unit) -> Unit
) {
    // Le dissolvenze fra una schermata e l'altra seguono l'impostazione
    // delle animazioni: piene come di serie, brevi, o nessuna.
    val enter: EnterTransition = when (animations) {
        AnimationLevel.FULL -> fadeIn(tween(FULL_FADE_MS))
        AnimationLevel.REDUCED -> fadeIn(tween(REDUCED_FADE_MS))
        AnimationLevel.OFF -> EnterTransition.None
    }
    val exit: ExitTransition = when (animations) {
        AnimationLevel.FULL -> fadeOut(tween(FULL_FADE_MS))
        AnimationLevel.REDUCED -> fadeOut(tween(REDUCED_FADE_MS))
        AnimationLevel.OFF -> ExitTransition.None
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit }
    ) {

        // **Il cancello delle pagine bloccate con la password** (dal
        // 26/09/2026) sta davanti a ogni pagina e database a schermo
        // intero: da qualunque parte ci si arrivi — un collegamento, la
        // barra, la ricerca, i preferiti, una notifica — prima di vederla
        // si chiede impronta o password (vedi `LockGate`). La pagina
        // principale non si blocca, ma passandoci le altre si richiudono.
        composable(Routes.HOME) {
            LockGate(PageEntity.ROOT_PAGE_ID, factory.pageRepository, fullScreen = true) {
            PageEditorScreen(
                pageId = PageEntity.ROOT_PAGE_ID,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenUpdates = { id -> navController.navigate(Routes.updates(id)) },
                onNavigateToPage = { pageId -> navController.navigate(Routes.pageEditor(pageId)) },
                onNavigateToDatabase = { pageId -> navController.navigate(Routes.databaseView(pageId)) },
                onOpenSidebar = onOpenSidebar
            )
            }
        }

        composable(
            route = Routes.PAGE_EDITOR,
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.read { getStringOrNull("pageId") } ?: return@composable
            LockGate(pageId, factory.pageRepository, fullScreen = true, onBack = { navController.popBackStack() }) {
            PageEditorScreen(
                pageId = pageId,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenUpdates = { id -> navController.navigate(Routes.updates(id)) },
                onNavigateToPage = { linkedId -> navController.navigate(Routes.pageEditor(linkedId)) },
                onNavigateToDatabase = { linkedId -> navController.navigate(Routes.databaseView(linkedId)) },
                onOpenSidebar = onOpenSidebar,
                // La pagina dopo prende il posto di questa: "indietro" torna
                // al database, non a tutte le pagine scorse con le frecce.
                onOpenSiblingRow = { rowId ->
                    onOpenRowPage(rowId) { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.pageEditor(id))
                    }
                }
            )
            }
        }

        composable(
            route = Routes.UPDATES,
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.read { getStringOrNull("pageId") } ?: return@composable
            UpdatesScreen(
                pageId = pageId,
                factory = factory,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DATABASE_VIEW,
            arguments = listOf(navArgument("pageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.read { getStringOrNull("pageId") } ?: return@composable
            LockGate(pageId, factory.pageRepository, fullScreen = true, isDatabase = true, onBack = { navController.popBackStack() }) {
            DatabaseViewScreen(
                pageId = pageId,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenRowPage = { rowPageId -> navController.navigate(Routes.pageEditor(rowPageId)) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenUpdates = { id -> navController.navigate(Routes.updates(id)) },
                onOpenPage = { id -> navController.navigate(Routes.pageEditor(id)) },
                onOpenDatabase = { id -> navController.navigate(Routes.databaseView(id)) },
                onOpenSidebar = onOpenSidebar
            )
            }
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenHit = { hit ->
                    // Via la ricerca prima di aprire la pagina, così
                    // "indietro" dalla pagina torna da dove si era partiti
                    // e non alla ricerca vuota.
                    val pageId = hit.pageId
                    val rowId = hit.rowId
                    if (pageId != null) {
                        navController.popBackStack()
                        navController.navigate(Routes.forPage(pageId, hit.isDatabase))
                    } else if (rowId != null) {
                        onOpenRowPage(rowId) { id ->
                            navController.popBackStack()
                            navController.navigate(Routes.pageEditor(id))
                        }
                    }
                }
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPage = { page -> navController.navigate(Routes.forPage(page.id, page.isDatabase)) }
            )
        }

        composable(Routes.TEMPLATES) {
            TemplatesScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenTemplate = { id -> navController.navigate(Routes.pageEditor(id)) }
            )
        }

        composable(Routes.TRASH) {
            TrashScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPage = { page -> navController.navigate(Routes.forPage(page.id, page.isDatabase)) }
            )
        }

        // **Una sezione di un quaderno** (dal 26/09/2026): le sue pagine. Se
        // la sezione è protetta con la password, prima si chiede quella.
        composable(
            route = Routes.NOTEBOOK_SECTION,
            arguments = listOf(navArgument("sectionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.read { getStringOrNull("sectionId") } ?: return@composable
            LockGate(
                sectionId,
                factory.pageRepository,
                fullScreen = true,
                lockedTitle = NotebookStrings.lockedSection,
                onBack = { navController.popBackStack() }
            ) {
            NotebookSectionScreen(
                sectionId = sectionId,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenSidebar = onOpenSidebar,
                onOpenPage = { pageId -> navController.navigate(Routes.pageEditor(pageId)) },
                onOpenSection = { id ->
                    navController.navigate(Routes.notebookSection(id)) {
                        popUpTo(Routes.HOME)
                        launchSingleTop = true
                    }
                }
            )
            }
        }

        // **Un quaderno** (dal 26/09/2026): toccandone il nome nella barra,
        // tutte le sue sezioni e i gruppi in una pagina a sé.
        composable(
            route = Routes.NOTEBOOK,
            arguments = listOf(navArgument("notebookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.read { getStringOrNull("notebookId") } ?: return@composable
            NotebookScreen(
                notebookId = notebookId,
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenSidebar = onOpenSidebar,
                onOpenSection = { id -> navController.navigate(Routes.notebookSection(id)) }
            )
        }

        composable(Routes.STARTUP) {
            StartupScreen(factory = factory, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                factory = factory,
                onBack = { navController.popBackStack() },
                onOpenPatchNotes = { navController.navigate(Routes.PATCH_NOTES) { launchSingleTop = true } },
                onOpenStartup = { navController.navigate(Routes.STARTUP) { launchSingleTop = true } }
            )
        }

        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CONNECTIONS) {
            ConnectionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PATCH_NOTES) {
            PatchNotesScreen(
                onBack = { navController.popBackStack() },
                onOpenVersion = { version -> navController.navigate(Routes.patchNote(version)) }
            )
        }

        composable(
            route = Routes.PATCH_NOTE,
            arguments = listOf(navArgument("version") { type = NavType.StringType })
        ) { entry ->
            PatchNoteScreen(
                version = entry.arguments?.read { getStringOrNull("version") }.orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// Piena è la dissolvenza di serie della navigazione, quella che l'app ha
// sempre avuto; ridotta è un sesto.
private const val FULL_FADE_MS = 700
private const val REDUCED_FADE_MS = 120

/** Lasciata una nota al volo, quanto aspettare prima di chiuderla: vedi `QuickNotes`. */
private const val QUICK_NOTE_SETTLE_MS = 1_500L
