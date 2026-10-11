package com.Lia.assistant.ui.components

import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.Lia.assistant.ui.fx.edgeGlowArgb
import com.Lia.assistant.voice.VoiceSessionManager

/**
 * A slim glow along all four screen edges. Draws nothing about where it is hosted.
 * [color] null hides it.
 */
@Composable
fun ListeningEdgeGlow(color: Color?, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (color != null) 1f else 0f,
        animationSpec = tween(300),
        label = "edgeGlowAlpha",
    )
    val shown = color ?: Color.Transparent
    Canvas(modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        val band = 26.dp.toPx()
        val faded = shown.copy(alpha = 0f)
        val strong = shown.copy(alpha = 0.7f)
        drawRect(
            brush = Brush.verticalGradient(listOf(strong, faded), startY = 0f, endY = band),
            size = Size(size.width, band),
        )
        drawRect(
            brush = Brush.verticalGradient(listOf(faded, strong), startY = size.height - band, endY = size.height),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - band),
            size = Size(size.width, band),
        )
        drawRect(
            brush = Brush.horizontalGradient(listOf(strong, faded), startX = 0f, endX = band),
            size = Size(band, size.height),
        )
        drawRect(
            brush = Brush.horizontalGradient(listOf(faded, strong), startX = size.width - band, endX = size.width),
            topLeft = androidx.compose.ui.geometry.Offset(size.width - band, 0f),
            size = Size(band, size.height),
        )
    }
}

/**
 * Shows the edge glow above every screen while a voice session is live.
 * The dialog window is transparent and does not take touches.
 */
@Composable
fun EdgeGlowHost() {
    val active by VoiceSessionManager.isActive.collectAsState()
    val state by VoiceSessionManager.state.collectAsState()
    if (!active) return

    val color = edgeGlowArgb(state)?.let { Color(it) }
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let { w ->
                w.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
        ListeningEdgeGlow(color)
    }
}
