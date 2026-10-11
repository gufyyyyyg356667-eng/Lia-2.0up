package com.Lia.assistant.access

import android.content.Context
import com.Lia.assistant.action.AgentTrace
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.text.GeminiTextClient
import kotlinx.coroutines.runBlocking

/**
 * Runs a request as an ordered list of subtasks. Stops at the first failure or at a yes/no question,
 * and continues from the same subtask after the user answers.
 */
object ScreenTasks {
    private val lock = Any()
    private val queue = ArrayDeque<String>()
    @Volatile private var awaitingYes = false

    fun run(ctx: Context, goal: String, confirmed: Boolean): ScreenAgent.Outcome = synchronized(lock) {
        if (confirmed && awaitingYes) {
            awaitingYes = false
            return finish(drain(ctx, approvedFirst = true))
        }
        if (goal.isBlank()) return ScreenAgent.Outcome.Stopped("Tell me what to do on the phone.")
        queue.clear()
        queue.addAll(plan(ctx, goal))
        awaitingYes = false
        return finish(drain(ctx, approvedFirst = false))
    }

    private fun finish(outcome: ScreenAgent.Outcome): ScreenAgent.Outcome {
        AgentTrace.add("Finished: ${outcome.message}")
        return outcome
    }

    private fun drain(ctx: Context, approvedFirst: Boolean): ScreenAgent.Outcome {
        val done = mutableListOf<String>()
        var approve = approvedFirst
        while (queue.isNotEmpty()) {
            val task = queue.first()
            when (val outcome = ScreenAgent.run(ctx, task, confirmed = approve)) {
                is ScreenAgent.Outcome.Done -> {
                    queue.removeFirst()
                    done += outcome.message
                }
                is ScreenAgent.Outcome.NeedsConfirmation -> {
                    // Keep the current task at the front so it resumes after the user's yes.
                    awaitingYes = true
                    return ScreenAgent.Outcome.NeedsConfirmation("${outcome.message} (step: $task)")
                }
                is ScreenAgent.Outcome.Stopped -> {
                    queue.clear()
                    return ScreenAgent.Outcome.Stopped(
                        "Stopped at \"$task\": ${outcome.message}. Done so far: ${done.ifEmpty { listOf("nothing") }.joinToString("; ")}"
                    )
                }
            }
            approve = false
        }
        return ScreenAgent.Outcome.Done(done.joinToString("; ").ifBlank { "Finished." })
    }

    private fun plan(ctx: Context, goal: String): List<String> {
        if (!Prefs.hasApiKey(ctx)) return listOf(goal)
        val reply = runBlocking {
            GeminiTextClient.reply(Prefs.getApiKey(ctx), TaskPlan.systemPrompt, listOf(GeminiTextClient.ChatTurn(true, goal)))
        }
        val text = (reply as? GeminiTextClient.Reply.Success)?.text ?: run {
            AgentTrace.add("Planner error, running the request as one step: ${(reply as? GeminiTextClient.Reply.Error)?.message}")
            return listOf(goal)
        }
        return TaskPlan.parse(text) ?: listOf(goal)
    }
}
