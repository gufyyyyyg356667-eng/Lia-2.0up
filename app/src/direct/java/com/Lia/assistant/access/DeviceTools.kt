package com.Lia.assistant.access

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import org.json.JSONObject

/** Phone controls that do not need the screen: torch, volume, alarms, settings pages, URLs, global keys. */
object DeviceTools {
    private val names = setOf(
        "set_flashlight", "change_volume", "set_alarm", "set_timer", "open_settings",
        "open_url", "take_screenshot", "lock_screen", "open_notifications", "quick_settings",
        "media_control", "set_ringer", "set_brightness", "open_camera", "navigate", "open_recent",
        "share_text", "dial_number", "prepare_sms", "open_whatsapp_number", "copy_text", "set_auto_rotate",
        "add_calendar_event", "add_contact", "compose_email", "open_gallery", "open_app_info",
        "open_notification_settings", "open_wallpaper", "open_alarms", "open_contacts",
    )

    fun owns(name: String): Boolean = name in names

    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject? {
        if (!owns(name)) return null
        return try {
            when (name) {
                "set_flashlight" -> flashlight(ctx, args.optString("state") == "on")
                "change_volume" -> volume(ctx, args)
                "set_alarm" -> alarm(ctx, args)
                "set_timer" -> timer(ctx, args)
                "open_settings" -> settingsPage(ctx, args.optString("panel"))
                "open_url" -> openUrl(ctx, args.optString("url"))
                "take_screenshot" -> global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                "lock_screen" -> global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                "open_notifications" -> global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                "quick_settings" -> global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                "media_control" -> mediaControl(ctx, args.optString("action"))
                "set_ringer" -> ringer(ctx, args.optString("mode"))
                "set_brightness" -> brightness(ctx, args.optInt("level", -1))
                "open_camera" -> start(ctx, Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), "camera_opened")
                "navigate" -> navigate(ctx, args.optString("destination"))
                "open_recent" -> global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                "share_text" -> shareText(ctx, args.optString("text"))
                "dial_number" -> dial(ctx, args.optString("number"))
                "prepare_sms" -> prepareSms(ctx, args.optString("number"), args.optString("message"))
                "open_whatsapp_number" -> whatsAppNumber(ctx, args.optString("number"), args.optString("message"))
                "copy_text" -> copyText(ctx, args.optString("text"))
                "set_auto_rotate" -> autoRotate(ctx, args.optString("state") == "on")
                "add_calendar_event" -> calendarEvent(ctx, args.optString("title"), args.optString("start_time"), args.optString("location"))
                "add_contact" -> addContact(ctx, args.optString("name"), args.optString("number"))
                "compose_email" -> composeEmail(ctx, args.optString("to"), args.optString("subject"), args.optString("body"))
                "open_gallery" -> start(ctx, Intent(Intent.ACTION_VIEW).setType("image/*"), "gallery_opened")
                "open_app_info" -> appInfo(ctx, args.optString("package"))
                "open_notification_settings" -> notificationSettings(ctx, args.optString("package"))
                "open_wallpaper" -> start(ctx, Intent(Intent.ACTION_SET_WALLPAPER), "wallpaper_opened")
                "open_alarms" -> start(ctx, Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS), "alarms_opened")
                "open_contacts" -> start(ctx, Intent(Intent.ACTION_VIEW, android.provider.ContactsContract.Contacts.CONTENT_URI), "contacts_opened")
                else -> result("failed")
            }
        } catch (e: Exception) {
            result("failed")
        }
    }

    private fun result(value: String): JSONObject = JSONObject().put("result", value)

    private fun flashlight(ctx: Context, on: Boolean): JSONObject {
        val cm = ctx.getSystemService(CameraManager::class.java) ?: return result("failed")
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return result("no_flashlight")
        cm.setTorchMode(id, on)
        return result(if (on) "flashlight_on" else "flashlight_off")
    }

    private fun volume(ctx: Context, args: JSONObject): JSONObject {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return result("failed")
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val dir = args.optString("direction")
        val level = args.optInt("level", -1)
        when {
            dir == "up" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            dir == "down" -> am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            level in 0..100 -> am.setStreamVolume(AudioManager.STREAM_MUSIC, level * max / 100, AudioManager.FLAG_SHOW_UI)
            else -> return result("bad_volume_request")
        }
        return result("volume_changed")
    }

    private fun alarm(ctx: Context, args: JSONObject): JSONObject {
        val hour = args.optInt("hour", -1)
        val minute = args.optInt("minute", -1)
        if (hour !in 0..23 || minute !in 0..59) return result("bad_time")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, args.optString("message", "Lia alarm"))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return start(ctx, intent, "alarm_set")
    }

    private fun timer(ctx: Context, args: JSONObject): JSONObject {
        val minutes = args.optInt("minutes", -1)
        if (minutes !in 1..720) return result("bad_duration")
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return start(ctx, intent, "timer_set")
    }

    private fun settingsPage(ctx: Context, panel: String): JSONObject {
        val action = ActionSafety.allowedSettings[panel.lowercase()] ?: return result("panel_not_allowed")
        return start(ctx, Intent(action), "settings_opened")
    }

    private fun openUrl(ctx: Context, url: String): JSONObject {
        if (!ActionSafety.isWebUrl(url)) return result("url_not_allowed")
        return start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())), "url_opened")
    }

    private fun global(action: Int): JSONObject {
        val r = NovaAccessibilityService.instance?.global(action)
            ?: return result("accessibility_not_enabled")
        return result(if (r == com.Lia.assistant.agent.core.DriverResult.OK) "done" else "failed")
    }

    private fun mediaControl(ctx: Context, action: String): JSONObject {
        val code = when (action) {
            "play_pause" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            "previous" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return result("bad_media_action")
        }
        val am = ctx.getSystemService(AudioManager::class.java) ?: return result("failed")
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, code))
        return result("media_$action")
    }

    private fun ringer(ctx: Context, mode: String): JSONObject {
        val am = ctx.getSystemService(AudioManager::class.java) ?: return result("failed")
        val value = when (mode) {
            "normal" -> AudioManager.RINGER_MODE_NORMAL
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            "silent" -> AudioManager.RINGER_MODE_SILENT
            else -> return result("bad_ringer_mode")
        }
        return try {
            am.ringerMode = value
            result("ringer_$mode")
        } catch (e: SecurityException) {
            // Silent mode needs Do Not Disturb access, which the user grants in Settings.
            start(ctx, Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), "grant_dnd_access")
        }
    }

    private fun brightness(ctx: Context, level: Int): JSONObject {
        if (level !in 0..100) return result("bad_brightness")
        if (!android.provider.Settings.System.canWrite(ctx)) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
            return start(ctx, intent, "grant_write_settings_then_retry")
        }
        val cr = ctx.contentResolver
        android.provider.Settings.System.putInt(cr, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        android.provider.Settings.System.putInt(cr, android.provider.Settings.System.SCREEN_BRIGHTNESS, level * 255 / 100)
        return result("brightness_set")
    }

    private fun navigate(ctx: Context, destination: String): JSONObject {
        val dest = destination.trim()
        if (dest.isEmpty()) return result("nothing_to_navigate")
        val maps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(dest)}"))
        if (ctx.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps") != null) {
            maps.setPackage("com.google.android.apps.maps")
        }
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(dest)}"))
        return if (maps.resolveActivity(ctx.packageManager) != null) start(ctx, maps, "navigation_started")
        else start(ctx, fallback, "map_opened")
    }

    private fun digits(number: String): String = number.filter { it.isDigit() || it == '+' }

    private fun shareText(ctx: Context, text: String): JSONObject {
        if (text.isBlank()) return result("nothing_to_share")
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        return start(ctx, Intent.createChooser(send, "Share"), "share_opened")
    }

    /** Puts the number in the dialer. The user presses Call. */
    private fun dial(ctx: Context, number: String): JSONObject {
        val n = digits(number)
        if (n.isEmpty()) return result("bad_number")
        return start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")), "dialer_ready")
    }

    /** Opens a text to the number with the message filled in. The user presses Send. */
    private fun prepareSms(ctx: Context, number: String, message: String): JSONObject {
        val n = digits(number)
        if (n.isEmpty()) return result("bad_number")
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$n")).putExtra("sms_body", message)
        return start(ctx, intent, "sms_ready")
    }

    /** Opens WhatsApp on a number with the text filled in. The user presses Send. */
    private fun whatsAppNumber(ctx: Context, number: String, message: String): JSONObject {
        val n = digits(number).removePrefix("+")
        if (n.isEmpty()) return result("bad_number")
        val url = "https://wa.me/$n?text=${Uri.encode(message)}"
        return start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "whatsapp_ready")
    }

    private fun copyText(ctx: Context, text: String): JSONObject {
        if (text.isEmpty()) return result("nothing_to_copy")
        val cm = ctx.getSystemService(android.content.ClipboardManager::class.java) ?: return result("failed")
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Lia", text))
        return result("copied")
    }

    private fun autoRotate(ctx: Context, on: Boolean): JSONObject {
        if (!android.provider.Settings.System.canWrite(ctx)) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
            return start(ctx, intent, "grant_write_settings_then_retry")
        }
        android.provider.Settings.System.putInt(ctx.contentResolver, android.provider.Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0)
        return result(if (on) "auto_rotate_on" else "auto_rotate_off")
    }

    /** Opens the calendar with a new event filled in. The user saves it. start_time is epoch milliseconds. */
    private fun calendarEvent(ctx: Context, title: String, startTime: String, location: String): JSONObject {
        if (title.isBlank()) return result("nothing_to_add")
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(android.provider.CalendarContract.Events.CONTENT_URI)
            .putExtra(android.provider.CalendarContract.Events.TITLE, title)
            .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, location)
        startTime.toLongOrNull()?.let {
            intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, it)
        }
        return start(ctx, intent, "calendar_ready")
    }

    /** Opens the contact form with the name and number filled in. The user saves it. */
    private fun addContact(ctx: Context, name: String, number: String): JSONObject {
        val intent = Intent(Intent.ACTION_INSERT).setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE)
            .putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
            .putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, digits(number))
        return start(ctx, intent, "contact_form_ready")
    }

    /** Opens an email with the fields filled in. The user presses Send. */
    private fun composeEmail(ctx: Context, to: String, subject: String, body: String): JSONObject {
        if (to.isBlank()) return result("bad_email")
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(to.trim())}"))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        return start(ctx, intent, "email_ready")
    }

    private fun notificationSettings(ctx: Context, pkg: String): JSONObject {
        if (pkg.isBlank()) return result("bad_package")
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, pkg)
        return start(ctx, intent, "notification_settings_opened")
    }

    private fun appInfo(ctx: Context, pkg: String): JSONObject {
        if (pkg.isBlank()) return result("bad_package")
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$pkg"))
        return start(ctx, intent, "app_info_opened")
    }

    private fun start(ctx: Context, intent: Intent, ok: String): JSONObject = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        result(ok)
    } catch (e: Exception) {
        result("app_not_found")
    }
}
