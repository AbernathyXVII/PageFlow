package com.gabriele.notionlocal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.gabriele.notionlocal.platform.isPc

/**
 * **Sul PC le misure di Notion, sul telefono quelle per il dito** (dal
 * 11/11/2026, seconda fase dell'aspetto da Notion, chiesto dall'utente):
 * righe, icone e margini misurati nelle foto di Notion dell'utente. Per
 * esempio le voci della barra laterale stanno a 28 l'una dall'altra in
 * Notion e a 36 qui, le righe delle tabelle a 32 e a 44.
 */
fun pcOr(pc: Dp, phone: Dp): Dp = if (isPc) pc else phone

/**
 * Sul PC, dentro un gruppo di voci di un menu, le linee fra una voce e
 * l'altra non si vedono, come nei menu di Notion: il colore delle linee
 * (`outlineVariant`, quello di `HorizontalDivider`) diventa trasparente.
 * Sul telefono restano.
 */
@Composable
fun NoRowLinesOnPc(content: @Composable () -> Unit) {
    if (!isPc) return content()
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(outlineVariant = Color.Transparent),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}
