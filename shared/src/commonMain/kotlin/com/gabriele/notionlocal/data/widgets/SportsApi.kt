package com.gabriele.notionlocal.data.widgets

import com.gabriele.notionlocal.platform.isPc
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * **Le domande a ESPN**, per il widget dello sport: le giornate di una
 * stagione, le partite di una giornata, la classifica — per il tennis quella
 * ATP o WTA dei giocatori — e (calcio) i migliori giocatori della stagione.
 * Le domande che si possono fare insieme partono insieme: sul telefono il
 * DNS a volte impiega 16 secondi a trovare ESPN, e una dopo l'altra facevano
 * aspettare minuti.
 */
object SportsApi {

    private const val BASE = "https://site.api.espn.com/apis"
    private val DAY = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val MONTH = DateTimeFormatter.ofPattern("yyyyMM")

    /** Le giornate di una stagione e la stagione stessa. */
    data class Rounds(val rounds: List<SportRound>, val season: SeasonInfo?)

    /**
     * Le giornate della stagione (vedi `buildRounds`): quella in corso, o
     * quella che contiene [day]. Null senza rete.
     */
    suspend fun rounds(competition: SportCompetition, day: LocalDate? = null): Rounds? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        val query = day?.let { "?dates=" + DAY.format(it) }.orEmpty()
        val board = WidgetHttp.get(scoreboardUrl(competition) + query)?.let { parseScoreboard(it, sport) } ?: return@withContext null
        // Le coppe di calcio hanno solo le fasi: i giorni di partita si
        // cercano mese per mese, tutti insieme.
        val extraDays = if (sport == Sport.SOCCER && board.matchDays.isEmpty() && board.stages.isNotEmpty()) {
            monthsOf(board.stages).map { month ->
                async {
                    WidgetHttp.get(scoreboardUrl(competition) + "?dates=${MONTH.format(month)}&limit=1000")
                        ?.let { parseScoreboard(it, sport)?.matches }
                }
            }.awaitAll().filterNotNull().flatten().map { espnDay(it.start) }
        } else {
            emptyList()
        }
        Rounds(buildRounds(board, sport, extraDays), board.season)
    }

    /** I mesi della stagione, dalla prima fase all'ultima (al massimo un anno e mezzo). */
    private fun monthsOf(stages: List<CalendarStage>): List<YearMonth> {
        val first = YearMonth.from(stages.minOf { it.from })
        val last = YearMonth.from(stages.maxOf { it.to })
        return generateSequence(first) { it.plusMonths(1) }.takeWhile { it <= last }.take(18).toList()
    }

    /** Le partite di una giornata, in ordine di tempo; null senza rete. */
    suspend fun roundMatches(competition: SportCompetition, round: SportRound): List<SportMatch>? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        val queries = if (round.week != null) {
            // La settimana di quella stagione: senza l'anno, ESPN dà quella in corso.
            listOf("?seasontype=${round.seasonType ?: 2}&week=${round.week}" + (round.season?.let { "&dates=$it" }.orEmpty()))
        } else {
            round.days.map { "?dates=" + DAY.format(LocalDate.parse(it)) }
        }
        val answers = queries.map { query ->
            async { WidgetHttp.get(scoreboardUrl(competition) + query)?.let { parseScoreboard(it, sport)?.matches } }
        }.awaitAll()
        if (answers.isNotEmpty() && answers.all { it == null }) return@withContext null
        var matches = answers.filterNotNull().flatten().distinctBy { it.id }.sortedBy { it.start }
        if (sport == Sport.TENNIS) {
            // Per un giorno ESPN dà i tornei interi (più di cento incontri): solo quelli del giorno.
            matches = onDays(matches, round.days.map(LocalDate::parse).toSet())
        }
        matches
    }

    /**
     * La classifica (nel tennis il ranking, che c'è solo per adesso): quella
     * in corso, o di un'altra stagione ([season]). Null senza rete.
     */
    suspend fun standings(competition: SportCompetition, season: Int? = null): List<StandingGroup>? = withContext(Dispatchers.IO) {
        val sport = Sport.of(competition.sport)
        if (sport == Sport.TENNIS) {
            if (season != null) return@withContext emptyList()
            WidgetHttp.get("$BASE/site/v2/sports/tennis/${competition.league}/rankings")?.let { parseTennisRankings(it) }
        } else {
            val query = season?.let { "?season=$it" }.orEmpty()
            WidgetHttp.get("$BASE/v2/sports/${sport.path}/${competition.league}/standings$query")?.let { parseStandings(it, sport) }
        }
    }

    /**
     * **I migliori giocatori di una stagione** (calcio): marcatori, assist,
     * ammonizioni ed espulsioni. ESPN dà le classifiche con i giocatori
     * "per riferimento": i nomi si chiedono a parte, tutti insieme. Null
     * senza rete.
     */
    suspend fun leaders(competition: SportCompetition, season: Int): List<LeaderCategory>? = withContext(Dispatchers.IO) {
        val base = "https://sports.core.api.espn.com/v2/sports/soccer/leagues/${competition.league}/seasons/$season/types/1/leaders"
        // **Sul PC le classifiche complete di gol e assist** (dal 11/11/2026, chiesto
        // dall'utente): ESPN ne dà 25 per volta, con `limit` tutte (anche chi è a
        // zero, che si lascia fuori). La risposta è grande: un limite più alto.
        val refs = if (isPc) {
            WidgetHttp.get("$base?limit=$FULL_LEADERS", maxBytes = 8 * 1024 * 1024)
                ?.let { parseLeaderRefs(it, limit = FULL_LEADERS, keys = FULL_LEADER_KEYS, scoredOnly = true) }
        } else {
            WidgetHttp.get(base)?.let { parseLeaderRefs(it) }
        } ?: return@withContext null
        // Sul PC i nomi sono più di trecento: il doppio delle domande insieme (misurato: 8 secondi con otto).
        val gate = Semaphore(if (isPc) 16 else 8)
        // I nomi si chiedono uno per uno: quelli già visti restano in memoria.
        val names = refs.values.flatten().map { it.athleteUrl }.distinct().map { athlete ->
            async {
                athlete to (athleteNames[athlete] ?: gate.withPermit {
                    WidgetHttp.get(athlete, maxBytes = 256 * 1024)?.let { parseAthleteName(it) }?.also { athleteNames[athlete] = it }
                })
            }
        }.awaitAll().toMap()
        (if (isPc) FULL_LEADER_KEYS else LEADER_KEYS).mapNotNull { key ->
            refs[key]?.let { list ->
                LeaderCategory(key, list.map { Leader(names[it.athleteUrl] ?: "—", it.teamLogo, it.value) })
            }
        }
    }

    /** I nomi dei giocatori già chiesti, per indirizzo della loro scheda. */
    private val athleteNames = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Sul PC: le classifiche complete, di gol e assist, fino a quanti giocatori. */
    private val FULL_LEADER_KEYS = listOf("goals", "assists")
    private const val FULL_LEADERS = 500

    private fun scoreboardUrl(competition: SportCompetition): String =
        "$BASE/site/v2/sports/${Sport.of(competition.sport).path}/${competition.league}/scoreboard"
}

/**
 * **Quando si riscarica**: solo con la barra aperta, e solo la competizione
 * che si vede. Le giornate due volte al giorno, la giornata di adesso ogni
 * cinque minuti (ogni minuto con una partita in corso), la classifica ogni
 * mezz'ora; o tutto subito con "Refresh" (e con la freccia accanto ai tre
 * puntini). Le altre giornate, quando ci si arriva scorrendo, e le altre
 * stagioni restano in memoria finché l'app è aperta.
 */
object SportsUpdates {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Quello che si sta scaricando: competizioni, giornate, stagioni. Si vede (la freccia gira). */
    private val inFlightState = mutableStateOf<Set<String>>(emptySet())
    private val inFlight: Set<String> get() = inFlightState.value
    private fun start(key: String) { inFlightState.value = inFlightState.value + key }
    private fun finish(key: String) { inFlightState.value = inFlightState.value - key }

    private val failedState = mutableStateOf<Set<String>>(emptySet())

    /** Le competizioni (per id), le giornate (`roundKey`) e le stagioni la cui ultima domanda non ha avuto risposta. */
    val failed: Set<String> get() = failedState.value

    /** Una giornata scaricata scorrendo. */
    data class CachedRound(val fetchedAt: Long, val matches: List<SportMatch>)

    private val roundCache = mutableStateMapOf<String, CachedRound>()

    /** La competizione che si vede in ogni widget, per id del widget. */
    val shown = mutableStateMapOf<String, String>()

    /** La stagione scelta per ogni competizione (per id); senza, quella in corso. */
    val season = mutableStateMapOf<String, Int>()

    /** Quante volte è stato toccato "Today" in ogni widget (per id): a ogni tocco il widget torna alla giornata di oggi. */
    val todayRequests = mutableStateMapOf<String, Int>()

    /**
     * **"Today"** (dal 20/10/2026, chiesto dall'utente): la competizione che si
     * vede torna alla stagione in corso e alla giornata di oggi (o la
     * prossima), e se è vecchia si riscarica.
     */
    fun goToToday(widget: SportsWidget, zone: ZoneId) {
        val competition = widget.competitions.find { it.id == shown[widget.id] } ?: widget.competitions.firstOrNull() ?: return
        season.remove(competition.id)
        todayRequests[widget.id] = (todayRequests[widget.id] ?: 0) + 1
        refresh(widget, competition, zone)
    }

    fun roundKey(competitionId: String, round: SportRound): String = "$competitionId/${round.key}"

    fun cachedRound(competitionId: String, round: SportRound): CachedRound? = roundCache[roundKey(competitionId, round)]

    /** Se si sta scaricando qualcosa della competizione che si vede nel widget. */
    fun busy(widget: SportsWidget): Boolean {
        val id = shown[widget.id] ?: widget.competitions.firstOrNull()?.id ?: return false
        return inFlight.any { it == id || it.startsWith("$id/") }
    }

    /** La competizione che si vede: le giornate, la giornata di adesso e la classifica, se sono vecchie. */
    fun refresh(widget: SportsWidget, competition: SportCompetition, zone: ZoneId, force: Boolean = false, now: Long = System.currentTimeMillis()) {
        if (competition.id in inFlight) return
        val previous = widget.reports[competition.id]
        @Suppress("NAME_SHADOWING")
        val force = force || (previous != null && previous.version < SPORT_REPORT_VERSION)
        val roundsStale = previous == null || previous.rounds.isEmpty() ||
            previous.roundsFetchedAt?.let { now - it >= ROUNDS_REFRESH_MS } != false
        val standingsStale = previous?.standingsFetchedAt?.let { now - it >= STANDINGS_REFRESH_MS } != false
        val roundMoved = previous != null && previous.round != currentRound(previous.rounds, todayIn(zone, now))
        if (!force && !roundsStale && !standingsStale && !roundMoved && !previous.isStale(now)) return
        start(competition.id)
        scope.launch {
            val (report, answered) = update(competition, previous, zone, now, force || roundsStale, force || standingsStale)
            if (report != null) {
                WidgetStore.setSportReport(widget.id, competition.id, report)
            }
            failedState.value = if (answered) failedState.value - competition.id else failedState.value + competition.id
            finish(competition.id)
        }
    }

    /**
     * Tutto subito, per "Refresh" e per la freccia: la competizione che si
     * vede (nella stagione scelta) e le giornate guardate scorrendo.
     */
    fun refreshShown(widget: SportsWidget, zone: ZoneId) {
        val competition = widget.competitions.find { it.id == shown[widget.id] } ?: widget.competitions.firstOrNull() ?: return
        roundCache.keys.filter { it.startsWith(competition.id + "/") }.forEach { roundCache.remove(it) }
        val chosen = season[competition.id]
        val info = widget.reports[competition.id]?.season
        if (chosen != null && info != null && chosen != info.year) {
            loadSeason(competition, info, chosen, force = true)
        } else {
            refresh(widget, competition, zone, force = true)
        }
    }

    /** Il nuovo stato della competizione (null se non è arrivato niente), e se ESPN ha risposto. */
    private suspend fun update(
        competition: SportCompetition,
        previous: SportReport?,
        zone: ZoneId,
        now: Long,
        fetchRounds: Boolean,
        fetchStandings: Boolean
    ): Pair<SportReport?, Boolean> = coroutineScope {
        val newStandings = if (fetchStandings) async { SportsApi.standings(competition) } else null
        val newRounds = if (fetchRounds) SportsApi.rounds(competition) else null
        val rounds = newRounds?.rounds ?: previous?.rounds.orEmpty()
        val current = currentRound(rounds, todayIn(zone, now))
        val fresh = rounds.getOrNull(current)?.let { SportsApi.roundMatches(competition, it) }
        val standings = newStandings?.await()
        // Ha risposto: le partite della giornata, o (fuori stagione) un calendario vuoto.
        val answered = fresh != null || (newRounds != null && rounds.isEmpty())
        if (!answered && newRounds == null && previous == null) return@coroutineScope null to false
        val report = SportReport(
            // Senza le partite nuove resta la giornata di prima, con le sue.
            fetchedAt = if (fresh != null) now else previous?.fetchedAt ?: 0L,
            matches = fresh ?: previous?.matches.orEmpty(),
            standings = standings ?: previous?.standings.orEmpty(),
            standingsFetchedAt = if (standings != null) now else previous?.standingsFetchedAt,
            rounds = rounds,
            roundsFetchedAt = if (newRounds != null) now else previous?.roundsFetchedAt,
            round = if (fresh != null || previous == null) current else previous.round,
            season = newRounds?.season ?: previous?.season,
            version = SPORT_REPORT_VERSION
        )
        report to answered
    }

    /**
     * **Una giornata guardata scorrendo** (non quella di adesso, che sta nel
     * widget): si scarica se non c'è o è vecchia. Una giornata passata con
     * tutte le partite finite non cambia più.
     */
    fun loadRound(competition: SportCompetition, round: SportRound, today: LocalDate, now: Long = System.currentTimeMillis()) {
        val key = roundKey(competition.id, round)
        if (key in inFlight) return
        val cached = roundCache[key]
        if (cached != null) {
            val settled = round.lastDay < today && cached.matches.all { it.state == MatchState.POST || it.postponed }
            if (settled || !matchesStale(cached.matches, cached.fetchedAt, now)) return
        }
        start(key)
        scope.launch {
            val matches = SportsApi.roundMatches(competition, round)
            if (matches != null) {
                roundCache[key] = CachedRound(now, matches)
                failedState.value = failedState.value - key
            } else {
                failedState.value = failedState.value + key
            }
            finish(key)
        }
    }

    // --- Le altre stagioni (dal 17/10/2026, chiesto dall'utente) ---

    /** Un'altra stagione: le sue giornate e la sua classifica. */
    data class PastSeason(val rounds: List<SportRound>, val standings: List<StandingGroup>)

    private val pastSeasons = mutableStateMapOf<String, PastSeason>()

    fun seasonKey(competitionId: String, year: Int): String = "$competitionId/season-$year"

    fun pastSeason(competitionId: String, year: Int): PastSeason? = pastSeasons[seasonKey(competitionId, year)]

    /**
     * **Un'altra stagione** della competizione: le giornate (chiedendo a
     * ESPN un giorno dentro quella stagione, vedi `seasonProbeDay`) e la
     * classifica di quell'anno. Una volta scaricata resta: le stagioni
     * passate non cambiano.
     */
    fun loadSeason(competition: SportCompetition, current: SeasonInfo, year: Int, force: Boolean = false) {
        val key = seasonKey(competition.id, year)
        if (key in inFlight || (!force && key in pastSeasons)) return
        val day = seasonProbeDay(current, year) ?: return
        start(key)
        scope.launch {
            coroutineScope {
                val standings = async { SportsApi.standings(competition, year) }
                val result = SportsApi.rounds(competition, day)
                if (result != null) {
                    pastSeasons[key] = PastSeason(result.rounds, standings.await().orEmpty())
                    failedState.value = failedState.value - key
                } else {
                    failedState.value = failedState.value + key
                }
            }
            finish(key)
        }
    }

    // --- I migliori giocatori ---

    private val leadersCache = mutableStateMapOf<String, Pair<Long, List<LeaderCategory>>>()

    private fun leadersKey(competitionId: String, year: Int): String = "$competitionId/leaders-$year"

    fun leaders(competitionId: String, year: Int): List<LeaderCategory>? = leadersCache[leadersKey(competitionId, year)]?.second

    fun leadersFailed(competitionId: String, year: Int): Boolean = leadersKey(competitionId, year) in failed

    /** Marcatori, assist e cartellini di una stagione: quella in corso si riscarica ogni mezz'ora, le altre mai. */
    fun loadLeaders(competition: SportCompetition, year: Int, current: Boolean, now: Long = System.currentTimeMillis()) {
        if (Sport.of(competition.sport) != Sport.SOCCER) return
        val key = leadersKey(competition.id, year)
        if (key in inFlight) return
        val cached = leadersCache[key]
        if (cached != null && (!current || now - cached.first < STANDINGS_REFRESH_MS)) return
        start(key)
        scope.launch {
            val leaders = SportsApi.leaders(competition, year)
            if (leaders != null) {
                leadersCache[key] = now to leaders
                failedState.value = failedState.value - key
            } else {
                failedState.value = failedState.value + key
            }
            finish(key)
        }
    }
}
