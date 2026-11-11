package com.gabriele.notionlocal.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.platform.isPc

/** Il corpo del testo quando non se ne è scelto uno: quello di sempre. */
const val DEFAULT_PAGE_FONT_SIZE = 16
const val MIN_PAGE_FONT_SIZE = 5
const val MAX_PAGE_FONT_SIZE = 72

/**
 * **Sul PC il testo è grande come in Notion** (dal 11/11/2026, chiesto
 * dall'utente con una foto di Notion accanto a PageFlow, misurata pixel per
 * pixel: il suo Notion ha "Small text" e lo zoom al 90%, cioè il testo a
 * 12,6 dove PageFlow lo scrive a 16). Il tema (`NotionLocalTheme`)
 * rimpicciolisce tutto quello che è scritto in sp di questo tanto, così
 * pallini, numeri e altezze delle righe restano in proporzione col testo;
 * `PcTypography` corregge le poche cose che in Notion hanno un'altra
 * misura. Sul telefono niente.
 */
const val PC_TEXT_SCALE = 12.6f / 16f

/** Quanto il tema rimpicciolisce il testo: [PC_TEXT_SCALE] sul PC, niente sul telefono. */
val TEXT_SCALE: Float get() = if (isPc) PC_TEXT_SCALE else 1f

/**
 * Una grandezza scelta a mano dalla barra Aa, in sp: sul PC compensa il tema,
 * così il testo ingrandito dall'utente resta grande come l'ha scelto (lo ha
 * chiesto lui, l'11/11/2026).
 */
fun chosenSizeSp(size: Int): TextUnit = (size / TEXT_SCALE).sp
