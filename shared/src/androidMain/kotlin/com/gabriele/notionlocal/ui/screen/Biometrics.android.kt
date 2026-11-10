package com.gabriele.notionlocal.ui.screen

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import com.gabriele.notionlocal.platform.PlatformContext
import com.gabriele.notionlocal.ui.i18n.LockStrings

/**
 * **L'impronta**, con la finestra di sistema di Android (quella di tutte le
 * app), da Android 11. Sotto non c'è: si usa solo la password.
 */
actual object Biometrics {
    actual val windowsHello: Boolean = false

    actual fun available(context: PlatformContext): Boolean = canAuthenticate(context, BiometricManager.Authenticators.BIOMETRIC_WEAK)

    /** Il blocco schermo del telefono (PIN, sequenza, password): serve a rifare una password dimenticata. */
    actual fun deviceCredentialAvailable(context: PlatformContext): Boolean =
        canAuthenticate(context, BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    private fun canAuthenticate(context: Context, authenticators: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val manager = context.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Chiede l'impronta (o, con `deviceCredential`, il blocco schermo). Una
     * sola delle due risposte arriva: `onSuccess`, o `onFallback` se si è
     * toccato "Usa la password", si è annullato o non è andata.
     */
    actual fun authenticate(
        context: PlatformContext,
        title: String,
        deviceCredential: Boolean,
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    ) {
        // Sotto Android 11 l'impronta non c'è (`available` dice di no), e chi
        // chiedeva non arrivava fin qui: come prima, non succede niente.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        var answered = false
        fun answer(block: () -> Unit) {
            if (answered) return
            answered = true
            block()
        }
        val executor = context.mainExecutor
        val builder = BiometricPrompt.Builder(context).setTitle(title)
        if (deviceCredential) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        } else {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            builder.setNegativeButton(LockStrings.usePassword, executor) { _, _ -> answer(onFallback) }
        }
        builder.build().authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    answer(onSuccess)

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    answer(onFallback)
            }
        )
    }
}
