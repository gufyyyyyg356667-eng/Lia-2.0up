package com.Lia.assistant.access

import android.content.Context
import com.Lia.assistant.action.ActionExecutor
import com.Lia.assistant.action.AgentTrace
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.text.GeminiTextClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Looks at the screen, picks one step, runs it, and checks the result, until the goal is done.
 * Stops for a yes/no on risky taps, when it needs information, or after [MAX_STEPS].
 */
object ScreenAgent {
    const val MAX_STEPS = 40

    /** Set when the user asks to stop; the running task checks it before each step. */
    @Volatile var cancelRequested: Boolean = false
    private val failedStatuses = setOf(
        "element_not_found", "failed", "no_focused_field", "nothing_scrollable", "search_not_found",
        "no_search_field", "search_tap_failed", "bad_number", "app_not_found", "unsupported",
    )
    private const val TIME_LIMIT_MS = 300_000L

    sealed class Outcome(val message: String) {
        class Done(message: String) : Outcome(message)
        class NeedsConfirmation(message: String) : Outcome(message)
        class Stopped(message: String) : Outcome(message)
    }

    private data class Snapshot(val app: String, val screen: String)

    private fun recent(steps: List<String>): String =
        if (steps.isEmpty()) "none" else steps.takeLast(3).joinToString("; ")

    /** Reads the current screen now. Never reuses an older reading. */
    private fun readFresh(ctx: Context): Snapshot {
        val r = ScreenTools.execute(ctx, "read_screen", JSONObject())
        val status = r?.optString("result").orEmpty()
        if (status != "ok") AgentTrace.add("Screen read failed: ${status.ifEmpty { "no response" }}")
        else AgentTrace.add("Read: app=${r?.optString("app")}, ${r?.optString("stats")}")
        return Snapshot(r?.optString("app").orEmpty(), r?.optString("screen").orEmpty())
    }

    /** After an action, waits (up to about 4 s) until the app or the visible labels change. */
    private fun waitForChange(ctx: Context, before: Snapshot, rounds: Int) {
        repeat(rounds) {
            Thread.sleep(250)
            val now = readFresh(ctx)
            if (now.app != before.app || now.screen != before.screen) return
        }
    }

    /** A risky tap the user has not approved yet. */
    @Volatile private var pendingTap: String? = null

    fun run(ctx: Context, goal: String, confirmed: Boolean): Outcome {
        if (goal.isBlank()) return Outcome.Stopped("Tell me what to do on the phone.")
        if (!Prefs.hasApiKey(ctx)) return Outcome.Stopped("Add your Gemini API key in Settings first.")
        val enabled = NovaAccessibilityService.isEnabled(ctx)
        val connected = NovaAccessibilityService.instance != null
        AgentTrace.add("Screen service: enabled=$enabled, connected=$connected")
        if (!enabled) return Outcome.Stopped("Screen control is off. Turn it on in Settings > Accessibility > Lia AI screen control.")
        if (!connected) return Outcome.Stopped("Screen control is on but disconnected. In Lia's Permissions, turn Screen control off and on, then try again.")

        AgentTrace.start(goal)
        cancelRequested = false
        val startedAt = System.currentTimeMillis()
        val approved = if (confirmed) pendingTap else null
        pendingTap = null
        val apiKey = Prefs.getApiKey(ctx)
        val system = AgentPrompt.system(Prefs.assistantName(ctx))
        val steps = mutableListOf<String>()
        var invalidReplies = 0

        repeat(MAX_STEPS) {
            if (cancelRequested) {
                cancelRequested = false
                return Outcome.Stopped("Stopped at your request.")
            }
            if (System.currentTimeMillis() - startedAt > TIME_LIMIT_MS) {
                return Outcome.Stopped("I ran out of time. Last steps: ${recent(steps)}")
            }
            val snapshot = readFresh(ctx)
            if (snapshot.app.isBlank() && snapshot.screen.isBlank()) {
                return Outcome.Stopped("I cannot read the screen right now. Turn Screen control off and on in Permissions, then try again.")
            }
            val turn = AgentPrompt.turn(
                goal, steps, snapshot.app, snapshot.screen, approved,
                com.Lia.assistant.action.AppMemory.linesFor(ctx, snapshot.app),
            )
            val reply = runBlocking {
                GeminiTextClient.reply(apiKey, system, listOf(GeminiTextClient.ChatTurn(true, turn)))
            }
            val text = when (reply) {
                is GeminiTextClient.Reply.Success -> reply.text
                is GeminiTextClient.Reply.Error -> {
                    AgentTrace.add("AI error: ${reply.message}")
                    return Outcome.Stopped(reply.message)
                }
            }

            val decision = AgentDecision.parse(text)
            AgentTrace.add("AI: ${decision?.let { it.toString().take(160) } ?: "unreadable reply: ${text.take(120)}"}")
            when (decision) {
                null -> {
                    invalidReplies++
                    if (invalidReplies >= 3) return Outcome.Stopped("I could not work out the next step. Last steps: ${recent(steps)}")
                    // Ask again next round; the screen is re-read first.
                }
                is AgentDecision.Done -> return Outcome.Done(decision.summary)
                is AgentDecision.Ask -> return Outcome.Stopped(decision.question)
                is AgentDecision.Act -> {
                    val args = decision.args.toMutableMap()
                    if (decision.name == "tap_text" && approved != null && args["text"] == approved) {
                        args["confirmed"] = "yes"
                    }
                    val result = ActionExecutor.execute(ctx, decision.name, JSONObject(args))
                    val status = result.optString("result")
                    if (status == "needs_confirmation") {
                        val label = args["text"].orEmpty()
                        pendingTap = label
                        return Outcome.NeedsConfirmation("Should I tap \"$label\"? Say yes to continue.")
                    }
                    if (status == "license_required") return Outcome.Stopped(result.optString("message"))
                    steps += "${decision.name} $args -> $status"
                    AgentTrace.add("Did: ${decision.name} $args -> $status")
                    if (status in failedStatuses) {
                        steps += "NOTE: that step FAILED ($status). Read the screen again and pick a different label or approach. Do not repeat the same step."
                    }
                    if (steps.size >= 3 && steps.takeLast(3).distinct().size == 1) {
                        steps += "NOTE: this same step repeated 3 times with no progress. Try a different approach."
                    }
                    when (decision.name) {
                        // Changes that do not move to another screen: no waiting.
                        "set_flashlight", "change_volume", "set_alarm", "set_timer", "take_screenshot" -> Unit
                        // Opening apps or pages takes longer to load.
                        "open_app", "search_youtube", "web_search", "open_url", "open_settings" -> waitForChange(ctx, snapshot, rounds = 12)
                        // Taps, typing and scrolling: the screen usually changes quickly.
                        else -> waitForChange(ctx, snapshot, rounds = 6)
                    }
                }
            }
        }
        return Outcome.Stopped("I stopped after $MAX_STEPS steps. Last steps: ${recent(steps)}")
    }
}
