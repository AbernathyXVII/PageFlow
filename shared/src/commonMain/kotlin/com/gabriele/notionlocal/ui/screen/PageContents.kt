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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import com.gabriele.notionlocal.platform.isPc
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import com.gabriele.notionlocal.ui.theme.DarkSurface

/**
 * **L'indice della pagina, a destra** (dal 18/11/2026, chiesto dall'utente, come la "Table of
 * contents" di Notion): nel margine a destra della pagina (anche nei peek), una lineetta per ogni
 * blocco segnato con "Flag (for Table of contents)" ([targets], vedi `tocTargetsOf`; fino alla
 * v0.155.39.1 c'erano tutte le destinazioni dei collegamenti, titoli grandi compresi). Più corta
 * quanto più è rientrata; quella del punto dove si è arrivati leggendo ([current]) è chiara. Sul PC
 * col mouse sopra si apre l'elenco coi nomi; **sul telefono** (dal 18/11/2026) un tocco lo apre in
 * una finestra dal basso. Un clic o un tocco su un nome porta lì ([onJump]).
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
    if (!isPc) {
        PhoneContents(targets, current, onJump, dim, bright, modifier)
        return
    }
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

/** Sul telefono: le lineette, e toccandole l'elenco coi nomi in una finestra dal basso. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneContents(
    targets: List<LinkTarget>,
    current: String?,
    onJump: (String) -> Unit,
    dim: Color,
    bright: Color,
    modifier: Modifier
) {
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .testTag(PAGE_CONTENTS_TAG)
            .clickable { open = true }
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
    if (open) {
        NoteBottomSheet(onDismissRequest = { open = false }, containerColor = DarkSheet) {
            Text(
                text = EditorStrings.tableOfContents,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                targets.forEach { target ->
                    Text(
                        text = target.label.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (target.blockId == current) bright else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                open = false
                                onJump(target.blockId)
                            }
                            .padding(start = (16 + 16 * target.level.coerceIn(0, 3)).dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

/** Le lineette dell'indice sul telefono, per le prove. */
const val PAGE_CONTENTS_TAG = "page-contents"
