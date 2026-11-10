package com.gabriele.notionlocal.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.sync.SyncDatabase
import com.gabriele.notionlocal.data.sync.SyncPlan
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La sincronizzazione col database del telefono** (dal 10/11/2026): sul
 * telefono Room non ha il driver del PC, e leggere e scrivere le righe con
 * l'SQL (`SyncDatabase`) deve andare lo stesso. Una pagina con un database
 * passa da un telefono finto all'altro, poi una pagina va nel cestino.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SyncPhoneTest {
    private fun newDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()

    private val one = newDb()
    private val two = newDb()

    @After
    fun tearDown() {
        one.close()
        two.close()
    }

    @Test
    fun aPageAndADatabaseGoFromOneSideToTheOther() = runBlocking<Unit> {
        val repo = PageRepository(one)
        repo.createPage(PageEntity(id = "casa", title = "Casa"))
        repo.saveBlock(BlockEntity(id = "b1", pageId = "casa", textJson = """[{"text":"bollette"}]""", orderIndex = 0))
        repo.createPage(PageEntity(id = "spese", title = "Spese", isDatabase = true))
        repo.saveBlock(BlockEntity(id = "b2", pageId = "casa", linkedPageId = "spese", orderIndex = 1))
        one.databaseDao().insertColumn(DatabaseColumnEntity(id = "importo", pageId = "spese", name = "Importo"))
        one.databaseDao().insertRow(DatabaseRowEntity(id = "riga", pageId = "spese"))

        val plan = SyncPlan.of(SyncDatabase.manifest(one), SyncDatabase.manifest(two), emptyMap())
        assertEquals(setOf("p:casa", "p:spese", "r:riga"), plan.send)
        SyncDatabase.apply(two, SyncDatabase.readUnits(one, plan.send).values)
        assertEquals(SyncDatabase.manifest(one).mapValues { it.value.hash }, SyncDatabase.manifest(two).mapValues { it.value.hash })
        assertEquals("spese", two.blockDao().getBlocksForPageOnce("casa").single { it.orderIndex == 1 }.linkedPageId)

        SyncDatabase.apply(two, emptyList(), toTrash = listOf("p:spese"))
        assertNotNull(two.pageDao().getById("spese")?.trashedAt)
        assertTrue("il collegamento se ne va col cestino", two.blockDao().getBlocksLinkingTo("spese").isEmpty())
    }
}
