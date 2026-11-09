package com.gabriele.notionlocal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.viewmodel.ViewModelFactory

// shortcut: sul PC Impostazioni e Backup sono pagine vuote, finché le loro schermate non passano nel codice comune.
@Composable
private fun NotYetOnPc(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize().background(DarkBackground).padding(8.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Top)) {
            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = NotionWhite, modifier = Modifier.align(Alignment.Top).padding(top = 10.dp))
    }
}

@Composable
actual fun SettingsRoute(factory: ViewModelFactory, onBack: () -> Unit, onOpenPatchNotes: () -> Unit, onOpenStartup: () -> Unit) =
    NotYetOnPc(Strings.settings, onBack)

@Composable
actual fun BackupRoute(onBack: () -> Unit) = NotYetOnPc(Strings.backup, onBack)
