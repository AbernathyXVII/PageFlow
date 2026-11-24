package com.gabriele.notionlocal.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.ui.screen.PAGE_SCROLL_INDICATOR_TAG
import com.gabriele.notionlocal.ui.screen.PageScrollIndicator
import com.gabriele.notionlocal.ui.screen.estimateExtent
import com.gabriele.notionlocal.ui.screen.scrollThumb
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **La barretta di scorrimento della pagina** (20/10/2026): compare mentre la
 * pagina scorre, svanisce poco dopo, e sta dove si è nella pagina.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h800dp-xhdpi")
class PageScrollIndicatorTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theUnseenItemsCountAsTheAverage() {
        // Viste la 0 (100) e la 1 (300): le altre otto valgono 200.
        val (scrolled, total) = estimateExtent(mapOf(0 to 100, 1 to 300), count = 10, firstIndex = 3, firstOffset = 50)
        assertEquals(100f + 300f + 200f + 50f, scrolled)
        assertEquals(400f + 8 * 200f, total)
    }

    @Test
    fun theThumbIsAsTallAsTheVisiblePartAndFollowsTheScroll() {
        // Si vede un quarto della pagina: la barretta è un quarto della corsia.
        val half = scrollThumb(viewport = 1000f, scrolled = 1500f, total = 4000f, track = 1000f, minThumb = 50f, atStart = false, atEnd = false)
        assertEquals(375f to 250f, half)
        // Mai più corta del minimo, e in fondo tocca il bordo.
        val end = scrollThumb(viewport = 1000f, scrolled = 98_000f, total = 100_000f, track = 1000f, minThumb = 50f, atStart = false, atEnd = true)
        assertEquals(950f to 50f, end)
        // In cima, anche se la stima dice altro.
        assertEquals(0f, scrollThumb(1000f, 30f, 4000f, 1000f, 50f, atStart = true, atEnd = false)!!.first)
        // Una pagina che sta tutta nello schermo non ha barretta.
        assertNull(scrollThumb(viewport = 1000f, scrolled = 0f, total = 900f, track = 1000f, minThumb = 50f, atStart = true, atEnd = true))
    }

    @Test
    fun itShowsWhileScrollingAndFadesAfter() {
        compose.setContent {
            NotionLocalTheme {
                val list = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("page")) {
                        items(100) { i -> Text("riga $i", modifier = Modifier.fillMaxWidth().height(40.dp)) }
                    }
                    PageScrollIndicator(list, modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
        compose.waitForIdle()
        // Ferma: niente barretta.
        compose.onNodeWithTag(PAGE_SCROLL_INDICATOR_TAG).assertDoesNotExist()

        // Il tempo va avanti a mano, se no la barretta svanirebbe prima di guardarla.
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("page").performTouchInput { swipeUp(durationMillis = 400) }
            compose.mainClock.advanceTimeBy(200)
            compose.onNodeWithTag(PAGE_SCROLL_INDICATOR_TAG).assertExists()
            // Poco dopo che la pagina si è fermata, se ne va.
            compose.mainClock.advanceTimeBy(5_000)
            compose.onNodeWithTag(PAGE_SCROLL_INDICATOR_TAG).assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
        }
    }

    /**
     * **Sul telefono la barretta si trascina col dito** (24/11/2026, trovato dall'utente nel collaudo): mentre si
     * vede, trascinata in giù porta giù la pagina; sparita, un tocco sul bordo destro arriva alla riga sotto.
     */
    @Test
    fun onThePhoneTheBarCanBeDraggedWithAFinger() {
        lateinit var list: LazyListState
        var clicks = 0
        compose.setContent {
            NotionLocalTheme {
                list = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("page")) {
                        items(100) { i -> Text("riga $i", modifier = Modifier.fillMaxWidth().height(40.dp).clickable { clicks++ }) }
                    }
                    PageScrollIndicator(list, modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
        compose.waitForIdle()
        // Sparita: il bordo destro è della pagina.
        compose.onRoot().performTouchInput { click(Offset(width - 8f, 60f)) }
        compose.waitForIdle()
        assertEquals("il tocco sul bordo arriva alla riga", 1, clicks)

        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("page").performTouchInput { swipeUp(durationMillis = 400) }
            compose.mainClock.advanceTimeBy(200)
            val before = list.firstVisibleItemIndex
            compose.onRoot().performTouchInput {
                // Dov'è la barretta adesso: alta un quinto della corsia, scesa quanto la pagina.
                val item = height / 20f
                val scrolled = list.firstVisibleItemIndex * item + list.firstVisibleItemScrollOffset
                val margin = 4.dp.toPx()
                val track = height - 2 * margin
                val length = track / 5f
                val top = margin + (track - length) * (scrolled / (100 * item - height))
                down(Offset(width - 8f, top + length / 2f))
                repeat(10) { moveBy(Offset(0f, 30f)) }
                up()
            }
            compose.mainClock.advanceTimeBy(100)
            assertTrue("la pagina è scesa con la barretta: da $before a ${list.firstVisibleItemIndex}", list.firstVisibleItemIndex > before + 10)
            assertEquals("nessuna riga toccata trascinando", 1, clicks)
        } finally {
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
        }
    }
}
