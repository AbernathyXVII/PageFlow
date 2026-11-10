package com.gabriele.notionlocal.ui.screen

import com.gabriele.notionlocal.platform.PlatformContext

/**
 * **L'impronta** (e il blocco schermo del telefono) per le pagine bloccate
 * con la password: sul telefono la finestra di sistema di Android, da Android
 * 11; sul PC, dal 10/11/2026, Windows Hello. Nel codice comune dal 04/11/2026
 * (prima stava in `LockUi`).
 */
expect object Biometrics {
    /** Sul PC è Windows Hello (volto, impronta o PIN): le scritte lo chiamano così. */
    val windowsHello: Boolean

    /** Se si può chiedere l'impronta. */
    fun available(context: PlatformContext): Boolean

    /** Il blocco schermo del telefono (PIN, sequenza, password): serve a rifare una password dimenticata. */
    fun deviceCredentialAvailable(context: PlatformContext): Boolean

    /**
     * Chiede l'impronta (o, con [deviceCredential], il blocco schermo). Una
     * sola delle due risposte arriva: [onSuccess], o [onFallback] se si è
     * toccato "Usa la password", si è annullato o non è andata.
     */
    fun authenticate(
        context: PlatformContext,
        title: String,
        deviceCredential: Boolean,
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    )
}
