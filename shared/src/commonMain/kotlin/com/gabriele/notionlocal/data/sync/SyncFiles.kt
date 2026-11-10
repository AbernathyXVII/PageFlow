package com.gabriele.notionlocal.data.sync

import com.gabriele.notionlocal.data.backup.BackupArchive
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.IOException

/**
 * **I file delle pagine** (immagini, audio, video, PDF…) nella
 * sincronizzazione: hanno nomi unici, quindi va solo quello che dall'altra
 * parte manca, e non si cambia mai un file che c'è già.
 */
object SyncFiles {
    private const val PART = ".part"

    /** I file della cartella, col percorso ("a/b.png") e la grandezza; non quelli a metà. */
    fun list(dir: File): Map<String, Long> =
        BackupArchive.filesOf(dir).filter { !it.first.endsWith(PART) }.associate { (path, file) -> path to file.length() }

    /** Manda [names]: per ognuno nome, grandezza (-1 se non c'è più) e contenuto. */
    fun send(out: DataOutputStream, dir: File, names: Collection<String>) {
        out.writeInt(names.size)
        for (name in names) {
            out.writeUTF(name)
            val file = File(dir, name).takeIf { safe(name) && it.isFile }
            if (file == null) {
                out.writeLong(-1)
                continue
            }
            val size = file.length()
            out.writeLong(size)
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER)
                var left = size
                while (left > 0) {
                    val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                    if (n < 0) throw IOException("$name è più corto di prima")
                    out.write(buffer, 0, n)
                    left -= n
                }
            }
        }
    }

    /** Riceve i file di `send` in [dir]: prima in un ".part", poi col suo nome. Quanti ne sono arrivati. */
    fun receive(input: DataInputStream, dir: File): Int {
        var count = 0
        repeat(input.readInt()) {
            val name = input.readUTF()
            val size = input.readLong()
            if (size < 0) return@repeat
            if (!safe(name)) throw IOException("nome di file non valido: $name")
            val target = File(dir, name)
            val part = File(target.path + PART)
            part.parentFile?.mkdirs()
            part.outputStream().use { output ->
                val buffer = ByteArray(BUFFER)
                var left = size
                while (left > 0) {
                    val n = input.read(buffer, 0, minOf(buffer.size.toLong(), left).toInt())
                    if (n < 0) throw EOFException()
                    output.write(buffer, 0, n)
                    left -= n
                }
            }
            if (target.exists()) {
                part.delete()
            } else if (part.renameTo(target)) {
                count++
            }
        }
        return count
    }

    /** Un nome che resta nella cartella: niente "..", percorsi assoluti o barre di Windows. */
    fun safe(name: String): Boolean =
        name.isNotEmpty() && !name.startsWith("/") && '\\' !in name && ':' !in name &&
            name.split('/').none { it.isEmpty() || it == "." || it == ".." }

    private const val BUFFER = 64 * 1024
}
