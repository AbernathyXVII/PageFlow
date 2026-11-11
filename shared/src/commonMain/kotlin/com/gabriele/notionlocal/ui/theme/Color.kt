package com.gabriele.notionlocal.ui.theme

import androidx.compose.ui.graphics.Color
import com.gabriele.notionlocal.data.settings.AppSettings

// Palette ispirata al look pulito di Notion: molto neutro, con un
// singolo accento. Niente colori sgargianti — l'attenzione va sul
// contenuto, non sulla UI.
//
// **I colori seguono il tema da soli.** Sono letti in centinaia di punti,
// dentro e fuori dalle schermate (anche da chi disegna il testo), e
// passarli tutti da `MaterialTheme` avrebbe voluto dire riscrivere mezza
// app. Qui ognuno sceglie la sua versione chiara o scura leggendo
// `AppSettings.isDark`, che è uno stato di Compose: cambiando tema chi li
// usa si ridisegna col colore nuovo.
//
// I nomi sono rimasti quelli di quando l'app era solo scura: in tema
// chiaro `NotionWhite` è il colore del **testo**, cioè quasi nero, e
// `DarkBackground` è il bianco della pagina. Dicono il ruolo, non la tinta.

private inline fun themed(dark: Long, light: Long): Color =
    Color(if (AppSettings.isDark) dark else light)

/** Il testo e le icone principali. */
val NotionWhite: Color get() = themed(0xFFFFFFFF, 0xFF37352F)
val NotionBlack: Color get() = themed(0xFF191919, 0xFFFFFFFF)
val NotionGray900: Color get() = themed(0xFF2F2F2F, 0xFFE3E2E0)
val NotionGray700: Color get() = themed(0xFF5F5F5F, 0xFF787774)
/** Il testo secondario: suggerimenti, voci spente, etichette. */
val NotionGray400: Color get() = themed(0xFF9B9B9B, 0xFF9B9A97)
val NotionGray200: Color get() = themed(0xFFE9E9E7, 0xFF3F3F3F)
val NotionGray100: Color get() = themed(0xFFF4F4F2, 0xFF2F2F2F)

val AccentBlue = Color(0xFF2383E2)
val AccentBlueLight = Color(0xFFE7F3FF)

// I tre grigi di Notion, campionati dalle sue schermate: lo sfondo
// delle pagine, quello delle finestre che salgono dal basso, e i
// riquadri che raggruppano le voci dentro quelle finestre. Sono vicini
// fra loro di proposito — bastano a separare i piani senza che si
// veda un contrasto. In chiaro sono i bianchi sporchi di Notion.
val DarkBackground: Color get() = themed(0xFF191919, 0xFFFFFFFF)
val DarkSheet: Color get() = themed(0xFF202020, 0xFFF7F7F5)
val DarkSurface: Color get() = themed(0xFF252525, 0xFFF1F1EF)
val DarkSurfaceVariant: Color get() = themed(0xFF2F2F2F, 0xFFE9E9E7)

// La barra laterale è **un filo più scura** delle pagine, come chiesto:
// quanto basta a dire "questo è un altro piano" senza diventare una
// macchia. In chiaro è il grigio caldo della barra di Notion.
val SidebarBackground: Color get() = themed(0xFF131313, 0xFFF3F3F1)
/** Il pallino del pulsante che fissa la barra laterale sul PC: appena diverso dal fondo. */
val SidebarPinCircle: Color get() = themed(0xFF2A2A2A, 0xFFE2E2DF)

// La fascia del Quaderno nella barra laterale: un colore suo, diverso
// dal resto della barra, perché sia chiaro che è una cosa a parte.
val NotebookBand: Color get() = themed(0xFF1E2A38, 0xFFE3EEFA)

// Il velo che copre un testo da spoiler, e la traccia che resta quando
// è stato scoperto. Il velo è **più chiaro dei grigi qui sopra**: deve
// leggersi come una fascia piena anche in mezzo a una riga di testo, non
// come un'ombra. La traccia invece è appena accennata, quanto basta per
// ricordare che quel pezzo è coperto per chi ancora non l'ha toccato.
// In chiaro il velo è un grigio medio, pieno: il testo scuro non ci
// traspare.
val SpoilerCover: Color get() = themed(0xFF4A4A4A, 0xFFBDBDBA)
val SpoilerRevealed = Color(0x2E7A7A7A)

// Il testo con un collegamento (dal 17/10/2026): il blu del testo di
// Notion, quello del tema scuro e quello del tema chiaro. Sottolineato,
// così si riconosce anche da chi non distingue i colori.
val TextLinkColor: Color get() = themed(0xFF529CCA, 0xFF0B6E99)

// La stellina dei preferiti da accesa. Da spenta resta del colore del
// testo, come tutte le altre icone della barra: il giallo serve a dire
// "questa è accesa", e se lo avesse sempre non direbbe niente.
val FavoriteStar = Color(0xFFE3B341)

// Il cerchietto dei pulsanti sospesi sopra la pagina (indietro, barra
// laterale, tre puntini): un grigio medio al 70%, che si legge sopra una
// copertina scura come sopra una chiara e la lascia intravedere. In
// chiaro è più chiaro, così l'icona scura ci sta sopra.
val FloatingButtonBackground: Color get() = themed(0xB3505050, 0xB3E0E0DE)

// Lo sfondo di un riquadro ("Callout", dal 06/10/2026), quando l'utente
// non ne ha scelto uno: un grigio più chiaro della pagina, che la stacca
// senza colorarla, come su Notion.
val CalloutBackground: Color get() = themed(0xFF373737, 0xFFF1F1EF)

// Lo sfondo del blocco "Code" (dal 06/10/2026): appena staccato dalla pagina,
// più scuro del riquadro, perché i colori del codice si leggano bene.
val CodeBackground: Color get() = themed(0xFF252525, 0xFFF7F6F3)
