package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.platform.DesktopContext
import java.io.File
import java.nio.file.Files

/**
 * **`WidgetStore` è uno solo per tutti i test** (si apre una volta e poi resta
 * lì): una cartella sola per tutti i test che lo usano, tolta quando i test
 * finiscono. Con una cartella per test, il primo che lo apriva se la teneva e
 * gli altri cercavano il file dei widget nella loro (visto l'11/11/2026).
 */
object TestWidgets {
    val dir: File = Files.createTempDirectory("pageflow-widgets").toFile().also { dir ->
        Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
    }
    val context = DesktopContext(dir)
}
