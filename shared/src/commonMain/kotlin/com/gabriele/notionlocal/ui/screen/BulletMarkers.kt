package com.gabriele.notionlocal.ui.screen

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * I marcatori degli elenchi puntati, uno per livello di rientro, come
 * su OneNote: pallino pieno, pallino vuoto, quadratino pieno,
 * quadratino vuoto, rombo pieno, rombo vuoto, triangolino pieno,
 * triangolino vuoto — e poi si ricomincia.
 *
 * **Dipende solo dal livello di rientro**, non da cosa c'era prima:
 * così una riga riportata indietro ritrova il segno della colonna in
 * cui arriva, qualunque strada abbia fatto. Con un contatore
 * "prosegui da dove eri" due righe alla stessa altezza potrebbero
 * avere segni diversi, che è esattamente quello che non si vuole.
 *
 * Sono tutti caratteri delle varianti "piccole" dove esistono (▪ ▫ ▸
 * ▹): il quadratino vuoto deve restare distinguibile a colpo d'occhio
 * da una vera casella da spuntare, che è un riquadro disegnato e molto
 * più grande.
 */
val BULLET_MARKERS = listOf("•", "◦", "▪", "▫", "◆", "◇", "▸", "▹")

/**
 * Quanto va scritto ogni segno perché si **vedano tutti della stessa
 * misura**.
 *
 * Questi caratteri sono disegnati molto diversi fra loro: un pallino
 * occupa meno di un terzo del suo spazio, un rombo quasi due terzi. Con
 * un'unica dimensione per tutti — com'era prima — i pallini venivano
 * minuscoli e i rombi delle macchie. Qui ognuno ha il corpo che lo
 * porta a occupare all'incirca la stessa altezza sullo schermo.
 *
 * **Nessuno supera i 20**, ed è una regola, non un caso. Il limite non
 * è la misura del testo (16) ma **l'altezza della riga**: il testo
 * scorrevole sta su righe da 24, e un carattere da 16 ne occupa in
 * altezza meno di 19, quindi c'è margine. Finché il marcatore resta
 * dentro quei 24 la riga non si allarga; oltre, si allargherebbero
 * solo le righe con il segno più grande e l'elenco verrebbe a scalini.
 */
val BULLET_MARKER_SIZES = listOf(
    20.sp, // •  pallino pieno: il glifo è piccolo di suo, va scritto grande
    20.sp, // ◦  pallino vuoto
    18.sp, // ▪  quadratino pieno
    18.sp, // ▫  quadratino vuoto
    13.sp, // ◆  rombo pieno: il glifo è grande, va scritto piccolo
    13.sp, // ◇  rombo vuoto
    17.sp, // ▸  triangolino pieno
    17.sp  // ▹  triangolino vuoto
)

fun bulletIndexFor(indent: Int): Int =
    ((indent % BULLET_MARKERS.size) + BULLET_MARKERS.size) % BULLET_MARKERS.size

/**
 * Il segno da mettere davanti a un elenco puntato rientrato di `indent`
 * livelli. `internal` per la stessa ragione di `numberedMarkerFor`.
 * Con [chosen] quello scelto dal clic destro, qualunque sia il rientro (dal 25/11/2026,
 * vedi `BlockAttrs.listMarker`).
 */
fun bulletMarkerFor(indent: Int, chosen: Int? = null): String = BULLET_MARKERS[chosen?.mod(BULLET_MARKERS.size) ?: bulletIndexFor(indent)]

/**
 * Con che corpo va scritto quel segno. `internal` per i pezzi delle pagine
 * lunghe: vedi `PageEditorViewModel.markerSizeOf`.
 */
fun bulletMarkerSizeFor(indent: Int, chosen: Int? = null): TextUnit =
    BULLET_MARKER_SIZES[chosen?.mod(BULLET_MARKER_SIZES.size) ?: bulletIndexFor(indent)]
