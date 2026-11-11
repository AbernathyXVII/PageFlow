package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.ui.screen.TreeRow
import com.gabriele.notionlocal.ui.screen.SettingsRow
import com.gabriele.notionlocal.ui.screen.OptionsGroup
import com.gabriele.notionlocal.ui.screen.OptionRow
import com.gabriele.notionlocal.ui.screen.FlatTreeRow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
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
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.PageEditorScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.abs

/**
 * **Sul PC il testo è grande come in Notion** (dal 11/11/2026, chiesto
 * dall'utente con una foto di Notion accanto a PageFlow). Nella sua foto,
 * rimpicciolita a 0,78, queste scritte di Notion erano larghe: il titolo
 * "My personal workspace" 247 pixel, la riga "Denti alle 9:00" 62, il
 * titolo del database "Database principale" 149. Qui, a densità 1, le
 * stesse scritte di PageFlow devono essere larghe quanto quelle divise per
 * 0,78, con un margine del 4% (la foto è piccola).
 */
class DesktopNotionSizesTest {
    private val dir: File = Files.createTempDirectory("pageflow-sizes").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theTextIsAsLargeAsInNotion() {
        AppSettings.init(context)
        runBlocking {
            val repo = PageRepository(AppDatabase.getInstance(context))
            repo.createPage(PageEntity(id = "spazio", title = "My personal workspace"))
            repo.createPage(PageEntity(id = "principale", title = "Database principale", isDatabase = true, parentId = "spazio"))
            repo.saveBlock(BlockEntity(id = "denti", pageId = "spazio", textJson = """[{"text":"Denti alle 9:00"}]""", orderIndex = 0))
            repo.saveBlock(BlockEntity(id = "db", pageId = "spazio", type = BlockType.DATABASE_LINK, linkedPageId = "principale", orderIndex = 1))
        }
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 900, height = 700, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "spazio", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
                    }
                }
            }
        }
        try {
            var time = 0L
            repeat(40) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            val page = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots", "notion_sizes.png").apply { parentFile.mkdirs() }
                .writeBytes(Image.makeFromBitmap(page).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 140 && ((c shr 8) and 0xFF) > 140 && (c and 0xFF) > 140
            // Le fasce di righe col testo bianco (da sotto la barra in alto), e quanto è larga ognuna.
            val rows = (60 until page.height).filter { y -> (40 until 700).any { x -> bright(page.getColor(x, y)) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            fun widthOf(band: IntRange): Int {
                val xs = (40 until 700).filter { x -> band.any { y -> bright(page.getColor(x, y)) } }
                return xs.last() - xs.first() + 1
            }
            val widths = bands.map { widthOf(it) }
            // Dopo l'icona della pagina: il titolo, la riga, il titolo del database.
            val expected = listOf("My personal workspace" to 247 / 0.78, "Denti alle 9:00" to 62 / 0.78, "Database principale" to 149 / 0.78)
            val found = widths.drop(1).take(3)
            for ((i, pair) in expected.withIndex()) {
                val (text, want) = pair
                val got = found.getOrNull(i) ?: 0
                assertTrue("\"$text\": larga $got, in Notion ${want.toInt()} (fasce $bands, larghezze $widths)", abs(got - want) / want < 0.04)
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    /**
     * **Le righe sono fitte come in Notion** (fase 2, chiesta dall'utente
     * l'11/11/2026): nella sua foto le voci della barra laterale di Notion
     * stanno a 28 l'una dall'altra, le voci dei menu a 26; qui le righe
     * dell'albero sono alte 28, quelle dei menu e delle impostazioni fra 26
     * e 30, e fra una voce e l'altra di un menu non c'è nessuna linea.
     */
    @Test
    fun theRowsAreAsTightAsInNotion() {
        AppSettings.init(context)
        val heights = HashMap<String, Int>()
        fun Modifier.measured(name: String) = onSizeChanged { heights[name] = it.height }
        val node = PageRepository.PageTreeNode(pageId = "p", rowId = null, title = "Main calendar", emoji = "", iconImage = null, isDatabase = false, hasChildren = true)
        val scene = onWindowThread {
            ImageComposeScene(width = 400, height = 400, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context) {
                    NotionLocalTheme {
                        Column(Modifier.background(Color.Black)) {
                            Box(Modifier.measured("albero")) { TreeRow(FlatTreeRow(node, 0), isCurrent = false, isExpanded = false, onOpen = {}, onToggle = {}) }
                            Box(Modifier.measured("menu")) {
                                OptionsGroup {
                                    OptionRow(icon = Icons.Filled.Add, label = "Add above", onClick = {})
                                    HorizontalDivider()
                                    OptionRow(icon = Icons.Filled.Add, label = "Add below", onClick = {})
                                }
                            }
                            Box(Modifier.measured("impostazioni")) { SettingsRow(icon = Icons.Filled.Add, label = "Layout", value = "Table", onClick = {}) }
                        }
                    }
                }
            }
        }
        try {
            val picture = onWindowThread { scene.render(0L); Bitmap.makeFromImage(scene.render(100_000_000L)) }
            val tree = heights.getValue("albero")
            val menu = heights.getValue("menu")
            val settings = heights.getValue("impostazioni")
            assertTrue("riga dell'albero alta $tree", tree == 28)
            assertTrue("due voci di menu alte $menu", menu in 52..61)
            assertTrue("riga delle impostazioni alta $settings", settings in 26..30)
            // Fra le due voci del menu nessuna linea: la riga di mezzo è del colore del fondo.
            val top = tree
            val middle = top + menu / 2
            val lineSeen = (middle - 2..middle + 2).any { y -> (10 until 300).count { x -> picture.getColor(x, y) != picture.getColor(5, y) } > 200 }
            assertTrue("una linea fra le voci del menu", !lineSeen)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
