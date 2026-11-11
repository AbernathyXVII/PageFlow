package com.gabriele.notionlocal.data.widgets

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * **Una partita da vicino** (dal 17/10/2026, chiesto dall'utente, come la
 * fa Sofascore): toccando una partita del widget si vedono chi ha segnato e
 * quando (con l'assist), il recupero di ogni tempo, le sostituzioni, i
 * cartellini col motivo, i gol annullati dal VAR, e le statistiche delle due
 * squadre. Nel basket, nel football americano e nel baseball i punti per
 * periodo, le azioni da punti (NFL) e le statistiche.
 *
 * Viene dal "summary" di ESPN: gli eventi principali (`keyEvents`) e, per il
 * recupero annunciato e il VAR, la cronaca (`commentary`). Non si salva:
 * si scarica quando si apre la partita, e si aggiorna ogni minuto se è in
 * corso.
 */
data class MatchDetail(
    /** Gli eventi in ordine di tempo (calcio). */
    val events: List<MatchEvent> = emptyList(),
    val stats: List<MatchStat> = emptyList(),
    /** I punti per periodo (quarti, inning): i nomi delle colonne e le due righe. */
    val periods: List<String> = emptyList(),
    val homeLines: List<String> = emptyList(),
    val awayLines: List<String> = emptyList(),
    /** NFL: le azioni che hanno fatto punti. */
    val scoringPlays: List<ScoringPlay> = emptyList()
)

enum class Side { HOME, AWAY }

enum class MatchEventKind {
    GOAL, OWN_GOAL, PENALTY_GOAL, PENALTY_MISSED,
    SUBSTITUTION, YELLOW_CARD, SECOND_YELLOW, RED_CARD,
    VAR_NO_GOAL, ADDED_TIME, HALF_TIME, FULL_TIME
}

/** I motivi dei cartellini che ESPN scrive, da tradurre; gli altri restano com'è. */
enum class CardReason { FOUL, HANDBALL, VIOLENT_CONDUCT, EXCESSIVE_CELEBRATION, DANGEROUS_PLAY, TIME_WASTING, UNSPORTING, DISSENT, SIMULATION }

data class MatchEvent(
    val kind: MatchEventKind,
    /** Il minuto come lo scrive ESPN: "9'", "45'+1'". */
    val minute: String,
    /** Di chi è: null per gli eventi di tutti (fine tempo, recupero). */
    val side: Side? = null,
    /** Chi ha segnato, chi entra, chi è ammonito ("D. Berardi"). */
    val player: String = "",
    /** L'assist, o chi esce. */
    val other: String = "",
    /** Il motivo del cartellino: uno noto, o il testo di ESPN. */
    val reason: CardReason? = null,
    val reasonText: String = "",
    /** Il punteggio dopo il gol, o alla fine del tempo. */
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    /** I minuti di recupero annunciati. */
    val added: Int = 0,
    /** [player] col nome intero ("Domenico Berardi"), per i marcatori sotto il risultato. */
    val playerFull: String = ""
)

/**
 * **I marcatori di una squadra, come li scrive Sofascore** (dal 18/10/2026,
 * chiesto dall'utente): uno per riga, col nome intero e i minuti dei suoi
 * gol ("Antonio Raimondo 26', 30', 57'"); [marks] dice quali minuti erano
 * un rigore o un autogol, nello stesso ordine. In ordine del primo gol.
 */
data class ScorerLine(val name: String, val minutes: List<String>, val marks: List<MatchEventKind>)

/** I marcatori della casa e degli ospiti; l'autogol va alla squadra che ne approfitta, come su Sofascore. */
fun scorersOf(events: List<MatchEvent>): Pair<List<ScorerLine>, List<ScorerLine>> {
    val goals = events.filter { it.kind == MatchEventKind.GOAL || it.kind == MatchEventKind.PENALTY_GOAL || it.kind == MatchEventKind.OWN_GOAL }
    fun linesOf(side: Side): List<ScorerLine> = goals.filter { it.side == side }
        .groupBy { it.playerFull.ifEmpty { it.player } }
        .map { (name, list) -> ScorerLine(name, list.map { it.minute }, list.map { it.kind }) }
    return linesOf(Side.HOME) to linesOf(Side.AWAY)
}

/** Una statistica: la chiave di ESPN ("possessionPct") e i due valori già scritti. */
data class MatchStat(val key: String, val home: String, val away: String, val homeShare: Float?)

data class ScoringPlay(val period: Int, val clock: String, val type: String, val side: Side?, val text: String, val homeScore: Int, val awayScore: Int)

/** Le statistiche da mostrare, nell'ordine di Sofascore (calcio) o il più simile. */
val MATCH_STAT_KEYS: Map<Sport, List<String>> = mapOf(
    Sport.SOCCER to listOf(
        "possessionPct", "totalShots", "shotsOnTarget", "wonCorners", "offsides", "foulsCommitted",
        "yellowCards", "redCards", "saves", "totalPasses", "accuratePasses", "totalTackles", "interceptions"
    ),
    Sport.BASKETBALL to listOf(
        "fieldGoalsMade-fieldGoalsAttempted", "threePointFieldGoalsMade-threePointFieldGoalsAttempted",
        "freeThrowsMade-freeThrowsAttempted", "totalRebounds", "assists", "steals", "blocks", "turnovers"
    ),
    Sport.FOOTBALL to listOf("firstDowns", "totalYards", "netPassingYards", "rushingYards", "turnovers", "totalPenaltiesYards", "possessionTime")
)

/** "Domenico Berardi" → "D. Berardi", come su Sofascore; un nome solo resta com'è. */
fun shortPlayerName(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    if (parts.size < 2) return name.trim()
    return parts.first().first() + ". " + parts.drop(1).joinToString(" ")
}

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Il "summary" di ESPN di una partita, letto; null se non si legge. */
fun parseMatchSummary(json: String, sport: Sport): MatchDetail? = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    val competition = root.obj("header")?.array("competitions")?.firstOrNull() as? JsonObject
    val sides = competition?.array("competitors").orEmpty().mapNotNull { it as? JsonObject }
    val home = sides.find { it.string("homeAway") == "home" }
    val away = sides.find { it.string("homeAway") == "away" }
    val homeId = home?.obj("team")?.string("id")
    val awayId = away?.obj("team")?.string("id")
    fun sideOf(teamId: String?): Side? = when (teamId) {
        null -> null
        homeId -> Side.HOME
        awayId -> Side.AWAY
        else -> null
    }
    fun namesOf(side: JsonObject?): List<String> = side?.obj("team")?.let { team ->
        listOfNotNull(team.string("displayName"), team.string("shortDisplayName"), team.string("name"), team.string("location"))
    }.orEmpty()
    val homeNames = namesOf(home)
    val awayNames = namesOf(away)
    fun sideByName(name: String): Side? = when {
        homeNames.any { it.equals(name, true) || name.contains(it, true) || it.contains(name, true) } -> Side.HOME
        awayNames.any { it.equals(name, true) || name.contains(it, true) || it.contains(name, true) } -> Side.AWAY
        else -> null
    }

    val stats = parseStats(root, sport)
    if (sport == Sport.SOCCER) {
        return@runCatching MatchDetail(events = soccerEvents(root, ::sideOf, ::sideByName), stats = stats)
    }
    val homeLines = home?.array("linescores").orEmpty().mapNotNull { (it as? JsonObject)?.string("displayValue") }
    val awayLines = away?.array("linescores").orEmpty().mapNotNull { (it as? JsonObject)?.string("displayValue") }
    val count = maxOf(homeLines.size, awayLines.size)
    val regular = if (sport == Sport.BASEBALL) 9 else 4
    val periods = (1..count).map { n -> if (n <= regular) n.toString() else if (sport == Sport.BASEBALL) n.toString() else "OT" + if (n - regular > 1) (n - regular).toString() else "" }
    val plays = root.array("scoringPlays").mapNotNull { element ->
        val play = element as? JsonObject ?: return@mapNotNull null
        ScoringPlay(
            period = play.obj("period")?.int("number") ?: 0,
            clock = play.obj("clock")?.string("displayValue").orEmpty(),
            type = play.obj("type")?.string("text").orEmpty(),
            side = sideOf(play.obj("team")?.string("id")),
            text = play.string("text").orEmpty(),
            homeScore = play.int("homeScore") ?: 0,
            awayScore = play.int("awayScore") ?: 0
        )
    }
    MatchDetail(stats = stats, periods = periods, homeLines = homeLines, awayLines = awayLines, scoringPlays = plays)
}.getOrNull()

/** Il tempo di un minuto scritto da ESPN: 1 fino al 45' (e il suo recupero), 2 fino al 90', poi i supplementari. */
private fun halfOf(minute: String): Int {
    val base = Regex("^(\\d+)").find(minute.trim())?.groupValues?.get(1)?.toIntOrNull() ?: return 0
    return when {
        base <= 45 -> 1
        base <= 90 -> 2
        base <= 105 -> 3
        else -> 4
    }
}

/** Per ordinare: il minuto e il recupero ("45'+1'" → 45, 1). */
private fun minuteKey(minute: String): Pair<Int, Int> {
    val numbers = Regex("\\d+").findAll(minute).map { it.value.toInt() }.toList()
    return (numbers.getOrNull(0) ?: 0) to (numbers.getOrNull(1) ?: 0)
}

private val ADDED_TIME = Regex("announced (\\d+) minutes? of added time", RegexOption.IGNORE_CASE)
private val VAR_GOAL = Regex("GOAL OVERTURNED BY VAR:\\s*(.+?)\\s*\\((.+?)\\)")
private val CARD_REASON = Regex("card(?: to .*?\\))? for (.+?)\\.?$", RegexOption.IGNORE_CASE)

/** Il motivo del cartellino dal testo di ESPN ("... is shown the yellow card for a bad foul."). */
fun cardReasonOf(text: String): Pair<CardReason?, String> {
    val raw = CARD_REASON.find(text.trim())?.groupValues?.get(1)?.trim().orEmpty()
    if (raw.isEmpty()) return null to ""
    val plain = raw.removePrefix("a ").removePrefix("an ").lowercase()
    val known = when {
        "foul" in plain -> CardReason.FOUL
        "hand" in plain -> CardReason.HANDBALL
        "violent" in plain -> CardReason.VIOLENT_CONDUCT
        "celebration" in plain -> CardReason.EXCESSIVE_CELEBRATION
        "dangerous" in plain -> CardReason.DANGEROUS_PLAY
        "time wasting" in plain || "delay" in plain -> CardReason.TIME_WASTING
        "unsporting" in plain -> CardReason.UNSPORTING
        "argument" in plain || "dissent" in plain -> CardReason.DISSENT
        "simulation" in plain || "diving" in plain -> CardReason.SIMULATION
        else -> null
    }
    return known to plain.replaceFirstChar { it.uppercase() }
}

/**
 * **Gli eventi di una partita di calcio**, in ordine di tempo: dagli eventi
 * principali di ESPN (gol con l'assist, rigori, autogol, sostituzioni,
 * cartellini, fine dei tempi col punteggio) e dalla cronaca (il recupero
 * annunciato, i gol annullati dal VAR).
 */
private fun soccerEvents(
    root: JsonObject,
    sideOf: (String?) -> Side?,
    sideByName: (String) -> Side?
): List<MatchEvent> {
    data class Timed(val half: Int, val minute: Pair<Int, Int>, val index: Int, val event: MatchEvent)
    val timed = mutableListOf<Timed>()
    var homeGoals = 0
    var awayGoals = 0
    root.array("keyEvents").forEachIndexed { index, element ->
        val key = element as? JsonObject ?: return@forEachIndexed
        // I rigori dopo i supplementari non sono gol della partita.
        if (key.boolean("shootout")) return@forEachIndexed
        val type = key.obj("type")?.string("type").orEmpty()
        val minute = key.obj("clock")?.string("displayValue").orEmpty()
        val half = key.obj("period")?.int("number") ?: halfOf(minute)
        val side = sideOf(key.obj("team")?.string("id"))
        val fullNames = key.array("participants").mapNotNull { (it as? JsonObject)?.obj("athlete")?.string("displayName") }
        val people = fullNames.map(::shortPlayerName)
        val text = key.string("text").orEmpty()
        val event = when {
            key.boolean("scoringPlay") -> {
                if (side == Side.HOME) homeGoals++ else if (side == Side.AWAY) awayGoals++
                val kind = when {
                    type == "own-goal" -> MatchEventKind.OWN_GOAL
                    type.startsWith("penalty") -> MatchEventKind.PENALTY_GOAL
                    else -> MatchEventKind.GOAL
                }
                MatchEvent(
                    kind = kind,
                    minute = minute,
                    side = side,
                    player = people.getOrElse(0) { "" },
                    playerFull = fullNames.getOrElse(0) { "" },
                    other = if (kind == MatchEventKind.GOAL) people.getOrElse(1) { "" } else "",
                    homeScore = homeGoals,
                    awayScore = awayGoals
                )
            }
            type.startsWith("penalty") -> MatchEvent(MatchEventKind.PENALTY_MISSED, minute, side, people.getOrElse(0) { "" })
            type == "substitution" -> MatchEvent(MatchEventKind.SUBSTITUTION, minute, side, people.getOrElse(0) { "" }, people.getOrElse(1) { "" })
            type.contains("card") -> {
                val kind = when {
                    type.contains("yellow") && type.contains("red") -> MatchEventKind.SECOND_YELLOW
                    type.contains("red") && text.startsWith("Second yellow", ignoreCase = true) -> MatchEventKind.SECOND_YELLOW
                    type.contains("red") -> MatchEventKind.RED_CARD
                    else -> MatchEventKind.YELLOW_CARD
                }
                val (reason, reasonText) = cardReasonOf(text)
                MatchEvent(kind, minute, side, people.getOrElse(0) { "" }, reason = reason, reasonText = reasonText)
            }
            type == "halftime" -> MatchEvent(MatchEventKind.HALF_TIME, minute, homeScore = homeGoals, awayScore = awayGoals)
            type == "end-regular-time" -> MatchEvent(MatchEventKind.FULL_TIME, minute, homeScore = homeGoals, awayScore = awayGoals)
            else -> null
        } ?: return@forEachIndexed
        timed += Timed(half, minuteKey(minute), index, event)
    }
    root.array("commentary").forEachIndexed { index, element ->
        val item = element as? JsonObject ?: return@forEachIndexed
        val text = item.string("text").orEmpty()
        val minute = item.obj("time")?.string("displayValue").orEmpty()
        val event = ADDED_TIME.find(text)?.let { MatchEvent(MatchEventKind.ADDED_TIME, minute, added = it.groupValues[1].toInt()) }
            ?: VAR_GOAL.find(text)?.let { MatchEvent(MatchEventKind.VAR_NO_GOAL, minute, sideByName(it.groupValues[2]), shortPlayerName(it.groupValues[1])) }
            ?: return@forEachIndexed
        // Dopo gli eventi principali dello stesso minuto: l'annuncio del
        // recupero viene prima della fine del tempo, che è al minuto dopo.
        timed += Timed(halfOf(minute), minuteKey(minute), 10_000 + index, event)
    }
    return timed.sortedWith(compareBy<Timed>({ it.half }, { it.minute.first }, { it.minute.second }, { it.index })).map { it.event }
}

/** Le statistiche delle due squadre che interessano per lo sport, coi valori già scritti. */
private fun parseStats(root: JsonObject, sport: Sport): List<MatchStat> {
    val keys = MATCH_STAT_KEYS[sport] ?: return emptyList()
    val teams = root.obj("boxscore")?.array("teams").orEmpty().mapNotNull { it as? JsonObject }
    val home = teams.find { it.string("homeAway") == "home" } ?: teams.getOrNull(1)
    val away = teams.find { it.string("homeAway") == "away" } ?: teams.getOrNull(0)
    fun statsOf(team: JsonObject?): Map<String, JsonObject> =
        team?.array("statistics").orEmpty().mapNotNull { it as? JsonObject }.associateBy { it.string("name").orEmpty() }
    val h = statsOf(home)
    val a = statsOf(away)
    if (h.isEmpty() || a.isEmpty()) return emptyList()
    fun shown(stats: Map<String, JsonObject>, key: String): String? {
        val stat = stats[key] ?: return null
        val value = stat.string("displayValue") ?: return null
        return when (key) {
            "possessionPct" -> value.toDoubleOrNull()?.let { Math.round(it).toString() + "%" } ?: value
            // I passaggi riusciti con la percentuale, come Sofascore: "246 (74%)".
            "accuratePasses" -> {
                val total = stats["totalPasses"]?.string("displayValue")?.toDoubleOrNull()
                val made = value.toDoubleOrNull()
                if (total != null && made != null && total > 0) "$value (${Math.round(made * 100 / total)}%)" else value
            }
            else -> value
        }
    }
    fun number(text: String): Double? = Regex("[\\d.]+").find(text)?.value?.toDoubleOrNull()
    return keys.mapNotNull { key ->
        val hv = shown(h, key) ?: return@mapNotNull null
        val av = shown(a, key) ?: return@mapNotNull null
        val hn = number(hv)
        val an = number(av)
        val share = if (hn != null && an != null && hn + an > 0) (hn / (hn + an)).toFloat() else null
        MatchStat(key, hv, av, share)
    }
}

/**
 * **I dettagli delle partite aperte**: si scaricano quando si tocca una
 * partita, e restano in memoria finché l'app è aperta. Una partita in corso
 * si riscarica ogni minuto.
 */
object MatchDetails {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val inFlight = mutableSetOf<String>()

    data class Loaded(val fetchedAt: Long, val detail: MatchDetail)

    private val cache = mutableStateMapOf<String, Loaded>()
    private val failedState = mutableStateOf<Set<String>>(emptySet())

    fun of(matchId: String): Loaded? = cache[matchId]
    fun failed(matchId: String): Boolean = matchId in failedState.value

    /** Nei test: una partita già scaricata, senza rete. */
    fun preload(matchId: String, detail: MatchDetail, at: Long = System.currentTimeMillis()) {
        cache[matchId] = Loaded(at, detail)
    }

    fun load(competition: SportCompetition, match: SportMatch, now: Long = System.currentTimeMillis()) {
        if (match.id in inFlight) return
        val cached = cache[match.id]
        if (cached != null) {
            // Finita, non cambia più; in corso, ogni minuto; non cominciata, ogni cinque.
            val every = when (match.state) {
                MatchState.POST -> Long.MAX_VALUE
                MatchState.IN -> SPORTS_LIVE_REFRESH_MS
                MatchState.PRE -> SPORTS_REFRESH_MS
            }
            if (now - cached.fetchedAt < every) return
        }
        inFlight += match.id
        scope.launch {
            val sport = Sport.of(competition.sport)
            val detail = withContext(Dispatchers.IO) {
                WidgetHttp.get(
                    "https://site.api.espn.com/apis/site/v2/sports/${sport.path}/${competition.league}/summary?event=${match.id}",
                    maxBytes = 4 * 1024 * 1024
                )?.let { parseMatchSummary(it, sport) }
            }
            if (detail != null) {
                cache[match.id] = Loaded(now, detail)
                failedState.value = failedState.value - match.id
            } else {
                failedState.value = failedState.value + match.id
            }
            inFlight -= match.id
        }
    }
}

// --- I migliori giocatori di una stagione (calcio, dal 17/10/2026, chiesto dall'utente) ---

/** Le classifiche dei giocatori che si mostrano: gol, assist, ammonizioni, espulsioni. */
val LEADER_KEYS = listOf("goals", "assists", "yellowCards", "redCards")

data class Leader(val name: String, val teamLogo: String, val value: String)

data class LeaderCategory(val key: String, val leaders: List<Leader>)

/** Un giocatore di una classifica come lo dà ESPN: l'indirizzo dove chiedere il nome, la squadra, il numero. */
data class LeaderRef(val athleteUrl: String, val teamLogo: String, val value: String)

/** Le classifiche dei giocatori di una stagione (i primi [limit] di ognuna), coi giocatori da chiedere a parte. */
fun parseLeaderRefs(json: String, limit: Int = 10, keys: List<String> = LEADER_KEYS, scoredOnly: Boolean = false): Map<String, List<LeaderRef>> = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    root.array("categories").mapNotNull { it as? JsonObject }
        .filter { it.string("name") in keys }
        .associate { category ->
            category.string("name").orEmpty() to category.array("leaders").mapNotNull { it as? JsonObject }
                // La classifica completa (sul PC): chi ha almeno un gol o un assist, non chi è a zero.
                .filter { !scoredOnly || (it.int("value") ?: 0) > 0 }
                .take(limit).mapNotNull { leader ->
                val athlete = leader.obj("athlete")?.string("\$ref")?.replace("http://", "https://") ?: return@mapNotNull null
                // Lo stemma si ricava dal numero della squadra, senza chiederla.
                val teamId = leader.obj("team")?.string("\$ref")?.let { Regex("/teams/(\\d+)").find(it)?.groupValues?.get(1) }
                LeaderRef(
                    athleteUrl = athlete,
                    teamLogo = teamId?.let { "https://a.espncdn.com/i/teamlogos/soccer/500/$it.png" }.orEmpty(),
                    value = leader.string("displayValue") ?: leader.int("value")?.toString().orEmpty()
                )
            }
        }
}.getOrDefault(emptyMap())

/** Il nome corto di un giocatore ("M. Pongracic") dalla sua scheda di ESPN. */
fun parseAthleteName(json: String): String? = runCatching {
    val root = lenientJson.parseToJsonElement(json).jsonObject
    root.string("shortName") ?: root.string("displayName")?.let(::shortPlayerName)
}.getOrNull()

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.array(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()
private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.let { d -> Math.round(d).toInt() } ?: it.contentOrNull?.toIntOrNull() }
private fun JsonObject.boolean(key: String): Boolean = (this[key] as? JsonPrimitive)?.contentOrNull == "true"
