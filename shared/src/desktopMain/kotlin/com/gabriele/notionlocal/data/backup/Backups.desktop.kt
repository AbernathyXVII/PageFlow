package com.gabriele.notionlocal.data.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.execSQL
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.DATABASE_VERSION
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.notify.DesktopNotifications
import com.gabriele.notionlocal.data.notify.NotificationTarget
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.SettingsStore
import com.gabriele.notionlocal.data.settings.openSettingsStore
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import com.gabriele.notionlocal.ui.screen.appVersionName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import javax.swing.filechooser.FileSystemView

/** Un backup sul PC: il file nella cartella dei backup. */
actual data class BackupFile(val file: File, actual val name: String, actual val createdAt: Long, actual val size: Long)

/**
 * **I backup sul PC** (dal 09/11/2026), come quelli del telefono (`BackupArchive`,
 * lo stesso file zip: uno fatto qui si apre anche sul telefono, e viceversa).
 *
 * - Stanno in **Documenti/PageFlow backup** (la cartella Documenti vera, anche
 *   se Windows l'ha spostata su OneDrive); se ne tengono gli ultimi [KEEP].
 * - **Ogni giorno**: un minuto dopo l'apertura e poi ogni ora, finché PageFlow
 *   è aperta, se oggi non ce n'è ancora uno (il telefono lo fa uscendo dall'app).
 * - **"Carica su Proton Drive"** copia un backup dove si sceglie (la cartella di
 *   Proton Drive sul PC, una chiavetta…); la volta dopo la finestra si apre lì.
 * - **Il promemoria**, passati [remindDays] giorni senza caricare: una notifica di
 *   Windows al giorno che apre la pagina Backup.
 * - **Il ripristino** fa prima un backup di adesso, poi prepara i file a parte;
 *   li mette al loro posto [applyPendingRestore], alla riapertura. Quello che
 *   c'era resta anche in `before-restore-<ora>`, nella cartella dei dati.
 */
actual object Backups {
    const val KEEP = 7
    private const val PENDING = "restore-pending"
    private const val READY = "ready"
    private const val DATABASE_NAME = "notion_local.db"
    private const val PREFS = "backup"
    private const val FIRST_CHECK_MS = 60 * 1000L
    private const val CHECK_EVERY_MS = 60 * 60 * 1000L

    actual val REMIND_CHOICES = listOf(3, 7, 14, 30, 0)

    actual var autoBackup by mutableStateOf(true)
        private set
    actual var remindDays by mutableIntStateOf(7)
        private set
    actual var lastBackupAt by mutableLongStateOf(0L)
        private set
    actual var lastUploadAt by mutableLongStateOf(0L)
        private set
    actual var changes by mutableIntStateOf(0)
        private set

    /** La cartella dove si è caricato l'ultima volta: lì si riapre la finestra. */
    var lastUploadDir: String? = null
        private set
    private var firstBackupAt = 0L
    private var lastReminderAt = 0L

    private var context: DesktopContext? = null
    private var prefs: SettingsStore? = null
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Nei test: un'altra cartella (mai i Documenti veri). */
    var folderOverride: File? = null
    private val documents by lazy { FileSystemView.getFileSystemView().defaultDirectory }

    /** Dove stanno i backup. */
    val folder: File get() = folderOverride ?: File(documents, "PageFlow backup")

    fun init(context: DesktopContext) {
        this.context = context
        val p = openSettingsStore(context, PREFS)
        prefs = p
        autoBackup = p.getBoolean("auto", true)
        remindDays = p.getInt("remindDays", 7)
        lastBackupAt = p.getLong("lastBackupAt", 0L)
        lastUploadAt = p.getLong("lastUploadAt", 0L)
        lastUploadDir = p.getString("lastUploadDir", null)
        firstBackupAt = p.getLong("firstBackupAt", 0L)
        lastReminderAt = p.getLong("lastReminderAt", 0L)
    }

    private fun save() {
        prefs?.edit()
            ?.putBoolean("auto", autoBackup)
            ?.putInt("remindDays", remindDays)
            ?.putLong("lastBackupAt", lastBackupAt)
            ?.putLong("lastUploadAt", lastUploadAt)
            ?.putString("lastUploadDir", lastUploadDir)
            ?.putLong("firstBackupAt", firstBackupAt)
            ?.putLong("lastReminderAt", lastReminderAt)
            ?.apply()
    }

    private fun context(): DesktopContext = context ?: error("Backups.init non chiamato")

    actual fun setAuto(on: Boolean) {
        autoBackup = on
        save()
    }

    actual fun setRemind(days: Int) {
        remindDays = days
        save()
    }

    actual fun list(): List<BackupFile> =
        (folder.listFiles() ?: emptyArray()).filter { it.isFile && isBackupName(it.name) }
            .map { BackupFile(it, it.name, it.lastModified(), it.length()) }
            .sortedWith(compareByDescending<BackupFile> { it.createdAt }.thenByDescending { it.name })

    actual fun delete(file: BackupFile): Boolean = file.file.delete().also { if (it) changes++ }

    actual fun open(file: BackupFile): InputStream = file.file.inputStream()

    // --- Il backup ---

    actual suspend fun backupNow(reason: BackupReason): BackupFile = mutex.withLock { create(reason) }

    private suspend fun create(reason: BackupReason): BackupFile = withContext(Dispatchers.IO) {
        val ctx = context()
        val now = System.currentTimeMillis()
        val work = File(ctx.dataDir, "backup-work-$now").apply { mkdirs() }
        try {
            // Il database tutto in un istante, anche mentre l'app scrive.
            val database = File(work, BackupArchive.DATABASE)
            AppDatabase.getInstance(ctx).useWriterConnection {
                it.execSQL("VACUUM INTO '" + database.absolutePath.replace("'", "''") + "'")
            }
            val filesDir = File(ctx.dataDir, PageImageStore.DIRECTORY)
            val manifest = BackupManifest(
                createdAt = now,
                appVersion = appVersionName(ctx).orEmpty(),
                databaseVersion = DATABASE_VERSION,
                files = BackupArchive.filesOf(filesDir).size,
                reason = reason.name
            )
            val file = write(backupFileName(now, reason, AppSettings.zoneId)) { out ->
                BackupArchive.write(out, manifest, database, filesDir, snapshotPrefs(ctx))
            }
            lastBackupAt = now
            if (firstBackupAt == 0L) firstBackupAt = now
            save()
            // Se ne tengono gli ultimi KEEP.
            list().drop(KEEP).forEach { it.file.delete() }
            changes++
            file
        } finally {
            work.deleteRecursively()
        }
    }

    /** Scrive [name] nella cartella dei backup: prima accanto, poi al suo posto, così non resta niente a metà. */
    private fun write(name: String, writer: (OutputStream) -> Unit): BackupFile {
        folder.mkdirs()
        val target = File(folder, name)
        val partial = File(folder, "$name.part")
        try {
            partial.outputStream().use(writer)
            if (!partial.renameTo(target)) throw IOException("Non si rinomina $partial")
        } finally {
            partial.delete()
        }
        return BackupFile(target, name, target.lastModified(), target.length())
    }

    /**
     * Le impostazioni del PC, tutte come testo (il PC le tiene così): il
     * telefono le legge lo stesso (vedi `SettingsStore.android.kt`).
     * shortcut: si leggono i file mentre l'app potrebbe riscriverli; al peggio un backup senza impostazioni, il database c'è.
     */
    private fun snapshotPrefs(ctx: DesktopContext): Map<String, Map<String, PrefValue>> =
        BackupArchive.PREF_NAMES.associateWith { name ->
            val file = File(ctx.dataDir, "$name.properties")
            val values = Properties().apply { if (file.isFile) file.inputStream().use { load(it) } }
            values.stringPropertyNames().associateWith { PrefValue("string", values.getProperty(it)) }
        }

    // --- Ogni giorno e il promemoria ---

    /** Il backup di oggi e il promemoria: un minuto dopo l'apertura, poi ogni ora finché PageFlow è aperta. */
    fun startDailyTasks() {
        scope.launch {
            delay(FIRST_CHECK_MS)
            while (true) {
                runDailyTasks()
                delay(CHECK_EVERY_MS)
            }
        }
    }

    suspend fun runDailyTasks(now: Long = System.currentTimeMillis()) {
        runCatching { if (autoBackup && isBackupDue(lastBackupAt, now, AppSettings.zoneId)) backupNow(BackupReason.DAILY) }
        runCatching { remindIfDue(now) }
    }

    private fun remindIfDue(now: Long) {
        if (!isUploadReminderDue(now, remindDays, lastUploadAt, firstBackupAt, lastReminderAt)) return
        val shown = !DesktopNotifications.muted &&
            DesktopNotifications.show(BackupStrings.reminderTitle, BackupStrings.reminderText(daysSince(lastUploadAt, now))) {
                NotificationTarget.openBackup.value = true
            }
        if (shown) {
            lastReminderAt = now
            save()
        }
    }

    /** Copia [file] in [target] (scelto con "Salva come"); riuscito, conta come caricato adesso. */
    suspend fun upload(file: BackupFile, target: File): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching { file.file.copyTo(target, overwrite = true) }.isSuccess
        if (ok) {
            lastUploadAt = System.currentTimeMillis()
            lastUploadDir = target.parent
            lastReminderAt = 0L
            save()
            changes++
        }
        ok
    }

    // --- Il ripristino ---

    actual suspend fun prepareRestore(open: () -> InputStream): BackupManifest = withContext(Dispatchers.IO) {
        val ctx = context()
        val pending = File(ctx.dataDir, PENDING).apply { deleteRecursively(); mkdirs() }
        try {
            val content = File(pending, "content")
            val extracted = open().use { BackupArchive.read(it, content) }
            checkDatabase(extracted.database)
            File(content, BackupArchive.PREFS).writeText(json.encodeToString(extracted.prefs))
            backupNow(BackupReason.BEFORE_RESTORE)
            File(pending, READY).writeText(extracted.manifest.createdAt.toString())
            extracted.manifest
        } catch (e: Exception) {
            pending.deleteRecursively()
            throw e
        }
    }

    /**
     * Il database del backup si apre, è sano, ed è proprio della versione di
     * questa app: il PC non ha le migrazioni dei database vecchi (stanno nel
     * codice del telefono), e uno più vecchio non si aprirebbe più.
     */
    private fun checkDatabase(database: File) {
        val version = try {
            BundledSQLiteDriver().open(database.path).use { connection ->
                connection.prepare("PRAGMA quick_check").use { check ->
                    check.step()
                    if (check.getText(0) != "ok") throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Database rovinato")
                }
                connection.prepare("PRAGMA user_version").use { it.step(); it.getLong(0).toInt() }
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Database che non si apre: ${e.message}")
        }
        if (version > DATABASE_VERSION) throw BackupException(BackupException.Problem.TOO_NEW, "Database $version, app $DATABASE_VERSION")
        if (version < DATABASE_VERSION) throw BackupException(BackupException.Problem.TOO_OLD, "Database $version, app $DATABASE_VERSION")
    }

    /**
     * **Mette al loro posto i file di un ripristino preparato**: da chiamare
     * per prima cosa all'avvio. Le copie si fanno accanto a quelli di adesso, e
     * solo se riescono prendono il loro posto; quelli di prima vanno in
     * `before-restore-<ora>`. Vero se ha ripristinato.
     */
    fun applyPendingRestore(context: DesktopContext, now: Long = System.currentTimeMillis()): Boolean {
        val dir = context.dataDir
        val pending = File(dir, PENDING)
        if (!File(pending, READY).exists()) {
            if (pending.exists()) pending.deleteRecursively()
            return false
        }
        val content = File(pending, "content")
        val database = File(dir, DATABASE_NAME)
        val images = File(dir, PageImageStore.DIRECTORY)
        val newDatabase = File(dir, "$DATABASE_NAME.restoring")
        val newImages = File(dir, PageImageStore.DIRECTORY + ".restoring")
        try {
            File(content, BackupArchive.DATABASE).copyTo(newDatabase, overwrite = true)
            newImages.deleteRecursively()
            File(content, "files").copyRecursively(newImages, overwrite = true)
        } catch (e: Exception) {
            newDatabase.delete()
            newImages.deleteRecursively()
            pending.deleteRecursively()
            return false
        }
        AppDatabase.closeInstance()
        // Quello che c'era, da parte: il database coi suoi file accanto, le immagini, le impostazioni.
        val before = File(dir, "before-restore-$now").apply { mkdirs() }
        val previous = listOf("", "-wal", "-shm", "-journal").map { File(database.path + it) } + images +
            BackupArchive.PREF_NAMES.map { File(dir, "$it.properties") }
        previous.filter { it.exists() }.forEach { it.renameTo(File(before, it.name)) }
        newDatabase.renameTo(database)
        newImages.renameTo(images)
        runCatching {
            val prefsFile = File(content, BackupArchive.PREFS)
            if (prefsFile.exists()) restorePrefs(context, json.decodeFromString(prefsFile.readText()))
        }
        pending.deleteRecursively()
        return true
    }

    /**
     * Le impostazioni del telefono nei file del PC (che le tengono tutte come
     * testo). Solo quelle di [BackupArchive.PREF_NAMES], come sul telefono.
     */
    private fun restorePrefs(context: DesktopContext, saved: Map<String, Map<String, PrefValue>>) {
        saved.filterKeys { it in BackupArchive.PREF_NAMES }.forEach { (name, values) ->
            val editor = openSettingsStore(context, name).edit()
            values.forEach { (key, v) ->
                // shortcut: gli insiemi di testi ("set") non hanno un posto nelle impostazioni del PC; nessuna di queste li usa.
                if (v.type != "set") editor.putString(key, v.value)
            }
            editor.apply()
        }
    }
}
