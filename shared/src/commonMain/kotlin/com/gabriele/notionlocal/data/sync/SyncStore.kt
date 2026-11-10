package com.gabriele.notionlocal.data.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64
import java.util.UUID

/** Un dispositivo abbinato: il PC per il telefono, i telefoni per il PC. */
@Serializable
data class SyncPeer(
    val id: String,
    val name: String,
    /** La chiave dell'abbinamento, in Base64. */
    val key: String,
    /** L'ultima sincronizzazione finita, in millisecondi (0 mai). */
    val lastSyncAt: Long = 0,
    /** Le impronte dei pezzi all'ultima sincronizzazione (solo il telefono, che decide). */
    val base: Map<String, String> = emptyMap(),
    /** Per ogni pagina, le impronte delle sue righe all'ultima sincronizzazione: per unirle (`SyncMerge`). */
    val elements: Map<String, Map<String, String>> = emptyMap(),
    /** Sul PC: l'indirizzo da cui il telefono ha sincronizzato l'ultima volta, per chiamarlo col tasto. */
    val address: String? = null
) {
    val keyBytes: ByteArray get() = Base64.getDecoder().decode(key)
}

@Serializable
data class SyncState(
    val deviceId: String = UUID.randomUUID().toString(),
    /** Sul PC: se aspetta il telefono. */
    val acceptPhones: Boolean = false,
    val peers: List<SyncPeer> = emptyList()
) {
    fun peer(id: String): SyncPeer? = peers.firstOrNull { it.id == id }

    fun withPeer(peer: SyncPeer): SyncState = copy(peers = peers.filter { it.id != peer.id } + peer)
}

/**
 * **Chi siamo e con chi siamo abbinati** (dal 10/11/2026), in `sync.json`
 * nella cartella dell'app: non nelle impostazioni, che viaggiano nei backup —
 * un backup del PC rimesso sul telefono gli darebbe l'identità del PC.
 */
class SyncStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Synchronized
    fun load(): SyncState {
        val saved = runCatching { json.decodeFromString(SyncState.serializer(), file.readText()) }.getOrNull()
        return saved ?: SyncState().also { save(it) }
    }

    @Synchronized
    fun update(change: (SyncState) -> SyncState): SyncState = change(load()).also { save(it) }

    private fun save(state: SyncState) {
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.writeText(json.encodeToString(SyncState.serializer(), state))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    companion object {
        fun encodeKey(key: ByteArray): String = Base64.getEncoder().encodeToString(key)
    }
}
