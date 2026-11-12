package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.withEmojiFont
import com.gabriele.notionlocal.platform.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.gabriele.notionlocal.data.entity.RichTextSpan
import com.gabriele.notionlocal.data.proofing.SpellChecker
import com.gabriele.notionlocal.ui.i18n.ProofingStrings
import com.gabriele.notionlocal.ui.theme.NotionGray400
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.gabriele.notionlocal.platform.isKeyboardVisible

/**
 * **Il correttore nelle pagine** (dal 08/10/2026), come quello di
 * OneNote: le parole che il dizionario non conosce hanno sotto **una riga
 * rossa ondulata**, e toccandone una si apre il suo menu — fino a cinque
 * parole al posto suo, "Add to dictionary", "Ignore".
 *
 * Il correttore della pagina aperta arriva dall'alto, da [LocalProofing]:
 * `null` quando è spento, che è il caso di partenza. I campi di testo
 * della pagina fanno tre cose:
 *  1. chiedono le parole sbagliate delle loro righe a
 *     [rememberMisspellings], che le cerca fuori dal filo principale e
 *     aspetta che si smetta di scrivere;
 *  2. le segnano nel testo mostrato ([markMisspellings]) con
 *     un'annotazione: viaggia col testo fin dentro il `TextLayoutResult`,
 *     quindi chi disegna sa dove stanno senza rifare i conti delle
 *     posizioni (pallini, numeri, a-capo nascosto);
 *  3. le disegnano ([drawMisspellings]) e ascoltano i tocchi
 *     ([proofingTaps]), che non tolgono niente al campo: il cursore si
 *     posa comunque dove si è toccato.
 */
class PageProofing(
    val checker: SpellChecker,
    val addToDictionary: (String) -> Unit,
    val ignore: (String) -> Unit
)

val LocalProofing = compositionLocalOf<PageProofing?> { null }

const val PROOFING_TAG = "proofing"

/** Il rosso della riga ondulata, uguale coi due temi. */
private val SquiggleColor = Color(0xFFE5534B)

/** L'altezza di una riga del menu di una parola: vedi `ProofingMenu`. */
private val MENU_ROW_HEIGHT = 36.dp

/** Dopo quanto che non si scrive le parole nuove si ricontrollano. */
private const val PROOFING_DELAY_MS = 400L

/**
 * Le parole sbagliate di ogni riga di [lines], come posizioni dentro la
 * riga. Una riga si ricontrolla solo quando cambia, e dopo
 * [PROOFING_DELAY_MS] che non si scrive: la riga appena cambiata resta
 * senza segni intanto, ed è giusto così — la parola che si sta scrivendo
 * non è sbagliata, è a metà.
 */
@Composable
fun rememberMisspellings(lines: List<String>): Map<String, List<IntRange>> {
    val proofing = LocalProofing.current
    val checker = proofing?.checker
    var results by remember { mutableStateOf<Pair<SpellChecker?, Map<String, List<IntRange>>>>(null to emptyMap()) }
    LaunchedEffect(lines, checker) {
        if (checker == null) {
            results = null to emptyMap()
            return@LaunchedEffect
        }
        val known = if (results.first === checker) results.second else emptyMap()
        val missing = lines.any { it !in known }
        if (!missing) return@LaunchedEffect
        if (known.isNotEmpty()) delay(PROOFING_DELAY_MS)
        val computed = withContext(Dispatchers.Default) {
            lines.distinct().associateWith { known[it] ?: checker.misspelled(it) }
        }
        results = checker to computed
    }
    // Cambiato il correttore (una parola aggiunta o ignorata, un'altra
    // lingua) i segni di prima restano finché non arrivano i nuovi: un
    // attimo, ma senza farli sparire e ricomparire tutti.
    return if (checker != null) results.second else emptyMap()
}

/**
 * Segna in [this] le parole sbagliate [ranges] della riga [line], che nel
 * testo mostrato comincia a [at]. Non segna quelle dentro uno spoiler
 * (coperte non si leggono, e la riga direbbe che c'è una parola) e
 * quella appena scritta, che finisce dove sta il cursore ([typingEnd],
 * in posizioni della riga).
 */
fun AnnotatedString.Builder.markMisspellings(
    line: Int,
    ranges: List<IntRange>,
    at: Int,
    spans: List<RichTextSpan>?,
    typingEnd: Int? = null
) {
    if (ranges.isEmpty()) return
    val spoilers = ArrayList<IntRange>()
    if (spans != null) {
        var pos = 0
        for (span in spans) {
            if (span.spoiler && span.text.isNotEmpty()) spoilers += pos until pos + span.text.length
            pos += span.text.length
        }
    }
    for (r in ranges) {
        if (typingEnd != null && typingEnd in r.first..r.last + 1) continue
        if (spoilers.any { it.first <= r.last && r.first <= it.last }) continue
        if (at + r.last + 1 > length) continue
        addStringAnnotation(PROOFING_TAG, "$line:${r.first}:${r.last + 1}", at + r.first, at + r.last + 1)
    }
}

/**
 * Per un campo che non ha già una sua trasformazione (il titolo): il testo
 * resta quello, con in più i segni delle parole sbagliate.
 */
class ProofingTransformation(
    private val ranges: List<IntRange>,
    private val typingEnd: Int?
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (ranges.isEmpty()) return TransformedText(text.withEmojiFont(), OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text)
        builder.markMisspellings(0, ranges, 0, null, typingEnd)
        return TransformedText(builder.toAnnotatedString().withEmojiFont(), OffsetMapping.Identity)
    }
}

/** Una parola sottolineata: la riga del campo e le sue posizioni dentro la riga. */
data class MisspelledWord(val line: Int, val start: Int, val end: Int) {
    companion object {
        fun parse(tag: String): MisspelledWord? {
            val p = tag.split(':')
            if (p.size != 3) return null
            return MisspelledWord(p[0].toIntOrNull() ?: return null, p[1].toIntOrNull() ?: return null, p[2].toIntOrNull() ?: return null)
        }
    }
}

/** La riga rossa ondulata sotto ogni parola segnata nel testo di [layout]. */
fun DrawScope.drawMisspellings(layout: TextLayoutResult?) {
    layout ?: return
    val text = layout.layoutInput.text
    val marks = text.getStringAnnotations(PROOFING_TAG, 0, text.length)
    if (marks.isEmpty()) return
    val half = 1.3.dp.toPx()
    val step = 2.dp.toPx()
    // Sotto le gambe di "q" e "p", non sopra.
    val drop = 4.dp.toPx()
    val stroke = Stroke(width = 1.2.dp.toPx())
    // Dove sta la base delle lettere dentro una riga: le righe dei campi
    // della pagina sono tutte alte uguale, quindi basta la prima.
    val baselineInLine = layout.firstBaseline - layout.getLineTop(0)
    for (m in marks) {
        val start = m.start.coerceIn(0, text.length)
        val end = m.end.coerceIn(start, text.length)
        if (start >= end) continue
        val firstLine = layout.getLineForOffset(start)
        val lastLine = layout.getLineForOffset(end - 1)
        for (line in firstLine..lastLine) {
            val from = maxOf(start, layout.getLineStart(line))
            val to = minOf(end, layout.getLineEnd(line, visibleEnd = true))
            if (from >= to) continue
            val left = layout.getBoundingBox(from).left
            val right = layout.getBoundingBox(to - 1).right
            val baseline = layout.getLineTop(line) + baselineInLine + drop
            val path = Path()
            var x = minOf(left, right)
            val stop = maxOf(left, right)
            var up = true
            path.moveTo(x, baseline)
            while (x < stop) {
                x = minOf(x + step, stop)
                path.lineTo(x, baseline + if (up) -half else half)
                up = !up
            }
            drawPath(path, SquiggleColor, style = stroke)
        }
    }
}

/**
 * Ascolta i tocchi brevi su una parola segnata, senza prenderli: il campo
 * sotto li riceve lo stesso e ci mette il cursore. [onWord] riceve la
 * parola e il riquadro dove sta, a cui attaccare il menu.
 */
fun Modifier.proofingTaps(
    enabled: Boolean,
    layout: () -> TextLayoutResult?,
    onWord: (MisspelledWord, IntRect) -> Unit
): Modifier = if (!enabled) this else pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation(PointerEventPass.Initial)
        } ?: return@awaitEachGesture
        if ((up.position - down.position).getDistance() > viewConfiguration.touchSlop) return@awaitEachGesture
        val l = layout() ?: return@awaitEachGesture
        val offset = l.getOffsetForPosition(up.position)
        val text = l.layoutInput.text
        val mark = text.getStringAnnotations(PROOFING_TAG, 0, text.length)
            .firstOrNull { offset >= it.start && offset <= it.end } ?: return@awaitEachGesture
        val word = MisspelledWord.parse(mark.item) ?: return@awaitEachGesture
        val last = (text.length - 1).coerceAtLeast(0)
        val line = l.getLineForOffset(mark.start)
        val left = l.getBoundingBox(mark.start.coerceIn(0, last)).left
        val end = l.getBoundingBox((mark.end - 1).coerceIn(0, last))
        val right = if (l.getLineForOffset(mark.end - 1) == line) end.right else left + 1f
        onWord(
            word,
            IntRect(left.roundToInt(), l.getLineTop(line).roundToInt(), right.roundToInt(), l.getLineBottom(line).roundToInt())
        )
    }
}

/**
 * Il menu di una parola sottolineata: fino a cinque parole al suo posto
 * (le cerca fuori dal filo principale: per una parola molto storpiata ci
 * può volere un attimo), poi "Add to dictionary" e "Ignore".
 * Va messo dentro un `Box` che abbia lo stesso angolo in alto a sinistra
 * del testo: [anchor] è il riquadro della parola in quelle coordinate. Il
 * menu si apre sotto la parola o, se sotto non c'è posto, sopra: mai
 * sopra la parola stessa.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProofingMenu(
    word: String,
    anchor: IntRect,
    onReplace: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val proofing = LocalProofing.current ?: return
    val suggestions by produceState<List<String>?>(null, word, proofing.checker) {
        value = withContext(Dispatchers.Default) { proofing.checker.suggestions(word) }
    }
    val density = LocalDensity.current
    // **Compatto**, perché con la tastiera aperta deve stare sopra o sotto
    // la parola senza coprirla: righe più basse di quelle di serie, e
    // quattro parole invece di cinque finché la tastiera c'è.
    val shown = if (isKeyboardVisible()) 4 else 5
    val row = Modifier.height(MENU_ROW_HEIGHT)
    // Il menu non prende il fuoco, quindi "Indietro" arriverebbe alla
    // pagina: lo chiude, invece (dopo la tastiera, che se ne va per prima).
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .offset { IntOffset(anchor.left, anchor.top) }
            .size(with(density) { anchor.width.toDp() }, with(density) { anchor.height.toDp() })
    ) {
        // **Senza prendere il fuoco**: il cursore resta nel testo e la
        // tastiera non si chiude, come nel menu di OneNote.
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false)
        ) {
            val list = suggestions
            when {
                list == null -> DropdownMenuItem(
                    text = { Text("…", color = NotionGray400) },
                    onClick = {},
                    enabled = false,
                    modifier = row
                )
                list.isEmpty() -> DropdownMenuItem(
                    text = { Text(ProofingStrings.noSuggestions, color = NotionGray400) },
                    onClick = {},
                    enabled = false,
                    modifier = row
                )
                else -> list.take(shown).forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s, fontWeight = FontWeight.SemiBold) },
                        modifier = row,
                        onClick = {
                            onDismiss()
                            onReplace(s)
                        }
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(ProofingStrings.addToDictionary) },
                leadingIcon = { Icon(Icons.Filled.LibraryAdd, contentDescription = null) },
                modifier = row,
                onClick = {
                    onDismiss()
                    proofing.addToDictionary(word)
                }
            )
            DropdownMenuItem(
                text = { Text(ProofingStrings.ignore) },
                leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                modifier = row,
                onClick = {
                    onDismiss()
                    proofing.ignore(word)
                }
            )
        }
    }
}
