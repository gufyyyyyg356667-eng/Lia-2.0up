package com.Lia.assistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    DARK, LIGHT, SYSTEM;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemDark
    }

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: DARK
    }
}

val LocalLiaColors = staticCompositionLocalOf { LiaColorSchemes.Dark }
val LocalReducedMotion = staticCompositionLocalOf { false }

// Serif for display lines, sans for everything else. Drop the bundled Instrument Serif and
// Manrope files into res/font and switch these two families to R.font references.
private val DisplayFamily = FontFamily.Serif
private val BodyFamily = FontFamily.SansSerif

val LiaTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontSize = 40.sp, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFamily, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = BodyFamily, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = BodyFamily, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = BodyFamily, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontFamily = BodyFamily, fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun LiaTheme(
    mode: ThemeMode = ThemeMode.DARK,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark(isSystemInDarkTheme())
    val c = if (dark) LiaColorSchemes.Dark else LiaColorSchemes.Light
    val colorScheme = if (dark) {
        darkColorScheme(
            background = c.background,
            surface = c.surface,
            surfaceVariant = c.surfaceRaised,
            primary = c.accent,
            onPrimary = c.onAccent,
            secondary = c.accentSecondary,
            onBackground = c.textPrimary,
            onSurface = c.textPrimary,
            onSurfaceVariant = c.textSecondary,
            outline = c.surfaceBorder,
            error = c.error,
        )
    } else {
        lightColorScheme(
            background = c.background,
            surface = c.surface,
            surfaceVariant = c.surfaceRaised,
            primary = c.accent,
            onPrimary = c.onAccent,
            secondary = c.accentSecondary,
            onBackground = c.textPrimary,
            onSurface = c.textPrimary,
            onSurfaceVariant = c.textSecondary,
            outline = c.surfaceBorder,
            error = c.error,
        )
    }

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = c.background.toArgb()
            window.navigationBarColor = c.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalLiaColors provides c,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LiaTypography,
            shapes = LiaShapes.material,
            content = content,
        )
    }
}
