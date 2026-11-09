package com.gabriele.notionlocal.desktop

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.DATABASE_VERSION
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.backup.BackupArchive
import com.gabriele.notionlocal.data.backup.BackupException
import com.gabriele.notionlocal.data.backup.BackupManifest
import com.gabriele.notionlocal.data.backup.BackupReason
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.backup.PrefValue
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.settings.openSettingsStore
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * **I backup sul PC** (dal 09/11/2026). Un backup del telefono (il file zip
 * fatto con lo stesso codice, `BackupArchive.write`) prende il posto del
 * database, delle immagini e delle impostazioni del PC; prima si fa un backup
 * di com'era, e quello che il PC aveva resta anche in `before-restore-<ora>`.
 * Uno di una versione più vecchia, o un file che non è un backup, non tocca
 * niente. Un backup fatto sul PC torna com'era.
 */
class DesktopBackupTest {
    private val phoneDir: File = Files.createTempDirectory("pageflow-phone").toFile()
    private val pcDir: File = Files.createTempDirectory("pageflow-pc").toFile()
    private val backupsDir: File = Files.createTempDirectory("pageflow-backups").toFile()
    private val phone = DesktopContext(phoneDir)
    private val pc = DesktopContext(pcDir)

    @Before
    fun setUp() {
        // Mai i Documenti veri.
        Backups.folderOverride = backupsDir
        Backups.init(pc)
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        Backups.folderOverride = null
        phoneDir.deleteRecursively()
        pcDir.deleteRecursively()
        backupsDir.deleteRecursively()
    }

    private fun pages(context: DesktopContext) = PageRepository(AppDatabase.getInstance(context))

    /** Il backup "del telefono": una pagina, un'immagine, un'impostazione. */
    private fun phoneBackup(): File = runBlocking {
        pages(phone).createPage(PageEntity(id = "dal-telefono", title = "Lista della spesa"))
        AppDatabase.closeInstance()
        File(phoneDir, PageImageStore.DIRECTORY).apply { mkdirs() }.resolve("foto.png").writeText("immagine")
        File(phoneDir, "backup.zip").also { zip ->
            zip.outputStream().use {
                BackupArchive.write(
                    it,
                    BackupManifest(createdAt = 1_000L, databaseVersion = DATABASE_VERSION),
                    database = File(phoneDir, "notion_local.db"),
                    filesDir = File(phoneDir, PageImageStore.DIRECTORY),
                    prefs = mapOf("settings" to mapOf("theme" to PrefValue("string", "DARK"), "zoom" to PrefValue("int", "120")))
                )
            }
        }
    }

    @Test
    fun aPhoneBackupTakesThePlaceOfWhatThePcHad() = runBlocking {
        val backup = phoneBackup()
        // Il PC ha già qualcosa di suo.
        pages(pc).createPage(PageEntity(id = "del-pc", title = "Appunti del PC"))
        AppDatabase.closeInstance()
        openSettingsStore(pc, "settings").edit().putString("theme", "LIGHT").apply()

        assertEquals(1_000L, Backups.prepareRestore { backup.inputStream() }.createdAt)
        // Prima un backup di com'era il PC, per poter tornare indietro.
        assertTrue(Backups.list().single().name.endsWith("-before-restore.zip"))
        assertTrue(Backups.applyPendingRestore(pc, now = 42L))

        val repo = pages(pc)
        assertEquals("Lista della spesa", repo.getPage("dal-telefono")?.title)
        assertNull(repo.getPage("del-pc"))
        assertEquals("immagine", File(pcDir, "${PageImageStore.DIRECTORY}/foto.png").readText())
        assertEquals("DARK", openSettingsStore(pc, "settings").getString("theme", null))
        assertEquals(120, openSettingsStore(pc, "settings").getInt("zoom", 0))

        // Quello di prima, da parte.
        val before = File(pcDir, "before-restore-42")
        assertTrue(File(before, "notion_local.db").exists())
        assertTrue(File(before, "settings.properties").readText().contains("LIGHT"))
        // Una volta sola.
        assertFalse(Backups.applyPendingRestore(pc))
    }

    @Test
    fun anOlderBackupIsRefusedAndNothingChanges() {
        val old = File(phoneDir, "vecchio.db")
        BundledSQLiteDriver().open(old.path).use { db ->
            db.execSQL("CREATE TABLE t (x INTEGER)")
            db.execSQL("PRAGMA user_version = ${DATABASE_VERSION - 1}")
        }
        val zip = File(phoneDir, "vecchio.zip")
        zip.outputStream().use {
            BackupArchive.write(it, BackupManifest(createdAt = 1L, databaseVersion = DATABASE_VERSION - 1), old, File(phoneDir, "niente"), emptyMap())
        }
        try {
            runBlocking { Backups.prepareRestore { zip.inputStream() } }
            fail("un backup più vecchio non deve aprirsi sul PC")
        } catch (e: BackupException) {
            assertEquals(BackupException.Problem.TOO_OLD, e.problem)
        }
        assertTrue(Backups.list().isEmpty())
        assertFalse(Backups.applyPendingRestore(pc))
        assertFalse(File(pcDir, "notion_local.db").exists())
    }

    @Test
    fun aFileThatIsNotABackupIsRefused() {
        val notZip = File(phoneDir, "foto.zip").apply { writeText("non sono uno zip") }
        val problem = try {
            runBlocking { Backups.prepareRestore { notZip.inputStream() } }
            null
        } catch (e: BackupException) {
            e.problem
        }
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem)
        assertNotNull(pcDir.listFiles())
        assertFalse(Backups.applyPendingRestore(pc))
    }

    @Test
    fun aPcBackupBringsBackWhatThePcHad() = runBlocking {
        pages(pc).createPage(PageEntity(id = "nota", title = "Prima del backup"))
        File(pcDir, PageImageStore.DIRECTORY).apply { mkdirs() }.resolve("foto.png").writeText("immagine")
        openSettingsStore(pc, "settings").edit().putBoolean("muted", true).apply()

        val made = Backups.backupNow(BackupReason.MANUAL)
        assertEquals(listOf(made.name), Backups.list().map { it.name })
        assertTrue(Backups.lastBackupAt > 0)

        // Dopo il backup cambia qualcosa; il ripristino riporta tutto com'era.
        pages(pc).createPage(PageEntity(id = "dopo", title = "Dopo il backup"))
        File(pcDir, "${PageImageStore.DIRECTORY}/foto.png").delete()
        openSettingsStore(pc, "settings").edit().putBoolean("muted", false).apply()
        Backups.prepareRestore { Backups.open(made) }
        assertTrue(Backups.applyPendingRestore(pc, now = 7L))

        val repo = pages(pc)
        assertEquals("Prima del backup", repo.getPage("nota")?.title)
        assertNull(repo.getPage("dopo"))
        assertEquals("immagine", File(pcDir, "${PageImageStore.DIRECTORY}/foto.png").readText())
        assertTrue(openSettingsStore(pc, "settings").getBoolean("muted", false))
    }
}
