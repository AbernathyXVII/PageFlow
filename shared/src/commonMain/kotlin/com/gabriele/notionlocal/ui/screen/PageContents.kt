package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.LinkTarget
import com.gabriele.notionlocal.ui.theme.DarkSurface

/**
 * **L'indice della pagina, a destra** (dal 18/11/2026, chiesto dall'utente, come la "Table of
 * contents" di Notion): sul PC, nel margine a destra della pagina (anche nei peek), una lineetta per
 * ogni punto a cui si può collegare del testo con "Link → In this page" ([targets], vedi
 * `linkTargetsOf`): le righe col pin e quelle scritte tutte in grande. Più corta quanto più è
 * rientrata; quella del punto dove si è arrivati leggendo ([current]) è chiara. Col mouse sopra si
 * apre l'elenco coi nomi; un clic porta lì ([onJump]).
 */
@Composable
fun PageContentsRail(
    targets: List<LinkTarget>,
    current: String?,
    onJump: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val bright = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Enter -> open = true
                        PointerEventType.Exit -> open = false
                        else -> {}
                    }
                }
            }
        },
        contentAlignment = Alignment.TopEnd
    ) {
        if (!open) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                targets.forEach { target ->
                    Box(
                        Modifier
                            .width((18 - 4 * target.level.coerceIn(0, 2)).dp)
                            .height(2.dp)
                            .background(if (target.blockId == current) bright else dim, RoundedCornerShape(1.dp))
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface,
                shadowElevation = 8.dp,
                modifier = Modifier.widthIn(min = 160.dp, max = 260.dp)
            ) {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                    targets.forEach { target ->
                        Text(
                            text = target.label.ifBlank { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (target.blockId == current) bright else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    open = false
                                    onJump(target.blockId)
                                }
                                .padding(start = (12 + 12 * target.level.coerceIn(0, 3)).dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
