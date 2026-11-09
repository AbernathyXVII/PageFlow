package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Sul PC il gesto "indietro" è il tasto **Esc** (dal 09/11/2026): la finestra
 * di Compose lo passa a questi gestori, come il telefono il suo gesto. Così Esc
 * chiude la barra laterale, toglie il cursore dalla riga, e poi torna indietro.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) =
    androidx.compose.ui.backhandler.BackHandler(enabled = enabled, onBack = onBack)
