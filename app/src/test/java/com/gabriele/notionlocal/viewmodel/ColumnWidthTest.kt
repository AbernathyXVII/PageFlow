package com.gabriele.notionlocal.viewmodel

import android.app.Application
import android.os.Looper
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.ColumnType
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.MAX_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.MIN_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.entity.NAME_COLUMN_KEY
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.encodeColumnWidths
import com.gabriele.notionlocal.data.entity.encodeFitColumns
import com.gabriele.notionlocal.data.entity.parseColumnWidths
import com.gabriele.notionlocal.data.entity.parseFitColumns
import com.gabriele.notionlocal.data.entity.snapColumnWidth
import com.gabriele.notionlocal.data.entity.MIN_REACHABLE_COLUMN_WIDTH_DP
import com.gabriele.notionlocal.data.repository.DatabaseRepository
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.repository.PageRepository.DuplicateTarget
import com.gabriele.notionlocal.ui.i18n.DbStrings
import com.gabriele.notionlocal.ui.screen.cellFitWidth
import com.gabriele.notionlocal.ui.screen.fitClamp
import com.gabriele.notionlocal.ui.screen.nameFitWidth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * **La larghezza delle colonne** (29/09/2026): col segnalino si sceglie una
 * misura, "Fit to content" la adatta a quello che c'è scritto. Ogni vista
 * ha le sue.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ColumnWidthTest {

    private lateinit var db: AppDatabase
    private lateinit var pages: PageRepository
    private lateinit var viewModel: DatabaseViewModel

    private val home = PageEntity(title = "Pagina")
    private val games = PageEntity(title = "Giochi", isDatabase = true)
    private val platform = DatabaseColumnEntity(pageId = games.id, name = "Platform", type = ColumnType.SELECT)

    @Before
    fun setUp() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pages = PageRepository(db)
        pages.createPage(home)
        pages.createPage(games)
        // Il database sta in una pagina, come sempre: la copia gli nasce accanto.
        db.blockDao().insertAll(
            listOf(BlockEntity(pageId = home.id, type = BlockType.DATABASE_LINK, linkedPageId = games.id, orderIndex = 0))
        )
        db.databaseDao().insertColumn(platform)
        viewModel = DatabaseViewModel(DatabaseRepository(db), pages)
        viewModel.load(games.id)
        waitFor("il database") { viewModel.page.value != null }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        fail("Non è successo: $what")
    }

    private fun saved() = runBlocking { db.pageDao().getById(games.id)!! }

    /** Salvato, e tornato al ViewModel: il passo dopo parte da lì, come quando si tocca davvero. */
    private fun settled(what: String, condition: (PageEntity) -> Boolean) = waitFor(what) {
        val page = saved()
        condition(page) && viewModel.page.value?.let {
            it.fitColumns == page.fitColumns && it.columnWidths == page.columnWidths
        } == true
    }

    @Test
    fun widthsAndFitColumnsSurviveTheRoundTrip() {
        val widths = mapOf(NAME_COLUMN_KEY to 240, "abc" to 90)
        assertEquals(widths, parseColumnWidths(encodeColumnWidths(widths)))
        assertNull(encodeColumnWidths(emptyMap()))
        assertEquals(emptyMap<String, Int>(), parseColumnWidths("non è json"))
        assertEquals(setOf("a", NAME_COLUMN_KEY), parseFitColumns(encodeFitColumns(setOf(NAME_COLUMN_KEY, "a"))))
        assertNull(encodeFitColumns(emptySet()))
    }

    @Test
    fun draggingSavesTheWidthForThisViewOnly() {
        viewModel.setColumnWidth(platform.id, 220)
        settled("la prima") { parseColumnWidths(it.columnWidths)[platform.id] == 220 }
        viewModel.setColumnWidth(NAME_COLUMN_KEY, 310)
        settled("salvate") { parseColumnWidths(it.columnWidths) == mapOf(platform.id to 220, NAME_COLUMN_KEY to 310) }
        // Dentro i limiti: a mano fin quasi a niente (dal 18/10/2026), ma ancora da toccare (dal 24/11/2026),
        // e non oltre la massima.
        viewModel.setColumnWidth(platform.id, 40)
        settled("stretta stretta") { parseColumnWidths(it.columnWidths)[platform.id] == 40 }
        viewModel.setColumnWidth(platform.id, -20)
        settled("la più stretta") { parseColumnWidths(it.columnWidths)[platform.id] == MIN_REACHABLE_COLUMN_WIDTH_DP }
        viewModel.setColumnWidth(platform.id, 99_999)
        settled("la massima") { parseColumnWidths(it.columnWidths)[platform.id] == MAX_COLUMN_WIDTH_DP }
    }

    @Test
    fun nearTheUsualWidthItSnapsBackAndNothingIsKept() {
        assertEquals(150, snapColumnWidth(platform.id, 153))
        assertEquals(200, snapColumnWidth(NAME_COLUMN_KEY, 197))
        assertEquals(156, snapColumnWidth(platform.id, 156))
        viewModel.setColumnWidth(platform.id, 90)
        settled("stretta") { parseColumnWidths(it.columnWidths)[platform.id] == 90 }
        // Riportata a mano quasi a 150: torna com'era, e non resta scritto niente.
        viewModel.setColumnWidth(platform.id, 148)
        settled("com'era") { it.columnWidths == null }
    }

    @Test
    fun fitToContentTurnsOnAndOffAndADragTurnsItOff() {
        viewModel.setColumnWidth(platform.id, 220)
        settled("la misura") { parseColumnWidths(it.columnWidths)[platform.id] == 220 }
        viewModel.setFitToContent(platform.id, true)
        settled("acceso") { parseFitColumns(it.fitColumns) == setOf(platform.id) }
        // La misura scelta resta da parte.
        assertEquals(220, parseColumnWidths(saved().columnWidths)[platform.id])
        viewModel.setFitToContent(platform.id, false)
        settled("spento") { it.fitColumns == null }
        viewModel.setFitToContent(NAME_COLUMN_KEY, true)
        settled("acceso su Name") { parseFitColumns(it.fitColumns) == setOf(NAME_COLUMN_KEY) }
        viewModel.setColumnWidth(NAME_COLUMN_KEY, 180)
        waitFor("il trascinamento lo spegne") {
            saved().fitColumns == null && parseColumnWidths(saved().columnWidths)[NAME_COLUMN_KEY] == 180
        }
    }

    @Test
    fun aCopiedDatabaseKeepsTheWidthsOnItsOwnColumns() = runBlocking<Unit> {
        db.pageDao().update(
            saved().copy(
                columnWidths = encodeColumnWidths(mapOf(platform.id to 220, NAME_COLUMN_KEY to 300)),
                fitColumns = encodeFitColumns(setOf(platform.id))
            )
        )
        val copyId = pages.duplicatePage(games.id, DuplicateTarget.NextToOriginal, "copy") { it }!!
        val copyColumn = db.databaseDao().getColumnsForPageOnce(copyId).single()
        val copy = db.pageDao().getById(copyId)!!
        assertEquals(mapOf(copyColumn.id to 220, NAME_COLUMN_KEY to 300), parseColumnWidths(copy.columnWidths))
        assertEquals(setOf(copyColumn.id), parseFitColumns(copy.fitColumns))
    }

    // --- "Fit to content": quanto spazio chiede ogni cella ---

    /** Ogni lettera larga 7 punti: basta per vedere i conti. */
    private val measure: (String, androidx.compose.ui.text.TextStyle) -> androidx.compose.ui.unit.Dp = { text, _ -> (text.length * 7).dp }
    private val typography = Typography()
    private val row = DatabaseRowEntity(pageId = games.id, title = "Zelda", orderIndex = 0)

    @Test
    fun aTextCellAsksForAllItsText() {
        val text = DatabaseColumnEntity(pageId = games.id, name = "Text", type = ColumnType.TEXT)
        // Dieci lettere più il cursore.
        assertEquals(72.dp, cellFitWidth(text, row, "abcdefghij", typography, measure))
    }

    @Test
    fun tagsAskForEveryChip() {
        val tags = DatabaseColumnEntity(pageId = games.id, name = "Tags", type = ColumnType.MULTI_SELECT)
        // "PS5" e "PC": il nome, 8 punti per lato e 4 fra un tag e l'altro.
        assertEquals((21 + 20 + 14 + 20).dp, cellFitWidth(tags, row, "PS5|PC", typography, measure))
        val one = DatabaseColumnEntity(pageId = games.id, name = "One", type = ColumnType.SELECT)
        assertEquals(0.dp, cellFitWidth(one, row, "", typography, measure))
    }

    @Test
    fun theNameColumnCountsTitleAndOpen() {
        val rows = listOf(row, row.copy(id = "altra", title = "The Legend of Zelda"))
        // 12 + titolo più lungo (19 lettere) + 12 + OPEN (le sue lettere + 16 + 8), più il margine.
        val open = DbStrings.open.length * 7 + 24
        assertEquals(fitClamp((12 + 133 + 12 + open).dp), nameFitWidth(rows, simple = false, withIcon = emptySet(), typography = typography, measure = measure))
        // In un database semplice non c'è OPEN.
        assertEquals(fitClamp((12 + 133 + 12).dp), nameFitWidth(rows, simple = true, withIcon = emptySet(), typography = typography, measure = measure))
    }

    @Test
    fun fitStaysWithinTheLimits() {
        assertEquals(MIN_COLUMN_WIDTH_DP.dp, fitClamp(3.dp))
        assertEquals(MAX_COLUMN_WIDTH_DP.dp, fitClamp(50_000.dp))
    }
}
