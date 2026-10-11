package com.Lia.assistant.action

import com.Lia.assistant.access.ScreenToolCatalog
import org.json.JSONArray
import org.json.JSONObject

/** Tools Gemini is allowed to call. Only tools that exist in this build are listed. */
object LiaToolCatalog {

    fun declarations(): JSONArray {
        val all = JSONArray()
        val screen = ScreenToolCatalog.declarations()
        for (source in listOf(base(), screen, InfoTools.declarations())) {
            for (i in 0 until source.length()) all.put(source.get(i))
        }
        return all
    }

    private fun base(): JSONArray = JSONArray()
        .put(
            functionDecl(
                "search_youtube",
                "Search YouTube for a video or song and open the results. Use this for any YouTube search.",
                mapOf("query" to "What to search for"),
                listOf("query"),
            )
        )
        .put(
            functionDecl(
                "web_search",
                "Search the web in the browser and open the results.",
                mapOf("query" to "What to search for"),
                listOf("query"),
            )
        )
        .put(
            functionDecl(
                "build_website",
                "Build a new animated website from a description. Call only when the user asks to build, make or create a website.",
                mapOf("prompt" to "What the website is about, in the user's words"),
                listOf("prompt"),
            )
        )
        .put(
            functionDecl(
                "open_app",
                "Open an installed app by its name.",
                mapOf("app_name" to "Name of the app, for example YouTube"),
                listOf("app_name"),
            )
        )
        .put(
            functionDecl(
                "call_contact",
                "Call a contact by name or phone number.",
                mapOf("contact_name" to "Contact name or phone number"),
                listOf("contact_name"),
            )
        )
        .put(
            functionDecl(
                "message_contact",
                "Open a WhatsApp or SMS message to a contact. The user taps Send themselves.",
                mapOf("contact_name" to "Contact name or phone number", "message" to "Message text"),
                listOf("contact_name", "message"),
            )
        )

    fun instruction(assistantName: String): String =
        "You can open apps, call contacts, and open message drafts for the user. " +
            "You can read the screen and control the phone for the user when they ask. " +
            "A message is only opened for the user; they tap Send themselves. " +
            "Use these tools only when the user clearly asks for the action. $assistantName never sends messages silently. " + ScreenToolCatalog.instruction() +
            " Big requests with several steps (for example: message someone on WhatsApp, then play a song, then message someone on Messenger): " +
            "call run_screen_task at once with the full request, in one call. Do not describe the plan, do not ask permission to start, " +
            "and do not split it into questions. Say briefly that you are doing it, then report the result when the tool finishes. " +
            "Ask only when a real detail is missing, such as which contact or which song. " +
            "Language: reply in the same language the user is speaking. If the user speaks Bangla (Bengali script or Bangla words), " +
            "reply in Bangla in Bengali script from the first sentence, not in English first. If the user speaks English, reply in English. " +
            "Keep short numbers and app names as they are. " + com.Lia.assistant.voice.MoodState.mood.instruction 

    internal fun functionDecl(
        name: String,
        description: String,
        params: Map<String, String>,
        required: List<String>,
    ): JSONObject {
        val properties = JSONObject()
        params.forEach { (key, desc) ->
            properties.put(key, JSONObject().put("type", "STRING").put("description", desc))
        }
        return JSONObject()
            .put("name", name)
            .put("description", description)
            .put(
                "parameters",
                JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", properties)
                    .put("required", JSONArray(required))
            )
    }
}
