package com.Lia.assistant.alerts

import android.content.Context
import org.json.JSONObject

/** Named routines: a name and the full request it stands for. Stored privately on the phone. */
object Routines {
    private const val KEY = "routines_json"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("lia_routines", Context.MODE_PRIVATE)

    @Synchronized
    fun save(ctx: Context, name: String, steps: String) {
        val json = JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}")
        json.put(name.trim().lowercase(), steps.trim())
        prefs(ctx).edit().putString(KEY, json.toString()).apply()
    }

    @Synchronized
    fun get(ctx: Context, name: String): String? {
        val json = JSONObject(prefs(ctx).getString(KEY, "{}") ?: "{}")
        val key = name.trim().lowercase()
        return if (json.has(key)) json.getString(key) else null
    }
}
