package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.data.LinkPreviews
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/**
 * **Il titolo di una pagina web, svelto** (13/11/2026, l'utente: "ci metta
 * tantissimo ad ottenere il titolo del sito", e la prima volta "No title
 * found"). Un sito finto sul PC: un indirizzo vecchio che rimanda alla
 * pagina, e una pagina che manda subito la `<head>` e il resto dopo cinque
 * secondi. Il titolo arriva subito, senza aspettare il resto.
 */
class DesktopLinkPreviewsTest {

    @Test
    fun theTitleArrivesWithoutWaitingForTheRestAndRedirectsAreFollowed() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/vecchio") { exchange ->
            exchange.responseHeaders.add("Location", "/pagina")
            exchange.sendResponseHeaders(301, -1)
            exchange.close()
        }
        server.createContext("/pagina") { exchange ->
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, 0)
            val out = exchange.responseBody
            out.write("<html><head><title>Una pagina lenta</title></head>".toByteArray())
            out.flush()
            // Il resto della pagina (spesso centinaia di KB) arriva tardi.
            Thread.sleep(5_000)
            runCatching {
                out.write("<body>Tanto testo</body></html>".toByteArray())
                out.close()
            }
        }
        server.executor = Executors.newCachedThreadPool()
        server.start()
        try {
            val start = System.currentTimeMillis()
            val preview = runBlocking { LinkPreviews.fetch("http://127.0.0.1:${server.address.port}/vecchio") }
            val took = System.currentTimeMillis() - start
            assertEquals("Una pagina lenta", preview?.title)
            assertTrue("ci ha messo $took ms", took < 2_000)
        } finally {
            server.stop(0)
        }
    }

    /** Una pagina fatta dal JavaScript, col titolo vuoto: almeno il nome del sito. */
    @Test
    fun withoutATitleTheSiteName() {
        val preview = LinkPreviews.parse("""<head><title></title><meta property="og:site_name" content="Il mio sito"></head>""", "https://example.com/")
        assertEquals("Il mio sito", preview.title)
    }
}
