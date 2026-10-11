package com.Lia.assistant.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.Lia.assistant.alerts.BatteryGuide
import com.Lia.assistant.alerts.Routines
import com.Lia.assistant.alerts.InfoSources
import com.Lia.assistant.alerts.Reminders
import com.Lia.assistant.data.Prefs
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Reminders, weather, nearest pharmacy, calendar, and the emergency dialer. Runs on a background thread. */
object InfoTools {
    private val names = setOf(
        "set_reminder", "check_weather", "nearest_pharmacy", "read_calendar", "emergency_call",
        "save_routine", "run_routine", "ask_after", "keep_running_help", "teach_app",
    )

    fun declarations(): JSONArray = JSONArray()
        .put(LiaToolCatalog.functionDecl(
            "set_reminder",
            "Say something at a later time. Give minutes from now, or a time HH:MM. Set daily to yes for a repeating reminder.",
            mapOf("text" to "What to say", "minutes" to "minutes from now", "time" to "HH:MM, 24-hour", "daily" to "yes or no"),
            listOf("text"),
        ))
        .put(LiaToolCatalog.functionDecl("check_weather", "Tell the current weather where the user is.", emptyMap(), emptyList()))
        .put(LiaToolCatalog.functionDecl("nearest_pharmacy", "Find the nearest pharmacies to the user.", emptyMap(), emptyList()))
        .put(LiaToolCatalog.functionDecl(
            "read_calendar",
            "Read calendar events for the next hours.",
            mapOf("hours" to "how many hours ahead"),
            emptyList(),
        ))
        .put(LiaToolCatalog.functionDecl(
            "save_routine",
            "Save a named routine: a name and the full steps in one sentence, for example 'good night' = lower brightness, set ringer silent, set alarm for 7 AM.",
            mapOf("name" to "Routine name", "steps" to "The full request in one sentence"),
            listOf("name", "steps"),
        ))
        .put(LiaToolCatalog.functionDecl(
            "run_routine",
            "Run a saved routine by name. The result says run_with_screen_task: then call run_screen_task with the goal it returns.",
            mapOf("name" to "Routine name"),
            listOf("name"),
        ))
        .put(LiaToolCatalog.functionDecl(
            "ask_after",
            "Ask the user a question after some minutes, e.g. after a timer or alarm: 'did you take your medicine?'",
            mapOf("minutes" to "minutes from now", "question" to "What to ask, in the user's language"),
            listOf("minutes", "question"),
        ))
        .put(LiaToolCatalog.functionDecl(
            "teach_app",
            "Remember where something is inside an app, when the user tells you. Example: app 'Instagram', note 'the DM button is at the top right'.",
            mapOf("app" to "App name", "note" to "What the user said about the app"),
            listOf("app", "note"),
        ))
        .put(LiaToolCatalog.functionDecl(
            "keep_running_help",
            "Explain how to stop this phone from closing Lia in the background.",
            emptyMap(),
            emptyList(),
        ))
        .put(LiaToolCatalog.functionDecl(
            "emergency_call",
            "Open the dialer with the emergency contact. Only when the user asks for emergency help. The user presses Call.",
            emptyMap(),
            emptyList(),
        ))

    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject? {
        if (name !in names) return null
        return try {
            when (name) {
                "set_reminder" -> setReminder(ctx, args)
                "check_weather" -> checkWeather(ctx)
                "nearest_pharmacy" -> nearestPharmacy(ctx)
                "read_calendar" -> readCalendar(ctx, args.optInt("hours", 24).coerceIn(1, 72))
                "emergency_call" -> emergencyCall(ctx)
                "save_routine" -> saveRoutine(ctx, args)
                "run_routine" -> runRoutine(ctx, args.optString("name"))
                "ask_after" -> askAfter(ctx, args)
                "keep_running_help" -> say("ok", BatteryGuide.message(android.os.Build.MANUFACTURER))
                "teach_app" -> teachApp(ctx, args.optString("app"), args.optString("note"))
                else -> result("failed")
            }
        } catch (e: Exception) {
            result("failed")
        }
    }

    private fun result(value: String): JSONObject = JSONObject().put("result", value)

    private fun say(value: String, message: String): JSONObject = result(value).put("message", message)

    private fun setReminder(ctx: Context, args: JSONObject): JSONObject {
        val text = args.optString("text").trim()
        if (text.isEmpty()) return result("nothing_to_remind")
        val minutes = args.optInt("minutes", -1)
        val time = args.optString("time").trim()
        val at: Long = when {
            minutes > 0 -> System.currentTimeMillis() + minutes * 60_000L
            time.matches(Regex("\\d{1,2}:\\d{2}")) -> {
                val (h, m) = time.split(":").map { it.toInt() }
                if (h !in 0..23 || m !in 0..59) return result("bad_time")
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            else -> return result("bad_time")
        }
        Reminders.add(ctx, at, text, daily = args.optString("daily") == "yes")
        return say("reminder_set", "ঠিক আছে, মনে করিয়ে দেবো।")
    }

    private fun checkWeather(ctx: Context): JSONObject {
        val loc = InfoSources.lastLocation(ctx) ?: return say("location_off", "লোকেশন অনুমতি বা লোকেশন চালু করুন।")
        val text = InfoSources.weatherText(loc.latitude, loc.longitude) ?: return say("failed", "আবহাওয়ার তথ্য পাওয়া যায়নি।")
        return say("ok", text)
    }

    private fun nearestPharmacy(ctx: Context): JSONObject {
        val loc = InfoSources.lastLocation(ctx) ?: return say("location_off", "লোকেশন অনুমতি বা লোকেশন চালু করুন।")
        val places = InfoSources.nearestPharmacies(loc.latitude, loc.longitude, 3)
        if (places.isEmpty()) return say("none_found", "কাছাকাছি ফার্মেসি পাওয়া যায়নি।")
        val first = places[0]
        val rest = places.drop(1).joinToString(", ") { "${it.name} (${it.meters} মিটার)" }
        val tail = if (rest.isEmpty()) "" else " আরও: $rest।"
        return say("ok", "সবচেয়ে কাছের: ${first.name}, প্রায় ${first.meters} মিটার দূরে।$tail")
    }

    private fun readCalendar(ctx: Context, hours: Int): JSONObject {
        val now = System.currentTimeMillis()
        val events = InfoSources.eventsBetween(ctx, now, now + hours * 3_600_000L)
        if (events.isEmpty()) return say("ok", "পরের $hours ঘণ্টায় কোনো ইভেন্ট নেই।")
        return say("ok", events.joinToString("; ") { it.first } + "।")
    }

    private fun teachApp(ctx: Context, appName: String, note: String): JSONObject {
        if (appName.isBlank() || note.isBlank()) return result("nothing_to_learn")
        val pkg = AppMatcher.match(BanglaNames.toEnglish(appName), AppIndex.get(ctx)) ?: appName.trim().lowercase()
        AppMemory.teach(ctx, pkg, note)
        return say("learned", "ঠিক আছে, মনে রাখলাম।")
    }

    private fun saveRoutine(ctx: Context, args: JSONObject): JSONObject {
        val name = args.optString("name").trim()
        val steps = args.optString("steps").trim()
        if (name.isEmpty() || steps.isEmpty()) return result("nothing_to_save")
        Routines.save(ctx, name, steps)
        return say("routine_saved", "রুটিন '$name' সেভ হয়েছে।")
    }

    private fun runRoutine(ctx: Context, name: String): JSONObject {
        val goal = Routines.get(ctx, name) ?: return say("routine_not_found", "রুটিন '$name' পাওয়া যায়নি।")
        return say("run_with_screen_task", "এখন চালাচ্ছি।").put("goal", goal)
    }

    private fun askAfter(ctx: Context, args: JSONObject): JSONObject {
        val minutes = args.optInt("minutes", -1)
        val question = args.optString("question").trim()
        if (minutes <= 0 || question.isEmpty()) return result("bad_request")
        Reminders.add(ctx, System.currentTimeMillis() + minutes * 60_000L, question, daily = false)
        return say("ask_set", "ঠিক আছে, $minutes মিনিট পরে জিজ্ঞেস করবো।")
    }

    private fun emergencyCall(ctx: Context): JSONObject {
        val number = Prefs.emergencyNumber(ctx)
        if (number.isBlank()) return say("emergency_not_set", "জরুরি নম্বর সেট করা নেই। Profile-এ নম্বর দিন।")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
        return say("dialer_ready", "ডায়ালার প্রস্তুত। কল বাটন আপনি চাপবেন।")
    }
}
