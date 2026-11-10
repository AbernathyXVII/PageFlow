package com.gabriele.notionlocal.data.webclip

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * **Microsoft Edge nascosto**, per il Web Clipper del PC (dal 10/11/2026):
 * avviato senza finestra, con un suo profilo nella cartella dei dati (niente
 * cronologia né account dell'Edge di tutti i giorni), e comandato col
 * protocollo dei suoi strumenti per sviluppatori (Chrome DevTools Protocol)
 * su un WebSocket. Se Edge non c'è, va bene anche Chrome.
 */
class EdgeBrowser private constructor(private val process: Process, private val socket: WebSocket, private val inbox: Inbox) : AutoCloseable {

    /** Esegue [method] con [params]; il suo "result", o un errore se il browser dice di no. */
    suspend fun send(method: String, params: JsonObject = JsonObject(emptyMap()), timeoutMs: Long = COMMAND_TIMEOUT_MS): JsonObject {
        val id = inbox.nextId.incrementAndGet()
        val answer = CompletableDeferred<JsonObject>()
        inbox.pending[id] = answer
        val message = buildJsonObject {
            put("id", id)
            put("method", method)
            put("params", params)
        }
        socket.sendText(message.toString(), true).await()
        val reply = withTimeoutOrNull(timeoutMs) { answer.await() }
        inbox.pending.remove(id)
        reply ?: error("$method: nessuna risposta")
        (reply["error"] as? JsonObject)?.let { error("$method: ${it["message"]}") }
        return reply["result"] as? JsonObject ?: JsonObject(emptyMap())
    }

    /** Aspetta che il browser dica [method] (un evento, come "Page.loadEventFired"); null dopo [timeoutMs]. */
    suspend fun awaitEvent(method: String, timeoutMs: Long): JsonObject? {
        val event = CompletableDeferred<JsonObject>()
        val waiter = method to event
        inbox.waiters += waiter
        return try {
            withTimeoutOrNull(timeoutMs) { event.await() }
        } finally {
            inbox.waiters -= waiter
        }
    }

    /** Il JavaScript [script] nella pagina; il suo risultato (anche una promessa, aspettata), o null. */
    suspend fun evaluate(script: String): JsonElement? {
        val result = send(
            "Runtime.evaluate",
            buildJsonObject {
                put("expression", script)
                put("returnByValue", true)
                put("awaitPromise", true)
            }
        )
        if (result["exceptionDetails"] != null) return null
        return (result["result"] as? JsonObject)?.get("value")
    }

    override fun close() {
        runCatching { socket.sendText("""{"id":0,"method":"Browser.close"}""", true) }
        runCatching { socket.abort() }
        if (!runCatching { process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) }.getOrDefault(false)) process.destroyForcibly()
    }

    /** Le risposte che arrivano dal browser, e chi le aspetta. */
    private class Inbox : WebSocket.Listener {
        val nextId = AtomicInteger(0)
        val pending = ConcurrentHashMap<Int, CompletableDeferred<JsonObject>>()
        val waiters = CopyOnWriteArrayList<Pair<String, CompletableDeferred<JsonObject>>>()
        private val text = StringBuilder()

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            text.append(data)
            if (last) {
                val message = runCatching { Json.parseToJsonElement(text.toString()).jsonObject }.getOrNull()
                text.setLength(0)
                if (message != null) {
                    val id = (message["id"] as? JsonPrimitive)?.int
                    if (id != null) {
                        pending[id]?.complete(message)
                    } else {
                        val method = message["method"]?.jsonPrimitive?.content
                        val params = message["params"] as? JsonObject ?: JsonObject(emptyMap())
                        waiters.filter { it.first == method }.forEach { it.second.complete(params) }
                    }
                }
            }
            webSocket.request(1)
            return null
        }
    }

    companion object {
        private const val COMMAND_TIMEOUT_MS = 60_000L
        private const val START_TIMEOUT_MS = 20_000L

        /** Edge (o Chrome), dove li mette l'installazione di Windows; null se non c'è nessuno dei due. */
        fun find(): File? {
            val roots = listOfNotNull(System.getenv("ProgramFiles(x86)"), System.getenv("ProgramFiles"), System.getenv("LOCALAPPDATA"))
            val programs = listOf("Microsoft\\Edge\\Application\\msedge.exe", "Google\\Chrome\\Application\\chrome.exe")
            return programs.flatMap { program -> roots.map { File(it, program) } }.firstOrNull { it.isFile }
        }

        /** Avvia il browser nascosto col profilo [profile] e si collega alla sua pagina; null se non parte. */
        suspend fun launch(profile: File): EdgeBrowser? = withContext(Dispatchers.IO) {
            val program = find() ?: return@withContext null
            profile.mkdirs()
            val portFile = File(profile, "DevToolsActivePort").apply { delete() }
            val process = ProcessBuilder(
                program.path,
                "--headless=new",
                "--remote-debugging-port=0",
                "--user-data-dir=${profile.path}",
                "--no-first-run",
                "--no-default-browser-check",
                "--disable-extensions",
                "--hide-scrollbars",
                "--mute-audio",
                "about:blank"
            ).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
            try {
                // Il browser scrive nel profilo la porta su cui ascolta.
                val port = withTimeoutOrNull(START_TIMEOUT_MS) {
                    var found: Int? = null
                    while (found == null) {
                        found = portFile.takeIf { it.isFile }?.readLines()?.firstOrNull()?.trim()?.toIntOrNull()
                        if (found == null) delay(100)
                    }
                    found
                } ?: error("il browser non è partito")
                val http = HttpClient.newHttpClient()
                val list = http.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port/json/list")).build(), HttpResponse.BodyHandlers.ofString()).body()
                val page = Json.parseToJsonElement(list).jsonArray.map { it.jsonObject }
                    .firstOrNull { it["type"]?.jsonPrimitive?.content == "page" } ?: error("nessuna pagina")
                val address = page["webSocketDebuggerUrl"]!!.jsonPrimitive.content
                val inbox = Inbox()
                val socket = http.newWebSocketBuilder().buildAsync(URI(address), inbox).await()
                EdgeBrowser(process, socket, inbox)
            } catch (e: Exception) {
                process.destroyForcibly()
                null
            }
        }
    }
}

/** Un oggetto JSON da coppie, per i parametri dei comandi. */
internal fun jsonOf(vararg pairs: Pair<String, Any?>): JsonObject = JsonObject(pairs.associate { (key, value) -> key to toJson(value) })

private fun toJson(value: Any?): JsonElement = when (value) {
    null -> kotlinx.serialization.json.JsonNull
    is JsonElement -> value
    is String -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is List<*> -> JsonArray(value.map(::toJson))
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJson(v) })
    else -> JsonPrimitive(value.toString())
}
