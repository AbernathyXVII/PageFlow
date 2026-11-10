package com.gabriele.notionlocal.data.sync

import com.gabriele.notionlocal.data.AppDatabase
import com.gabriele.notionlocal.data.DATABASE_VERSION
import com.gabriele.notionlocal.data.entity.VersionReason
import com.gabriele.notionlocal.data.repository.PageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

/** Quello che serve a un lato per sincronizzare: il database, la cartella dei file delle pagine, l'abbinamento, il nome. */
class SyncSide(
    val db: AppDatabase,
    val filesDir: File,
    val store: SyncStore,
    val deviceName: String,
    /** Prima della prima sincronizzazione con un dispositivo: un backup (nei test, niente). */
    val backupFirst: suspend () -> Unit = {}
)

/** Chiede di confermare il codice dell'abbinamento: vero se sull'altro schermo c'è lo stesso. */
fun interface SyncConfirm {
    suspend fun confirm(otherName: String, code: String): Boolean
}

class SyncException(val reason: Reason, message: String = reason.name) : IOException(message) {
    enum class Reason {
        /** PageFlow di versioni diverse: il database non è lo stesso. */
        DIFFERENT_VERSION,

        /** Il codice non è stato confermato, da una parte o dall'altra. */
        REFUSED,

        /** L'abbinamento non vale più (rifatto dall'altra parte): da rifare. */
        PAIRING_LOST
    }
}

/** Com'è andata: quanti pezzi (pagine, righe, quaderni) e file sono andati e venuti. */
data class SyncResult(
    val otherName: String,
    val sent: Int,
    val received: Int,
    val trashedHere: Int,
    val trashedThere: Int,
    val conflicts: Int,
    val filesSent: Int,
    val filesReceived: Int
) {
    /** Due giri insieme, per la scritta: i conflitti sono quelli del primo (il secondo rimette solo i posti delle pagine). */
    operator fun plus(other: SyncResult) = SyncResult(
        otherName, sent + other.sent, received + other.received, trashedHere + other.trashedHere, trashedThere + other.trashedThere,
        conflicts, filesSent + other.filesSent, filesReceived + other.filesReceived
    )
}

/**
 * **La sincronizzazione Wi‑Fi** (dal 10/11/2026): il telefono chiama, il PC
 * risponde. Una connessione TCP; prima un saluto in chiaro (chi sono, la
 * versione del database, un numero a caso), la prima volta l'abbinamento col
 * codice (`SyncCrypto`), poi tutto cifrato:
 * 1. il telefono chiede le impronte del PC, dei suoi registri e dei suoi file;
 * 2. decide (`SyncPlan`), si scambiano i file che mancano;
 * 3. prende i pezzi del PC che vincono, manda i suoi: il PC li mette, e
 *    risponde con le impronte nuove;
 * 4. mette quelli del PC, e si ricorda le impronte (`SyncPlan.newBase`).
 *
 * Una pagina sostituita lascia prima com'era nella cronologia
 * (`VersionReason.BEFORE_SYNC`); la prima volta con un dispositivo, prima di
 * tutto, un backup.
 */
object SyncSession {
    private const val MAGIC = "PAGEFLOW-SYNC"
    private const val PROTOCOL = 1
    private const val OK = "PAGEFLOW-OK"
    private const val STATUS_OK = 0
    private const val STATUS_DIFFERENT_VERSION = 1
    private const val MODE_SESSION = 0
    private const val MODE_PAIR = 1
    private const val CMD_MANIFEST = 1
    private const val CMD_PUT_FILES = 2
    private const val CMD_GET_FILES = 3
    private const val CMD_GET = 4
    private const val CMD_PUT = 5
    private const val CMD_BYE = 6
    private const val CONNECT_TIMEOUT_MS = 5_000
    /** Abbastanza per andare dal telefono al PC a confermare il codice. */
    private const val READ_TIMEOUT_MS = 180_000
    /** I registri si guardano da un giorno prima dell'ultima volta: gli orologi dei due lati non sono uguali. */
    private const val LOG_MARGIN_MS = 24 * 60 * 60 * 1000L
    private const val MAX_PLAIN_BYTES = 4096

    // --- Il telefono ---

    suspend fun runClient(side: SyncSide, host: String, port: Int, confirm: SyncConfirm, progress: (SyncStep) -> Unit = {}): SyncResult =
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                progress(SyncStep.CONNECTING)
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = READ_TIMEOUT_MS
                val rawIn = DataInputStream(BufferedInputStream(socket.getInputStream()))
                val rawOut = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                val me = side.store.load()
                val clientNonce = SyncCrypto.randomBytes(16)
                rawOut.writeUTF(MAGIC)
                rawOut.writeInt(PROTOCOL)
                rawOut.writeInt(DATABASE_VERSION)
                rawOut.writeUTF(me.deviceId)
                rawOut.writeUTF(side.deviceName)
                writeBytes(rawOut, clientNonce)
                rawOut.flush()
                if (rawIn.readUTF() != MAGIC) throw IOException("non è PageFlow")
                if (rawIn.readInt() != STATUS_OK) throw SyncException(SyncException.Reason.DIFFERENT_VERSION)
                val serverId = rawIn.readUTF()
                val serverName = rawIn.readUTF()
                val serverNonce = readBytes(rawIn)
                val knowsMe = rawIn.readBoolean()
                val known = me.peer(serverId)
                val key = if (known != null && knowsMe) {
                    rawOut.writeByte(MODE_SESSION)
                    rawOut.flush()
                    known.keyBytes
                } else {
                    rawOut.writeByte(MODE_PAIR)
                    progress(SyncStep.PAIRING)
                    pairAsClient(rawIn, rawOut, side, me.deviceId, serverId, serverName, confirm)
                }
                val (toServer, toClient) = SyncCrypto.sessionKeys(key, clientNonce, serverNonce)
                val out = DataOutputStream(EncryptingOutputStream(rawOut, toServer))
                val input = DataInputStream(DecryptingInputStream(rawIn, toClient))
                out.writeUTF(OK)
                out.flush()
                val answer = try {
                    input.readUTF()
                } catch (e: IOException) {
                    // Il PC non ha più la nostra chiave (abbinato di nuovo, o rifatto da capo): la prossima volta si riabbina.
                    side.store.update { state -> state.copy(peers = state.peers.filter { it.id != serverId }) }
                    throw SyncException(SyncException.Reason.PAIRING_LOST)
                }
                if (answer != OK) throw IOException("risposta sbagliata")
                exchange(side, serverId, serverName, input, out, progress)
            }
        }

    private suspend fun exchange(
        side: SyncSide,
        serverId: String,
        serverName: String,
        input: DataInputStream,
        out: DataOutputStream,
        progress: (SyncStep) -> Unit
    ): SyncResult {
        val peer = side.store.load().peer(serverId) ?: throw SyncException(SyncException.Reason.PAIRING_LOST)
        if (peer.lastSyncAt == 0L) {
            progress(SyncStep.BACKUP)
            side.backupFirst()
        }
        val startedAt = System.currentTimeMillis()
        val since = (peer.lastSyncAt - LOG_MARGIN_MS).coerceAtLeast(0)
        progress(SyncStep.COMPARING)
        out.writeByte(CMD_MANIFEST)
        out.writeLong(since)
        out.flush()
        val theirUnits = readManifest(input)
        val theirLogs = readLogKeys(input)
        val theirFiles = readFileList(input)
        val myUnits = SyncDatabase.manifest(side.db)
        val myLogs = SyncDatabase.logKeys(side.db, since)
        val myFiles = SyncFiles.list(side.filesDir)
        val plan = SyncPlan.of(myUnits, theirUnits, peer.base)

        progress(SyncStep.FILES)
        val filesToSend = myFiles.keys - theirFiles.keys
        out.writeByte(CMD_PUT_FILES)
        SyncFiles.send(out, side.filesDir, filesToSend)
        out.flush()
        if (input.readByte().toInt() != 1) throw IOException("file non arrivati")
        out.writeByte(CMD_GET_FILES)
        writeStrings(out, theirFiles.keys - myFiles.keys)
        out.flush()
        val filesReceived = SyncFiles.receive(input, side.filesDir)

        progress(SyncStep.PAGES)
        out.writeByte(CMD_GET)
        writeStrings(out, plan.fetch)
        writeLogKeys(out, missing(theirLogs, myLogs))
        out.flush()
        val fetched = List(input.readInt()) { SyncUnit.read(input) }
        val fetchedLogs = readLogs(input)
        out.writeByte(CMD_PUT)
        writeUnits(out, SyncDatabase.readUnits(side.db, plan.send).values)
        writeStrings(out, plan.trashThere)
        writeLogs(out, SyncDatabase.readLogs(side.db, missing(myLogs, theirLogs)))
        out.flush()
        val theirAfter = readManifest(input)
        applyHere(side, fetched, plan.trashHere, fetchedLogs)
        val myAfter = SyncDatabase.manifest(side.db)
        val base = SyncPlan.newBase(myAfter, theirAfter, plan, peer.base)
        side.store.update { state ->
            val current = state.peer(serverId) ?: peer
            state.withPeer(current.copy(name = serverName, lastSyncAt = startedAt, base = base))
        }
        out.writeByte(CMD_BYE)
        out.flush()
        return SyncResult(
            otherName = serverName,
            sent = plan.send.size,
            received = plan.fetch.size,
            trashedHere = plan.trashHere.size,
            trashedThere = plan.trashThere.size,
            conflicts = plan.conflicts.size,
            filesSent = filesToSend.size,
            filesReceived = filesReceived
        )
    }

    private suspend fun pairAsClient(
        rawIn: DataInputStream,
        rawOut: DataOutputStream,
        side: SyncSide,
        myId: String,
        serverId: String,
        serverName: String,
        confirm: SyncConfirm
    ): ByteArray {
        val keys = SyncCrypto.newKeyPair()
        val public = keys.public.encoded
        val salt = SyncCrypto.randomBytes(16)
        writeBytes(rawOut, SyncCrypto.sha256(public, salt))
        rawOut.flush()
        val serverPublic = readBytes(rawIn)
        val serverSalt = readBytes(rawIn)
        writeBytes(rawOut, public)
        writeBytes(rawOut, salt)
        rawOut.flush()
        val shared = SyncCrypto.sharedSecret(keys.private, serverPublic)
        val transcript = SyncCrypto.sha256(myId.encodeToByteArray(), serverId.encodeToByteArray(), public, serverPublic, salt, serverSalt)
        val accepted = confirm.confirm(serverName, SyncCrypto.pairingCode(shared, transcript))
        rawOut.writeBoolean(accepted)
        rawOut.flush()
        val theyAccepted = rawIn.readBoolean()
        if (!accepted || !theyAccepted) throw SyncException(SyncException.Reason.REFUSED)
        val key = SyncCrypto.pairingKey(shared, transcript)
        side.store.update { it.withPeer(SyncPeer(serverId, serverName, SyncStore.encodeKey(key))) }
        return key
    }

    // --- Il PC ---

    /**
     * Risponde a un telefono sulla connessione [socket]: com'è andata, o null
     * se non era PageFlow, non si è abbinato o si è interrotto.
     */
    suspend fun serve(side: SyncSide, socket: Socket, confirm: SyncConfirm): SyncResult? = withContext(Dispatchers.IO) {
        socket.use {
            try {
                socket.soTimeout = READ_TIMEOUT_MS
                val rawIn = DataInputStream(BufferedInputStream(socket.getInputStream()))
                val rawOut = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                if (rawIn.readUTF() != MAGIC) return@withContext null
                val protocol = rawIn.readInt()
                val databaseVersion = rawIn.readInt()
                val clientId = rawIn.readUTF()
                val clientName = rawIn.readUTF()
                val clientNonce = readBytes(rawIn)
                rawOut.writeUTF(MAGIC)
                if (protocol != PROTOCOL || databaseVersion != DATABASE_VERSION) {
                    rawOut.writeInt(STATUS_DIFFERENT_VERSION)
                    rawOut.flush()
                    return@withContext null
                }
                rawOut.writeInt(STATUS_OK)
                val me = side.store.load()
                val serverNonce = SyncCrypto.randomBytes(16)
                val known = me.peer(clientId)
                rawOut.writeUTF(me.deviceId)
                rawOut.writeUTF(side.deviceName)
                writeBytes(rawOut, serverNonce)
                rawOut.writeBoolean(known != null)
                rawOut.flush()
                val key = when (rawIn.readByte().toInt()) {
                    MODE_SESSION -> known?.keyBytes ?: return@withContext null
                    else -> pairAsServer(rawIn, rawOut, side, me.deviceId, clientId, clientName, confirm) ?: return@withContext null
                }
                val (toServer, toClient) = SyncCrypto.sessionKeys(key, clientNonce, serverNonce)
                val input = DataInputStream(DecryptingInputStream(rawIn, toServer))
                val out = DataOutputStream(EncryptingOutputStream(rawOut, toClient))
                if (input.readUTF() != OK) return@withContext null
                out.writeUTF(OK)
                out.flush()
                answer(side, clientId, clientName, input, out)
            } catch (e: IOException) {
                null
            }
        }
    }

    private suspend fun answer(side: SyncSide, clientId: String, clientName: String, input: DataInputStream, out: DataOutputStream): SyncResult? {
        var sent = 0
        var received = 0
        var trashed = 0
        var filesSent = 0
        var filesReceived = 0
        while (true) {
            when (input.readByte().toInt()) {
                CMD_MANIFEST -> {
                    val since = input.readLong()
                    if (side.store.load().peer(clientId)?.lastSyncAt == 0L) side.backupFirst()
                    writeManifest(out, SyncDatabase.manifest(side.db))
                    writeLogKeys(out, SyncDatabase.logKeys(side.db, since))
                    writeFileList(out, SyncFiles.list(side.filesDir))
                    out.flush()
                }
                CMD_PUT_FILES -> {
                    filesReceived += SyncFiles.receive(input, side.filesDir)
                    out.writeByte(1)
                    out.flush()
                }
                CMD_GET_FILES -> {
                    val names = readStrings(input)
                    SyncFiles.send(out, side.filesDir, names)
                    filesSent += names.size
                    out.flush()
                }
                CMD_GET -> {
                    val ids = readStrings(input).toSet()
                    val wanted = readLogKeys(input)
                    val units = SyncDatabase.readUnits(side.db, ids).values
                    sent += units.size
                    writeUnits(out, units)
                    writeLogs(out, SyncDatabase.readLogs(side.db, wanted))
                    out.flush()
                }
                CMD_PUT -> {
                    val units = List(input.readInt()) { SyncUnit.read(input) }
                    val toTrash = readStrings(input)
                    val logs = readLogs(input)
                    received += units.size
                    trashed += toTrash.size
                    applyHere(side, units, toTrash, logs)
                    writeManifest(out, SyncDatabase.manifest(side.db))
                    out.flush()
                }
                CMD_BYE -> {
                    side.store.update { state ->
                        state.peer(clientId)?.let { state.withPeer(it.copy(name = clientName, lastSyncAt = System.currentTimeMillis())) } ?: state
                    }
                    return SyncResult(clientName, sent, received, trashed, 0, 0, filesSent, filesReceived)
                }
                else -> return null
            }
        }
    }

    private suspend fun pairAsServer(
        rawIn: DataInputStream,
        rawOut: DataOutputStream,
        side: SyncSide,
        myId: String,
        clientId: String,
        clientName: String,
        confirm: SyncConfirm
    ): ByteArray? {
        val commitment = readBytes(rawIn)
        val keys = SyncCrypto.newKeyPair()
        val public = keys.public.encoded
        val salt = SyncCrypto.randomBytes(16)
        writeBytes(rawOut, public)
        writeBytes(rawOut, salt)
        rawOut.flush()
        val clientPublic = readBytes(rawIn)
        val clientSalt = readBytes(rawIn)
        // Il telefono ha scelto la sua chiave prima di vedere la nostra: deve essere quella promessa.
        if (!MessageDigest.isEqual(SyncCrypto.sha256(clientPublic, clientSalt), commitment)) return null
        val shared = SyncCrypto.sharedSecret(keys.private, clientPublic)
        val transcript = SyncCrypto.sha256(clientId.encodeToByteArray(), myId.encodeToByteArray(), clientPublic, public, clientSalt, salt)
        val accepted = confirm.confirm(clientName, SyncCrypto.pairingCode(shared, transcript))
        rawOut.writeBoolean(accepted)
        rawOut.flush()
        val theyAccepted = rawIn.readBoolean()
        if (!accepted || !theyAccepted) return null
        val key = SyncCrypto.pairingKey(shared, transcript)
        side.store.update { it.withPeer(SyncPeer(clientId, clientName, SyncStore.encodeKey(key))) }
        return key
    }

    // --- Tutti e due ---

    /** Mette qui i pezzi arrivati: prima, delle pagine che cambiano, com'erano (nella cronologia). */
    private suspend fun applyHere(side: SyncSide, units: Collection<SyncUnit>, toTrash: Collection<String>, logs: Map<String, List<SyncRow>>) {
        if (units.isEmpty() && toTrash.isEmpty() && logs.isEmpty()) return
        val repository = PageRepository(side.db)
        for (unit in units) {
            if (unit.id.startsWith("p:")) runCatching { repository.saveVersion(unit.id.removePrefix("p:"), VersionReason.BEFORE_SYNC) }
        }
        SyncDatabase.apply(side.db, units, toTrash, logs)
    }

    /** Le righe dei registri che ha [from] e non [to]. */
    private fun missing(from: LogKeys, to: LogKeys) = LogKeys(
        versions = from.versions - to.versions.keys,
        edits = from.edits - to.edits.keys,
        imageTexts = from.imageTexts - to.imageTexts,
        words = from.words - to.words
    )

    private fun writeBytes(out: DataOutputStream, bytes: ByteArray) {
        out.writeShort(bytes.size)
        out.write(bytes)
    }

    private fun readBytes(input: DataInputStream): ByteArray {
        val size = input.readUnsignedShort()
        if (size > MAX_PLAIN_BYTES) throw IOException("troppo lungo")
        return ByteArray(size).also { input.readFully(it) }
    }

    private fun writeStrings(out: DataOutputStream, values: Collection<String>) {
        out.writeInt(values.size)
        values.forEach { out.writeUTF(it) }
    }

    private fun readStrings(input: DataInputStream): List<String> = List(input.readInt()) { input.readUTF() }

    private fun writeManifest(out: DataOutputStream, manifest: Map<String, UnitState>) {
        out.writeInt(manifest.size)
        for ((id, state) in manifest) {
            out.writeUTF(id)
            out.writeUTF(state.hash)
            out.writeLong(state.changedAt)
        }
    }

    private fun readManifest(input: DataInputStream): Map<String, UnitState> {
        val result = HashMap<String, UnitState>()
        repeat(input.readInt()) { result[input.readUTF()] = UnitState(input.readUTF(), input.readLong()) }
        return result
    }

    private fun writeLogKeys(out: DataOutputStream, keys: LogKeys) {
        for (map in listOf(keys.versions, keys.edits)) {
            out.writeInt(map.size)
            for ((id, pageId) in map) {
                out.writeUTF(id)
                out.writeUTF(pageId)
            }
        }
        writeStrings(out, keys.imageTexts)
        writeStrings(out, keys.words)
    }

    private fun readLogKeys(input: DataInputStream): LogKeys {
        fun map(): Map<String, String> = HashMap<String, String>().apply { repeat(input.readInt()) { put(input.readUTF(), input.readUTF()) } }
        val versions = map()
        val edits = map()
        return LogKeys(versions, edits, readStrings(input).toSet(), readStrings(input).toSet())
    }

    private fun writeFileList(out: DataOutputStream, files: Map<String, Long>) {
        out.writeInt(files.size)
        for ((name, size) in files) {
            out.writeUTF(name)
            out.writeLong(size)
        }
    }

    private fun readFileList(input: DataInputStream): Map<String, Long> =
        HashMap<String, Long>().apply { repeat(input.readInt()) { put(input.readUTF(), input.readLong()) } }

    private fun writeUnits(out: DataOutputStream, units: Collection<SyncUnit>) {
        out.writeInt(units.size)
        units.forEach { it.write(out) }
    }

    private fun writeLogs(out: DataOutputStream, logs: Map<String, List<SyncRow>>) {
        out.writeInt(logs.size)
        for ((table, rows) in logs) {
            out.writeUTF(table)
            writeRows(out, rows)
        }
    }

    private fun readLogs(input: DataInputStream): Map<String, List<SyncRow>> =
        LinkedHashMap<String, List<SyncRow>>().apply { repeat(input.readInt()) { put(input.readUTF(), readRows(input)) } }
}

/** A che punto è: per la scritta sul telefono. */
enum class SyncStep { SEARCHING, CONNECTING, PAIRING, BACKUP, COMPARING, FILES, PAGES }
