package com.gabriele.notionlocal.ui

import android.app.Application
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.platform.rememberSaveCopy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * **"Salva una copia"** di un blocco-file (dal 09/11/2026): il "Salva in…" di
 * sistema riceve il nome proposto, e il file del blocco finisce dove l'utente
 * ha scelto. Il "Salva in…" qui è finto: risponde subito con un file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SaveCopyTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theBlockFileLandsWhereTheUserChose() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = PageImageStore(app)
        val (fileName, file) = store.newFile("txt")
        file.writeText("ciao")
        val target = File(app.cacheDir, "copia.txt")
        var proposed: Any? = null
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                proposed = input
                dispatchResult(requestCode, Uri.fromFile(target))
            }
        }
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        var done: Boolean? = null
        lateinit var save: (String, String) -> Unit
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                save = rememberSaveCopy(store, "text/plain") { done = it }
            }
        }

        compose.runOnIdle { save(fileName, "nota.txt") }
        compose.waitUntil(5_000) { done != null }

        assertEquals("nota.txt", proposed)
        assertEquals(true, done)
        assertEquals("ciao", target.readText())
    }
}
