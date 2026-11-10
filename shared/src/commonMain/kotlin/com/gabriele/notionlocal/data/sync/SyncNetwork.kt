package com.gabriele.notionlocal.data.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

/**
 * **Trovarsi sulla rete di casa** (dal 10/11/2026): il telefono manda a tutti
 * (broadcast UDP) "c'è un PageFlow?", e il PC che aspetta risponde con la
 * porta, chi è e come si chiama. Niente indirizzi da scrivere.
 */
object SyncDiscovery {
    const val PORT = 47390
    /** Dove il telefono, con PageFlow aperto, ascolta il PC che gli chiede di sincronizzare. */
    const val PHONE_PORT = 47391
    private const val ASK = "PAGEFLOW-SYNC?"
    private const val ANSWER = "PAGEFLOW-SYNC!"
    private const val SYNC_NOW = "PAGEFLOW-SYNC-NOW\n"

    data class Found(val host: String, val port: Int, val id: String, val name: String)

    /** Il telefono: chi risponde entro [waitMs]. */
    suspend fun search(waitMs: Long = 2_500): List<Found> = withContext(Dispatchers.IO) {
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.soTimeout = 250
            val ask = ASK.encodeToByteArray()
            val targets = broadcastAddresses() + InetAddress.getByName("255.255.255.255")
            repeat(2) { targets.forEach { runCatching { socket.send(DatagramPacket(ask, ask.size, it, PORT)) } } }
            val found = LinkedHashMap<String, Found>()
            val buffer = ByteArray(1024)
            val end = System.currentTimeMillis() + waitMs
            while (System.currentTimeMillis() < end) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                }
                parse(packet.data.decodeToString(0, packet.length), packet.address.hostAddress ?: continue)?.let { found[it.id] = it }
            }
            found.values.toList()
        }
    }

    /** Il PC: la risposta a "c'è un PageFlow?". */
    fun answerFor(port: Int, id: String, name: String): String = ANSWER + listOf(port.toString(), id, name.replace('\n', ' ')).joinToString("\n")

    fun isQuestion(text: String) = text == ASK

    /** Il PC al telefono: "sincronizza adesso", con chi lo chiede (deve essere un PC abbinato). */
    fun syncRequest(pcId: String): String = SYNC_NOW + pcId

    /** Il PC che chiede di sincronizzare, o null se non è una richiesta. */
    fun requestingPc(text: String): String? = text.takeIf { it.startsWith(SYNC_NOW) }?.removePrefix(SYNC_NOW)?.takeIf { it.isNotBlank() }

    fun parse(text: String, host: String): Found? {
        if (!text.startsWith(ANSWER)) return null
        val parts = text.removePrefix(ANSWER).split('\n')
        if (parts.size < 3) return null
        val port = parts[0].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return Found(host, port, parts[1], parts[2])
    }

    /** Gli indirizzi "a tutti" delle reti di questo dispositivo (il Wi‑Fi di casa), oltre a 255.255.255.255. */
    fun broadcastAddresses(): List<InetAddress> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.interfaceAddresses }
            .mapNotNull { it.broadcast }
    }.getOrDefault(emptyList())

    /** Gli indirizzi di questo dispositivo sulla rete, da mostrare sul PC. */
    fun localAddresses(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
    }.getOrDefault(emptyList())
}

/**
 * **La domanda del codice**, sulla finestra che c'è: telefono e PC mostrano
 * lo stesso codice di sei cifre e chiedono di confermarlo (`SyncPairingDialog`).
 * Senza risposta in tre minuti, è un no.
 */
object SyncPrompts {
    class Request(val otherName: String, val code: String) {
        val answer = CompletableDeferred<Boolean>()
    }

    private val _current = MutableStateFlow<Request?>(null)
    val current: StateFlow<Request?> = _current

    /** Sul PC: la finestra torna davanti quando un telefono chiede (la mette `Main.kt`). */
    var onRequest: () -> Unit = {}

    val confirm = SyncConfirm { otherName, code ->
        val request = Request(otherName, code)
        _current.value = request
        onRequest()
        try {
            withTimeoutOrNull(ANSWER_TIMEOUT_MS) { request.answer.await() } ?: false
        } finally {
            _current.compareAndSet(request, null)
        }
    }

    private const val ANSWER_TIMEOUT_MS = 170_000L
}

/**
 * **Il PC che aspetta il telefono** (dal 10/11/2026): finché è acceso
 * risponde a chi lo cerca e a una sincronizzazione alla volta. Windows, la
 * prima volta, chiede se PageFlow può usare la rete.
 */
object SyncServer {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _last = MutableStateFlow<SyncResult?>(null)
    /** L'ultima sincronizzazione finita, per la schermata. */
    val last: StateFlow<SyncResult?> = _last

    private val _syncedAt = MutableStateFlow(0L)
    /** Quando è finita l'ultima: cambia a ogni sincronizzazione, anche uguale alla precedente. */
    val syncedAt: StateFlow<Long> = _syncedAt

    /** Dopo una sincronizzazione (sul PC: la notifica). */
    var onSynced: (SyncResult) -> Unit = {}

    @Volatile
    private var server: ServerSocket? = null

    @Volatile
    private var discovery: DatagramSocket? = null

    /** Comincia ad aspettare; falso se non si può (la porta di PageFlow è già presa, per esempio da un altro PageFlow). */
    @Synchronized
    fun start(side: SyncSide): Boolean {
        if (server != null) return true
        val tcp = runCatching { ServerSocket(0) }.getOrNull() ?: return false
        val udp = runCatching {
            DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(SyncDiscovery.PORT))
            }
        }.getOrNull()
        if (udp == null) {
            tcp.close()
            return false
        }
        server = tcp
        discovery = udp
        _running.value = true
        val me = side.store.load()
        thread(isDaemon = true, name = "sync-discovery") {
            val buffer = ByteArray(256)
            while (!udp.isClosed) {
                val packet = DatagramPacket(buffer, buffer.size)
                runCatching { udp.receive(packet) }.onFailure { if (udp.isClosed) return@thread }.getOrNull() ?: continue
                if (!SyncDiscovery.isQuestion(packet.data.decodeToString(0, packet.length))) continue
                val answer = SyncDiscovery.answerFor(tcp.localPort, me.deviceId, side.deviceName).encodeToByteArray()
                runCatching { udp.send(DatagramPacket(answer, answer.size, packet.socketAddress)) }
            }
        }
        thread(isDaemon = true, name = "sync-server") {
            while (!tcp.isClosed) {
                val socket = runCatching { tcp.accept() }.getOrNull() ?: continue
                val result = runCatching { runBlocking { SyncSession.serve(side, socket, SyncPrompts.confirm) } }.getOrNull()
                if (result != null) {
                    _last.value = result
                    _syncedAt.value = System.currentTimeMillis()
                    runCatching { onSynced(result) }
                }
            }
        }
        return true
    }

    /**
     * **Il tasto "Sincronizza col telefono"**: chiede ai telefoni abbinati di
     * sincronizzare, all'indirizzo dell'ultima volta e a tutta la rete. È
     * il telefono a partire (decide lui cosa va dove), quindi ci vuole
     * PageFlow aperto sul telefono. Falso se nessun telefono è abbinato.
     */
    fun askPhones(side: SyncSide): Boolean {
        val me = side.store.load()
        if (me.peers.isEmpty()) return false
        val request = SyncDiscovery.syncRequest(me.deviceId).encodeToByteArray()
        val targets = me.peers.mapNotNull { peer -> peer.address?.let { runCatching { InetAddress.getByName(it) }.getOrNull() } } +
            SyncDiscovery.broadcastAddresses() + InetAddress.getByName("255.255.255.255")
        DatagramSocket().use { socket ->
            socket.broadcast = true
            repeat(2) { targets.forEach { runCatching { socket.send(DatagramPacket(request, request.size, it, SyncDiscovery.PHONE_PORT)) } } }
        }
        return true
    }

    @Synchronized
    fun stop() {
        runCatching { server?.close() }
        runCatching { discovery?.close() }
        server = null
        discovery = null
        _running.value = false
    }
}
