package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

/**
 * **Il menu del tasto destro sul PC** (dal 13/11/2026, chiesto dall'utente):
 * cliccando col tasto destro su una riga della pagina si apre, in colonna,
 * quello che sul telefono sta nella barra sopra la tastiera — dai sei puntini
 * a Undo e Redo — con l'aspetto dei menu di Windows 11: scuro, angoli tondi,
 * l'icona a sinistra, la scorciatoia in grigio a destra, i sottomenu che si
 * aprono passandoci sopra col mouse.
 */
sealed interface PcMenuEntry

class PcMenuItem(
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
    /** A destra, in grigio: la scorciatoia da tastiera, o il valore di adesso (il font). */
    val hint: String? = null,
    val enabled: Boolean = true,
    /** Le voci che si aprono di lato passandoci sopra. */
    val submenu: List<PcMenuEntry> = emptyList(),
    /** Null con un sottomenu: il clic lo apre e basta. */
    val onClick: (() -> Unit)? = null
) : PcMenuEntry

object PcMenuSeparator : PcMenuEntry

/** L'altezza di una voce: come nei menu di Windows usati col mouse. */
val PC_MENU_ROW_HEIGHT = 30.dp

private val PcMenuBackground = Color(0xFF2C2C2C)
private val PcMenuBorder = Color(0xFF454545)
private val PcMenuHover = Color(0xFF3D3D3D)
private val PcMenuText = Color(0xFFF3F3F3)
private val PcMenuHint = Color(0xFF9D9D9D)
private val PcMenuShape = RoundedCornerShape(8.dp)

/**
 * Il menu aperto, con l'angolo in alto a sinistra sul punto dove sta chi lo
 * chiama (un `Box` grande zero, messo dove si è cliccato). **Non prende il
 * fuoco**: il cursore e il testo selezionato restano nella riga, e le voci
 * (grassetto, colore, incolla…) sanno su cosa agire.
 */
@Composable
fun PcContextMenu(
    entries: List<PcMenuEntry>,
    onDismiss: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    /** Scelta una voce (anche di un sottomenu): chiude tutto il menu. */
    onPicked: () -> Unit = onDismiss
) {
    var openSubmenu by remember(entries) { mutableStateOf<Int?>(null) }
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = offset,
        properties = PopupProperties(focusable = false),
        shape = PcMenuShape,
        containerColor = PcMenuBackground,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, PcMenuBorder)
    ) {
        entries.forEachIndexed { index, entry ->
            when (entry) {
                PcMenuSeparator -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = PcMenuBorder
                )
                is PcMenuItem -> PcMenuRow(
                    item = entry,
                    submenuOpen = openSubmenu == index,
                    onHover = { openSubmenu = if (entry.submenu.isNotEmpty()) index else null },
                    onPick = {
                        val action = entry.onClick
                        if (action != null) {
                            onPicked()
                            action()
                        } else if (entry.submenu.isNotEmpty()) {
                            openSubmenu = index
                        }
                    },
                    onCloseSubmenu = { openSubmenu = null },
                    onDismissAll = onPicked
                )
            }
        }
    }
}

@Composable
private fun PcMenuRow(
    item: PcMenuItem,
    submenuOpen: Boolean,
    onHover: () -> Unit,
    onPick: () -> Unit,
    onCloseSubmenu: () -> Unit,
    onDismissAll: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val latestHover by rememberUpdatedState(onHover)
    LaunchedEffect(hovered) { if (hovered) latestHover() }
    val color = if (item.enabled) PcMenuText else PcMenuText.copy(alpha = 0.38f)
    Box {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .height(PC_MENU_ROW_HEIGHT)
                .clip(RoundedCornerShape(4.dp))
                .background(if (item.enabled && (hovered || submenuOpen)) PcMenuHover else Color.Transparent)
                .hoverable(interaction)
                .clickable(enabled = item.enabled, interactionSource = interaction, indication = null, onClick = onPick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides color) { item.icon?.invoke() }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(item.label, color = color, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
            item.hint?.let {
                Spacer(modifier = Modifier.width(32.dp))
                Text(it, color = PcMenuHint, fontSize = 12.sp, maxLines = 1)
            }
            if (item.submenu.isNotEmpty()) {
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
        if (submenuOpen && item.submenu.isNotEmpty()) {
            // Il sottomenu parte dal bordo destro della voce, alla sua altezza.
            Box(modifier = Modifier.align(Alignment.TopEnd).size(0.dp)) {
                PcContextMenu(item.submenu, onDismiss = onCloseSubmenu, offset = DpOffset(0.dp, (-8).dp), onPicked = onDismissAll)
            }
        }
    }
}

/** Le azioni di modifica del campo sotto il mouse: quelle vere di Ctrl+X, Ctrl+C e Ctrl+V. */
class TextEditActions(val cut: () -> Unit, val copy: () -> Unit, val paste: () -> Unit)

/** Chi apre il menu della pagina: lo dà la pagina (vedi `PageEditorScreen`), lo usa il campo di testo. */
class PageTextMenuHost(
    /** [window] è il punto del clic nella finestra. */
    val open: (window: Offset, edit: TextEditActions) -> Unit,
    val isOpen: () -> Boolean,
    val close: () -> Unit
)

val LocalPageTextMenu = staticCompositionLocalOf<PageTextMenuHost?> { null }

/**
 * Sul PC il clic destro sul campo dentro [content] apre il menu della pagina
 * (vedi [LocalPageTextMenu]) al posto di quello di Compose (taglia, copia,
 * incolla); sul telefono, o senza pagina, il campo resta com'è.
 */
@Composable
expect fun PageTextMenuArea(content: @Composable () -> Unit)

/**
 * **Col clic destro il cursore va dove si è cliccato**, come in Windows: a
 * meno che il punto non sia dentro il testo già selezionato, che allora resta
 * selezionato per le voci del menu (grassetto, colore, copia). Il clic non si
 * ferma qui: il menu lo apre [PageTextMenuArea].
 */
fun Modifier.rightClickCaret(
    enabled: Boolean,
    layout: () -> TextLayoutResult?,
    value: () -> TextFieldValue,
    transformation: () -> VisualTransformation,
    focused: () -> Boolean,
    onCaret: (TextFieldValue) -> Unit
): Modifier = if (!enabled) this else composed {
    val latestCaret by rememberUpdatedState(onCaret)
    val latestTransformation by rememberUpdatedState(transformation)
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Press || !event.buttons.isSecondaryPressed) continue
                val l = layout() ?: continue
                val current = value()
                val shown = l.getOffsetForPosition(event.changes.first().position)
                val raw = latestTransformation().filter(current.annotatedString).offsetMapping
                    .transformedToOriginal(shown).coerceIn(0, current.text.length)
                val selection = current.selection
                if (focused() && !selection.collapsed && raw in selection.min..selection.max) continue
                latestCaret(current.copy(selection = TextRange(raw)))
            }
        }
    }
}
