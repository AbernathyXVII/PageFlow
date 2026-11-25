package com.gabriele.notionlocal.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
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
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.CalendarMode
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
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * **La testa dei calendari e i giorni degli altri mesi** (25/11/2026, chiesti dall'utente): le frecce
 * accanto al titolo, Year/Month/Week/Day al centro della riga, e nella vista Month i giorni del mese
 * prima e dopo scuriti con i loro eventi.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCalendarHeaderTest {
    private val dir: File = Files.createTempDirectory("pageflow-calhead").toFile()
    private val context = DesktopContext(dir)
    private val today = LocalDate.now()
    private val first = today.withDayOfMonth(1)
    private val gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private var shot: Bitmap? = null

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
        repo.createPage(
            PageEntity(
                id = "eventi", title = "EVENTI", isDatabase = true, parentId = "agenda",
                databaseLayout = DatabaseLayout.CALENDAR, calendarMode = CalendarMode.MONTH
            )
        )
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "eventi", name = "Date", type = ColumnType.DATE, orderIndex = 0))
        // Uno nel mese e uno in un giorno del mese prima, nella prima settimana della griglia.
        listOf("dentro" to first.plusDays(14), "fuori" to gridStart).forEachIndexed { i, (id, day) ->
            db.databaseDao().insertRow(DatabaseRowEntity(id = id, pageId = "eventi", title = id.uppercase(), orderIndex = i))
            val at = day.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.databaseDao().insertCell(DatabaseCellEntity(rowId = id, columnId = "quando", value = at.toString()))
        }
        repo.saveBlock(BlockEntity(id = "cal", pageId = "agenda", type = BlockType.DATABASE_LINK, linkedPageId = "eventi", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    /** I testi e le descrizioni sullo schermo largo [width], con dove stanno. */
    private fun texts(width: Int, name: String): List<Pair<String, Rect>> {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = width, height = 900, density = Density(1f)) {
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
        try {
            var time = 0L
            repeat(50) {
                onWindowThread { scene.render(time) }
                time += 100_000_000L
                Thread.sleep(30)
            }
            val shot = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
            this.shot = shot
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            return onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }.mapNotNull { node ->
                    (node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
                        ?: node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(""))?.let { it to node.boundsInRoot }
                }
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun List<Pair<String, Rect>>.at(text: String): Rect =
        lastOrNull { it.first == text }?.second ?: throw AssertionError("manca \"$text\" fra ${map { it.first }.take(80)}")

    @Test
    fun arrowsNextToTheTitleAndTheModesInTheMiddle() {
        val shown = texts(1100, "calendar_header_pc")
        val title = shown.single { it.first.endsWith(today.year.toString()) && it.first.length > 4 }.second
        val previous = shown.at("Previous")
        val next = shown.at("Next")
        assertTrue("la freccia indietro accanto al titolo: ${title.left - previous.right}", title.left - previous.right in 0f..24f)
        assertTrue("la freccia avanti accanto al titolo: ${next.left - title.right}", next.left - title.right in 0f..24f)
        val modes = (shown.at("Day").right + shown.at("Year").left) / 2
        assertEquals("Year…Day al centro, sotto il titolo", title.center.x, modes, 4f)

        // Stretto come un telefono: i pulsanti non finiscono sotto Today.
        val narrow = texts(380, "calendar_header_narrow")
        assertTrue("Day prima di Today", narrow.at("Day").right < narrow.at("Today").left)
        assertTrue("Year dentro lo schermo", narrow.at("Year").left >= 0f)
    }

    @Test
    fun theOtherMonthsEventsAreDarker() {
        assumeTrue("il mese comincia di lunedì: nessun giorno del mese prima", gridStart < first)
        val shown = texts(1100, "calendar_month_other_days")
        fun brightest(r: Rect) = (r.top.toInt()..r.bottom.toInt()).maxOf { y ->
            (r.left.toInt()..r.right.toInt()).maxOf { x -> shot!!.getColor(x, y).let { c -> maxOf((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF) } }
        }
        val inside = brightest(shown.at("DENTRO"))
        val outside = brightest(shown.at("FUORI"))
        assertTrue("l'evento del mese prima più scuro: $outside contro $inside", outside < inside - 60)
    }
}
