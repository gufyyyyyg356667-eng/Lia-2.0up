package com.Lia.assistant.action

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-app memory: which labels worked where, and notes the user taught.
 * Stored privately on the phone. Used to tell the agent what is known about the open app.
 */
object AppMemory {
    private const val KEY = "memory_json"
    private const val MAX_TAPS = 40
    private const val MAX_NOTES = 30

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("lia_app_memory", Context.MODE_PRIVATE)

    private fun load(ctx: Context): JSONObject =
        try {
            JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}")
        } catch (e: org.json.JSONException) {
            JSONObject()
        }

    private fun save(ctx: Context, root: JSONObject) {
        prefs(ctx).edit().putString(KEY, root.toString()).apply()
    }

    private fun appEntry(root: JSONObject, pkg: String): JSONObject {
        root.optJSONObject(pkg)?.let { return it }
        val fresh = JSONObject()
        root.put(pkg, fresh)
        return fresh
    }

    /** Records that tapping [label] at [region] worked. Each repeat raises its count. */
    @Synchronized
    fun recordTap(ctx: Context, pkg: String, label: String, region: String) {
        val key = label.trim().lowercase()
        if (pkg.isBlank() || key.isEmpty()) return
        val root = load(ctx)
        val app = appEntry(root, pkg)
        val taps = app.optJSONArray("taps") ?: JSONArray().also { app.put("taps", it) }
        var found = false
        for (i in 0 until taps.length()) {
            val t = taps.getJSONObject(i)
            if (t.getString("label") == key && t.getString("region") == region) {
                t.put("hits", t.getInt("hits") + 1)
                found = true
                break
            }
        }
        if (!found && taps.length() < MAX_TAPS) {
            taps.put(JSONObject().put("label", key).put("region", region).put("hits", 1))
        }
        save(ctx, root)
    }

    /** Stores a fact the user taught, for example "DM button is at the top right". */
    @Synchronized
    fun teach(ctx: Context, pkg: String, note: String) {
        val text = note.trim()
        if (pkg.isBlank() || text.isEmpty()) return
        val root = load(ctx)
        val app = appEntry(root, pkg)
        val notes = app.optJSONArray("notes") ?: JSONArray().also { app.put("notes", it) }
        if (notes.length() < MAX_NOTES) notes.put(text)
        save(ctx, root)
    }

    /** Lines describing this app for the agent. Empty if nothing is known yet. */
    @Synchronized
    fun linesFor(ctx: Context, pkg: String): List<String> {
        val app = load(ctx).optJSONObject(pkg) ?: return emptyList()
        val taps = mutableListOf<Triple<String, String, Int>>()
        app.optJSONArray("taps")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                taps += Triple(t.getString("label"), t.getString("region"), t.getInt("hits"))
            }
        }
        val notes = mutableListOf<String>()
        app.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) notes += arr.getString(i)
        }
        return describe(taps, notes)
    }

    /** Pure formatting, so it can be tested: strongest known taps first, then the user's notes. */
    fun describe(taps: List<Triple<String, String, Int>>, notes: List<String>): List<String> {
        val lines = taps.sortedByDescending { it.third }.take(12).map { (label, region, hits) ->
            val where = if (region.isBlank()) "" else " at $region"
            "\"$label\"$where (worked $hits times)"
        }
        return lines + notes.map { "Note from the user: $it" }
    }
}
