package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.isPc
import com.gabriele.notionlocal.ui.theme.NoRowLinesOnPc
import com.gabriele.notionlocal.ui.theme.pcOr
import androidx.compose.foundation.layout.Box
import com.gabriele.notionlocal.ui.i18n.EditorStrings
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.WidthFull
import com.gabriele.notionlocal.ui.i18n.ParagraphStrings
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gabriele.notionlocal.data.TextStats
import com.gabriele.notionlocal.ui.format.Formats
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.repository.PageRepository.MoveExclusions
import com.gabriele.notionlocal.data.repository.PageRepository.PageTreeNode
import com.gabriele.notionlocal.viewmodel.ViewModelFactory
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.settings.TrashPolicy
import com.gabriele.notionlocal.ui.i18n.InkStrings
import com.gabriele.notionlocal.ui.i18n.LockStrings
import com.gabriele.notionlocal.ui.i18n.ProofingStrings
import com.gabriele.notionlocal.ui.i18n.NotebookStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.HistoryStrings
import com.gabriele.notionlocal.ui.i18n.WebClipStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.DarkSurface
import com.gabriele.notionlocal.ui.theme.FavoriteStar
import androidx.compose.material.icons.filled.Bolt
import com.gabriele.notionlocal.ui.i18n.AutomationStrings

/**
 * Il menu dei tre puntini, uguale per una pagina e per un database.
 *
 * Sta in un file suo perché lo aprono due schermate diverse —
 * `PageEditorScreen` e `DatabaseViewScreen` — e due copie si
 * sarebbero separate al primo cambiamento, che è esattamente quello
 * che si vuole evitare in un menu di comandi.
 *
 * Ogni voce che non ha senso per la pagina aperta **non si vede**,
 * invece di vedersi spenta: la pagina principale non si duplica né si
 * butta, e "Lock view" riguarda solo l'impaginazione di un database.
 * Le spente sono Import ed Export, che si vedono apposta per dire che
 * arriveranno, e "Move to" sulla pagina principale, chiesta così
 * dall'utente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageOptionsSheet(
    page: PageEntity,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSearch: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onMoveToTrash: () -> Unit,
    /** "Move to database" (dal 05/10/2026): null = la voce non c'è. */
    onMoveToDatabase: (() -> Unit)? = null,
    onToggleViewLock: () -> Unit,
    onToggleLock: () -> Unit,
    onUpdates: () -> Unit,
    /** "Page history", le versioni della pagina (dal 02/11/2026); null dove non c'è (i database). */
    onHistory: (() -> Unit)? = null,
    /** "Export", la pagina in PDF o Markdown (dal 02/11/2026); null = voce grigia (i database). */
    onExport: (() -> Unit)? = null,
    /** "Automations" di un database (dal 03/11/2026); null dove non c'è (le pagine). */
    onAutomations: (() -> Unit)? = null,
    /**
     * **La matita**: disegnare a mano sulla pagina (dal 25/09/2026). Solo
     * per le pagine di testo — un database a schermo intero non ha dove
     * tenere i disegni — e null altrove, e allora la voce non c'è.
     */
    onDraw: (() -> Unit)? = null,
    /**
     * **"Blocca con password"** (dal 26/09/2026): per aprire la pagina (o
     * il database) servono la password o l'impronta. Null sulla pagina
     * principale, che non si blocca: chiuderebbe l'app intera.
     */
    onTogglePasswordLock: (() -> Unit)? = null,
    /**
     * Solo per un database che da qualche parte è mostrato come pagina
     * (dopo "Turn into page"): lo rimette dentro la pagina che lo
     * richiama. Null altrove, e allora la voce non c'è.
     */
    onTurnIntoDatabase: (() -> Unit)? = null,
    /**
     * Quanto testo c'è nella pagina, per la voce "X words" in fondo.
     * Null per i database — il loro contenuto sono righe, e le righe non
     * si contano — e per il breve istante in cui il conteggio si sta
     * facendo: la voce compare quando c'è il numero.
     */
    textStats: TextStats? = null,
    /**
     * **Una pagina di un quaderno** (dal 26/09/2026): niente preferiti,
     * ricerca, duplica e sposta, che sono del menu principale, e al posto di
     * "Move to trash" "Delete page", per sempre.
     */
    inNotebook: Boolean = false,
    /**
     * **"Add cover"**, in cima a tutto (dal 05/10/2026, chiesto
     * dall'utente): prima stava accanto all'icona della pagina. Null quando
     * la pagina ha già una copertina (si cambia toccandola), per i database
     * e nelle pagine dei quaderni: allora la voce non c'è.
     */
    onAddCover: (() -> Unit)? = null,
    /**
     * **"Proofing"** (dal 08/10/2026): il correttore di questa pagina, con
     * scritto accanto com'è adesso ([proofingValue]). Null per i database,
     * che non hanno testo da correggere: allora la voce non c'è.
     */
    proofingValue: String? = null,
    onProofing: (() -> Unit)? = null,
    /**
     * **"Paragraph"** (dal 21/10/2026, chiesto dall'utente): lo spazio fra i
     * blocchi e dopo i marcatori degli elenchi di questa pagina, con le
     * misure che valgono adesso ([paragraphValue]). Null per i database:
     * allora la voce non c'è.
     */
    paragraphValue: String? = null,
    onParagraph: (() -> Unit)? = null,
    /**
     * **"Full or page line"** (dal 27/10/2026, chiesto dall'utente): le righe
     * lunghe di questa pagina intere o a capo, o come Settings, con scritto
     * accanto com'è adesso ([fullLineValue]). Null per i database: allora la
     * voce non c'è. Vedi `FullLineChoice.kt`.
     */
    fullLineValue: String? = null,
    onFullLine: (() -> Unit)? = null,
    /**
     * **"Full width"** (dal 12/11/2026, solo sul PC, come in Notion): il testo
     * largo quanto lo spazio ([fullWidth]) o in una colonna al centro. Null
     * sul telefono e per i database: allora la voce non c'è.
     */
    fullWidth: Boolean = true,
    onToggleFullWidth: (() -> Unit)? = null,
    /**
     * **"Web Clipper"** (dal 20/10/2026, chiesto dall'utente): la foto di un
     * sito nella pagina, come OneNote Web Clipper. Null per i database e le
     * pagine nel cestino: allora la voce non c'è.
     */
    onWebClipper: (() -> Unit)? = null
) {
    val isRoot = page.id == PageEntity.ROOT_PAGE_ID

    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        // Scorre: aperto il conteggio del testo, le voci sono più di
        // quante ne stiano su uno schermo basso.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = page.title.ifBlank { Strings.untitled },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (onAddCover != null) {
                OptionsGroup {
                    OptionRow(icon = Icons.Filled.Image, label = EditorStrings.addCover, onClick = onAddCover)
                }
                Spacer(modifier = Modifier.size(12.dp))
            }

            OptionsGroup {
                // La pagina principale non si mette fra i preferiti: è
                // quella da cui si parte sempre.
                if (!isRoot && !inNotebook) {
                    OptionRow(
                        icon = if (page.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        label = Strings.favorite,
                        iconTint = if (page.isFavorite) FavoriteStar else null,
                        trailing = { Switch(checked = page.isFavorite, onCheckedChange = { onToggleFavorite() }) },
                        onClick = onToggleFavorite
                    )
                    HorizontalDivider()
                }
                if (!inNotebook) {
                    OptionRow(
                        icon = Icons.Filled.Search,
                        label = Strings.search,
                        onClick = onSearch
                    )
                }
                if (onDraw != null) {
                    if (!inNotebook) HorizontalDivider()
                    // Bloccata, la pagina non si scrive e non si disegna.
                    OptionRow(
                        icon = Icons.Filled.Draw,
                        label = InkStrings.draw,
                        enabled = !page.isLocked,
                        onClick = onDraw
                    )
                }
                if (onWebClipper != null) {
                    if (!inNotebook || onDraw != null) HorizontalDivider()
                    // Bloccata, nella pagina non si aggiunge niente.
                    OptionRow(
                        icon = Icons.Filled.ContentCut,
                        label = WebClipStrings.webClipper,
                        enabled = !page.isLocked,
                        onClick = onWebClipper
                    )
                }
                // Duplicare la pagina principale vorrebbe dire copiare
                // l'archivio intero, sottopagine comprese, dentro sé
                // stesso. Buttarla non si può comunque.
                // Nemmeno per un template: la copia finirebbe nel menu
                // principale, come una pagina, e i template stanno a parte.
                if (!isRoot && !inNotebook && !page.isTemplate) {
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.ContentCopy,
                        label = Strings.duplicate,
                        onClick = onDuplicate
                    )
                }
                // Spostare sì, tranne la pagina principale, che non sta
                // dentro niente. Lì la voce c'è ma è **grigia**, invece di
                // sparire come le altre: l'utente ha chiesto di vederla,
                // e grigia dice "non qui" senza bisogno di un messaggio
                // (24/09/2026).
                // Un template non sta dentro nessuna pagina: "Move to" lo
                // farebbe diventare una pagina qualsiasi.
                if (!inNotebook && !page.isTemplate) {
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.DriveFileMove,
                        label = Strings.moveTo,
                        enabled = !isRoot,
                        onClick = onMoveTo
                    )
                    // **"Move to database"** (dal 05/10/2026, chiesto
                    // dall'utente): diventare una pagina di un database.
                    if (!isRoot && onMoveToDatabase != null) {
                        HorizontalDivider()
                        OptionRow(
                            icon = Icons.Filled.TableRows,
                            label = Strings.moveToDatabase,
                            onClick = onMoveToDatabase
                        )
                    }
                }
                if (!isRoot) {
                    if (onTurnIntoDatabase != null) {
                        HorizontalDivider()
                        OptionRow(
                            icon = Icons.Filled.TableChart,
                            label = Strings.turnIntoDatabase,
                            onClick = onTurnIntoDatabase
                        )
                    }
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.Delete,
                        label = if (inNotebook) NotebookStrings.deletePage else Strings.moveToTrash,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onMoveToTrash
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            OptionsGroup {
                // Solo sui database: bloccare l'impaginazione di una
                // pagina di testo non vorrebbe dire niente.
                if (page.isDatabase) {
                    OptionRow(
                        icon = Icons.Filled.ViewQuilt,
                        label = Strings.lockView,
                        trailing = { Switch(checked = page.isViewLocked, onCheckedChange = { onToggleViewLock() }) },
                        onClick = onToggleViewLock
                    )
                    HorizontalDivider()
                }
                OptionRow(
                    icon = if (page.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    label = Strings.lockPage,
                    trailing = { Switch(checked = page.isLocked, onCheckedChange = { onToggleLock() }) },
                    onClick = onToggleLock
                )
                if (onTogglePasswordLock != null) {
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.Fingerprint,
                        label = LockStrings.passwordLock,
                        trailing = {
                            Switch(checked = page.isProtected, onCheckedChange = { onTogglePasswordLock() })
                        },
                        onClick = onTogglePasswordLock
                    )
                }
            }

            if (onProofing != null) {
                Spacer(modifier = Modifier.size(12.dp))
                OptionsGroup {
                    OptionRow(
                        icon = Icons.Filled.Spellcheck,
                        label = ProofingStrings.proofing,
                        trailing = {
                            Text(
                                proofingValue.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onProofing
                    )
                    if (onParagraph != null) {
                        HorizontalDivider()
                        OptionRow(
                            icon = Icons.Filled.FormatLineSpacing,
                            label = ParagraphStrings.paragraph,
                            trailing = {
                                Text(
                                    paragraphValue.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = onParagraph
                        )
                    }
                    if (onFullLine != null) {
                        HorizontalDivider()
                        OptionRow(
                            icon = Icons.AutoMirrored.Filled.WrapText,
                            label = Strings.fullOrPageLine,
                            trailing = {
                                Text(
                                    fullLineValue.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = onFullLine
                        )
                    }
                    if (onToggleFullWidth != null) {
                        HorizontalDivider()
                        OptionRow(
                            icon = Icons.Filled.WidthFull,
                            label = Strings.fullWidth,
                            trailing = { Switch(checked = fullWidth, onCheckedChange = { onToggleFullWidth() }) },
                            onClick = onToggleFullWidth
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            OptionsGroup {
                OptionRow(
                    icon = Icons.Filled.SystemUpdateAlt,
                    label = Strings.import,
                    enabled = false,
                    onClick = {}
                )
                HorizontalDivider()
                OptionRow(
                    icon = Icons.Filled.Upload,
                    label = Strings.export,
                    enabled = onExport != null,
                    onClick = { onExport?.invoke() }
                )
            }

            if (onAutomations != null) {
                Spacer(modifier = Modifier.size(12.dp))
                OptionsGroup {
                    OptionRow(
                        icon = Icons.Filled.Bolt,
                        label = AutomationStrings.automations,
                        onClick = onAutomations
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            OptionsGroup {
                OptionRow(
                    icon = Icons.Filled.History,
                    label = Strings.updates,
                    onClick = onUpdates
                )
                if (onHistory != null) {
                    HorizontalDivider()
                    OptionRow(
                        icon = Icons.Filled.Restore,
                        label = HistoryStrings.pageHistory,
                        onClick = onHistory
                    )
                }
            }

            // **Il conteggio del testo**, in fondo come l'ha chiesto
            // l'utente e come sta su Notion. Chiuso dice solo le parole,
            // che è quello che si cerca quasi sempre; toccandolo si apre
            // **lì sotto** il resto, come la finestra "Word Count" di
            // Word. Sotto e non in una finestra nuova: una finestra sopra
            // un'altra finestra, su un telefono, copre proprio il menu da
            // cui si arriva, e per richiuderla servirebbe un tocco in più.
            if (textStats != null) {
                var statsOpen by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.size(12.dp))
                OptionsGroup {
                    OptionRow(
                        icon = Icons.Filled.TextFields,
                        label = Strings.wordCount(textStats.words),
                        trailing = {
                            Icon(
                                imageVector = if (statsOpen) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { statsOpen = !statsOpen }
                    )
                    if (statsOpen) {
                        HorizontalDivider()
                        // Nell'ordine chiesto dall'utente: le parole per
                        // prime, anche se si leggono già sulla voce sopra.
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            StatRow(Strings.statWords, textStats.words)
                            StatRow(Strings.statLetters, textStats.letters)
                            StatRow(Strings.statNumbers, textStats.digits)
                            StatRow(Strings.statLettersAndNumbers, textStats.lettersAndDigits)
                            StatRow(Strings.statCharactersNoSpaces, textStats.charactersNoSpaces)
                            StatRow(Strings.statCharactersWithSpaces, textStats.charactersWithSpaces)
                            StatRow(Strings.statLines, textStats.lines)
                            StatRow(Strings.statJapanese, textStats.japanese)
                            StatRow(Strings.statChinese, textStats.chinese)
                            StatRow(Strings.statKorean, textStats.korean)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Una riga del conteggio del testo: il nome a sinistra, il numero a
 * destra, come nella finestra di Word. Rientrata fin sotto il testo della
 * voce "X words", così si legge come il suo contenuto e non come altre
 * voci del menu; il numero segue il formato scelto nelle impostazioni
 * (1.000 o 1,000).
 */
@Composable
private fun StatRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = Formats.number(value.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * "Move to": dove mettere la pagina (o il database) che si sposta.
 *
 * È la stessa scelta di "Duplicate → Dentro una pagina" — l'albero della
 * barra laterale con la ricerca in cima — perché l'utente la voleva così
 * (24/09/2026): prima era un elenco piatto, in ordine alfabetico, delle
 * sole pagine di testo, senza le pagine dei database e senza dire dove
 * stesse ciascuna. Qui i database si aprono come cartelle e si sceglie
 * una delle loro pagine; la pagina che si sposta e quello che ha dentro
 * si vedono grigie (vedi `PageRepository.moveExclusions`).
 *
 * La riga di spiegazione cambia con quello che si sposta: un database
 * arriva come collegamento a pagina, e la pagina di una riga esce dal suo
 * database (vedi `PageRepository.movePageTo`).
 */
@Composable
fun MovePagePicker(
    page: PageEntity,
    exclusions: MoveExclusions,
    factory: ViewModelFactory,
    onDismiss: () -> Unit,
    /** La destinazione, e per un database se arriva aperto dentro la pagina ("Move as inline"). */
    onPick: (PageTreeNode, Boolean) -> Unit
) {
    // **"Move as inline"** (dal 08/10/2026, chiesto dall'utente): per un
    // database, in fondo, se arriva aperto dentro la pagina o come
    // collegamento a pagina. Parte com'è adesso.
    var inline by remember(page.id) { mutableStateOf(exclusions.shownInline) }
    DestinationPickerSheet(
        factory = factory,
        title = Strings.moveTo,
        hint = when {
            page.isDatabase -> Strings.moveDatabaseHint
            page.isRowPage -> Strings.moveRowPageHint
            else -> Strings.moveHint
        },
        viewModelKey = "move-destination",
        onDismiss = onDismiss,
        onPick = { onPick(it, page.isDatabase && inline) },
        excluded = exclusions,
        footer = if (page.isDatabase) {
            {
                HorizontalDivider()
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SettingsToggleRow(
                        icon = Icons.Filled.TableChart,
                        label = Strings.moveAsInline,
                        checked = inline,
                        onCheckedChange = { inline = it }
                    )
                }
            }
        } else {
            null
        }
    )
}

/**
 * **"Move to database"** (dal 05/10/2026): lo stesso albero di "Move to",
 * ma si tocca un database (vedi `DestinationPickerSheet.pickDatabases`). La
 * pagina, o il database, diventa una sua pagina nuova (vedi
 * `PageRepository.moveIntoDatabase`).
 *
 * **Scelto il database, si sceglie il nome della pagina nuova** (dal
 * 05/10/2026, chiesto dall'utente), prima di spostare: è già scritto quello
 * di adesso, tutto selezionato. "Cancel" torna alla scelta del database.
 */
@Composable
fun MoveToDatabasePicker(
    page: PageEntity,
    exclusions: MoveExclusions,
    factory: ViewModelFactory,
    onDismiss: () -> Unit,
    onPick: (destination: PageTreeNode, rowTitle: String) -> Unit
) {
    var chosen by remember { mutableStateOf<PageTreeNode?>(null) }
    val database = chosen
    if (database == null) {
        DestinationPickerSheet(
            factory = factory,
            title = Strings.moveToDatabase,
            hint = if (page.isDatabase) Strings.moveDatabaseToDatabaseHint else Strings.moveToDatabaseHint,
            viewModelKey = "move-to-database",
            onDismiss = onDismiss,
            onPick = { chosen = it },
            excluded = exclusions,
            pickDatabases = true
        )
    } else {
        NewRowNameDialog(
            current = page.title.takeUnless { it == "Untitled" }.orEmpty(),
            explanation = if (page.isDatabase) {
                Strings.newRowNameDatabaseHint(database.placeName())
            } else {
                Strings.newRowNameHint(database.placeName())
            },
            onBack = { chosen = null },
            onConfirm = { name -> onPick(database, name) }
        )
    }
}

/** Il nome della pagina nuova di "Move to database": quello di adesso, tutto selezionato. */
@Composable
private fun NewRowNameDialog(
    current: String,
    explanation: String,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val confirm = { onConfirm(value.text.replace("\n", "").trim()) }
    FitAlertDialog(
        onDismissRequest = onBack,
        title = { Text(Strings.newRowName) },
        text = {
            Column {
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.size(12.dp))
                AutoCaps {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.copy(text = it.text.replace("\n", "")) },
                        singleLine = true,
                        placeholder = { Text(Strings.untitled) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { confirm() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = confirm) { Text(Strings.moveButton) } },
        dismissButton = { TextButton(onClick = onBack) { Text(Strings.cancel) } }
    )
}

/** Come chiamare nell'avviso il posto scelto: il nome della pagina, o "Menu principale". */
fun PageTreeNode.placeName(): String =
    if (pageId == PageEntity.ROOT_PAGE_ID) Strings.mainMenu else title.ifBlank { Strings.untitled }

/** La conferma prima di buttare una pagina: è l'unica voce che toglie qualcosa di visibile. */
@Composable
fun MoveToTrashDialog(
    pageTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Dice dove va la pagina e quando se ne andrà davvero, secondo la
    // regola scelta nel cestino: buttarla non deve sembrare più
    // definitivo — né meno — di quello che è.
    val fate = when (AppSettings.trashPolicy) {
        TrashPolicy.KEEP_30_DAYS -> Strings.trashFateKeep
        TrashPolicy.EMPTY_AUTOMATICALLY -> Strings.trashFateEmpty
    }
    FitAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.moveToTrash) },
        text = { Text("${Strings.moveToTrashText(pageTitle.ifBlank { Strings.untitled })} $fate") },
        confirmButton = { TextButton(onClick = onConfirm) { Text(Strings.moveToTrash) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

@Composable
fun OptionsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Sul PC niente riquadro grigio, come i menu di Notion.
            .then(if (isPc) Modifier else Modifier.background(DarkSurface, RoundedCornerShape(12.dp)))
    ) {
        NoRowLinesOnPc(content)
    }
}

@Composable
fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color? = null,
    iconTint: Color? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    // Spenta, la voce si vede ma non risponde: è come si dice "questa
    // arriverà" senza doverlo scrivere da nessuna parte.
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        tint != null -> tint
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = pcOr(6.dp, 16.dp), vertical = pcOr(5.dp, 14.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) (iconTint ?: contentColor) else contentColor,
                modifier = Modifier.size(pcOr(18.dp, 20.dp))
            )
            Spacer(modifier = Modifier.size(pcOr(10.dp, 16.dp)))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
        }
        trailing?.invoke()
    }
}
