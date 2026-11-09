package com.gabriele.notionlocal.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.gabriele.notionlocal.data.dao.BlockDao
import com.gabriele.notionlocal.data.dao.DatabaseDao
import com.gabriele.notionlocal.data.dao.InkDao
import com.gabriele.notionlocal.data.dao.NotebookDao
import com.gabriele.notionlocal.data.dao.PageDao
import com.gabriele.notionlocal.data.dao.PersonalWordDao
import com.gabriele.notionlocal.data.dao.PageEditDao
import com.gabriele.notionlocal.data.dao.PageVersionDao
import com.gabriele.notionlocal.data.dao.TableCellDao
import com.gabriele.notionlocal.data.dao.AutomationDao
import com.gabriele.notionlocal.data.dao.ImageTextDao
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.DatabaseCellEntity
import com.gabriele.notionlocal.data.entity.DatabaseColumnEntity
import com.gabriele.notionlocal.data.entity.DatabaseRowEntity
import com.gabriele.notionlocal.data.entity.InkStrokeEntity
import com.gabriele.notionlocal.data.entity.NotebookEntity
import com.gabriele.notionlocal.data.entity.NotebookSectionEntity
import com.gabriele.notionlocal.data.entity.PageEditEntity
import com.gabriele.notionlocal.data.entity.PageVersionEntity
import com.gabriele.notionlocal.data.entity.PageEntity
import com.gabriele.notionlocal.data.entity.PersonalWordEntity
import com.gabriele.notionlocal.data.entity.TableCellEntity
import com.gabriele.notionlocal.data.entity.AutomationEntity
import com.gabriele.notionlocal.data.entity.ImageTextEntity
import com.gabriele.notionlocal.platform.PlatformContext

/**
 * Il database SQLite locale dell'app. Questo è letteralmente "tutto
 * salvato sul telefono": nessuna chiamata di rete, nessun account,
 * nessun cloud. Il file .db vive dentro la sandbox dell'app su disco.
 *
 * **Da qui in avanti ogni cambio di schema vuole una migrazione vera.**
 * Fino alla versione 5 il database usava fallbackToDestructiveMigration,
 * cioè si ricreava da zero ad ogni cambio di schema, perdendo tutto: una
 * scelta deliberata finché sul telefono c'erano solo dati di prova. Ora
 * che ci sono note vere quella scorciatoia è stata tolta. Se si aggiunge
 * un campo senza scrivere la migrazione corrispondente, l'app si rifiuta
 * di partire — ed è quello che vogliamo: un errore rumoroso è molto
 * meglio delle note cancellate in silenzio.
 *
 * **Dal 04/11/2026 è nel codice comune a telefono e PC** (verso Windows e
 * Mac): qui le tabelle e i DAO, uguali dappertutto. Ad aprire il file ci
 * pensa [buildAppDatabase], diverso per ognuno: sul telefono con tutte le
 * migrazioni della sua storia (`AppDatabase.android.kt`, scritte con
 * l'SQLite di Android), sul PC con l'SQLite messo dentro l'app
 * (`AppDatabase.desktop.kt`), dove il database nasce già alla versione di
 * oggi. **Le migrazioni da qui in avanti** vanno scritte col
 * `SQLiteConnection` di Room, che vale su tutti e due.
 */
/** La versione del database: anche quella che un backup deve avere per aprirsi sul PC (vedi `Backups` del PC). */
const val DATABASE_VERSION = 70

@Database(
    entities = [
        PageEntity::class,
        BlockEntity::class,
        DatabaseColumnEntity::class,
        DatabaseRowEntity::class,
        DatabaseCellEntity::class,
        TableCellEntity::class,
        PageEditEntity::class,
        InkStrokeEntity::class,
        NotebookEntity::class,
        NotebookSectionEntity::class,
        PersonalWordEntity::class,
        PageVersionEntity::class,
        AutomationEntity::class,
        ImageTextEntity::class
    ],
    version = DATABASE_VERSION,
    exportSchema = false
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pageDao(): PageDao
    abstract fun pageEditDao(): PageEditDao
    abstract fun pageVersionDao(): PageVersionDao
    abstract fun automationDao(): AutomationDao
    abstract fun imageTextDao(): ImageTextDao
    abstract fun blockDao(): BlockDao
    abstract fun databaseDao(): DatabaseDao
    abstract fun tableCellDao(): TableCellDao
    abstract fun inkDao(): InkDao
    abstract fun notebookDao(): NotebookDao
    abstract fun personalWordDao(): PersonalWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Solo per i test: chiude il database e lo dimentica, così il
         * `getInstance` dopo riapre il file da capo (e ne esegue le
         * migrazioni). Senza, il secondo test troverebbe il database del
         * primo, già chiuso.
         */
        fun resetForTests() = closeInstance()

        /**
         * Chiude il database e lo dimentica: lo usa anche il ripristino di
         * un backup (dal 28/10/2026), che sostituisce il file prima che
         * l'app lo riapra (vedi `Backups.applyPendingRestore`).
         */
        fun closeInstance() = synchronized(this) {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getInstance(context: PlatformContext): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildAppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}

/** Il costruttore del database: lo scrive Room da sé, per ogni piattaforma. */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * Apre il file del database: sul telefono quello di sempre, con le sue
 * migrazioni; sul PC quello nella cartella dei dati di PageFlow.
 */
expect fun buildAppDatabase(context: PlatformContext): AppDatabase
