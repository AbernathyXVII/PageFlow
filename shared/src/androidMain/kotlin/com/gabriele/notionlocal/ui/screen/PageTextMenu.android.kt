package com.gabriele.notionlocal.ui.screen

import androidx.compose.runtime.Composable

/** Sul telefono niente tasto destro: il campo resta com'è (la barra sta sopra la tastiera). */
@Composable
actual fun PageTextMenuArea(content: @Composable () -> Unit) = content()
