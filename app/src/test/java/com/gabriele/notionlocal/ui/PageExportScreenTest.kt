package com.gabriele.notionlocal.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gabriele.notionlocal.data.export.ExportActions
import com.gabriele.notionlocal.data.export.ExportFormat
import com.gabriele.notionlocal.data.export.ExportOptions
import com.gabriele.notionlocal.data.export.ExportedFile
import com.gabriele.notionlocal.data.export.PageExporter
import com.gabriele.notionlocal.ui.screen.PAGE_EXPORT_TAG
import com.gabriele.notionlocal.ui.screen.PageExportSheet
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

/**
 * **"Export"** nel menu "..." della pagina (02/11/2026): si sceglie il
 * formato, se con le sottopagine, e il file parte verso un'altra app.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-mdpi")
class PageExportScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Aspetta, lasciando lavorare il thread dello schermo: è lì che il file arriva. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(10_000) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
            condition()
        }
    }

    @Test
    fun markdownWithSubpagesIsSharedWithAnotherApp() {
        val file = File(compose.activity.cacheDir, "${PageExporter.EXPORT_DIRECTORY}/Viaggio.zip")
        var asked: ExportOptions? = null
        var shared: ExportedFile? = null
        var closed = false
        compose.setContent {
            NotionLocalTheme {
                PageExportSheet(
                    title = "Viaggio",
                    hasSubpages = true,
                    hasDrawings = true,
                    export = { options ->
                        asked = options
                        ExportedFile(file, PageExporter.ZIP_MIME)
                    },
                    onDismiss = { closed = true },
                    // `FileProvider` sul PC (Windows) non riconosce le sue cartelle:
                    // la scelta delle app si prova sotto, con un indirizzo dato.
                    share = { shared = it }
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(PAGE_EXPORT_TAG).fetchSemanticsNodes().isNotEmpty() }
        // Il PDF è il formato di partenza, e solo lui si stampa e porta i disegni.
        assertTrue(compose.onAllNodesWithText("Print").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Include drawings").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithText("Markdown").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Print").fetchSemanticsNodes().isEmpty() }
        assertTrue(compose.onAllNodesWithText("Include drawings").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Include subpages").performClick()
        compose.onNodeWithText("Share").performClick()
        waitFor { closed }

        assertEquals(ExportOptions(ExportFormat.MARKDOWN, subpages = true, drawings = false), asked)
        assertEquals(file, shared!!.file)
    }

    @Test
    fun drawingsCanBeIncludedInThePdf() {
        val file = File(compose.activity.cacheDir, "${PageExporter.EXPORT_DIRECTORY}/Viaggio.pdf")
        var asked: ExportOptions? = null
        var closed = false
        compose.setContent {
            NotionLocalTheme {
                PageExportSheet(
                    title = "Viaggio",
                    hasSubpages = false,
                    hasDrawings = true,
                    export = { options ->
                        asked = options
                        ExportedFile(file, ExportFormat.PDF.mime)
                    },
                    onDismiss = { closed = true },
                    share = {}
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(PAGE_EXPORT_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Include drawings").performClick()
        compose.onNodeWithText("Share").performClick()
        waitFor { closed }
        assertEquals(ExportOptions(ExportFormat.PDF, subpages = false, drawings = true), asked)
    }

    @Test
    fun theFileGoesToTheOtherAppWithPermissionToReadIt() {
        val exported = ExportedFile(File("Viaggio.zip"), PageExporter.ZIP_MIME)
        val chooser = ExportActions.shareIntent(Uri.parse("content://com.gabriele.notionlocal.files/exports/Viaggio.zip"), exported, "Viaggio")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals(PageExporter.ZIP_MIME, send.type)
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertTrue(uri.toString(), uri.toString().endsWith("/Viaggio.zip"))
        assertEquals("Viaggio", send.getStringExtra(Intent.EXTRA_SUBJECT))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(uri, send.clipData!!.getItemAt(0).uri)
    }
}
