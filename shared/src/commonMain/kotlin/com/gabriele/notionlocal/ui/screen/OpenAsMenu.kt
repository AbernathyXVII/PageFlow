package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.settings.OpenMode
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.NotionGray400

/** Il segno di ogni modo di aprire una pagina, come nel menu di Notion. */
fun openModeIcon(mode: OpenMode): ImageVector = when (mode) {
    OpenMode.SIDE_PEEK -> Icons.Filled.ViewSidebar
    OpenMode.CENTER_PEEK -> Icons.Filled.CropSquare
    OpenMode.FULL_PAGE -> Icons.Filled.Fullscreen
    OpenMode.NEW_TAB -> Icons.Filled.Tab
}

/**
 * **"Open as…"** (dal 12/11/2026, solo sul PC, chiesto dall'utente come in
 * Notion): la voce dei menu col destro su una pagina (in un blocco o una riga
 * di un database), con le quattro scelte in una tendina. [anchor] è la voce,
 * nello stile del suo menu; riceve chi apre la tendina.
 */
@Composable
fun OpenAsMenu(onPick: (OpenMode) -> Unit, anchor: @Composable (open: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        anchor { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OpenModeItems(current = null) { mode ->
                expanded = false
                onPick(mode)
            }
        }
    }
}

/** Le quattro voci, con la spunta su [current]: nel menu "Open as…" e nel pulsante in alto del peek. */
@Composable
fun OpenModeItems(current: OpenMode?, onPick: (OpenMode) -> Unit) {
    OpenMode.entries.forEach { mode ->
        DropdownMenuItem(
            text = { Text(Strings.openModeName(mode)) },
            leadingIcon = { Icon(openModeIcon(mode), contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (mode == current) {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else {
                null
            },
            onClick = { onPick(mode) }
        )
    }
}
