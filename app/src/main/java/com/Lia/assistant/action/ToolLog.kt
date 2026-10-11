package com.Lia.assistant.action

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/** Every command Lia runs: when, what, the result, how long, and whether it failed. Shown on Lia Monitor. */
object ToolLog {
    data class Entry(
        val id: Long,
        val at: Long,
        val name: String,
        val args: String,
        val result: String,
        val message: String,
        val ms: Long,
        val running: Boolean,
        val failed: Boolean,
    )

    private const val MAX = 200
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries
    private var nextId = 1L

    @Synchronized
    fun begin(name: String, args: JSONObject): Long {
        val id = nextId++
        val started = Entry(id, System.currentTimeMillis(), name, args.toString().take(160), "", "", 0, running = true, failed = false)
        _entries.value = (listOf(started) + _entries.value).take(MAX)
        return id
    }

    @Synchronized
    fun finish(id: Long, result: JSONObject, ms: Long) {
        val code = result.optString("result")
        val message = result.optString("message").ifBlank { result.optString("error") }
        val failed = isFailure(code)
        _entries.value = _entries.value.map {
            if (it.id == id) it.copy(result = code, message = message, ms = ms, running = false, failed = failed) else it
        }
    }

    /** True for results that mean the step did not work. Asking for a yes is not a failure. */
    fun isFailure(result: String): Boolean {
        val r = result.lowercase()
        if (r.isEmpty() || r == "needs_confirmation") return false
        val markers = listOf("fail", "not_", "no_", "bad_", "error", "unsupported", "missing", "denied",
            "disconnected", "unknown", "nothing_to", "stopped", "license", "cannot", "_off", "off")
        return markers.any { r.contains(it) }
    }

    /** Tools that failed, most failures first, with the last message for each. */
    fun failuresByTool(list: List<Entry>): List<Triple<String, Int, String>> =
        list.filter { it.failed }
            .groupBy { it.name }
            .map { (name, items) -> Triple(name, items.size, items.first().message.ifBlank { items.first().result }) }
            .sortedByDescending { it.second }
}
