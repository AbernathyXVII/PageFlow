package com.gabriele.notionlocal.platform

import com.gabriele.notionlocal.ui.screen.Biometrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * **La finestra dei file del sistema di adesso** (dal 12/11/2026, chiesto
 * dall'utente): su Windows quella di Windows 10 e 11, come in Esplora file.
 * Il `FileDialog` di Java su Windows apre quella vecchia ("Look in", "Object
 * name"); sul Mac apre già quella di macOS, e lì resta. Quella nuova si apre
 * da PowerShell (`System.Windows.Forms`, che chiede quella di Windows), legata
 * alla finestra di PageFlow come le sue finestre; se PowerShell non risponde,
 * torna quella di Java. PowerShell ci mette circa un secondo ad aprirla.
 */
object SystemFileDialog {
    private val windows = System.getProperty("os.name").orEmpty().lowercase().contains("win")

    /** Un file da aprire, coi soli [extensions] (senza punto; vuoto = tutti), o null. */
    suspend fun open(title: String, extensions: Set<String> = emptySet(), directory: String? = null): File? =
        choose(save = false, title, extensions, name = null, directory)

    /** Dove salvare un file che si chiama [name], o null. */
    suspend fun save(title: String, name: String, directory: String? = null): File? =
        choose(save = true, title, emptySet(), name, directory)

    /** Il filtro di Windows: "*.jpg, *.png|*.jpg;*.png". */
    fun windowsFilter(extensions: Set<String>): String? =
        extensions.takeIf { it.isNotEmpty() }?.let { exts -> exts.joinToString(", ") { "*.$it" } + "|" + exts.joinToString(";") { "*.$it" } }

    /**
     * Gli argomenti dello script, ognuno con "=" davanti: PowerShell perde quelli
     * vuoti, e uno che comincia con "-" (anche il nome di un file) lo prende per
     * un suo comando e si ferma. "=" da solo vuol dire "niente".
     */
    fun arguments(mode: String, title: String, extensions: Set<String>, name: String?, directory: String?, owner: Long) =
        listOf(mode, title, windowsFilter(extensions).orEmpty(), name.orEmpty(), directory.orEmpty(), owner.toString()).map { "=$it" }

    /** Vero se PowerShell sa preparare la finestra di Windows (senza aprirla): per i test. */
    fun canPrepare(extensions: Set<String>, name: String = "a.txt"): Boolean =
        windows && run(arguments("check", "PageFlow", extensions, name, null, 0))?.trim() == "ok"

    private suspend fun choose(save: Boolean, title: String, extensions: Set<String>, name: String?, directory: String?): File? {
        if (windows) {
            val args = arguments(if (save) "save" else "open", title, extensions, name, directory, Biometrics.windowHandle)
            val answer = withContext(Dispatchers.IO) { run(args) }
            if (answer != null) return answer.trim().takeIf { it.isNotEmpty() }?.let(::File)
        }
        val dialog = FileDialog(null as Frame?, title, if (save) FileDialog.SAVE else FileDialog.LOAD).apply {
            directory?.let { this.directory = it }
            name?.let { file = it }
            if (extensions.isNotEmpty()) setFilenameFilter { _, file -> file.substringAfterLast('.', "").lowercase() in extensions }
            isVisible = true
        }
        return dialog.file?.let { File(dialog.directory, it) }
    }

    // La finestra resta aperta finché l'utente sceglie: un giorno basta.
    private fun run(args: List<String>): String? =
        WinRtScript.run(DesktopContext(File(System.getProperty("java.io.tmpdir"), "pageflow-dialogs")), "file-dialog", SCRIPT, args, timeoutSeconds = 24 * 3600)

    private val SCRIPT = """
        Add-Type -AssemblyName System.Windows.Forms
        # Nitida anche con lo schermo ingrandito (125%, 150%…): senza, Windows la ingrandisce sfocata.
        try {
            Add-Type -MemberDefinition '[DllImport("shcore.dll")] public static extern int SetProcessDpiAwareness(int v);' -Name Dpi -Namespace PageFlow
            [void][PageFlow.Dpi]::SetProcessDpiAwareness(2)
        } catch {}
        [System.Windows.Forms.Application]::EnableVisualStyles()
        ${'$'}mode, ${'$'}title, ${'$'}filter, ${'$'}name, ${'$'}folder, ${'$'}owner = ${'$'}Arguments | ForEach-Object { ${'$'}_.Substring(1) }
        ${'$'}dialog = if (${'$'}mode -eq 'save') { New-Object System.Windows.Forms.SaveFileDialog } else { New-Object System.Windows.Forms.OpenFileDialog }
        ${'$'}dialog.Title = ${'$'}title
        if (${'$'}filter) { ${'$'}dialog.Filter = ${'$'}filter }
        if (${'$'}name) { ${'$'}dialog.FileName = ${'$'}name }
        if (${'$'}folder) { ${'$'}dialog.InitialDirectory = ${'$'}folder }
        if (${'$'}mode -eq 'check') { 'ok'; exit 0 }
        ${'$'}window = New-Object System.Windows.Forms.NativeWindow
        if ([long]${'$'}owner -ne 0) { ${'$'}window.AssignHandle([IntPtr][long]${'$'}owner) }
        try { ${'$'}result = ${'$'}dialog.ShowDialog(${'$'}window) } finally { if ([long]${'$'}owner -ne 0) { ${'$'}window.ReleaseHandle() } }
        if (${'$'}result -eq [System.Windows.Forms.DialogResult]::OK) { ${'$'}dialog.FileName }
    """
}
