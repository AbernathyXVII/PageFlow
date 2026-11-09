package com.gabriele.notionlocal.data

import androidx.room.execSQL
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Sul PC Room non ha `clearAllTables`: si svuotano una per una tutte le tabelle
 * del database (non quelle di SQLite e di Room), in una transazione, coi
 * controlli delle chiavi esterne rimandati alla fine, quando sono vuote tutte.
 */
actual suspend fun clearAllTablesOf(db: AppDatabase) {
    db.useWriterConnection { transactor ->
        val tables = transactor.usePrepared(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'"
        ) { statement -> buildList { while (statement.step()) add(statement.getText(0)) } }
        transactor.immediateTransaction {
            execSQL("PRAGMA defer_foreign_keys = TRUE")
            tables.forEach { execSQL("DELETE FROM `$it`") }
        }
    }
}
