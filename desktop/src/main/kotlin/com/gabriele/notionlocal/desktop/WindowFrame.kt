package com.gabriele.notionlocal.desktop

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.invoke.MethodHandle

/**
 * **La barra in alto della finestra come l'app** (dal 13/11/2026, chiesto
 * dall'utente: era bianca). Scura col tema scuro (i tre pulsanti di Windows
 * diventano bianchi), e col vetro di Windows 11 (l'acrilico: si vede, sfocato,
 * quello che sta dietro la finestra) dove c'è; se in Windows gli effetti di
 * trasparenza sono spenti, o su Windows 10, resta solo scura. Il contenuto
 * della finestra è tutto disegnato dall'app, quindi il vetro si vede solo
 * nella barra.
 *
 * Le chiamate a Windows (`DwmSetWindowAttribute`) passano dall'API "foreign
 * function" di Java 21 (`java.lang.foreign`), senza librerie in più; se
 * qualcosa non va, la barra resta com'era.
 */
object WindowFrame {
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    /** Lo stesso su Windows 10 prima della 20H1. */
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE_OLD = 19
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    /** L'acrilico, il vetro (Windows 11 22H2 e dopo). */
    private const val DWMSBT_TRANSIENTWINDOW = 3
    /** SWP_FRAMECHANGED | NOMOVE | NOSIZE | NOZORDER | NOACTIVATE: la barra si ridisegna subito. */
    private const val SWP_REDRAW_FRAME = 0x0020 or 0x0002 or 0x0001 or 0x0004 or 0x0010

    private class Calls {
        private val linker = Linker.nativeLinker()
        private val arena = Arena.global()
        val setAttribute: MethodHandle = linker.downcallHandle(
            SymbolLookup.libraryLookup("dwmapi", arena).find("DwmSetWindowAttribute").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT)
        )
        val setWindowPos: MethodHandle = linker.downcallHandle(
            SymbolLookup.libraryLookup("user32", arena).find("SetWindowPos").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
        )
    }

    private val calls: Calls? by lazy {
        if (System.getProperty("os.name").orEmpty().startsWith("Windows")) runCatching { Calls() }.getOrNull() else null
    }

    /** Scura se [dark], e col vetro: la finestra [hwnd] (`ComposeWindow.windowHandle`). */
    fun style(hwnd: Long, dark: Boolean) {
        val c = calls ?: return
        if (hwnd == 0L) return
        runCatching {
            val window = MemorySegment.ofAddress(hwnd)
            Arena.ofConfined().use { arena ->
                val value = arena.allocate(JAVA_INT)
                fun set(attribute: Int, v: Int): Int {
                    value.set(JAVA_INT, 0, v)
                    return c.setAttribute.invokeWithArguments(window, attribute, value, 4) as Int
                }
                val mode = if (dark) 1 else 0
                if (set(DWMWA_USE_IMMERSIVE_DARK_MODE, mode) != 0) set(DWMWA_USE_IMMERSIVE_DARK_MODE_OLD, mode)
                set(DWMWA_SYSTEMBACKDROP_TYPE, DWMSBT_TRANSIENTWINDOW)
            }
            c.setWindowPos.invokeWithArguments(window, MemorySegment.NULL, 0, 0, 0, 0, SWP_REDRAW_FRAME)
        }
    }
}
