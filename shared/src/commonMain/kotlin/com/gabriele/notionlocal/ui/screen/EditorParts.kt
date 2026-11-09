package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.ui.i18n.EditorStrings
import androidx.compose.foundation.Image
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.ui.theme.label
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.DatabaseLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.outlined.Description
import com.gabriele.notionlocal.ui.theme.NotionWhite
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import com.gabriele.notionlocal.platform.rememberImagePicker
import com.gabriele.notionlocal.data.toImageBitmap

/** Quale delle due immagini di una pagina si sta cambiando. */
enum class PageImageTarget { ICON, COVER }

/**
 * Scegliere l'immagine di un'icona o di una copertina: dalla galleria
 * del telefono ("Upload"), e basta.
 *
 * Fino al 25/09/2026 c'era anche un campo per il link di un'immagine da
 * scaricare: tolto su richiesta dell'utente, che le immagini le carica
 * dal telefono. (Il permesso di andare in rete, andato via con lui, è
 * tornato il 06/10/2026 per i video da link e i segnalibri: qui resta
 * solo il caricamento dal telefono.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageImageSheet(
    target: PageImageTarget,
    /** Il titolo della finestra, se non è "Icon" o "Cover" (la proprietà "Image"). */
    title: String? = null,
    hasImage: Boolean,
    store: PageImageStore,
    onPicked: (String?) -> String?,
    /** Null per l'icona: lì non c'è niente da inquadrare, è un quadratino. */
    onReposition: (() -> Unit)?,
    onDismiss: () -> Unit,
    /** "View full screen", in cima quando c'è già un'immagine (la proprietà "Image"); null = niente. */
    onView: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Quando il nuovo file prende il posto del vecchio, il vecchio va
    // cancellato: resterebbe nella cartella per sempre senza che
    // nessuno possa più arrivarci.
    fun apply(fileName: String?) {
        val replaced = onPicked(fileName)
        scope.launch { store.delete(replaced) }
        onDismiss()
    }

    // Il selettore di foto di sistema: non chiede il permesso di leggere
    // tutta la galleria, l'utente sceglie una foto e l'app vede solo quella
    // (vedi `rememberImagePicker`; sul PC la scelta di un file).
    val pickImage = rememberImagePicker(
        store = store,
        onStart = {
            busy = true
            error = null
        }
    ) { result ->
        busy = false
        result.fold(
            onSuccess = { apply(it) },
            onFailure = { error = EditorStrings.imageReadFailed }
        )
    }

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = title ?: if (target == PageImageTarget.ICON) EditorStrings.icon else EditorStrings.cover,
                style = MaterialTheme.typography.titleMedium,
                color = NotionWhite,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // **Vederla o cambiarla** (dal 29/09/2026): toccando l'immagine di
            // una cella "Image" si sceglie qui — in grande, o "Upload" per
            // metterne un'altra.
            if (hasImage && onView != null) {
                Text(
                    text = EditorStrings.viewFullScreen,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NotionWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) {
                            onDismiss()
                            onView()
                        }
                        .padding(vertical = 14.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
            }

            PageImageAction(
                label = EditorStrings.upload,
                enabled = !busy,
                onClick = { pickImage() }
            )

            val message = error
            if (message != null) {
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (hasImage && onReposition != null) {
                Spacer(modifier = Modifier.size(20.dp))
                Text(
                    text = EditorStrings.reposition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NotionWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) {
                            onReposition()
                            onDismiss()
                        }
                        .padding(vertical = 14.dp)
                )
            }

            if (hasImage) {
                Spacer(modifier = Modifier.size(20.dp))
                Text(
                    text = EditorStrings.remove,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !busy) { apply(null) }
                        .padding(vertical = 14.dp)
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Quanto si può ingrandire una copertina, e quanto più fine la si
 * legge quando è ingrandita.
 *
 * `MAX_COVER_DETAIL` moltiplica la larghezza da chiedere al disco: a
 * ingrandimento 2 la striscia mostra metà immagine su tutta la sua
 * larghezza, e leggerla alla misura della striscia la farebbe vedere
 * sgranata. Il tetto vero lo mette comunque `PageImageStore`, che non
 * porta in memoria più di sei megapixel.
 */
const val MAX_COVER_ZOOM = 4f

internal const val MAX_COVER_DETAIL = 2

/**
 * La copertina, disegnata a mano invece che con `ContentScale.Crop`.
 *
 * Crop taglia l'immagine e tiene solo il riquadro; ingrandire dopo
 * quel risultato ingrandisce il ritaglio, e i lati già tagliati non
 * tornano più — spostarsi a destra e a sinistra non mostrerebbe
 * niente di nuovo. Qui l'immagine viene messa **più grande della
 * striscia** (la stessa misura che userebbe Crop, moltiplicata per
 * l'ingrandimento) e spostata dentro, con la striscia che ritaglia.
 * A ingrandimento 1 e senza spostamento si vede esattamente quello
 * che si vedeva prima.
 *
 * **Il file non viene mai toccato**: la risoluzione resta quella
 * originale, quello che cambia è soltanto cosa se ne guarda.
 *
 * `clampOffset` serve a chi mostra la stessa inquadratura in un
 * riquadro di **forma diversa** dalla striscia della pagina — le schede
 * della galleria. Lo spostamento è salvato in frazioni della striscia,
 * e in un riquadro più alto o più stretto la stessa frazione può
 * portare l'immagine oltre il bordo e scoprire un angolo vuoto: acceso,
 * lo spostamento si ferma dove l'immagine smette di coprire. Nella
 * pagina resta spento, perché lì il limite lo mette già il dito mentre
 * la si sposta, e la striscia è quella su cui l'inquadratura è nata.
 */
@Composable
fun CoverImage(
    fileName: String,
    store: PageImageStore,
    scale: Float,
    offsetFraction: Offset,
    onImageSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
    clampOffset: Boolean = false
) {
    var box by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
    val detail = ceil(scale).toInt().coerceIn(1, MAX_COVER_DETAIL)

    LaunchedEffect(fileName, box.width, detail) {
        if (box.width <= 0) return@LaunchedEffect
        val loaded = store.load(fileName, box.width * detail)?.toImageBitmap() ?: return@LaunchedEffect
        bitmap = loaded
        onImageSize(IntSize(loaded.width, loaded.height))
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { box = it },
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image == null) {
            // Finché l'immagine si carica, il posto resta suo: senza,
            // il titolo salterebbe su e giù ad ogni apertura.
            Box(modifier = Modifier.matchParentSize().background(DarkSurface))
        } else {
            val density = LocalDensity.current
            val fit = coverFitFactor(box, image.width, image.height) * scale
            val drawnWidth = image.width * fit
            val drawnHeight = image.height * fit
            val limit = if (clampOffset) {
                coverPanLimit(box, image.width, image.height, scale)
            } else {
                null
            }
            Image(
                bitmap = image,
                contentDescription = EditorStrings.cover,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .requiredSize(
                        with(density) { drawnWidth.toDp() },
                        with(density) { drawnHeight.toDp() }
                    )
                    .offset {
                        var x = offsetFraction.x * box.width
                        var y = offsetFraction.y * box.height
                        if (limit != null) {
                            x = x.coerceIn(-limit.x, limit.x)
                            y = y.coerceIn(-limit.y, limit.y)
                        }
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
            )
        }
    }
}

/**
 * Di quanto va moltiplicata l'immagine perché copra tutta la striscia
 * senza lasciare bordi vuoti: è quello che fa `ContentScale.Crop`.
 */
internal fun coverFitFactor(box: IntSize, imageWidth: Int, imageHeight: Int): Float {
    if (box.width <= 0 || box.height <= 0 || imageWidth <= 0 || imageHeight <= 0) return 1f
    return max(
        box.width.toFloat() / imageWidth,
        box.height.toFloat() / imageHeight
    )
}

/**
 * Di quanti pixel si può spostare la copertina prima di scoprire il
 * bordo. Senza questo limite si può trascinare l'immagine fuori dalla
 * striscia e restare a guardare un rettangolo vuoto.
 */
fun coverPanLimit(
    box: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    scale: Float
): Offset {
    if (imageWidth <= 0 || imageHeight <= 0) return Offset.Zero
    val fit = coverFitFactor(box, imageWidth, imageHeight) * scale
    return Offset(
        x = max(0f, (imageWidth * fit - box.width) / 2f),
        y = max(0f, (imageHeight * fit - box.height) / 2f)
    )
}

/** Le tre famiglie del menu "/", nell'ordine in cui si vedono. */
enum class SlashCategory(val title: String) {
    BASIC("Basic blocks"),
    MEDIA("Media"),
    DATABASE("Database")
}

/**
 * Cosa fa una voce del menu "/" oltre a cambiare il tipo di blocco.
 * Le viste dei database creano un database dentro la pagina e gli
 * mettono subito la vista giusta, che è l'unica differenza fra loro.
 */
sealed class SlashAction {
    data class Type(val type: BlockType) : SlashAction()
    object PageLink : SlashAction()
    object Divider : SlashAction()
    /** `simple`: un database semplice, le cui righe non diventano pagine. */
    data class Database(val layout: DatabaseLayout?, val simple: Boolean = false) : SlashAction()
    /**
     * Una **vista collegata** di un database che c'è già: prima si sceglie
     * quale, da un elenco (vedi `LinkedSourcePickerSheet`).
     */
    object LinkedView : SlashAction()
    /**
     * Un'immagine, un video, un audio, un file o un segnalibro (dal
     * 06/10/2026): la riga diventa il blocco ancora vuoto, e si apre subito
     * la scelta del file o del link.
     */
    data class Media(val type: BlockType) : SlashAction()
    /** "Link to page": si sceglie una pagina che c'è già, e la riga rimanda a lei (dal 06/10/2026). */
    object LinkToPage : SlashAction()
    /** "Database - Full page": un database nuovo che si apre a pagina intera (dal 06/10/2026). */
    object FullPageDatabase : SlashAction()
    /** "Button": un bottone ancora senza azioni, e si apre la finestra per sceglierle (dal 27/10/2026). */
    object Button : SlashAction()
    /** "Link to button": si sceglie un bottone che c'è già, e la riga ne diventa una copia collegata (dal 27/10/2026). */
    object LinkButton : SlashAction()
    /** "Synced block": righe uguali in più pagine (dal 03/11/2026, vedi `data/sync/SyncedBlocks.kt`). */
    object Synced : SlashAction()
    /** "Record audio": un blocco audio che registra subito (dal 03/11/2026, vedi `AudioRecorder`). */
    object Record : SlashAction()
    /** "Scan document": lo scanner di Google, e le pagine diventano foto (dal 03/11/2026). */
    object Scan : SlashAction()
    /** Elencata ma non ancora costruita: si vede spenta. */
    object NotYet : SlashAction()
}

data class SlashEntry(
    val label: String,
    val category: SlashCategory,
    val action: SlashAction,
    val icon: ImageVector
) {
    val enabled: Boolean get() = action != SlashAction.NotYet
}

/**
 * Tutte le voci del menu "/", comprese **quelle che ancora non
 * esistono**. **È anche il catalogo del menu "+"** (`BlockTypeMenu`),
 * nello stesso ordine: una voce aggiunta, tolta o spostata qui cambia
 * tutti e due i menu, che è proprio lo scopo — prima avevano due elenchi
 * e si erano separati. Cosa fa ogni voce lo decide `applyInsertAction`,
 * anche quello unico per i due menu.
 *
 * Elencarle spente è una scelta: dice cosa c'è e cosa manca senza
 * costringere a cercarlo, e quando una verrà costruita basterà
 * cambiarle l'azione. È l'unico posto dell'app dove una voce inerte è
 * meglio di una voce assente — altrove (le icone della barra di un
 * database, per dire) vale la regola opposta, perché lì l'utente non
 * sta scegliendo da un catalogo ma cercando un comando.
 */
val SLASH_ENTRIES: List<SlashEntry> = listOf(
    SlashEntry("Text", SlashCategory.BASIC, SlashAction.Type(BlockType.PARAGRAPH), Icons.Filled.Notes),
    SlashEntry(
        "Bulleted list",
        SlashCategory.BASIC,
        SlashAction.Type(BlockType.BULLET_LIST_ITEM),
        Icons.Filled.FormatListBulleted
    ),
    SlashEntry(
        "Numbered list",
        SlashCategory.BASIC,
        SlashAction.Type(BlockType.NUMBERED_LIST_ITEM),
        Icons.Filled.FormatListNumbered
    ),
    SlashEntry("To-do list", SlashCategory.BASIC, SlashAction.Type(BlockType.CHECKBOX), Icons.Filled.CheckBox),
    SlashEntry("Toggle list", SlashCategory.BASIC, SlashAction.Type(BlockType.TOGGLE), Icons.Filled.ArrowRight),
    // Una riga con data e ora che manda una notifica: vedi `makeNotification`.
    SlashEntry("Notification", SlashCategory.BASIC, SlashAction.Type(BlockType.NOTIFICATION), Icons.Filled.Notifications),
    SlashEntry("Page", SlashCategory.BASIC, SlashAction.PageLink, Icons.Filled.Description),
    SlashEntry("Callout", SlashCategory.BASIC, SlashAction.Type(BlockType.CALLOUT), Icons.Filled.Info),
    SlashEntry("Quote", SlashCategory.BASIC, SlashAction.Type(BlockType.QUOTE), Icons.Filled.FormatQuote),
    SlashEntry("Table", SlashCategory.BASIC, SlashAction.Type(BlockType.TABLE), Icons.Filled.TableChart),
    SlashEntry("Divider", SlashCategory.BASIC, SlashAction.Divider, Icons.Filled.Remove),
    SlashEntry("Link to page", SlashCategory.BASIC, SlashAction.LinkToPage, Icons.Filled.Link),
    // I bottoni (dal 27/10/2026): vedi `data/buttons/Buttons.kt`.
    SlashEntry("Button", SlashCategory.BASIC, SlashAction.Button, Icons.Filled.TouchApp),
    SlashEntry("Link to button", SlashCategory.BASIC, SlashAction.LinkButton, Icons.Filled.Link),
    SlashEntry("Synced block", SlashCategory.BASIC, SlashAction.Synced, Icons.Filled.Sync),

    SlashEntry("Image", SlashCategory.MEDIA, SlashAction.Media(BlockType.IMAGE), Icons.Filled.Image),
    SlashEntry("Scan document", SlashCategory.MEDIA, SlashAction.Scan, Icons.Filled.DocumentScanner),
    SlashEntry("Video", SlashCategory.MEDIA, SlashAction.Media(BlockType.VIDEO), Icons.Filled.Videocam),
    SlashEntry("Audio", SlashCategory.MEDIA, SlashAction.Media(BlockType.AUDIO), Icons.Filled.VolumeUp),
    SlashEntry("Record audio", SlashCategory.MEDIA, SlashAction.Record, Icons.Filled.Mic),
    SlashEntry("Code", SlashCategory.MEDIA, SlashAction.Type(BlockType.CODE), Icons.Filled.Code),
    SlashEntry("File", SlashCategory.MEDIA, SlashAction.Media(BlockType.FILE), Icons.Filled.AttachFile),
    SlashEntry("Web bookmark", SlashCategory.MEDIA, SlashAction.Media(BlockType.BOOKMARK), Icons.Filled.Bookmark),

    SlashEntry(
        "Table view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.TABLE),
        Icons.Filled.TableChart
    ),
    SlashEntry(
        "Board view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.BOARD),
        Icons.Filled.ViewColumn
    ),
    SlashEntry(
        "Gallery view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.GALLERY),
        Icons.Filled.GridView
    ),
    SlashEntry(
        "List view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.LIST),
        Icons.Filled.List
    ),
    SlashEntry(
        "Calendar view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.CALENDAR),
        Icons.Filled.CalendarToday
    ),
    SlashEntry(
        "Timeline view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.TIMELINE),
        Icons.Filled.ViewTimeline
    ),
    SlashEntry(
        "Chart view",
        SlashCategory.DATABASE,
        SlashAction.Database(DatabaseLayout.CHART),
        Icons.Filled.PieChart
    ),
    SlashEntry(
        "Database - Inline",
        SlashCategory.DATABASE,
        SlashAction.Database(null),
        Icons.Filled.TableChart
    ),
    SlashEntry("Database - Full page", SlashCategory.DATABASE, SlashAction.FullPageDatabase, Icons.Filled.OpenInFull),
    // Il database le cui righe sono solo testo: vedi
    // `PageEntity.isSimpleDatabase`. Nasce come tabella, come "Database -
    // Inline", e la vista si cambia poi dalle impostazioni.
    SlashEntry(
        "Simple database",
        SlashCategory.DATABASE,
        SlashAction.Database(null, simple = true),
        Icons.Filled.GridOn
    ),
    // Lo stesso database mostrato in un'altra pagina, con filtri e
    // raggruppamenti suoi: vedi `PageEntity.sourceDatabaseId`.
    SlashEntry("Linked view of data source", SlashCategory.DATABASE, SlashAction.LinkedView, Icons.Filled.Link)
)

/**
 * La pagina aperta sta in un quaderno (vedi `NotebookEntity`). Lo sanno i
 * menu, che lì non offrono quello che porta al menu principale.
 */
val LocalInNotebook = compositionLocalOf { false }

/**
 * Le voci dei menu "/" e "+" per la pagina aperta. **In un quaderno senza
 * sottopagine, database e collegamenti a pagine**: i quaderni sono un mondo
 * a parte, e una pagina o un database nati lì dentro non starebbero né di
 * qua né di là.
 */
@Composable
fun slashEntries(): List<SlashEntry> = if (LocalInNotebook.current) NOTEBOOK_SLASH_ENTRIES else SLASH_ENTRIES

internal val NOTEBOOK_SLASH_ENTRIES: List<SlashEntry> by lazy {
    SLASH_ENTRIES.filter { entry ->
        entry.category != SlashCategory.DATABASE &&
            entry.action != SlashAction.PageLink &&
            entry.label != "Link to page" &&
            entry.action != SlashAction.LinkButton
    }
}

/**
 * Come si scrive il numero di un elenco numerato, un modo per livello
 * di rientro, come su OneNote: `1.`, `i.`, `1)`, `i)`, `a.`, `One.`,
 * `a)`, `First.` — e poi si ricomincia.
 *
 * Come per i pallini, **dipende solo dal livello**: una riga riportata
 * indietro ritrova il modo di scrivere della colonna in cui arriva.
 */
internal val NUMBERED_STYLES: List<(Int) -> String> = listOf(
    { n -> "$n." },
    { n -> romanNumeral(n) + "." },
    { n -> "$n)" },
    { n -> romanNumeral(n) + ")" },
    { n -> letterOrdinal(n) + "." },
    { n -> cardinalWord(n) + "." },
    { n -> letterOrdinal(n) + ")" },
    { n -> ordinalWord(n) + "." }
)

/**
 * Il numero scritto come lo vuole il livello in cui si trova. `internal`
 * perché lo usa anche l'anteprima delle schede della galleria: un elenco
 * deve leggersi uguale nella pagina e nella scheda.
 */
fun numberedMarkerFor(indent: Int, ordinal: Int): String {
    val i = ((indent % NUMBERED_STYLES.size) + NUMBERED_STYLES.size) % NUMBERED_STYLES.size
    return NUMBERED_STYLES[i](ordinal.coerceAtLeast(1))
}

/**
 * `1` → `i`, `4` → `iv`, `9` → `ix`. Minuscoli, come nella foto di
 * OneNote.
 *
 * Oltre il 3999 i numeri romani non si scrivono più con le lettere
 * latine: lì si torna alle cifre, che è meglio di una riga di M.
 */
internal fun romanNumeral(n: Int): String {
    if (n !in 1..3999) return n.toString()
    val values = listOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val signs = listOf("m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i")
    var left = n
    val out = StringBuilder()
    values.indices.forEach { i ->
        while (left >= values[i]) {
            out.append(signs[i])
            left -= values[i]
        }
    }
    return out.toString()
}

/**
 * `1` → `a`, `26` → `z`, `27` → `aa`: le lettere come le colonne di un
 * foglio di calcolo, così dopo la zeta si continua invece di fermarsi.
 */
internal fun letterOrdinal(n: Int): String {
    if (n < 1) return "a"
    var left = n
    val out = StringBuilder()
    while (left > 0) {
        val rest = (left - 1) % 26
        out.append(('a' + rest))
        left = (left - 1) / 26
    }
    return out.reverse().toString()
}

internal val CARDINAL_WORDS = listOf(
    "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
    "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
    "Eighteen", "Nineteen", "Twenty"
)

internal val ORDINAL_WORDS = listOf(
    "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth",
    "Ninth", "Tenth", "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth",
    "Sixteenth", "Seventeenth", "Eighteenth", "Nineteenth", "Twentieth"
)

/**
 * `1` → `One`. Oltre il ventesimo si torna alla cifra: un elenco a
 * parole così lungo non si legge più, e "Thirtyseventh" occuperebbe
 * mezza riga.
 */
internal fun cardinalWord(n: Int): String = CARDINAL_WORDS.getOrNull(n - 1) ?: n.toString()

/** `1` → `First`, con lo stesso limite di `cardinalWord`. */
internal fun ordinalWord(n: Int): String = ORDINAL_WORDS.getOrNull(n - 1) ?: n.toString()
