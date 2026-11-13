package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Sul PC il campo di testo apre il suo menu del tasto destro da
 * `LocalTextContextMenu`: qui al suo posto c'è quello della pagina. Da Compose
 * arrivano le azioni vere del campo (taglia, copia, incolla: quelle di Ctrl+X,
 * Ctrl+C, Ctrl+V), che la pagina mette nel suo menu. Esc lo chiude, perché il
 * menu non prende il fuoco e i tasti arrivano al campo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun PageTextMenuArea(content: @Composable () -> Unit) {
    val host = LocalPageTextMenu.current
    if (host == null) {
        content()
        return
    }
    val menu = remember(host) { PageTextContextMenu(host) }
    CompositionLocalProvider(LocalTextContextMenu provides menu, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
private class PageTextContextMenu(private val host: PageTextMenuHost) : TextContextMenu {
    @Composable
    override fun Area(textManager: TextContextMenu.TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        val manager by rememberUpdatedState(textManager)
        val where = remember { arrayOfNulls<LayoutCoordinates>(1) }
        val edit = remember {
            fun run(action: () -> TextContextMenu.Action?) {
                action()?.takeIf { it.enabled }?.execute?.invoke()
            }
            TextEditActions(
                cut = { run { manager.cut } },
                copy = { run { manager.copy } },
                paste = { run { manager.paste } }
            )
        }
        Box(
            modifier = Modifier
                .onGloballyPositioned { where[0] = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                            val at = where[0]?.takeIf { it.isAttached }?.localToWindow(event.changes.first().position) ?: continue
                            // Fermato qui: il menu di Compose non si apre anche lui.
                            event.changes.forEach { it.consume() }
                            host.open(at, edit)
                        }
                    }
                }
                .onPreviewKeyEvent {
                    if (it.key == Key.Escape && it.type == KeyEventType.KeyDown && host.isOpen()) {
                        host.close()
                        true
                    } else {
                        false
                    }
                }
        ) {
            content()
        }
    }
}
