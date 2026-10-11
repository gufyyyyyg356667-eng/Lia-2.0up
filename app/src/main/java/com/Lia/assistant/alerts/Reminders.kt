package com.Lia.assistant.alerts

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Reminder(val id: Long, val atMillis: Long, val text: String, val daily: Boolean)

/** Reminders the phone says aloud at a time. Stored privately on the phone. */
object Reminders {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val KEY = "reminders_json"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("lia_reminders", Context.MODE_PRIVATE)

    @Synchronized
    fun all(ctx: Context): List<Reminder> {
        val raw = prefs(ctx).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Reminder(o.getLong("id"), o.getLong("at"), o.getString("text"), o.getBoolean("daily"))
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    @Synchronized
    fun add(ctx: Context, atMillis: Long, text: String, daily: Boolean): Reminder {
        val reminder = Reminder(System.currentTimeMillis(), atMillis, text, daily)
        save(ctx, all(ctx) + reminder)
        return reminder
    }

    /** Due reminders. Daily ones move to the next day; the rest are removed. */
    @Synchronized
    fun takeDue(ctx: Context, now: Long): List<Reminder> {
        val list = all(ctx)
        val due = list.filter { it.atMillis <= now }
        if (due.isEmpty()) return emptyList()
        val kept = list.mapNotNull { r ->
            when {
                r.atMillis > now -> r
                r.daily -> r.copy(atMillis = r.atMillis + DAY_MS)
                else -> null
            }
        }
        save(ctx, kept)
        return due
    }

    private fun save(ctx: Context, list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().put("id", r.id).put("at", r.atMillis).put("text", r.text).put("daily", r.daily))
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }
}
