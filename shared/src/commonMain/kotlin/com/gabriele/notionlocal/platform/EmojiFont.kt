package com.gabriele.notionlocal.platform

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily

/**
 * **Il carattere delle emoji**: sul PC quello di Google (dal 12/11/2026, vedi
 * `PcEmoji`), sul telefono `null`, cioè quello del sistema. Va chiesto dove
 * un'emoji si disegna da sola (icone, selettore) e per quelle scritte col
 * selettore "emoji" U+FE0F (☺️, ♥️, 1️⃣): Windows ha anche la loro versione
 * in lettere, bianca, e la sceglierebbe prima di Noto.
 */
expect val emojiFontFamily: FontFamily?

/** Il testo con le emoji scritte col U+FE0F nel carattere delle emoji, se ce n'è uno. */
fun AnnotatedString.withEmojiFont(): AnnotatedString {
    val family = emojiFontFamily ?: return this
    val ranges = emojiPresentationRanges(text)
    if (ranges.isEmpty()) return this
    return buildAnnotatedString {
        append(this@withEmojiFont)
        ranges.forEach { addStyle(SpanStyle(fontFamily = family), it.first, it.last + 1) }
    }
}

/**
 * Dove stanno le emoji scritte col U+FE0F, sequenza intera: "1️⃣" col suo
 * tasto, "🏴‍☠️" e "👁️‍🗨️" con tutti i pezzi uniti dal U+200D. Una sequenza
 * divisa a metà fra due stili si spezzerebbe in due disegni.
 */
fun emojiPresentationRanges(text: String): List<IntRange> {
    fun cpStartBefore(end: Int) = if (end >= 2 && text[end - 1].isLowSurrogate() && text[end - 2].isHighSurrogate()) end - 2 else end - 1
    fun cpEndAfter(start: Int) = if (start + 1 < text.length && text[start].isHighSurrogate() && text[start + 1].isLowSurrogate()) start + 2 else start + 1
    // I toni della pelle: U+1F3FB…U+1F3FF, cioè 🏻…🏿.
    fun isTone(at: Int) = at + 1 < text.length && text[at] == '\uD83C' && text[at + 1] in '\uDFFB'..'\uDFFF'
    fun joinsPrevious(at: Int) = text[at] == '‍' || text[at] == '️' || text[at] == '⃣' || isTone(at)

    val ranges = mutableListOf<IntRange>()
    var i = text.indexOf('️')
    while (i >= 0) {
        if (i == 0) {
            i = text.indexOf('️', 1)
            continue
        }
        var start = cpStartBefore(i)
        while (start > 0) {
            val before = cpStartBefore(start)
            if (text[before] == '‍' || joinsPrevious(start)) start = before else break
        }
        var end = i + 1
        while (end < text.length) {
            end = when {
                text[end] == '️' || text[end] == '⃣' -> end + 1
                isTone(end) -> end + 2
                text[end] == '‍' && end + 1 < text.length -> cpEndAfter(end + 1)
                else -> break
            }
        }
        val last = ranges.lastOrNull()
        if (last != null && start <= last.last + 1) ranges[ranges.size - 1] = last.first until end else ranges += start until end
        i = text.indexOf('️', end)
    }
    return ranges
}
