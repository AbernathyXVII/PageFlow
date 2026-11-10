package com.gabriele.notionlocal.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual val canPinSidebar: Boolean = true

actual fun Modifier.horizontalResizeCursor(): Modifier = pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
