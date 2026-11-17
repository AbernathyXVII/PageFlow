package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
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
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.DatabaseViewScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import com.gabriele.notionlocal.viewmodel.orderWithShown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId

/**
 * **Nel calendario le proprietà sotto il nome degli eventi** (17/11/2026,
 * chiesto dall'utente: "fai come fa Notion, che nella vista Calendar posso
 * vedere i valori delle proprietà, in ordine manuale che si può decidere
 * sempre dentro Property visibility"): l'evento di oggi ha il nome e sotto
 * Luogo e Note (non la data: dice già dove sta); nascosta Note, resta Luogo.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCalendarPropertiesTest {
    private val dir: File = Files.createTempDirectory("pageflow-calprops").toFile()
    private val context = DesktopContext(dir)
    private val today = LocalDate.now()
    private lateinit var db: AppDatabase

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    private val note = DatabaseColumnEntity(id = "note", pageId = "eventi", name = "Note", type = ColumnType.TEXT, orderIndex = 2)

    @Before
    fun setUp() = runBlocking {
        // Le finestre del PC si aprono accanto all'ultimo clic, anche di un altro test: qui al centro.
        com.gabriele.notionlocal.ui.screen.LastPress.point = null
        AppSettings.init(context)
        db = AppDatabase.getInstance(context)
        PageRepository(db).createPage(PageEntity(id = "eventi", title = "EVENTI", isDatabase = true, databaseLayout = DatabaseLayout.CALENDAR))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "eventi", name = "Date", type = ColumnType.DATE, orderIndex = 0))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "luogo", pageId = "eventi", name = "Luogo", type = ColumnType.TEXT, orderIndex = 1))
        db.databaseDao().insertColumn(note)
        fun millis(day: LocalDate) = day.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.databaseDao().insertRow(DatabaseRowEntity(id = "riunione", pageId = "eventi", title = "Riunione", orderIndex = 0))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "riunione", columnId = "quando", value = millis(today).toString()))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "riunione", columnId = "luogo", value = "Roma"))
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "riunione", columnId = "note", value = "Portare i fogli"))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    @Test
    fun theOrderOfPropertyVisibilityKeepsTheHiddenInTheirPlace() {
        // Name, A, (B nascosta), C: trascinata C in cima, B resta terza.
        assertEquals(listOf("c", NAME_COLUMN_KEY, "b", "a"), orderWithShown(listOf(NAME_COLUMN_KEY, "a", "b", "c"), listOf("c", NAME_COLUMN_KEY, "a")))
        assertNull("una chiave che non c'è", orderWithShown(listOf("a", "b"), listOf("b", "x")))
    }

    @Test
    fun thePropertiesAreUnderTheNameOfTheEvent() {
        val factory = ViewModelFactory(context)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1100, height = 1000, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        Box(Modifier.fillMaxSize()) {
                            DatabaseViewScreen(
                                pageId = "eventi", factory = factory, onBack = {}, onOpenRowPage = {}, onOpenSearch = {},
                                onOpenUpdates = {}, onOpenPage = {}, onOpenDatabase = {}
                            )
                        }
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
        fun shot(name: String): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }.also {
            File("build/desktop-screenshots", "$name.png").apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        try {
            frames(50)
            val first = shot("calendar_properties")
            // Il numero di oggi è blu (come in DesktopCalendarTest): lì sotto c'è il suo evento.
            val anyBlue = (150 until first.height).flatMap { y ->
                (0 until first.width).filter { x -> first.getColor(x, y).let { c -> ((c shr 16) and 0xFF) < 100 && (c and 0xFF) > 200 && ((c shr 8) and 0xFF) in 100..180 } }.map { x -> x to y }
            }
            val blue = anyBlue.filter { it.second <= anyBlue.minOf { p -> p.second } + 12 }
            assertTrue("il numero di oggi", blue.isNotEmpty())
            val dayX = blue.map { it.first }.average().toInt()
            val dayBottom = blue.maxOf { it.second }
            /** Le righe con del testo chiaro nella colonna di oggi, sotto il suo numero e prima dei numeri della settimana dopo. */
            fun lines(bitmap: Bitmap): List<IntRange> {
                fun bright(c: Int) = ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120
                val rows = (dayBottom + 3 until minOf(dayBottom + 300, bitmap.height)).filter { y -> (dayX - 50 until dayX + 50).any { x -> bright(bitmap.getColor(x, y)) } }
                val bands = ArrayList<IntRange>()
                for (y in rows) {
                    val last = bands.lastOrNull()
                    // I puntini delle "i" stanno un pixel sopra le lettere: sono la stessa riga.
                    if (last != null && y <= last.last + 3) bands[bands.lastIndex] = last.first..y else bands += y..y
                }
                val neighbor = if (dayX > bitmap.width / 2) dayX - 138 else dayX + 138
                return bands.takeWhile { band -> band.none { y -> (neighbor - 40 until neighbor + 40).any { x -> bright(bitmap.getColor(x, y)) } } }
            }
            val shown = lines(first)
            assertEquals("il nome, Luogo e Note: $shown", 3, shown.size)
            // Note nascosta in Property visibility: sotto il nome resta Luogo.
            runBlocking { db.databaseDao().updateColumn(note.copy(hidden = true)) }
            frames(20)
            val hidden = lines(shot("calendar_properties_hidden"))
            assertEquals("il nome e Luogo: $hidden", 2, hidden.size)
            // Property visibility, dalle impostazioni della vista.
            fun click(x: Float, y: Float) = onWindowThread {
                val at = androidx.compose.ui.geometry.Offset(x, y)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Move, at, type = androidx.compose.ui.input.pointer.PointerType.Mouse)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, at, type = androidx.compose.ui.input.pointer.PointerType.Mouse, buttons = androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, at, type = androidx.compose.ui.input.pointer.PointerType.Mouse, buttons = androidx.compose.ui.input.pointer.PointerButtons(), button = androidx.compose.ui.input.pointer.PointerButton.Primary)
            }
            runBlocking { db.databaseDao().updateColumn(note) }
            frames(10)
            click(1003f, 99f)
            frames(15)
            shot("calendar_properties_settings")
            click(488f, 561f)
            frames(15)
            val visibility = shot("calendar_properties_visibility")
            // Le tre lineette di Note (l'ultima riga), trascinate sopra Luogo: l'ordine cambia anche sugli eventi.
            val handleX = 667
            val handles = (300 until 700).filter { y -> (handleX - 8..handleX + 8).count { x -> visibility.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 120 } } > 8 }
            val noteY = handles.last().toFloat()
            fun drag(type: androidx.compose.ui.input.pointer.PointerEventType, y: Float, pressed: Boolean) = onWindowThread {
                scene.sendPointerEvent(
                    type, androidx.compose.ui.geometry.Offset(handleX.toFloat(), y), type = androidx.compose.ui.input.pointer.PointerType.Mouse,
                    buttons = if (pressed) androidx.compose.ui.input.pointer.PointerButtons(isPrimaryPressed = true) else androidx.compose.ui.input.pointer.PointerButtons(),
                    button = if (type == androidx.compose.ui.input.pointer.PointerEventType.Move) null else androidx.compose.ui.input.pointer.PointerButton.Primary
                )
            }
            drag(androidx.compose.ui.input.pointer.PointerEventType.Move, noteY, false)
            drag(androidx.compose.ui.input.pointer.PointerEventType.Press, noteY, true)
            frames(2)
            var y = noteY
            while (y > noteY - 75) {
                y -= 8f
                drag(androidx.compose.ui.input.pointer.PointerEventType.Move, y, true)
                frames(1)
            }
            drag(androidx.compose.ui.input.pointer.PointerEventType.Release, y, false)
            frames(15)
            shot("calendar_properties_reordered")
            val order = runBlocking { db.databaseDao().getColumnsForPageOnce("eventi").sortedBy { it.orderIndex }.map { it.id } }
            assertEquals("Note prima di Luogo", listOf("quando", "note", "luogo"), order)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
