package com.gabriele.notionlocal.ui.screen

import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import kotlin.math.abs
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// **"Back" quando si è scesi molto** (dal 27/10/2026, chiesto dall'utente):
// in una pagina qualsiasi, scendendo per più di un paio di schermate compare
// in basso al centro lo stesso "Back" dell'indice (`LinkBackButton`), con la
// freccia in su; toccandolo si torna in cima. "Ovviamente l'app deve capire
// che, se vado manualmente verso l'alto, l'icona non deve apparire": conta
// l'ultimo verso in cui il dito ha fatto scorrere la pagina, e salendo se ne
// va. Mentre si scrive (tastiera aperta) non c'è: coprirebbe il testo.
// ---------------------------------------------------------------------------

const val BACK_TO_TOP_TAG = "backToTop"

/** Da quante schermate sotto la cima compare. */
const val BACK_TO_TOP_SCREENS = 1.5f

/**
 * **E solo oltre i due terzi della pagina** (dal 13/11/2026, chiesto
 * dall'utente): il fondo di quello che si vede deve aver passato questa parte
 * della pagina, che in una pagina molto lunga è molto più giù.
 */
const val BACK_TO_TOP_SHARE = 2f / 3f

/** Fermi da tanto (ms), il pulsante se ne va (dal 13/11/2026, chiesto dall'utente: "dopo due secondi"). */
const val BACK_TO_TOP_IDLE_MS = 2_000L

/** Il dito deve muoversi almeno di tanto (px) per cambiare verso: un tremolio non conta. */
private const val DIRECTION_SLOP_PX = 1f

/**
 * Il verso in cui il dito sta facendo scorrere la pagina, e quanto si è
 * scesi. Lo scorrimento arriva da `connection`, messa sulla lista: lì passa
 * solo quello del dito (trascinamento e lancio), non quello che fa l'app da
 * sé — il cursore portato in vista, il salto di un collegamento.
 */
@Stable
class BackToTop(private val list: LazyListState) {
    private val sizes = ItemSizes()

    /** L'ultimo verso del dito: true = si scende nella pagina. */
    var goingDown by mutableStateOf(false)

    /** Quante volte la pagina è scorsa col dito, la rotella o il trackpad: ogni volta il conto dei due secondi riparte. */
    var moves by mutableIntStateOf(0)

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Il dito che sale porta giù la pagina (y negativa), e viceversa.
            if (available.y < -DIRECTION_SLOP_PX) goingDown = true
            else if (available.y > DIRECTION_SLOP_PX) goingDown = false
            if (abs(available.y) > DIRECTION_SLOP_PX) moves++
            return Offset.Zero
        }
    }

    /**
     * Lontano dalla cima: più di [BACK_TO_TOP_SCREENS] schermate, e col fondo di
     * quello che si vede oltre [BACK_TO_TOP_SHARE] della pagina, con la stima
     * della barretta (`ItemSizes`).
     */
    fun farFromTop(): Boolean {
        val info = list.layoutInfo
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        if (viewport <= 0f || !list.canScrollBackward) return false
        sizes.record(info)
        val (scrolled, total) = sizes.extent(info.totalItemsCount, list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        return scrolled > viewport * BACK_TO_TOP_SCREENS && scrolled + viewport >= total * BACK_TO_TOP_SHARE
    }
}

@Composable
fun rememberBackToTop(list: LazyListState): BackToTop = remember(list) { BackToTop(list) }

/**
 * Il pulsante, quando serve: lontani dalla cima, l'ultimo movimento del dito
 * verso il basso, la tastiera chiusa e [allowed] (niente selezione della
 * pagina, niente "Back" di un collegamento, niente disegno).
 */
@Composable
fun BackToTopButton(state: BackToTop, list: LazyListState, allowed: Boolean, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    // Solo quando cambia sì/no: letta così la tastiera non ridisegna niente mentre sale.
    val keyboardOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    val far by remember(state) { derivedStateOf { state.farFromTop() } }
    val scope = rememberCoroutineScope()
    // Fermi da due secondi, se ne va; riscorrendo torna (dal 13/11/2026).
    // Il conto si legge qui dentro, non nel disegno: a ogni passo il pulsante non si rifà.
    var resting by remember(state) { mutableStateOf(false) }
    LaunchedEffect(state) {
        snapshotFlow { state.moves }.collectLatest {
            resting = false
            delay(BACK_TO_TOP_IDLE_MS)
            resting = true
        }
    }
    AnimatedVisibility(
        visible = allowed && far && state.goingDown && !keyboardOpen && !resting,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        BackPill(
            upward = true,
            tag = BACK_TO_TOP_TAG,
            onClick = {
                state.goingDown = false
                scope.launch { list.animateScrollToItem(0) }
            }
        )
    }
}
