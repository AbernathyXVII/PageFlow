package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppCleanup
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.backup.BackupReason
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.navigation.Routes
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.screen.BackupScreen
import com.gabriele.notionlocal.ui.screen.PageExportSheet
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **L'editor delle pagine sul PC** (dal 09/11/2026, quando è passato nel
 * codice comune): disegnato per qualche secondo con una pagina vera, gira
 * senza errori e carica le sue righe. La foto resta in
 * `build/desktop-screenshots/page_editor.png`.
 */
class DesktopEditorTest {
    private val dir: File = Files.createTempDirectory("pageflow-editor").toFile()
    private val context = DesktopContext(dir)

    /** Quello che l'app dà alle schermate: chi tiene i ViewModel, e una vita "in primo piano". */
    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = "p", title = "Lista della spesa"))
        listOf("Latte", "Pane").forEachIndexed { i, text ->
            repo.saveBlock(BlockEntity(pageId = "p", textJson = """[{"text":"$text"}]""", orderIndex = i))
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    /** Disegna [content] per qualche secondo, come la finestra, e salva la foto in `build/desktop-screenshots`. */
    private fun draw(
        name: String,
        owner: ScreenOwner = ScreenOwner(),
        width: Int = 520,
        height: Int = 900,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        // Sul filo della finestra (quello di Swing), come nella finestra vera: la navigazione lo pretende.
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = width, height = height, density = Density(1f)) {
                CompositionLocalProvider(
                    LocalPlatformContext provides context,
                    LocalLifecycleOwner provides owner,
                    LocalViewModelStoreOwner provides owner
                ) {
                    NotionLocalTheme { content() }
                }
            }
        }
        // Il database si legge fuori dal filo principale: qualche giro di disegno.
        var time = 0L
        repeat(40) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(50)
        }
        val png = onWindowThread { scene.render(time).encodeToData(EncodedImageFormat.PNG)!!.bytes.also { scene.close() } }
        File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(png)
    }

    /** Le impostazioni, le stesse del telefono (senza le notifiche): la foto in `settings.png`. */
    @Test
    fun theSettingsDrawOnThePc() {
        val factory = ViewModelFactory(context)
        draw("settings.png") { SettingsScreen(onBack = {}, factory = factory) }
    }

    /** La pagina Backup, la stessa del telefono, con un backup fatto sul PC nell'elenco: la foto in `backup.png`. */
    @Test
    fun theBackupPageDrawsOnThePc() {
        val folder = File(dir, "backups")
        Backups.folderOverride = folder
        try {
            Backups.init(context)
            val made = runBlocking { Backups.backupNow(BackupReason.MANUAL) }
            assertTrue(File(folder, made.name).exists())
            draw("backup.png") { BackupScreen(onBack = {}, onRestart = {}) }
        } finally {
            Backups.folderOverride = null
        }
    }

    /** La finestra "Export" sul PC: PDF e Markdown, "Print" e "Save" (pieno: "Share" non c'è). Foto in `export.png`. */
    @Test
    fun theExportSheetDrawsOnThePc() {
        draw("export.png") {
            PageExportSheet(title = "Lista della spesa", hasSubpages = true, export = { null }, onDismiss = {})
        }
    }

    /** "Delete all content" sul PC: tutte le tabelle vuote, e rinasce solo My Space. */
    @Test
    fun deleteAllContentLeavesAnEmptyMySpace() = runBlocking {
        AppCleanup.deleteAllContent(context)
        val repo = PageRepository(AppDatabase.getInstance(context))
        assertNull(repo.getPage("p"))
        assertEquals("My Space", repo.getPage(PageEntity.ROOT_PAGE_ID)?.title)
    }

    /** Tutta l'app, come la apre la finestra del PC (`Main.kt`): arriva alla home. */
    @Test
    fun theWholeAppOpensTheHomeOnThePc() {
        AppSettings.lastVisitedRoute = null
        val factory = ViewModelFactory(context)
        draw("app_home.png") { AppNavHost(factory = factory) }
        assertEquals(Routes.HOME, AppSettings.lastVisitedRoute)
    }

    /**
     * La barra laterale fissata, in una finestra larga (chiesto dall'utente il
     * 09/11/2026, "come fa Notion"): la barra a sinistra, la pagina in tutto lo
     * spazio che resta, senza il pulsante per aprire la barra. Foto in `app_pinned.png`.
     */
    @Test
    fun thePinnedSidebarSitsBesideThePage() {
        AppSettings.sidebarPinned = true
        AppSettings.sidebarWidth = 260
        try {
            val factory = ViewModelFactory(context)
            draw("app_pinned.png", width = 1200, height = 800) {
                Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory) }
            }
        } finally {
            AppSettings.sidebarPinned = false
            AppSettings.sidebarWidth = AppSettings.SIDEBAR_DEFAULT_WIDTH
        }
    }

    @Test
    fun theEditorDrawsAPageOnThePc() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        draw("page_editor.png") {
            PageEditorScreen(
                pageId = "p",
                factory = factory,
                onBack = {},
                onOpenSearch = {},
                onOpenUpdates = {},
                onNavigateToPage = {},
                onNavigateToDatabase = {},
                viewModel = viewModel
            )
        }

        assertEquals("Lista della spesa", viewModel.page.value?.title)
        assertEquals(2, viewModel.blocks.value.size)
    }
}
