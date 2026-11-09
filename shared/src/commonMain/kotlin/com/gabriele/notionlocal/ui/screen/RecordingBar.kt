package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.record.AudioRecorder
import com.gabriele.notionlocal.data.record.Recording
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Color
import com.gabriele.notionlocal.viewmodel.PageEditorViewModel

/** Il rosso della registrazione: il pallino, la barra del volume, "Stop". */
internal val RecordRed = Color(0xFFE5484D)

/**
 * **Il blocco audio mentre registra** (dal 03/11/2026): il pallino rosso, il
 * tempo, quanto è forte il suono, e per finire "Stop" (il file va nel
 * blocco) o la crocetta (la registrazione se ne va). Vedi `AudioRecorder`.
 */
@Composable
fun RecordingBar(
    recording: Recording,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var level by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(recording) {
        while (true) {
            now = System.currentTimeMillis()
            level = sqrt(AudioRecorder.level())
            delay(LEVEL_EVERY_MS)
        }
    }
    val elapsed = (now - recording.startedAt).coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recording:" + recording.blockId)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Il pallino si accende e si spegne ogni mezzo secondo.
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if ((elapsed / 500) % 2 == 0L) RecordRed else RecordRed.copy(alpha = 0.35f))
        )
        Spacer(Modifier.width(8.dp))
        Text(elapsedText(elapsed), style = MaterialTheme.typography.titleSmall, color = NotionWhite)
        Spacer(Modifier.width(8.dp))
        Text(CaptureStrings.recording, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NotionGray400.copy(alpha = 0.25f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0.02f, 1f))
                    .background(RecordRed)
            )
        }
        IconButton(onClick = onDiscard) {
            Icon(Icons.Filled.Close, contentDescription = CaptureStrings.discard, tint = NotionGray400)
        }
        IconButton(onClick = onStop) {
            Icon(Icons.Filled.Stop, contentDescription = CaptureStrings.stop, tint = RecordRed)
        }
    }
}

/** "0:42", "12:05", "1:02:33". */
fun elapsedText(ms: Long): String {
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

internal const val LEVEL_EVERY_MS = 120L

/**
 * Comincia a registrare nel blocco audio [block] (sul telefono chiedendo
 * prima il microfono, se serve). Una registrazione già in corso altrove lo
 * dice. Sul PC, per ora, dice che non si può.
 */
@Composable
expect fun rememberRecordStarter(block: BlockEntity): () -> Unit

/**
 * **"Scan document"**: lo scanner dei documenti, e le pagine scansionate al
 * posto della riga col suo id (o sotto). Sul telefono quello di Google; sul
 * PC, per ora, non c'è.
 */
@Composable
expect fun rememberDocumentScanner(viewModel: PageEditorViewModel): (String) -> Unit
