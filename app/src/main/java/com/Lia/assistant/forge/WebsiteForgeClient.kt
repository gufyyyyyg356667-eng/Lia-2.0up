package com.Lia.assistant.forge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Streams a page from Gemini over server-sent events. Cancelling the coroutine cancels the HTTP call. */
object WebsiteForgeClient {

    class Ended(val finishReason: String?)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    suspend fun stream(
        apiKey: String,
        model: String,
        system: String,
        user: String,
        onText: (String) -> Unit,
    ): Ended = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", user)))
                ),
            )
        // 2.5-class models think first, which makes the first token take a minute. Turn that off.
        if (model.contains("2.5")) {
            body.put("generationConfig", JSONObject().put("thinkingConfig", JSONObject().put("thinkingBudget", 0)))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = http.newCall(request)
        coroutineContext.job.invokeOnCompletion { call.cancel() }

        call.execute().use { response ->
            if (!response.isSuccessful) throw ForgeErrors.fromHttp(response.code, response.body?.string())
            val source = response.body?.source() ?: throw ForgeError("Gemini sent an empty reply.")
            var finish: String? = null
            while (true) {
                val line = source.readUtf8Line() ?: break
                val payload = SseParser.dataPayload(line) ?: continue
                if (payload == "[DONE]") break
                SseParser.extractError(payload)?.let { throw ForgeErrors.fromStream(it) }
                val text = SseParser.extractText(payload)
                if (text.isNotEmpty()) onText(text)
                SseParser.extractFinishReason(payload)?.let { finish = it }
            }
            Ended(finish)
        }
    }
}
