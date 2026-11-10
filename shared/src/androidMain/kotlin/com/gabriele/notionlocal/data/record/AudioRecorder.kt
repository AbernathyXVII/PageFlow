package com.gabriele.notionlocal.data.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gabriele.notionlocal.platform.mainActivityIntent
import com.gabriele.notionlocal.shared.R
import com.gabriele.notionlocal.data.PageImageStore
import com.gabriele.notionlocal.ui.i18n.CaptureStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * **Registrare l'audio da un blocco** (dal 03/11/2026, chiesto dall'utente:
 * "registrare l'audio dall'app, come Evernote").
 *
 * Una registrazione alla volta, di tutta l'app: parte da un blocco audio
 * vuoto e, fermata, il file va in quel blocco (`PageRepository.putRecording`),
 * anche se intanto si è usciti dalla pagina. Mentre registra c'è un servizio
 * in primo piano ([RecordingService]) con la sua notifica, così il microfono
 * resta acceso a schermo spento o con l'app chiusa, e dalla notifica si ferma.
 */
actual object AudioRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _current = MutableStateFlow<Recording?>(null)
    actual val current: StateFlow<Recording?> = _current
    private var recorder: MediaRecorder? = null

    /** Comincia a registrare per il blocco [blockId]. Falso se ce n'è già una o il microfono non parte. */
    @Synchronized
    actual fun start(context: Context, blockId: String, pageId: String): Boolean {
        if (_current.value != null) return false
        val appContext = context.applicationContext
        val name = "${UUID.randomUUID()}.m4a"
        val file = PageImageStore(appContext).fileFor(name)
        val media = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            media.setAudioSource(MediaRecorder.AudioSource.MIC)
            media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            media.setAudioChannels(1)
            media.setAudioSamplingRate(44_100)
            media.setAudioEncodingBitRate(96_000)
            media.setOutputFile(file.path)
            media.prepare()
            media.start()
            recorder = media
            _current.value = Recording(blockId, pageId, name, System.currentTimeMillis())
            runCatching { RecordingService.start(appContext) }
            true
        } catch (e: Exception) {
            media.release()
            file.delete()
            false
        }
    }

    /** Quanto è forte il suono adesso, da 0 a 1: la barretta che si muove. */
    actual fun level(): Float = runCatching { (recorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f).coerceIn(0f, 1f)

    /**
     * Ferma la registrazione. Con [keep] il file va nel suo blocco; senza, o
     * se è troppo corta per essere un file vero, se ne va.
     */
    actual fun stop(context: Context, keep: Boolean) {
        val appContext = context.applicationContext
        val (recording, media) = synchronized(this) {
            val now = _current.value ?: return
            val media = recorder
            recorder = null
            _current.value = null
            now to media
        }
        val stopped = runCatching { media?.stop() }.isSuccess
        media?.release()
        runCatching { RecordingService.stop(appContext) }
        scope.launch { placeRecording(appContext, recording, "audio/mp4", keep && stopped) }
    }
}

/**
 * **Il servizio della registrazione**: tiene l'app viva col microfono acceso
 * e mostra la notifica col tempo che passa e "Stop". Vedi [AudioRecorder].
 */
class RecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AudioRecorder.stop(this)
            return START_NOT_STICKY
        }
        val recording = AudioRecorder.current.value
        if (recording == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = notificationFor(recording)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    private fun notificationFor(recording: Recording): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CHANNEL, CaptureStrings.channel, NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                }
            )
        }
        val open = mainActivityIntent(this).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val stop = Intent(this, RecordingService::class.java).setAction(ACTION_STOP)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(CaptureStrings.recordingTitle)
            .setWhen(recording.startedAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .addAction(0, CaptureStrings.stop, PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    companion object {
        private const val CHANNEL = "recordings"
        private const val NOTIFICATION_ID = 0x5EC0
        private const val ACTION_STOP = "com.gabriele.notionlocal.record.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecordingService::class.java))
        }
    }
}
