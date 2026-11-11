package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Sul telefono le finestrelle salgono dal basso (`NoteBottomSheet`): questo non si chiama mai. */
@Composable
actual fun PcPanel(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) = Unit
