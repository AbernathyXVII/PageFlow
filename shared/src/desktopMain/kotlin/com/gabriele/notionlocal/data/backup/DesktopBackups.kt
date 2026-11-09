package com.gabriele.notionlocal.data.backup

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.gabriele.notionlocal.data.DATABASE_VERSION
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.settings.openSettingsStore
import com.gabriele.notionlocal.platform.DesktopContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** Un backup fatto da una versione di PageFlow più vecchia di questa: il PC non sa ancora aggiornarne il database. */
class OlderBackupException(message: String) : IOException(message)

/**
 * **Aprire sul PC un backup del telefono** (dal 09/11/2026): lo stesso file
 * zip che fa il telefono (`BackupArchive`), in due tempi come sul telefono.
 * [prepareRestore] lo toglie dallo zip e controlla il database; i file al
 * loro posto li mette [applyPendingRestore], alla riapertura, prima che
 * impostazioni e database vengano letti.
 *
 * **Niente si perde**: il database, le immagini e le impostazioni che il PC
 * aveva finiscono in `before-restore-<ora>` nella cartella dei dati.
 */
object DesktopBackups {
    private const val PENDING = "restore-pending"
    private const val READY = "ready"
    private const val DATABASE_NAME = "notion_local.db"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Toglie [file] dallo zip e lo controlla; alla prossima apertura [applyPendingRestore] lo mette al suo posto. */
    fun prepareRestore(context: DesktopContext, file: File): BackupManifest {
        val pending = File(context.dataDir, PENDING).apply { deleteRecursively(); mkdirs() }
        try {
            val content = File(pending, "content")
            val extracted = file.inputStream().use { BackupArchive.read(it, content) }
            checkDatabase(extracted.database)
            File(content, BackupArchive.PREFS).writeText(json.encodeToString(extracted.prefs))
            File(pending, READY).writeText(extracted.manifest.createdAt.toString())
            return extracted.manifest
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
        if (version < DATABASE_VERSION) throw OlderBackupException("Database $version, app $DATABASE_VERSION")
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
