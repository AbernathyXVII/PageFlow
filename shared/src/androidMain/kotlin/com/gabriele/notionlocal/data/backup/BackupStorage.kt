package com.gabriele.notionlocal.data.backup

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Un backup salvato: dove sta, come si chiama, quando è nato e quanto pesa. */
actual data class BackupFile(val uri: Uri, actual val name: String, actual val createdAt: Long, actual val size: Long)

/**
 * **Dove stanno i backup sul telefono**. Su Android 10 e dopo in
 * Download/notE backup (`MediaStoreStorage`): si vedono dall'app dei file e
 * restano anche disinstallando notE. Prima, nella cartella dell'app sulla
 * memoria del telefono. Nei test, una cartella qualsiasi (`DirectoryStorage`).
 */
interface BackupStorage {
    /** I backup, dal più nuovo. */
    fun list(): List<BackupFile>

    /** Un backup nuovo di nome [name]: [writer] ci scrive dentro. Se fallisce, non resta niente a metà. */
    fun write(name: String, writer: (OutputStream) -> Unit): BackupFile

    fun delete(file: BackupFile): Boolean

    fun open(file: BackupFile): InputStream
}

/**
 * Una cartella normale: per i telefoni prima di Android 10 e per i test. I
 * backup delle cartelle di prima ([oldDirs]) si spostano qui, col nome nuovo.
 */
class DirectoryStorage(private val dir: File, private val oldDirs: List<File> = emptyList()) : BackupStorage {

    override fun list(): List<BackupFile> {
        moveOld()
        return (dir.listFiles() ?: emptyArray()).filter { it.isFile && isBackupName(it.name) }
            .map { BackupFile(Uri.fromFile(it), it.name, it.lastModified(), it.length()) }
            .sortedWith(compareByDescending<BackupFile> { it.createdAt }.thenByDescending { it.name })
    }

    private fun moveOld() {
        val old = oldDirs.flatMap { d -> (d.listFiles() ?: emptyArray()).filter { it.isFile && isBackupName(it.name) } }
        if (old.isEmpty()) return
        dir.mkdirs()
        old.forEach { file -> file.renameTo(File(dir, renamedBackup(file.name))) }
    }

    override fun write(name: String, writer: (OutputStream) -> Unit): BackupFile {
        dir.mkdirs()
        val target = File(dir, name)
        val partial = File(dir, "$name.part")
        try {
            partial.outputStream().use(writer)
            if (!partial.renameTo(target)) throw IOException("Non si rinomina $partial")
        } finally {
            partial.delete()
        }
        return BackupFile(Uri.fromFile(target), name, target.lastModified(), target.length())
    }

    override fun delete(file: BackupFile): Boolean = File(dir, file.name).delete()

    override fun open(file: BackupFile): InputStream = File(dir, file.name).inputStream()
}

/**
 * Download/PageFlow backup, da Android 10: i file li crea l'app stessa, quindi
 * non serve nessun permesso per scriverli, leggerli e cancellarli. Mentre si
 * scrive il file è "in sospeso" e non si vede; finito, compare.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreStorage(private val context: Context) : BackupStorage {

    private val collection: Uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val relativePath = Environment.DIRECTORY_DOWNLOADS + "/" + Backups.FOLDER + "/"
    private val oldPaths = Backups.OLD_FOLDERS.map { Environment.DIRECTORY_DOWNLOADS + "/" + it + "/" }

    /**
     * I backup in "Download/notE backup", di quando l'app si chiamava notE
     * (fino al 03/11/2026): quelli che l'app ha creato si spostano nella
     * cartella nuova, col nome nuovo. Quelli che Android non le lascia spostare
     * (di un'installazione di prima) restano lì, e si vedono lo stesso.
     */
    private fun moveOld() {
        val resolver = context.contentResolver
        val where = oldPaths.joinToString(" OR ") { "${MediaStore.MediaColumns.RELATIVE_PATH} = ?" }
        val moves = mutableListOf<Pair<Uri, String>>()
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            "($where) AND ${MediaStore.MediaColumns.IS_PENDING} = 0",
            oldPaths.toTypedArray(),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (isBackupName(name)) moves += ContentUris.withAppendedId(collection, cursor.getLong(0)) to name
            }
        }
        for ((uri, name) in moves) {
            runCatching {
                resolver.update(
                    uri,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.DISPLAY_NAME, renamedBackup(name))
                    },
                    null,
                    null
                )
            }
        }
    }

    override fun list(): List<BackupFile> {
        runCatching { moveOld() }
        val paths = listOf(relativePath) + oldPaths
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val found = mutableListOf<BackupFile>()
        context.contentResolver.query(
            collection,
            projection,
            "(${paths.joinToString(" OR ") { "${MediaStore.MediaColumns.RELATIVE_PATH} = ?" }}) AND ${MediaStore.MediaColumns.IS_PENDING} = 0",
            paths.toTypedArray(),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (!isBackupName(name)) continue
                found += BackupFile(
                    uri = ContentUris.withAppendedId(collection, cursor.getLong(0)),
                    name = name,
                    createdAt = cursor.getLong(3) * 1000,
                    size = cursor.getLong(2)
                )
            }
        }
        return found.sortedWith(compareByDescending<BackupFile> { it.createdAt }.thenByDescending { it.name })
    }

    override fun write(name: String, writer: (OutputStream) -> Unit): BackupFile {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Download non disponibile")
        try {
            (resolver.openOutputStream(uri, "w") ?: throw IOException("Non si apre $uri")).use(writer)
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return list().firstOrNull { it.uri == uri } ?: BackupFile(uri, name, System.currentTimeMillis(), 0)
    }

    override fun delete(file: BackupFile): Boolean =
        runCatching { context.contentResolver.delete(file.uri, null, null) > 0 }.getOrDefault(false)

    override fun open(file: BackupFile): InputStream =
        context.contentResolver.openInputStream(file.uri) ?: throw IOException("Non si apre ${file.uri}")
}
