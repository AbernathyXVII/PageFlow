package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.data.entity.LinkRange

/** Niente link che eseguono codice, né vuoti. */
fun usableLink(url: String): Boolean = url.isNotBlank() && !url.trim().startsWith("javascript:", ignoreCase = true)

/**
 * **Un indirizzo copiato da solo si incolla come link** (dal 18/11/2026, chiesto dall'utente: "se
 * copio un link… lo incollo in una pagina, incolla solo plain text"): dalla barra del browser negli
 * appunti c'è solo il testo, senza il link dell'HTML. Se [text] è tutto e solo un indirizzo web
 * (http://, https:// o www.), il link che lo copre; se no null.
 */
fun bareWebLink(text: String): LinkRange? {
    val url = text.trim()
    if (url.isEmpty() || url.any { it.isWhitespace() }) return null
    val href = when {
        (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) && url.substringAfter("//").isNotEmpty() -> url
        url.startsWith("www.", ignoreCase = true) && '.' in url.drop(4) -> "https://$url"
        else -> return null
    }
    val start = text.indexOf(url)
    return LinkRange(start, start + url.length, href)
}

fun decodeEntities(text: String): String =
    Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(text) { m ->
        val code = m.groupValues[1]
        when {
            code.startsWith("#x") || code.startsWith("#X") -> code.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
            code.startsWith("#") -> code.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
            else -> when (code) {
                "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> "\u00A0"
                else -> null
            }
        } ?: m.value
    }
