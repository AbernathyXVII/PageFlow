package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Sul telefono si scorre col dito: nessuna barra. */
@Composable
actual fun HorizontalBar(state: ScrollState, modifier: Modifier) = Unit
