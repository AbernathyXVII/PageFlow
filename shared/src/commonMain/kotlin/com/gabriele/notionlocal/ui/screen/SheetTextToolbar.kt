package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlin.math.roundToInt
import com.gabriele.notionlocal.platform.EditCommand
import com.gabriele.notionlocal.platform.editCommandLabel
import com.gabriele.notionlocal.platform.isPc

/**
 * **Le finestrelle che salgono dal basso, con la barrettina Copy / Cut /
 * Paste / Select all.**
 *
 * Il `ModalBottomSheet` di Material (versione 1.2) si apre in una
 * finestra tutta sua, aggiunta a mano sopra quella dell'app. Una finestra
 * così non sa mostrare la barrettina del telefono: il campo di testo la
 * chiede, e Android risponde che lì non si può. Tenendo premuto un testo
 * in una di queste finestrelle — il riquadro Hex del selettore di colore,
 * il collegamento di un'immagine, la ricerca dei fusi orari — la parola
 * si selezionava ma non compariva niente. Trovato dall'utente il
 * 25/09/2026 sul colore delle barre del widget Life progress bar.
 *
 * Qui dentro la barrettina la disegna l'app (`PopupTextToolbar`): stesse
 * voci e stessi nomi di quella del telefono (sono le scritte di Android,
 * nella lingua del telefono), in una fascia arrotondata sopra il testo
 * selezionato. Fuori da queste finestrelle resta quella del telefono.
 *
 * Si usa **al posto di `ModalBottomSheet`**, con gli stessi parametri.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    content: @Composable ColumnScope.() -> Unit
) {
    // Sul PC un riquadro accanto al clic (vedi `PcPanel.desktop.kt`).
    if (isPc) return PcPanel(onDismissRequest, modifier, containerColor, content)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor
    ) {
        val column = this
        ProvidePopupTextToolbar { column.content() }
    }
}

/** Mette `PopupTextToolbar` al posto della barrettina del telefono per tutto quello che c'è dentro. */
@Composable
fun ProvidePopupTextToolbar(content: @Composable () -> Unit) {
    val toolbar = remember { PopupTextToolbar() }
    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        content()
    }
    // La barrettina si legge **solo qui dentro**: se la leggesse questa
    // funzione, a ogni apertura ridisegnerebbe anche il contenuto, campo
    // di testo compreso (vedi `PopupTextToolbar.status`).
    TextToolbarHost(toolbar)
}

/** Cosa ha chiesto il campo di testo: dove sta la selezione e quali voci mostrare. */
private class ToolbarRequest(
    val rect: Rect,
    val onCopy: (() -> Unit)?,
    val onPaste: (() -> Unit)?,
    val onCut: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?
)

/**
 * La barrettina come la vede il campo di testo: gli basta sapere se è
 * aperta e poterla aprire e chiudere. `request` è osservabile, così il
 * disegno (`TextToolbarHost`) segue da solo.
 *
 * **`status` invece no**, e non è un dettaglio. Il campo di testo lo
 * legge mentre disegna le maniglie della selezione: se fosse osservabile,
 * ogni apertura della barrettina farebbe ridisegnare le maniglie, che
 * nel ridisegnarsi la chiudono e la riaprono — un botta e risposta che
 * finiva sempre con la barrettina chiusa. Visto nel registro del
 * telefono il 25/09/2026: aperta e chiusa quattro volte in 30 ms, poi
 * più niente. La barrettina del telefono tiene lo stato in un campo
 * semplice, e così fa questa.
 */
@Stable
private class PopupTextToolbar : TextToolbar {
    var request by mutableStateOf<ToolbarRequest?>(null)
        private set

    private var shown = false

    override val status: TextToolbarStatus
        get() = if (shown) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        shown = true
        request = ToolbarRequest(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
    }

    override fun hide() {
        shown = false
        request = null
    }
}

/**
 * La fascia con le voci, sopra il testo selezionato (sotto, se sopra non
 * c'è posto).
 *
 * **Non prende il fuoco** (`focusable = false`): il campo lo tiene, e con
 * lui la tastiera e la selezione. Un tocco fuori passa al campo, che
 * chiude la barrettina da sé quando la selezione cambia.
 */
@Composable
private fun TextToolbarHost(toolbar: PopupTextToolbar) {
    // Il campo dice dov'è la selezione rispetto alla radice della
    // finestrella, la finestra del menu vuole posizioni rispetto alla
    // finestra: lo scarto fra le due si ricava dal punto in cui sta questo
    // riquadro vuoto, che si sa sia rispetto all'una sia rispetto all'altra.
    // C'è sempre, anche a barrettina chiusa: così quando serve il punto è
    // già noto e la barrettina compare subito.
    var anchorInRoot by remember { mutableStateOf<Offset?>(null) }
    val gap = with(LocalDensity.current) { TOOLBAR_GAP.toPx() }
    val onDone = { toolbar.hide() }
    Box(modifier = Modifier.onGloballyPositioned { anchorInRoot = it.positionInRoot() }) {
        val request = toolbar.request ?: return@Box
        val anchor = anchorInRoot ?: return@Box
        Popup(
            popupPositionProvider = remember(request, anchor, gap) {
                AboveSelection(request.rect, anchor, gap)
            },
            onDismissRequest = onDone,
            properties = PopupProperties(focusable = false)
        ) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = NotionGray900,
                shadowElevation = 6.dp
            ) {
                Row(modifier = Modifier.padding(horizontal = 6.dp)) {
                    // Nell'ordine della barrettina del telefono.
                    request.onCut?.let { ToolbarItem(editCommandLabel(EditCommand.CUT), it, onDone) }
                    request.onCopy?.let { ToolbarItem(editCommandLabel(EditCommand.COPY), it, onDone) }
                    request.onPaste?.let { ToolbarItem(editCommandLabel(EditCommand.PASTE), it, onDone) }
                    request.onSelectAll?.let { ToolbarItem(editCommandLabel(EditCommand.SELECT_ALL), it, onDone) }
                }
            }
        }
    }
}

@Composable
private fun ToolbarItem(label: String, action: () -> Unit, onDone: () -> Unit) {
    Text(
        text = label,
        color = NotionWhite,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clickable {
                // Prima si chiude, poi si agisce: "Select all" riapre
                // subito la barrettina sulla selezione nuova, e chiuderla
                // dopo la farebbe sparire.
                onDone()
                action()
            }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

/** Centrata sopra la selezione, dentro lo schermo; sotto se sopra non ci sta. */
private class AboveSelection(
    private val selectionInRoot: Rect,
    private val anchorInRoot: Offset,
    private val gap: Float
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // Da coordinate della radice a coordinate della finestra.
        val dx = anchorBounds.left - anchorInRoot.x
        val dy = anchorBounds.top - anchorInRoot.y
        val selection = selectionInRoot.translate(dx, dy)
        val x = (selection.center.x - popupContentSize.width / 2f)
            .coerceIn(gap, (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap))
        val above = selection.top - popupContentSize.height - gap
        // Sotto, lo spazio va lasciato anche alle maniglie della selezione.
        val y = if (above >= gap) above else selection.bottom + gap * 4
        return IntOffset(x.roundToInt(), y.roundToInt())
    }
}

private val TOOLBAR_GAP = 8.dp
