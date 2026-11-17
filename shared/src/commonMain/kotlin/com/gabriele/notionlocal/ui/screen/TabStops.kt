package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import kotlin.math.floor

/**
 * **Il Tab vero** (dal 17/11/2026, chiesto dall'utente: "vorrei che facesse
 * il classico Tab che si fa su OneNote e Word"): un carattere di tabulazione
 * arriva fino al prossimo punto di tabulazione, ogni [STOP_EM] volte la
 * grandezza del testo dall'inizio della riga — come i punti ogni 1,27 cm di
 * Word —, così le parole dopo un Tab si allineano da una riga all'altra.
 * Compose lo disegnava largo come uno spazio: qui gli si aggiunge la
 * spaziatura che manca. Solo aspetto: nel testo resta un carattere solo.
 *
 * Si misura la riga fino al Tab ([measurer], con lo [style] del campo): una
 * riga che va a capo da sola ha i Tab della seconda parte misurati come se
 * continuasse dritta.
 */
class TabStops(private val measurer: TextMeasurer, private val style: TextStyle, private val density: Density) {
    fun widen(text: AnnotatedString): AnnotatedString {
        if ('\t' !in text.text) return text
        val stop = with(density) { style.fontSize.toPx() } * STOP_EM
        if (stop <= 0f) return text
        var current = text
        var lineStart = 0
        text.text.forEachIndexed { at, char ->
            if (char == '\n') lineStart = at + 1
            if (char != '\t') return@forEachIndexed
            // La riga fino al Tab compreso, coi Tab di prima già allargati.
            val layout = measurer.measure(current.subSequence(lineStart, at + 1), style, softWrap = false, maxLines = 1)
            val x = layout.getHorizontalPosition(at - lineStart, usePrimaryDirection = true)
            val glyph = layout.getHorizontalPosition(at - lineStart + 1, usePrimaryDirection = true) - x
            var target = (floor(x / stop) + 1) * stop
            while (target - x < glyph) target += stop
            val extra = (target - x - glyph) / (density.density * density.fontScale)
            current = AnnotatedString.Builder(current).apply { addStyle(SpanStyle(letterSpacing = extra.sp), at, at + 1) }.toAnnotatedString()
        }
        return current
    }

    companion object {
        /** Un punto di tabulazione ogni tre volte la grandezza del testo: a 16 punti, 48 (1,27 cm a schermo). */
        const val STOP_EM = 3f
    }
}
