package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Sul PC le finestrelle sono un riquadro accanto al clic (vedi
 * `PcPanel.desktop.kt`), chiamato da `NoteBottomSheet` al posto del pannello
 * che sale dal basso. Sul telefono non si usa.
 */
@Composable
expect fun PcPanel(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
)
