package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.gabriele.notionlocal.data.backup.BackupException
import com.gabriele.notionlocal.data.backup.BackupFile
import com.gabriele.notionlocal.data.backup.BackupReason
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.BackupStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import com.gabriele.notionlocal.platform.showMessage
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.PlatformContext

const val BACKUP_SCREEN_TAG = "backupScreen"

/** Da dove viene il backup da ripristinare: uno di quelli sul telefono, o un file scelto. */
private class RestoreChoice(val label: String, val open: () -> InputStream)

/**
 * **La pagina Backup** (dal 28/10/2026, dalla barra laterale): com'è
 * l'ultimo backup e l'ultimo caricamento su Proton Drive, "Carica su Proton
 * Drive", "Fai un backup adesso", il backup di ogni giorno e il promemoria,
 * i backup sul telefono, e il ripristino. [onRestart]: riapre l'app dopo un
 * ripristino preparato (i test lo sostituiscono).
 *
 * Dal 09/11/2026 è la stessa sul PC: cambiano dove stanno i backup, le
 * finestre per scegliere i file e qualche scritta (`BackupPlatformParts`).
 */
@Composable
fun BackupScreen(onBack: () -> Unit, onRestart: (PlatformContext) -> Unit = ::restartAfterRestore) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val changes = Backups.changes
    var backups by remember { mutableStateOf<List<BackupFile>?>(null) }
    LaunchedEffect(changes) { backups = withContext(Dispatchers.IO) { Backups.list() } }
    var busy by remember { mutableStateOf<String?>(null) }
    var restoreChoice by remember { mutableStateOf<RestoreChoice?>(null) }
    var deleting by remember { mutableStateOf<BackupFile?>(null) }
    var choosingReminder by remember { mutableStateOf(false) }

    // Sempre dal filo principale: dopo un lavoro in sottofondo si torna anche altrove.
    fun toast(text: String) {
        scope.launch(Dispatchers.Main) { showMessage(context, text, long = true) }
    }

    val upload = rememberBackupUpload(
        onStart = { busy = BackupStrings.uploading },
        onDone = { ok ->
            busy = null
            toast(if (ok) BackupStrings.uploaded else BackupStrings.uploadFailed)
        }
    )

    fun backupNow(thenUpload: Boolean) {
        scope.launch {
            busy = BackupStrings.backingUp
            val made = runCatching { Backups.backupNow(BackupReason.MANUAL) }.getOrNull()
            busy = null
            when {
                made == null -> toast(BackupStrings.backupFailed)
                thenUpload -> upload(made)
                else -> toast(BackupStrings.backupDone)
            }
        }
    }

    val pickFile = rememberBackupPicker { label, open -> restoreChoice = RestoreChoice(label, open) }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.backup, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(BACKUP_SCREEN_TAG)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            val latest = backups?.firstOrNull()
            // Com'è adesso, e il pulsante che serve di più.
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(16.dp)
            ) {
                StatusLine(BackupStrings.lastBackup, Backups.lastBackupAt.takeIf { it > 0 }?.let(::whenLabel) ?: BackupStrings.noBackupYet)
                Spacer(modifier = Modifier.size(6.dp))
                StatusLine(BackupStrings.lastUpload, Backups.lastUploadAt.takeIf { it > 0 }?.let(::whenLabel) ?: BackupStrings.neverUploaded)
                Spacer(modifier = Modifier.size(14.dp))
                Button(
                    onClick = { if (latest != null) upload(latest) else backupNow(thenUpload = true) },
                    enabled = busy == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(BackupStrings.uploadToProton)
                }
                Spacer(modifier = Modifier.size(6.dp))
                OutlinedButton(onClick = { backupNow(thenUpload = false) }, enabled = busy == null, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(BackupStrings.backupNow)
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(uploadNote, style = MaterialTheme.typography.bodySmall, color = NotionGray400)
            }

            SectionHeader(BackupStrings.automatic)
            SettingsItem(
                BackupStrings.dailyBackup,
                icon = Icons.Filled.Schedule,
                value = dailyBackupNote,
                onClick = { Backups.setAuto(!Backups.autoBackup) },
                trailing = { Switch(checked = Backups.autoBackup, onCheckedChange = { Backups.setAuto(it) }) }
            )
            SettingsItem(
                BackupStrings.reminder,
                icon = Icons.Filled.NotificationsActive,
                value = reminderLabel(Backups.remindDays),
                onClick = { choosingReminder = true }
            )
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(backupPlaceTitle)
            Text(
                backupPlaceNote,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
            )
            when {
                backups == null -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                backups!!.isEmpty() -> Text(
                    noBackupsHere,
                    color = NotionGray400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                else -> backups!!.forEach { file ->
                    BackupRow(
                        file = file,
                        enabled = busy == null,
                        onUpload = { upload(file) },
                        // Nella domanda la data vera: "del 28/10/2026 09:51", non "del Oggi alle 09:51".
                        onRestore = { restoreChoice = RestoreChoice(Formats.dateTime(file.createdAt)) { Backups.open(file) } },
                        onDelete = { deleting = file }
                    )
                }
            }
            HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(BackupStrings.restore)
            SettingsItem(
                BackupStrings.restoreFromFile,
                icon = Icons.Filled.Restore,
                value = BackupStrings.restoreFromFileNote,
                enabled = busy == null,
                onClick = pickFile
            )
        }
    }

    restoreChoice?.let { choice ->
        AlertDialog(
            onDismissRequest = { restoreChoice = null },
            containerColor = DarkSheet,
            title = { Text(BackupStrings.restoreTitle) },
            text = { Text(BackupStrings.restoreText(choice.label)) },
            confirmButton = {
                TextButton(onClick = {
                    restoreChoice = null
                    scope.launch {
                        busy = BackupStrings.restoring
                        try {
                            Backups.prepareRestore(choice.open)
                            onRestart(context)
                        } catch (e: BackupException) {
                            toast(
                                when (e.problem) {
                                    BackupException.Problem.TOO_NEW -> BackupStrings.tooNew
                                    BackupException.Problem.TOO_OLD -> BackupStrings.tooOldForPc
                                    BackupException.Problem.NOT_A_BACKUP -> BackupStrings.notABackup
                                }
                            )
                        } catch (e: Exception) {
                            toast(BackupStrings.restoreFailed)
                        } finally {
                            busy = null
                        }
                    }
                }) { Text(BackupStrings.restore, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { restoreChoice = null }) { Text(Strings.cancel) } }
        )
    }

    deleting?.let { file ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = DarkSheet,
            title = { Text(BackupStrings.deleteTitle) },
            text = { Text(whenLabel(file.createdAt)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { withContext(Dispatchers.IO) { Backups.delete(file) } }
                }) { Text(Strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(Strings.cancel) } }
        )
    }

    if (choosingReminder) {
        AlertDialog(
            onDismissRequest = { choosingReminder = false },
            containerColor = DarkSheet,
            title = { Text(BackupStrings.reminder) },
            text = {
                Column {
                    Backups.REMIND_CHOICES.forEach { days ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Backups.setRemind(days)
                                    choosingReminder = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = Backups.remindDays == days, onClick = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(reminderLabel(days))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingReminder = false }) { Text(Strings.cancel) } }
        )
    }

    busy?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            containerColor = DarkSheet,
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(message)
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NotionGray400, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(12.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = NotionWhite, fontWeight = FontWeight.Medium)
    }
}

/** Un backup sul telefono: quando, quanto pesa, e il menu con caricare, ripristinare, eliminare. */
@Composable
private fun BackupRow(file: BackupFile, enabled: Boolean, onUpload: () -> Unit, onRestore: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { menu = true }
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.FolderZip, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(whenLabel(file.createdAt), color = NotionWhite, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = formatFileSize(file.size) +
                if (file.name.contains("-before-restore")) " · " + BackupStrings.beforeRestore else ""
            Text(detail, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
        }
        Box {
            IconButton(onClick = { menu = true }, enabled = enabled) {
                Icon(Icons.Filled.MoreVert, contentDescription = com.gabriele.notionlocal.ui.i18n.ButtonStrings.options, tint = NotionGray400)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(BackupStrings.uploadToProton) }, onClick = { menu = false; onUpload() })
                DropdownMenuItem(text = { Text(BackupStrings.restore) }, onClick = { menu = false; onRestore() })
                DropdownMenuItem(
                    text = { Text(Strings.delete, color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

/** "Oggi alle 14:32", "Ieri alle 9:05", o la data con l'ora. */
private fun whenLabel(millis: Long): String {
    val zone = AppSettings.zoneId
    val day = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (day) {
        today -> BackupStrings.todayAt(Formats.time(millis))
        today.minusDays(1) -> BackupStrings.yesterdayAt(Formats.time(millis))
        else -> Formats.dateTime(millis)
    }
}

private fun reminderLabel(days: Int): String = if (days <= 0) BackupStrings.never else BackupStrings.afterDays(days)
