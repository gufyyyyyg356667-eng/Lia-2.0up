package com.Lia.assistant.voice

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

/** Full-duplex voice connection to the Gemini Live API. Never logs raw server messages or the key. */
class GeminiLiveClient(private val listener: Listener) {

    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(pcm: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onTurnComplete()
        fun onInterrupted()
        fun onError(message: String)
        fun onClosed()
    }

    companion object {
        // Keep the model name in this one place. Check the Live API docs for the current native-audio model.
        const val MODEL = "models/gemini-3.1-flash-live-preview"
        private const val WS_BASE =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    private val http = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val dedup = ToolCallDeduplicator()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var ready = false

    fun connect(apiKey: String, systemInstruction: String, voiceName: String, declarations: JSONArray) {
        disconnect()
        val request = Request.Builder().url("$WS_BASE?key=$apiKey").build()
        socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(setupMessage(systemInstruction, voiceName, declarations).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket === socket) handle(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (webSocket === socket) handle(bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (webSocket === socket) listener.onError("Connection problem")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket === socket) listener.onClosed()
            }
        })
    }

    fun sendAudio(pcm: ByteArray) {
        if (!ready) return
        val msg = JSONObject().put(
            "realtimeInput",
            JSONObject().put(
                "audio",
                JSONObject()
                    .put("mimeType", "audio/pcm;rate=16000")
                    .put("data", Base64.getEncoder().encodeToString(pcm))
            )
        )
        socket?.send(msg.toString())
    }

    fun sendToolResponse(id: String, name: String, result: JSONObject) {
        val msg = JSONObject().put(
            "toolResponse",
            JSONObject().put(
                "functionResponses",
                JSONArray().put(JSONObject().put("id", id).put("name", name).put("response", result))
            )
        )
        socket?.send(msg.toString())
    }

    /** Sends a text turn so the assistant can report a finished background task aloud. */
    fun sendText(text: String) {
        if (!ready) return
        val msg = JSONObject().put(
            "clientContent",
            JSONObject()
                .put("turns", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", text)))))
                .put("turnComplete", true)
        )
        socket?.send(msg.toString())
    }

    fun disconnect() {
        ready = false
        val s = socket
        socket = null
        s?.close(1000, "bye")
    }

    private fun setupMessage(systemInstruction: String, voiceName: String, declarations: JSONArray): JSONObject {
        val generation = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put(
                "speechConfig",
                JSONObject().put(
                    "voiceConfig",
                    JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName))
                )
            )
        val setup = JSONObject()
            .put("model", MODEL)
            .put("generationConfig", generation)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            .put("inputAudioTranscription", JSONObject())
            .put("outputAudioTranscription", JSONObject())
        if (declarations.length() > 0) {
            setup.put("tools", JSONArray().put(JSONObject().put("functionDeclarations", declarations)))
        }
        return JSONObject().put("setup", setup)
    }

    private fun handle(raw: String) {
        val msg = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            return
        }

        if (msg.has("setupComplete")) {
            ready = true
            listener.onSetupComplete()
            return
        }

        msg.optJSONObject("toolCall")?.optJSONArray("functionCalls")?.let { calls ->
            for (i in 0 until calls.length()) {
                val call = calls.getJSONObject(i)
                val id = call.optString("id")
                if (id.isNotEmpty() && !dedup.firstTime(id)) continue
                listener.onToolCall(id, call.optString("name"), call.optJSONObject("args") ?: JSONObject())
            }
        }

        msg.optJSONObject("serverContent")?.let { content ->
            content.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
                for (i in 0 until parts.length()) {
                    val inline = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
                    val data = inline.optString("data")
                    if (data.isNotEmpty() && inline.optString("mimeType").startsWith("audio/pcm")) {
                        listener.onAudioChunk(Base64.getDecoder().decode(data))
                    }
                }
            }
            content.optJSONObject("inputTranscription")?.optString("text")
                ?.takeIf { it.isNotEmpty() }?.let { listener.onInputTranscript(it) }
            content.optJSONObject("outputTranscription")?.optString("text")
                ?.takeIf { it.isNotEmpty() }?.let { listener.onOutputTranscript(it) }
            if (content.optBoolean("interrupted")) listener.onInterrupted()
            if (content.optBoolean("turnComplete")) listener.onTurnComplete()
        }
    }
}
