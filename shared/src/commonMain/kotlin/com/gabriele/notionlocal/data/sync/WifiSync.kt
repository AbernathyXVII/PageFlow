package com.gabriele.notionlocal.data.sync

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.backup.BackupReason
import com.gabriele.notionlocal.data.backup.Backups
import com.gabriele.notionlocal.data.filesDirOf
import com.gabriele.notionlocal.platform.PlatformContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread

/** Sul PC si aspetta il telefono (`SyncServer`); sul telefono si cerca il PC (`WifiSync.start`). */
expect val syncWaitsForPhone: Boolean

/** Il nome di questo dispositivo, che l'altro mostra: il modello del telefono, il nome del PC. */
expect fun syncDeviceName(): String

/** Questo lato: il database dell'app, la cartella dei file delle pagine, `sync.json`, e il backup prima della prima volta. */
fun syncSide(context: PlatformContext): SyncSide {
    val dir = filesDirOf(context)
    return SyncSide(
        db = AppDatabase.getInstance(context),
        filesDir = File(dir, PageImageStore.DIRECTORY).apply { mkdirs() },
        store = SyncStore(File(dir, "sync.json")),
        deviceName = syncDeviceName(),
        backupFirst = { runCatching { Backups.backupNow(BackupReason.BEFORE_SYNC) } }
    )
}

/**
 * **Il telefono che sincronizza** (dal 10/11/2026): cerca il PC sulla rete
 * (prima quello già abbinato), lo chiama e racconta a che punto è. Gira per
 * conto suo: uscendo dalla pagina "Connections" non si ferma a metà.
 */
object WifiSync {
    sealed interface Status {
        data object Idle : Status
        data class Working(val step: SyncStep, val otherName: String?) : Status
        data class Done(val result: SyncResult) : Status
        data class Failed(val problem: Problem) : Status
    }

    enum class Problem { NOT_FOUND, DIFFERENT_VERSION, REFUSED, PAIRING_LOST, NETWORK }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status

    /** Dopo una sincronizzazione chiesta dal PC (sul telefono: la scritta in basso, ovunque si sia). */
    var onPcRequestDone: (Status) -> Unit = {}

    fun start(side: SyncSide, onDone: (Status) -> Unit = {}) {
        if (_status.value is Status.Working) return
        _status.value = Status.Working(SyncStep.SEARCHING, null)
        scope.launch {
            _status.value = try {
                val found = SyncDiscovery.search()
                val paired = side.store.load().peers.mapTo(HashSet()) { it.id }
                val target = found.firstOrNull { it.id in paired } ?: found.firstOrNull()
                if (target == null) {
                    Status.Failed(Problem.NOT_FOUND)
                } else {
                    val progress: (SyncStep) -> Unit = { _status.value = Status.Working(it, target.name) }
                    val first = SyncSession.runClient(side, target.host, target.port, SyncPrompts.confirm, progress)
                    // Dopo un conflitto le pagine rimaste senza posto tornano dove stavano (`SyncDatabase.apply`),
                    // da una parte sola: un secondo giro porta anche quello di là.
                    val result = if (first.conflicts > 0) first + SyncSession.runClient(side, target.host, target.port, SyncPrompts.confirm, progress) else first
                    Status.Done(result)
                }
            } catch (e: SyncException) {
                Status.Failed(
                    when (e.reason) {
                        SyncException.Reason.DIFFERENT_VERSION -> Problem.DIFFERENT_VERSION
                        SyncException.Reason.REFUSED -> Problem.REFUSED
                        SyncException.Reason.PAIRING_LOST -> Problem.PAIRING_LOST
                    }
                )
            } catch (e: IOException) {
                Status.Failed(Problem.NETWORK)
            }
            onDone(_status.value)
            // Abbinato adesso: da qui in poi anche il tasto del PC fa partire la sincronizzazione.
            if (_status.value is Status.Done && !syncWaitsForPhone) listenForPc(side)
        }
    }

    @Volatile
    private var listening: DatagramSocket? = null

    /**
     * **Il telefono ascolta il PC** finché PageFlow è aperto: al tasto
     * "Sincronizza col telefono" di un PC abbinato parte la sincronizzazione,
     * come col tasto del telefono. Una richiesta da un PC sconosciuto non fa niente.
     */
    @Synchronized
    fun listenForPc(side: SyncSide) {
        if (listening != null) return
        val socket = runCatching {
            DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(SyncDiscovery.PHONE_PORT))
            }
        }.getOrNull() ?: return
        listening = socket
        thread(isDaemon = true, name = "sync-listen") {
            val buffer = ByteArray(256)
            while (!socket.isClosed) {
                val packet = DatagramPacket(buffer, buffer.size)
                if (runCatching { socket.receive(packet) }.isFailure) continue
                val pcId = SyncDiscovery.requestingPc(packet.data.decodeToString(0, packet.length)) ?: continue
                if (side.store.load().peer(pcId) != null) start(side) { onPcRequestDone(it) }
            }
        }
    }

    @Synchronized
    fun stopListening() {
        runCatching { listening?.close() }
        listening = null
    }

    /** Dimentica un dispositivo abbinato: la prossima volta si riabbina col codice. */
    fun forget(side: SyncSide, id: String) {
        side.store.update { state -> state.copy(peers = state.peers.filter { it.id != id }) }
    }
}
