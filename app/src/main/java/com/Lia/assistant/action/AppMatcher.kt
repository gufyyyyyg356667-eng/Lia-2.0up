package com.Lia.assistant.action

import android.content.Context
import android.content.Intent

/** Finds an installed app by spoken name. Pure logic, so it is unit-tested without a device. */
object AppMatcher {

    /** [labels] maps lowercase app label to package name. */
    fun match(query: String, labels: Map<String, String>): String? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        labels[q]?.let { return it }

        val queryWords = q.split(' ').filter { it.isNotEmpty() }
        val candidates = labels.keys.filter { key ->
            if (key.length < 3) return@filter false
            val keyWords = key.split(' ').filter { it.isNotEmpty() }
            key.startsWith(q) ||
                q.startsWith(key) ||
                (queryWords.isNotEmpty() && queryWords.all { w -> keyWords.any { it.startsWith(w) } })
        }
        val best = candidates.minByOrNull { kotlin.math.abs(it.length - q.length) } ?: return null
        return labels[best]
    }
}

/** Process-wide cache of launchable apps, refreshed every 15 minutes. */
object AppIndex {
    private const val TTL_MS = 15 * 60_000L
    @Volatile private var cache: Map<String, String> = emptyMap()
    @Volatile private var builtAt = 0L

    @Synchronized
    fun get(ctx: Context): Map<String, String> {
        if (cache.isNotEmpty() && System.currentTimeMillis() - builtAt < TTL_MS) return cache
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val map = HashMap<String, String>()
        pm.queryIntentActivities(intent, 0).forEach { info ->
            val label = info.loadLabel(pm).toString().trim().lowercase()
            if (label.isNotEmpty() && !map.containsKey(label)) {
                map[label] = info.activityInfo.packageName
            }
        }
        cache = map
        builtAt = System.currentTimeMillis()
        return map
    }
}
