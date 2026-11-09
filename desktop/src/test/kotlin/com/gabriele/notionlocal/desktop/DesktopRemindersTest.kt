package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.notify.DesktopReminders
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

/**
 * **I promemoria sul PC** (dal 09/11/2026): una riga "/notification" arriva
 * una volta sola, quando è l'ora; una persa da più di 12 ore (PC spento) non
 * arriva più; alla prima apertura le date già passate non arrivano tutte insieme.
 */
class DesktopRemindersTest {
    private val dir = Files.createTempDirectory("pageflow-reminders").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        dir.deleteRecursively()
    }

    private fun reminderAt(at: Long, text: String) = runBlocking {
        val repo = PageRepository(AppDatabase.getInstance(context))
        val page = PageEntity(title = "Paragraph – prova")
        repo.createPage(page)
        repo.saveBlock(
            BlockEntity(pageId = page.id, type = BlockType.NOTIFICATION, textJson = """[{"text":"$text"}]""", orderIndex = 0, notifyAt = at)
        )
    }

    private fun due(now: Long) = runBlocking { DesktopReminders.takeDue(context, now).map { it.title } }

    @Test
    fun eachReminderArrivesOnceAtItsTime() {
        AppSettings.init(context)
        val start = 1_800_000_000_000L
        reminderAt(start - 60_000, "già passato")
        reminderAt(start + 60_000, "fra un minuto")

        assertEquals("la prima volta il passato non arriva", emptyList<String>(), due(start))
        assertEquals(emptyList<String>(), due(start + 30_000))
        assertEquals(listOf("fra un minuto"), due(start + 90_000))
        assertEquals("niente doppioni", emptyList<String>(), due(start + 120_000))
    }

    @Test
    fun aReminderMissedForMoreThanTwelveHoursIsDropped() {
        AppSettings.init(context)
        val start = 1_800_000_000_000L
        val hour = 60 * 60 * 1000L
        reminderAt(start + hour, "perso")
        reminderAt(start + 2 * hour, "recente")

        due(start)
        // Il PC riacceso 13 ore dopo il primo e 12 dopo il secondo.
        assertEquals(listOf("recente"), due(start + 14 * hour))
    }
}
