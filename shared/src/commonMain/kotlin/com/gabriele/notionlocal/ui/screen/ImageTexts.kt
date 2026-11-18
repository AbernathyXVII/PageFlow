package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.IMAGE_LEFT
import com.gabriele.notionlocal.data.entity.IMAGE_RIGHT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Come va il testo sotto o sopra un'immagine allineata [align] (vedi `BlockAttrs.align`). */
fun imageTextAlign(align: String?): TextAlign = when (align) {
    IMAGE_LEFT -> TextAlign.Start
    IMAGE_RIGHT -> TextAlign.End
    else -> TextAlign.Center
}

/**
 * **La didascalia sotto un'immagine, o il suo titolo sopra** (dal 18/11/2026, chiesto dall'utente,
 * come su Notion): larga quanto l'immagine e allineata come lei, così la segue a sinistra, al centro
 * o a destra. Si scrive lì; ogni lettera si salva nel blocco ([onChange]). Lasciata vuota, uscendo
 * se ne va ([onLeftEmpty]). [focus] la apre col cursore dentro, appena scelta dal menu.
 */
@Composable
fun ImageTextField(
    text: String,
    hint: String,
    style: TextStyle,
    color: Color,
    align: TextAlign,
    locked: Boolean,
    focus: Boolean,
    onFocused: () -> Unit,
    onChange: (String) -> Unit,
    onLeftEmpty: () -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    var focused by remember { mutableStateOf(false) }
    // Cambiata da fuori (Annulla, l'altro dispositivo) mentre non ci si scrive.
    LaunchedEffect(text) { if (!focused && text != value.text) value = TextFieldValue(text) }
    val requester = remember { FocusRequester() }
    LaunchedEffect(focus) {
        if (!focus) return@LaunchedEffect
        // Un giro, che il campo appena nato sia al suo posto.
        delay(50)
        runCatching { requester.requestFocus() }
        onFocused()
    }
    // Ctrl+A qui: prima tutto il suo testo, poi la pagina (vedi `BlockSelectAll`).
    val blockSelectAll = LocalBlockSelectAll.current
    if (blockSelectAll != null && focused) SideEffect {
        blockSelectAll.inField = {
            val all = TextRange(0, value.text.length)
            val take = focused && value.text.isNotEmpty() && value.selection != all
            if (take) value = value.copy(selection = all)
            take
        }
    }
    val textStyle = style.copy(color = color, textAlign = align)
    // La maiuscola a inizio frase della tastiera del telefono, come negli altri campi.
    AutoCaps {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.text != value.text) onChange(it.text)
            value = it
        },
        readOnly = locked,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .focusRequester(requester)
            .onFocusChanged { state ->
                if (focused && !state.isFocused && value.text.isBlank()) onLeftEmpty()
                focused = state.isFocused
            },
        decorationBox = { inner ->
            // Largo quanto l'immagine; il testo, largo quanto è, sta dove dice l'allineamento
            // (dentro un campo il testo non si allarga da solo, e l'allineamento non si vedrebbe).
            val place = when (align) {
                TextAlign.Start -> Alignment.TopStart
                TextAlign.End -> Alignment.TopEnd
                else -> Alignment.TopCenter
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = place) {
                if (value.text.isEmpty()) Text(hint, style = textStyle.copy(color = color.copy(alpha = 0.45f)))
                inner()
            }
        }
    )
    }
}
