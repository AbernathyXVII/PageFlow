package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.GroupRuleSpec
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.encodeGroupRules
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **I gruppi anche nell'elenco, sul PC** (18/11/2026, chiesto dall'utente: "Implementa il grouping
 * anche nelle viste List dei database"): fra le righe le intestazioni dei gruppi, con la freccia
 * sotto le icone delle righe.
 */
class DesktopListGroupsTest {
    private val dir: File = Files.createTempDirectory("pageflow-listgroups").toFile()
    private val context = DesktopContext(dir)

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        db.pageDao().insert(
            PageEntity(
                id = "spesa", title = "Spesa", isDatabase = true, databaseLayout = DatabaseLayout.LIST,
                tableGroupRulesJson = encodeGroupRules(listOf(GroupRuleSpec("shop"), GroupRuleSpec("kind")))
            )
        )
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "shop", pageId = "spesa", name = "Supermercato", type = ColumnType.SELECT, orderIndex = 0, optionsJson = """[{"label":"IPERPOLI"},{"label":"Lidl"}]"""))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "kind", pageId = "spesa", name = "Tipo", type = ColumnType.SELECT, orderIndex = 1, optionsJson = """[{"label":"Cibo"},{"label":"Casa"}]"""))
        listOf("pasta" to "Pasta", "sale" to "Sale", "shampoo" to "Shampoo").forEachIndexed { i, (id, title) ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "spesa", title = title, orderIndex = i))
        }
        mapOf(
            ("pasta" to "shop") to "IPERPOLI", ("pasta" to "kind") to "Cibo",
            ("sale" to "shop") to "IPERPOLI", ("sale" to "kind") to "Cibo",
            ("shampoo" to "shop") to "Lidl", ("shampoo" to "kind") to "Casa"
        ).forEach { (key, value) -> db.databaseDao().insertCell(DatabaseCellEntity(rowId = key.first, columnId = key.second, value = value)) }
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theListHasTheGroupHeaders() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 700, height = 600, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "spesa", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
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
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            File("build/desktop-screenshots", "list_groups.png").apply { parentFile.mkdirs() }
                .writeBytes(Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            // Le righe di testo chiaro sotto la barra: IPERPOLI, Cibo, Pasta, Sale, Lidl, Casa, Shampoo.
            fun bright(c: Int) = ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120
            val lines = (125 until 600).filter { y -> (0 until 300).any { x -> bright(shot.getColor(x, y)) } }
                .fold(mutableListOf<IntRange>()) { acc, y -> if (acc.isNotEmpty() && acc.last().last >= y - 3) acc[acc.lastIndex] = acc.last().first..y else acc += y..y; acc }
            assertTrue("le righe dell'elenco coi gruppi: $lines", lines.size == 7)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
