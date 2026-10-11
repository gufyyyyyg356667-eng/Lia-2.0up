package com.Lia.assistant.forge

import android.content.Context
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.text.GeminiTextClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Process-wide Website Forge. Runs the build on its own scope, so it keeps going when the screen closes.
 * Progress comes from the real stream and is published at most every 120 ms.
 */
object ForgeController {

    enum class Phase { IDLE, BUILDING, DONE, FAILED, CANCELLED }

    data class Metrics(val chars: Int = 0, val sections: Int = 0, val elapsedSec: Int = 0, val tokensPerSec: Int = 0)

    data class UiState(
        val phase: Phase = Phase.IDLE,
        val prompt: String = "",
        val isEdit: Boolean = false,
        val stage: ForgeStage = ForgeStage.READING,
        val progress: Float = 0f,
        val metrics: Metrics = Metrics(),
        val sections: List<String> = emptyList(),
        val log: List<String> = emptyList(),
        val logSeq: Int = 0,
        val buildId: Long = 0,
        val file: String? = null,
        val error: String? = null,
    )

    sealed class StartResult(val message: String) {
        class Started(message: String) : StartResult(message)
        class MissingKey(message: String) : StartResult(message)
        class Busy(message: String) : StartResult(message)
        class NothingToEdit(message: String) : StartResult(message)
        class EmptyPrompt(message: String) : StartResult(message)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _pendingOpen = MutableStateFlow(false)
    val pendingOpen: StateFlow<Boolean> = _pendingOpen

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var buildCounter = 0L

    /** The last finished page, used as the base for edits. */
    @Volatile private var currentHtml: String? = null

    fun requestOpen() {
        _pendingOpen.value = true
    }

    fun consumeOpen() {
        _pendingOpen.value = false
    }

    fun start(ctx: Context, prompt: String): StartResult {
        if (_state.value.phase == Phase.BUILDING) {
            return StartResult.Busy("I'm already building a website. Say cancel to stop it first.")
        }
        val text = prompt.trim()
        if (text.isEmpty()) return StartResult.EmptyPrompt("Tell me what the website is about.")
        if (!Prefs.hasApiKey(ctx)) return StartResult.MissingKey("Add your Gemini API key in Settings first.")
        return launch(ctx, text, ForgeSystemPrompt.forNewSite(text), isEdit = false)
    }

    fun edit(ctx: Context, change: String): StartResult {
        if (_state.value.phase == Phase.BUILDING) {
            return StartResult.Busy("I'm still building. Wait for it to finish, then ask me to change it.")
        }
        val html = currentHtml ?: return StartResult.NothingToEdit("Build a website first, then ask me to change it.")
        val text = change.trim()
        if (text.isEmpty()) return StartResult.EmptyPrompt("Tell me what to change.")
        if (!Prefs.hasApiKey(ctx)) return StartResult.MissingKey("Add your Gemini API key in Settings first.")
        return launch(ctx, text, ForgeSystemPrompt.forEdit(html, text), isEdit = true)
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(phase = Phase.CANCELLED, error = "Cancelled")
    }

    /** Back to the start screen. A build that is running is cancelled. */
    fun reset() {
        job?.cancel()
        job = null
        _state.value = UiState(buildId = buildCounter)
    }

    /** Loads a saved page as the current one, so it can be edited. */
    fun openSaved(ctx: Context, fileName: String) {
        val html = readSaved(ctx, fileName) ?: return
        currentHtml = html
        _state.value = UiState(phase = Phase.DONE, file = fileName, progress = 1f, stage = ForgeStage.READY)
    }

    fun readSaved(ctx: Context, fileName: String): String? =
        runCatching { File(websitesDir(ctx), fileName).readText() }.getOrNull()

    fun delete(ctx: Context, fileName: String) {
        File(websitesDir(ctx), fileName).delete()
        if (_state.value.file == fileName) {
            currentHtml = null
            _state.value = UiState()
        }
    }

    private fun websitesDir(ctx: Context): File = File(ctx.applicationContext.filesDir, "Websites").apply { mkdirs() }

    private fun launch(ctx: Context, prompt: String, system: String, isEdit: Boolean): StartResult {
        val app = ctx.applicationContext
        val apiKey = Prefs.getApiKey(app)
        buildCounter++
        val startedAt = System.currentTimeMillis()
        _state.value = UiState(
            phase = Phase.BUILDING,
            prompt = prompt,
            isEdit = isEdit,
            log = listOf("Reading your idea"),
            logSeq = 1,
            buildId = buildCounter,
        )
        job = scope.launch { runBuild(app, apiKey, system, prompt, startedAt) }
        return StartResult.Started(
            if (isEdit) "Okay, updating the website." else "Building your website now. Watch the Forge."
        )
    }

    private suspend fun runBuild(app: Context, apiKey: String, system: String, prompt: String, startedAt: Long) {
        try {
            val model = GeminiTextClient.resolveModel(apiKey)
                ?: throw ForgeError("No Gemini model is available for this key.")

            val first = generate(app, apiKey, model, system, prompt, startedAt)
            var html = ForgeHtml.stripFences(first.text)
            if (html.length < 1500 || !html.lowercase().contains("</html>") || first.finishReason == "RECITATION") {
                val retry = generate(app, apiKey, model, system + "\n\n" + ForgeSystemPrompt.RETRY_HINT, prompt, startedAt)
                html = ForgeHtml.stripFences(retry.text)
            }

            if (!ForgeHtml.looksLikeHtml(html)) {
                throw ForgeError("Gemini did not return a web page. Try describing it differently.")
            }
            if (!html.lowercase().contains("</html>")) {
                if (html.length >= 14_000) {
                    html = ForgeHtml.repairTruncated(html)
                } else {
                    throw ForgeError("Gemini stopped before finishing. Try again.")
                }
            }
            html = ForgeHtml.withSafetyNet(html)

            val file = save(app, html)
            currentHtml = html
            val s = _state.value
            _state.value = s.copy(
                phase = Phase.DONE,
                stage = ForgeStage.READY,
                progress = 1f,
                file = file,
                log = (s.log + "Saved your website").takeLast(12),
                logSeq = s.logSeq + 1,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ForgeError) {
            fail(e.message ?: "Something went wrong.")
        } catch (e: Exception) {
            fail("Something went wrong while building the website.")
        }
    }

    private class Attempt(val text: String, val finishReason: String?)

    /** One streamed generation. A busy (503) answer is retried once after 4 s. */
    private suspend fun generate(
        app: Context,
        apiKey: String,
        model: String,
        system: String,
        prompt: String,
        startedAt: Long,
    ): Attempt {
        val tracker = ProgressTracker(startedAt, read = { _state.value }, write = { _state.value = it })
        return try {
            streamOnce(apiKey, model, system, prompt, tracker)
        } catch (e: ForgeError) {
            if (!e.retryable) throw e
            delay(4_000)
            streamOnce(apiKey, model, system, prompt, tracker)
        }
    }

    private suspend fun streamOnce(
        apiKey: String,
        model: String,
        system: String,
        prompt: String,
        tracker: ProgressTracker,
    ): Attempt {
        val sb = StringBuilder()
        val ended = WebsiteForgeClient.stream(apiKey, model, system, prompt) { chunk ->
            sb.append(chunk)
            tracker.onChunk(sb.length) { sb.toString() }
        }
        return Attempt(sb.toString(), ended.finishReason)
    }

    private fun fail(message: String) {
        val s = _state.value
        _state.value = s.copy(phase = Phase.FAILED, error = message, log = (s.log + message).takeLast(12), logSeq = s.logSeq + 1)
    }

    private fun save(app: Context, html: String): String {
        val name = "website_${System.currentTimeMillis()}.html"
        File(websitesDir(app), name).writeText(html)
        return name
    }

    /** Turns streamed text into stage, progress, section and log updates. */
    private class ProgressTracker(
        private val startedAt: Long,
        private val read: () -> UiState,
        private val write: (UiState) -> Unit,
    ) {
        private var lastEmit = 0L
        private var lastStage = ForgeStage.READING
        private var lastLoggedKb = 0
        private val seenSections = LinkedHashSet<String>()

        fun onChunk(chars: Int, snapshot: () -> String) {
            val now = System.currentTimeMillis()
            if (now - lastEmit < 120) return
            lastEmit = now

            val html = snapshot()
            val stage = ForgeStageDetector.detect(html)
            val logs = mutableListOf<String>()
            if (stage != lastStage) {
                lastStage = stage
                logs += stage.label
            }
            val kb = chars / 1024
            if (kb >= lastLoggedKb + 10) {
                lastLoggedKb = kb
                logs += "Wrote $kb KB"
            }
            for (name in ForgeStageDetector.sectionNames(html)) {
                if (seenSections.add(name)) logs += "Laying out $name"
            }

            val s = read()
            val elapsed = ((now - startedAt) / 1000).toInt()
            val log = if (logs.isEmpty()) s.log else (s.log + logs).takeLast(12)
            write(
                s.copy(
                    stage = stage,
                    progress = ForgeStageDetector.progress(stage, chars),
                    metrics = Metrics(
                        chars = chars,
                        sections = seenSections.size,
                        elapsedSec = elapsed,
                        tokensPerSec = (chars / 4) / maxOf(elapsed, 1),
                    ),
                    sections = seenSections.toList(),
                    log = log,
                    logSeq = s.logSeq + logs.size,
                )
            )
        }
    }
}
