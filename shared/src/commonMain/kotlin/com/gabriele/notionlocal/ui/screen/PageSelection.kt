package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.entity.plainText
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.platform.copyRichText
import com.gabriele.notionlocal.platform.systemAnnouncesCopies

// ---------------------------------------------------------------------------
// **Tutta la pagina selezionata, come su Notion** (dal 20/10/2026, chiesto
// dall'utente). Dalla v0.113.1 le pagine lunghe sono divise in pezzi, e "Select
// all" del testo arrivava solo ai bordi del pezzo; e comunque una selezione di
// testo non passa sopra un database o un'immagine. Ora "Select all" (nella
// barra sopra la tastiera e nel menu del testo) vela di blu tutta la pagina,
// e in basso ci sono Copy, Cut e Delete. Un tocco sulla pagina, la ✕ o
// Indietro la tolgono.
// ---------------------------------------------------------------------------

const val PAGE_SELECTION_BAR_TAG = "pageSelectionBar"

/** Il velo blu sopra un pezzo della pagina selezionata. */
fun Modifier.pageVeil(selected: Boolean, color: Color): Modifier =
    if (!selected) this else drawWithContent {
        drawContent()
        drawBlockVeil(color, Offset.Zero, size)
    }

/**
 * **Il velo di un blocco scelto, staccato dai vicini** (dal 25/11/2026, chiesto dall'utente: "come
 * Notion, con un piccolo spazio tra un blocco e l'altro"): ogni blocco ha il suo, con gli angoli
 * tondi, e fra due blocchi scelti di seguito resta una riga sottile scura. Così si vede dove comincia
 * un blocco intero (il gruppo di colonne, un velo solo) e dove una riga dentro una colonna.
 */
fun DrawScope.drawBlockVeil(color: Color, topLeft: Offset, size: Size) {
    val half = BLOCK_VEIL_GAP.toPx() / 2f
    drawRoundRect(
        color = color.copy(alpha = 0.18f),
        topLeft = Offset(topLeft.x, topLeft.y + half),
        size = Size(size.width, (size.height - 2 * half).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(BLOCK_VEIL_CORNER.toPx())
    )
}

/** Lo spazio fra i veli di due blocchi scelti uno sotto l'altro. */
private val BLOCK_VEIL_GAP = 3.dp

/** Gli angoli dei veli dei blocchi scelti. */
private val BLOCK_VEIL_CORNER = 4.dp

/**
 * Mentre la pagina è selezionata, **un tocco la toglie** e non arriva al
 * testo sotto (niente cursore né tastiera). Lo scorrimento passa come sempre.
 */
fun Modifier.tapToEndPageSelection(selected: Boolean, onEnd: () -> Unit): Modifier =
    if (!selected) this else pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val up = waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
            if ((up.position - down.position).getDistance() > viewConfiguration.touchSlop) return@awaitEachGesture
            up.consume()
            onEnd()
        }
    }

/**
 * Il menu del testo (Copy, Paste, Cut, Select all) di sempre, ma "Select
 * all" seleziona tutta la pagina.
 */
class WholePageTextToolbar(val base: TextToolbar, private val selectPage: () -> Unit) : TextToolbar {
    override val status: TextToolbarStatus get() = base.status
    override fun hide() = base.hide()
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = base.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested?.let { { selectPage() } })
}

/**
 * Il menu del testo di sempre, dove "Select all" prende solo quel campo: il
 * titolo della pagina, le celle di un database dentro la pagina.
 */
@Composable
fun PlainTextToolbar(content: @Composable () -> Unit) {
    val current = LocalTextToolbar.current
    CompositionLocalProvider(LocalTextToolbar provides ((current as? WholePageTextToolbar)?.base ?: current), content = content)
}

/**
 * La pagina negli appunti: il testo semplice e, accanto, l'HTML con la
 * formattazione e il tipo delle righe (`linesMarker`), così incollata in notE
 * torna com'era e altrove arriva formattata.
 */
fun copyPageToClipboard(context: PlatformContext, lines: List<PageEditorViewModel.PageCopyLine>) {
    val plain = lines.joinToString("\n") { it.spans.plainText() }
    val joined = lines.flatMapIndexed { i, line -> (if (i > 0) listOf(RichTextSpan(text = "\n")) else emptyList()) + line.spans }
    val html = spansToHtml(joined) + linesMarker(lines.map { it.kind })
    if (!copyRichText(context, plain, html)) return
    // Da Android 13 il "copiato" lo dice il telefono.
    if (!systemAnnouncesCopies) {
        showMessage(context, EditorStrings.pageCopied)
    }
}

/** La barra in basso con la pagina selezionata: Copy, Cut, Delete, e la ✕ per lasciarla. Bloccata, solo Copy. */
@Composable
fun PageSelectionBar(
    editable: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag(PAGE_SELECTION_BAR_TAG)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSheet)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionAction(Icons.Filled.ContentCopy, EditorStrings.copy, onCopy)
        if (editable) {
            SelectionAction(Icons.Filled.ContentCut, EditorStrings.cut, onCut)
            SelectionAction(Icons.Filled.Delete, Strings.delete, onDelete, tint = MaterialTheme.colorScheme.error)
        }
        SelectionAction(Icons.Filled.Close, Strings.cancel, onClose)
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = NotionWhite) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}
