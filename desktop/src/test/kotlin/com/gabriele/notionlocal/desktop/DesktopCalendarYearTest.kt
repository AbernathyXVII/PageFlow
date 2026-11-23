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
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/**
 * **La vista Year come Google Calendar** (dal 23/11/2026, chiesto dall'utente con una foto):
 * quattro mesi per riga sul PC e due quando lo spazio è poco, il nome intero dei mesi, i
 * numeri delle settimane a sinistra e le sei righe di giorni.
 */
@OptIn(ExperimentalComposeUiApi::class, InternalComposeUiApi::class)
class DesktopCalendarYearTest {
    private val dir: File = Files.createTempDirectory("pageflow-year").toFile()
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
        repo.createPage(
            PageEntity(
                id = "eventi", title = "EVENTI", isDatabase = true, parentId = "agenda",
                databaseLayout = DatabaseLayout.CALENDAR, calendarMode = CalendarMode.YEAR
            )
        )
        db.databaseDao().insertColumn(DatabaseColumnEntity(id = "quando", pageId = "eventi", name = "Date", type = ColumnType.DATE, orderIndex = 0))
        db.databaseDao().insertRow(DatabaseRowEntity(id = "e0", pageId = "eventi", title = "Oggi", orderIndex = 0))
        val at = today.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.databaseDao().insertCell(DatabaseCellEntity(rowId = "e0", columnId = "quando", value = at.toString()))
        repo.saveBlock(BlockEntity(id = "cal", pageId = "agenda", type = BlockType.DATABASE_LINK, linkedPageId = "eventi", orderIndex = 0))
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    /** I testi sullo schermo largo [width], con dove stanno. */
    private fun texts(width: Int, height: Int, name: String): List<Pair<String, Rect>> {
        val factory = ViewModelFactory(context)
        val viewModel = factory.create(PageEditorViewModel::class, CreationExtras.Empty)
        val owner = ScreenOwner()
        val scene = onWindowThread {
            ImageComposeScene(width = width, height = height, density = Density(1f)) {
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
            File("build/desktop-screenshots").mkdirs()
            File("build/desktop-screenshots", "$name.png").writeBytes(org.jetbrains.skia.Image.makeFromBitmap(shot).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            return onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }?.let { it to node.boundsInRoot } }
            }
        } finally {
            onWindowThread { scene.close() }
        }
    }

    private fun List<Pair<String, Rect>>.at(text: String): Rect =
        firstOrNull { it.first == text }?.second ?: throw AssertionError("manca \"$text\" fra ${map { it.first }.take(80)}")

    @Test
    fun fourMonthsARowWithTheirWeekNumbers() {
        val shown = texts(1100, 1400, "calendar_year_pc")
        val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val january = shown.at("January")
        assertEquals("aprile sulla riga di gennaio", january.top, shown.at("April").top, 2f)
        assertTrue("maggio sotto", shown.at("May").top > january.bottom + 100)
        assertEquals("dicembre sulla riga di settembre", shown.at("September").top, shown.at("December").top, 2f)
        months.forEach { shown.at(it) }
        assertTrue("le iniziali dei giorni in tutti i mesi", shown.count { it.first == "M" } >= 12)

        // Le sei settimane di gennaio, una sotto l'altra, nel riquadro a sinistra dei giorni.
        val firstMonday = LocalDate.of(today.year, 1, 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val expected = (0L until 6L).map { firstMonday.plusWeeks(it).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString() }
        val weekColumn = shown.filter { (_, r) -> r.center.x in january.left..(january.left + 24f) && r.top > january.bottom + 15f && r.top < january.bottom + 170f }
            .sortedBy { it.second.top }.map { it.first }
        assertEquals("i numeri delle settimane di gennaio", expected, weekColumn)

        // Il primo lunedì della griglia c'è anche se è di dicembre: le sei righe partono da lì.
        val firstDay = shown.filter { (text, r) -> text == firstMonday.dayOfMonth.toString() && r.center.x in (january.left + 24f)..(january.left + 60f) && r.top > january.bottom + 15f }
        assertTrue("il ${firstMonday.dayOfMonth} in alto a sinistra di gennaio", firstDay.isNotEmpty())
    }

    @Test
    fun twoMonthsARowWhenItIsNarrow() {
        val shown = texts(420, 1900, "calendar_year_narrow")
        val january = shown.at("January")
        assertEquals("febbraio accanto a gennaio", january.top, shown.at("February").top, 2f)
        assertTrue("marzo sotto", shown.at("March").top > january.bottom + 100)
    }
}
