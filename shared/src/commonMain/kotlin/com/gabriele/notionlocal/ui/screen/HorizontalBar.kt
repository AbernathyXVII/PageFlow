package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * **Una barra di scorrimento orizzontale**, sottile, da prendere col mouse: sul PC per le viste di
 * un database in fila (vedi `ViewTabs`). Sul telefono non c'è: lì si scorre col dito.
 */
@Composable
expect fun HorizontalBar(state: ScrollState, modifier: Modifier = Modifier)
