package com.gabriele.notionlocal.data.sync

import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/**
 * **Le righe che la sincronizzazione si passa** (dal 10/11/2026): una riga è
 * colonna → valore, come SQLite la tiene (Long, Double, String, ByteArray o
 * null). Si copiano tali e quali: niente da riscrivere quando le tabelle
 * cambiano, basta che telefono e PC abbiano la stessa versione del database.
 */
typealias SyncRow = Map<String, Any?>

/**
 * **Un pezzo che si sincronizza tutto insieme**, con le sue righe tabella per
 * tabella: una pagina ("p:" e il suo id, coi blocchi, le celle delle tabelle,
 * i disegni e, per un database, le colonne e le automazioni), una riga di
 * database ("r:", con le sue celle) o un quaderno ("n:", con le sezioni).
 */
class SyncUnit(val id: String, val tables: Map<String, List<SyncRow>>) {
    /** L'impronta del contenuto: uguale sui due lati se le righe sono le stesse, in qualunque ordine. */
    val hash: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val out = DataOutputStream(DigestSink(digest))
        for (table in tables.keys.sorted()) {
            out.writeUTF(table)
            val local = LOCAL_ONLY[table].orEmpty()
            for (row in tables.getValue(table).sortedBy { keyOf(table, it) }) {
                for (column in row.keys.sorted()) {
                    if (column in local) continue
                    out.writeUTF(column)
                    writeValue(out, row[column])
                }
                out.writeByte(0xFF)
            }
        }
        out.flush()
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun write(out: DataOutputStream) {
        out.writeUTF(id)
        out.writeInt(tables.size)
        for ((table, rows) in tables) {
            out.writeUTF(table)
            writeRows(out, rows)
        }
    }

    companion object {
        fun read(input: DataInputStream): SyncUnit {
            val id = input.readUTF()
            val tables = LinkedHashMap<String, List<SyncRow>>()
            repeat(input.readInt()) { tables[input.readUTF()] = readRows(input) }
            return SyncUnit(id, tables)
        }

        /** La chiave di una riga: "id", o quella delle due tabelle che non ce l'hanno. */
        fun keyOf(table: String, row: SyncRow): String = (row[keyColumn(table)] ?: "").toString()

        /**
         * Colonne che restano di ogni lato: non contano nell'impronta e non si
         * sovrascrivono (una riga nuova le prende come sono arrivate). L'ultima
         * volta che un'automazione è partita, e i quaderni aperti o chiusi nella
         * barra laterale.
         */
        val LOCAL_ONLY = mapOf(
            "automations" to setOf("lastRunAt"),
            "notebooks" to setOf("isExpanded"),
            "notebook_sections" to setOf("isExpanded")
        )

        fun keyColumn(table: String): String = when (table) {
            "personal_words" -> "word"
            "image_texts" -> "file"
            else -> "id"
        }
    }
}

fun writeRows(out: DataOutputStream, rows: List<SyncRow>) {
    out.writeInt(rows.size)
    for (row in rows) {
        out.writeInt(row.size)
        for ((column, value) in row) {
            out.writeUTF(column)
            writeValue(out, value)
        }
    }
}

fun readRows(input: DataInputStream): List<SyncRow> = List(input.readInt()) {
    val row = LinkedHashMap<String, Any?>()
    repeat(input.readInt()) { row[input.readUTF()] = readValue(input) }
    row
}

fun writeValue(out: DataOutputStream, value: Any?) {
    when (value) {
        null -> out.writeByte(0)
        is Long -> { out.writeByte(1); out.writeLong(value) }
        is Int -> { out.writeByte(1); out.writeLong(value.toLong()) }
        is Double -> { out.writeByte(2); out.writeDouble(value) }
        is String -> { out.writeByte(3); writeLongText(out, value) }
        is ByteArray -> { out.writeByte(4); out.writeInt(value.size); out.write(value) }
        else -> error("valore che SQLite non tiene: ${value::class.simpleName}")
    }
}

fun readValue(input: DataInputStream): Any? = when (val tag = input.readByte().toInt()) {
    0 -> null
    1 -> input.readLong()
    2 -> input.readDouble()
    3 -> readLongText(input)
    4 -> ByteArray(input.readInt()).also { input.readFully(it) }
    else -> error("valore sconosciuto: $tag")
}

/** Un testo di qualunque lunghezza (`writeUTF` si ferma a 64 KB, e una riga può essere più lunga). */
fun writeLongText(out: DataOutputStream, text: String) {
    val bytes = text.encodeToByteArray()
    out.writeInt(bytes.size)
    out.write(bytes)
}

fun readLongText(input: DataInputStream): String = ByteArray(input.readInt()).also { input.readFully(it) }.decodeToString()

/** Dove scrivere per calcolare un'impronta senza tenere tutto in memoria. */
private class DigestSink(private val digest: MessageDigest) : java.io.OutputStream() {
    override fun write(b: Int) = digest.update(b.toByte())
    override fun write(b: ByteArray, off: Int, len: Int) = digest.update(b, off, len)
}
