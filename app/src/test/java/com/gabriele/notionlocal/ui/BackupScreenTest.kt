package com.gabriele.notionlocal.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.backup.BackupReason
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.backup.DirectoryStorage
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.screen.BackupScreen
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * **La pagina Backup** (28/10/2026): com'è adesso, "Fai un backup adesso",
 * il promemoria, "Carica su Proton Drive" che apre "Salva come" col nome già
 * scritto (e la seconda volta dove si è salvato), il ripristino dal menu di
 * un backup che prepara i file e riapre l'app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class BackupScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var folder: File
    private var restarted = false

    @Before
    fun setUp() {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath(Backups.DATABASE_NAME).delete()
        context.getSharedPreferences("backup", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking { AppDatabase.getInstance(context).pageDao().insert(PageEntity(id = "p", title = "Pagina")) }
        folder = File(context.cacheDir, "screen-backups").apply { deleteRecursively(); mkdirs() }
        Backups.storageOverride = DirectoryStorage(folder)
        Backups.init(context)
    }

    @After
    fun tearDown() {
        compose.waitForIdle()
        Thread.sleep(200)
        AppDatabase.resetForTests()
        Backups.storageOverride = null
        File(context.filesDir, "restore-pending").deleteRecursively()
    }

    private fun open() {
        compose.setContent { NotionLocalTheme { BackupScreen(onBack = {}, onRestart = { restarted = true }) } }
        compose.waitForIdle()
    }

    private fun shown(text: String, substring: Boolean = false) =
        compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(8_000) {
                Thread.sleep(20)
                condition()
            }
        } catch (e: Throwable) {
            throw AssertionError("Non è successo: $what", e)
        }
        compose.waitForIdle()
    }

    @Test
    fun withoutBackupsItSaysSoAndBackUpNowMakesOne() {
        open()
        assertTrue(shown("No backup yet"))
        // Dal 18/11/2026 niente più caricamento su Proton Drive: basta "Back up now".
        assertFalse(shown("Upload to Proton Drive"))
        assertTrue(shown("There are no backups on this phone yet."))
        compose.onNodeWithText("Back up now").performClick()
        waitFor("il backup") { Backups.list().size == 1 }
        waitFor("il backup nell'elenco") { compose.onAllNodesWithText("Today at", substring = true).fetchSemanticsNodes().size >= 2 }
        assertFalse(shown("No backup yet"))
    }

    @Test
    fun theReminderAndTheDailyBackupAreChosenHere() {
        open()
        assertTrue(shown("After 7 days"))
        compose.onNodeWithText("Backup reminder").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("After 14 days").performClick()
        compose.waitForIdle()
        assertEquals(14, Backups.remindDays)
        assertTrue(shown("After 14 days"))
        compose.onNodeWithText("Daily backup").performClick()
        compose.waitForIdle()
        assertFalse(Backups.autoBackup)
        // Si ricorda.
        Backups.init(context)
        assertEquals(14, Backups.remindDays)
        assertFalse(Backups.autoBackup)
    }

    @Test
    fun restoreFromTheMenuAsksThenPreparesAndRestarts() {
        runBlocking { Backups.backupNow(BackupReason.DAILY) }
        open()
        waitFor("il backup nell'elenco") { compose.onAllNodesWithContentDescription("Options").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription("Options").onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        // La voce del menu: anche il titolo della sezione si chiama "Restore", ma non si tocca.
        compose.onNode(hasText("Restore") and hasClickAction()).performClick()
        waitFor("la domanda") { shown("Restore this backup?") }
        assertTrue(shown("First a backup of now is made", substring = true))
        // La data vera del backup, non la scritta dell'elenco ("Today at…").
        val date = Formats.dateTime(Backups.list().first().createdAt)
        assertTrue(shown("the backup of $date.", substring = true))
        compose.onNode(hasText("Restore") and hasClickAction()).performClick()
        waitFor("la riapertura") { restarted }
        assertTrue(File(context.filesDir, "restore-pending/ready").exists())
        // Il backup di prima del ripristino c'è.
        assertTrue(Backups.list().any { it.name.contains("before-restore") })
    }
}
