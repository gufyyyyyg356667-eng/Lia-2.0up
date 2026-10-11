package com.Lia.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Identity: "a pearl of light in the night". Lotus and lagoon are reserved for the orb. */
data class LiaColorScheme(
    val background: Color,
    val backgroundGradientTop: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val surfaceBorder: Color,
    val surfaceRaised: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val userBubble: Color,
    val assistantBubble: Color,
    val depthShadow: Color,
    val rimLight: Color,
    val lagoon: Color,
    val lotus: Color,
)

object LiaColorSchemes {
    val Dark = LiaColorScheme(
        background = Color(0xFF06050E),
        backgroundGradientTop = Color(0xFF15113A),
        surface = Color(0xFF17143A),
        surfaceGlass = Color(0xCC221E4D),
        surfaceBorder = Color(0x2EFFFFFF),
        surfaceRaised = Color(0xFF221E4D),
        accent = Color(0xFFFFB547),
        accentSecondary = Color(0xFFFF8A3D),
        accentGlow = Color(0x66FFB547),
        onAccent = Color(0xFF1A1200),
        textPrimary = Color(0xFFEEE9FF),
        textSecondary = Color(0xFFA7A0CC),
        textTertiary = Color(0xFF6F6896),
        success = Color(0xFF6BDB9A),
        warning = Color(0xFFFFD166),
        error = Color(0xFFFF8A8A),
        userBubble = Color(0xFF2E2870),
        assistantBubble = Color(0xFF17143A),
        depthShadow = Color(0xFF000000),
        rimLight = Color(0x33FFFFFF),
        lagoon = Color(0xFF3FE0D0),
        lotus = Color(0xFFFF6FAE),
    )

    val Light = LiaColorScheme(
        background = Color(0xFFF3F0FB),
        backgroundGradientTop = Color(0xFFE4DEF7),
        surface = Color(0xFFFFFFFF),
        surfaceGlass = Color(0xCCFFFFFF),
        surfaceBorder = Color(0x1F15112E),
        surfaceRaised = Color(0xFFFFFFFF),
        accent = Color(0xFFB86A00),
        accentSecondary = Color(0xFFD9731F),
        accentGlow = Color(0x40B86A00),
        onAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF15112E),
        textSecondary = Color(0xFF5A5478),
        textTertiary = Color(0xFF8A85A3),
        success = Color(0xFF1E8E5A),
        warning = Color(0xFFA65F00),
        error = Color(0xFFB3261E),
        userBubble = Color(0xFFE6DFFA),
        assistantBubble = Color(0xFFFFFFFF),
        depthShadow = Color(0x1F15112E),
        rimLight = Color(0xFFFFFFFF),
        lagoon = Color(0xFF1FA89A),
        lotus = Color(0xFFD6457F),
    )
}

object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object LiaShapes {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(percent = 50)

    val material = Shapes(small = small, medium = medium, large = large)
}

object Motion {
    const val FAST_MS = 150
    const val MEDIUM_MS = 300
    const val SLOW_MS = 500
}
