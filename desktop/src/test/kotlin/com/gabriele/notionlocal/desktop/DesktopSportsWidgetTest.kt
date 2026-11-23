package com.gabriele.notionlocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.SPORT_CATALOG
import com.gabriele.notionlocal.data.widgets.Sport
import com.gabriele.notionlocal.data.widgets.SportReport
import com.gabriele.notionlocal.data.widgets.SportsWidget
import com.gabriele.notionlocal.data.widgets.WidgetKind
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.buildRounds
import com.gabriele.notionlocal.data.widgets.currentRound
import com.gabriele.notionlocal.data.widgets.parseScoreboard
import com.gabriele.notionlocal.data.widgets.parseStandings
import com.gabriele.notionlocal.data.widgets.teamDisplayName
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import com.gabriele.notionlocal.platform.DesktopContext
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.ui.screen.TeamLogos
import com.gabriele.notionlocal.ui.screen.WidgetsSection
import com.gabriele.notionlocal.ui.screen.notePresses
import com.gabriele.notionlocal.ui.theme.NotionLocalTheme
import com.gabriele.notionlocal.ui.theme.SidebarBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

/**
 * **Il widget dello sport sul PC** (dal 11/11/2026, chiesto dall'utente):
 * sotto il nome della competizione le frecce fra le giornate con "Today" in
 * mezzo, e "Standings" e "Players"; la giornata intera non si illumina più
 * col mouse sopra (e dal 17/11/2026 nemmeno la partita). Con la giornata vera della Serie A
 * salvata per i test del telefono, senza rete.
 */
@OptIn(ExperimentalComposeUiApi::class)
class DesktopSportsWidgetTest {
    private val dir: File = Files.createTempDirectory("pageflow-sports").toFile()
    private val context = DesktopContext(dir)

    @After
    fun tearDown() {
        TeamLogos.enabled = true
        dir.deleteRecursively()
    }

    private fun <T> onWindowThread(block: () -> T): T = runBlocking(Dispatchers.Main) { block() }

    private fun resource(name: String) = File("../app/src/test/resources/sports/$name").readText()

    @Test
    fun theBarUnderTheNameAndNoBigHighlight() {
        AppSettings.init(context)
        // `WidgetStore` è uno solo per tutti i test (vedi `TestWidgets`).
        WidgetStore.init(TestWidgets.context)
        TeamLogos.enabled = false
        WidgetStore.state.widgets.forEach { WidgetStore.remove(it.id) }
        WidgetStore.add(WidgetKind.SPORTS)
        val widgetId = WidgetStore.state.widgets.filterIsInstance<SportsWidget>().last().id
        WidgetStore.addSportCompetition(widgetId, SPORT_CATALOG.first { it.league == "ita.1" }.toCompetition())
        val serieA = WidgetStore.state.widgets.filterIsInstance<SportsWidget>().single().competitions.single()
        val now = System.currentTimeMillis()
        val board = parseScoreboard(resource("seriea_scoreboard.json"), Sport.SOCCER)!!
        val rounds = buildRounds(board, Sport.SOCCER)
        WidgetStore.setSportReport(
            widgetId,
            serieA.id,
            SportReport(
                fetchedAt = now,
                matches = board.matches,
                standings = parseStandings(resource("soccer_ita_1_standings.json"), Sport.SOCCER),
                standingsFetchedAt = now,
                rounds = rounds,
                roundsFetchedAt = now,
                round = currentRound(rounds, LocalDate.of(2026, 10, 17)),
                season = board.season
            )
        )
        val scene = onWindowThread {
            ImageComposeScene(width = 600, height = 1000, density = Density(1f)) {
                CompositionLocalProvider(LocalPlatformContext provides context) {
                    NotionLocalTheme {
                        // Barra chiusa: niente rete nel test.
                        Column(modifier = Modifier.width(300.dp).background(SidebarBackground).notePresses()) { WidgetsSection(active = false) }
                    }
                }
            }
        }
        var time = 0L
        fun frames(count: Int) = repeat(count) {
            onWindowThread { scene.render(time) }
            time += 100_000_000L
            Thread.sleep(30)
        }
        fun picture(): Bitmap = onWindowThread { Bitmap.makeFromImage(scene.render(time)) }
        fun save(bitmap: Bitmap, name: String) =
            File("build/desktop-screenshots", name).apply { parentFile.mkdirs() }.writeBytes(Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        fun mouse(type: PointerEventType, at: Offset, pressed: Boolean = false) = onWindowThread {
            scene.sendPointerEvent(
                type, at, type = PointerType.Mouse,
                buttons = if (pressed) PointerButtons(isPrimaryPressed = true) else PointerButtons(),
                button = if (type == PointerEventType.Move) null else PointerButton.Primary
            )
        }
        fun click(at: Offset) {
            mouse(PointerEventType.Move, at)
            mouse(PointerEventType.Press, at, pressed = true)
            mouse(PointerEventType.Release, at)
            frames(10)
        }
        try {
            frames(30)
            val start = picture()
            save(start, "sports_widget.png")
            val background = start.getColor(4, 200)
            fun ink(bitmap: Bitmap, x: Int, y: Int): Boolean {
                val c = bitmap.getColor(x, y)
                return ((c shr 16) and 0xFF) > 90 && ((c shr 8) and 0xFF) > 90 && (c and 0xFF) > 90
            }
            // Le fasce di righe con qualcosa scritto.
            val rows = (0 until start.height).filter { y -> (8 until 292).any { x -> ink(start, x, y) } }
            val bands = ArrayList<IntRange>()
            for (y in rows) {
                val last = bands.lastOrNull()
                if (last != null && y <= last.last + 1) bands[bands.lastIndex] = last.first..y else bands += y..y
            }
            fun groupsIn(bitmap: Bitmap, band: IntRange): List<IntRange> {
                val columns = (8 until 292).filter { x -> band.any { y -> ink(bitmap, x, y) } }
                val groups = ArrayList<IntRange>()
                for (x in columns) {
                    val last = groups.lastOrNull()
                    if (last != null && x <= last.last + 6) groups[groups.lastIndex] = last.first..x else groups += x..x
                }
                return groups
            }
            // Le fasce: "Widgets", "Sports", la competizione, la riga nuova, il titolo della giornata.
            val bar = bands[3]
            val title = bands[4]
            val words = groupsIn(start, bar)
            assertEquals("sotto il nome: ‹, Today, ›, Standings, Players ($words)", 5, words.size)
            fun center(group: IntRange) = Offset((group.first + group.last) / 2f, (bar.first + bar.last) / 2f)
            fun same(a: Bitmap, b: Bitmap, band: IntRange) = band.all { y -> (8 until 292).all { x -> a.getColor(x, y) == b.getColor(x, y) } }

            // **Sul PC una partita su una riga** (dal 23/11/2026, chiesto dall'utente): le due squadre di una
            // partita della giornata alla stessa altezza, quella di casa a sinistra e l'altra a destra.
            val texts = onWindowThread {
                scene.semanticsOwners.flatMap { it.getAllSemanticsNodes(mergingEnabled = false) }
                    .mapNotNull { node -> node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }?.let { it to node.boundsInRoot } }
            }
            val shown = board.matches.firstOrNull { m -> texts.any { it.first == teamDisplayName(m.home.name) } && texts.any { it.first == teamDisplayName(m.away.name) } }
            assertTrue("una partita della giornata sullo schermo", shown != null)
            val home = texts.first { it.first == teamDisplayName(shown!!.home.name) }.second
            val away = texts.first { it.first == teamDisplayName(shown!!.away.name) }.second
            assertTrue("sulla stessa riga: $home e $away", kotlin.math.abs(home.center.y - away.center.y) < 2f && home.center.x < away.center.x)

            // Col mouse sopra una partita non si illumina niente, né lei né la giornata (dal 17/11/2026,
            // chiesto dall'utente: niente riquadri grigi col mouse sopra, "dappertutto").
            val match = bands.first { it.first > title.last }
            mouse(PointerEventType.Move, Offset(200f, (match.first + match.last) / 2f))
            frames(5)
            val hovered = picture()
            save(hovered, "sports_widget_hover.png")
            assertTrue("il titolo della giornata resta com'era", same(start, hovered, title))
            assertTrue("la partita sotto il mouse non si illumina", same(start, hovered, title.last + 4..match.last))

            // La freccia a destra passa alla giornata dopo, quella a sinistra torna indietro.
            click(center(words[2]))
            frames(10)
            assertTrue("la freccia a destra cambia giornata", !same(start, picture(), title))
            click(center(words[0]))
            frames(10)
            assertTrue("la freccia a sinistra torna alla giornata di prima", same(start, picture(), title))

            // "Standings" apre la classifica.
            click(center(words[3]))
            frames(10)
            val standings = picture()
            save(standings, "sports_widget_standings.png")
            val changedRows = (0 until standings.height step 4).count { y -> (0 until 300 step 4).any { x -> standings.getColor(x, y) != start.getColor(x, y) } }
            assertTrue("si apre la classifica ($changedRows righe cambiate)", changedRows > 60)
            // Solo la classifica, senza le schede "Matches" e "Players" (la scheda scelta era azzurra, con la sua linea).
            val blue = (0 until standings.height).sumOf { y ->
                (0 until standings.width).count { x ->
                    val c = standings.getColor(x, y)
                    val r = (c shr 16) and 0xFF
                    val b = c and 0xFF
                    b > 150 && b - r > 60
                }
            }
            assertTrue("niente schede nel riquadro della classifica ($blue punti azzurri)", blue < 20)
            assertTrue(background != 0)
        } finally {
            onWindowThread { scene.close() }
            WidgetStore.remove(widgetId)
        }
    }
}
