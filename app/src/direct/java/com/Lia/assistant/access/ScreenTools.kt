package com.Lia.assistant.access

import android.content.Context
import com.Lia.assistant.agent.core.DriverResult
import org.json.JSONObject

/** Tools the model can call on the phone. Returns null for names it does not own. */
object ScreenTools {
    private val screenNames = setOf(
        "read_screen", "tap_text", "type_text", "press_enter", "scroll_screen", "go_back", "go_home", "run_screen_task",
        "search_in_app", "tap_sequence",
    )

    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject? {
        DeviceTools.execute(ctx, name, args)?.let { return it }
        ExtraTools.execute(ctx, name, args)?.let { return it }
        PhoneControl.execute(ctx, name, args)?.let { return it }
        if (name !in screenNames) return null
        if (!NovaAccessibilityService.isEnabled(ctx)) return result("accessibility_not_enabled")
        if (NovaAccessibilityService.instance == null) return result("screen_service_disconnected")
        if (name == "tap_sequence") {
            return AppSearch.tapSequence(AccessibilityScreenDriver(), args.optString("labels"))
        }
        if (name == "search_in_app") {
            return AppSearch.searchInApp(AccessibilityScreenDriver(), args.optString("query"))
        }
        if (name == "run_screen_task") {
            val outcome = ScreenTasks.run(ctx, args.optString("goal"), args.optString("confirmed") == "yes")
            val status = when (outcome) {
                is ScreenAgent.Outcome.Done -> "done"
                is ScreenAgent.Outcome.NeedsConfirmation -> "needs_confirmation"
                is ScreenAgent.Outcome.Stopped -> "stopped"
            }
            return result(status).put("message", outcome.message)
        }
        val driver = AccessibilityScreenDriver()
        return when (name) {
            "read_screen" -> {
                val obs = driver.capture() ?: return result("failed")
                JSONObject()
                    .put("result", "ok")
                    .put("app", obs.packageName)
                    .put("stats", NovaAccessibilityService.instance?.lastStats.orEmpty())
                    .put("screen", ScreenFormat.lines(obs).joinToString("\n"))
            }
            "tap_text" -> {
                val (obs, target) = AppSearch.findWithScroll(driver, args.optString("text")) ?: return result("element_not_found")
                if (ActionSafety.needsConfirmation(target.label) && args.optString("confirmed") != "yes") {
                    return JSONObject()
                        .put("result", "needs_confirmation")
                        .put("message", "Ask the user first: tap \"${target.label}\"? Only call again with confirmed=yes after they say yes.")
                }
                when (driver.clickElement(obs.observationId, target.id)) {
                    DriverResult.OK -> {
                        val region = NovaAccessibilityService.instance?.regionOf(target.id).orEmpty()
                        com.Lia.assistant.action.AppMemory.recordTap(ctx, obs.packageName, target.label, region)
                        result("tapped")
                    }
                    DriverResult.NOT_FOUND, DriverResult.STALE -> result("element_not_found")
                    else -> result("failed")
                }
            }
            "type_text" -> {
                val obs = driver.capture() ?: return result("failed")
                val field = ScreenFinder.firstEditable(obs) ?: return result("no_focused_field")
                when (driver.setText(obs.observationId, field.id, args.optString("text"))) {
                    DriverResult.OK -> result("typed")
                    else -> result("no_focused_field")
                }
            }
            "scroll_screen" -> {
                val down = args.optString("direction", "down") != "up"
                if (driver.scroll(down) == DriverResult.OK) result("scrolled") else result("nothing_scrollable")
            }
            "press_enter" -> when (NovaAccessibilityService.instance?.pressEnter() ?: DriverResult.UNSUPPORTED) {
                DriverResult.OK -> result("done")
                DriverResult.UNSUPPORTED -> result("needs_android_11")
                else -> result("no_focused_field")
            }
            "go_back" -> if (driver.back() == DriverResult.OK) result("done") else result("failed")
            "go_home" -> {
                val r = NovaAccessibilityService.instance?.home() ?: DriverResult.UNSUPPORTED
                if (r == DriverResult.OK) result("done") else result("failed")
            }
            else -> null
        }
    }

    private fun result(value: String): JSONObject = JSONObject().put("result", value)
}
