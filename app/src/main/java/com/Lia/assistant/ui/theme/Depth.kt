package com.Lia.assistant.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.State
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Space a screen should leave at the bottom so content can scroll under the floating nav bar. */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Indigo gradient with a sparse, fixed star field. Stars are static, so nothing animates. */
fun Modifier.nightSky(): Modifier = composed {
    val c = LocalLiaColors.current
    val stars = remember {
        val rnd = Random(7)
        List(90) { Star(rnd.nextFloat(), rnd.nextFloat(), 0.5f + rnd.nextFloat() * 1.2f, 0.12f + rnd.nextFloat() * 0.35f) }
    }
    this.drawWithContent {
        drawRect(Brush.verticalGradient(listOf(c.backgroundGradientTop, c.background)))
        stars.forEach { s ->
            drawCircle(
                color = c.textPrimary.copy(alpha = s.alpha),
                radius = s.radius.dp.toPx(),
                center = Offset(s.x * size.width, s.y * size.height),
            )
        }
        drawContent()
    }
}

/** Coloured ambient shadow plus a 1 px light rim along the top edge. */
fun Modifier.depthSurface(shape: Shape, elevation: Dp = 8.dp): Modifier = composed {
    val c = LocalLiaColors.current
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = c.depthShadow,
        spotColor = c.accentGlow,
    )
        .clip(shape)
        .drawWithContent {
            drawContent()
            drawLine(
                color = c.rimLight,
                start = Offset(size.width * 0.1f, 0.5f),
                end = Offset(size.width * 0.9f, 0.5f),
                strokeWidth = 1.dp.toPx(),
            )
        }
}


/** Shifts a layer by the device tilt. [depthDp] is the largest shift; deeper layers use a bigger value less often. */
fun Modifier.parallax(tilt: State<Offset>, depthDp: Float): Modifier = graphicsLayer {
    translationX = tilt.value.x * depthDp.dp.toPx()
    translationY = tilt.value.y * depthDp.dp.toPx() * 0.5f
}
