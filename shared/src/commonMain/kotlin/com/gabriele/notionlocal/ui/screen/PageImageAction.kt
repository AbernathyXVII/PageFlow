package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.shape.RoundedCornerShape
import com.gabriele.notionlocal.ui.theme.label
import com.gabriele.notionlocal.ui.theme.DarkSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.theme.NotionWhite

/** Un pulsante della finestra dell'immagine (e di quella che sposta la copertina). */
@Composable
internal fun PageImageAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) NotionWhite else NotionWhite.copy(alpha = 0.35f),
        modifier = Modifier
            .background(DarkSurface, RoundedCornerShape(percent = 50))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}
