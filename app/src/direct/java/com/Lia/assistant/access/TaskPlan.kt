package com.Lia.assistant.access

import org.json.JSONObject

/** Reads the planner's reply into ordered subtasks. Pure, so it is unit-tested. */
object TaskPlan {
    const val MAX_TASKS = 6

    fun parse(reply: String): List<String>? {
        val start = reply.indexOf('{')
        val end = reply.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val arr = try {
            JSONObject(reply.substring(start, end + 1)).optJSONArray("tasks") ?: return null
        } catch (e: org.json.JSONException) {
            return null
        }
        val tasks = (0 until arr.length()).map { arr.optString(it).trim() }.filter { it.isNotEmpty() }
        return if (tasks.isEmpty()) null else tasks.take(MAX_TASKS)
    }

    val systemPrompt: String = """
Split the user's phone request into ordered steps. Each step must be one self-contained task for a phone agent,
with the names and text the user gave. If the request is a single task, return it as one step.
Do not add steps the user did not ask for. Keep each step in the user's language.
Reply with ONLY this JSON and nothing else: {"tasks":["step 1","step 2"]}
""".trimIndent()
}
