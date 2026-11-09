package com.gabriele.notionlocal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.backup.BackupException
import com.gabriele.notionlocal.data.backup.DesktopBackups
import com.gabriele.notionlocal.data.backup.OlderBackupException
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.ui.screen.SettingsScreen
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.system.exitProcess

/** La riga in cima: la freccia per tornare e il titolo. */
@Composable
private fun TopRow(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = NotionWhite)
    }
}

/** Le impostazioni del telefono, le stesse (senza la sezione delle notifiche: vedi `NotificationSettingsSection`). */
@Composable
actual fun SettingsRoute(factory: ViewModelFactory, onBack: () -> Unit, onOpenPatchNotes: () -> Unit, onOpenStartup: () -> Unit) =
    SettingsScreen(factory = factory, onBack = onBack, onOpenPatchNotes = onOpenPatchNotes, onOpenStartup = onOpenStartup)

/**
 * **Backup sul PC** (dal 09/11/2026): per ora solo "Ripristina da un file…",
 * per aprire sul PC un backup fatto dal telefono (vedi `DesktopBackups`).
 * Confermato, il backup si prepara e PageFlow si chiude: alla riapertura c'è.
 */
@Composable
actual fun BackupRoute(onBack: () -> Unit) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var chosen by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        TopRow(Strings.backup, onBack)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(
                enabled = !busy,
                onClick = {
                    val dialog = FileDialog(null as Frame?, BackupStrings.restoreFromFile, FileDialog.LOAD).apply {
                        setFilenameFilter { _, name -> name.endsWith(".zip", ignoreCase = true) }
                        isVisible = true
                    }
                    dialog.file?.let { chosen = File(dialog.directory, it) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) BackupStrings.restoring else BackupStrings.restoreFromFile) }
            Spacer(Modifier.height(8.dp))
            Text(BackupStrings.restoreFromFileNote, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
        }
    }
    chosen?.let { file ->
        AlertDialog(
            onDismissRequest = { chosen = null },
            title = { Text(BackupStrings.restoreTitle) },
            text = { Text(BackupStrings.restoreText(file.name) + "\n\n" + BackupStrings.restoreClosesOnPc) },
            confirmButton = {
                TextButton(onClick = {
                    chosen = null
                    busy = true
                    scope.launch {
                        val problem = withContext(Dispatchers.IO) {
                            try {
                                DesktopBackups.prepareRestore(context as DesktopContext, file)
                                null
                            } catch (e: BackupException) {
                                if (e.problem == BackupException.Problem.TOO_NEW) BackupStrings.tooNew else BackupStrings.notABackup
                            } catch (e: OlderBackupException) {
                                BackupStrings.tooOldForPc
                            } catch (e: Exception) {
                                BackupStrings.restoreFailed
                            }
                        }
                        if (problem == null) {
                            // Il ripristino si fa alla riapertura (`Main.kt`), col database chiuso.
                            AppDatabase.closeInstance()
                            exitProcess(0)
                        }
                        busy = false
                        showMessage(context, problem)
                    }
                }) { Text(BackupStrings.restore) }
            },
            dismissButton = { TextButton(onClick = { chosen = null }) { Text(Strings.cancel) } }
        )
    }
}
