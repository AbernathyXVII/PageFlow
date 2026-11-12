package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.patchnotes.NoteBlock
import com.gabriele.notionlocal.data.patchnotes.PatchKind
import com.gabriele.notionlocal.data.patchnotes.PatchNotes
import com.gabriele.notionlocal.data.patchnotes.PatchNotesAsset
import com.gabriele.notionlocal.data.patchnotes.PatchVersion
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.DarkSurfaceVariant
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionGray900
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.platform.LocalPlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **Settings → Patch Notes** (dal 30/09/2026, chiesto dall'utente): tutte
 * le versioni dell'app, con data e ora, **come sono scritte nella
 * Cronologia del README** — lo stesso testo, copiato a ogni compilazione
 * (vedi `PatchNotesAsset`).
 *
 * **Qui ci sono solo i titoli** (dal 30/09/2026, chiesto dall'utente: tutto
 * in una pagina sola era lunghissimo): l'emoji, il numero, il giorno e
 * l'ora, e il titolo. Toccandone uno si apre la sua pagina
 * (`PatchNoteScreen`) con quello che c'è scritto sotto. In cima resta
 * l'introduzione della Cronologia, che spiega i numeri.
 *
 * Di suo la più recente sta in cima; il pulsante in alto a destra le mette
 * dalla più vecchia, e viceversa.
 */
@Composable
fun PatchNotesScreen(onBack: () -> Unit, onOpenVersion: (String) -> Unit) {
    val notes = rememberPatchNotes()
    var oldestFirst by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Cambiato l'ordine si riparte dalla cima; tornando da una versione,
    // invece, l'elenco resta dov'era.
    var shownOrder by rememberSaveable { mutableStateOf(oldestFirst) }
    LaunchedEffect(oldestFirst) {
        if (oldestFirst != shownOrder) {
            listState.scrollToItem(0)
            shownOrder = oldestFirst
        }
    }

    Scaffold(
        topBar = {
            WorkspaceTopBar(title = Strings.patchNotes, onBack = onBack) {
                TextButton(onClick = { oldestFirst = !oldestFirst }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = null, tint = NotionWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (oldestFirst) Strings.oldestFirst else Strings.newestFirst,
                        color = NotionWhite,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        val loaded = notes
        if (loaded == null) {
            Loading(Modifier.padding(padding))
            return@Scaffold
        }
        val versions = if (oldestFirst) loaded.versions.asReversed() else loaded.versions
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            itemsIndexed(loaded.intro, key = { index, _ -> "intro-$index" }) { _, block ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) { NoteBlockView(block) }
            }
            item(key = "intro-end") { Spacer(modifier = Modifier.height(12.dp)) }
            itemsIndexed(versions, key = { _, version -> "v${version.version}" }) { _, version ->
                HorizontalDivider(color = NotionGray900)
                VersionRow(version, onClick = { onOpenVersion(version.version) })
            }
        }
    }
}

/**
 * **La pagina di una versione** (dal 30/09/2026): il suo titolo come
 * nell'elenco, e sotto quello che c'è scritto nella Cronologia — la
 * richiesta dell'utente, le modifiche con le etichette, le verifiche.
 */
@Composable
fun PatchNoteScreen(version: String, onBack: () -> Unit) {
    val notes = rememberPatchNotes()
    Scaffold(
        topBar = { WorkspaceTopBar(title = "v$version", onBack = onBack) },
        containerColor = DarkBackground
    ) { padding ->
        val loaded = notes
        if (loaded == null) {
            Loading(Modifier.padding(padding))
            return@Scaffold
        }
        val shown = loaded.versions.find { it.version == version }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)
        ) {
            if (shown != null) {
                item(key = "head") { VersionHeader(shown) }
                itemsIndexed(shown.blocks, key = { index, _ -> "block-$index" }) { _, block ->
                    NoteBlockView(block)
                }
            }
        }
    }
}

/** Le Patch Notes, lette una volta sola (vedi `PatchNotesAsset.load`); null finché non ci sono. */
@Composable
private fun rememberPatchNotes(): PatchNotes? {
    val context = LocalPlatformContext.current
    val notes by produceState(PatchNotesAsset.loaded, context) {
        value = withContext(Dispatchers.IO) { PatchNotesAsset.load(context) }
    }
    return notes
}

@Composable
private fun Loading(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Una versione nell'elenco: l'emoji, il numero, il giorno e l'ora, il titolo e la freccia per aprirla. */
@Composable
private fun VersionRow(version: PatchVersion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            VersionLine(version)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = inlineMarkdown(version.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = NotionWhite
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = NotionGray400)
    }
}

/** L'emoji, il numero (azzurro per le versioni nuove) e il giorno con l'ora. */
@Composable
private fun VersionLine(version: PatchVersion) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(version.emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "v${version.version}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (version.kind == PatchKind.RELEASE) Color.White else NotionWhite,
            modifier = Modifier
                .background(
                    if (version.kind == PatchKind.RELEASE) AccentBlue else DarkSurfaceVariant,
                    RoundedCornerShape(percent = 50)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${version.date} · ${version.time}",
            style = MaterialTheme.typography.labelLarge,
            color = NotionGray400
        )
    }
}

/** Il titolo in cima alla pagina di una versione: l'emoji, il numero, il giorno e l'ora, e il titolo. */
@Composable
private fun VersionHeader(version: PatchVersion) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)) {
        VersionLine(version)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = inlineMarkdown(version.title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = NotionWhite
        )
        HorizontalDivider(color = NotionGray900, modifier = Modifier.padding(top = 14.dp))
    }
}

/** Un pezzo della Cronologia: paragrafo, voce di un elenco, citazione o tabella. */
@Composable
private fun NoteBlockView(block: NoteBlock) {
    val body = MaterialTheme.typography.bodyMedium
    when (block) {
        is NoteBlock.Paragraph -> Text(
            text = inlineMarkdown(block.text),
            style = body,
            color = NotionWhite,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        is NoteBlock.Bullet -> Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp * block.level, top = 3.dp, bottom = 3.dp)
        ) {
            Text(
                text = block.marker ?: if (block.level == 0) "•" else "◦",
                style = body,
                color = NotionGray400,
                modifier = Modifier.width(if (block.marker != null) 24.dp else 16.dp)
            )
            Text(text = inlineMarkdown(block.text), style = body, color = NotionWhite, modifier = Modifier.weight(1f))
        }

        is NoteBlock.Quote -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(NotionGray400.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
            Text(
                text = inlineMarkdown(block.text),
                style = body,
                color = NotionGray400,
                modifier = Modifier.padding(start = 12.dp).weight(1f)
            )
        }

        is NoteBlock.Table -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp * block.level, top = 6.dp, bottom = 6.dp)
                .border(1.dp, NotionGray900, RoundedCornerShape(4.dp))
        ) {
            val columns = block.rows.maxOfOrNull { it.size } ?: 0
            block.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = NotionGray900)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .then(if (index == 0) Modifier.background(DarkSurface) else Modifier),
                    horizontalArrangement = Arrangement.Start
                ) {
                    (0 until columns).forEach { column ->
                        Text(
                            text = inlineMarkdown(row.getOrElse(column) { "" }),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (index == 0) FontWeight.SemiBold else null,
                            color = NotionWhite,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grassetto (`**`), corsivo (`*`), barrato (`~~`) e codice (`` ` ``) dentro
 * una riga, come li mostra GitHub. Un `*` fra due spazi resta un asterisco.
 * L'etichetta [REVIEW_TAG] è rossa (vedi lì).
 */
fun inlineMarkdown(text: String, codeBackground: Color = CODE_BACKGROUND): AnnotatedString = buildAnnotatedString {
    var bold = false
    var italic = false
    var strike = false
    val pending = StringBuilder()

    fun style() = SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = if (strike) TextDecoration.LineThrough else null
    )

    fun flush() {
        if (pending.isEmpty()) return
        withStyle(style()) { append(pending.toString()) }
        pending.clear()
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            text.startsWith(REVIEW_TAG, i) -> {
                flush()
                withStyle(style().merge(SpanStyle(color = ReviewRed))) { append(REVIEW_TAG) }
                i += REVIEW_TAG.length
            }
            text.startsWith("**", i) -> {
                flush()
                bold = !bold
                i += 2
            }
            text.startsWith("~~", i) -> {
                flush()
                strike = !strike
                i += 2
            }
            c == '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close < 0) {
                    pending.append(c)
                    i++
                } else {
                    flush()
                    withStyle(style().merge(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground))) {
                        append(text, i + 1, close)
                    }
                    i = close + 1
                }
            }
            c == '*' && !(text.getOrNull(i - 1)?.isWhitespace() != false && text.getOrNull(i + 1)?.isWhitespace() != false) -> {
                flush()
                italic = !italic
                i++
            }
            c == '\\' && i + 1 < text.length && text[i + 1] in "*`~_\\|" -> {
                pending.append(text[i + 1])
                i += 2
            }
            else -> {
                pending.append(c)
                i++
            }
        }
    }
    flush()
}

private val CODE_BACKGROUND = Color(0x33878787)

/**
 * **L'etichetta rossa delle scelte da rivedere** (dal 18/10/2026, chiesta
 * dall'utente): una cosa fatta in un modo fra due, che l'utente vuole
 * ritrovare per decidere se tenerla o passare all'altra, scritta accanto. Nel
 * README ha davanti 🔴, perché su GitHub il testo non si colora.
 */
// In inglese dal 12/11/2026, come tutto il README.
const val REVIEW_TAG = "[To review]"
private val ReviewRed = Color(0xFFE5484D)
