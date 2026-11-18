package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Chiara sul fondo scuro (quella di serie è nera), più chiara col mouse sopra. */
@Composable
actual fun HorizontalBar(state: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = ScrollbarStyle(
            minimalHeight = 24.dp,
            thickness = 4.dp,
            shape = RoundedCornerShape(2.dp),
            hoverDurationMillis = 200,
            unhoverColor = Color.White.copy(alpha = 0.2f),
            hoverColor = Color.White.copy(alpha = 0.45f)
        )
    )
}
