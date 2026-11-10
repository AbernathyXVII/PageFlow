package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.platform.WinRtScript
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * **Windows Hello per le pagine bloccate** (dal 10/11/2026), come l'impronta
 * sul telefono: il volto, l'impronta o il PIN di Windows, con la sua finestra
 * davanti a quella di PageFlow. Lo chiede uno script PowerShell
 * (`IUserConsentVerifierInterop`, il modo delle app per il desktop); serve
 * anche a rifare una password dimenticata, come il blocco schermo del telefono.
 *
 * Una richiesta lasciata a metà non si chiude da qui (provato: annullarla non
 * chiude la finestra di Windows), quindi si aspetta la risposta dell'utente.
 */
actual object Biometrics {
    actual val windowsHello: Boolean = true

    /** La finestra di PageFlow, a cui Windows Hello attacca la sua (la mette `Main.kt`). */
    @Volatile
    var windowHandle: Long = 0

    @Volatile
    private var ready = false
    private val asking = AtomicBoolean(false)

    /** All'avvio: chiede a Windows, in sottofondo, se Windows Hello è pronto. */
    fun init(context: DesktopContext) {
        thread(isDaemon = true, name = "windows-hello") { ready = check(context) == "Available" }
    }

    /** Cosa risponde Windows: "Available" se Windows Hello è pronto (altrimenti, per esempio, "NotConfiguredForUser"). */
    fun check(context: DesktopContext): String? = WinRtScript.run(context, "hello", SCRIPT, listOf("check"), CHECK_TIMEOUT_S)?.trim()

    actual fun available(context: PlatformContext): Boolean = ready

    actual fun deviceCredentialAvailable(context: PlatformContext): Boolean = ready

    actual fun authenticate(
        context: PlatformContext,
        title: String,
        deviceCredential: Boolean,
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    ) {
        // Una finestra di Windows Hello alla volta: un secondo clic mentre è aperta non ne apre un'altra.
        if (context !is DesktopContext || !asking.compareAndSet(false, true)) return
        thread(isDaemon = true, name = "windows-hello") {
            val answer = try {
                WinRtScript.run(context, "hello", SCRIPT, listOf("ask", windowHandle.toString(), title), ASK_TIMEOUT_S)?.trim()
            } finally {
                asking.set(false)
            }
            EventQueue.invokeLater { if (answer == "Verified") onSuccess() else onFallback() }
        }
    }

    private const val CHECK_TIMEOUT_S = 60L
    private const val ASK_TIMEOUT_S = 15 * 60L

    /**
     * "check": se Windows Hello è pronto; "ask <finestra> <titolo>": lo chiede, e
     * scrive "Verified" se è andata. Il pezzo in C# serve a dare a Windows la
     * finestra di PageFlow; "check" lo compila anche, così il test lo prova.
     */
    private val SCRIPT = """
        [Windows.Security.Credentials.UI.UserConsentVerifier, Windows.Security.Credentials.UI, ContentType = WindowsRuntime] | Out-Null
        Add-Type -TypeDefinition @'
        using System;
        using System.Runtime.InteropServices;
        public static class PageFlowHello {
            [ComImport, Guid("39E050C3-4E74-441A-8DC0-B81104DF949C"), InterfaceType(ComInterfaceType.InterfaceIsIInspectable)]
            private interface IUserConsentVerifierInterop {
                [return: MarshalAs(UnmanagedType.IInspectable)]
                object RequestVerificationForWindowAsync(IntPtr appWindow, [MarshalAs(UnmanagedType.HString)] string message, [In] ref Guid riid);
            }
            [DllImport("combase.dll", PreserveSig = false)]
            private static extern void RoGetActivationFactory([MarshalAs(UnmanagedType.HString)] string id, [In] ref Guid iid, [MarshalAs(UnmanagedType.IInspectable)] out object factory);
            public static object Request(long window, string message, Guid operation) {
                Guid iid = typeof(IUserConsentVerifierInterop).GUID;
                object factory;
                RoGetActivationFactory("Windows.Security.Credentials.UI.UserConsentVerifier", ref iid, out factory);
                return ((IUserConsentVerifierInterop)factory).RequestVerificationForWindowAsync(new IntPtr(window), message, ref operation);
            }
        }
        '@
        ${'$'}result = [Windows.Security.Credentials.UI.UserConsentVerificationResult]
        ${'$'}operation = [Windows.Foundation.IAsyncOperation`1].MakeGenericType(${'$'}result).GUID
        if (${'$'}Arguments[0] -eq 'check') {
            Await ([Windows.Security.Credentials.UI.UserConsentVerifier]::CheckAvailabilityAsync()) ([Windows.Security.Credentials.UI.UserConsentVerifierAvailability])
        } else {
            Await ([PageFlowHello]::Request([long]${'$'}Arguments[1], ${'$'}Arguments[2], ${'$'}operation)) ${'$'}result
        }
    """
}
