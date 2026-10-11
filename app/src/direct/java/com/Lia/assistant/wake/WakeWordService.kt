package com.Lia.assistant.wake

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.Lia.assistant.alerts.WakeStatus
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.voice.VoiceForegroundService
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * Listens offline for the wake name using Vosk. When heard, it starts the voice session and greets the user.
 * Listening pauses while the voice session runs (the microphone is shared) and resumes when it ends.
 */
class WakeWordService : Service() {

    @Volatile private var listening = false
    private var thread: Thread? = null
    private var model: Model? = null
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var resumeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            WakeStatus.set("microphone permission missing")
            stopSelf()
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(NotificationChannel(CHANNEL, "Lia listening", NotificationManager.IMPORTANCE_LOW))
        val note: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Lia")
            .setContentText("Say my name and I will answer")
            .setOngoing(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTE_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTE_ID, note)
            }
        } catch (e: Exception) {
            WakeStatus.set("could not start: ${e.javaClass.simpleName}")
            stopSelf()
            return
        }
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        resumeJob?.cancel()
        stopLoop()
        model?.close()
        model = null
        WakeStatus.set("off")
        super.onDestroy()
    }

    private fun startListening() {
        if (model != null) {
            startLoop()
            return
        }
        // Loading the model copies and reads many megabytes, so it runs off the main thread.
        Thread {
            try {
                val dir = modelDir()
                if (dir == null) {
                    main.post { WakeStatus.set("model missing: put the Vosk model in assets/wake/model") }
                    return@Thread
                }
                model = Model(dir.absolutePath)
                main.post { startLoop() }
            } catch (e: Throwable) {
                main.post { WakeStatus.set("failed: ${e.javaClass.simpleName}") }
            }
        }.apply { name = "lia-wake-model"; start() }
    }

    private fun startLoop() {
        if (listening) return
        listening = true
        WakeStatus.set("listening")
        thread = Thread { listenLoop() }.apply { name = "lia-wake"; start() }
    }

    private fun stopLoop() {
        listening = false
        thread?.join(1500)
        thread = null
    }

    /** Reads microphone audio and feeds it to the recogniser, restricted to the wake name. */
    private fun listenLoop() {
        val loaded = model ?: run {
            listening = false
            return
        }
        val wake = Prefs.wakeName(this).trim().lowercase()
        val grammar = "[\"$wake\", \"[unk]\"]"
        val rate = 16000
        val bufSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val recognizer = Recognizer(loaded, rate.toFloat(), grammar)
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize * 2)
        } catch (e: SecurityException) {
            WakeStatus.set("microphone permission missing")
            listening = false
            recognizer.close()
            return
        }
        try {
            record.startRecording()
            val buffer = ByteArray(4096)
            while (listening) {
                val n = record.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                val finished = recognizer.acceptWaveForm(buffer, n)
                val heard = if (finished) {
                    JSONObject(recognizer.result).optString("text")
                } else {
                    JSONObject(recognizer.partialResult).optString("partial")
                }
                if (heard.split(" ").any { it == wake }) {
                    listening = false
                    main.post { onWake() }
                    break
                }
            }
        } finally {
            runCatching { record.stop() }
            record.release()
            recognizer.close()
        }
    }

    private fun onWake() {
        WakeStatus.set("heard")
        // Wait until the wake listener has really let go of the microphone, then start the voice session.
        Thread {
            stopLoop()
            main.post { VoiceForegroundService.start(this@WakeWordService) }
        }.apply { name = "lia-wake-release"; start() }
        scope.launch {
            // Wait for the live session to connect, then greet.
            VoiceSessionManager.isActive.filter { it }.first()
            delay(2500)
            VoiceSessionManager.announce("The user just called your name. Greet them in Bangla: say হ্যাঁ, আমি এসেছি, বলুন।")
            // Resume listening when the conversation ends.
            resumeJob = scope.launch {
                VoiceSessionManager.isActive.filter { !it }.first()
                startLoop()
            }
        }
    }

    /** Copies the model from assets to a folder the engine can read. Returns null if no model is bundled. */
    private fun modelDir(): File? {
        val files = assets.list("wake/model")
        if (files.isNullOrEmpty()) return null
        val target = File(filesDir, "wake-model")
        if (!target.exists()) copyAssets("wake/model", target)
        return target
    }

    private fun copyAssets(path: String, dest: File) {
        val children = assets.list(path) ?: return
        if (children.isEmpty()) {
            dest.parentFile?.mkdirs()
            assets.open(path).use { input -> dest.outputStream().use { input.copyTo(it) } }
            return
        }
        dest.mkdirs()
        for (child in children) copyAssets("$path/$child", File(dest, child))
    }

    companion object {
        private const val CHANNEL = "lia_wake"
        private const val NOTE_ID = 4412
    }
}
