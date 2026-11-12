package com.gabriele.notionlocal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.repository.PageRepository
import com.gabriele.notionlocal.data.security.AppLock
import com.gabriele.notionlocal.ui.i18n.LockStrings
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.theme.DarkBackground
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import com.gabriele.notionlocal.platform.LocalPlatformContext

/*
 * **Le pagine bloccate con la password** (dal 26/09/2026): il cancello
 * davanti a una pagina o a un database bloccato, la richiesta di impronta o
 * password, la scelta della password. I conti stanno in `AppLock`.
 */

private object Checking

/**
 * **L'ultima risposta del cancello, pagina per pagina** (dal 28/09/2026),
 * con la versione delle protezioni a cui valeva (`AppLock.protectionVersion`).
 *
 * Dentro una pagina ogni database passa dal cancello, e finché la domanda
 * al database non tornava il cancello non disegnava niente. Scorrendo la
 * pagina un database che usciva dallo schermo veniva smontato, e al
 * rientro per un attimo era alto zero, poi di colpo alto quanto la sua
 * tabella: la pagina teneva fermo il suo bordo di sopra e tutto il resto
 * saltava giù — risalendo si scavalcava un database intero (trovato
 * dall'utente). Con la risposta di prima il database torna subito alto
 * com'è. Se nel frattempo una protezione è cambiata, la versione non torna
 * e si richiede da capo, come prima: niente si vede che non debba.
 */
private val knownLocks = mutableMapOf<String, Pair<Int, String?>>()

/**
 * **Il cancello**: se la pagina (o chi la contiene) è bloccata con la
 * password e non è aperta, al posto del contenuto c'è la richiesta di
 * sbloccarla. `fullScreen` per una schermata intera — che fa anche
 * richiudere le pagine bloccate che non c'entrano (`AppLock.keepOnlyUnlocked`)
 * —, altrimenti un riquadro al posto di un database dentro una pagina.
 */
@Composable
fun LockGate(
    pageId: String,
    repository: PageRepository,
    fullScreen: Boolean,
    isDatabase: Boolean = false,
    onBack: (() -> Unit)? = null,
    /** Il titolo al posto di "This page is locked": una sezione di un quaderno dice la sua. */
    lockedTitle: String? = null,
    content: @Composable () -> Unit
) {
    val version = AppLock.protectionVersion
    val known = knownLocks[pageId]?.takeIf { it.first == version }
    val lockingId by produceState<Any?>(if (known != null) known.second else Checking, pageId, version) {
        value = repository.lockingPageOf(pageId).also { knownLocks[pageId] = version to it }
    }
    LaunchedEffect(lockingId) {
        if (fullScreen && lockingId !== Checking) AppLock.keepOnlyUnlocked(lockingId as String?)
    }
    val closedBy = lockingId as? String
    when {
        // Finché non si sa, niente: il contenuto non deve fare in tempo a vedersi.
        lockingId === Checking -> if (fullScreen) Box(Modifier.fillMaxSize().background(DarkBackground))
        closedBy == null || AppLock.isUnlocked(closedBy) -> content()
        else -> LockedView(
            fullScreen = fullScreen,
            isDatabase = isDatabase,
            lockedTitle = lockedTitle,
            onBack = onBack,
            onUnlocked = { AppLock.unlock(closedBy) }
        )
    }
}

@Composable
private fun LockedView(
    fullScreen: Boolean,
    isDatabase: Boolean,
    lockedTitle: String?,
    onBack: (() -> Unit)?,
    onUnlocked: () -> Unit
) {
    val context = LocalPlatformContext.current
    var prompting by remember { mutableStateOf(false) }
    // **A schermo intero la richiesta parte da sola**, ma solo quando la
    // pagina è davvero quella davanti (RESUMED). Tornando indietro da una
    // pagina aperta, quella che se ne va si richiude mentre scivola via
    // (`keepOnlyUnlocked` della pagina dove si arriva), e senza questo
    // controllo chiedeva l'impronta da una pagina che non c'era già più:
    // visto sul telefono il 26/09/2026.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateAsState()
    var autoPrompted by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycleState) {
        if (fullScreen && !autoPrompted && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
            autoPrompted = true
            prompting = true
        }
    }
    val withFingerprint = AppLock.biometricEnabled && Biometrics.available(context)
    val title = lockedTitle ?: if (isDatabase) LockStrings.lockedDatabase else LockStrings.lockedTitle

    if (fullScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = NotionWhite, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (withFingerprint) LockStrings.lockedText else LockStrings.lockedTextPasswordOnly,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotionGray400,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { prompting = true }) {
                    if (withFingerprint) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(LockStrings.unlock)
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { prompting = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = NotionGray400)
            Text(title, color = NotionWhite, modifier = Modifier.weight(1f))
            Text(LockStrings.unlock, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (prompting) {
        UnlockPrompt(
            onUnlocked = {
                prompting = false
                onUnlocked()
            },
            onDismiss = { prompting = false }
        )
    }
}

/**
 * **Chiede di sbloccare**: prima l'impronta, se c'è e la si vuole; se no, o
 * se si tocca "Usa la password", la password. Da lì, "Password
 * dimenticata?" chiede il blocco schermo del telefono e poi una password
 * nuova.
 */
@Composable
fun UnlockPrompt(onUnlocked: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalPlatformContext.current
    val fingerprint = AppLock.biometricEnabled && Biometrics.available(context)
    // 0 impronta, 1 password, 2 password nuova dopo il blocco schermo
    var step by remember { mutableStateOf(if (fingerprint) 0 else 1) }

    fun askFingerprint() {
        Biometrics.authenticate(
            context, LockStrings.unlock, deviceCredential = false,
            onSuccess = onUnlocked,
            onFallback = { step = 1 }
        )
    }

    LaunchedEffect(Unit) { if (step == 0) askFingerprint() }

    when (step) {
        1 -> PasswordDialog(
            onUnlocked = onUnlocked,
            onDismiss = onDismiss,
            onUseFingerprint = if (fingerprint) {
                {
                    step = 0
                    askFingerprint()
                }
            } else {
                null
            },
            onForgot = if (Biometrics.deviceCredentialAvailable(context)) {
                {
                    Biometrics.authenticate(
                        context, LockStrings.confirmItsYou, deviceCredential = true,
                        onSuccess = { step = 2 },
                        onFallback = {}
                    )
                }
            } else {
                null
            }
        )
        2 -> SetPasswordDialog(
            subtitle = LockStrings.newPasswordAfterCheck,
            onSaved = onUnlocked,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PasswordDialog(
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
    onUseFingerprint: (() -> Unit)?,
    onForgot: (() -> Unit)?
) {
    var password by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    fun tryUnlock() {
        if (AppLock.checkPassword(password)) onUnlocked() else wrong = true
    }
    FitAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text(LockStrings.unlock) },
        text = {
            Column {
                AutoCaps {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            wrong = false
                        },
                        label = { Text(LockStrings.password) },
                        singleLine = true,
                        isError = wrong,
                        supportingText = if (wrong) {
                            { Text(LockStrings.wrongPassword) }
                        } else {
                            null
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (onUseFingerprint != null) {
                    TextButton(onClick = onUseFingerprint) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(LockStrings.useFingerprint)
                    }
                }
                if (onForgot != null) {
                    TextButton(onClick = onForgot) { Text(LockStrings.forgotPassword) }
                }
            }
        },
        confirmButton = { TextButton(onClick = ::tryUnlock, enabled = password.isNotEmpty()) { Text(LockStrings.unlock) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * **La password delle pagine bloccate**, scelta la prima volta che se ne
 * blocca una (o rifatta): due volte, almeno quattro caratteri, e
 * l'interruttore dell'impronta se il telefono ce l'ha.
 */
@Composable
fun SetPasswordDialog(
    subtitle: String? = null,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalPlatformContext.current
    val fingerprintAvailable = Biometrics.available(context)
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf(AppLock.biometricEnabled) }
    var tried by remember { mutableStateOf(false) }
    val tooShort = first.length < MIN_PASSWORD_LENGTH
    val differ = first != second
    FitAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text(LockStrings.setPasswordTitle) },
        text = {
            Column {
                Text(subtitle ?: LockStrings.setPasswordText, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                AutoCaps {
                    OutlinedTextField(
                        value = first,
                        onValueChange = { first = it },
                        label = { Text(LockStrings.password) },
                        singleLine = true,
                        isError = tried && tooShort,
                        supportingText = if (tried && tooShort) {
                            { Text(LockStrings.passwordTooShort) }
                        } else {
                            null
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                AutoCaps {
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it },
                        label = { Text(LockStrings.repeatPassword) },
                        singleLine = true,
                        isError = tried && !tooShort && differ,
                        supportingText = if (tried && !tooShort && differ) {
                            { Text(LockStrings.passwordsDiffer) }
                        } else {
                            null
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (fingerprintAvailable) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fingerprint = !fingerprint },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                        Spacer(Modifier.size(12.dp))
                        Text(LockStrings.alsoFingerprint, modifier = Modifier.weight(1f))
                        Switch(checked = fingerprint, onCheckedChange = { fingerprint = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tried = true
                if (!tooShort && !differ) {
                    AppLock.setPassword(first)
                    AppLock.biometricEnabled = fingerprint
                    onSaved()
                }
            }) { Text(Strings.done) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } }
    )
}

/**
 * **"Blocca con password" dai tre puntini**: bloccare vuole una password
 * (la prima volta si sceglie), sbloccare vuole di nuovo impronta o
 * password — chi ha il telefono in mano con la pagina aperta non deve
 * poterla sbloccare per sempre con un tocco.
 */
@Composable
fun PasswordLockFlow(wanted: Boolean, onFinished: () -> Unit, apply: (Boolean) -> Unit) {
    if (wanted) {
        if (AppLock.hasPassword) {
            LaunchedEffect(Unit) {
                apply(true)
                onFinished()
            }
        } else {
            SetPasswordDialog(
                onSaved = {
                    apply(true)
                    onFinished()
                },
                onDismiss = onFinished
            )
        }
    } else {
        UnlockPrompt(
            onUnlocked = {
                apply(false)
                onFinished()
            },
            onDismiss = onFinished
        )
    }
}

private const val MIN_PASSWORD_LENGTH = 4
