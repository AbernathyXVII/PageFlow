package com.gabriele.notionlocal.ui.navigation

import com.gabriele.notionlocal.platform.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.width
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
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.notify.NotificationTarget
import com.gabriele.notionlocal.data.settings.AnimationLevel
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.share.QuickNotes
import com.gabriele.notionlocal.ui.i18n.ShareStrings
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
import com.gabriele.notionlocal.ui.screen.SettingsScreen
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
    val navController = rememberNavController()
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

    fun openSidebar() {
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

    // Ctrl+F sul PC (vedi `AppShortcuts`): la ricerca, come dalla barra laterale.
    DisposableEffect(navController) {
        AppShortcuts.openSearch = { closeSidebar(); navController.navigate(Routes.SEARCH) { launchSingleTop = true } }
        onDispose { AppShortcuts.openSearch = null }
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
        onBackup = { closeSidebar(); navController.navigate(Routes.BACKUP) { launchSingleTop = true } }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Il trascinamento serve solo a chiuderla: aperta col dito dal bordo
        // sinistro litigherebbe col gesto "indietro" del telefono, che parte
        // proprio da lì.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SidebarBackground,
                windowInsets = NoInsets,
                modifier = Modifier.width(SIDEBAR_WIDTH)
            ) {
                SidebarContent(
                    root = treeRoot,
                    children = treeChildren,
                    expanded = treeExpanded,
                    currentPageId = currentPageId,
                    actions = actions,
                    // Aperta o mentre si apre: da chiusa la barra resta
                    // disegnata dietro alla pagina, e gli orologi non
                    // devono ridisegnarsi ogni secondo per nessuno.
                    widgetsActive = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open,
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
        }
    ) {
        AppRoutes(
            navController = navController,
            factory = factory,
            animations = animations,
            onOpenSidebar = ::openSidebar,
            onOpenRowPage = { rowId, then ->
                scope.launch { factory.pageRepository.ensureRowPage(rowId)?.let(then) }
            }
        )
    }
}

private val SIDEBAR_WIDTH = 300.dp

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
