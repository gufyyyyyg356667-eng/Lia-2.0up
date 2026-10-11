package com.Lia.assistant.data

import android.content.Context
import com.Lia.assistant.ui.fx.NovaOrbStyle
import com.Lia.assistant.ui.theme.ThemeMode
import com.Lia.assistant.voice.LanguagePreference
import com.Lia.assistant.voice.Personality

object AssistantBrand {
    const val NAME = "Lia"
    const val FULL_NAME = "Lia AI"
    const val TAGLINE = "Your Intelligent AI Companion"
}

/**
 * Local settings. The Gemini API key is typed by the user in the app and stored only on this device.
 * It is never hardcoded and never logged.
 */
object Prefs {
    private const val FILE = "lia_prefs"
    private const val KEY_API = "gemini_api_key"
    private const val KEY_PERSONALITY = "personality"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_NAME = "assistant_name"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_REDUCED_MOTION = "reduced_motion"
    private const val KEY_ORB = "orb_style"
    private const val KEY_ONBOARDED = "has_onboarded"

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getApiKey(ctx: Context): String = sp(ctx).getString(KEY_API, null).orEmpty()
    fun hasApiKey(ctx: Context): Boolean = getApiKey(ctx).isNotBlank()
    fun saveApiKey(ctx: Context, key: String) {
        sp(ctx).edit().putString(KEY_API, key.trim()).apply()
    }
    fun clearApiKey(ctx: Context) {
        sp(ctx).edit().remove(KEY_API).apply()
    }

    fun personality(ctx: Context): Personality =
        Personality.fromName(sp(ctx).getString(KEY_PERSONALITY, null))

    fun savePersonality(ctx: Context, p: Personality) {
        sp(ctx).edit().putString(KEY_PERSONALITY, p.name).apply()
    }

    fun language(ctx: Context): LanguagePreference =
        LanguagePreference.fromName(sp(ctx).getString(KEY_LANGUAGE, null))

    fun saveLanguage(ctx: Context, l: LanguagePreference) {
        sp(ctx).edit().putString(KEY_LANGUAGE, l.name).apply()
    }

    fun assistantName(ctx: Context): String =
        sp(ctx).getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: AssistantBrand.NAME

    fun saveAssistantName(ctx: Context, name: String) {
        sp(ctx).edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun userName(ctx: Context): String = sp(ctx).getString(KEY_USER_NAME, null).orEmpty()

    fun saveUserName(ctx: Context, name: String) {
        sp(ctx).edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun themeMode(ctx: Context): ThemeMode = ThemeMode.fromName(sp(ctx).getString(KEY_THEME, null))

    fun saveThemeMode(ctx: Context, mode: ThemeMode) {
        sp(ctx).edit().putString(KEY_THEME, mode.name).apply()
    }

    fun reducedMotion(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_REDUCED_MOTION, false)

    fun saveReducedMotion(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean(KEY_REDUCED_MOTION, on).apply()
    }

    fun orbStyle(ctx: Context): NovaOrbStyle = NovaOrbStyle.fromName(sp(ctx).getString(KEY_ORB, null))

    fun saveOrbStyle(ctx: Context, style: NovaOrbStyle) {
        sp(ctx).edit().putString(KEY_ORB, style.name).apply()
    }

    fun briefingHour(ctx: Context): Int = sp(ctx).getInt("briefing_hour", 8)

    fun saveBriefingHour(ctx: Context, hour: Int) {
        sp(ctx).edit().putInt("briefing_hour", hour.coerceIn(0, 23)).apply()
    }

    fun emergencyNumber(ctx: Context): String = sp(ctx).getString("emergency_number", null).orEmpty()

    fun saveEmergencyNumber(ctx: Context, number: String) {
        sp(ctx).edit().putString("emergency_number", number.trim()).apply()
    }

    fun mood(ctx: Context): com.Lia.assistant.voice.Mood =
        com.Lia.assistant.voice.Mood.fromName(sp(ctx).getString("mood", null))

    fun saveMood(ctx: Context, mood: com.Lia.assistant.voice.Mood) {
        sp(ctx).edit().putString("mood", mood.name).apply()
        com.Lia.assistant.voice.MoodState.mood = mood
    }

    fun wakeName(ctx: Context): String = sp(ctx).getString("wake_name", null)?.takeIf { it.isNotBlank() } ?: "Lia"

    fun saveWakeName(ctx: Context, name: String) {
        sp(ctx).edit().putString("wake_name", name.trim()).apply()
    }

    fun wakeOn(ctx: Context): Boolean = sp(ctx).getBoolean("wake_on", false)

    fun saveWakeOn(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean("wake_on", on).apply()
    }

    fun alertsOn(ctx: Context): Boolean = sp(ctx).getBoolean("phone_alerts", true)

    fun saveAlertsOn(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean("phone_alerts", on).apply()
    }

    fun hasOnboarded(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_ONBOARDED, false)

    fun saveOnboarded(ctx: Context) {
        sp(ctx).edit().putBoolean(KEY_ONBOARDED, true).apply()
    }
}
