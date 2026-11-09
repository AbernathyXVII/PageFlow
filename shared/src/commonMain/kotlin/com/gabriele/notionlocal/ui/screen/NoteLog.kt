package com.gabriele.notionlocal.ui.screen

/** Una riga nel registro di prova dell'editor (sul telefono `adb logcat -s NOTE:D`). */
internal expect fun noteLog(message: String)
