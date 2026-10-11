package com.Lia.assistant.ui.fx

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Motion
import com.Lia.assistant.voice.VoiceState

/**
 * Layer 1 of the 3D stage: a soft light field behind the content. Its colour follows the session
 * state (lagoon while listening, marigold while speaking) and eases between them.
 */
@Composable
fun AmbientGlow(state: VoiceState, modifier: Modifier = Modifier) {
    val c = LocalLiaColors.current
    val target = when (state) {
        VoiceState.LISTENING -> c.lagoon
        VoiceState.SPEAKING -> c.accent
        VoiceState.ERROR -> c.error
        VoiceState.CONNECTING -> c.accentSecondary
        VoiceState.IDLE -> c.lotus
    }
    val color by animateColorAsState(target, tween(Motion.SLOW_MS), label = "ambientGlow")

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.42f),
                        radius = size.width * 0.9f,
                    ),
                )
            }
    )
}
