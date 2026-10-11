package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.core.content.ContextCompat

object ContactResolver {

    sealed class Lookup {
        data class Found(val name: String, val number: String) : Lookup()
        object NoPermission : Lookup()
        object NotFound : Lookup()
    }

    fun lookup(ctx: Context, query: String): Lookup {
        val q = query.trim()
        if (q.isEmpty()) return Lookup.NotFound
        if (looksLikePhone(q)) return Lookup.Found(q, q.filter { it.isDigit() || it == '+' })

        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return Lookup.NoPermission

        val candidates = mutableListOf<Pair<String, String>>()
        ctx.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER),
            "${Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$q%"),
            null
        )?.use { cursor ->
            while (cursor.moveToNext() && candidates.size < 5) {
                candidates += cursor.getString(0).orEmpty() to cursor.getString(1).orEmpty()
            }
        }
        val best = pick(q, candidates) ?: return Lookup.NotFound
        return Lookup.Found(best.first, best.second)
    }

    fun looksLikePhone(q: String): Boolean =
        q.all { it.isDigit() || it in " +-()." } && q.count { it.isDigit() } >= 5

    /** Ranks exact match > prefix > contains. Returns null when nothing matches. */
    fun pick(query: String, candidates: List<Pair<String, String>>): Pair<String, String>? {
        val q = query.trim().lowercase()
        fun rank(name: String): Int {
            val n = name.lowercase()
            return when {
                n == q -> 0
                n.startsWith(q) -> 1
                n.contains(q) -> 2
                else -> 3
            }
        }
        return candidates.filter { rank(it.first) < 3 }.minByOrNull { rank(it.first) }
    }
}
