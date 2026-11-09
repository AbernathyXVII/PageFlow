package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.ui.i18n.SyncStrings
import com.gabriele.notionlocal.ui.theme.NotionGray400

/** Il colore della cornice di un blocco sincronizzato, come su Notion: si vede sul chiaro e sullo scuro. */
private val SyncedAccent = Color(0xFFE0703C)

/** Quanto la cornice sta fuori dalle righe, a sinistra e a destra. */
private val SYNCED_FRAME_OUTSET = 8.dp

/**
 * **Un blocco sincronizzato nell'editor** (dal 03/11/2026): una cornice
 * arancione con, in cima, "Synced block" e in quante pagine sta, e dentro le
 * sue righe ([content]), che si scrivono come quelle di un toggle. Toccando
 * la scritta si apre il menu del blocco. La cornice si disegna dietro, così
 * le righe restano allineate al resto della pagina.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SyncedBlockFrame(
    block: BlockEntity,
    start: Dp,
    end: Dp,
    isEmpty: Boolean,
    locked: Boolean,
    /** Cambia quando le copie possono essere cambiate: si ricontano. */
    refreshKey: Any,
    pageCount: suspend (String) -> Int,
    onOpenMenu: (() -> Unit)?,
    onAddFirstLine: () -> Unit,
    content: @Composable () -> Unit
) {
    val syncId = block.attrs.syncId
    val pages by produceState(initialValue = 0, syncId, refreshKey) {
        value = syncId?.let { runCatching { pageCount(it) }.getOrNull() } ?: 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("synced:" + block.id)
            .drawBehind {
                val left = (start - SYNCED_FRAME_OUTSET).toPx().coerceAtLeast(0f)
                val right = size.width - (end - SYNCED_FRAME_OUTSET).toPx().coerceAtLeast(0f)
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = SyncedAccent.copy(alpha = 0.75f),
                    topLeft = Offset(left + stroke / 2, stroke / 2),
                    size = Size(right - left - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(width = stroke)
                )
            }
            .padding(top = 4.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = start, end = end)
                .then(
                    if (onOpenMenu != null) {
                        Modifier.onRightClick(onOpenMenu).combinedClickable(onClick = onOpenMenu, onLongClick = onOpenMenu)
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 2.dp)
        ) {
            Icon(
                Icons.Filled.Sync,
                contentDescription = null,
                tint = SyncedAccent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = SyncStrings.header(pages),
                style = MaterialTheme.typography.labelMedium,
                color = SyncedAccent
            )
        }
        content()
        if (isEmpty && !locked) {
            Text(
                text = SyncStrings.empty,
                style = MaterialTheme.typography.bodyLarge,
                color = NotionGray400,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddFirstLine)
                    .padding(start = start, end = end, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}
