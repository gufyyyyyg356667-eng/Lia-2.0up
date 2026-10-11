package com.Lia.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.fx.orbStateFor
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.ui.theme.depthSurface
import com.Lia.assistant.voice.VoiceSessionManager
import androidx.compose.runtime.getValue

/** Floating bar: Home, Chat, a centre Talk orb, History, Settings. */
@Composable
fun LiaBottomBar(
    current: String?,
    onNavigate: (String) -> Unit,
    onTalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLiaColors.current
    val voiceState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .depthSurface(LiaShapes.pill)
            .background(c.surfaceGlass, LiaShapes.pill)
            .border(1.dp, c.surfaceBorder, LiaShapes.pill)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarItem("Home", current == "home") { onNavigate("home") }
        BarItem("Chat", current == "chat") { onNavigate("chat") }
        Box(
            modifier = Modifier
                .offset(y = (-10).dp)
                .size(64.dp)
                .semantics { contentDescription = "Talk to Lia" }
                .clickable(role = Role.Button, onClick = onTalk),
            contentAlignment = Alignment.Center,
        ) {
            LiaOrb(
                state = orbStateFor(voiceState),
                amplitude = amplitude,
                diameter = 56.dp,
            )
        }
        BarItem("History", current == "history") { onNavigate("history") }
        BarItem("Settings", current == "settings") { onNavigate("settings") }
    }
}

@Composable
private fun BarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalLiaColors.current
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = Spacing.sm)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) c.accent else c.textSecondary,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Space content needs at the bottom so it can scroll above the floating bar. */
val BottomBarLift = 96.dp
