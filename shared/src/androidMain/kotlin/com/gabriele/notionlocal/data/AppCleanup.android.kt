package com.gabriele.notionlocal.data

actual suspend fun clearAllTablesOf(db: AppDatabase) = db.clearAllTables()
