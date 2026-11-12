package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.sync.SyncDiscovery
import com.gabriele.notionlocal.data.sync.SyncPrompts
import com.gabriele.notionlocal.data.sync.SyncResult
import com.gabriele.notionlocal.data.sync.SyncServer
import com.gabriele.notionlocal.data.sync.SyncSide
import com.gabriele.notionlocal.data.sync.SyncState
import com.gabriele.notionlocal.data.sync.SyncStep
import com.gabriele.notionlocal.data.sync.WifiSync
import com.gabriele.notionlocal.data.sync.syncSide
import com.gabriele.notionlocal.data.sync.syncWaitsForPhone
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WifiSyncStrings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **"Connections"** (dal 10/11/2026), la voce della barra laterale che c'era
 * già, grigia: la sincronizzazione Wi‑Fi. Sul telefono il pulsante che cerca
 * il PC e sincronizza, con a che punto è e com'è andata; sul PC l'interruttore
 * che lo fa aspettare il telefono, e i telefoni abbinati.
 */
@Composable
fun ConnectionsScreen(onBack: () -> Unit) {
    val context = LocalPlatformContext.current
    val side = remember(context) { syncSide(context) }
    val status by WifiSync.status.collectAsState()
    val running by SyncServer.running.collectAsState()
    val last by SyncServer.last.collectAsState()
    var state by remember { mutableStateOf<SyncState?>(null) }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(status, last, running, refresh) { state = withContext(Dispatchers.IO) { side.store.load() } }

    Scaffold(
        topBar = { WorkspaceTopBar(title = Strings.connections, onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            val current = state ?: return@Column
            if (syncWaitsForPhone) {
                PcPart(side, current, running, last, onChanged = { refresh++ })
            } else {
                PhonePart(side, current, status, onChanged = { refresh++ })
            }
            Text(
                WifiSyncStrings.whatSyncs,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun PhonePart(side: SyncSide, state: SyncState, status: WifiSync.Status, onChanged: () -> Unit) {
    val pc = state.peers.maxByOrNull { it.lastSyncAt }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(WifiSyncStrings.lastSync, style = MaterialTheme.typography.bodyMedium, color = NotionGray400, modifier = Modifier.weight(1f))
            Text(
                pc?.lastSyncAt?.takeIf { it > 0 }?.let(::whenLabel) ?: WifiSyncStrings.never,
                style = MaterialTheme.typography.bodyMedium,
                color = NotionWhite,
                fontWeight = FontWeight.Medium
            )
        }
        if (pc != null) {
            Spacer(modifier = Modifier.size(4.dp))
            Text(WifiSyncStrings.pairedWith(pc.name), style = MaterialTheme.typography.bodySmall, color = NotionGray400)
        }
        Spacer(modifier = Modifier.size(14.dp))
        Button(onClick = { WifiSync.start(side) }, enabled = status !is WifiSync.Status.Working, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(WifiSyncStrings.syncWithPc)
        }
        val message = when (status) {
            WifiSync.Status.Idle -> null
            is WifiSync.Status.Working -> stepLabel(status.step, status.otherName)
            is WifiSync.Status.Done -> syncSummary(status.result)
            is WifiSync.Status.Failed -> when (status.problem) {
                WifiSync.Problem.NOT_FOUND -> WifiSyncStrings.notFound
                WifiSync.Problem.DIFFERENT_VERSION -> WifiSyncStrings.differentVersion
                WifiSync.Problem.REFUSED -> WifiSyncStrings.refused
                WifiSync.Problem.PAIRING_LOST -> WifiSyncStrings.pairingLost
                WifiSync.Problem.NETWORK -> WifiSyncStrings.network
            }
        }
        if (message != null) {
            Spacer(modifier = Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (status is WifiSync.Status.Working) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = NotionWhite)
            }
        }
    }
    if (pc != null && status !is WifiSync.Status.Working) {
        SettingsItem(WifiSyncStrings.forgetPc, icon = Icons.Filled.Wifi, value = pc.name, onClick = {
            WifiSync.forget(side, pc.id)
            onChanged()
        })
    }
}

@Composable
private fun PcPart(side: SyncSide, state: SyncState, running: Boolean, last: SyncResult?, onChanged: () -> Unit) {
    var portBusy by remember { mutableStateOf(false) }
    SettingsItem(
        WifiSyncStrings.waitForPhone,
        icon = Icons.Filled.Wifi,
        value = if (running) WifiSyncStrings.waitingOn(SyncDiscovery.localAddresses().joinToString().ifEmpty { "—" }) else WifiSyncStrings.off,
        onClick = null,
        trailing = {
            Switch(checked = state.acceptPhones && running, onCheckedChange = { on ->
                side.store.update { it.copy(acceptPhones = on) }
                if (on) portBusy = !SyncServer.start(side) else SyncServer.stop()
                onChanged()
            })
        }
    )
    // Il telefono decide cosa va dove: il tasto gli chiede di partire, e la risposta arriva come una sua sincronizzazione.
    val scope = rememberCoroutineScope()
    val syncedAt by SyncServer.syncedAt.collectAsState()
    var askedAt by remember { mutableStateOf<Long?>(null) }
    var noAnswer by remember { mutableStateOf(false) }
    LaunchedEffect(syncedAt) { if (askedAt != null && syncedAt > askedAt!!) askedAt = null }
    LaunchedEffect(askedAt) {
        val asked = askedAt ?: return@LaunchedEffect
        delay(PHONE_ANSWER_MS)
        if (askedAt == asked) {
            askedAt = null
            noAnswer = true
        }
    }
    if (running && state.peers.isNotEmpty()) {
        Button(
            onClick = {
                noAnswer = false
                askedAt = System.currentTimeMillis()
                scope.launch(Dispatchers.IO) { SyncServer.askPhones(side) }
            },
            enabled = askedAt == null,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth()
        ) {
            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(WifiSyncStrings.syncWithPhone)
        }
        val waiting = when {
            askedAt != null -> WifiSyncStrings.askingPhone
            noAnswer -> WifiSyncStrings.phoneNotAnswering
            else -> null
        }
        if (waiting != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                if (askedAt != null) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(waiting, style = MaterialTheme.typography.bodyMedium, color = NotionWhite)
            }
        }
    }
    if (portBusy) {
        Text(WifiSyncStrings.portBusy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp))
    }
    Text(WifiSyncStrings.pcNote, style = MaterialTheme.typography.bodySmall, color = NotionGray400, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    if (last != null) {
        Text(syncSummary(last), style = MaterialTheme.typography.bodyMedium, color = NotionWhite, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
    }
    SectionHeader(WifiSyncStrings.pairedPhones)
    if (state.peers.isEmpty()) {
        Text(WifiSyncStrings.noPhones, style = MaterialTheme.typography.bodyMedium, color = NotionGray400, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
    }
    for (phone in state.peers) {
        SettingsItem(
            phone.name,
            icon = Icons.Filled.PhoneAndroid,
            value = WifiSyncStrings.lastSync + ": " + (phone.lastSyncAt.takeIf { it > 0 }?.let(::whenLabel) ?: WifiSyncStrings.never),
            trailing = {
                TextButton(onClick = {
                    WifiSync.forget(side, phone.id)
                    onChanged()
                }) { Text(WifiSyncStrings.remove) }
            }
        )
    }
}

/** Quanto aspettare che il telefono chiamato dal PC cominci e finisca (con la prima volta e i file, di più). */
private const val PHONE_ANSWER_MS = 90_000L

private fun stepLabel(step: SyncStep, otherName: String?): String = when (step) {
    SyncStep.SEARCHING -> WifiSyncStrings.searching
    SyncStep.CONNECTING -> WifiSyncStrings.connecting(otherName ?: "PC")
    SyncStep.PAIRING -> WifiSyncStrings.pairing
    SyncStep.BACKUP -> WifiSyncStrings.backingUp
    SyncStep.COMPARING -> WifiSyncStrings.comparing
    SyncStep.FILES -> WifiSyncStrings.files
    SyncStep.PAGES -> WifiSyncStrings.pages
}

internal fun syncSummary(result: SyncResult): String = WifiSyncStrings.done(
    result.otherName,
    result.sent,
    result.received,
    result.trashedHere + result.trashedThere,
    result.conflicts,
    result.filesSent + result.filesReceived
)

/**
 * **La domanda del codice** (`SyncPrompts`), sopra qualunque schermata: sul
 * telefono durante la prima sincronizzazione, sul PC quando un telefono
 * nuovo chiama. Il codice è lo stesso sui due schermi, se nessuno si è messo
 * in mezzo.
 */
@Composable
fun SyncPairingDialog() {
    val request by SyncPrompts.current.collectAsState()
    val asking = request ?: return
    FitAlertDialog(
        onDismissRequest = { asking.answer.complete(false) },
        icon = { Icon(Icons.Filled.Sync, contentDescription = null) },
        title = { Text(WifiSyncStrings.pairTitle(asking.otherName)) },
        text = {
            Column {
                Text(WifiSyncStrings.pairText, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    asking.code.chunked(3).joinToString(" "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NotionWhite
                )
            }
        },
        confirmButton = { TextButton(onClick = { asking.answer.complete(true) }) { Text(WifiSyncStrings.pair) } },
        dismissButton = { TextButton(onClick = { asking.answer.complete(false) }) { Text(Strings.cancel) } }
    )
}
