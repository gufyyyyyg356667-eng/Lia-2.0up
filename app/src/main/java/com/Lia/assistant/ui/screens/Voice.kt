package com.Lia.assistant.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.font.FontStyle
import com.Lia.assistant.ui.components.LiaVoiceRing
import com.Lia.assistant.ui.fx.AmbientGlow
import com.Lia.assistant.ui.fx.orbStateFor
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.depthSurface
import com.Lia.assistant.voice.VoiceState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.voice.VoiceForegroundService
import com.Lia.assistant.voice.VoiceSessionManager

/**
 * Full voice screen. Starts the session if needed. Leaving the screen does NOT stop the session,
 * so the conversation keeps running in the background.
 */
@Composable
fun VoiceScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val active by VoiceSessionManager.isActive.collectAsState()
    val state by VoiceSessionManager.state.collectAsState()
    val status by VoiceSessionManager.statusText.collectAsState()
    val error by VoiceSessionManager.lastError.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val muted by VoiceSessionManager.muted.collectAsState()

    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.RECORD_AUDIO] == true) VoiceForegroundService.start(ctx)
    }

    LaunchedEffect(Unit) {
        if (!VoiceSessionManager.isActive.value) {
            val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (micGranted) VoiceForegroundService.start(ctx) else launcher.launch(permissions)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AmbientGlow(state)
        Column(
            Modifier.fillMaxSize().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val orbSize = maxWidth * 0.7f
                Box(contentAlignment = Alignment.Center) {
                    LiaOrb(
                        state = orbStateFor(state),
                        amplitude = amplitude,
                        diameter = orbSize,
                        style = Prefs.orbStyle(ctx),
                    )
                    if (active && state == VoiceState.SPEAKING) {
                        LiaVoiceRing(amplitude = amplitude, diameter = orbSize * 1.25f)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                status,
                style = MaterialTheme.typography.headlineMedium,
                fontStyle = FontStyle.Italic,
                color = c.textPrimary,
            )
            error?.let {
                Spacer(Modifier.height(Spacing.xs))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = c.error)
            }
            Spacer(Modifier.weight(1f))

            Row(
                Modifier
                    .fillMaxWidth()
                    .depthSurface(LiaShapes.pill, elevation = 10.dp)
                    .background(c.surfaceGlass, LiaShapes.pill)
                    .border(1.dp, c.surfaceBorder, LiaShapes.pill)
                    .padding(Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (active) {
                    LiaButton(
                        text = if (muted) "Unmute" else "Mute",
                        onClick = { VoiceSessionManager.toggleMute() },
                        style = LiaButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                    LiaButton(
                        text = "Stop",
                        onClick = { VoiceForegroundService.stop(ctx) },
                        style = LiaButtonStyle.DANGER,
                        modifier = Modifier.weight(1.2f),
                    )
                } else {
                    LiaButton(
                        text = "Start",
                        onClick = {
                            val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (micGranted) VoiceForegroundService.start(ctx) else launcher.launch(permissions)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                LiaButton(text = "Close", onClick = onClose, style = LiaButtonStyle.TEXT)
            }
        }
    }
}
