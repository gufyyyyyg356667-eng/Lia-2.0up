package com.Lia.assistant.ui.fx

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.theme.LocalReducedMotion
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Canvas orb with seven personality styles. Used on Android below 13 and for non-Nova styles. */
@Composable
fun NovaOrb(
    state: OrbState,
    amplitude: Float,
    diameter: Dp,
    style: NovaOrbStyle,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "novaOrb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "breath",
    )
    val spinMs = when (state) {
        OrbState.THINKING -> 2600
        OrbState.CONNECTING -> 1100
        else -> 9000
    }
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(spinMs, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )

    val t = if (reduced) 0f else phase
    val spinDeg = if (reduced) 0f else spin
    val palette = paletteFor(state)
    val level = amplitude.coerceIn(0f, 1f)
    val label = "${style.displayName} status: ${state.name.lowercase()}"

    Canvas(modifier.size(diameter).semantics { contentDescription = label }) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val core = Color(palette.core)
        val rim = Color(palette.rim)
        val halo = Color(palette.halo)

        val breath = 1f + 0.035f * sin(2f * PI_F * t)
        val stateScale = when (state) {
            OrbState.LISTENING -> 1.12f + level * 0.28f
            OrbState.SPEAKING -> 1.05f + level * 0.35f
            else -> 1f
        }
        val r = min(w, h) / 2f * 0.6f * breath * stateScale

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(listOf(halo.copy(alpha = 0.45f), Color.Transparent), center, r * 1.9f),
            radius = r * 1.9f,
            center = center,
        )

        // Listening rings
        if (state == OrbState.LISTENING) {
            for (k in 0..1) {
                val p = (t + k * 0.5f) % 1f
                drawCircle(
                    color = core.copy(alpha = (1f - p) * 0.5f),
                    radius = r * (1f + p * 0.6f),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // Connecting arc
        if (state == OrbState.CONNECTING) {
            drawArc(
                color = rim,
                startAngle = spinDeg,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - r * 1.25f, center.y - r * 1.25f),
                size = Size(r * 2.5f, r * 2.5f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Orbiting particles
        val particles = when {
            state == OrbState.ERROR || style == NovaOrbStyle.MINIMAL -> 0
            style == NovaOrbStyle.ENERGY -> 8
            style == NovaOrbStyle.PLASMA -> 6
            style == NovaOrbStyle.GLASS -> 3
            else -> 5
        }
        for (i in 0 until particles) {
            val a = (spinDeg + i * 360f / particles) * DEG
            drawCircle(
                color = rim.copy(alpha = 0.85f),
                radius = 2.5f.dp.toPx(),
                center = Offset(center.x + cos(a) * r * 1.35f, center.y + sin(a) * r * 1.35f),
            )
        }

        if (style == NovaOrbStyle.ARCHER) {
            drawArcherSphere(center, r, core, level, t, spinDeg)
        } else {
            drawCircle(
                brush = Brush.radialGradient(listOf(rim.copy(alpha = 0.9f), core, halo.copy(alpha = 0f)), center, r),
                radius = r,
                center = center,
            )
        }

        if (style == NovaOrbStyle.GLASS) {
            drawArc(
                color = Color.White.copy(alpha = 0.55f),
                startAngle = 200f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - r * 0.9f, center.y - r * 0.9f),
                size = Size(r * 1.8f, r * 1.8f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        if (state == OrbState.ERROR) {
            drawCircle(
                color = rim.copy(alpha = 0.5f),
                radius = r * 1.15f,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

/** Points on a golden-spiral sphere, depth-scaled, slowly turning. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcherSphere(
    center: Offset,
    r: Float,
    color: Color,
    level: Float,
    t: Float,
    spinDeg: Float,
) {
    val n = 400
    val golden = 2.39996f
    val yaw = spinDeg * DEG * 0.5f
    val cy = cos(yaw)
    val sy = sin(yaw)
    for (i in 0 until n) {
        val y = 1f - 2f * (i + 0.5f) / n
        val ring = sqrt(1f - y * y)
        val theta = i * golden
        val x = cos(theta) * ring
        val z = sin(theta) * ring
        val xr = x * cy + z * sy
        val zr = -x * sy + z * cy
        val depth = (zr + 1f) / 2f
        val jitter = 1f + level * 0.08f * sin(i * 1.7f + 2f * PI_F * t)
        drawCircle(
            color = color.copy(alpha = 0.25f + 0.75f * depth),
            radius = (1f + 1.5f * depth).dp.toPx(),
            center = Offset(center.x + xr * r * jitter, center.y + y * r * jitter),
        )
    }
}

private const val PI_F = 3.1415927f
private const val DEG = PI_F / 180f
