package com.gabriele.notionlocal.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriele.notionlocal.data.settings.AppLanguage
import com.gabriele.notionlocal.data.settings.AppSettings
import com.gabriele.notionlocal.data.widgets.CatalogEntry
import com.gabriele.notionlocal.data.widgets.MAX_SPORT_COMPETITIONS
import com.gabriele.notionlocal.data.widgets.MatchSide
import com.gabriele.notionlocal.data.widgets.MatchState
import com.gabriele.notionlocal.data.widgets.SPORT_CATALOG
import com.gabriele.notionlocal.data.widgets.Sport
import com.gabriele.notionlocal.data.widgets.SportCompetition
import com.gabriele.notionlocal.data.widgets.SportMatch
import com.gabriele.notionlocal.data.widgets.SportReport
import com.gabriele.notionlocal.data.widgets.SeasonInfo
import com.gabriele.notionlocal.data.widgets.SportRound
import com.gabriele.notionlocal.data.widgets.SportsUpdates
import com.gabriele.notionlocal.data.widgets.SportsWidget
import com.gabriele.notionlocal.data.widgets.StandingGroup
import com.gabriele.notionlocal.data.widgets.StandingRow
import com.gabriele.notionlocal.data.widgets.StandingZone
import com.gabriele.notionlocal.data.widgets.standingZoneLabel
import com.gabriele.notionlocal.data.widgets.standingZoneOf
import com.gabriele.notionlocal.data.widgets.WidgetStore
import com.gabriele.notionlocal.data.widgets.competitionFlag
import com.gabriele.notionlocal.data.widgets.currentRound
import com.gabriele.notionlocal.data.widgets.seasonLabel
import com.gabriele.notionlocal.data.widgets.seasonYears
import com.gabriele.notionlocal.data.widgets.hasMatchdayNumbers
import com.gabriele.notionlocal.data.widgets.teamDisplayName
import com.gabriele.notionlocal.data.widgets.todayIn
import com.gabriele.notionlocal.data.widgets.todayRound
import com.gabriele.notionlocal.ui.format.Formats
import com.gabriele.notionlocal.ui.i18n.Strings
import com.gabriele.notionlocal.ui.i18n.WidgetStrings
import com.gabriele.notionlocal.ui.theme.DarkSheet
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import com.gabriele.notionlocal.platform.LocalPlatformContext
import com.gabriele.notionlocal.platform.cacheDirOf

// ---------------------------------------------------------------------------
// **Il widget dello sport** (dal 17/10/2026, chiesto dall'utente). In alto la
// competizione, con la bandiera, e le frecce per passare alle altre (grigie
// alla prima e all'ultima). Sotto una giornata intera, con gli stemmi delle
// squadre; scorrendo, le giornate prima e dopo. Un tocco apre le partite
// della giornata e la classifica; tenendo premuto si scelgono le
// competizioni. Vedi `SportsWidget`.
// ---------------------------------------------------------------------------

/** Il rosso delle partite in corso. */
private val LiveRed = Color(0xFFE5484D)

@Composable
fun SportsView(
    widget: SportsWidget,
    active: Boolean,
    now: Long,
    managing: Boolean,
    onManagingChange: (Boolean) -> Unit
) {
    var adding by remember(widget.id) { mutableStateOf(false) }
    var pickingSeason by remember(widget.id) { mutableStateOf(false) }

    // Aprendo la barra si vede la prima competizione, nella stagione in corso.
    LaunchedEffect(widget.id, active) {
        if (active) {
            widget.competitions.firstOrNull()?.let { SportsUpdates.shown[widget.id] = it.id }
            widget.competitions.forEach { SportsUpdates.season.remove(it.id) }
        }
    }
    val index = widget.competitions.indexOfFirst { it.id == SportsUpdates.shown[widget.id] }.coerceAtLeast(0)
    val competition = widget.competitions.getOrNull(index)

    if (competition == null) {
        AddRow(label = WidgetStrings.addCompetition, onClick = { adding = true })
    } else {
        // Con la barra aperta, ogni mezzo minuto si guarda se c'è da
        // riscaricare la competizione che si vede (vedi `SportsUpdates`).
        val latest by rememberUpdatedState(widget)
        LaunchedEffect(active, competition.id) {
            while (active) {
                latest.competitions.find { it.id == competition.id }?.let { SportsUpdates.refresh(latest, it, AppSettings.zoneId) }
                delay(30_000)
            }
        }
        val report = widget.reports[competition.id]
        val info = report?.season
        // Un'altra stagione scelta toccando il nome (null: quella in corso).
        val chosen = SportsUpdates.season[competition.id]?.takeIf { info != null && it != info.year }
        LaunchedEffect(competition.id, chosen) {
            if (chosen != null && info != null) SportsUpdates.loadSeason(competition, info, chosen)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            CompetitionHeader(
                competition = competition,
                index = index,
                count = widget.competitions.size,
                season = info?.let { seasonLabel(it, chosen ?: it.year) },
                pastSeason = chosen != null,
                onMove = { delta -> widget.competitions.getOrNull(index + delta)?.let { SportsUpdates.shown[widget.id] = it.id } },
                onPickSeason = { if (info != null) pickingSeason = true }
            )
            val view = if (chosen != null) pastSeasonView(competition, chosen, now) else currentSeasonView(competition, report)
            CompetitionRounds(
                competition,
                view,
                seasonName = info?.let { seasonLabel(it, chosen ?: it.year) },
                active = active,
                now = now,
                todayTick = SportsUpdates.todayRequests[widget.id] ?: 0,
                onManage = { onManagingChange(true) }
            )
        }
        if (pickingSeason && info != null) {
            SeasonPicker(
                info = info,
                selected = chosen ?: info.year,
                onPick = { year ->
                    if (year == info.year) SportsUpdates.season.remove(competition.id) else SportsUpdates.season[competition.id] = year
                    pickingSeason = false
                },
                onDismiss = { pickingSeason = false }
            )
        }
    }

    if (managing) {
        CompetitionsSheet(widget = widget, startWithCatalog = false, onDismiss = { onManagingChange(false) })
    }
    if (adding) {
        CompetitionsSheet(widget = widget, startWithCatalog = true, onDismiss = { adding = false })
    }
}

/** Il colore della stagione quando non è quella in corso: si vede subito che si guarda il passato. */
private val PastSeasonColor = Color(0xFFE5A04C)

/**
 * La bandiera, la stagione e il nome in mezzo — un tocco lì sceglie la
 * stagione —, le frecce ai lati: grigie se non c'è una competizione da
 * quella parte.
 */
@Composable
private fun CompetitionHeader(
    competition: SportCompetition,
    index: Int,
    count: Int,
    season: String?,
    pastSeason: Boolean,
    onMove: (Int) -> Unit,
    onPickSeason: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ArrowButton(Icons.Filled.KeyboardArrowLeft, WidgetStrings.previousCompetition, enabled = index > 0) { onMove(-1) }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onPickSeason)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompetitionFlag(competition.league, 18.sp)
            if (season != null) {
                Text(
                    season,
                    color = if (pastSeason) PastSeasonColor else NotionGray400,
                    fontSize = 12.sp,
                    fontWeight = if (pastSeason) FontWeight.SemiBold else null,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                competition.name,
                color = NotionWhite,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        ArrowButton(Icons.Filled.KeyboardArrowRight, WidgetStrings.nextCompetition, enabled = index < count - 1) { onMove(1) }
    }
}

/** La scelta della stagione: quella in corso e le nove prima. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonPicker(info: SeasonInfo, selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding()) {
            Text(WidgetStrings.season, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                items(seasonYears(info)) { year ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(year) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(seasonLabel(info, year), color = NotionWhite, style = MaterialTheme.typography.bodyLarge)
                        if (year == info.year) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(WidgetStrings.currentSeason, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (year == selected) Icon(Icons.Filled.Check, contentDescription = null, tint = NotionWhite, modifier = Modifier.size(18.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = if (enabled) NotionWhite else NotionGray400.copy(alpha = 0.35f))
    }
}

/** La bandiera della competizione (vedi `competitionFlag`), con lo spazio dopo; niente se non c'è. */
@Composable
fun CompetitionFlag(league: String, size: TextUnit) {
    val flag = competitionFlag(league)
    if (flag.isNotEmpty()) {
        Text(flag, fontSize = size)
        Spacer(modifier = Modifier.width(8.dp))
    }
}

/** **"Today"** (dal 20/10/2026), a sinistra della freccia che aggiorna: vedi `SportsUpdates.goToToday`. */
@Composable
fun SportsTodayButton(widget: SportsWidget) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable { SportsUpdates.goToToday(widget, AppSettings.zoneId) },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Today, contentDescription = WidgetStrings.today, tint = NotionGray400, modifier = Modifier.size(18.dp))
    }
}

/**
 * **La freccia che aggiorna** (dal 17/10/2026, chiesto dall'utente), accanto
 * ai tre puntini: riscarica subito la competizione che si vede. Gira mentre
 * si scarica.
 */
@Composable
fun SportsRefreshButton(widget: SportsWidget) {
    val busy = SportsUpdates.busy(widget)
    val angle = if (busy) {
        val turning = rememberInfiniteTransition(label = "refresh")
        val value by turning.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "angle"
        )
        value
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable { SportsUpdates.refreshShown(widget, AppSettings.zoneId) },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = WidgetStrings.refresh, tint = NotionGray400, modifier = Modifier.size(18.dp).rotate(angle))
    }
}

/**
 * Quello che il widget mostra di una stagione: le giornate, quella da cui si
 * parte, quella le cui partite stanno nel widget (-1: tutte in memoria), la
 * classifica; o, al posto delle giornate, un testo ("Loading…").
 */
private data class RoundsView(
    val key: String,
    val rounds: List<SportRound> = emptyList(),
    val start: Int = 0,
    val livePage: Int = -1,
    val liveMatches: List<SportMatch> = emptyList(),
    val liveFetchedAt: Long = 0L,
    val liveFailed: Boolean = false,
    val standings: List<StandingGroup> = emptyList(),
    val seasonYear: Int? = null,
    val current: Boolean = true,
    val message: String? = null
)

/** La stagione in corso: quella salvata nel widget. */
private fun currentSeasonView(competition: SportCompetition, report: SportReport?): RoundsView = when {
    report == null -> RoundsView(
        key = competition.id,
        message = if (competition.id in SportsUpdates.failed) WidgetStrings.noConnection else WidgetStrings.loading
    )
    report.rounds.isEmpty() -> RoundsView(key = competition.id, message = WidgetStrings.noMatches)
    else -> RoundsView(
        key = competition.id,
        rounds = report.rounds,
        start = report.round.coerceIn(0, report.rounds.lastIndex),
        livePage = report.round,
        liveMatches = report.matches,
        liveFetchedAt = report.fetchedAt,
        liveFailed = competition.id in SportsUpdates.failed,
        standings = report.standings,
        seasonYear = report.season?.year,
        current = true
    )
}

/** Un'altra stagione: tutto in memoria, si parte dall'ultima giornata (o da quella di oggi). */
private fun pastSeasonView(competition: SportCompetition, year: Int, now: Long): RoundsView {
    val key = SportsUpdates.seasonKey(competition.id, year)
    val past = SportsUpdates.pastSeason(competition.id, year)
        ?: return RoundsView(key = key, message = if (key in SportsUpdates.failed) WidgetStrings.noConnection else WidgetStrings.loading)
    if (past.rounds.isEmpty()) return RoundsView(key = key, message = WidgetStrings.noMatches)
    return RoundsView(
        key = key,
        rounds = past.rounds,
        start = currentRound(past.rounds, todayIn(AppSettings.zoneId, now)).coerceIn(0, past.rounds.lastIndex),
        standings = past.standings,
        seasonYear = year,
        current = false
    )
}

/** Le partite di una giornata: null finché non arrivano. */
private data class RoundContent(val matches: List<SportMatch>?, val fetchedAt: Long, val failed: Boolean)

/** La giornata di adesso sta nel widget; le altre nella memoria di `SportsUpdates`. */
private fun roundContent(view: RoundsView, competitionId: String, page: Int, round: SportRound): RoundContent =
    if (page == view.livePage) {
        RoundContent(view.liveMatches.takeIf { view.liveFetchedAt > 0 }, view.liveFetchedAt, view.liveFailed)
    } else {
        val cached = SportsUpdates.cachedRound(competitionId, round)
        RoundContent(cached?.matches, cached?.fetchedAt ?: 0L, SportsUpdates.roundKey(competitionId, round) in SportsUpdates.failed)
    }

/** Le giornate della stagione, una per pagina, a partire da quella di adesso (o dall'ultima). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompetitionRounds(
    competition: SportCompetition,
    view: RoundsView,
    // La stagione ("2025-2026"), nel titolo della finestra della giornata.
    seasonName: String?,
    active: Boolean,
    now: Long,
    /** I tocchi su "Today" (vedi `SportsUpdates.goToToday`): quando cambia, si va alla giornata di oggi. */
    todayTick: Int,
    onManage: () -> Unit
) {
    val sport = Sport.of(competition.sport)
    if (view.message != null) {
        Text(
            text = view.message,
            color = NotionGray400,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .onRightClick(onManage)
                .combinedClickable(onClick = {}, onLongClick = onManage)
                .padding(vertical = 16.dp)
        )
        return
    }
    var detail by remember(view.key) { mutableStateOf<Int?>(null) }
    // La partita toccata (la pagina e l'id): vedi `MatchSheet`.
    var openMatch by remember(view.key) { mutableStateOf<Pair<Int, String>?>(null) }
    val numbered = hasMatchdayNumbers(view.rounds, view.standings, sport)
    // Gli stemmi di tutte le squadre (la classifica) e della giornata, in
    // anticipo: scorrendo, o aprendo la classifica, ci sono già.
    val cacheDir = cacheDirOf(LocalPlatformContext.current)
    LaunchedEffect(view.key, view.standings, view.liveMatches) {
        TeamLogos.prefetch(
            cacheDir,
            view.standings.flatMap { group -> group.rows.map { it.logo } } +
                view.liveMatches.flatMap { listOf(it.home.logo, it.away.logo) }
        )
    }
    key(view.key) {
        val pagerState = rememberPagerState(initialPage = view.start) { view.rounds.size }
        // Le giornate nascono già su quella di oggi: conta solo un tocco dopo.
        var handledToday by remember { mutableIntStateOf(todayTick) }
        LaunchedEffect(todayTick) {
            if (todayTick == handledToday) return@LaunchedEffect
            handledToday = todayTick
            val target = todayRound(view.rounds, todayIn(AppSettings.zoneId, System.currentTimeMillis()))
            if (target >= 0) pagerState.animateScrollToPage(target)
        }
        // La giornata su cui ci si ferma scorrendo si scarica (quella di adesso si aggiorna da sé).
        val latest by rememberUpdatedState(view)
        LaunchedEffect(active, pagerState.settledPage) {
            while (active) {
                val page = pagerState.settledPage
                val shown = latest
                if (page != shown.livePage) {
                    shown.rounds.getOrNull(page)?.let { round ->
                        SportsUpdates.loadRound(competition, round, todayIn(AppSettings.zoneId, System.currentTimeMillis()))
                    }
                }
                delay(30_000)
            }
        }
        HorizontalPager(
            state = pagerState,
            key = { page -> view.rounds.getOrNull(page)?.key ?: page },
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val round = view.rounds.getOrNull(page) ?: return@HorizontalPager
            RoundPage(
                title = roundTitle(round, page, numbered),
                content = roundContent(view, competition.id, page, round),
                sport = sport,
                now = now,
                onMatchClick = { openMatch = page to it.id },
                onMatchLongClick = onManage,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .onRightClick(onManage)
                    .combinedClickable(onClick = { detail = page }, onLongClick = onManage)
            )
        }
    }
    detail?.let { page ->
        val round = view.rounds.getOrNull(page)
        if (round == null) {
            detail = null
        } else {
            CompetitionSheet(
                competition = competition,
                title = listOfNotNull(seasonName, roundTitle(round, page, numbered)).joinToString(" · "),
                content = roundContent(view, competition.id, page, round),
                standings = view.standings,
                seasonYear = view.seasonYear,
                currentSeason = view.current,
                now = now,
                // Dalla finestra della giornata alla partita: una finestra per volta.
                onMatchClick = { match ->
                    detail = null
                    openMatch = page to match.id
                },
                onDismiss = { detail = null }
            )
        }
    }
    openMatch?.let { (page, id) ->
        // La partita com'è adesso (il punteggio cambia mentre è aperta).
        val match = view.rounds.getOrNull(page)
            ?.let { roundContent(view, competition.id, page, it).matches }
            ?.find { it.id == id }
        if (match == null) {
            openMatch = null
        } else {
            MatchSheet(competition, match, now, onDismiss = { openMatch = null })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoundPage(
    title: String,
    content: RoundContent,
    sport: Sport,
    now: Long,
    onMatchClick: (SportMatch) -> Unit,
    onMatchLongClick: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(title, color = NotionGray400, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val matches = content.matches
        when {
            matches == null -> Text(
                text = if (content.failed) WidgetStrings.noConnection else WidgetStrings.loading,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            matches.isEmpty() -> Text(
                WidgetStrings.noMatches,
                color = NotionGray400,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            else -> {
                Spacer(modifier = Modifier.height(4.dp))
                MatchList(matches, sport, now, compact = true, onMatchClick = onMatchClick, onMatchLongClick = onMatchLongClick)
            }
        }
        if (content.fetchedAt > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = WidgetStrings.updatedAt(clockAt(content.fetchedAt)) + if (content.failed) " · " + WidgetStrings.noConnection else "",
                color = NotionGray400,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Il nome della giornata e i suoi giorni: "Giornata 7 · Fri 16 – Mon 19
 * Oct" dove il numero torna (vedi `hasMatchdayNumbers`), "Week 7 · …" o
 * "League Phase · …" col nome di ESPN, altrimenti solo i giorni.
 */
private fun roundTitle(round: SportRound, index: Int, numbered: Boolean): String {
    val name = when {
        numbered -> WidgetStrings.matchday(index + 1)
        round.label.isNotEmpty() -> round.label
        else -> ""
    }
    val dates = roundDates(round.firstDay, round.lastDay)
    return if (name.isEmpty()) dates else "$name · $dates"
}

/** "Sat 17 Oct", "Fri 16 – Mon 19 Oct", "Sat 31 Oct – Mon 2 Nov"; in Asia "10月16日 (金)". */
private fun roundDates(from: LocalDate, to: LocalDate): String {
    val language = AppSettings.language
    val locale = language.locale
    fun weekday(day: LocalDate) = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    if (language == AppLanguage.CHINESE || language == AppLanguage.JAPANESE || language == AppLanguage.KOREAN) {
        val pattern = DateTimeFormatter.ofPattern(if (language == AppLanguage.KOREAN) "M월 d일" else "M月d日")
        fun one(day: LocalDate) = pattern.format(day) + " (" + weekday(day) + ")"
        return if (from == to) one(from) else one(from) + " – " + one(to)
    }
    fun month(day: LocalDate) = day.month.getDisplayName(TextStyle.SHORT, locale)
    fun dayOf(day: LocalDate) = weekday(day) + " " + day.dayOfMonth
    return when {
        from == to -> "${dayOf(from)} ${month(from)}"
        from.year == to.year && from.month == to.month -> "${dayOf(from)} – ${dayOf(to)} ${month(to)}"
        else -> "${dayOf(from)} ${month(from)} – ${dayOf(to)} ${month(to)}"
    }
}

/**
 * **Le partite di una giornata**, in ordine di tempo, divise per giorno
 * (col nome del giorno solo se sono più d'uno); nel tennis per torneo.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MatchList(
    matches: List<SportMatch>,
    sport: Sport,
    now: Long,
    compact: Boolean,
    // Un tocco su una partita la apre (vedi `MatchSheet`); tenendo premuto, nel widget, si scelgono le competizioni.
    onMatchClick: (SportMatch) -> Unit,
    onMatchLongClick: (() -> Unit)? = null
) {
    val zone = AppSettings.zoneId
    val today = todayIn(zone, now)
    val sorted = matches.sortedBy { it.start }
    val groups: List<Pair<String, List<SportMatch>>> = if (sport == Sport.TENNIS) {
        sorted.groupBy { it.tournament }.toList()
    } else {
        sorted.groupBy { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }.map { (day, list) -> dayTitle(day, today) to list }
    }
    groups.forEachIndexed { i, (title, list) ->
        if (title.isNotEmpty() && (groups.size > 1 || sport == Sport.TENNIS)) {
            Text(
                title,
                color = if (compact) NotionGray400 else NotionWhite,
                fontSize = if (compact) 11.sp else 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (i == 0) 2.dp else 10.dp, bottom = 2.dp)
            )
        }
        list.forEach { match ->
            MatchRow(
                match,
                sport,
                compact,
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .onRightClick(onMatchLongClick)
                    .combinedClickable(onClick = { onMatchClick(match) }, onLongClick = onMatchLongClick)
            )
        }
    }
}

/**
 * Una partita su due righe, una per squadra (o giocatore), con lo stemma
 * (o la bandiera) a sinistra, il punteggio a destra e il vincitore in
 * grassetto. Prima: l'ora, o il minuto in rosso se è in corso, o "Finale".
 */
@Composable
private fun MatchRow(match: SportMatch, sport: Sport, compact: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = if (compact) 3.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
        val (status, live) = statusOf(match)
        Text(
            status,
            color = if (live) LiveRed else NotionGray400,
            fontSize = 11.sp,
            maxLines = 2,
            fontWeight = if (live) FontWeight.SemiBold else null,
            modifier = Modifier.width(if (compact) 48.dp else 60.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            SideLine(match.home, match.state, sport)
            SideLine(match.away, match.state, sport)
            if (!compact && match.note.isNotEmpty()) {
                Text(match.note, color = NotionGray400, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SideLine(side: MatchSide, state: MatchState, sport: Sport) {
    val weight = if (state == MatchState.POST && side.winner) FontWeight.Bold else null
    Row(modifier = Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        TeamLogo(side.logo, 16.dp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            teamDisplayName(side.name),
            color = NotionWhite,
            fontSize = 13.sp,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val score = if (sport == Sport.TENNIS) side.sets.joinToString(" ") else side.score.orEmpty()
        if (score.isNotEmpty()) {
            // In rosso mentre si gioca, come il minuto: non si confonde coi risultati finali.
            Text(score, color = if (state == MatchState.IN) LiveRed else NotionWhite, fontSize = 13.sp, fontWeight = weight, textAlign = TextAlign.End)
        }
    }
}

/** A sinistra di una partita: (cosa scrivere, se è in corso). */
private fun statusOf(match: SportMatch): Pair<String, Boolean> = when {
    match.postponed -> WidgetStrings.postponed to false
    match.state == MatchState.IN -> match.status.ifEmpty { "Live" } to true
    match.state == MatchState.POST -> WidgetStrings.finalScore to false
    else -> Formats.timeFormatter().format(Instant.ofEpochMilli(match.start).atZone(AppSettings.zoneId)) to false
}

private fun clockAt(millis: Long): String = Formats.timeFormatter().format(Instant.ofEpochMilli(millis).atZone(AppSettings.zoneId))

private fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
    today -> WidgetStrings.today
    today.plusDays(1) -> WidgetStrings.tomorrow
    else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, AppSettings.language.locale)
        .replaceFirstChar { it.titlecase(AppSettings.language.locale) } + " ${day.dayOfMonth}"
}

// --- Partite e classifica ---

/** Le partite della giornata toccata, divise per giorno, e la classifica (o il ranking, nel tennis). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionSheet(
    competition: SportCompetition,
    title: String,
    content: RoundContent,
    standings: List<StandingGroup>,
    seasonYear: Int?,
    currentSeason: Boolean,
    now: Long,
    onMatchClick: (SportMatch) -> Unit,
    onDismiss: () -> Unit
) {
    val sport = Sport.of(competition.sport)
    var tab by remember { mutableIntStateOf(0) }
    // Marcatori, assist e cartellini: solo nel calcio, e serve sapere la stagione.
    val withPlayers = sport == Sport.SOCCER && seasonYear != null
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompetitionFlag(competition.league, 22.sp)
                Text(competition.name, color = NotionWhite, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(title, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            TabRow(selectedTabIndex = tab, containerColor = DarkSheet) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(WidgetStrings.matches) }, unselectedContentColor = NotionGray400)
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    unselectedContentColor = NotionGray400,
                    text = { Text(if (sport == Sport.TENNIS) WidgetStrings.ranking else WidgetStrings.standings) }
                )
                if (withPlayers) {
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(WidgetStrings.players) }, unselectedContentColor = NotionGray400)
                }
            }
            if (tab == 1) {
                StandingsTable(standings, sport)
                return@Column
            }
            if (tab == 2 && seasonYear != null) {
                PlayersTable(competition, seasonYear, currentSeason)
                return@Column
            }
            val matches = content.matches
            when {
                matches == null -> Text(
                    if (content.failed) WidgetStrings.noConnection else WidgetStrings.loading,
                    color = NotionGray400,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
                matches.isEmpty() -> Text(WidgetStrings.noMatches, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
                else -> Column(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MatchList(matches, sport, now, compact = false, onMatchClick = onMatchClick)
                    Text(
                        WidgetStrings.updatedAt(clockAt(content.fetchedAt)) + " · " + WidgetStrings.sportsSource,
                        color = NotionGray400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
                    )
                }
            }
        }
    }
}

/**
 * **I migliori giocatori della stagione** (dal 17/10/2026, chiesto
 * dall'utente): i primi dieci per gol, assist, ammonizioni ed espulsioni,
 * con lo stemma della squadra.
 */
@Composable
private fun PlayersTable(competition: SportCompetition, seasonYear: Int, currentSeason: Boolean) {
    LaunchedEffect(competition.id, seasonYear) { SportsUpdates.loadLeaders(competition, seasonYear, currentSeason) }
    val categories = SportsUpdates.leaders(competition.id, seasonYear)
    if (categories == null) {
        Text(
            if (SportsUpdates.leadersFailed(competition.id, seasonYear)) WidgetStrings.noConnection else WidgetStrings.loading,
            color = NotionGray400,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        return
    }
    if (categories.all { it.leaders.isEmpty() }) {
        Text(WidgetStrings.noStats, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        categories.filter { it.leaders.isNotEmpty() }.forEach { category ->
            item(key = "head-${category.key}") {
                Text(
                    leaderTitle(category.key),
                    color = NotionWhite,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                HorizontalDivider()
            }
            items(category.leaders.withIndex().toList(), key = { "${category.key}-${it.index}" }) { (i, leader) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = NotionGray400, fontSize = 13.sp, modifier = Modifier.width(28.dp))
                    TeamLogo(leader.teamLogo, 16.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(leader.name, color = NotionWhite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(leader.value, color = NotionWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun leaderTitle(key: String): String = when (key) {
    "goals" -> WidgetStrings.goalsLabel
    else -> WidgetStrings.statLabel(key) ?: key
}

/** La classifica: una tabella per gruppo, col nome del gruppo se ce n'è più d'uno, e gli stemmi. */
@Composable
private fun StandingsTable(groups: List<StandingGroup>, sport: Sport) {
    if (groups.isEmpty()) {
        Text(WidgetStrings.noStandings, color = NotionGray400, modifier = Modifier.padding(vertical = 24.dp))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        groups.forEachIndexed { g, group ->
            item(key = "group-$g") {
                if (groups.size > 1) {
                    Text(
                        group.name,
                        color = NotionWhite,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                StandingLine(
                    rank = "#",
                    team = if (sport == Sport.TENNIS) WidgetStrings.player else WidgetStrings.team,
                    values = group.columns,
                    logo = null
                )
                HorizontalDivider()
            }
            itemsIndexed(group.rows, key = { _, it -> "g$g-${it.rank}-${it.team}" }) { r, row ->
                // Come Sofascore: il nome della zona sopra la sua prima squadra, e
                // la parentesi colorata a sinistra dal nome all'ultima squadra.
                val color = zoneColorOf(row)
                val first = color != null && group.rows.getOrNull(r - 1)?.zone != row.zone
                val last = color != null && group.rows.getOrNull(r + 1)?.zone != row.zone
                Column {
                    if (first) {
                        Text(
                            standingZoneLabel(row.zone),
                            color = NotionGray400,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .zoneBracket(color!!, top = true, bottom = false)
                                .padding(start = ZONE_GUTTER, top = 2.dp, bottom = 2.dp)
                        )
                    }
                    StandingLine(
                        rank = row.rank.toString(),
                        team = teamDisplayName(row.team),
                        values = row.values,
                        logo = row.logo,
                        modifier = if (color == null) Modifier else Modifier.zoneBracket(color, top = false, bottom = last)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/** Lo spazio a sinistra di ogni riga, per la parentesi delle zone. */
private val ZONE_GUTTER = 12.dp

/** Il colore della zona di [row], come su Sofascore; per le altre quello di ESPN. Null a metà classifica. */
private fun zoneColorOf(row: StandingRow): Color? = when (standingZoneOf(row.zone)) {
    null -> null
    StandingZone.CHAMPIONS, StandingZone.PROMOTION -> Color(0xFF3DAA4F)
    StandingZone.CHAMPIONS_QUALIFYING, StandingZone.PROMOTION_PLAYOFF -> Color(0xFF8ACB6E)
    StandingZone.EUROPA -> Color(0xFF2F7FE6)
    StandingZone.CONFERENCE -> Color(0xFF1FC1C8)
    StandingZone.RELEGATION_PLAYOFF -> Color(0xFFF08A3C)
    StandingZone.RELEGATION -> Color(0xFFE2484C)
    StandingZone.OTHER -> row.zoneColor.takeIf { it.isNotEmpty() }?.let { Color(("FF" + it.drop(1)).toLong(16)) } ?: NotionGray400
}

/**
 * Un pezzo della parentesi di una zona, sul margine sinistro: la linea, e un
 * trattino verso destra in cima ([top], a metà del nome della zona) e in fondo ([bottom]).
 */
private fun Modifier.zoneBracket(color: Color, top: Boolean, bottom: Boolean): Modifier = drawBehind {
    val stroke = 2.dp.toPx()
    val tick = 6.dp.toPx()
    val x = stroke / 2
    val from = if (top) size.height / 2 else 0f
    val to = if (bottom) size.height - 3.dp.toPx() else size.height
    drawLine(color, Offset(x, from), Offset(x, to), stroke)
    if (top) drawLine(color, Offset(0f, from), Offset(tick, from), stroke)
    if (bottom) drawLine(color, Offset(0f, to), Offset(tick, to), stroke)
}

/** Una riga della classifica; [logo] null è l'intestazione. */
@Composable
private fun StandingLine(rank: String, team: String, values: List<String>, logo: String?, modifier: Modifier = Modifier) {
    val header = logo == null
    val color = if (header) NotionGray400 else NotionWhite
    val size = if (header) 11.sp else 13.sp
    Row(
        modifier = Modifier.fillMaxWidth().then(modifier).padding(start = ZONE_GUTTER, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(rank, color = NotionGray400, fontSize = size, modifier = Modifier.width(28.dp))
        if (logo == null) Spacer(modifier = Modifier.width(22.dp)) else {
            TeamLogo(logo, 16.dp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(team, color = color, fontSize = size, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        values.forEachIndexed { i, value ->
            Text(
                value,
                color = color,
                fontSize = size,
                // L'ultima colonna (i punti, nel calcio) in grassetto.
                fontWeight = if (!header && i == values.lastIndex) FontWeight.SemiBold else null,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(if (value.length > 4 || values.size <= 2) 52.dp else 36.dp)
            )
        }
    }
}

// --- Le competizioni ---

/**
 * **Le competizioni del widget**: "Add a competition" in cima, e per ognuna,
 * dal menu ⋮, Show first, Move up, Move down, Remove competition.
 * [startWithCatalog]: si apre già sull'elenco da cui scegliere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompetitionsSheet(widget: SportsWidget, startWithCatalog: Boolean, onDismiss: () -> Unit) {
    var picking by remember { mutableStateOf(startWithCatalog) }
    NoteBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            if (picking) {
                Catalog(
                    widget = widget,
                    onBack = if (startWithCatalog && widget.competitions.isEmpty()) null else ({ picking = false }),
                    onPick = { entry ->
                        WidgetStore.addSportCompetition(widget.id, entry.toCompetition())
                        if (startWithCatalog) onDismiss() else picking = false
                    }
                )
            } else {
                Text(WidgetStrings.competitions, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(WidgetStrings.competitionsNote, color = NotionGray400, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                if (widget.competitions.size < MAX_SPORT_COMPETITIONS) {
                    AddRow(label = WidgetStrings.addCompetition, onClick = { picking = true })
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    items(widget.competitions, key = { it.id }) { competition ->
                        val index = widget.competitions.indexOf(competition)
                        CompetitionRow(
                            competition = competition,
                            index = index,
                            count = widget.competitions.size,
                            onMove = { delta -> WidgetStore.moveSportCompetition(widget.id, competition.id, delta) },
                            onRemove = { WidgetStore.removeSportCompetition(widget.id, competition.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CompetitionRow(competition: SportCompetition, index: Int, count: Int, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var menu by remember(competition.id) { mutableStateOf(false) }
    val sport = Sport.of(competition.sport)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", color = NotionGray400, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
        CompetitionFlag(competition.league, 18.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(competition.name, color = NotionWhite, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${sport.emoji} ${WidgetStrings.sportName(sport)}", color = NotionGray400, style = MaterialTheme.typography.bodySmall)
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = WidgetStrings.widgetOptions, tint = NotionGray400)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (index > 0) {
                    DropdownMenuItem(text = { Text(WidgetStrings.makeFirst) }, onClick = { menu = false; onMove(-index) })
                }
                DropdownMenuItem(text = { Text(WidgetStrings.moveUp) }, enabled = index > 0, onClick = { menu = false; onMove(-1) })
                DropdownMenuItem(text = { Text(WidgetStrings.moveDown) }, enabled = index < count - 1, onClick = { menu = false; onMove(1) })
                DropdownMenuItem(
                    text = { Text(WidgetStrings.removeCompetition, color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; onRemove() }
                )
            }
        }
    }
}

/** Le competizioni fra cui scegliere, divise per sport, con la bandiera; quelle già aggiunte hanno la spunta. */
@Composable
private fun Catalog(widget: SportsWidget, onBack: (() -> Unit)?, onPick: (CatalogEntry) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = Strings.back, tint = NotionWhite) }
        }
        Text(WidgetStrings.addCompetition, color = NotionWhite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    val added = widget.competitions.map { it.sport to it.league }.toSet()
    val full = widget.competitions.size >= MAX_SPORT_COMPETITIONS
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
        SPORT_CATALOG.groupBy { it.sport }.forEach { (sport, entries) ->
            item(key = "sport-${sport.path}") {
                Text(
                    "${sport.emoji}  ${WidgetStrings.sportName(sport)}",
                    color = NotionGray400,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
                )
            }
            items(entries, key = { "${it.sport.path}/${it.league}" }) { entry ->
                val already = (entry.sport.path to entry.league) in added
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !already && !full) { onPick(entry) }
                        .padding(horizontal = 8.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompetitionFlag(entry.league, 16.sp)
                    Text(
                        entry.name,
                        color = if (already || full) NotionGray400 else NotionWhite,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (already) Icon(Icons.Filled.Check, contentDescription = null, tint = NotionGray400, modifier = Modifier.size(18.dp))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
