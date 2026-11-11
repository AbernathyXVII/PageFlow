package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * **Le piccole cose che ogni sistema fa a modo suo** (dal 04/11/2026, per le
 * schermate nel codice comune): un avviso breve, aprire un link, gli appunti,
 * i nomi dei comandi di modifica. Sul telefono sono quelle di Android di
 * sempre (`PlatformActions.android.kt`), sul PC quelle di Windows e Mac.
 */

/**
 * Vero sul PC (mouse e finestra): le finestrelle si aprono accanto al clic e
 * i blocchi hanno i sei puntini (dal 11/11/2026, vedi `PcPanel.desktop.kt`).
 */
expect val isPc: Boolean

/** Un avviso breve in basso: il `Toast` del telefono ([long]: quello che resta di più). */
expect fun showMessage(context: PlatformContext, text: String, long: Boolean = false)

/**
 * Apre un indirizzo (una pagina web, una mail, un numero) con l'app o il
 * browser di sistema. Falso se nessuno lo sa aprire.
 */
expect fun openUrl(context: PlatformContext, url: String): Boolean

/**
 * Copia [text] negli appunti come testo semplice. Vero se il sistema dice già
 * da sé "copiato" (Android 13 e dopo): allora l'app non lo ripete.
 */
expect fun copyPlainText(context: PlatformContext, label: String, text: String): Boolean

/** C'è qualcosa negli appunti, senza leggerlo (sul telefono leggerlo fa comparire un avviso). */
expect fun clipboardHasContent(context: PlatformContext): Boolean

/** Il testo negli appunti, o null. */
expect fun clipboardText(context: PlatformContext): String?

/** I comandi di modifica del testo, coi nomi del sistema: vedi [editCommandLabel]. */
enum class EditCommand { CUT, COPY, PASTE, SELECT_ALL }

/**
 * Il nome di un comando di modifica: sul telefono quello di Android, nella
 * lingua del telefono (come la barrettina di sistema); sul PC quello dell'app.
 */
@Composable
expect fun editCommandLabel(command: EditCommand): String

/**
 * La larghezza e l'altezza dello schermo (sul PC, della finestra): sul telefono
 * quelle di `LocalConfiguration`, come prima.
 */
@Composable
expect fun screenWidthDp(): Dp

@Composable
expect fun screenHeightDp(): Dp
