package com.gabriele.notionlocal.desktop

import com.gabriele.notionlocal.platform.SystemFileDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * **Sul PC la finestra dei file di Windows 11** (12/11/2026, chiesto
 * dall'utente al posto di quella vecchia di Java): il filtro dei tipi e gli
 * argomenti dello script, e PowerShell che la prepara davvero — senza aprirla,
 * che comparirebbe sullo schermo.
 */
class DesktopFileDialogTest {

    @Test
    fun theFilterAndTheArguments() {
        assertEquals("*.jpg, *.png|*.jpg;*.png", SystemFileDialog.windowsFilter(linkedSetOf("jpg", "png")))
        assertNull(SystemFileDialog.windowsFilter(emptySet()))
        assertEquals(
            listOf("=open", "=Upload", "=*.zip|*.zip", "=", "=", "=0"),
            SystemFileDialog.arguments("open", "Upload", setOf("zip"), null, null, 0)
        )
        assertEquals(
            listOf("=save", "=Save", "=", "=-Pagina.pdf", "=C:\\Backup", "=42"),
            SystemFileDialog.arguments("save", "Save", emptySet(), "-Pagina.pdf", "C:\\Backup", 42)
        )
    }

    @Test
    fun powerShellPreparesTheWindowsDialog() {
        assumeTrue(System.getProperty("os.name").orEmpty().lowercase().contains("win"))
        assertTrue("con un filtro", SystemFileDialog.canPrepare(linkedSetOf("jpg", "jpeg", "png")))
        assertTrue("senza filtro", SystemFileDialog.canPrepare(emptySet()))
        // Un nome che comincia con "-": PowerShell lo prendeva per un suo comando.
        assertTrue("nome con il trattino", SystemFileDialog.canPrepare(emptySet(), "-note.pdf"))
    }
}
