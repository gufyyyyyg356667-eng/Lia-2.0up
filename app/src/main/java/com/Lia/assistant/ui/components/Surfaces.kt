package com.Lia.assistant.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.LocalReducedMotion
import com.Lia.assistant.ui.theme.Motion
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.ui.theme.depthSurface
import kotlin.math.sin
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size

/** Frosted-looking card with a rim light and depth shadow. */
@Composable
fun LiaGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = LocalLiaColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .depthSurface(LiaShapes.medium)
            .background(c.surfaceGlass, LiaShapes.medium)
            .border(1.dp, c.surfaceBorder, LiaShapes.medium)
            .padding(Spacing.md),
    ) { content() }
}

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
)

@Composable
fun LiaMessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val c = LocalLiaColors.current
    val reduced = LocalReducedMotion.current
    val appear = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) appear.animateTo(1f, tween(Motion.MEDIUM_MS))
    }
    val container = when {
        message.isError -> c.error.copy(alpha = 0.18f)
        message.isUser -> c.accent.copy(alpha = 0.22f)
        else -> c.surfaceGlass
    }
    val who = if (message.isUser) "You" else "Lia"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 12.dp.toPx()
            },
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .semantics { contentDescription = "$who said: ${message.text}" }
                .depthSurface(LiaShapes.medium, elevation = if (message.isUser) 2.dp else 6.dp)
                .background(container, LiaShapes.medium)
                .border(1.dp, c.surfaceBorder, LiaShapes.medium)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            if (message.isStreaming && message.text.isEmpty()) {
                LiaTypingDots()
            } else {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isError) c.error else c.textPrimary,
                )
            }
        }
    }
}

@Composable
fun LiaTypingDots(modifier: Modifier = Modifier) {
    val c = LocalLiaColors.current
    val reduced = LocalReducedMotion.current
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "dotsPhase",
    )
    Canvas(modifier.height(12.dp).widthIn(min = 40.dp)) {
        val gap = 12.dp.toPx()
        for (i in 0 until 3) {
            val lift = if (reduced) 0f else (sin(phase + i * 1.2f) * 3.dp.toPx()).coerceAtLeast(0f)
            drawCircle(
                color = c.textSecondary,
                radius = 3.dp.toPx(),
                center = Offset(i * gap + 4.dp.toPx(), size.height / 2 - lift),
            )
        }
    }
}

@Composable
fun LiaEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    val c = LocalLiaColors.current
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
        Spacer(Modifier.height(Spacing.sm))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
    }
}

/** Bars that move with the voice amplitude (0..1). Still bars when reduce-motion is on. */
@Composable
fun LiaVoiceVisualizer(amplitude: Float, active: Boolean, modifier: Modifier = Modifier) {
    val c = LocalLiaColors.current
    val reduced = LocalReducedMotion.current
    val t by rememberInfiniteTransition(label = "viz").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "vizPhase",
    )
    val phase = if (reduced) 0f else t
    val level = if (active) amplitude.coerceIn(0f, 1f) else 0f

    Canvas(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = if (active) "Lia is listening" else "Voice is off" }
    ) {
        val bars = 24
        val barWidth = 4.dp.toPx()
        val step = size.width / bars
        val midY = size.height / 2
        for (i in 0 until bars) {
            val wave = (sin(phase + i * 0.5f) + 1f) / 2f
            val h = (0.12f + level * (0.4f + 0.5f * wave)).coerceAtMost(1f) * size.height
            val x = step * i + step / 2
            drawLine(
                color = if (active) c.accent else c.textTertiary,
                start = Offset(x, midY - h / 2),
                end = Offset(x, midY + h / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Ring of 48 bars around the orb. Used while Lia is speaking; bar length follows amplitude. */
@Composable
fun LiaVoiceRing(amplitude: Float, diameter: Dp, modifier: Modifier = Modifier) {
    val c = LocalLiaColors.current
    val level = amplitude.coerceIn(0f, 1f)
    Canvas(modifier.size(diameter)) {
        val bars = 48
        val cx = size.width / 2f
        val cy = size.height / 2f
        val inner = size.minDimension / 2f * 0.86f
        val maxLen = size.minDimension / 2f * 0.14f
        for (i in 0 until bars) {
            val a = (i * 2.0 * Math.PI / bars).toFloat()
            val len = 2.dp.toPx() + level * maxLen
            val dx = kotlin.math.cos(a)
            val dy = kotlin.math.sin(a)
            drawLine(
                color = c.accent.copy(alpha = 0.3f + 0.7f * level),
                start = Offset(cx + dx * inner, cy + dy * inner),
                end = Offset(cx + dx * (inner + len), cy + dy * (inner + len)),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
