package com.gabriele.notionlocal.data.export

import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.data.applicationContextOf
import com.gabriele.notionlocal.data.entity.BlockEntity
import com.gabriele.notionlocal.data.entity.BlockType
import com.gabriele.notionlocal.data.entity.attrs
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.platform.cacheDirOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * **Prepara il file** di una pagina esportata, nella cartella "exports"
 * della cache dell'app (ogni volta svuotata: tiene solo l'ultimo).
 *
 * In Markdown una pagina con immagini, file, sottopagine o database diventa
 * un .zip, come fa Notion: "Pagina.md" e accanto la cartella "Pagina" con i
 * file, le sottopagine, il CSV di ogni database e le pagine delle sue righe,
 * a cui il testo porta con dei link. Senza, un .md da solo.
 *
 * In PDF le pagine delle righe dei database che hanno qualcosa di scritto
 * (dal 03/11/2026) diventano PDF a sé: allora il file è uno .zip con
 * "Pagina.pdf" e la cartella "Pagina/Database/Riga.pdf".
 *
 * Nel codice comune dal 09/11/2026: sul PC lo stesso, tranne il PDF (vedi
 * [pdfWriter]); mandare, salvare e stampare sono di ogni sistema.
 */
class PageExporter(
    context: PlatformContext,
    private val repository: PageRepository,
    /** Chi legge i database come li mostra la loro vista; null = di un database resta il nome. */
    private val databases: DatabaseSource? = null
) {

    private val appContext = applicationContextOf(context)
    private val store = PageImageStore(appContext)

    suspend fun export(pageId: String, options: ExportOptions, untitled: String): ExportedFile? =
        withContext(Dispatchers.IO) {
            val base = repository.exportPage(pageId, options.subpages) ?: return@withContext null
            // Un database a schermo intero: sotto il suo nome, la sua tabella.
            val shaped = if (base.page.isDatabase) {
                base.copy(blocks = listOf(BlockEntity(id = DATABASE_ROOT_BLOCK, pageId = base.page.id, type = BlockType.DATABASE_LINK, linkedPageId = base.page.id)))
            } else {
                base
            }
            val root = enrich(shaped, options, hashSetOf(pageId), 0, untitled)
            val directory = File(cacheDirOf(appContext), EXPORT_DIRECTORY).apply {
                deleteRecursively()
                mkdirs()
            }
            val name = exportFileName(root.page.title, untitled)
            when (options.format) {
                ExportFormat.PDF -> {
                    val pdf = pdfWriter(appContext, ::fileOf, untitled, options.drawings) ?: return@withContext null
                    val pages = flatten(root)
                    if (options.singleFile || pages.none { page -> page.databases.values.any { db -> db.rows.any { it.page != null } } }) {
                        val file = File(directory, "$name.pdf")
                        FileOutputStream(file).use { out -> pdf.write(pages, out) }
                        ExportedFile(file, ExportFormat.PDF.mime)
                    } else {
                        val file = File(directory, "$name.zip")
                        ZipOutputStream(FileOutputStream(file)).use { zip -> writePdfs(zip, pages, folder = "", name = name, pdf = pdf, untitled = untitled) }
                        ExportedFile(file, ZIP_MIME)
                    }
                }
                ExportFormat.MARKDOWN -> {
                    if (root.subpages.isEmpty() && filesOf(root).isEmpty() && root.databases.isEmpty()) {
                        val file = File(directory, "$name.md")
                        file.writeText(markdownOf(root, untitled))
                        ExportedFile(file, ExportFormat.MARKDOWN.mime)
                    } else {
                        val file = File(directory, "$name.zip")
                        ZipOutputStream(FileOutputStream(file)).use { zip ->
                            writeMarkdown(zip, root, folder = "", name = name, untitled = untitled)
                        }
                        ExportedFile(file, ZIP_MIME)
                    }
                }
            }
        }

    /**
     * La pagina con quello che le serve per l'export oltre alle righe: i
     * bottoni a parole, i database come li mostra la loro vista (con le
     * pagine delle righe che hanno qualcosa, a loro volta complete) e, se
     * richiesti, i disegni. [seen] evita di esportare due volte la stessa
     * pagina in un giro di collegamenti.
     */
    private suspend fun enrich(page: ExportPage, options: ExportOptions, seen: MutableSet<String>, depth: Int, untitled: String): ExportPage {
        val strokes = if (options.drawings && options.format == ExportFormat.PDF) repository.strokesOf(page.page.id) else emptyList()
        val found = LinkedHashMap<String, ExportDatabase>()
        val reader = databases
        if (reader != null && depth < MAX_DEPTH) {
            for (block in page.blocks) {
                if (block.type != BlockType.DATABASE_LINK) continue
                val databaseId = block.linkedPageId ?: continue
                val read = runCatching { reader.read(databaseId) }.getOrNull() ?: continue
                val rows = read.database.rows.mapIndexed { index, row ->
                    val rowPageId = read.rowPageIds.getOrNull(index)
                    if (rowPageId == null || !seen.add(rowPageId)) {
                        row
                    } else {
                        val rowPage = repository.exportPage(rowPageId, options.subpages)?.let { enrich(it, options, seen, depth + 1, untitled) }
                        row.copy(page = rowPage?.takeIf(::hasContent))
                    }
                }
                found[block.id] = read.database.copy(rows = rows)
            }
        }
        return page.copy(
            strokes = strokes,
            buttons = buttonsOf(page, untitled),
            databases = found,
            subpages = page.subpages.map { enrich(it, options, seen, depth + 1, untitled) }
        )
    }

    /** I bottoni della pagina a parole; una copia collegata dice le azioni del bottone vero. */
    private suspend fun buttonsOf(page: ExportPage, untitled: String): Map<String, ExportButton> {
        val result = LinkedHashMap<String, ExportButton>()
        for (block in page.blocks) {
            if (block.type != BlockType.BUTTON) continue
            val attrs = block.attrs
            val config = attrs.button ?: attrs.buttonLink?.let { repository.blockOf(it)?.attrs?.button } ?: continue
            val (pageIds, databaseIds) = buttonReferences(config)
            val names = ButtonNames(repository.titlesOf(pageIds), databaseIds.associateWith { repository.columnsOf(it) })
            result[block.id] = exportButton(config, names, untitled)
        }
        return result
    }

    private fun fileOf(fileName: String): File? = store.fileFor(fileName).takeIf { it.isFile }

    /** I file dei blocchi della pagina che ci sono davvero: le immagini, i video, gli allegati. */
    private fun filesOf(page: ExportPage): List<String> =
        page.blocks.mapNotNull { it.attrs.file }.distinct().filter { fileOf(it) != null }

    /**
     * [pages] in "[folder][name].pdf"; le pagine delle righe dei loro database
     * in "[folder][name]/Database/Riga.pdf", e così via per i database di
     * quelle.
     */
    private fun writePdfs(zip: ZipOutputStream, pages: List<ExportPage>, folder: String, name: String, pdf: PdfWriter, untitled: String) {
        zip.putNextEntry(ZipEntry("$folder$name.pdf"))
        pdf.write(pages, zip)
        zip.closeEntry()
        val used = HashSet<String>()
        for (page in pages) {
            for (database in page.databases.values) {
                val withPages = database.rows.filter { it.page != null }
                if (withPages.isEmpty()) continue
                val databaseName = unique(exportFileName(database.title, untitled), used)
                val rowNames = HashSet<String>()
                for (row in withPages) {
                    val rowName = unique(exportFileName(row.title, untitled) + ".pdf", rowNames).removeSuffix(".pdf")
                    writePdfs(zip, flatten(row.page!!), "$folder$name/$databaseName/", rowName, pdf, untitled)
                }
            }
        }
    }

    /**
     * La pagina [page] in "[folder][name].md", i suoi file, le sottopagine, il
     * CSV dei database e le pagine delle loro righe nella cartella
     * "[folder][name]/". I nomi uguali nella stessa cartella prendono un
     * numero ("foto 2.jpg").
     */
    private fun writeMarkdown(zip: ZipOutputStream, page: ExportPage, folder: String, name: String, untitled: String) {
        val inside = "$name/"
        val used = HashSet<String>()
        val fileLinks = filesOf(page).associateWith { fileName ->
            val attrsName = page.blocks.firstOrNull { it.attrs.file == fileName }?.attrs?.name
            var shown = exportFileName(attrsName ?: fileName, fileName)
            // Il nome com'era sul telefono a volte non ha l'estensione: quella del file salvato.
            if ('.' !in shown && '.' in fileName) shown += fileName.substring(fileName.lastIndexOf('.'))
            unique(shown, used)
        }
        val subpageNames = page.subpages.associate { sub ->
            sub.page.id to unique(exportFileName(sub.page.title, untitled) + ".md", used).removeSuffix(".md")
        }
        // Ogni database: "Nome.csv", e le pagine delle righe in "Nome/".
        val databaseNames = page.databases.mapValues { (_, database) ->
            unique(exportFileName(database.title, untitled) + ".csv", used).removeSuffix(".csv")
        }
        val rowNames = page.databases.mapValues { (_, database) ->
            val usedRows = HashSet<String>()
            database.rows.map { row -> row.page?.let { unique(exportFileName(row.title, untitled) + ".md", usedRows).removeSuffix(".md") } }
        }
        val text = markdownOf(
            page,
            untitled,
            fileLink = { fileLinks[it]?.let { stored -> inside + stored } },
            pageLink = { id -> subpageNames[id]?.let { sub -> "$inside$sub.md" } },
            databaseLinks = { blockId ->
                databaseNames[blockId]?.let { databaseName ->
                    MarkdownDatabaseLinks(
                        csv = "$inside$databaseName.csv",
                        rows = rowNames[blockId].orEmpty().map { row -> row?.let { "$inside$databaseName/$it.md" } }
                    )
                }
            }
        )
        zip.putNextEntry(ZipEntry("$folder$name.md"))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        for ((fileName, stored) in fileLinks) {
            val source = fileOf(fileName) ?: continue
            zip.putNextEntry(ZipEntry("$folder$inside$stored"))
            FileInputStream(source).use { it.copyTo(zip) }
            zip.closeEntry()
        }
        for ((blockId, database) in page.databases) {
            val databaseName = databaseNames.getValue(blockId)
            zip.putNextEntry(ZipEntry("$folder$inside$databaseName.csv"))
            zip.write(csvOf(database).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            database.rows.forEachIndexed { index, row ->
                val rowName = rowNames.getValue(blockId)[index] ?: return@forEachIndexed
                writeMarkdown(zip, row.page!!, "$folder$inside$databaseName/", rowName, untitled)
            }
        }
        for (sub in page.subpages) {
            writeMarkdown(zip, sub, folder + inside, subpageNames.getValue(sub.page.id), untitled)
        }
    }

    companion object {
        const val EXPORT_DIRECTORY = "exports"
        const val ZIP_MIME = "application/zip"

        /** Il blocco finto con cui un database a schermo intero si esporta come la sua tabella. */
        const val DATABASE_ROOT_BLOCK = "__database__"

        /** Quanti livelli di database dentro le pagine delle righe si seguono. */
        private const val MAX_DEPTH = 6

        /** La pagina e poi le sottopagine, in ordine di lettura. */
        fun flatten(page: ExportPage): List<ExportPage> = listOf(page) + page.subpages.flatMap(::flatten)

        /** [name] se nessun altro file nella stessa cartella si chiama così, altrimenti "nome 2.ext", "nome 3.ext"… */
        fun unique(name: String, used: MutableSet<String>): String {
            val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
            var candidate = name
            var n = 2
            while (!used.add(candidate.lowercase())) {
                candidate = name.substring(0, dot) + " " + n++ + name.substring(dot)
            }
            return candidate
        }
    }
}

/** Chi disegna il PDF di una o più pagine (sul telefono `PdfExport`). */
fun interface PdfWriter {
    fun write(pages: List<ExportPage>, out: OutputStream)
}

/** Il PDF di questo sistema, coi disegni a mano se [drawings]; null dove non c'è ancora (il PC). */
expect fun pdfWriter(context: PlatformContext, fileOf: (String) -> File?, untitled: String, drawings: Boolean): PdfWriter?
