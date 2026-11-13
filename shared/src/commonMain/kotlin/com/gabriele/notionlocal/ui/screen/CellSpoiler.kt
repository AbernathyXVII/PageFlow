package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageParagraph
import com.gabriele.notionlocal.ui.theme.SpoilerCover

/*
 * **"Cell spoiler"** (dal 13/11/2026, chiesto dall'utente su telefono e PC):
 * acceso in "Database settings", un tocco su una cella della tabella la copre
 * col velo dello spoiler del testo (`SpoilerCover`), qualunque cosa ci sia
 * dentro (testo, numeri, date, tag colorati, caselle…), e un altro tocco la
 * scopre. Le celle coperte si ricordano (`PageParagraph.coveredCells`, sulla
 * pagina del database); spento, la tabella torna come sempre: si vede tutto e
 * le celle si toccano per scriverci, e riaccendendolo i veli tornano.
 */

/** Le celle coperte, come "idRiga/colonna" (Name = `NAME_COLUMN_KEY`), e chi ne copre o scopre una. */
class CellSpoilers(val covered: Set<String>, val onToggle: (rowId: String, columnKey: String) -> Unit)

/** La chiave di una cella fra quelle coperte. */
fun coverKey(rowId: String, columnKey: String): String = "$rowId/$columnKey"

/** Dato dalla tabella quando "Cell spoiler" è acceso; null = spento. */
val LocalCellSpoilers = compositionLocalOf<CellSpoilers?> { null }

/** "Cell spoiler" di un database dal suo `paragraphJson`: null se è spento. */
@Composable
fun rememberCellSpoilers(paragraphJson: String?, onToggle: (rowId: String, columnKey: String) -> Unit): CellSpoilers? {
    val toggle by rememberUpdatedState(onToggle)
    return remember(paragraphJson) {
        PageParagraph.parse(paragraphJson).takeIf { it.cellSpoiler == true }
            ?.let { p -> CellSpoilers(p.coveredCells.orEmpty().toSet()) { row, column -> toggle(row, column) } }
    }
}

/**
 * La cella [columnKey] della riga [rowId] col "Cell spoiler" acceso: coperta
 * se è fra quelle coperte, e un tocco la copre o la scopre. Il tocco si prende
 * prima della cella (passaggio `Initial`), così non la apre e non ci scrive;
 * trascinando si scorre come sempre, e il tasto destro del mouse non conta.
 */
@Composable
fun Modifier.cellSpoiler(rowId: String, columnKey: String): Modifier {
    val spoilers = LocalCellSpoilers.current ?: return this
    val covered = coverKey(rowId, columnKey) in spoilers.covered
    val toggle by rememberUpdatedState { spoilers.onToggle(rowId, columnKey) }
    val cover = SpoilerCover
    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                down.consume()
                var moved = false
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                    if (!change.pressed) {
                        if (!moved) {
                            change.consume()
                            toggle()
                        }
                        break
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (covered) {
                val inset = 5.dp.toPx()
                drawRoundRect(
                    color = cover,
                    topLeft = Offset(inset, inset),
                    size = Size((size.width - 2 * inset).coerceAtLeast(0f), (size.height - 2 * inset).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
}
