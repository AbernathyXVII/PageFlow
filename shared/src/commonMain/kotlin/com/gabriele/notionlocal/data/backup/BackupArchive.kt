package com.gabriele.notionlocal.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * **Il backup** (dal 28/10/2026, chiesto dall'utente): un file zip con dentro
 * tutto notE.
 * - `notE-backup.json`: quando è nato, con quale versione dell'app e del
 *   database, perché (ogni giorno, a mano, prima di un ripristino);
 * - `database.db`: il database, copiato tutto insieme in un istante;
 * - `files/`: le immagini e i file della cartella dell'app (`PageImageStore`);
 * - `prefs.json`: le impostazioni, i widget, gli strumenti della penna e la
 *   password delle pagine bloccate.
 *
 * Il resto (la cache, le sveglie) l'app lo rifà da sola.
 */
@Serializable
data class BackupManifest(
    val format: Int = BackupArchive.FORMAT,
    val createdAt: Long,
    val appVersion: String = "",
    val databaseVersion: Int,
    val files: Int = 0,
    val reason: String = BackupReason.DAILY.name
)

/** Perché è nato un backup (prima della prima sincronizzazione con un dispositivo: dal 10/11/2026). */
enum class BackupReason { DAILY, MANUAL, BEFORE_RESTORE, BEFORE_SYNC }

/** Il valore di una preferenza, col suo tipo, per rimetterla com'era. */
@Serializable
data class PrefValue(val type: String, val value: String? = null, val set: List<String>? = null)

/** Un backup aperto: il manifest, il database, la cartella dei file e le preferenze, tolti dallo zip. */
class ExtractedBackup(
    val manifest: BackupManifest,
    val database: File,
    val files: File,
    val prefs: Map<String, Map<String, PrefValue>>
)

/** Perché un file non si può ripristinare. */
class BackupException(val problem: Problem, message: String) : IOException(message) {
    enum class Problem {
        /** Non è un backup di notE (manca il database o il manifest, o non è uno zip). */
        NOT_A_BACKUP,
        /** Fatto da una versione di notE più nuova di questa. */
        TOO_NEW,

        /** Fatto da una versione più vecchia, che il PC non sa aggiornare (le migrazioni vecchie sono solo del telefono). */
        TOO_OLD
    }
}

object BackupArchive {
    const val FORMAT = 1
    const val MANIFEST = "PageFlow-backup.json"

    /** Il manifest dei backup di quando l'app si chiamava notE (fino al 03/11/2026). */
    const val OLD_MANIFEST = "notE-backup.json"
    const val DATABASE = "database.db"
    const val FILES = "files/"
    const val PREFS = "prefs.json"

    /** Le impostazioni che vanno nel backup (e solo quelle si ripristinano): le stesse su telefono e PC. */
    val PREF_NAMES = listOf("settings", "widgets", "ink", "app_lock")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun write(
        out: OutputStream,
        manifest: BackupManifest,
        database: File,
        filesDir: File,
        prefs: Map<String, Map<String, PrefValue>>
    ) {
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(json.encodeToString(manifest).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATABASE))
            database.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            filesOf(filesDir).forEach { (path, file) ->
                zip.putNextEntry(ZipEntry(FILES + path))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry(PREFS))
            zip.write(json.encodeToString(prefs).toByteArray())
            zip.closeEntry()
        }
    }

    /** I file di [dir], anche nelle sottocartelle, col loro percorso relativo ("a/b.png"), in ordine. */
    fun filesOf(dir: File): List<Pair<String, File>> =
        if (!dir.isDirectory) {
            emptyList()
        } else {
            dir.walkTopDown().filter { it.isFile }
                .map { it.relativeTo(dir).invariantSeparatorsPath to it }
                .sortedBy { it.first }
                .toList()
        }

    /**
     * Toglie lo zip in [dir] (che deve essere vuota) e ne legge le parti.
     * **Nessun file esce da [dir]**: un nome con ".." o un percorso assoluto
     * dentro lo zip lo rende "non un backup", invece di scrivere altrove.
     */
    fun read(input: InputStream, dir: File): ExtractedBackup {
        dir.mkdirs()
        val root = dir.canonicalFile
        val files = File(root, "files").apply { mkdirs() }
        val database = File(root, DATABASE)
        var manifest: BackupManifest? = null
        var prefs: Map<String, Map<String, PrefValue>> = emptyMap()
        var hasDatabase = false
        try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (entry.isDirectory) continue
                    when {
                        name == MANIFEST || name == OLD_MANIFEST -> manifest = json.decodeFromString<BackupManifest>(zip.readBytes().decodeToString())
                        name == PREFS -> prefs = json.decodeFromString(zip.readBytes().decodeToString())
                        name == DATABASE -> {
                            database.outputStream().use { zip.copyTo(it) }
                            hasDatabase = true
                        }
                        name.startsWith(FILES) -> {
                            val target = File(files, name.removePrefix(FILES)).canonicalFile
                            if (!target.path.startsWith(files.canonicalPath + File.separator)) {
                                throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Percorso fuori dalla cartella: $name")
                            }
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                        }
                        // Il resto non è di notE: lo si lascia stare.
                        else -> Unit
                    }
                }
            }
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Non si legge: ${e.message}")
        }
        val found = manifest ?: throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Manca $MANIFEST")
        if (!hasDatabase) throw BackupException(BackupException.Problem.NOT_A_BACKUP, "Manca $DATABASE")
        if (found.format > FORMAT) throw BackupException(BackupException.Problem.TOO_NEW, "Formato ${found.format}")
        return ExtractedBackup(found, database, files, prefs)
    }
}
