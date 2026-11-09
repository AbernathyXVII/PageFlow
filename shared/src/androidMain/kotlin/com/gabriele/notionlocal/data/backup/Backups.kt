package com.gabriele.notionlocal.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.notify.Notifier
import com.gabriele.notionlocal.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.time.ZoneId

/**
 * **I backup di notE** (dal 28/10/2026, chiesto dall'utente: "backup
 * automatico ogni giorno sul telefono, più «Carica su Proton Drive» con un
 * tocco e un promemoria se non lo carico da troppo").
 *
 * - **Ogni giorno**, uscendo dall'app, se oggi non ce n'è ancora uno, nasce un
 *   backup (vedi `BackupArchive`) in Download/notE backup; se ne tengono
 *   gli ultimi [KEEP].
 * - **"Carica su Proton Drive"** copia un backup dove si sceglie con "Salva
 *   come": Proton Drive lascia creare un file alla volta, ma non dà alle
 *   altre app una cartella in cui scrivere da sole (controllato sul telefono
 *   il 28/10/2026, Proton Drive 3.2.0). La volta dopo "Salva come" si apre
 *   dove si è salvato l'ultima volta.
 * - **Il promemoria**: passati [remindDays] giorni dall'ultimo caricamento,
 *   una notifica al giorno, finché non lo si carica.
 * - **Il ripristino** prima fa un backup di com'è adesso, poi prepara i
 *   file in una cartella a parte; li mette al loro posto `applyPendingRestore`,
 *   alla riapertura, prima che qualcuno apra il database.
 */
actual object Backups {
    /** Dal 03/11/2026, quando l'app si chiamava ancora notE, era "notE backup": vedi [OLD_FOLDERS]. */
    const val FOLDER = "PageFlow backup"

    /** Le cartelle dei backup coi nomi di prima: si leggono ancora, e quelli dell'app si spostano in [FOLDER]. */
    val OLD_FOLDERS = listOf("notE backup")
    const val KEEP = 7
    const val DATABASE_NAME = "notion_local.db"

    /** La versione del database quando sono nati i backup: uno più vecchio non può esistere. */
    const val FIRST_DATABASE_VERSION = 65

    /** Le preferenze che vanno nel backup: impostazioni, widget, penna, password delle pagine. */
    val PREF_NAMES = BackupArchive.PREF_NAMES

    actual val REMIND_CHOICES = listOf(3, 7, 14, 30, 0)

    private const val PREFS = "backup"
    private const val PENDING = "restore-pending"
    private const val READY = "ready"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Nei test: dove stanno i backup, l'ora e il fuso. */
    var storageOverride: BackupStorage? = null
    var clock: () -> Long = { System.currentTimeMillis() }
    var zone: () -> ZoneId = { AppSettings.zoneId }

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

    /** Dove si è salvato l'ultima volta con "Carica su Proton Drive": lì si riapre "Salva come". */
    var lastUploadUri: String? = null
        private set
    private var firstBackupAt = 0L
    private var lastReminderAt = 0L

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        autoBackup = p.getBoolean("auto", true)
        remindDays = p.getInt("remindDays", 7)
        lastBackupAt = p.getLong("lastBackupAt", 0L)
        lastUploadAt = p.getLong("lastUploadAt", 0L)
        lastUploadUri = p.getString("lastUploadUri", null)
        firstBackupAt = p.getLong("firstBackupAt", 0L)
        lastReminderAt = p.getLong("lastReminderAt", 0L)
    }

    private fun save() {
        prefs?.edit()
            ?.putBoolean("auto", autoBackup)
            ?.putInt("remindDays", remindDays)
            ?.putLong("lastBackupAt", lastBackupAt)
            ?.putLong("lastUploadAt", lastUploadAt)
            ?.putString("lastUploadUri", lastUploadUri)
            ?.putLong("firstBackupAt", firstBackupAt)
            ?.putLong("lastReminderAt", lastReminderAt)
            ?.apply()
    }

    actual fun setAuto(on: Boolean) {
        autoBackup = on
        save()
    }

    actual fun setRemind(days: Int) {
        remindDays = days
        save()
    }

    private fun context(): Context = appContext ?: error("Backups.init non chiamato")

    val storage: BackupStorage
        get() = storageOverride ?: context().let { ctx ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStoreStorage(ctx)
            } else {
                val root = ctx.getExternalFilesDir(null) ?: ctx.filesDir
                DirectoryStorage(File(root, FOLDER), OLD_FOLDERS.map { File(root, it) })
            }
        }

    actual fun list(): List<BackupFile> = runCatching { storage.list() }.getOrDefault(emptyList())

    // --- Il backup ---

    actual suspend fun backupNow(reason: BackupReason): BackupFile =
        mutex.withLock { withContext(Dispatchers.IO) { create(reason) } }

    private fun create(reason: BackupReason): BackupFile {
        val ctx = context()
        val now = clock()
        val work = File(ctx.cacheDir, "backup-work-$now").apply { mkdirs() }
        try {
            val database = File(work, BackupArchive.DATABASE)
            val version = snapshotDatabase(ctx, database)
            val filesDir = File(ctx.filesDir, PageImageStore.DIRECTORY)
            val manifest = BackupManifest(
                createdAt = now,
                appVersion = appVersion(ctx),
                databaseVersion = version,
                files = BackupArchive.filesOf(filesDir).size,
                reason = reason.name
            )
            val prefsNow = snapshotPrefs(ctx)
            val file = storage.write(fileName(now, reason)) { out ->
                BackupArchive.write(out, manifest, database, filesDir, prefsNow)
            }
            lastBackupAt = now
            if (firstBackupAt == 0L) firstBackupAt = now
            save()
            rotate()
            changes++
            return file
        } finally {
            work.deleteRecursively()
        }
    }

    /**
     * **Il database tutto in un istante.** Da Android 11 `VACUUM INTO` ne fa
     * una copia coerente anche mentre l'app scrive; prima si svuota il
     * giornale (WAL) dentro il file e lo si copia.
     */
    private fun snapshotDatabase(ctx: Context, target: File): Int {
        val sqlite = AppDatabase.getInstance(ctx).openHelper.writableDatabase
        target.delete()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val copied = runCatching { sqlite.execSQL("VACUUM INTO '" + target.absolutePath.replace("'", "''") + "'") }.isSuccess
            if (copied) return sqlite.version
            // Una SQLite che non lo conosce (quella dei test, o un telefono strano): la copia del file.
            target.delete()
        }
        sqlite.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        ctx.getDatabasePath(DATABASE_NAME).copyTo(target, overwrite = true)
        return sqlite.version
    }

    private fun appVersion(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()

    /** "PageFlow-backup-2026-10-28-143205.zip"; prima di un ripristino, con "-before-restore". */
    fun fileName(at: Long, reason: BackupReason): String = backupFileName(at, reason, zone())

    /** Se ne tengono gli ultimi [KEEP]: i più vecchi si cancellano. */
    private fun rotate() {
        val all = runCatching { storage.list() }.getOrDefault(emptyList())
        all.drop(KEEP).forEach { runCatching { storage.delete(it) } }
    }

    actual fun delete(file: BackupFile): Boolean = storage.delete(file).also { if (it) changes++ }

    actual fun open(file: BackupFile): InputStream = storage.open(file)

    /**
     * **Uscendo dall'app** (vedi `MainActivity.onStop`): il backup di oggi, se
     * manca, e il promemoria, se è ora. Dopo un giorno d'uso, perché quello
     * che cambia nell'app cambia mentre la si usa.
     */
    fun onAppStopped() {
        if (prefs == null) return
        scope.launch { onStopTasks() }
    }

    /** Quello che si fa uscendo dall'app, in fila: il backup di oggi, poi il promemoria. */
    suspend fun onStopTasks() {
        runCatching { if (autoBackup && isBackupDue(lastBackupAt, clock(), zone())) backupNow(BackupReason.DAILY) }
        runCatching { remindIfDue() }
    }

    // --- Caricare e il promemoria ---

    /**
     * Copia [file] in [target], il file scelto con "Salva come" (su Proton
     * Drive, o dove si vuole). Riuscito, conta come caricato adesso.
     */
    suspend fun upload(file: BackupFile, target: Uri): Boolean = withContext(Dispatchers.IO) {
        val ctx = context()
        val ok = runCatching {
            storage.open(file).use { input ->
                (ctx.contentResolver.openOutputStream(target, "w") ?: error("Non si apre $target")).use { input.copyTo(it) }
            }
        }.isSuccess
        if (ok) {
            lastUploadAt = clock()
            lastUploadUri = target.toString()
            lastReminderAt = 0L
            save()
            changes++
        }
        ok
    }

    private fun remindIfDue() {
        val now = clock()
        if (!isUploadReminderDue(now, remindDays, lastUploadAt, firstBackupAt, lastReminderAt)) return
        if (Notifier.postBackupReminder(context(), daysSince(lastUploadAt, now))) {
            lastReminderAt = now
            save()
        }
    }

    // --- Il ripristino ---

    /**
     * **Prepara il ripristino** del backup che [open] legge: lo toglie dallo
     * zip in una cartella a parte, controlla che sia un backup di notE con un
     * database che questa versione sa aprire, e fa un backup di com'è adesso
     * (così si può tornare indietro). I file al loro posto li mette
     * `applyPendingRestore`, alla riapertura dell'app.
     */
    actual suspend fun prepareRestore(open: () -> InputStream): BackupManifest = withContext(Dispatchers.IO) {
        val ctx = context()
        val pending = File(ctx.filesDir, PENDING).apply { deleteRecursively(); mkdirs() }
        try {
            val content = File(pending, "content")
            val extracted = open().use { BackupArchive.read(it, content) }
            checkDatabase(ctx, extracted)
            File(content, BackupArchive.PREFS).writeText(json.encodeToString(extracted.prefs))
            mutex.withLock { create(BackupReason.BEFORE_RESTORE) }
            File(pending, READY).writeText(extracted.manifest.createdAt.toString())
            extracted.manifest
        } catch (e: Exception) {
            pending.deleteRecursively()
            throw e
        }
    }

    /** Il database del backup si apre, è sano, e questa versione dell'app lo sa leggere. */
    private fun checkDatabase(ctx: Context, extracted: ExtractedBackup) {
        val version = try {
            SQLiteDatabase.openDatabase(extracted.database.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.rawQuery("PRAGMA quick_check", null).use { cursor ->
                    cursor.moveToFirst()
                    if (cursor.getString(0) != "ok") throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Database rovinato")
                }
                db.version
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Database che non si apre: ${e.message}")
        }
        val current = AppDatabase.getInstance(ctx).openHelper.readableDatabase.version
        if (version > current) throw BackupException(BackupException.Problem.TOO_NEW, "Database $version, app $current")
        if (version < FIRST_DATABASE_VERSION) throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Database $version")
    }

    /**
     * **Mette al loro posto i file di un ripristino preparato**: da chiamare
     * per prima cosa all'avvio, prima delle impostazioni e del database. Il
     * database e le immagini si copiano prima accanto a quelli di adesso, e
     * solo se le copie riescono prendono il loro posto: un telefono pieno a
     * metà copia lascia notE com'era. Vero se ha ripristinato.
     */
    fun applyPendingRestore(context: Context): Boolean {
        val ctx = context.applicationContext
        val pending = File(ctx.filesDir, PENDING)
        if (!File(pending, READY).exists()) {
            if (pending.exists()) pending.deleteRecursively()
            return false
        }
        val content = File(pending, "content")
        val dbFile = ctx.getDatabasePath(DATABASE_NAME)
        val images = File(ctx.filesDir, PageImageStore.DIRECTORY)
        val newDb = File(dbFile.path + ".restoring")
        val newImages = File(ctx.filesDir, PageImageStore.DIRECTORY + ".restoring")
        try {
            File(content, BackupArchive.DATABASE).copyTo(newDb, overwrite = true)
            newImages.deleteRecursively()
            File(content, "files").copyRecursively(newImages, overwrite = true)
        } catch (e: Exception) {
            newDb.delete()
            newImages.deleteRecursively()
            pending.deleteRecursively()
            return false
        }
        AppDatabase.closeInstance()
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        newDb.renameTo(dbFile)
        images.deleteRecursively()
        newImages.renameTo(images)
        runCatching {
            val prefsFile = File(content, BackupArchive.PREFS)
            if (prefsFile.exists()) restorePrefs(ctx, json.decodeFromString(prefsFile.readText()))
        }
        pending.deleteRecursively()
        return true
    }

    // --- Le preferenze ---

    private fun snapshotPrefs(ctx: Context): Map<String, Map<String, PrefValue>> =
        PREF_NAMES.associateWith { name ->
            ctx.getSharedPreferences(name, Context.MODE_PRIVATE).all.mapNotNull { (key, value) ->
                val stored = when (value) {
                    is Boolean -> PrefValue("boolean", value.toString())
                    is Int -> PrefValue("int", value.toString())
                    is Long -> PrefValue("long", value.toString())
                    is Float -> PrefValue("float", value.toString())
                    is String -> PrefValue("string", value)
                    is Set<*> -> PrefValue("set", set = value.map { it.toString() })
                    else -> null
                }
                stored?.let { key to it }
            }.toMap()
        }

    /** Solo le preferenze di [PREF_NAMES]: un backup non può scriverne altre. */
    private fun restorePrefs(ctx: Context, saved: Map<String, Map<String, PrefValue>>) {
        saved.filterKeys { it in PREF_NAMES }.forEach { (name, values) ->
            val editor = ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            values.forEach { (key, v) ->
                when (v.type) {
                    "boolean" -> editor.putBoolean(key, v.value.toBoolean())
                    "int" -> v.value?.toIntOrNull()?.let { editor.putInt(key, it) }
                    "long" -> v.value?.toLongOrNull()?.let { editor.putLong(key, it) }
                    "float" -> v.value?.toFloatOrNull()?.let { editor.putFloat(key, it) }
                    "string" -> editor.putString(key, v.value)
                    "set" -> editor.putStringSet(key, v.set.orEmpty().toSet())
                }
            }
            editor.commit()
        }
    }
}
