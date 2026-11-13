package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
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
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId

/**
 * **Il calendario sul PC** (dal 13/11/2026, chiesto dall'utente): settimane
 * compatte con al massimo cinque eventi per giorno e "Show more"/"Show less",
 * il lunedì come primo giorno, sabato e domenica in grigio.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCalendarTest {
    private val dir: File = Files.createTempDirectory("pageflow-calendar").toFile()
    private val context = DesktopContext(dir)
    private val today = LocalDate.now()

    private class ScreenOwner : LifecycleOwner, ViewModelStoreOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() = runBlocking {
        AppSettings.init(context)
        val db = AppDatabase.getInstance(context)
        val repo = PageRepository(db)
        repo.createPage(PageEntity(id = "agenda", title = "Agenda"))
        repo.createPage(PageEntity(id = "eventi", title = "EVENTI", isDatabase = true, parentId = "agenda", databaseLayout = DatabaseLayout.CALENDAR))
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "eventi", name = "Date", type = ColumnType.DATE, orderIndex = 0))
        fun millis(day: LocalDate, hour: Int) = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Otto eventi oggi, due in un altro giorno dello stesso mese.
        val other = if (today.dayOfMonth > 15) today.minusDays(9) else today.plusDays(9)
        val events = (1..8).map { "Evento $it" to millis(today, 8 + it) } + listOf("Altro 1" to millis(other, 9), "Altro 2" to millis(other, 10))
        events.forEachIndexed { i, (title, at) ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = "e$i", pageId = "eventi", title = title, orderIndex = i))
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = "e$i", columnId = "quando", value = at.toString()))
        }
        repo.saveBlock(BlockEntity(id = "cal", pageId = "agenda", type = BlockType.DATABASE_LINK, linkedPageId = "eventi", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    /**
     * **Cinque eventi al giorno, poi "Show more"** (chiesto dall'utente il
     * 13/11/2026): oggi ha otto eventi; se ne vedono cinque e "Show more";
     * un clic e si vedono tutti, con "Show less"; un altro e si richiude. La
     * settimana comincia di lunedì: sabato e domenica, grigi, sono le ultime
     * due colonne.
     */
    @Test
    fun fiveEventsADayThenShowMore() {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = 1100, height = 1300, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context, LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                    NotionLocalTheme {
                        PageEditorScreen(
                            pageId = "agenda", factory = factory, onBack = {}, onOpenSearch = {}, onOpenUpdates = {},
                            onNavigateToPage = {}, onNavigateToDatabase = {}, viewModel = viewModel
                        )
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
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(it).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        fun click(at: Offset) = onWindowThread {
            scene.sendPointerEvent(PointerEventType.Move, at, type = PointerType.Mouse)
            scene.sendPointerEvent(PointerEventType.Press, at, type = PointerType.Mouse, buttons = PointerButtons(isPrimaryPressed = true), button = PointerButton.Primary)
            scene.sendPointerEvent(PointerEventType.Release, at, type = PointerType.Mouse, buttons = PointerButtons(), button = PointerButton.Primary)
        }
        try {
            frames(50)
            val first = shot("calendar_pc")
            // Il numero di oggi è blu: lì sopra c'è la sua colonna.
            val anyBlue = (330 until first.height).flatMap { y ->
                (0 until first.width).filter { x -> first.getColor(x, y).let { c -> ((c shr 16) and 0xFF) < 100 && (c and 0xFF) > 200 && ((c shr 8) and 0xFF) in 100..180 } }.map { x -> x to y }
            }
            // Il blu più in alto (sotto c'è anche "New page", dello stesso colore).
            val blue = anyBlue.filter { it.second <= anyBlue.minOf { p -> p.second } + 12 }
            assertTrue("il numero di oggi", blue.isNotEmpty())
            val dayX = blue.map { it.first }.average().toInt()
            val dayBottom = blue.maxOf { it.second }
            /** Le righe con del testo nella colonna di oggi, sotto il suo numero: le barre e "Show more". */
            fun lines(bitmap: Bitmap): List<IntRange> {
                val rows = (dayBottom + 3 until minOf(dayBottom + 400, bitmap.height)).filter { y ->
                    (dayX - 60 until dayX + 60).any { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120 } }
                }
                val bands = ArrayList<IntRange>()
                for (y in rows) {
                    val last = bands.lastOrNull()
                    if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
                }
                // Non i numeri dei giorni (della settimana dopo): quelli hanno del testo anche nella colonna accanto.
                val neighbor = if (dayX > bitmap.width / 2) dayX - 138 else dayX + 138
                fun neighborHasText(band: IntRange) = band.any { y ->
                    (neighbor - 40 until neighbor + 40).any { x -> bitmap.getColor(x, y).let { c -> ((c shr 16) and 0xFF) > 120 && ((c shr 8) and 0xFF) > 120 && (c and 0xFF) > 120 } }
                }
                return bands.takeWhile { !neighborHasText(it) }
            }
            val closed = lines(first)
            assertEquals("cinque eventi e \"Show more\": $closed", 6, closed.size)
            click(Offset(dayX - 40f, (closed[5].first + closed[5].last) / 2f))
            frames(10)
            val open = lines(shot("calendar_pc_more"))
            assertEquals("otto eventi e \"Show less\": $open", 9, open.size)
            click(Offset(dayX - 40f, (open[8].first + open[8].last) / 2f))
            frames(10)
            assertEquals("di nuovo cinque e \"Show more\"", 6, lines(shot("calendar_pc_less")).size)
            // Sabato e domenica (le ultime due colonne) più grigi di lunedì (la prima): il calendario va
            // da un margine all'altro della pagina, e sopra il numero di oggi c'è la riga dei giorni.
            val headerY = blue.minOf { it.second } - 20
            val background = first.getColor(first.width - 5, first.height - 5)
            val tinted = (0 until first.width).filter { x -> first.getColor(x, headerY + 40) != background && first.getColor(x, headerY + 40).let { c -> ((c shr 16) and 0xFF) < 70 } }
            assertTrue("colonne grigie trovate", tinted.isNotEmpty())
            val calendarRight = tinted.max()
            val calendarLeftGuess = tinted.min()
            assertTrue("le colonne grigie sono le ultime due, a destra: da $calendarLeftGuess a $calendarRight", calendarLeftGuess > first.width / 2)
        } finally {
            onWindowThread { scene.close() }
        }
    }
}
