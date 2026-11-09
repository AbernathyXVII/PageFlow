package com.gabriele.notionlocal.data.backup

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.openSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * **I backup** (28/10/2026, chiesti dall'utente: "backup automatico ogni
 * giorno sul telefono, più «Carica su Proton Drive» con un tocco e un
 * promemoria se non lo carico da troppo"): dentro c'è tutto, se ne tengono
 * 7, uno al giorno, il ripristino rimette tutto com'era (e prima fa un
 * backup di adesso), un file che non è un backup non tocca niente, il
 * caricamento e il promemoria.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
// La SQLite vera, come sul telefono: quella di sempre dei test non conosce `VACUUM INTO`.
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class BackupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Rome")
    private var now = at(2026, 10, 28, 14, 30)
    private lateinit var folder: File
    private val images get() = File(context.filesDir, PageImageStore.DIRECTORY).apply { mkdirs() }
    private val db get() = AppDatabase.getInstance(context)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        AppSettings.init(context)
        AppDatabase.resetForTests()
        context.getDatabasePath(Backups.DATABASE_NAME).delete()
        context.getSharedPreferences("backup", Context.MODE_PRIVATE).edit().clear().commit()
        images.deleteRecursively()
        folder = File(context.cacheDir, "test-backups").apply { deleteRecursively(); mkdirs() }
        Backups.storageOverride = DirectoryStorage(folder)
        Backups.clock = { now }
        Backups.zone = { zone }
        Backups.init(context)
    }

    @After
    fun tearDown() {
        AppDatabase.resetForTests()
        Backups.storageOverride = null
        Backups.clock = { System.currentTimeMillis() }
        Backups.zone = { AppSettings.zoneId }
        File(context.filesDir, "restore-pending").deleteRecursively()
    }

    private fun backup(reason: BackupReason = BackupReason.MANUAL) = runBlocking { Backups.backupNow(reason) }

    private fun extract(file: BackupFile): ExtractedBackup {
        val dir = File(context.cacheDir, "extract-${System.nanoTime()}")
        return Backups.storage.open(file).use { BackupArchive.read(it, dir) }
    }

    private fun titles(database: File): List<String> =
        SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { sqlite ->
            sqlite.rawQuery("SELECT title FROM pages ORDER BY title", null).use { c ->
                buildList { while (c.moveToNext()) add(c.getString(0)) }
            }
        }

    @Test
    fun aBackupHasTheDatabaseTheFilesAndThePreferences() = runBlocking<Unit> {
        db.pageDao().insert(PageEntity(id = "diario", title = "Diario"))
        File(images, "foto.img").writeBytes(byteArrayOf(1, 2, 3))
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("backupTest", "sì").commit()
        context.getSharedPreferences("widgets", Context.MODE_PRIVATE).edit().putInt("backupTestCount", 4).commit()

        val file = backup()
        assertEquals("PageFlow-backup-2026-10-28-143000.zip", file.name)
        val extracted = extract(file)
        assertEquals(1, extracted.manifest.files)
        assertEquals(db.openHelper.readableDatabase.version, extracted.manifest.databaseVersion)
        assertEquals(BackupReason.MANUAL.name, extracted.manifest.reason)
        assertTrue("Diario" in titles(extracted.database))
        assertEquals(listOf<Byte>(1, 2, 3), File(extracted.files, "foto.img").readBytes().toList())
        assertEquals(PrefValue("string", "sì"), extracted.prefs["settings"]?.get("backupTest"))
        assertEquals(PrefValue("int", "4"), extracted.prefs["widgets"]?.get("backupTestCount"))
        // Le preferenze dei backup stessi non ci vanno.
        assertFalse("backup" in extracted.prefs.keys)
        assertEquals(now, Backups.lastBackupAt)
    }

    @Test
    fun aBackupFromWhenTheAppWasCalledNoteIsStillThere() = runBlocking<Unit> {
        // L'app si chiamava notE fino al 03/11/2026: i suoi backup avevano un
        // altro nome, un altro manifest e un'altra cartella. Si vedono ancora,
        // spostati e rinominati, e si ripristinano.
        db.pageDao().insert(PageEntity(id = "diario", title = "Diario"))
        val fresh = backup()
        val old = File(context.cacheDir, "test-backups-notE").apply { deleteRecursively(); mkdirs() }
        val oldFile = File(old, "notE-backup-2026-10-20-100000.zip")
        java.util.zip.ZipInputStream(File(folder, fresh.name).inputStream()).use { input ->
            ZipOutputStream(oldFile.outputStream()).use { output ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val name = if (entry.name == BackupArchive.MANIFEST) BackupArchive.OLD_MANIFEST else entry.name
                    output.putNextEntry(ZipEntry(name))
                    input.copyTo(output)
                    output.closeEntry()
                }
            }
        }
        File(folder, fresh.name).delete()
        Backups.storageOverride = DirectoryStorage(folder, listOf(old))

        val found = Backups.list().single()
        assertEquals("PageFlow-backup-2026-10-20-100000.zip", found.name)
        assertTrue(old.listFiles().isNullOrEmpty())
        assertTrue("Diario" in titles(extract(found).database))
    }

    @Test
    fun onlyTheLastSevenAreKept() {
        repeat(9) { i ->
            now = at(2026, 10, 10 + i, 20, 0)
            backup(BackupReason.DAILY)
            // Le date dei file come quelle vere.
            File(folder, Backups.fileName(now, BackupReason.DAILY)).setLastModified(now)
        }
        val left = Backups.list()
        assertEquals(7, left.size)
        assertEquals("PageFlow-backup-2026-10-18-200000.zip", left.first().name)
        assertEquals("PageFlow-backup-2026-10-12-200000.zip", left.last().name)
    }

    @Test
    fun theDailyBackupIsOncePerCalendarDay() {
        assertTrue(isBackupDue(0, now, zone))
        assertFalse(isBackupDue(at(2026, 10, 28, 0, 5), at(2026, 10, 28, 23, 50), zone))
        assertTrue(isBackupDue(at(2026, 10, 27, 23, 50), at(2026, 10, 28, 0, 5), zone))
        // Uscendo dall'app: il primo del giorno sì, il secondo no.
        runBlocking { Backups.onStopTasks() }
        assertEquals(1, Backups.list().size)
        now = at(2026, 10, 28, 18, 0)
        runBlocking { Backups.onStopTasks() }
        assertEquals(1, Backups.list().size)
        now = at(2026, 10, 29, 8, 0)
        runBlocking { Backups.onStopTasks() }
        assertEquals(2, Backups.list().size)
        // Spento, niente.
        Backups.setAuto(false)
        now = at(2026, 10, 30, 8, 0)
        runBlocking { Backups.onStopTasks() }
        assertEquals(2, Backups.list().size)
    }

    @Test
    fun aRestorePutsEverythingBackAfterABackupOfNow() = runBlocking<Unit> {
        db.pageDao().insert(PageEntity(id = "a", title = "Prima"))
        File(images, "a.img").writeBytes(byteArrayOf(7))
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("backupTest", "vecchio").commit()
        val saved = backup()

        // Dopo il backup cambia tutto.
        now = at(2026, 10, 29, 9, 0)
        db.pageDao().update(db.pageDao().getById("a")!!.copy(title = "Dopo"))
        db.pageDao().insert(PageEntity(id = "b", title = "Nuova"))
        File(images, "a.img").delete()
        File(images, "b.img").writeBytes(byteArrayOf(9))
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("backupTest", "nuovo").commit()

        val manifest = Backups.prepareRestore { Backups.storage.open(saved) }
        assertEquals(saved.name, Backups.fileName(manifest.createdAt, BackupReason.MANUAL))
        // Prima del ripristino, un backup di com'era adesso.
        val before = Backups.list().first { it.name.contains("before-restore") }
        assertTrue("Nuova" in titles(extract(before).database))
        // Finché l'app non si riapre non cambia niente.
        assertEquals("Dopo", db.pageDao().getById("a")!!.title)

        // La riapertura: il database si chiude e i file prendono il loro posto.
        assertTrue(Backups.applyPendingRestore(context))
        assertEquals("Prima", AppDatabase.getInstance(context).pageDao().getById("a")!!.title)
        assertNull(AppDatabase.getInstance(context).pageDao().getById("b"))
        assertTrue(File(images, "a.img").exists())
        assertFalse(File(images, "b.img").exists())
        assertEquals("vecchio", context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("backupTest", null))
        // Una volta sola.
        assertFalse(Backups.applyPendingRestore(context))
        assertFalse(File(context.filesDir, "restore-pending").exists())
    }

    @Test
    fun aPcBackupWithSettingsAsTextIsReadOnThePhone() = runBlocking<Unit> {
        db.pageDao().insert(PageEntity(id = "pc", title = "Dal PC"))
        // Il PC tiene le impostazioni tutte come testo: il suo backup le scrive così.
        val fromPhone = extract(backup())
        val pcZip = ByteArrayOutputStream().also { out ->
            BackupArchive.write(
                out, fromPhone.manifest, fromPhone.database, fromPhone.files,
                mapOf("settings" to mapOf("notifications_muted" to PrefValue("string", "true"), "skin_tone" to PrefValue("string", "3")))
            )
        }.toByteArray()

        Backups.prepareRestore { ByteArrayInputStream(pcZip) }
        assertTrue(Backups.applyPendingRestore(context))
        // Come le legge `AppSettings` alla riapertura dell'app.
        val settings = openSettingsStore(context, "settings")
        assertTrue(settings.getBoolean("notifications_muted", false))
        assertEquals(3, settings.getInt("skin_tone", 0))
        assertEquals("Dal PC", AppDatabase.getInstance(context).pageDao().getById("pc")!!.title)
    }

    @Test
    fun aFileThatIsNotABackupTouchesNothing() = runBlocking<Unit> {
        db.pageDao().insert(PageEntity(id = "a", title = "Resta"))
        val notZip = ByteArrayInputStream("ciao".toByteArray())
        expectProblem(BackupException.Problem.NOT_A_BACKUP) { Backups.prepareRestore { notZip } }
        // Uno zip qualsiasi, senza il database di notE.
        val other = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("foto.jpg"))
                zip.write(byteArrayOf(1))
                zip.closeEntry()
            }
        }.toByteArray()
        expectProblem(BackupException.Problem.NOT_A_BACKUP) { Backups.prepareRestore { ByteArrayInputStream(other) } }
        // Niente da ripristinare, e nessun backup "prima del ripristino".
        assertFalse(Backups.applyPendingRestore(context))
        assertTrue(Backups.list().isEmpty())
        assertEquals("Resta", db.pageDao().getById("a")!!.title)
    }

    @Test
    fun aBackupFromANewerAppIsRefused() = runBlocking<Unit> {
        val newer = File(context.cacheDir, "newer.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(newer, null).use { it.version = 999 }
        val zip = ByteArrayOutputStream().also { out ->
            BackupArchive.write(out, BackupManifest(createdAt = now, databaseVersion = 999), newer, File(context.cacheDir, "nessuna"), emptyMap())
        }.toByteArray()
        expectProblem(BackupException.Problem.TOO_NEW) { Backups.prepareRestore { ByteArrayInputStream(zip) } }
        assertFalse(Backups.applyPendingRestore(context))
    }

    @Test
    fun aNameThatLeavesTheFolderIsNotABackup() {
        val evil = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry(BackupArchive.FILES + "../../fuori.txt"))
                zip.write("no".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val dir = File(context.cacheDir, "evil").apply { deleteRecursively() }
        try {
            BackupArchive.read(ByteArrayInputStream(evil), dir)
            fail("doveva rifiutarlo")
        } catch (e: BackupException) {
            assertEquals(BackupException.Problem.NOT_A_BACKUP, e.problem)
        }
        assertFalse(File(context.cacheDir, "fuori.txt").exists())
        assertFalse(File(dir.parentFile, "fuori.txt").exists())
    }

    @Test
    fun uploadingCopiesTheBackupAndRemembersWhere() = runBlocking<Unit> {
        val file = backup()
        // Il file creato da "Salva come" su Proton Drive: qui uno finto, che tiene quello che riceve.
        val target = Uri.parse("content://me.proton.android.drive.documents/document/prova")
        val received = ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(target, received)
        now = at(2026, 10, 28, 15, 0)
        assertTrue(Backups.upload(file, target))
        assertEquals(File(folder, file.name).readBytes().toList(), received.toByteArray().toList())
        assertEquals(now, Backups.lastUploadAt)
        assertEquals(target.toString(), Backups.lastUploadUri)
        // Si ricorda anche dopo una riapertura.
        Backups.init(context)
        assertEquals(now, Backups.lastUploadAt)
    }

    @Test
    fun theReminderComesAfterTheChosenDaysOncePerDay() {
        val day = 24L * 60 * 60 * 1000
        val first = at(2026, 10, 1, 9, 0)
        // Mai caricato: dopo 7 giorni dal primo backup.
        assertFalse(isUploadReminderDue(first + 6 * day, 7, 0, first, 0))
        assertTrue(isUploadReminderDue(first + 7 * day, 7, 0, first, 0))
        // Già ricordato da poco: non di nuovo lo stesso giorno.
        assertFalse(isUploadReminderDue(first + 7 * day + 3_600_000, 7, 0, first, first + 7 * day))
        assertTrue(isUploadReminderDue(first + 8 * day, 7, 0, first, first + 7 * day))
        // Caricato 3 giorni fa: niente. Spento: mai. Senza backup: niente.
        assertFalse(isUploadReminderDue(first + 10 * day, 7, first + 7 * day, first, 0))
        assertFalse(isUploadReminderDue(first + 100 * day, 0, 0, first, 0))
        assertFalse(isUploadReminderDue(first + 100 * day, 7, 0, 0, 0))
    }

    @Test
    fun leavingTheAppAfterTooLongPostsTheReminder() {
        now = at(2026, 10, 1, 9, 0)
        backup(BackupReason.DAILY)
        now = at(2026, 10, 9, 9, 0)
        runBlocking { Backups.onStopTasks() }
        val manager = context.getSystemService(NotificationManager::class.java)
        val posted = shadowOf(manager).allNotifications
        assertNotNull(posted.find { it.extras.getString("android.title") == "Upload a backup to Proton Drive" })
        // Lo stesso giorno non torna.
        shadowOf(manager).allNotifications.size.let { count ->
            now = at(2026, 10, 9, 18, 0)
            runBlocking { Backups.onStopTasks() }
            assertEquals(count, shadowOf(manager).allNotifications.size)
        }
    }

    private suspend fun expectProblem(problem: BackupException.Problem, block: suspend () -> Unit) {
        try {
            block()
            fail("doveva dire $problem")
        } catch (e: BackupException) {
            assertEquals(problem, e.problem)
        }
    }
}
