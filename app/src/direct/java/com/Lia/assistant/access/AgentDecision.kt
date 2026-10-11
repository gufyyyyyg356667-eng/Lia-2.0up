package com.Lia.assistant.access

import org.json.JSONObject

/** One decision from the model. Pure parsing, so it is unit-tested without a device. */
sealed class AgentDecision {
    data class Act(val name: String, val args: Map<String, String>) : AgentDecision()
    data class Done(val summary: String) : AgentDecision()
    data class Ask(val question: String) : AgentDecision()

    companion object {
        /** Every action name the agent may choose. Anything else is rejected. */
        val allowedActions = setOf(
            "tap_text", "type_text", "press_enter", "scroll_screen", "go_back", "go_home",
            "open_app", "search_youtube", "web_search", "open_url", "set_flashlight",
            "change_volume", "set_alarm", "set_timer", "open_settings", "take_screenshot",
            "media_control", "set_ringer", "set_brightness", "open_camera", "navigate", "open_recent",
            "share_text", "dial_number", "prepare_sms", "open_whatsapp_number", "copy_text", "set_auto_rotate",
            "add_calendar_event", "add_contact", "compose_email", "open_gallery", "open_app_info",
            "open_notification_settings", "open_wallpaper", "open_alarms", "open_contacts", "search_in_app",
            "tap_sequence",
        ) + PhoneControl.names + ExtraTools.names

        /** Reads the first JSON object in the reply, even if the model added extra text. */
        fun parse(reply: String): AgentDecision? {
            val start = reply.indexOf('{')
            val end = reply.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val obj = try {
                JSONObject(reply.substring(start, end + 1))
            } catch (e: org.json.JSONException) {
                return null
            }
            return when (val action = obj.optString("action")) {
                "done" -> Done(obj.optString("summary").ifBlank { "Done." })
                "ask" -> obj.optString("question").takeIf { it.isNotBlank() }?.let { Ask(it) }
                else -> {
                    if (action !in allowedActions) return null
                    val args = obj.optJSONObject("args") ?: JSONObject()
                    val map = mutableMapOf<String, String>()
                    args.keys().forEach { key -> map[key] = args.optString(key) }
                    Act(action, map)
                }
            }
        }
    }
}
