package com.gabriele.notionlocal.ui.screen

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gabriele.notionlocal.ui.i18n.BlockStrings
import com.gabriele.notionlocal.ui.theme.AccentBlue
import com.gabriele.notionlocal.ui.theme.NotionGray400
import com.gabriele.notionlocal.ui.theme.NotionWhite
import kotlinx.coroutines.delay
import com.gabriele.notionlocal.platform.LocalPlatformContext

/**
 * **Il lettore di un video o di un audio** dentro la pagina (dal
 * 06/10/2026): il `MediaPlayer` di Android, che legge i file dell'app e gli
 * indirizzi in rete, con quello che serve ai comandi — a che punto è, quanto
 * dura, se suona. Si prepara solo quando lo si fa partire la prima volta:
 * una pagina con dieci audio non ne carica dieci all'apertura.
 */
@Stable
private class Playback(private val context: Context, private val source: Uri) {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var startWhenReady = false

    var prepared by mutableStateOf(false)
    var preparing by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var failed by mutableStateOf(false)
    var duration by mutableIntStateOf(0)
    var position by mutableIntStateOf(0)
    /** Larghezza su altezza del video; 16:9 finché non lo si sa. */
    var ratio by mutableFloatStateOf(16f / 9f)

    /** Il quadro su cui disegnare il video (null per l'audio, o quando sparisce). */
    fun attachSurface(texture: SurfaceTexture?) {
        surface?.release()
        surface = texture?.let { Surface(it) }
        runCatching { player?.setSurface(surface) }
    }

    /** Prepara senza far partire: per un video, mostra la prima immagine e la sua forma. */
    fun prepare() {
        if (player != null || failed) return
        preparing = true
        val created = MediaPlayer()
        player = created
        runCatching {
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            created.setDataSource(context, source)
            surface?.let { created.setSurface(it) }
            created.setOnPreparedListener { mp ->
                prepared = true
                preparing = false
                duration = mp.duration.coerceAtLeast(0)
                if (mp.videoWidth > 0 && mp.videoHeight > 0) ratio = mp.videoWidth.toFloat() / mp.videoHeight
                // La prima immagine, invece del nero.
                if (!startWhenReady) mp.seekTo(1) else play()
            }
            created.setOnVideoSizeChangedListener { _, w, h -> if (w > 0 && h > 0) ratio = w.toFloat() / h }
            created.setOnCompletionListener {
                playing = false
                position = duration
            }
            created.setOnErrorListener { _, _, _ ->
                failed = true
                preparing = false
                playing = false
                true
            }
            created.prepareAsync()
        }.onFailure {
            failed = true
            preparing = false
        }
    }

    fun play() {
        val mp = player
        if (mp == null || !prepared) {
            startWhenReady = true
            prepare()
            return
        }
        runCatching {
            // Arrivato in fondo, ricomincia.
            if (position >= duration && duration > 0) mp.seekTo(0)
            mp.start()
            playing = true
        }
    }

    fun pause() {
        startWhenReady = false
        // Solo se suona: fermare un lettore mai partito per Android è un
        // errore, e l'audio diventava "non riproducibile" (visto il 07/10).
        if (playing) runCatching { player?.pause() }
        playing = false
    }

    fun seekTo(ms: Int) {
        position = ms
        if (prepared) runCatching { player?.seekTo(ms) }
    }

    /** A che punto è adesso: lo chiede chi disegna la barra, mentre suona. */
    fun refresh() {
        if (prepared) runCatching { position = player?.currentPosition ?: position }
    }

    fun release() {
        runCatching { player?.release() }
        player = null
        surface?.release()
        surface = null
        prepared = false
        playing = false
    }
}

/** "1:05", o "1:02:07" oltre l'ora. */
fun formatPlayTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/**
 * **Uscendo dall'app o dalla pagina il lettore si ferma** (Home, un'altra
 * app, lo schermo spento, un'altra pagina): niente suono da una pagina che
 * non si vede più. Tornando non riparte da solo.
 */
@Composable
private fun OnLeaving(onLeave: () -> Unit, onReturn: () -> Unit = {}) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val leave by rememberUpdatedState(onLeave)
    val back by rememberUpdatedState(onReturn)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> leave()
                Lifecycle.Event.ON_START -> back()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

/** La posizione della barra mentre suona: quattro volte al secondo. */
@Composable
private fun FollowPosition(playback: Playback) {
    LaunchedEffect(playback.playing) {
        while (playback.playing) {
            playback.refresh()
            delay(250)
        }
    }
}

/**
 * **Play/pausa, il tempo e la barra col pallino**, come su YouTube: si
 * trascina il pallino per andare avanti o indietro, e il lettore ci salta
 * quando lo si lascia.
 */
@Composable
private fun PlaybackControls(playback: Playback, tint: Color, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if (playback.playing) playback.pause() else playback.play() },
            enabled = !playback.failed,
            modifier = Modifier.size(40.dp)
        ) {
            if (playback.preparing && !playback.prepared) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = tint)
            } else {
                Icon(
                    if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playback.playing) BlockStrings.pause else BlockStrings.play,
                    tint = tint
                )
            }
        }
        Text(
            formatPlayTime(dragging?.toInt() ?: playback.position),
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
        Slider(
            value = (dragging ?: playback.position.toFloat()).coerceIn(0f, playback.duration.coerceAtLeast(1).toFloat()),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { playback.seekTo(it.toInt()) }
                dragging = null
            },
            valueRange = 0f..playback.duration.coerceAtLeast(1).toFloat(),
            enabled = playback.prepared,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = tint.copy(alpha = 0.3f)
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Text(
            formatPlayTime(playback.duration),
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

/**
 * **Un video dentro la pagina**: un file dell'app o un indirizzo di un video
 * (un .mp4, un flusso). Toccando il video parte o si ferma; sotto, i
 * comandi. Se non si riesce a leggerlo lo dice, e dai sei puntini lo si apre
 * altrove.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InlineVideoPlayer(
    source: Uri,
    modifier: Modifier = Modifier,
    /** Il dito tenuto sul video: il menu del blocco. */
    onLongPress: (() -> Unit)? = null
) {
    val context = LocalPlatformContext.current
    val playback = remember(source) { Playback(context, source) }
    DisposableEffect(playback) { onDispose { playback.release() } }
    OnLeaving(onLeave = { playback.pause() })
    LaunchedEffect(playback) { playback.prepare() }
    FollowPosition(playback)

    Column(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(playback.ratio)
                .combinedClickable(
                    onClick = {
                        if (!playback.failed) {
                            if (playback.playing) playback.pause() else playback.play()
                        }
                    },
                    onLongClick = onLongPress
                ),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                                playback.attachSurface(texture)
                            }

                            override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit

                            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                                playback.attachSurface(null)
                                return true
                            }

                            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (playback.failed) {
                Text(
                    BlockStrings.cantPlay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NotionGray400,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (!playback.playing) {
                // Il triangolo grande in mezzo, finché è fermo.
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = BlockStrings.play, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
        PlaybackControls(playback, tint = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
    }
}

/**
 * **Un audio dentro la pagina**: la riga col nome e, sotto, play/pausa, il
 * tempo e la barra col pallino (settima foto dell'utente).
 */
@Composable
fun InlineAudioPlayer(source: Uri, title: String, modifier: Modifier = Modifier) {
    val context = LocalPlatformContext.current
    val playback = remember(source) { Playback(context, source) }
    DisposableEffect(playback) { onDispose { playback.release() } }
    OnLeaving(onLeave = { playback.pause() })
    // Preparato subito, come il video: così si vede quanto dura.
    LaunchedEffect(playback) { playback.prepare() }
    FollowPosition(playback)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = NotionWhite,
            maxLines = 1,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
        )
        if (playback.failed) {
            Text(
                BlockStrings.cantPlayAudio,
                style = MaterialTheme.typography.bodySmall,
                color = NotionGray400,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        PlaybackControls(playback, tint = NotionWhite, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        Spacer(modifier = Modifier.size(2.dp))
    }
}

/** L'id di un video di YouTube, da tutti i modi in cui si scrive il suo indirizzo; null se non lo è. */
actual fun youTubeId(url: String): String? {
    val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
    val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.")?.removePrefix("music.") ?: return null
    val id = when (host) {
        "youtu.be" -> uri.pathSegments.firstOrNull()
        "youtube.com", "youtube-nocookie.com" -> when (uri.pathSegments.firstOrNull()) {
            "watch" -> uri.getQueryParameter("v")
            "shorts", "embed", "live", "v" -> uri.pathSegments.getOrNull(1)
            else -> null
        }
        else -> null
    }
    return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) }
}

/** Il numero di un video di Vimeo ("vimeo.com/123456"); null se non lo è. */
actual fun vimeoId(url: String): String? {
    val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return null
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
    if (host != "vimeo.com" && host != "player.vimeo.com") return null
    return uri.pathSegments.lastOrNull { it.all(Char::isDigit) }
}

/**
 * **Un video di YouTube o di Vimeo dentro la pagina**, nel loro lettore
 * (play, pausa, la barra col pallino): è il modo in cui quei siti lasciano
 * vedere i loro video fuori da sé. Serve la rete.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun EmbeddedVideoPlayer(embedUrl: String, modifier: Modifier) {
    val shown = remember { WebViewRef() }
    OnLeaving(onLeave = { shown.view?.onPause() }, onReturn = { shown.view?.onResume() })
    val html = remember(embedUrl) {
        """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden}iframe{border:0;width:100%;height:100%}</style>
        </head><body><iframe src="$embedUrl" allow="autoplay; encrypted-media; picture-in-picture; fullscreen"></iframe></body></html>
        """.trimIndent()
    }
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                // Alto quanto il riquadro: lasciato "alto quanto il contenuto",
                // la pagina dentro si crede alta zero e il lettore non si vede.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = FullScreenChromeClient(ctx).also { shown.chrome = it }
                setBackgroundColor(android.graphics.Color.BLACK)
                // L'indirizzo "di casa" della pagina fa da referente per il
                // lettore: senza, YouTube rifiuta di mostrare il video.
                loadDataWithBaseURL("https://${ctx.packageName}", html, "text/html", "utf-8", null)
            }.also { shown.view = it }
        },
        onRelease = {
            if (shown.view === it) {
                shown.view = null
                shown.chrome?.close()
                shown.chrome = null
            }
            it.destroy()
        },
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(6.dp))
    )
}

/** Il WebView del lettore mentre è sullo schermo, per fermarlo quando si esce. */
private class WebViewRef {
    var view: WebView? = null
    var chrome: FullScreenChromeClient? = null
}

/**
 * **Il tasto "schermo intero" del lettore di YouTube e Vimeo**: il video
 * passa in una finestra nera che copre tutto, barre di sistema comprese.
 * Si esce col tasto del lettore o con Indietro. Senza, il tasto non faceva
 * nulla.
 */
private class FullScreenChromeClient(private val context: Context) : WebChromeClient() {
    private var dialog: Dialog? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        close()
        dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            setContentView(view)
            window?.let { window ->
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            // Indietro chiude la finestra: il lettore torna nella pagina.
            setOnDismissListener {
                dialog = null
                callback.onCustomViewHidden()
            }
            show()
        }
    }

    override fun onHideCustomView() = close()

    /** Chiude lo schermo intero, se è aperto. */
    fun close() {
        val open = dialog ?: return
        dialog = null
        open.setOnDismissListener(null)
        open.dismiss()
    }
}
