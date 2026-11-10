package com.gabriele.notionlocal.data.sync

import java.net.InetAddress

/** Il PC aspetta il telefono (`SyncServer`). */
actual val syncWaitsForPhone: Boolean = true

/** Il nome del PC, quello di Windows. */
actual fun syncDeviceName(): String =
    System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }
        ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
        ?: "PC"
