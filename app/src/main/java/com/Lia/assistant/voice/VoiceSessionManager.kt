package com.Lia.assistant.voice

import android.content.Context
import com.Lia.assistant.action.ActionExecutor
import com.Lia.assistant.action.LiaToolCatalog
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.license.AccessKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

enum class VoiceState { IDLE, CONNECTING, LISTENING, SPEAKING, ERROR }

/**
 * Owns the live voice session. It is a singleton, so the session keeps running when the screen closes.
 * The foreground service keeps the process alive; this object holds the state the UI reads.
 */
object VoiceSessionManager {
    private const val VOICE = "Aoede"
    private const val MAX_RECONNECTS = 5
    private const val RECONNECT_DELAY_MS = 1200L
    private const val SILENCE_MS = 300L

    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state
    private val _status = MutableStateFlow("Tap to talk")
    val statusText: StateFlow<String> = _status
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError
    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val memory = ConversationMemory()
    private val echo = EchoGuard()
    private var reconnects = 0
    private var reconnectJob: Job? = null
    private val _detected = MutableStateFlow(DetectedLanguage.UNKNOWN)
    val detectedLanguage: StateFlow<DetectedLanguage> = _detected
    val latency = VoiceLatencyTracker()

    @Volatile private var appCtx: Context? = null
    @Volatile private var client: GeminiLiveClient? = null
    @Volatile private var audio: LiveAudioIO? = null
    @Volatile private var epoch = 0

    fun start(ctx: Context) {
        if (_isActive.value) return
        val app = ctx.applicationContext
        if (!AccessKeyManager.isActive()) {
            fail(AccessKeyManager.refusalMessage())
            return
        }
        if (!Prefs.hasApiKey(app)) {
            fail("Add your Gemini API key in Settings first.")
            return
        }
        appCtx = app
        memory.clear()
        reconnects = 0
        _lastError.value = null
        _muted.value = false
        _isActive.value = true
        _state.value = VoiceState.CONNECTING
        _status.value = "Connecting..."
        audio = LiveAudioIO(
            onMicChunk = { chunk -> if (!_muted.value && !echo.isMicMuted()) client?.sendAudio(chunk) },
            onMicAmplitude = { _amplitude.value = it },
        )
        scope.launch { mutex.withLock { connectSession(resuming = false) } }
    }

    /** Applies changed personality or language. Live sessions cannot change instructions mid-stream, so reconnect. */
    fun refreshInstructions() {
        if (!_isActive.value) return
        scope.launch { mutex.withLock { if (_isActive.value) connectSession(resuming = true) } }
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    /** Speaks a short message in the running voice session. Does nothing if no session is live. */
    fun announce(text: String) {
        if (!_isActive.value) return
        client?.sendText("[Phone event] $text Say this to the user briefly, in the language they use.")
    }

    fun stop() {
        epoch++
        reconnectJob?.cancel()
        reconnectJob = null
        client?.disconnect()
        client = null
        audio?.release()
        audio = null
        echo.reset()
        memory.clear()
        _isActive.value = false
        _amplitude.value = 0f
        _state.value = VoiceState.IDLE
        _status.value = "Tap to talk"
    }

    private fun fail(message: String) {
        stop()
        _lastError.value = message
        _status.value = message
        _state.value = VoiceState.ERROR
    }

    private fun connectSession(resuming: Boolean) {
        val ctx = appCtx ?: return
        val myEpoch = ++epoch
        client?.disconnect()
        if (resuming) echo.reset()
        val name = Prefs.assistantName(ctx)
        val system = PromptBuilder.build(
            personality = Prefs.personality(ctx),
            language = Prefs.language(ctx),
            detected = _detected.value,
            recap = if (resuming) memory.recap() else null,
            assistantName = name,
            toolInstruction = LiaToolCatalog.instruction(name),
        )
        val c = GeminiLiveClient(listenerFor(myEpoch))
        client = c
        c.connect(Prefs.getApiKey(ctx), system, VOICE, LiaToolCatalog.declarations())
    }

    private fun scheduleReconnect() {
        if (!_isActive.value) return
        latency.onReconnect()
        if (reconnectJob?.isActive == true) return
        reconnects++
        if (reconnects > MAX_RECONNECTS) {
            fail("Connection lost - tap Stop and reopen Voice Mode")
            return
        }
        _state.value = VoiceState.CONNECTING
        _status.value = "Reconnecting..."
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            mutex.withLock { if (_isActive.value) connectSession(resuming = true) }
        }
    }

    private fun listenerFor(myEpoch: Int): GeminiLiveClient.Listener = object : GeminiLiveClient.Listener {
        private var playing = false
        private var silence: Job? = null
        private val assistantText = StringBuilder()

        private fun current() = myEpoch == epoch && _isActive.value

        override fun onSetupComplete() {
            if (!current()) return
            reconnects = 0
            _state.value = VoiceState.LISTENING
            _status.value = "Listening"
            if (audio?.isMicRunning() == false) {
                try {
                    audio?.startMic()
                } catch (e: Exception) {
                    fail("Microphone unavailable. Check the microphone permission.")
                }
            }
        }

        override fun onAudioChunk(pcm: ByteArray) {
            if (!current()) return
            if (!playing) {
                playing = true
                echo.notifyPlaybackStarted()
                latency.mark(VoiceLatencyTracker.Mark.FIRST_AUDIO)
            }
            _state.value = VoiceState.SPEAKING
            _status.value = "Speaking"
            _amplitude.value = audio?.playChunk(pcm) ?: 0f
            silence?.cancel()
            silence = scope.launch {
                delay(SILENCE_MS)
                endTurn()
            }
        }

        override fun onInputTranscript(text: String) {
            if (!current()) return
            memory.add(ConversationMemory.Role.USER, text)
            _detected.value = LanguageDetector.detect(text)
            latency.mark(VoiceLatencyTracker.Mark.SPEECH_SEEN)
        }

        override fun onOutputTranscript(text: String) {
            if (current()) assistantText.append(text)
        }

        override fun onToolCall(id: String, name: String, args: JSONObject) {
            if (!current()) return
            val ctx = appCtx ?: return
            if (name == "build_website") {
                val result = when (val r = if (AccessKeyManager.isActive()) ForgeController.start(ctx, args.optString("prompt")) else ForgeController.StartResult.MissingKey(AccessKeyManager.refusalMessage())) {
                    is ForgeController.StartResult.Started -> {
                        ForgeController.requestOpen()
                        JSONObject().put("result", "forge_started")
                    }
                    else -> JSONObject().put("result", "not_started").put("message", r.message)
                }
                client?.sendToolResponse(id, name, result)
                return
            }
            if (name == "run_screen_task") {
                client?.sendToolResponse(
                    id, name,
                    JSONObject().put("result", "started").put("note", "Working on it. I will report back when it is finished."),
                )
                scope.launch {
                    val r = ActionExecutor.execute(ctx, name, args)
                    val status = r.optString("result")
                    val message = r.optString("message").ifBlank { status }
                    if (current()) {
                        client?.sendText("[Screen task $status] $message. Tell the user the result briefly, in the language they use. If it needs their yes, ask that question.")
                    }
                }
                return
            }
            scope.launch {
                val result = ActionExecutor.execute(ctx, name, args)
                if (current()) client?.sendToolResponse(id, name, result)
            }
        }

        override fun onTurnComplete() {
            if (!current()) return
            latency.mark(VoiceLatencyTracker.Mark.TURN_DONE)
            latency.finishTurn()
            endTurn()
            val text = assistantText.toString()
            assistantText.clear()
            memory.add(ConversationMemory.Role.ASSISTANT, text)
        }

        override fun onInterrupted() {
            if (!current()) return
            audio?.flushPlayback()
            assistantText.clear()
            endTurn()
        }

        override fun onError(message: String) {
            if (current()) scheduleReconnect()
        }

        override fun onClosed() {
            if (current()) scheduleReconnect()
        }

        private fun endTurn() {
            silence?.cancel()
            silence = null
            if (playing) {
                playing = false
                echo.notifyPlaybackStopped()
            }
            if (current()) {
                _state.value = VoiceState.LISTENING
                _status.value = "Listening"
            }
        }
    }
}
