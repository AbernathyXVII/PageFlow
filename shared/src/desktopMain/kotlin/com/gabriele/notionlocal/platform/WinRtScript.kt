package com.gabriele.notionlocal.platform

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * **Uno script PowerShell che usa Windows** (dal 10/11/2026): il lettore di
 * testo delle foto, Windows Hello. Le API di Windows (WinRT) si chiamano da
 * PowerShell senza librerie in più; qui c'è il pezzo che serve a tutti, per
 * aspettare le loro risposte (`Await`). Lo script va nella cartella dei dati
 * e gli argomenti passano come testo, mai eseguiti.
 */
object WinRtScript {

    /**
     * Esegue [body] (dopo l'aiuto `Await`) con [args]; quello che scrive, o
     * null se PowerShell non risponde in [timeoutSeconds] o finisce con un errore.
     * L'uscita va in un file a parte: leggendola mentre arriva, un PowerShell
     * fermo avrebbe fermato anche l'attesa.
     */
    fun run(context: DesktopContext, name: String, body: String, args: List<String>, timeoutSeconds: Long = 120): String? {
        val output = runCatching { File.createTempFile("pageflow-$name", ".txt") }.getOrNull() ?: return null
        return try {
            val script = File(context.dataDir, "$name.ps1").apply { writeText(HEADER + "\n" + body.trimIndent(), Charsets.UTF_8) }
            val process = ProcessBuilder(
                listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.path) + args
            ).redirectOutput(output).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                null
            } else {
                output.readText(Charsets.UTF_8).takeIf { process.exitValue() == 0 }
            }
        } catch (e: Exception) {
            null
        } finally {
            output.delete()
        }
    }

    /** Gli argomenti in `Arguments`, l'uscita in UTF-8, e `Await` per le operazioni di Windows. */
    private val HEADER = """
        param([Parameter(ValueFromRemainingArguments = ${'$'}true)][string[]]${'$'}Arguments)
        ${'$'}ErrorActionPreference = 'Stop'
        [Console]::OutputEncoding = [Text.Encoding]::UTF8
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        ${'$'}asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
            ${'$'}_.Name -eq 'AsTask' -and ${'$'}_.GetParameters().Count -eq 1 -and ${'$'}_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
        } | Select-Object -First 1
        function Await(${'$'}operation, [Type]${'$'}type) {
            ${'$'}task = ${'$'}asTask.MakeGenericMethod(${'$'}type).Invoke(${'$'}null, @(${'$'}operation))
            ${'$'}task.Wait(-1) | Out-Null
            ${'$'}task.Result
        }
    """.trimIndent()
}
