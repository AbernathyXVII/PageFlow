package com.gabriele.notionlocal.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.gabriele.notionlocal.ui.theme.PcZoom
import com.gabriele.notionlocal.ui.theme.zoomKeyOf
import com.gabriele.notionlocal.viewmodel.FormatType

/**
 * **Le scorciatoie del testo selezionato in una pagina** (dal 16/11/2026,
 * chiesto dall'utente): ogni pagina aperta nella finestra dice qui se ha del
 * testo selezionato e, se sì, lo cambia. Senza testo selezionato rispondono
 * falso, e il tasto fa quello di sempre (Ctrl + e Ctrl − lo zoom).
 */
interface SelectionShortcuts {
    /** Lo stile [type] sul testo selezionato, come il pulsante della barra Aa; falso senza testo selezionato. */
    fun format(type: FormatType): Boolean

    /** Il corpo del testo selezionato di uno scatto più grande ([bigger]) o più piccolo; falso senza testo selezionato. */
    fun resize(bigger: Boolean): Boolean
}

/** Ctrl+B, Ctrl+I, Ctrl+U, Ctrl+S e Ctrl+Maiusc+S: lo stile che chiedono, null per gli altri tasti. */
fun selectionFormatOf(event: KeyEvent): FormatType? {
    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed || event.isAltPressed) return null
    return when (event.key) {
        Key.B -> FormatType.BOLD.takeUnless { event.isShiftPressed }
        Key.I -> FormatType.ITALIC.takeUnless { event.isShiftPressed }
        Key.U -> FormatType.UNDERLINE.takeUnless { event.isShiftPressed }
        Key.S -> if (event.isShiftPressed) FormatType.SPOILER else FormatType.STRIKETHROUGH
        else -> null
    }
}

/**
 * **Le scorciatoie da tastiera del PC** (dal 09/11/2026): la finestra le
 * riceve tutte (anche senza un campo col cursore) e chiede qui; a rispondere è
 * la navigazione aperta (`AppNavHost`), finché c'è.
 */
open class Shortcuts {
    /** Ctrl+F: la ricerca. */
    var openSearch: (() -> Unit)? = null

    /** Ctrl+N: una pagina nuova (dal 10/11/2026). */
    var newPage: (() -> Unit)? = null

    /** Ctrl+T: una scheda nuova sul menu principale, come il + (dal 12/11/2026, vedi `PcTabs`). */
    var newTab: (() -> Unit)? = null

    /** Ctrl+W: chiude la scheda aperta, come la sua × (dal 12/11/2026). */
    var closeTab: (() -> Unit)? = null

    /** Le pagine aperte (anche più di una, con un'anteprima di lato): l'ultima arrivata per prima. */
    val selection = mutableListOf<SelectionShortcuts>()

    /** Vero se una pagina aveva del testo selezionato e ci ha messo lo stile. */
    fun formatSelection(type: FormatType): Boolean = selection.asReversed().any { it.format(type) }

    /** Vero se una pagina aveva del testo selezionato e ne ha cambiato il corpo. */
    fun resizeSelection(bigger: Boolean): Boolean = selection.asReversed().any { it.resize(bigger) }

    /** Un tasto arrivato alla finestra del PC, prima di tutti: vero se era una scorciatoia ed è stata fatta. */
    fun windowKey(event: KeyEvent): Boolean {
        // Lo zoom come nei browser: Ctrl + e Ctrl − (vedi `PcZoom`). Con del testo
        // selezionato in una pagina, invece, il suo corpo (dal 16/11/2026).
        zoomKeyOf(event)?.let { bigger ->
            if (!resizeSelection(bigger)) PcZoom.step(bigger)
            return true
        }
        // Grassetto, corsivo, sottolineato, barrato e spoiler sul testo selezionato (vedi `SelectionShortcuts`).
        selectionFormatOf(event)?.let { type ->
            if (formatSelection(type)) return true
        }
        val shortcut = when {
            event.type != KeyEventType.KeyDown || !event.isCtrlPressed -> null
            event.key == Key.F -> openSearch
            event.key == Key.N -> newPage
            // Le schede, come nei browser (vedi `PcTabs`).
            event.key == Key.T -> newTab
            event.key == Key.W -> closeTab
            else -> null
        }
        shortcut?.invoke()
        return shortcut != null
    }
}

/** Quelle di sempre: sul telefono, nei test e dove una finestra non ne dà di sue. */
object AppShortcuts : Shortcuts()

/**
 * **Le scorciatoie della finestra** (dal 13/11/2026): con le schede staccate
 * in un'altra finestra (vedi `PcTabs`) ogni finestra ha le sue, e Ctrl+T apre
 * la scheda in quella dove si è.
 */
val LocalShortcuts = staticCompositionLocalOf<Shortcuts> { AppShortcuts }
