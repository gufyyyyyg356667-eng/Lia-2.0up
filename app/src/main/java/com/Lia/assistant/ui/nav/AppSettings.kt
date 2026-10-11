package com.Lia.assistant.ui.nav

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.ui.theme.ThemeMode

/**
 * Observable app settings. Theme and motion are read by the root theme. [prefsVersion] changes
 * whenever a stored preference changes, so screens that show those values refresh.
 */
object AppSettings {
    var themeMode by mutableStateOf(ThemeMode.DARK)
        private set
    var reducedMotion by mutableStateOf(false)
        private set
    var prefsVersion by mutableIntStateOf(0)
        private set

    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        themeMode = Prefs.themeMode(ctx)
        reducedMotion = Prefs.reducedMotion(ctx)
        loaded = true
    }

    fun setTheme(ctx: Context, mode: ThemeMode) {
        themeMode = mode
        Prefs.saveThemeMode(ctx, mode)
    }

    fun setReducedMotion(ctx: Context, on: Boolean) {
        reducedMotion = on
        Prefs.saveReducedMotion(ctx, on)
    }

    /** Call after saving any other preference. */
    fun notifyPrefsChanged() {
        prefsVersion++
    }
}
