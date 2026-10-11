package com.Lia.assistant.access

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import com.Lia.assistant.action.AppIndex
import com.Lia.assistant.agent.core.DriverResult
import org.json.JSONObject
import java.util.Calendar

/**
 * More phone tools: stop a running task, phone status, screen time, volume per sound type, wake the screen,
 * read full text, copy text from the screen, scroll to the top or bottom, call buttons, take a photo.
 * Returns null for names it does not own.
 */
object ExtraTools {
    val names = setOf(
        "stop_task", "device_status", "screen_time", "set_stream_volume", "wake_screen",
        "copy_screen_text", "read_full_text", "scroll_to_edge", "call_action", "take_photo",
    )

    private fun result(value: String): JSONObject = JSONObject().put("result", value)

    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject? {
        if (name !in names) return null
        return try {
            when (name) {
                "stop_task" -> { ScreenAgent.cancelRequested = true; result("stopping") }
                "device_status" -> deviceStatus(ctx)
                "screen_time" -> screenTime(ctx)
                "set_stream_volume" -> streamVolume(ctx, args.optString("stream"), args.optString("level").trim().toIntOrNull() ?: -1)
                "wake_screen" -> wakeScreen(ctx)
                else -> withService(ctx, name, args)
            }
        } catch (e: Exception) {
            result("failed")
        }
    }

    /** Tools that need the screen service. */
    private fun withService(ctx: Context, name: String, args: JSONObject): JSONObject {
        val svc = NovaAccessibilityService.instance ?: return result("accessibility_not_enabled")
        return when (name) {
            "read_full_text" -> readFullText(svc, args.optString("text"))
            "copy_screen_text" -> copyScreenText(ctx, svc, args.optString("text"))
            "scroll_to_edge" -> scrollToEdge(svc, args.optString("edge"))
            "call_action" -> callAction(svc, args)
            "take_photo" -> takePhoto(ctx, svc, args.optString("camera"))
            else -> result("failed")
        }
    }

    // ---- status ----

    private fun deviceStatus(ctx: Context): JSONObject {
        val parts = mutableListOf<String>()
        ctx.getSystemService(BatteryManager::class.java)?.let { bm ->
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            parts += "Battery $level%${if (bm.isCharging) ", charging" else ""}"
        }
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = if (cm == null) null else cm.getNetworkCapabilities(cm.activeNetwork)
        parts += when {
            caps == null -> "No internet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Internet on Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Internet on mobile data"
            else -> "Internet connected"
        }
        val stat = StatFs(Environment.getDataDirectory().path)
        val gb = 1024.0 * 1024 * 1024
        parts += "Storage free %.1f GB of %.1f GB".format(stat.availableBytes / gb, stat.totalBytes / gb)
        ctx.getSystemService(AudioManager::class.java)?.let { am ->
            val ringer = when (am.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "silent"
                AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
                else -> "normal"
            }
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            parts += "Ringer $ringer, media volume ${am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max}%"
        }
        try {
            val b = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            parts += "Brightness ${b * 100 / 255}%"
        } catch (e: Settings.SettingNotFoundException) {
            // Not reported on this phone.
        }
        return result("ok").put("status", parts.joinToString(". "))
    }

    private fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow("android:get_usage_stats", Process.myUid(), ctx.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Time spent in apps today, top five. Needs the Usage access permission, which the user grants once in Settings. */
    private fun screenTime(ctx: Context): JSONObject {
        if (!hasUsageAccess(ctx)) {
            return try {
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                result("grant_usage_access_then_retry")
            } catch (e: Exception) {
                result("failed")
            }
        }
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return result("failed")
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val stats = usm.queryAndAggregateUsageStats(start, System.currentTimeMillis())
        val labels = AppIndex.get(ctx).entries.associate { it.value to it.key }
        val ranked = stats.values
            .filter { it.totalTimeInForeground >= 60_000L && it.packageName != ctx.packageName }
            .sortedByDescending { it.totalTimeInForeground }
        val total = ranked.sumOf { it.totalTimeInForeground } / 60_000L
        val top = ranked.take(5).joinToString(", ") {
            "${labels[it.packageName] ?: it.packageName} ${it.totalTimeInForeground / 60_000L} min"
        }
        return result("ok").put("summary", "Screen time today: about $total minutes. Most used: ${top.ifEmpty { "nothing yet" }}.")
    }

    // ---- sound and screen ----

    private fun streamVolume(ctx: Context, stream: String, level: Int): JSONObject {
        if (level !in 0..100) return result("bad_volume_request")
        val type = when (stream.trim().lowercase()) {
            "ring", "ringtone" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "call" -> AudioManager.STREAM_VOICE_CALL
            "media", "music" -> AudioManager.STREAM_MUSIC
            else -> return result("bad_stream")
        }
        val am = ctx.getSystemService(AudioManager::class.java) ?: return result("failed")
        return try {
            am.setStreamVolume(type, level * am.getStreamMaxVolume(type) / 100, AudioManager.FLAG_SHOW_UI)
            result("volume_changed")
        } catch (e: SecurityException) {
            // Setting some sounds to zero needs Do Not Disturb access.
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            result("grant_dnd_access")
        }
    }

    private fun wakeScreen(ctx: Context): JSONObject {
        val pm = ctx.getSystemService(PowerManager::class.java) ?: return result("failed")
        @Suppress("DEPRECATION")
        val lock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Lia:wake",
        )
        lock.acquire(5_000L)
        return result("screen_on")
    }

    // ---- screen text ----

    /** Full text of one element, or of the whole screen when no label is given. Codes are hidden. */
    private fun readFullText(svc: NovaAccessibilityService, text: String): JSONObject {
        val obs = svc.readScreen() ?: return result("failed")
        val body = if (text.isBlank()) {
            svc.fullScreenText()
        } else {
            val el = ScreenFinder.find(obs, text, needClickable = false) ?: return result("element_not_found")
            svc.fullText(el.id) ?: return result("element_not_found")
        }
        val safe = body.lines().joinToString("\n") { if (ActionSafety.isSecretField(it)) "[hidden: sensitive]" else it }
        return result("ok").put("text", safe.take(4000))
    }

    private fun copyScreenText(ctx: Context, svc: NovaAccessibilityService, text: String): JSONObject {
        if (text.isBlank()) return result("element_not_found")
        val obs = svc.readScreen() ?: return result("failed")
        val el = ScreenFinder.find(obs, text, needClickable = false) ?: return result("element_not_found")
        val full = svc.fullText(el.id) ?: return result("element_not_found")
        if (ActionSafety.isSecretField(full)) return result("not_allowed_sensitive")
        val cm = ctx.getSystemService(android.content.ClipboardManager::class.java) ?: return result("failed")
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Lia", full))
        return result("copied")
    }

    private fun scrollToEdge(svc: NovaAccessibilityService, edge: String): JSONObject {
        val down = when (edge.trim().lowercase()) {
            "bottom", "end" -> true
            "top", "start" -> false
            else -> return result("bad_edge")
        }
        var moved = 0
        for (i in 0 until 25) {
            if (svc.scroll(down) != DriverResult.OK) break
            moved++
            Thread.sleep(250)
        }
        if (moved == 0) return result("nothing_scrollable")
        return result(if (down) "at_bottom" else "at_top")
    }

    // ---- calls and camera ----

    private fun callAction(svc: NovaAccessibilityService, args: JSONObject): JSONObject {
        val labels = CallRules.labels(args.optString("action")) ?: return result("bad_call_action")
        val obs = svc.readScreen() ?: return result("failed")
        val el = labels.firstNotNullOfOrNull { ScreenFinder.find(obs, it) } ?: return result("button_not_found")
        return if (svc.click(el.id) == DriverResult.OK) result("done") else result("failed")
    }

    private fun takePhoto(ctx: Context, svc: NovaAccessibilityService, camera: String): JSONObject {
        try {
            ctx.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            return result("app_not_found")
        }
        Thread.sleep(2200)
        if (camera.trim().lowercase() == "front") {
            val obs = svc.readScreen()
            val flip = obs?.let { o -> CallRules.flipLabels.firstNotNullOfOrNull { ScreenFinder.find(o, it) } }
            if (flip != null) {
                svc.click(flip.id)
                Thread.sleep(1000)
            }
        }
        val obs = svc.readScreen() ?: return result("failed")
        val shutter = CallRules.shutterLabels.firstNotNullOfOrNull { ScreenFinder.find(obs, it) }
            ?: return result("shutter_not_found")
        if (svc.click(shutter.id) != DriverResult.OK) return result("failed")
        Thread.sleep(900)
        return result("photo_taken")
    }
}
