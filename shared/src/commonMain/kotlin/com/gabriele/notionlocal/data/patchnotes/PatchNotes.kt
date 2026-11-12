package com.gabriele.notionlocal.data.patchnotes

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **Le Patch Notes** (dal 30/09/2026, chiesto dall'utente): la Cronologia
 * degli aggiornamenti del README, **la stessa**, dentro l'app (Settings →
 * Patch Notes).
 *
 * Non ne esiste una copia da tenere allineata a mano: a ogni compilazione
 * il build copia la sezione del README fra gli asset dell'app
 * (`patch_notes.md`, vedi `app/build.gradle.kts`), e qui la si legge. Così
 * ogni modifica scritta nel README è nell'app appena la si ricompila.
 *
 * Il README è in Markdown, e di Markdown la Cronologia usa poco: i titoli
 * delle versioni, i paragrafi, gli elenchi (anche rientrati), le citazioni
 * e qualche tabella. Qui lo si divide in versioni e ogni versione in
 * blocchi; grassetto, corsivo, barrato e codice dentro le righe li disegna
 * la schermata (`PatchNotesScreen`).
 */
object PatchNotesAsset {
    const val FILE_NAME = "patch_notes.md"

    /**
     * Le Patch Notes già lette, o null. Il file dentro l'app non cambia
     * finché l'app è aperta: si legge una volta, e l'elenco e la pagina di
     * una versione usano la stessa lettura.
     */
    @Volatile
    var loaded: PatchNotes? = null
        private set

    /** Le Patch Notes dell'app; vuote se il file non c'è. Legge un file: non dal thread principale. */
    fun load(context: PlatformContext): PatchNotes {
        loaded?.let { return it }
        val text = runCatching { readPatchNotesFile(context, FILE_NAME) }.getOrNull() ?: return PatchNotes(title = "", intro = emptyList(), versions = emptyList())
        return parsePatchNotes(text).also { loaded = it }
    }
}

/** Un pezzo di testo della Cronologia, come lo disegna la schermata. */
sealed class NoteBlock {
    /** Un paragrafo: le righe spezzate a mano nel README tornano una riga sola. */
    data class Paragraph(val text: String) : NoteBlock()

    /** Una voce di un elenco; `level` 0 in colonna, 1 rientrata. `marker` è "1." per gli elenchi numerati. */
    data class Bullet(val level: Int, val text: String, val marker: String? = null) : NoteBlock()

    /** Una citazione (le richieste dell'utente, parola per parola). */
    data class Quote(val text: String) : NoteBlock()

    /** Una tabella: la prima riga sono i titoli delle colonne. */
    data class Table(val level: Int, val rows: List<List<String>>) : NoteBlock()
}

/**
 * Che cosa porta una versione, dal suo numero (dal 21/10/2026, chiesto
 * dall'utente): 🆕 v0.N un'aggiunta, ✏️ v0.N.M una modifica di qualcosa
 * che c'era già, 🔧 v0.N.M.K solo correzioni di bug.
 */
enum class PatchKind(val emoji: String) {
    RELEASE("🆕"),
    CHANGE("✏️"),
    FIX("🔧");

    companion object {
        /** Dal numero: "0.124" un'aggiunta, "0.123.1" una modifica, "0.123.0.1" una correzione. */
        fun of(version: String): PatchKind = when (version.count { it == '.' }) {
            1 -> RELEASE
            2 -> CHANGE
            else -> FIX
        }
    }
}

/**
 * L'ora di una versione di cui non si sa (le prime, fatte in un'altra chat
 * fra l'8 e il 18/09/2026): si dice, invece di inventarla.
 */
const val UNKNOWN_TIME = "time not recorded"

/** Una versione: "🆕 v0.78 — 30/09/2026 · 15:09 — titolo", e sotto quello che contiene. */
data class PatchVersion(
    val kind: PatchKind,
    val emoji: String,
    /** "0.78", "0.77.1", "0.77.0.1": il numero senza la "v". */
    val version: String,
    /** "30/09/2026", o un periodo ("between 8 and 18/09/2026") quando l'ora non si sa. */
    val date: String,
    /** "15:09", o `UNKNOWN_TIME`. */
    val time: String,
    val title: String,
    val blocks: List<NoteBlock>
)

data class PatchNotes(
    val title: String,
    /** Quello che c'è fra il titolo della sezione e la prima versione. */
    val intro: List<NoteBlock>,
    /** Dalla più recente, come nel README. */
    val versions: List<PatchVersion>
)

/** Il titolo di una versione: "### 🆕 v0.78 — 30/09/2026 · 15:09 — Il time-blocking…". */
val VERSION_HEADING = Regex("""^### (\S+) v(\d+\.\d+(?:\.\d+){0,2}) — (.+?) · (\d{2}:\d{2}|$UNKNOWN_TIME) — (.+)$""")

fun parsePatchNotes(markdown: String): PatchNotes {
    val lines = markdown.removePrefix("﻿").replace("\r\n", "\n").split("\n")
    var title = ""
    val intro = mutableListOf<String>()
    val versions = mutableListOf<PatchVersion>()
    var current: MatchResult? = null
    var body = mutableListOf<String>()

    fun close() {
        val head = current ?: return
        val (emoji, version, date, time, name) = head.destructured
        versions += PatchVersion(
            kind = PatchKind.of(version),
            emoji = emoji,
            version = version,
            date = date,
            time = time,
            title = name,
            blocks = parseBlocks(body)
        )
        body = mutableListOf()
    }

    lines.forEach { line ->
        val heading = VERSION_HEADING.matchEntire(line)
        when {
            heading != null -> {
                close()
                current = heading
            }
            current == null && line.startsWith("## ") -> title = line.removePrefix("## ").trim()
            current == null -> intro += line
            else -> body += line
        }
    }
    close()
    return PatchNotes(title = title, intro = parseBlocks(intro), versions = versions)
}

private val BULLET = Regex("""^( *)[-*] (.*)$""")
private val NUMBERED = Regex("""^( *)(\d+\.) (.*)$""")
private val TABLE_RULE = Regex("""^[\s|:-]+$""")

private fun indentOf(line: String) = line.length - line.trimStart(' ').length

private fun startsBlock(line: String): Boolean {
    val t = line.trimStart()
    return BULLET.matches(line) || NUMBERED.matches(line) || t.startsWith("|") || t.startsWith(">")
}

/**
 * Le righe di una versione in blocchi. Come su GitHub, le righe spezzate a
 * mano si riuniscono: un paragrafo finisce a una riga vuota o dove comincia
 * un elenco, una citazione o una tabella; una voce di un elenco continua
 * sulle righe rientrate sotto di lei.
 */
fun parseBlocks(lines: List<String>): List<NoteBlock> {
    val blocks = mutableListOf<NoteBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.isBlank()) {
            i++
            continue
        }
        val indent = indentOf(line)
        val trimmed = line.trimStart()
        val bullet = BULLET.matchEntire(line)
        val numbered = NUMBERED.matchEntire(line)
        when {
            trimmed.startsWith("|") -> {
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                    val row = lines[i].trim()
                    if (!TABLE_RULE.matches(row)) {
                        rows += row.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                    }
                    i++
                }
                blocks += NoteBlock.Table(level = indent / 2, rows = rows)
            }

            trimmed.startsWith(">") -> {
                // Una riga ">" vuota separa due paragrafi della stessa citazione.
                val paragraphs = mutableListOf(StringBuilder())
                while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                    val part = lines[i].trimStart().removePrefix(">").trim()
                    if (part.isEmpty()) {
                        paragraphs += StringBuilder()
                    } else {
                        val current = paragraphs.last()
                        if (current.isNotEmpty()) current.append(' ')
                        current.append(part)
                    }
                    i++
                }
                blocks += NoteBlock.Quote(paragraphs.filter { it.isNotEmpty() }.joinToString("\n"))
            }

            bullet != null || numbered != null -> {
                val text = StringBuilder(bullet?.groupValues?.get(2) ?: numbered!!.groupValues[3])
                i++
                while (i < lines.size && lines[i].isNotBlank() && indentOf(lines[i]) > indent && !startsBlock(lines[i])) {
                    text.append(' ').append(lines[i].trim())
                    i++
                }
                blocks += NoteBlock.Bullet(
                    level = (indent / 2).coerceAtMost(3),
                    text = text.toString(),
                    marker = numbered?.groupValues?.get(2)
                )
            }

            else -> {
                val text = StringBuilder(trimmed)
                i++
                while (i < lines.size && lines[i].isNotBlank() && !startsBlock(lines[i])) {
                    text.append(' ').append(lines[i].trim())
                    i++
                }
                blocks += NoteBlock.Paragraph(text.toString())
            }
        }
    }
    return blocks
}
