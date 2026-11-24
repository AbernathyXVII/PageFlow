package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **I Workspace** (dal 24/11/2026, chiesto dall'utente come in Notion): in cima alla barra laterale, e
 * per ognuno il suo menu principale e il suo albero; ricerca, preferiti, recenti, template e cestino solo
 * del Workspace aperto. Le pagine di prima stanno nel primo, quello di sempre.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopWorkspacesTest {
    private val dir: File = Files.createTempDirectory("pageflow-workspaces").toFile()
    private val context = DesktopContext(dir)
    private lateinit var repo: PageRepository

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        AppSettings.currentWorkspaceId = null
        AppSettings.lastVisitedRoute = null
        AppSettings.pcTabs = null
        repo = PageRepository(AppDatabase.getInstance(context))
        repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
    }

    @After
    fun tearDown() {
        AppSettings.currentWorkspaceId = null
        AppSettings.sidebarPinned = false
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    /** I risultati come titolo → Workspace. */
    private fun where(found: List<PageRepository.SearchHit>) = found.associate { it.title to it.workspaceId }

    @Test
    fun everythingIsSeparatedByWorkspace() = runBlocking {
        val alpha = repo.createQuickNote(PageEntity.ROOT_PAGE_ID).first
        repo.renamePage(alpha, "Alpha nota")
        repo.setFavorite(alpha, true)
        val oldTemplate = repo.createTemplate()
        repo.renamePage(oldTemplate, "Modello vecchio")

        val work = repo.createWorkspace("Lavoro")
        assertEquals(listOf(PageEntity.ROOT_PAGE_ID, work), repo.workspaces().map { it.id })
        AppSettings.currentWorkspaceId = work
        val beta = repo.createQuickNote(repo.homeId()).first
        repo.renamePage(beta, "Beta nota")
        assertEquals(work, repo.workspaceOf(beta))
        assertEquals(PageEntity.ROOT_PAGE_ID, repo.workspaceOf(alpha))

        // La ricerca: tutti i Workspace, ogni risultato col suo (dal 24/11/2026, prima solo quello aperto).
        assertEquals(mapOf("Beta nota" to work, "Alpha nota" to PageEntity.ROOT_PAGE_ID), where(repo.search("nota", inContent = false, inTrash = false)))
        // Preferiti e template.
        repo.setFavorite(beta, true)
        assertEquals(listOf(beta), repo.observeFavorites().first().map { it.id })
        val newTemplate = repo.createTemplate()
        assertEquals(listOf(newTemplate), repo.observeTemplates().first().map { it.id })
        // Il cestino: una pagina buttata qui si vede solo qui, e torna qui.
        repo.moveToTrash(beta)
        assertEquals(listOf(beta), repo.observeTrash().first().map { it.id })
        // La pagina principale di un Workspace non si butta.
        assertNull(repo.moveToTrash(work))

        // Di nuovo quello di sempre: le sue cose, e niente di "Lavoro".
        AppSettings.currentWorkspaceId = null
        // Anche dal Workspace di sempre, e anche nel cestino: la pagina buttata in "Lavoro" resta sua.
        assertEquals(mapOf("Alpha nota" to PageEntity.ROOT_PAGE_ID, "Beta nota" to work), where(repo.search("nota", inContent = false, inTrash = true)))
        assertEquals(listOf(alpha), repo.observeFavorites().first().map { it.id })
        assertEquals(listOf(oldTemplate), repo.observeTemplates().first().map { it.id })
        assertTrue("il cestino di Lavoro non si vede qui", repo.observeTrash().first().isEmpty())
        // Svuotando il cestino di qui, quello di "Lavoro" resta.
        repo.emptyTrash()
        assertTrue("la pagina buttata in Lavoro c'è ancora", repo.getPage(beta) != null)

        // Ripristinata, torna nel menu principale di "Lavoro", anche da qui.
        repo.restoreFromTrash(beta)
        assertEquals(work, repo.workspaceOf(beta))
        assertTrue(repo.treeChildren(work).any { it.pageId == beta })
    }

    /** La pagina della ricerca: i risultati a gruppi, prima il Workspace aperto, ognuno col suo titolo. */
    @Test
    fun searchShowsEveryWorkspaceInItsGroup() {
        runBlocking {
            repo.renamePage(repo.createQuickNote(PageEntity.ROOT_PAGE_ID).first, "Casa nota")
            val id = repo.createWorkspace("Lavoro")
            repo.renamePage(repo.createQuickNote(id).first, "Lavoro nota")
            AppSettings.currentWorkspaceId = id
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(com.gabriele.notionlocal.viewmodel.SearchViewModel::class, androidx.lifecycle.viewmodel.CreationExtras.Empty)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        com.gabriele.notionlocal.ui.screen.SearchScreen(factory = factory, onBack = {}, onOpenHit = {}, viewModel = viewModel)
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        try {
            frames(10)
            onWindowThread { viewModel.onQueryChange("nota") }
            frames(30)
            onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
                File("build/desktop-screenshots").mkdirs()
                File("build/desktop-screenshots", "workspaces_search.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
            val shown = onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }?.let { it to node.boundsInRoot.top } }
                    .filter { it.first in setOf("Lavoro", "Lavoro nota", "My Space", "Casa nota") }
                    .sortedBy { it.second }.map { it.first }
            }
            assertEquals("prima Lavoro (aperto), poi quello di sempre", listOf("Lavoro", "Lavoro nota", "My Space", "Casa nota"), shown)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /** Il selettore in cima alla barra fissata: si passa a "Lavoro" e si vede il suo menu principale. */
    @Test
    fun theSwitcherOpensTheOtherWorkspace() {
        val work = runBlocking {
            repo.createQuickNote(PageEntity.ROOT_PAGE_ID).first.also { repo.renamePage(it, "Pagina di casa") }
            val id = repo.createWorkspace("Lavoro")
            repo.createQuickNote(id).first.also { repo.renamePage(it, "Pagina del lavoro") }
            id
        }
        AppSettings.sidebarPinned = true
        AppSettings.sidebarWidth = 260
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 800, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize()) { AppNavHost(factory = factory) } }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun texts(): List<Pair<String, androidx.compose.ui.geometry.Rect>> = onWindowThread {
            scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }.mapNotNull { node ->
                (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                    ?: node.config.getOrNull(SemanticsProperties.EditableText)?.text)?.let { it to node.boundsInRoot }
            }
        }
        fun click(at: Offset) = onWindowThread {
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        fun shot(name: String) = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        try {
            frames(40)
            shot("workspaces_home")
            val before = texts()
            val switcher = before.firstOrNull { (text, r) -> text == "My Space" && r.left < 260f && r.top < 60f }
                ?: error("il Workspace in cima alla barra: ${before.map { it.first }.take(40)}")
            assertTrue("l'albero è quello di sempre", before.any { it.first == "Pagina di casa" })
            click(switcher.second.center)
            frames(15)
            shot("workspaces_menu")
            val item = texts().lastOrNull { (text, r) -> text == "Lavoro" && r.left < 400f } ?: error("Lavoro nell'elenco")
            assertTrue("e la voce per crearne uno", texts().any { it.first == Strings.newWorkspace })
            click(item.second.center)
            frames(30)
            shot("workspaces_work")
            assertEquals(work, AppSettings.currentWorkspaceId)
            val after = texts()
            assertTrue("in cima alla barra c'è Lavoro", after.any { (text, r) -> text == "Lavoro" && r.left < 260f && r.top < 60f })
            assertTrue("l'albero è quello di Lavoro", after.any { it.first == "Pagina del lavoro" })
            assertTrue("e niente di quello di sempre", after.none { it.first == "Pagina di casa" })
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
