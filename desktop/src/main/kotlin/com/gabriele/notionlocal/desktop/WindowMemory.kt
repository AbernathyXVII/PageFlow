package com.gabriele.notionlocal.desktop

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.gabriele.notionlocal.data.settings.openSettingsStore
import com.gabriele.notionlocal.platform.DesktopContext
import java.awt.GraphicsEnvironment
import java.awt.Rectangle

/**
 * **La finestra com'era l'ultima volta** (dal 09/11/2026, chiesto dall'utente:
 * "appena si apre, la finestra è in formato mobile, e non come l'ultima volta
 * che l'ho aperta"): grandezza, posizione e se era ingrandita, nel file
 * "window" della cartella dei dati. La prima volta 1200 × 800, al centro.
 */
class WindowMemory(context: DesktopContext) {
    private val store = openSettingsStore(context, "window")

    fun initialState(): WindowState {
        val width = store.getInt(KEY_WIDTH, 0)
        val height = store.getInt(KEY_HEIGHT, 0)
        val size = if (width >= MIN_SIZE && height >= MIN_SIZE) DpSize(width.dp, height.dp) else DpSize(1200.dp, 800.dp)
        val x = store.getInt(KEY_X, NONE)
        val y = store.getInt(KEY_Y, NONE)
        // Su uno schermo che c'è ancora (uno staccato lascerebbe la finestra fuori dalla vista); se no, al centro.
        val position = if (x != NONE && y != NONE && onScreen(x, y, size)) WindowPosition(x.dp, y.dp) else WindowPosition(Alignment.Center)
        val placement = if (store.getBoolean(KEY_MAXIMIZED, false)) WindowPlacement.Maximized else WindowPlacement.Floating
        return WindowState(placement = placement, position = position, size = size)
    }

    /** Si ricorda [state]: grandezza e posizione solo da finestra normale, perché ingrandita le ha dello schermo. */
    fun save(state: WindowState) {
        val editor = store.edit().putBoolean(KEY_MAXIMIZED, state.placement == WindowPlacement.Maximized)
        if (state.placement == WindowPlacement.Floating && !state.isMinimized) {
            editor.putInt(KEY_WIDTH, state.size.width.value.toInt()).putInt(KEY_HEIGHT, state.size.height.value.toInt())
            val position = state.position
            if (position is WindowPosition.Absolute) {
                editor.putInt(KEY_X, position.x.value.toInt()).putInt(KEY_Y, position.y.value.toInt())
            }
        }
        editor.apply()
    }

    private fun onScreen(x: Int, y: Int, size: DpSize): Boolean = runCatching {
        val window = Rectangle(x, y, size.width.value.toInt(), size.height.value.toInt())
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { screen ->
            val visible = screen.defaultConfiguration.bounds.intersection(window)
            visible.width >= MIN_VISIBLE && visible.height >= MIN_VISIBLE
        }
    }.getOrDefault(false)

    private companion object {
        const val KEY_WIDTH = "width"
        const val KEY_HEIGHT = "height"
        const val KEY_X = "x"
        const val KEY_Y = "y"
        const val KEY_MAXIMIZED = "maximized"
        const val NONE = Int.MIN_VALUE
        const val MIN_SIZE = 300
        const val MIN_VISIBLE = 100
    }
}
