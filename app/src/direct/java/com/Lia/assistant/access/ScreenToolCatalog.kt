package com.Lia.assistant.access

import com.Lia.assistant.action.LiaToolCatalog
import org.json.JSONArray

/** Phone-control declarations, added to the model's tools in the direct build only. */
object ScreenToolCatalog {
    private fun decl(name: String, desc: String, params: Map<String, String> = emptyMap(), required: List<String> = emptyList()) =
        LiaToolCatalog.functionDecl(name, desc, params, required)

    fun declarations(): JSONArray = JSONArray()
        .put(decl("run_screen_task",
            "Do a multi-step task on the phone by looking at the screen (e.g. find something in an app, fill a form). " +
                "Give the goal in one sentence. If it asks to confirm a tap, ask the user and call again with confirmed=yes only after they say yes.",
            mapOf("goal" to "The task in one sentence", "confirmed" to "yes only after the user said yes"), listOf("goal")))
        .put(decl("read_screen", "Read the visible labels on the phone screen."))
        .put(decl("tap_text", "Tap a button or link on screen by its label. Buttons like Send, Pay, Delete, Install or Post need confirmed=yes.",
            mapOf("text" to "Label to tap", "confirmed" to "yes only after the user said yes"), listOf("text")))
        .put(decl("type_text", "Type text into the first non-secret text field. Never for passwords or codes.", mapOf("text" to "Text to type"), listOf("text")))
        .put(decl("press_enter", "Press Enter or Search in the focused text field, e.g. to run a search."))
        .put(decl("scroll_screen", "Scroll the screen.", mapOf("direction" to "up or down"), listOf("direction")))
        .put(decl("go_back", "Press the back button."))
        .put(decl("go_home", "Go to the home screen."))
        .put(decl("set_flashlight", "Turn the torch on or off.", mapOf("state" to "on or off"), listOf("state")))
        .put(decl("change_volume", "Change media volume: up, down, or a level 0-100.",
            mapOf("direction" to "up or down", "level" to "0 to 100"), emptyList()))
        .put(decl("set_alarm", "Set an alarm for a time (24-hour clock).",
            mapOf("hour" to "0-23", "minute" to "0-59", "message" to "Label"), listOf("hour", "minute")))
        .put(decl("set_timer", "Start a countdown timer.", mapOf("minutes" to "1-720"), listOf("minutes")))
        .put(decl("open_settings", "Open one settings page: wifi, bluetooth, display, sound, battery, airplane, location, apps, hotspot.",
            mapOf("panel" to "Panel name"), listOf("panel")))
        .put(decl("open_url", "Open a web link in the browser.", mapOf("url" to "http or https link"), listOf("url")))
        .put(decl("take_screenshot", "Take a screenshot."))
        .put(decl("media_control", "Play or pause music and video, or skip to the next or previous track.", mapOf("action" to "play_pause, next or previous"), listOf("action")))
        .put(decl("set_ringer", "Set the ringer to normal, vibrate or silent.", mapOf("mode" to "normal, vibrate or silent"), listOf("mode")))
        .put(decl("set_brightness", "Set screen brightness as a percentage (needs one-time permission).", mapOf("level" to "0-100"), listOf("level")))
        .put(decl("open_camera", "Open the camera app."))
        .put(decl("navigate", "Start navigation to a place in Google Maps.", mapOf("destination" to "Place name or address"), listOf("destination")))
        .put(decl("open_recent", "Show recent apps."))
        .put(decl("share_text", "Open the share menu with some text.", mapOf("text" to "Text to share"), listOf("text")))
        .put(decl("dial_number", "Put a phone number in the dialer. The user presses Call.", mapOf("number" to "Phone number"), listOf("number")))
        .put(decl("prepare_sms", "Open a text message to a number with the message filled in. The user presses Send.",
            mapOf("number" to "Phone number", "message" to "Message text"), listOf("number", "message")))
        .put(decl("open_whatsapp_number", "Open a WhatsApp chat with a phone number and the message filled in. The user presses Send.",
            mapOf("number" to "Phone number with country code", "message" to "Message text"), listOf("number", "message")))
        .put(decl("copy_text", "Copy text to the clipboard.", mapOf("text" to "Text to copy"), listOf("text")))
        .put(decl("set_auto_rotate", "Turn screen auto-rotate on or off.", mapOf("state" to "on or off"), listOf("state")))
        .put(decl("add_calendar_event", "Open the calendar with a new event filled in. The user saves it.",
            mapOf("title" to "Event title", "start_time" to "Start time as epoch milliseconds", "location" to "Place"), listOf("title")))
        .put(decl("add_contact", "Open the contact form with a name and number filled in. The user saves it.",
            mapOf("name" to "Name", "number" to "Phone number"), listOf("name")))
        .put(decl("compose_email", "Open an email with the fields filled in. The user presses Send.",
            mapOf("to" to "Email address", "subject" to "Subject", "body" to "Body text"), listOf("to")))
        .put(decl("open_gallery", "Open the photo gallery."))
        .put(decl("open_app_info", "Open the settings page of an app.", mapOf("package" to "Package name, e.g. com.whatsapp"), listOf("package")))
        .put(decl("open_notification_settings", "Open the notification settings of an app.", mapOf("package" to "Package name"), listOf("package")))
        .put(decl("open_wallpaper", "Open the wallpaper picker."))
        .put(decl("open_alarms", "Open the clock app's alarm list."))
        .put(decl("open_contacts", "Open the contacts app."))
        .put(decl("search_in_app", "Search inside the app that is open right now (Instagram, Telegram, YouTube...). Finds the search control, types the query and presses Enter.",
            mapOf("query" to "What to search for"), listOf("query")))
        .put(decl("lock_screen", "Lock the phone screen."))
        .put(decl("open_notifications", "Open the notification shade."))
        .put(decl("quick_settings", "Open quick settings."))
        .put(decl("swipe", "Swipe the screen in the middle. direction: up scrolls a list down.", mapOf("direction" to "up, down, left or right"), listOf("direction")))
        .put(decl("tap_xy", "Tap a position on screen, in percent of width and height (0-100).", mapOf("x" to "0-100", "y" to "0-100"), listOf("x", "y")))
        .put(decl("long_press", "Long press an element by its label.", mapOf("text" to "Label"), listOf("text")))
        .put(decl("double_tap", "Double tap an element by its label.", mapOf("text" to "Label"), listOf("text")))
        .put(decl("drag", "Drag from one position to another, in percent (0-100).", mapOf("from_x" to "0-100", "from_y" to "0-100", "to_x" to "0-100", "to_y" to "0-100"), listOf("from_x", "from_y", "to_x", "to_y")))
        .put(decl("clear_text", "Empty the focused text field."))
        .put(decl("paste_text", "Paste the clipboard into the focused text field."))
        .put(decl("wait", "Wait a few seconds for the screen to load.", mapOf("seconds" to "1-10"), listOf("seconds")))
        .put(decl("scroll_to_text", "Scroll down until an element with this label is visible, then tap nothing.", mapOf("text" to "Label"), listOf("text")))
        .put(decl("toggle_setting", "Turn a quick setting on or off: Wi-Fi, Bluetooth, mobile data, airplane mode, do not disturb.", mapOf("name" to "Setting name", "state" to "on or off"), listOf("name", "state")))
        .put(decl("read_notifications", "Read the notifications on screen."))
        .put(decl("open_all_apps", "Open the all-apps drawer."))
        .put(decl("split_screen", "Turn split screen on or off."))
        .put(decl("power_menu", "Open the power menu."))
        .put(decl("list_apps", "List installed apps, optionally filtered by a name.", mapOf("filter" to "Part of an app name"), emptyList()))
        .put(decl("stop_task", "Stop the screen task that is running now."))
        .put(decl("device_status", "Report battery, storage, network and sound status."))
        .put(decl("screen_time", "Report today's screen time, if usage access is granted."))
        .put(decl("set_stream_volume", "Set the volume of one sound stream.", mapOf("stream" to "media, ring, alarm or notification", "level" to "0-100"), listOf("stream", "level")))
        .put(decl("wake_screen", "Turn the screen on."))
        .put(decl("copy_screen_text", "Copy the text of one element to the clipboard.", mapOf("text" to "Label"), listOf("text")))
        .put(decl("read_full_text", "Read the full text of one element, or of the whole screen if no label is given.", mapOf("text" to "Label, optional"), emptyList()))
        .put(decl("scroll_to_edge", "Scroll to the top or bottom edge of the list.", mapOf("edge" to "top or bottom"), listOf("edge")))
        .put(decl("call_action", "Answer, decline, end, speaker, mute or hold a phone call.", mapOf("action" to "answer, decline, end, speaker, mute or hold"), listOf("action")))
        .put(decl("take_photo", "Take a photo with the camera app.", mapOf("camera" to "front or back"), emptyList()))
        .put(decl("tap_sequence", "Tap several buttons in order in ONE step: calculators, keypads, number pads. labels is comma separated, e.g. 8,0,0,+,7,0,0,= for 800+700.", mapOf("labels" to "Button labels in order, comma separated"), listOf("labels")))

    fun instruction(): String =
        "For YouTube searches always use search_youtube. For web searches use web_search. For ANY phone task (opening, searching, tapping, typing, messaging, changing settings, multi-step work) use run_screen_task with the full request as goal. Use simple tools only for a single immediate action. " +
            "Use screen tools only for things those tools cannot do. After every screen action, call read_screen again to check the result before the next step. If a button cannot be found, say so instead of guessing. You can control this phone for the user: read the screen, tap, type, scroll, use the torch, volume, alarms, timers, " +
            "settings pages, links, screenshots and lock. Only do what the user asks. " +
            "Before tapping Send, Pay, Delete, Install, Post, Share or Confirm, ask the user and wait for a clear yes. " +
            "Never describe the screen or say which app is open from memory: call read_screen again each time the user asks. " +
            "Never read, type or guess passwords, PINs or one-time codes, and never try to get past the lock screen or security prompts."
}
