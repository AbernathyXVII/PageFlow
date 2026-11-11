package com.gabriele.notionlocal.data.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * **Lo sport** (17/10/2026): risposte vere di ESPN (scaricate quel giorno,
 * alleggerite dei campi che l'app non legge, in `resources/sports`) lette
 * come si deve — partite, stemmi, classifiche, calendari — e le giornate
 * che se ne ricavano.
 */
class SportsTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("sports/$name")!!.bufferedReader().readText()

    private fun day(text: String) = LocalDate.parse(text)

    @Test
    fun serieATodayWithLogos() {
        val board = parseScoreboard(resource("seriea_scoreboard.json"), Sport.SOCCER)!!
        assertEquals(3, board.matches.size)
        val first = board.matches.first()
        assertEquals("Venezia", first.home.name)
        assertEquals("Napoli", first.away.name)
        assertEquals(MatchState.PRE, first.state)
        // Prima della partita niente "0 - 0".
        assertNull(first.home.score)
        assertEquals(Instant.parse("2026-10-17T13:00:00Z").toEpochMilli(), first.start)
        // Gli stemmi.
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/17530.png", first.home.logo)
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/114.png", first.away.logo)
        // Il calendario della stagione, a giorni.
        assertEquals(82, board.matchDays.size)
        // Il giorno prima: Frosinone-Sassuolo 4-2.
        val friday = parseScoreboard(resource("seriea_20261016.json"), Sport.SOCCER)!!.matches.single()
        assertEquals("4", friday.home.score)
        assertTrue(friday.home.winner)
    }

    @Test
    fun theRoundsOfSerieA() {
        val rounds = buildRounds(parseScoreboard(resource("seriea_scoreboard.json"), Sport.SOCCER)!!, Sport.SOCCER)
        // Le 38 giornate, dai giorni del calendario.
        assertEquals(38, rounds.size)
        assertEquals("2026-08-22" to "2026-08-24", rounds.first().from to rounds.first().to)
        // Oggi, sabato 17: la giornata da venerdì 16 a lunedì 19, la settima.
        val today = currentRound(rounds, day("2026-10-17"))
        assertEquals(6, today)
        assertEquals(listOf("2026-10-16", "2026-10-17", "2026-10-18", "2026-10-19"), rounds[today].days)
        // Martedì si vedono ancora i risultati; mercoledì la giornata dopo.
        assertEquals(6, currentRound(rounds, day("2026-10-20")))
        assertEquals(7, currentRound(rounds, day("2026-10-21")))
        // A stagione finita, l'ultima.
        assertEquals(37, currentRound(rounds, day("2027-07-01")))
        // "Today" non ha il giorno dopo: lunedì 19 (Parma-Torino) la settima,
        // da martedì 20 l'ottava.
        assertEquals(6, todayRound(rounds, day("2026-10-17")))
        assertEquals(6, todayRound(rounds, day("2026-10-19")))
        assertEquals(7, todayRound(rounds, day("2026-10-20")))
        assertEquals(37, todayRound(rounds, day("2027-07-01")))
        assertEquals(-1, todayRound(emptyList(), day("2026-10-20")))
        // "Giornata 7": 38 giornate con venti squadre tornano; 37 no.
        val standings = parseStandings(resource("soccer_ita_1_standings.json"), Sport.SOCCER)
        assertTrue(hasMatchdayNumbers(rounds, standings, Sport.SOCCER))
        assertFalse(hasMatchdayNumbers(rounds.drop(1), standings, Sport.SOCCER))
    }

    @Test
    fun longStretchesSplitIntoMidweekAndWeekend() {
        // LaLiga ad agosto 2026: una giornata, poi tredici giorni di fila.
        val days = (15..17).map { LocalDate.of(2026, 8, it) } + (19..31).map { LocalDate.of(2026, 8, it) }
        val rounds = soccerRounds(days).map { it.first().dayOfMonth to it.last().dayOfMonth }
        assertEquals(listOf(15 to 17, 19 to 20, 21 to 24, 25 to 27, 28 to 31), rounds)
        // Venerdì-lunedì resta una giornata sola.
        assertEquals(1, soccerRounds((16..19).map { LocalDate.of(2026, 10, it) }).size)
    }

    @Test
    fun nbaByDayAndNflByWeek() {
        val nba = parseScoreboard(resource("basketball_nba_scoreboard.json"), Sport.BASKETBALL)!!
        assertEquals(12, nba.matches.size)
        val game = nba.matches.first()
        assertEquals("Magic", game.home.name)
        assertEquals("136", game.home.score)
        assertTrue(game.home.winner)
        assertEquals("115", game.away.score)
        assertFalse(game.away.winner)
        assertEquals(MatchState.POST, game.state)
        assertEquals("https://a.espncdn.com/i/teamlogos/nba/500/scoreboard/orl.png", game.home.logo)
        // Nel basket una giornata è un giorno.
        val nbaRounds = buildRounds(nba, Sport.BASKETBALL)
        assertEquals(174, nbaRounds.size)
        assertTrue(nbaRounds.all { it.days.size == 1 })

        // La NFL va a settimane, e si chiede per settimana.
        val nfl = parseScoreboard(resource("football_nfl_scoreboard.json"), Sport.FOOTBALL)!!
        assertEquals(14, nfl.matches.size)
        assertEquals(13, nfl.matches.count { it.state == MatchState.PRE })
        assertTrue(nfl.matchDays.isEmpty())
        val weeks = buildRounds(nfl, Sport.FOOTBALL)
        assertEquals(26, weeks.size)
        assertEquals("Hall of Fame Weekend", weeks.first().label)
        val week7 = weeks.single { it.label == "Week 7" }
        assertEquals("2026-10-21" to "2026-10-27", week7.from to week7.to)
        assertEquals(2, week7.seasonType)
        assertEquals(7, week7.week)
        assertTrue(week7.days.isEmpty())
        assertEquals("Week 6", weeks[currentRound(weeks, day("2026-10-17"))].label)
    }

    @Test
    fun baseballCalendarListsTheDaysWithoutGames() {
        // Il calendario del baseball elenca i giorni di riposo (l'All-Star break), non quelli di partita.
        val json = """{"events":[],"leagues":[{"calendarType":"day","calendarIsWhitelist":false,
            "calendarStartDate":"2026-07-10T07:00Z","calendarEndDate":"2026-07-18T07:00Z",
            "calendar":["2026-07-13T07:00Z","2026-07-14T07:00Z","2026-07-15T07:00Z"]}]}"""
        val board = parseScoreboard(json, Sport.BASEBALL)!!
        assertEquals(listOf(10, 11, 12, 16, 17).map { LocalDate.of(2026, 7, it) }, board.matchDays)
    }

    @Test
    fun championsLeagueRoundsFromTheMonths() {
        // Per le coppe ESPN dà solo le fasi.
        val board = parseScoreboard(resource("champions_scoreboard.json"), Sport.SOCCER)!!
        assertTrue(board.matchDays.isEmpty())
        assertEquals(
            listOf("League Phase", "Knockout Round Playoffs", "Rd of 16", "Quarterfinals", "Semifinals", "Final"),
            board.stages.map { it.label }
        )
        assertEquals(day("2027-02-14"), board.stages.first().to)
        // I giorni di partita vengono dai mesi: a ottobre due giornate, martedì e mercoledì.
        val october = parseScoreboard(resource("champions_202610.json"), Sport.SOCCER)!!.matches
        assertEquals(36, october.size)
        val rounds = buildRounds(board, Sport.SOCCER, october.map { espnDay(it.start) })
        assertEquals(listOf("2026-10-13" to "2026-10-14", "2026-10-20" to "2026-10-21"), rounds.map { it.from to it.to })
        assertEquals(listOf("League Phase", "League Phase"), rounds.map { it.label })
        // Le coppe non hanno "Giornata 7".
        assertFalse(hasMatchdayNumbers(rounds, emptyList(), Sport.SOCCER))
    }

    @Test
    fun tennisSinglesWithSetsAndFlags() {
        val board = parseScoreboard(resource("tennis_atp_scoreboard.json"), Sport.TENNIS)!!
        assertEquals(209, board.matches.size)
        val match = board.matches.single { it.id == "186308" }
        assertEquals("A. Ilagan", match.home.name)
        assertEquals(listOf(3, 7, 5), match.home.sets)
        assertEquals("N. Basilashvili", match.away.name)
        assertEquals(listOf(6, 6, 7), match.away.sets)
        assertTrue(match.away.winner)
        assertEquals("Rolex Shanghai Masters · Qualifying 1st Round", match.note)
        assertEquals("Rolex Shanghai Masters", match.tournament)
        // Al posto dello stemma, la bandiera del giocatore.
        assertEquals("https://a.espncdn.com/i/teamlogos/countries/500/usa.png", match.home.logo)
        assertEquals("https://a.espncdn.com/i/teamlogos/countries/500/geo.png", match.away.logo)
        // Il giorno chiesto, dei tornei interi: 22 incontri.
        assertEquals(22, onDays(board.matches, setOf(day("2026-10-17"))).size)
        // Nel tennis una giornata è un giorno.
        assertEquals(304, buildRounds(board, Sport.TENNIS).size)
    }

    @Test
    fun standingsInOrderWithLogos() {
        val serieA = parseStandings(resource("soccer_ita_1_standings.json"), Sport.SOCCER)
        assertEquals(1, serieA.size)
        assertEquals(listOf("GP", "W", "D", "L", "GD", "P"), serieA.single().columns)
        assertEquals(20, serieA.single().rows.size)
        assertEquals(listOf("Inter Milan", "Lazio", "AS Roma"), serieA.single().rows.take(3).map { it.team })
        assertEquals(listOf("6", "5", "1", "0", "+10", "16"), serieA.single().rows.first().values)
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/110.png", serieA.single().rows.first().logo)
        // NBA: due conferenze, ognuna dalla prima testa di serie (ESPN le dà in un altro ordine).
        val nba = parseStandings(resource("basketball_nba_standings.json"), Sport.BASKETBALL)
        assertEquals(listOf("Eastern Conference", "Western Conference"), nba.map { it.name })
        assertEquals("Celtics", nba[0].rows.first().team)
        assertEquals(listOf("4", "0", "1.000", "-"), nba[0].rows.first().values)
        assertEquals("https://a.espncdn.com/i/teamlogos/nba/500/bos.png", nba[0].rows.first().logo)
        assertEquals("Grizzlies", nba[1].rows.first().team)
        // Tennis: la classifica dei giocatori, coi punti e la bandiera.
        val atp = parseTennisRankings(resource("tennis_atp_rankings.json"))
        assertEquals("Jannik Sinner", atp.single().rows.first().team)
        assertEquals(listOf("11000"), atp.single().rows.first().values)
        assertEquals("https://a.espncdn.com/i/teamlogos/countries/500/ita.png", atp.single().rows.first().logo)
        assertEquals(emptyList<StandingGroup>(), parseStandings("non è json", Sport.SOCCER))
    }

    @Test
    fun standingZonesFromTheNotesOfEspn() {
        // Come la Serie A di ESPN il 26/10/2026, accorciata; con il colore "##" che a volte ESPN sbaglia.
        val json = """
            {"children":[{"name":"2026-2027 Italian Serie A","standings":{"entries":[
              {"team":{"shortDisplayName":"Inter Milan"},"stats":[{"name":"rank","value":1}],"note":{"color":"#81D6AC","description":"Champions League","rank":1}},
              {"team":{"shortDisplayName":"Juventus"},"stats":[{"name":"rank","value":7}],"note":{"color":"##B2BFD0","description":"Conference League qualifying","rank":7}},
              {"team":{"shortDisplayName":"Torino"},"stats":[{"name":"rank","value":8}]},
              {"team":{"shortDisplayName":"Parma"},"stats":[{"name":"rank","value":19}],"note":{"color":"#FF7F84","description":"Relegated","rank":19}}
            ]}}]}
        """.trimIndent()
        val rows = parseStandings(json, Sport.SOCCER).single().rows
        assertEquals(listOf("Champions League", "Conference League qualifying", "", "Relegated"), rows.map { it.zone })
        assertEquals(listOf("#81D6AC", "#B2BFD0", "", "#FF7F84"), rows.map { it.zoneColor })
        // I nomi come su Sofascore.
        assertEquals(
            listOf("Champions League", "Conference League Qualification", "Relegation", "Relegation playoff"),
            listOf("Champions League", "Conference League qualifying", "Relegated", "Relegation playoff").map { standingZoneLabel(it) }
        )
        // Le zone, dai nomi che ESPN usa nei campionati europei, in Serie B e nelle coppe.
        mapOf(
            "Champions League" to StandingZone.CHAMPIONS,
            "Champions League qualifying" to StandingZone.CHAMPIONS_QUALIFYING,
            "Europa League" to StandingZone.EUROPA,
            "Europa League qualifying" to StandingZone.EUROPA,
            "Conference League qualifying" to StandingZone.CONFERENCE,
            "Promotion" to StandingZone.PROMOTION,
            "Promotion playoffs" to StandingZone.PROMOTION_PLAYOFF,
            "Relegation playoff" to StandingZone.RELEGATION_PLAYOFF,
            "Relegation" to StandingZone.RELEGATION,
            "Relegated" to StandingZone.RELEGATION,
            "Eliminated" to StandingZone.RELEGATION,
            "Qualifies for round of 16" to StandingZone.OTHER
        ).forEach { (description, zone) -> assertEquals(description, zone, standingZoneOf(description)) }
        assertNull(standingZoneOf(""))
        assertEquals("", standingZoneColor("rosso"))
        assertEquals("", standingZoneColor(null))
    }

    @Test
    fun theSeasonAndTheOthers() {
        // La stagione come la dà ESPN nella risposta della Serie A.
        val info = parseScoreboard(resource("seriea_scoreboard.json"), Sport.SOCCER)!!.season!!
        assertEquals(2026, info.year)
        // Per l'utente: "2026-2027", e le nove prima.
        assertEquals("2026-2027", seasonLabel(info))
        assertEquals("2025-2026", seasonLabel(info, 2025))
        assertEquals((2026 downTo 2017).toList(), seasonYears(info))
        // Per chiedere a ESPN la stagione 2025-26: la metà della stagione, un anno prima.
        assertEquals(LocalDate.of(2025, 12, 17), seasonProbeDay(info, 2025))
        // La NBA 2026-27 per ESPN è il 2027.
        val nba = SeasonInfo(year = 2027, name = "2026-27", start = "2026-09-30T07:00Z", end = "2027-06-26T06:59Z")
        assertEquals("2026-2027", seasonLabel(nba))
        assertEquals("2025-2026", seasonLabel(nba, 2026))
        // NFL, MLB, tennis: un anno solo.
        val nfl = SeasonInfo(year = 2026, name = "2026", start = "2026-08-06T07:00Z", end = "2027-02-16T07:59Z")
        assertEquals("2026", seasonLabel(nfl))
        assertEquals("2024", seasonLabel(nfl, 2024))
    }

    @Test
    fun theBestPlayersOfASeason() {
        val refs = parseLeaderRefs(resource("leaders_ita_2025.json"))
        assertEquals(LEADER_KEYS.toSet(), refs.keys)
        // I primi dieci di ogni classifica.
        assertTrue(refs.values.all { it.size == 10 })
        val scorer = refs.getValue("goals").first()
        assertEquals("17", scorer.value)
        assertTrue(scorer.athleteUrl.startsWith("https://"))
        // Lo stemma dal numero della squadra (l'Inter è la 110).
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/110.png", scorer.teamLogo)
        assertEquals("12", refs.getValue("yellowCards").first().value)
        assertEquals("2", refs.getValue("redCards").first().value)
        assertEquals("L. Martínez", parseAthleteName(resource("athlete_top_scorer.json")))
        assertEquals(emptyMap<String, List<LeaderRef>>(), parseLeaderRefs("non è json"))
    }

    /**
     * **Sul PC le classifiche complete di gol e assist** (dal 11/11/2026,
     * chiesto dall'utente): oltre i primi dieci, tutti quelli che hanno
     * segnato almeno una volta; chi è a zero no, e le altre classifiche
     * (i cartellini) nemmeno.
     */
    @Test
    fun allTheScorersButNotTheZeros() {
        val json = """{"categories":[
            {"name":"goals","leaders":[
                {"value":3,"displayValue":"3","athlete":{"${'$'}ref":"http://x/athletes/1"},"team":{"${'$'}ref":"http://x/teams/110"}},
                {"value":1,"displayValue":"1","athlete":{"${'$'}ref":"http://x/athletes/2"}},
                {"value":0,"displayValue":"0","athlete":{"${'$'}ref":"http://x/athletes/3"}}]},
            {"name":"yellowCards","leaders":[
                {"value":2,"displayValue":"2","athlete":{"${'$'}ref":"http://x/athletes/4"}}]}]}"""
        val refs = parseLeaderRefs(json, limit = 500, keys = listOf("goals", "assists"), scoredOnly = true)
        assertEquals(setOf("goals"), refs.keys)
        assertEquals(listOf("3", "1"), refs.getValue("goals").map { it.value })
        assertTrue(refs.getValue("goals").all { it.athleteUrl.startsWith("https://") })
        // Sul telefono come prima: i primi dieci, anche a zero.
        assertEquals(3, parseLeaderRefs(json).getValue("goals").size)
    }

    @Test
    fun flagsOfTheCompetitions() {
        assertEquals("🇮🇹", competitionFlag("ita.1"))
        assertEquals("🇮🇹", competitionFlag("ita.coppa_italia"))
        assertEquals("🏴󠁧󠁢󠁥󠁮󠁧󠁿", competitionFlag("eng.1"))
        assertEquals("🇪🇸", competitionFlag("esp.1"))
        assertEquals("🇩🇪", competitionFlag("ger.1"))
        assertEquals("🇫🇷", competitionFlag("fra.1"))
        // Le competizioni continentali: il globo col continente.
        assertEquals("🌍", competitionFlag("uefa.champions"))
        assertEquals("🌍", competitionFlag("fifa.worldq.uefa"))
        assertEquals("🌎", competitionFlag("conmebol.libertadores"))
        assertEquals("🌍", competitionFlag("caf.nations"))
        assertEquals("🌏", competitionFlag("afc.champions"))
        // Quelle mondiali: il globo intero.
        assertEquals("🌐", competitionFlag("fifa.world"))
        assertEquals("🌐", competitionFlag("atp"))
        assertEquals("🇺🇸", competitionFlag("nba"))
        assertEquals("", competitionFlag("boh"))
        // Tutte quelle che si possono scegliere ne hanno una.
        assertTrue(SPORT_CATALOG.all { competitionFlag(it.league).isNotEmpty() })
    }

    private fun match(id: String, state: MatchState) = SportMatch(
        id = id,
        start = 0,
        state = state,
        home = MatchSide("A"),
        away = MatchSide("B")
    )

    @Test
    fun whenToRefresh() {
        val now = 100 * 60 * MINUTE_MS
        // Con una partita in corso si riscarica ogni minuto, altrimenti ogni cinque.
        val live = SportReport(fetchedAt = now, matches = listOf(match("adesso", MatchState.IN), match("dopo", MatchState.PRE)))
        assertFalse(live.isStale(now + 30_000))
        assertTrue(live.isStale(now + MINUTE_MS))
        val quiet = SportReport(fetchedAt = now, matches = listOf(match("dopo", MatchState.PRE)))
        assertFalse(quiet.isStale(now + MINUTE_MS))
        assertTrue(quiet.isStale(now + 5 * MINUTE_MS))
        assertTrue((null as SportReport?).isStale(now))
        // Senza giornate niente giornata di adesso.
        assertEquals(-1, currentRound(emptyList(), LocalDate.of(2026, 10, 17)))
    }
}
