package com.Lia.assistant.access

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import com.Lia.assistant.action.AgentTrace
import com.Lia.assistant.action.AppIndex
import com.Lia.assistant.agent.core.DriverResult
import org.json.JSONObject

/**
 * Extra phone-control tools: gestures, tapping by position, long press, double tap, drag, clearing and pasting
 * text, quick-settings switches, notifications, app list, split screen, power menu.
 * Every tap-like tool goes through the same confirmation rules as tap_text.
 * Returns null for names it does not own.
 */
object PhoneControl {
    val names = setOf(
        "swipe", "tap_xy", "long_press", "double_tap", "drag", "clear_text", "paste_text", "wait",
        "scroll_to_text", "toggle_setting", "read_notifications", "open_all_apps", "split_screen",
        "power_menu", "list_apps",
    )

    private fun result(value: String): JSONObject = JSONObject().put("result", value)

    private fun needsConfirmation(label: String): JSONObject =
        JSONObject()
            .put("result", "needs_confirmation")
            .put("label", label)
            .put("message", "Ask the user first: tap \"$label\"? Only call again with confirmed=yes after they say yes.")

    /** The user said yes: either confirmed=yes, or the agent passes back the exact label the user approved. */
    fun isApproved(args: JSONObject, label: String): Boolean =
        args.optString("confirmed") == "yes" || args.optString("approved").equals(label, ignoreCase = true)

    private fun num(args: JSONObject, key: String): Double? = args.optString(key).trim().toDoubleOrNull()

    private fun status(r: DriverResult, ok: String): JSONObject = when (r) {
        DriverResult.OK -> result(ok)
        DriverResult.NOT_FOUND -> result("element_not_found")
        DriverResult.UNSUPPORTED -> result("bad_position")
        else -> result("failed")
    }

    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject? {
        if (name !in names) return null
        val svc = NovaAccessibilityService.instance ?: return result("accessibility_not_enabled")
        return when (name) {
            "swipe" -> status(svc.swipe(args.optString("direction")), "swiped")
            "tap_xy" -> tapXy(svc, args)
            "long_press" -> byTextOrPosition(svc, args, longPress = true)
            "double_tap" -> byTextOrPosition(svc, args, longPress = false)
            "drag" -> {
                val x1 = num(args, "from_x"); val y1 = num(args, "from_y")
                val x2 = num(args, "to_x"); val y2 = num(args, "to_y")
                if (x1 == null || y1 == null || x2 == null || y2 == null) return result("bad_position")
                status(svc.dragPercent(x1, y1, x2, y2), "dragged")
            }
            "clear_text" -> when (svc.clearFocusedText()) {
                DriverResult.OK -> result("cleared")
                else -> result("no_focused_field")
            }
            "paste_text" -> when (svc.pasteIntoFocused()) {
                DriverResult.OK -> result("pasted")
                else -> result("no_focused_field")
            }
            "wait" -> {
                val seconds = (num(args, "seconds") ?: 1.0).coerceIn(0.5, 10.0)
                Thread.sleep((seconds * 1000).toLong())
                result("waited")
            }
            "scroll_to_text" -> scrollToText(svc, args.optString("text"))
            "toggle_setting" -> QuickToggle.run(args.optString("name"), args.optString("state"))
            "read_notifications" -> readNotifications(svc)
            "open_all_apps" -> openAllApps(svc)
            "split_screen" -> status(svc.global(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN), "done")
            "power_menu" -> {
                if (!isApproved(args, "Power menu")) return needsConfirmation("Power menu")
                status(svc.global(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG), "done")
            }
            "list_apps" -> listApps(ctx, args.optString("filter"))
            else -> null
        }
    }

    /** Tap at a position (percent of the screen). Checked against the confirmation words of whatever is under it. */
    private fun tapXy(svc: NovaAccessibilityService, args: JSONObject): JSONObject {
        val x = num(args, "x"); val y = num(args, "y")
        if (x == null || y == null) return result("bad_position")
        svc.readScreen()
        val risky = svc.labelsAt(x, y).firstOrNull { ActionSafety.needsConfirmation(it) }
        if (risky != null && !isApproved(args, risky)) return needsConfirmation(risky)
        return status(svc.tapPercent(x, y), "tapped")
    }

    /** Long press or double tap on a label, or on a position. */
    private fun byTextOrPosition(svc: NovaAccessibilityService, args: JSONObject, longPress: Boolean): JSONObject {
        val done = if (longPress) "long_pressed" else "double_tapped"
        val text = args.optString("text").trim()
        if (text.isNotEmpty()) {
            val obs = svc.readScreen() ?: return result("failed")
            val el = ScreenFinder.find(obs, text, needClickable = false) ?: return result("element_not_found")
            if (ActionSafety.needsConfirmation(el.label) && !isApproved(args, el.label)) return needsConfirmation(el.label)
            return status(if (longPress) svc.longClick(el.id) else svc.doubleClick(el.id), done)
        }
        val x = num(args, "x"); val y = num(args, "y")
        if (x == null || y == null) return result("bad_position")
        svc.readScreen()
        val risky = svc.labelsAt(x, y).firstOrNull { ActionSafety.needsConfirmation(it) }
        if (risky != null && !isApproved(args, risky)) return needsConfirmation(risky)
        return status(if (longPress) svc.longPressPercent(x, y) else svc.doubleTapPercent(x, y), done)
    }

    /** Scrolls down until the label shows up (without tapping it). Gives up at the end of the list or after 12 scrolls. */
    private fun scrollToText(svc: NovaAccessibilityService, text: String): JSONObject {
        if (text.isBlank()) return result("element_not_found")
        for (attempt in 0..12) {
            val obs = svc.readScreen() ?: return result("failed")
            ScreenFinder.find(obs, text, needClickable = false)?.let {
                return result("found").put("message", "\"${it.label}\" is on screen now.")
            }
            if (svc.scroll(down = true) != DriverResult.OK) break
            Thread.sleep(400)
        }
        return result("element_not_found")
    }

    /** Reads the notification shade, then closes it. Codes and passwords are hidden from the reading. */
    private fun readNotifications(svc: NovaAccessibilityService): JSONObject {
        if (svc.global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) != DriverResult.OK) return result("failed")
        Thread.sleep(900)
        val obs = svc.readScreen()
        if (Build.VERSION.SDK_INT >= 31) svc.global(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        else svc.global(AccessibilityService.GLOBAL_ACTION_BACK)
        if (obs == null) return result("failed")
        val lines = obs.elements.take(60).map { if (ActionSafety.isSecretField(it.label)) "[hidden: sensitive]" else it.label }
        AgentTrace.add("Read notifications: ${lines.size} lines")
        return result("ok").put("notifications", lines.joinToString("\n"))
    }

    private fun openAllApps(svc: NovaAccessibilityService): JSONObject {
        if (Build.VERSION.SDK_INT >= 32) {
            return status(svc.global(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS), "done")
        }
        // Older Android: go home and swipe up to open the app drawer.
        svc.home()
        Thread.sleep(600)
        return status(svc.swipe("up"), "done")
    }

    private fun listApps(ctx: Context, filter: String): JSONObject {
        val f = filter.trim().lowercase()
        val names = AppIndex.get(ctx).keys.filter { f.isEmpty() || it.contains(f) }.sorted().take(80)
        return result("ok").put("apps", names.joinToString(", "))
    }
}
