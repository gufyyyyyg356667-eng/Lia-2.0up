package com.Lia.assistant.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.license.AccessKeyBanner
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaGlassCard
import com.Lia.assistant.ui.components.LiaSectionHeader
import com.Lia.assistant.ui.fx.AmbientGlow
import com.Lia.assistant.ui.fx.rememberTilt
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.fx.orbStateFor
import com.Lia.assistant.ui.nav.AppSettings
import com.Lia.assistant.ui.nav.Routes
import com.Lia.assistant.ui.theme.LocalBottomBarInset
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.LocalReducedMotion
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.ui.theme.parallax

import com.Lia.assistant.voice.VoiceSessionManager
import java.util.Calendar
import kotlin.math.sin

private val examples = listOf("Lia, YouTube kholo", "Mummy ko call karo", "Rahul ko WhatsApp karo")

/** Home as a 3D stage: glow field behind, orb in front, glass cards and floating buttons on top. */
@Composable
fun HomeScreen(
    onTalk: () -> Unit,
    onType: () -> Unit,
    onProfile: () -> Unit,
    onQuickActions: () -> Unit,
) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val reduced = LocalReducedMotion.current
    val voiceState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val status by VoiceSessionManager.statusText.collectAsState()
    val tilt = rememberTilt(enabled = !reduced)
    val scroll = rememberScrollState()
    AppSettings.prefsVersion

    // Greeting fades in first, before the rest of the stage.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val greetingAlpha by animateFloatAsState(
        targetValue = if (entered || reduced) 1f else 0f,
        animationSpec = tween(if (reduced) 0 else 500),
        label = "greetingFade",
    )

    val assistant = Prefs.assistantName(ctx)
    val user = Prefs.userName(ctx)
    val greeting = greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    val headline = if (user.isBlank()) greeting else "$greeting, $user"

    Box(Modifier.fillMaxSize()) {
        AmbientGlow(voiceState)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .padding(bottom = LocalBottomBarInset.current)
                .verticalScroll(scroll)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(assistant, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                Spacer(Modifier.weight(1f))
                LiaButton(text = "Profile", onClick = onProfile, style = LiaButtonStyle.TEXT)
            }
            Text(
                headline,
                style = MaterialTheme.typography.headlineMedium,
                color = c.textPrimary,
                modifier = Modifier.graphicsLayer { alpha = greetingAlpha },
            )

            Spacer(Modifier.height(Spacing.md))
            AccessKeyBanner(onOpen = onProfile)
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val orbSize = maxWidth * 0.55f
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .parallax(tilt, depthDp = 6f)
                            .clickable(role = Role.Button, onClick = onTalk)
                            .semantics { contentDescription = "Talk to Lia" }
                    ) {
                        LiaOrb(
                            state = orbStateFor(voiceState),
                            amplitude = amplitude,
                            diameter = orbSize,
                            style = Prefs.orbStyle(ctx),
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(status, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                    Text(
                        "Tap me to talk",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = c.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            LiaButton(text = "Talk", onClick = onTalk, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            LiaButton(text = "Type", onClick = onType, style = LiaButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.xs))
            LiaButton(text = "Quick actions", onClick = onQuickActions, style = LiaButtonStyle.TEXT, modifier = Modifier.fillMaxWidth())

            LiaSectionHeader("Try saying")
            examples.forEachIndexed { i, example ->
                // Cards turn slightly as the list scrolls past them.
                LiaGlassCard(
                    Modifier
                        .padding(bottom = Spacing.xs)
                        .graphicsLayer {
                            rotationY = if (reduced) 0f else 6f * sin(scroll.value / 300f + i * 0.8f)
                            cameraDistance = 12f * density
                        }
                ) {
                    Text("\"$example\"", style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                }
            }
        }
    }
}

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hello"
}

@Composable
fun QuickActionsScreen(onOpen: (String) -> Unit) {
    val c = LocalLiaColors.current
    val tiles = listOf(
        Triple("Start voice", "Talk to Lia hands-free", Routes.VOICE),
        Triple("New chat", "Type a message", Routes.chat()),
        Triple("Check permissions", "See what Lia is allowed to use", Routes.PERMISSIONS),
        Triple("Build a website", "Describe it and watch it build", Routes.FORGE),
        Triple("Lia Monitor", "Every command, each step, and what is not working", Routes.MONITOR),
        Triple("Voice debug", "Connection and audio status", Routes.DEBUG),
    )
    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        Text("Quick actions", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
        Spacer(Modifier.height(Spacing.md))
        tiles.forEach { (title, body, route) ->
            LiaGlassCard(Modifier.padding(bottom = Spacing.sm).clickable { onOpen(route) }) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                    Text(body, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }
        }
    }
}
