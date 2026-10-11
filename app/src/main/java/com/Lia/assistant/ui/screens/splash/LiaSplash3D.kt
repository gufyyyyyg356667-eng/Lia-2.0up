package com.Lia.assistant.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.fx.OrbState
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.LocalReducedMotion
import com.Lia.assistant.ui.theme.nightSky
import kotlinx.coroutines.delay

/** Set when the intro finishes. Only the current process sees it, so it never replays on resume. */
object SplashSession {
    @Volatile var played: Boolean = false
}

/** 3D intro over the loading app. Calls [onFinished] once; the caller hides the splash from then on. */
@Composable
fun LiaSplash3D(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val c = LocalLiaColors.current
    var started by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.6f,
        animationSpec = tween(if (reduced) 0 else 900),
        label = "splashScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(if (reduced) 0 else 600),
        label = "splashAlpha",
    )

    LaunchedEffect(Unit) {
        started = true
        delay(if (reduced) 400L else 1600L)
        SplashSession.played = true
        onFinished()
    }

    Box(modifier.fillMaxSize().nightSky(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
                LiaOrb(state = OrbState.IDLE, amplitude = 0f, diameter = 220.dp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                AssistantBrand.FULL_NAME,
                style = MaterialTheme.typography.displayLarge,
                color = c.textPrimary,
                modifier = Modifier.graphicsLayer { this.alpha = alpha },
            )
        }
    }
}
