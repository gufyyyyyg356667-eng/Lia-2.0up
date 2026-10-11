package com.Lia.assistant.forge

import org.json.JSONException
import org.json.JSONObject

/** Reads one Gemini server-sent-events line at a time. Pure, so it is unit-tested without a network. */
object SseParser {
    /** The part after "data:" or null for any other SSE line. */
    fun dataPayload(line: String): String? =
        if (line.startsWith("data:")) line.substring(5).trim() else null

    /** All text parts of the first candidate, concatenated. */
    fun extractText(json: String): String {
        val parts = parse(json)
            ?.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString()
    }

    /** The error message from the server, or null. */
    fun extractError(json: String): String? {
        val err = parse(json)?.optJSONObject("error") ?: return null
        return err.optString("message").ifEmpty { err.optString("status").ifEmpty { "error" } }
    }

    /** finishReason of the first candidate, or null while the answer is still going. */
    fun extractFinishReason(json: String): String? =
        parse(json)?.optJSONArray("candidates")?.optJSONObject(0)
            ?.optString("finishReason")?.takeIf { it.isNotEmpty() }

    private fun parse(json: String): JSONObject? = try {
        JSONObject(json)
    } catch (e: JSONException) {
        null
    }
}
