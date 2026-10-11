package com.Lia.assistant.forge

import org.json.JSONObject

/** A failure with a message that is safe to show or speak. */
class ForgeError(message: String, val retryable: Boolean = false) : Exception(message)

/** Turns HTTP and stream errors into short, friendly messages. Never includes keys or page text. */
object ForgeErrors {
    const val BUSY_MESSAGE = "Gemini is busy right now. Give it a minute and try again."

    fun fromHttp(code: Int, body: String?): ForgeError {
        val status = runCatching {
            JSONObject(body.orEmpty()).optJSONObject("error")?.optString("status")
        }.getOrNull()
        return when {
            code == 503 || status == "UNAVAILABLE" -> ForgeError(BUSY_MESSAGE, retryable = true)
            code == 429 -> ForgeError("Gemini's limit was reached for now. Try again shortly.")
            code == 400 || code == 401 || code == 403 ->
                ForgeError("Gemini rejected the request. Check your API key in Settings.")
            else -> ForgeError("Gemini returned an error ($code). Try again.")
        }
    }

    fun fromStream(message: String): ForgeError =
        if (message.contains("overloaded", ignoreCase = true) || message.contains("high demand", ignoreCase = true)) {
            ForgeError(BUSY_MESSAGE, retryable = true)
        } else {
            ForgeError("Gemini stopped with an error. Try again.")
        }
}
