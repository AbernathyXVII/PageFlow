package com.gabriele.notionlocal.data.settings

import com.gabriele.notionlocal.data.proofing.ProofingLanguage
import com.gabriele.notionlocal.data.SKIN_TONE_COUNT
import com.gabriele.notionlocal.data.entity.TimeFormatKind
import com.gabriele.notionlocal.data.entity.BLOCK_SPACING_STEPS
import com.gabriele.notionlocal.data.entity.ListGaps
import com.gabriele.notionlocal.platform.PlatformContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.ZoneId
import java.util.Locale

/** Il tema: scuro, chiaro, o come il telefono. */
enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** Quanto si muove l'app: tutto, poco (animazioni più brevi) o niente. */
enum class AnimationLevel { FULL, REDUCED, OFF }

/**
 * **Dove sta il titolo delle pagine** (dal 05/10/2026, chiesto dall'utente;
 * Settings → "Title position", per tutte le pagine insieme, menu principale
 * compreso): sotto l'icona (com'è sempre stato), accanto all'icona — una riga
 * sola, più spazio per il testo —, o nascosto.
 */
enum class TitlePosition { BELOW_ICON, BESIDE_ICON, HIDDEN }

/**
 * **La priorità delle notifiche** (dal 30/09/2026, la pagina Notifications):
 * bassa = in silenzio, solo nella tendina; normale = suono e vibrazione;
 * alta = anche il riquadro in cima allo schermo. Com'era prima: alta.
 */
enum class NotificationPriority { LOW, NORMAL, HIGH }

/**
 * **Come si scrivono i numeri con più di tre cifre**: `grouping` è il segno
 * fra un gruppo di tre cifre e l'altro (null = nessuno), `decimal` quello
 * prima dei decimali. Fino al 16/10/2026 c'erano solo 1.000.000 e
 * 1,000,000; dal 17/10 (chiesto dall'utente: "2 026", "2026", "2.026" e il
 * 2 col puntino in alto) anche lo spazio, niente e il punto in alto, tutti
 * con la virgola per i decimali. Lo spazio non va a capo, come quello di
 * "2 026" scritto dall'utente. Il punto in alto è "˙", in cima alle cifre:
 * "·", a metà, era troppo basso ("intendevo proprio in alto").
 */
enum class NumberStyle(val grouping: Char?, val decimal: Char) {
    SPACE_GROUPS('\u00A0', ','),
    NO_GROUPS(null, ','),
    DOT_GROUPS('.', ','),
    HIGH_DOT_GROUPS('\u02D9', ','),
    COMMA_GROUPS(',', '.')
}

/** Come si scrive una data. L'ora, quando c'è, segue sempre in 24 ore. */
enum class DateStyle {
    FULL,
    SHORT,
    MONTH_DAY_YEAR_DOT,
    MONTH_DAY_YEAR_SLASH,
    DAY_MONTH_YEAR_DOT,
    DAY_MONTH_YEAR_SLASH,
    YEAR_MONTH_DAY_DOT,
    YEAR_MONTH_DAY_SLASH,
    CHINESE
}

/**
 * La lingua delle voci dell'app. Traduce solo quello che l'app scrive da
 * sé — menu, impostazioni, nomi dei tipi di proprietà — mai quello che
 * scrive l'utente.
 */
enum class AppLanguage(val locale: Locale) {
    ENGLISH(Locale.ENGLISH),
    ITALIAN(Locale.ITALIAN),
    GERMAN(Locale.GERMAN),
    FRENCH(Locale.FRENCH),
    SPANISH(Locale("es")),
    CHINESE(Locale.SIMPLIFIED_CHINESE),
    KOREAN(Locale.KOREAN),
    JAPANESE(Locale.JAPANESE)
}

/** Da dove parte l'app quando la si apre. */
enum class StartupMode { LAST_VISITED, HOME, SPECIFIC_PAGE }

/** Cosa succede alle pagine nel cestino. */
enum class TrashPolicy { KEEP_30_DAYS, EMPTY_AUTOMATICALLY }

/** Come si ordinano i preferiti. */
enum class FavoritesSort { DATE_ADDED, ALPHABETICAL }

/**
 * Le impostazioni dell'app, salvate sul telefono.
 *
 * **Un oggetto solo, e osservabile.** Ogni valore sta in uno stato di
 * Compose: la schermata che lo legge si ridisegna da sola quando cambia,
 * senza che nessuno debba passarlo giù di mano in mano. Serve soprattutto
 * a lingua, tema e formati, che li leggono decine di punti diversi — anche
 * fuori dalle schermate, come la funzione che scrive una data.
 *
 * Il salvataggio è `SharedPreferences` e non il database: sono scelte del
 * telefono, non contenuto. Nel database finirebbero dentro ogni backup e
 * ogni copia, e una pagina esportata si porterebbe dietro il fuso orario
 * di chi l'ha scritta.
 *
 * `init` va chiamato una volta all'avvio, prima di qualunque lettura.
 */
object AppSettings {

    private lateinit var prefs: SettingsStore

    /** Berlino, che segue da sé l'ora legale: il fuso di partenza chiesto dall'utente. */
    const val DEFAULT_TIME_ZONE = "Europe/Berlin"

    /** Quanti giorni resta una pagina nel cestino, con `TrashPolicy.KEEP_30_DAYS`. */
    const val TRASH_RETENTION_DAYS = 30

    fun init(context: PlatformContext) {
        if (::prefs.isInitialized) return
        prefs = openSettingsStore(context, "settings")
        themeModeState.value = enumOr(KEY_THEME, ThemeMode.DARK)
        animationsState.value = enumOr(KEY_ANIMATIONS, AnimationLevel.FULL)
        titlePositionState.value = enumOr(KEY_TITLE_POSITION, TitlePosition.BELOW_ICON)
        fullLineViewState.value = prefs.getBoolean(KEY_FULL_LINE_VIEW, true)
        numberStyleState.value = enumOr(KEY_NUMBER_STYLE, NumberStyle.DOT_GROUPS)
        dateStyleState.value = enumOr(KEY_DATE_STYLE, DateStyle.DAY_MONTH_YEAR_DOT)
        timeFormatState.value = enumOr(KEY_TIME_FORMAT, TimeFormatKind.H24)
        languageState.value = enumOr(KEY_LANGUAGE, AppLanguage.ENGLISH)
        timeZoneIdState.value = prefs.getString(KEY_TIME_ZONE, null)
            ?.takeIf { runCatching { ZoneId.of(it) }.isSuccess }
            ?: DEFAULT_TIME_ZONE
        startupModeState.value = enumOr(KEY_STARTUP_MODE, StartupMode.HOME)
        startupPageIdState.value = prefs.getString(KEY_STARTUP_PAGE, null)
        sharePageIdState.value = prefs.getString(KEY_SHARE_PAGE, null)
        trashPolicyState.value = enumOr(KEY_TRASH_POLICY, TrashPolicy.KEEP_30_DAYS)
        favoritesSortState.value = enumOr(KEY_FAVORITES_SORT, FavoritesSort.DATE_ADDED)
        favoritesDescendingState.value = prefs.getBoolean(KEY_FAVORITES_DESC, false)
        searchContentState.value = prefs.getBoolean(KEY_SEARCH_CONTENT, true)
        notebooksCollapsedState.value = prefs.getBoolean(KEY_NOTEBOOKS_COLLAPSED, false)
        searchTrashState.value = prefs.getBoolean(KEY_SEARCH_TRASH, false)
        notificationSoundUriState.value = prefs.getString(KEY_SOUND_URI, null)
        notificationSoundNameState.value = prefs.getString(KEY_SOUND_NAME, null)
        notificationsMutedState.value = prefs.getBoolean(KEY_MUTED, false)
        mutedUntilState.value = prefs.getLong(KEY_MUTED_UNTIL, 0L).takeIf { it > 0L }
        notificationVibrateState.value = prefs.getBoolean(KEY_VIBRATE, true)
        notificationPriorityState.value = enumOr(KEY_PRIORITY, NotificationPriority.HIGH)
        notificationBadgeState.value = prefs.getBoolean(KEY_BADGE, true)
        recentColorsState.value = prefs.getString(KEY_RECENT_COLORS, null)
            ?.split(',')
            ?.filter { HEX_COLOR.matches(it) }
            ?.take(RECENT_COLORS_MAX)
            .orEmpty()
        favoriteColorsState.value = prefs.getString(KEY_FAVORITE_COLORS, null)
            ?.split(',')
            ?.filter { HEX_COLOR.matches(it) }
            ?.distinct()
            ?.take(FAVORITE_COLORS_MAX)
            .orEmpty()
        recentEmojisState.value = prefs.getString(KEY_RECENT_EMOJIS, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?.take(RECENT_EMOJIS_MAX)
            .orEmpty()
        skinToneState.value = prefs.getInt(KEY_SKIN_TONE, 0).coerceIn(0, SKIN_TONE_COUNT)
        iconColorState.value = prefs.getString(KEY_ICON_COLOR, null)?.takeIf { HEX_COLOR.matches(it) }
        webClipDarkState.value = prefs.getBoolean(KEY_WEB_CLIP_DARK, false)
        proofingEnabledState.value = prefs.getBoolean(KEY_PROOFING, false)
        proofingLanguageState.value = ProofingLanguage.fromCode(prefs.getString(KEY_PROOFING_LANGUAGE, null))
            ?.takeIf { it.available }
            ?: ProofingLanguage.forApp(languageState.value)
        blockSpacingState.value = prefs.getInt(KEY_BLOCK_SPACING, 0).coerceIn(0, BLOCK_SPACING_STEPS.last())
        listGapsState.value = ListGaps.parse(prefs.getString(KEY_LIST_GAPS, null)) ?: ListGaps()
        sidebarPinnedState.value = prefs.getBoolean(KEY_SIDEBAR_PINNED, false)
        sidebarWidthState.value = prefs.getInt(KEY_SIDEBAR_WIDTH, SIDEBAR_DEFAULT_WIDTH).coerceIn(SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH)
        zoomState.value = prefs.getInt(KEY_ZOOM, 100).coerceIn(ZOOM_MIN, ZOOM_MAX)
    }

    // --- Aspetto ---

    private val themeModeState = mutableStateOf(ThemeMode.DARK)
    var themeMode: ThemeMode
        get() = themeModeState.value
        set(value) { themeModeState.value = value; putEnum(KEY_THEME, value) }

    /**
     * Se il telefono è in modalità scura. Lo aggiorna `MainActivity`, che
     * è l'unica a saperlo; serve al tema "System".
     */
    var systemInDarkMode by mutableStateOf(true)

    /** Se in questo momento l'app è scura, tenendo conto di "System". */
    val isDark: Boolean
        get() = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> systemInDarkMode
        }

    private val animationsState = mutableStateOf(AnimationLevel.FULL)
    var animations: AnimationLevel
        get() = animationsState.value
        set(value) { animationsState.value = value; putEnum(KEY_ANIMATIONS, value) }

    private val titlePositionState = mutableStateOf(TitlePosition.BELOW_ICON)

    /** Dove sta il titolo di tutte le pagine: vedi `TitlePosition`. */
    var titlePosition: TitlePosition
        get() = titlePositionState.value
        set(value) { titlePositionState.value = value; putEnum(KEY_TITLE_POSITION, value) }

    private val fullLineViewState = mutableStateOf(true)

    /**
     * **"Full line view"** (dal 19/10/2026, chiesto dall'utente): le righe
     * lunghe delle pagine non vanno a capo e si scorrono di lato, tutte
     * insieme. Acceso di partenza, come ha chiesto. Vedi `FullLineView.kt`.
     */
    var fullLineView: Boolean
        get() = fullLineViewState.value
        set(value) {
            fullLineViewState.value = value
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_FULL_LINE_VIEW, value).apply()
        }

    private val sidebarPinnedState = mutableStateOf(false)

    /**
     * **La barra laterale fissata** (sul PC, dal 09/11/2026, chiesto
     * dall'utente: "come fa Notion, concedimi di bloccare la sidebar"): resta
     * aperta accanto alla pagina finché la si richiude.
     */
    var sidebarPinned: Boolean
        get() = sidebarPinnedState.value
        set(value) {
            sidebarPinnedState.value = value
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SIDEBAR_PINNED, value).apply()
        }

    private val sidebarWidthState = mutableStateOf(SIDEBAR_DEFAULT_WIDTH)

    /** Quanto è larga la barra fissata, in dp: si cambia trascinandone il bordo. */
    var sidebarWidth: Int
        get() = sidebarWidthState.value
        set(value) {
            sidebarWidthState.value = value.coerceIn(SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH)
            if (::prefs.isInitialized) prefs.edit().putInt(KEY_SIDEBAR_WIDTH, sidebarWidthState.value).apply()
        }

    private val zoomState = mutableStateOf(100)

    /**
     * **Lo zoom del PC, in percento** (dal 11/11/2026, chiesto dall'utente):
     * come quello di un browser, con Ctrl + e Ctrl − o con Ctrl e la rotella
     * (vedi `PcZoom`). Resta fino alla volta dopo.
     */
    var zoom: Int
        get() = zoomState.value
        set(value) {
            zoomState.value = value.coerceIn(ZOOM_MIN, ZOOM_MAX)
            if (::prefs.isInitialized) prefs.edit().putInt(KEY_ZOOM, zoomState.value).apply()
        }

    const val ZOOM_MIN = 50
    const val ZOOM_MAX = 300

    const val SIDEBAR_DEFAULT_WIDTH = 300
    const val SIDEBAR_MIN_WIDTH = 220
    const val SIDEBAR_MAX_WIDTH = 480

    // --- Formati ---

    private val numberStyleState = mutableStateOf(NumberStyle.DOT_GROUPS)
    var numberStyle: NumberStyle
        get() = numberStyleState.value
        set(value) { numberStyleState.value = value; putEnum(KEY_NUMBER_STYLE, value) }

    private val dateStyleState = mutableStateOf(DateStyle.DAY_MONTH_YEAR_DOT)
    var dateStyle: DateStyle
        get() = dateStyleState.value
        set(value) { dateStyleState.value = value; putEnum(KEY_DATE_STYLE, value) }

    /**
     * **Come si scrive l'ora in tutta l'app** (dal 16/10/2026, chiesto
     * dall'utente: prima si sceglieva per ogni colonna Date): a 24 ore
     * (15:07), a 12 ore (3:07 PM) o nascosta. Nascosta vale per le date dei
     * database; dove l'ora è tutto quello che c'è da dire (la cronologia,
     * "Created", la fine di un timer) resta a 24 ore.
     */
    private val timeFormatState = mutableStateOf(TimeFormatKind.H24)
    var timeFormat: TimeFormatKind
        get() = timeFormatState.value
        set(value) { timeFormatState.value = value; putEnum(KEY_TIME_FORMAT, value) }

    private val languageState = mutableStateOf(AppLanguage.ENGLISH)
    var language: AppLanguage
        get() = languageState.value
        set(value) { languageState.value = value; putEnum(KEY_LANGUAGE, value) }

    private val timeZoneIdState = mutableStateOf(DEFAULT_TIME_ZONE)
    var timeZoneId: String
        get() = timeZoneIdState.value
        set(value) {
            if (runCatching { ZoneId.of(value) }.isFailure) return
            timeZoneIdState.value = value
            prefs.edit().putString(KEY_TIME_ZONE, value).apply()
        }

    /** Il fuso in cui vive l'app: tutte le date si leggono e si scrivono qui. */
    val zoneId: ZoneId
        get() = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.of(DEFAULT_TIME_ZONE))

    // --- Avvio ---

    private val startupModeState = mutableStateOf(StartupMode.HOME)
    var startupMode: StartupMode
        get() = startupModeState.value
        set(value) { startupModeState.value = value; putEnum(KEY_STARTUP_MODE, value) }

    private val startupPageIdState = mutableStateOf<String?>(null)
    var startupPageId: String?
        get() = startupPageIdState.value
        set(value) {
            startupPageIdState.value = value
            prefs.edit().putString(KEY_STARTUP_PAGE, value).apply()
        }

    /**
     * **Dove finiscono le cose condivise e le note al volo** (dal 02/11/2026):
     * la pagina scelta in Settings o condividendo; null = "Inbox", che nasce
     * in fondo al menu principale la prima volta che serve (vedi
     * `PageRepository.inboxPageId`).
     */
    private val sharePageIdState = mutableStateOf<String?>(null)
    var sharePageId: String?
        get() = sharePageIdState.value
        set(value) {
            sharePageIdState.value = value
            prefs.edit().putString(KEY_SHARE_PAGE, value).apply()
        }

    /**
     * L'ultima pagina aperta, come rotta di navigazione. Non è uno stato
     * osservato: cambia a ogni pagina aperta e nessuna schermata lo
     * mostra, serve solo al prossimo avvio.
     */
    var lastVisitedRoute: String?
        get() = prefs.getString(KEY_LAST_ROUTE, null)
        set(value) { prefs.edit().putString(KEY_LAST_ROUTE, value).apply() }

    // --- Cestino e preferiti ---

    private val trashPolicyState = mutableStateOf(TrashPolicy.KEEP_30_DAYS)
    var trashPolicy: TrashPolicy
        get() = trashPolicyState.value
        set(value) { trashPolicyState.value = value; putEnum(KEY_TRASH_POLICY, value) }

    private val favoritesSortState = mutableStateOf(FavoritesSort.DATE_ADDED)
    var favoritesSort: FavoritesSort
        get() = favoritesSortState.value
        set(value) { favoritesSortState.value = value; putEnum(KEY_FAVORITES_SORT, value) }

    private val favoritesDescendingState = mutableStateOf(false)
    var favoritesDescending: Boolean
        get() = favoritesDescendingState.value
        set(value) {
            favoritesDescendingState.value = value
            prefs.edit().putBoolean(KEY_FAVORITES_DESC, value).apply()
        }

    // --- Ricerca ---

    private val searchContentState = mutableStateOf(true)
    var searchContent: Boolean
        get() = searchContentState.value
        set(value) {
            searchContentState.value = value
            prefs.edit().putBoolean(KEY_SEARCH_CONTENT, value).apply()
        }

    private val searchTrashState = mutableStateOf(false)
    var searchTrash: Boolean
        get() = searchTrashState.value
        set(value) {
            searchTrashState.value = value
            prefs.edit().putBoolean(KEY_SEARCH_TRASH, value).apply()
        }

    // --- Quaderni ---

    /** La voce "Notebooks" della barra laterale chiusa con la sua freccetta. */
    private val notebooksCollapsedState = mutableStateOf(false)
    var notebooksCollapsed: Boolean
        get() = notebooksCollapsedState.value
        set(value) {
            notebooksCollapsedState.value = value
            prefs.edit().putBoolean(KEY_NOTEBOOKS_COLLAPSED, value).apply()
        }

    // --- Suoni e notifiche ---

    private val notificationSoundUriState = mutableStateOf<String?>(null)
    val notificationSoundUri: String? get() = notificationSoundUriState.value

    private val notificationSoundNameState = mutableStateOf<String?>(null)
    val notificationSoundName: String? get() = notificationSoundNameState.value

    fun setNotificationSound(uri: String?, name: String?) {
        notificationSoundUriState.value = uri
        notificationSoundNameState.value = name
        prefs.edit().putString(KEY_SOUND_URI, uri).putString(KEY_SOUND_NAME, name).apply()
    }

    private val notificationsMutedState = mutableStateOf(false)
    var notificationsMuted: Boolean
        get() = notificationsMutedState.value
        set(value) {
            notificationsMutedState.value = value
            prefs.edit().putBoolean(KEY_MUTED, value).apply()
        }

    // **Vibrazione, priorità e pallino sull'icona** (dal 30/09/2026, la
    // pagina Notifications). Di suo come prima: vibra, priorità alta, pallino.

    private val notificationVibrateState = mutableStateOf(true)
    var notificationVibrate: Boolean
        get() = notificationVibrateState.value
        set(value) {
            notificationVibrateState.value = value
            prefs.edit().putBoolean(KEY_VIBRATE, value).apply()
        }

    private val notificationPriorityState = mutableStateOf(NotificationPriority.HIGH)
    var notificationPriority: NotificationPriority
        get() = notificationPriorityState.value
        set(value) { notificationPriorityState.value = value; putEnum(KEY_PRIORITY, value) }

    private val notificationBadgeState = mutableStateOf(true)
    var notificationBadge: Boolean
        get() = notificationBadgeState.value
        set(value) {
            notificationBadgeState.value = value
            prefs.edit().putBoolean(KEY_BADGE, value).apply()
        }

    private val mutedUntilState = mutableStateOf<Long?>(null)

    /** Fino a quando le notifiche tacciono, o null se non c'è una pausa in corso. */
    var mutedUntil: Long?
        get() = mutedUntilState.value?.takeIf { it > System.currentTimeMillis() }
        set(value) {
            mutedUntilState.value = value
            prefs.edit().putLong(KEY_MUTED_UNTIL, value ?: 0L).apply()
        }

    // --- I colori usati di recente (dal 05/10/2026, chiesto dall'utente) ---

    private val recentColorsState = mutableStateOf<List<String>>(emptyList())

    /**
     * Gli ultimi colori applicati col selettore (`ColorPickerSheet`, lo
     * stesso in tutta l'app: testo, celle, tag, penne, barre), "#RRGGBB",
     * il più recente per primo, al massimo `RECENT_COLORS_MAX`.
     */
    val recentColors: List<String> get() = recentColorsState.value

    /** Un colore appena applicato va in cima; se c'era già, sale lì. */
    fun rememberColor(hex: String) {
        val next = recentColorsState.value.withRecentColor(hex)
        if (next == recentColorsState.value) return
        recentColorsState.value = next
        if (::prefs.isInitialized) prefs.edit().putString(KEY_RECENT_COLORS, next.joinToString(",")).apply()
    }

    const val RECENT_COLORS_MAX = 10

    // --- I colori preferiti (dal 20/10/2026, chiesto dall'utente) ---

    private val favoriteColorsState = mutableStateOf<List<String>>(emptyList())

    /**
     * I colori messi fra i preferiti con la stella del selettore
     * (`ColorPickerSheet`), "#RRGGBB", nell'ordine in cui sono stati
     * aggiunti, al massimo `FAVORITE_COLORS_MAX`. Restano finché non li si
     * toglie, a differenza dei recenti.
     */
    val favoriteColors: List<String> get() = favoriteColorsState.value

    /** La stella: un colore che non è fra i preferiti ci va in fondo; uno che c'è già se ne va. */
    fun toggleFavoriteColor(hex: String) {
        val next = favoriteColorsState.value.withFavoriteColorToggled(hex)
        if (next == favoriteColorsState.value) return
        favoriteColorsState.value = next
        if (::prefs.isInitialized) prefs.edit().putString(KEY_FAVORITE_COLORS, next.joinToString(",")).apply()
    }

    const val FAVORITE_COLORS_MAX = 40

    // --- Le emoji usate di recente e il colore della pelle (dal 07/10/2026) ---

    private val recentEmojisState = mutableStateOf<List<String>>(emptyList())

    /**
     * Le ultime emoji scelte come icona nel selettore (`IconPickerSheet`),
     * la più recente per prima, al massimo `RECENT_EMOJIS_MAX`: la
     * categoria "Recent" di Notion.
     */
    val recentEmojis: List<String> get() = recentEmojisState.value

    /** Un'emoji appena scelta va in cima; se c'era già, sale lì. */
    fun rememberEmoji(emoji: String) {
        val next = recentEmojisState.value.withRecentEmoji(emoji)
        if (next == recentEmojisState.value) return
        recentEmojisState.value = next
        if (::prefs.isInitialized) prefs.edit().putString(KEY_RECENT_EMOJIS, next.joinToString("\n")).apply()
    }

    const val RECENT_EMOJIS_MAX = 24

    private val iconColorState = mutableStateOf<String?>(null)

    /**
     * Il colore delle icone di Material della scheda "Icons" (dal 08/10/2026),
     * "#RRGGBB"; null per il colore del testo, il bianco del tema scuro.
     * Resta quello dell'ultima volta, come su Notion.
     */
    var iconColor: String?
        get() = iconColorState.value
        set(value) {
            iconColorState.value = value?.uppercase()?.takeIf { HEX_COLOR.matches(it) }
            if (::prefs.isInitialized) prefs.edit().putString(KEY_ICON_COLOR, iconColorState.value).apply()
        }

    private val webClipDarkState = mutableStateOf(false)

    /**
     * **"Dark mode" del Web Clipper** (dal 20/10/2026, chiesto dall'utente:
     * "ci sarebbe troppo bianco"): le foto delle pagine (Full Page, Region,
     * Article) col sito scurito. Resta com'era l'ultima volta.
     */
    var webClipDark: Boolean
        get() = webClipDarkState.value
        set(value) {
            webClipDarkState.value = value
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_WEB_CLIP_DARK, value).apply()
        }

    private val skinToneState = mutableStateOf(0)

    /** Il colore della pelle delle emoji di persone e mani: 0 il giallo, 1..5 dal più chiaro al più scuro. */
    var skinTone: Int
        get() = skinToneState.value
        set(value) {
            skinToneState.value = value.coerceIn(0, SKIN_TONE_COUNT)
            if (::prefs.isInitialized) prefs.edit().putInt(KEY_SKIN_TONE, skinToneState.value).apply()
        }

    // --- Correttore (dal 08/10/2026) ---

    private val proofingEnabledState = mutableStateOf(false)

    /**
     * Se il correttore è acceso in tutte le pagine — Workspace e Notebooks
     * — che non dicono altro dai loro tre puntini. Spento di partenza.
     */
    var proofingEnabled: Boolean
        get() = proofingEnabledState.value
        set(value) {
            proofingEnabledState.value = value
            if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_PROOFING, value).apply()
        }

    private val proofingLanguageState = mutableStateOf(ProofingLanguage.ENGLISH_US)

    /** La lingua del correttore, se la pagina non ne sceglie un'altra. All'inizio quella dell'app. */
    var proofingLanguage: ProofingLanguage
        get() = proofingLanguageState.value
        set(value) {
            if (!value.available) return
            proofingLanguageState.value = value
            if (::prefs.isInitialized) prefs.edit().putString(KEY_PROOFING_LANGUAGE, value.code).apply()
        }

    // --- "Paragraph" (dal 21/10/2026, chiesto dall'utente, come su Word) ---

    private val blockSpacingState = mutableStateOf(0)

    /**
     * Lo spazio in più fra un blocco e l'altro in tutte le pagine che non
     * dicono altro dai loro tre puntini, in punti. 0 = com'era.
     */
    var blockSpacing: Int
        get() = blockSpacingState.value
        set(value) {
            blockSpacingState.value = value.coerceIn(0, BLOCK_SPACING_STEPS.last())
            if (::prefs.isInitialized) prefs.edit().putInt(KEY_BLOCK_SPACING, blockSpacingState.value).apply()
        }

    private val listGapsState = mutableStateOf(ListGaps())

    /**
     * Lo spazio fra il numero, il pallino o la casella e il primo carattere
     * degli elenchi, uno per tutti o uno per tipo. Tutti null = com'era.
     */
    var listGaps: ListGaps
        get() = listGapsState.value
        set(value) {
            listGapsState.value = value
            if (::prefs.isInitialized) prefs.edit().putString(KEY_LIST_GAPS, ListGaps.encode(value)).apply()
        }

    // --- Utilità ---

    private inline fun <reified T : Enum<T>> enumOr(key: String, default: T): T =
        prefs.getString(key, null)
            ?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
            ?: default

    private fun putEnum(key: String, value: Enum<*>) {
        prefs.edit().putString(key, value.name).apply()
    }

    private const val KEY_THEME = "theme"
    private const val KEY_ANIMATIONS = "animations"
    private const val KEY_TITLE_POSITION = "title_position"
    private const val KEY_FULL_LINE_VIEW = "full_line_view"
    private const val KEY_SIDEBAR_PINNED = "sidebar_pinned"
    private const val KEY_SIDEBAR_WIDTH = "sidebar_width"
    private const val KEY_ZOOM = "zoom"
    private const val KEY_NUMBER_STYLE = "number_style"
    private const val KEY_DATE_STYLE = "date_style"
    private const val KEY_TIME_FORMAT = "time_format"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_TIME_ZONE = "time_zone"
    private const val KEY_STARTUP_MODE = "startup_mode"
    private const val KEY_STARTUP_PAGE = "startup_page"
    private const val KEY_SHARE_PAGE = "share_page"
    private const val KEY_LAST_ROUTE = "last_route"
    private const val KEY_TRASH_POLICY = "trash_policy"
    private const val KEY_FAVORITES_SORT = "favorites_sort"
    private const val KEY_FAVORITES_DESC = "favorites_desc"
    private const val KEY_SEARCH_CONTENT = "search_content"
    private const val KEY_NOTEBOOKS_COLLAPSED = "notebooks_collapsed"
    private const val KEY_SEARCH_TRASH = "search_trash"
    private const val KEY_SOUND_URI = "notification_sound_uri"
    private const val KEY_SOUND_NAME = "notification_sound_name"
    private const val KEY_MUTED = "notifications_muted"
    private const val KEY_MUTED_UNTIL = "notifications_muted_until"
    private const val KEY_VIBRATE = "notifications_vibrate"
    private const val KEY_PRIORITY = "notifications_priority"
    private const val KEY_BADGE = "notifications_badge"
    private const val KEY_RECENT_COLORS = "recent_colors"
    private const val KEY_FAVORITE_COLORS = "favorite_colors"
    private const val KEY_RECENT_EMOJIS = "recent_emojis"
    private const val KEY_SKIN_TONE = "skin_tone"
    private const val KEY_ICON_COLOR = "icon_color"
    private const val KEY_WEB_CLIP_DARK = "web_clip_dark"
    private const val KEY_PROOFING = "proofing"
    private const val KEY_PROOFING_LANGUAGE = "proofing_language"
    private const val KEY_BLOCK_SPACING = "paragraph_block_spacing"
    private const val KEY_LIST_GAPS = "paragraph_list_gaps"
}

private val HEX_COLOR = Regex("#[0-9A-F]{6}")

/** Le emoji recenti dopo averne scelta una: lei in cima, senza doppioni. */
fun List<String>.withRecentEmoji(emoji: String, max: Int = AppSettings.RECENT_EMOJIS_MAX): List<String> {
    val clean = emoji.trim()
    if (clean.isEmpty() || clean.contains('\n')) return this
    return (listOf(clean) + filterNot { it == clean }).take(max)
}

/**
 * I colori recenti con `hex` in cima: maiuscolo, senza doppioni, al massimo
 * `max`. Un codice che non è un colore "#RRGGBB" lascia la lista com'è.
 */
fun List<String>.withRecentColor(hex: String, max: Int = AppSettings.RECENT_COLORS_MAX): List<String> {
    val clean = hex.trim().uppercase().let { if (it.startsWith("#")) it else "#$it" }
    if (!HEX_COLOR.matches(clean)) return this
    return (listOf(clean) + filterNot { it == clean }).take(max)
}

/**
 * I colori preferiti dopo aver toccato la stella su `hex`: se c'era se ne va,
 * se no va in fondo (maiuscolo, col "#"), finché ci sono meno di `max`
 * colori. Un codice che non è un colore "#RRGGBB" lascia la lista com'è.
 */
fun List<String>.withFavoriteColorToggled(hex: String, max: Int = AppSettings.FAVORITE_COLORS_MAX): List<String> {
    val clean = hex.trim().uppercase().let { if (it.startsWith("#")) it else "#$it" }
    if (!HEX_COLOR.matches(clean)) return this
    return when {
        clean in this -> filterNot { it == clean }
        size >= max -> this
        else -> this + clean
    }
}
