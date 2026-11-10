package com.gabriele.notionlocal.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * **Una versione salvata di una pagina** (dal 02/11/2026, chiesto
 * dall'utente: "Cronologia con ripristino"): com'era la pagina in quel
 * momento, da rimettere con "Page history". "Updates" (`PageEditEntity`)
 * racconta le modifiche una per una; questa è la pagina intera.
 *
 * Il contenuto (`VersionContent`: titolo, blocchi, tabelle, disegni) sta in
 * [content], JSON compresso: una pagina di settecento righe sono pochi
 * kilobyte. Accanto, quello che serve all'elenco senza aprirlo: il titolo
 * di allora, quante righe e quante parole. [hash] è l'impronta del
 * contenuto: una versione uguale all'ultima non si salva.
 *
 * Verso la pagina il vincolo è a cascata, come per "Updates": cancellata
 * per sempre la pagina, le sue versioni non le legge più nessuno. Nel
 * cestino invece restano.
 */
@Suppress("ArrayInDataClass")
@Entity(
    tableName = "page_versions",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageId")]
)
data class PageVersionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageId: String,
    /** Quando è stata salvata, in millisecondi: la pagina era così in quel momento. */
    val at: Long,
    val reason: VersionReason,
    val title: String,
    /** Quanti blocchi (le righe della pagina, anche quelle dentro i toggle). */
    val lines: Int,
    val words: Int,
    val hash: String,
    val content: ByteArray
)

/** Perché è nata una versione: all'elenco serve solo "prima di un ripristino". */
enum class VersionReason {
    /** Aprendo la pagina: com'era prima di metterci mano. */
    OPENED,

    /** Ogni dieci minuti con la pagina aperta, se è cambiata. */
    WHILE_OPEN,

    /** Uscendo dalla pagina, o dall'app. */
    CLOSED,

    /** Com'era subito prima di rimettere un'altra versione: per tornarci. */
    BEFORE_RESTORE,

    /** Com'era qui prima che la sincronizzazione ci mettesse quella dell'altro lato (dal 10/11/2026). */
    BEFORE_SYNC
}

/** Una versione senza il contenuto: quello che mostra l'elenco. */
data class PageVersionInfo(
    val id: String,
    val pageId: String,
    val at: Long,
    val reason: VersionReason,
    val title: String,
    val lines: Int,
    val words: Int
)

/** Per decidere quali versioni tenere: vedi `VersionRetention`. */
data class VersionStamp(val id: String, val at: Long)
