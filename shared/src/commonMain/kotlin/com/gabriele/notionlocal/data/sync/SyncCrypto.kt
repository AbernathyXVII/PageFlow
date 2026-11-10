package com.gabriele.notionlocal.data.sync

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * **La cifratura della sincronizzazione** (dal 10/11/2026), con quello che
 * Java e Android hanno già:
 * - la prima volta telefono e PC si scambiano una chiave (ECDH, curva
 *   P-256) e mostrano tutti e due lo stesso codice di sei cifre, da
 *   confrontare a occhio: chi si mettesse in mezzo ne farebbe vedere due
 *   diversi. Il telefono si impegna sulla sua chiave prima di vedere quella
 *   del PC, così nessuno può cercarne una che dia lo stesso codice;
 * - da lì ogni sincronizzazione ha chiavi sue (dalla chiave dell'abbinamento
 *   e da due numeri a caso), e tutto viaggia in AES-GCM: chi ascolta il
 *   Wi‑Fi non legge niente, e niente si può cambiare per strada.
 */
object SyncCrypto {
    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1"), random) }.generateKeyPair()

    fun sharedSecret(mine: PrivateKey, theirPublic: ByteArray): ByteArray {
        val public = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(theirPublic))
        return KeyAgreement.getInstance("ECDH").apply {
            init(mine)
            doPhase(public, true)
        }.generateSecret()
    }

    fun sha256(vararg parts: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").run {
        parts.forEach { update(lengthOf(it)); update(it) }
        digest()
    }

    fun hmac(key: ByteArray, vararg parts: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        parts.forEach { update(lengthOf(it)); update(it) }
        doFinal()
    }

    /** Il codice di sei cifre da confrontare sui due schermi. */
    fun pairingCode(shared: ByteArray, transcript: ByteArray): String {
        val bytes = hmac(shared, "pageflow-code".encodeToByteArray(), transcript)
        val number = ((bytes[0].toLong() and 0xFF) shl 24) or ((bytes[1].toLong() and 0xFF) shl 16) or
            ((bytes[2].toLong() and 0xFF) shl 8) or (bytes[3].toLong() and 0xFF)
        return "%06d".format(number % 1_000_000)
    }

    fun pairingKey(shared: ByteArray, transcript: ByteArray): ByteArray = hmac(shared, "pageflow-pair".encodeToByteArray(), transcript)

    /** Le due chiavi di una sincronizzazione: telefono → PC e PC → telefono. */
    fun sessionKeys(pairingKey: ByteArray, clientNonce: ByteArray, serverNonce: ByteArray): Pair<ByteArray, ByteArray> {
        val session = hmac(pairingKey, "pageflow-session".encodeToByteArray(), clientNonce, serverNonce)
        return hmac(session, "c2s".encodeToByteArray()) to hmac(session, "s2c".encodeToByteArray())
    }

    private fun lengthOf(bytes: ByteArray): ByteArray = byteArrayOf(
        (bytes.size ushr 24).toByte(), (bytes.size ushr 16).toByte(), (bytes.size ushr 8).toByte(), bytes.size.toByte()
    )
}

/**
 * Scrive a pezzi cifrati: ognuno con la sua lunghezza e un contatore come
 * nonce (una chiave per direzione, quindi mai due volte lo stesso). Un
 * pezzo parte quando è pieno o con `flush`.
 */
class EncryptingOutputStream(private val out: OutputStream, key: ByteArray) : OutputStream() {
    private val key = SecretKeySpec(key, "AES")
    private val buffer = ByteArray(FRAME_BYTES)
    private var size = 0
    private var counter = 0L

    override fun write(b: Int) {
        if (size == buffer.size) sendFrame()
        buffer[size++] = b.toByte()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var from = off
        var left = len
        while (left > 0) {
            if (size == buffer.size) sendFrame()
            val n = minOf(left, buffer.size - size)
            System.arraycopy(b, from, buffer, size, n)
            size += n
            from += n
            left -= n
        }
    }

    override fun flush() {
        if (size > 0) sendFrame()
        out.flush()
    }

    override fun close() {
        flush()
        out.close()
    }

    private fun sendFrame() {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce(counter++))) }
        val sealed = cipher.doFinal(buffer, 0, size)
        DataOutputStream(out).writeInt(sealed.size)
        out.write(sealed)
        size = 0
    }
}

/** Legge i pezzi di `EncryptingOutputStream`: uno cambiato o fuori posto ferma tutto (IOException). */
class DecryptingInputStream(input: InputStream, key: ByteArray) : InputStream() {
    private val input = DataInputStream(input)
    private val key = SecretKeySpec(key, "AES")
    private var plain = ByteArray(0)
    private var at = 0
    private var counter = 0L

    override fun read(): Int {
        if (!fill()) return -1
        return plain[at++].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        if (!fill()) return -1
        val n = minOf(len, plain.size - at)
        System.arraycopy(plain, at, b, off, n)
        at += n
        return n
    }

    private fun fill(): Boolean {
        while (at == plain.size) {
            val length = try {
                input.readInt()
            } catch (e: EOFException) {
                return false
            }
            if (length !in 16..(FRAME_BYTES + 16)) throw IOException("pezzo cifrato non valido")
            val sealed = ByteArray(length).also { input.readFully(it) }
            plain = try {
                Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce(counter++))) }.doFinal(sealed)
            } catch (e: Exception) {
                throw IOException("pezzo cifrato non valido", e)
            }
            at = 0
        }
        return true
    }
}

private const val FRAME_BYTES = 64 * 1024

private fun nonce(counter: Long): ByteArray = ByteArray(12).also { bytes ->
    for (i in 0 until 8) bytes[4 + i] = (counter ushr (56 - 8 * i)).toByte()
}
