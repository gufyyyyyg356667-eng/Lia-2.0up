package com.Lia.assistant.text

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Typed chat and the screen agent use this. If a model answers 404 (not available to this key),
 * the next model is tried, and the one that works is remembered for later calls.
 */
object GeminiTextClient {

    sealed class Reply {
        data class Success(val text: String) : Reply()
        data class Error(val message: String) : Reply()
    }

    data class ChatTurn(val isUser: Boolean, val text: String)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile private var ordered: List<String>? = null

    suspend fun reply(apiKey: String, systemInstruction: String, history: List<ChatTurn>): Reply =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext Reply.Error("Add your Gemini API key in Settings first.")
            if (history.isEmpty()) return@withContext Reply.Error("Say something first.")
            try {
                val models = modelsFor(apiKey)
                if (models.isEmpty()) {
                    return@withContext Reply.Error("Gemini did not list any text model for this key. Check the key in Google AI Studio.")
                }
                for (model in models) {
                    val body = buildBody(systemInstruction, history, model).toString()
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                        .post(body.toRequestBody("application/json".toMediaType()))
                        .build()
                    execute(request).use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (response.isSuccessful) {
                            // Remember the model that worked, so it is tried first next time.
                            ordered = listOf(model) + models.filterNot { it == model }
                            val text = extractText(raw)
                            return@withContext if (text.isBlank()) Reply.Error("Gemini sent an empty reply.")
                            else Reply.Success(text)
                        }
                        if (response.code != 404) {
                            return@withContext Reply.Error(if (response.code == 429) "Gemini limit reached. Wait a minute and try again." else "Gemini returned an error (${response.code}).")
                        }
                    }
                }
                Reply.Error("Gemini has no model this key can use (404). Check the key's access in Google AI Studio.")
            } catch (e: IOException) {
                Reply.Error("Couldn't reach Gemini. Check your internet connection.")
            }
        }

    /** Sends a request; on a rate limit (429) it waits and tries again, up to two more times. */
    private fun execute(request: Request): okhttp3.Response {
        var response = http.newCall(request).execute()
        for (waitMs in listOf(10_000L, 20_000L)) {
            if (response.code != 429) break
            response.close()
            Thread.sleep(waitMs)
            response = http.newCall(request).execute()
        }
        return response
    }

    /** The best model for this key. Used by the Forge. */
    internal fun resolveModel(apiKey: String): String? = modelsFor(apiKey).firstOrNull()

    private fun modelsFor(apiKey: String): List<String> {
        ordered?.let { return it }
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val models = JSONObject(response.body?.string().orEmpty()).optJSONArray("models") ?: return emptyList()
            val names = (0 until models.length())
                .map { models.getJSONObject(it) }
                .filter { supportsGenerate(it) }
                .map { it.optString("name") }
            val list = GeminiModelPicker.candidates(names)
            ordered = list
            return list
        }
    }

    private fun buildBody(systemInstruction: String, history: List<ChatTurn>, model: String): JSONObject {
        val contents = JSONArray()
        history.forEach { turn ->
            contents.put(
                JSONObject()
                    .put("role", if (turn.isUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
            )
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            .put("contents", contents)
        // Gemini 2.5 models think before answering, which adds seconds. Short step decisions do not need it.
        if (model.contains("2.5")) {
            body.put("generationConfig", JSONObject().put("thinkingConfig", JSONObject().put("thinkingBudget", 0)))
        }
        return body
    }

    private fun extractText(raw: String): String = try {
        JSONObject(raw).optJSONArray("candidates")
            ?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)
            ?.optString("text").orEmpty()
    } catch (e: org.json.JSONException) {
        ""
    }

    private fun supportsGenerate(model: JSONObject): Boolean {
        val methods = model.optJSONArray("supportedGenerationMethods") ?: return false
        return (0 until methods.length()).any { methods.getString(it) == "generateContent" }
    }
}
