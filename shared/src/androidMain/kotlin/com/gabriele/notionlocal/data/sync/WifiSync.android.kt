package com.gabriele.notionlocal.data.sync

import android.os.Build

/** Il telefono cerca il PC: non aspetta nessuno. */
actual val syncWaitsForPhone: Boolean = false

/** Il modello del telefono, come lo mostra il PC ("Samsung SM-S918B"). */
actual fun syncDeviceName(): String {
    val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
    val model = Build.MODEL.orEmpty()
    return if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model".trim()
}
