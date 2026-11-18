package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.navigation.AppNavHost
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **I file portati su un peek vanno nella pagina del peek** (18/11/2026, chiesto dall'utente: "Non
 * posso mettere un'immagine in una pagina aperta in Center Peek, e me la fa mettere in realtà nella
 * pagina aperta dietro"). Compose dà il file al primo bersaglio, nell'ordine dell'albero, che contiene
 * il punto, senza guardare cosa c'è disegnato sopra: la prova rifà la stessa scelta sui bersagli dei
 * file (le pagine; quelli dei campi di testo, bassi, prendono solo testo) e guarda dove cade.
 */
class DesktopPeekDropTest {
    private val dir: File = Files.createTempDirectory("pageflow-peekdrop").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    /** Un nodo in cima alla finestra, da cui visitare i bersagli del trascinamento. */
    private class Probe : Modifier.Node()
    private class ProbeElement(val probe: Probe) : ModifierNodeElement<Probe>() {
        override fun create() = probe
        override fun update(node: Probe) = Unit
        override fun hashCode() = System.identityHashCode(probe)
        override fun equals(other: Any?) = other is ProbeElement && other.probe === probe
    }

    @After
    fun tearDown() {
        AppSettings.openPagesAs = OpenMode.CENTER_PEEK
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun aFileDroppedOnThePeekGoesToThePeek() {
        AppSettings.init(context)
        AppSettings.sidebarPinned = false
        AppSettings.lastVisitedRoute = null
        AppSettings.pcTabs = null
        AppSettings.openPagesAs = OpenMode.CENTER_PEEK
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = PageEntity.ROOT_PAGE_ID, title = "My Space"))
            repo.createPage(PageEntity(id = "target", title = "Target"))
            repo.saveBlock(BlockEntity(pageId = "target", textJson = """[{"text":"Peeked text"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "link", pageId = PageEntity.ROOT_PAGE_ID, type = BlockType.PAGE_LINK, linkedPageId = "target", orderIndex = 0))
        }
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val probe = Probe()
        val scene = onWindowThread {
            ImageComposeScene(width = 1200, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme { Surface(modifier = Modifier.fillMaxSize().then(ProbeElement(probe))) { AppNavHost(factory = factory) } }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun click(x: Float, y: Float) = onWindowThread {
            val at = Offset(x, y)
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        // La chiave con cui Compose riconosce i suoi bersagli del trascinamento.
        val dropKey = Class.forName("androidx.compose.ui.draganddrop.DragAndDropNode\$Companion\$DragAndDropTraversableKey").getField("INSTANCE").apply { isAccessible = true }.get(null)
        /** Dove Compose porterebbe un file lasciato in [at]: il primo bersaglio dei file che lo contiene. */
        fun dropTargetAt(at: Offset): Rect? = onWindowThread {
            var found: Rect? = null
            probe.traverseDescendants(dropKey) { node: TraversableNode ->
                val coordinates = node.requireLayoutCoordinates()
                val bounds = Rect(coordinates.positionInRoot(), coordinates.size.toSize())
                if (bounds.height > 300f && bounds.contains(at)) {
                    found = bounds
                    TraversableNode.Companion.TraverseDescendantsAction.CancelTraversal
                } else {
                    TraversableNode.Companion.TraverseDescendantsAction.ContinueTraversal
                }
            }
            found
        }
        try {
            frames(25)
            assertNotNull("senza peek, la pagina prende i file", dropTargetAt(Offset(600f, 400f)))
            // Center peek: la finestra al centro; un file lasciato nel mezzo va nella sua pagina.
            click(128f, PEEK_LINK_Y)
            frames(25)
            val center = dropTargetAt(Offset(600f, 400f))
            assertNotNull("nel Center peek la pagina prende i file", center)
            assertTrue("il file va nella pagina del Center peek, non in quella dietro ($center)", center!!.left > 100f && center.right < 1100f)
            click(60f, 400f)
            frames(25)
            // Side peek: un file lasciato nel pannello va nella sua pagina.
            AppSettings.openPagesAs = OpenMode.SIDE_PEEK
            click(128f, PEEK_LINK_Y)
            frames(25)
            val side = dropTargetAt(Offset(900f, 400f))
            assertNotNull("nel Side peek la pagina prende i file", side)
            assertTrue("il file va nella pagina del Side peek, non in quella sotto ($side)", side!!.left > 550f)
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private companion object {
        const val PEEK_LINK_Y = 189f
    }
}
