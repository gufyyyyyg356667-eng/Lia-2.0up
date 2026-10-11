package com.Lia.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Lia.assistant.action.AgentTrace
import com.Lia.assistant.action.ToolLog
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaGlassCard
import com.Lia.assistant.ui.components.LiaTopBar
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.voice.VoiceSessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One screen that shows every command Lia runs, which step is running now, and what is not working. */
@Composable
fun MonitorScreen(onBack: () -> Unit) {
    val c = LocalLiaColors.current
    val entries by ToolLog.entries.collectAsState()
    val trace by AgentTrace.lines.collectAsState()
    val voice by VoiceSessionManager.state.collectAsState()
    val voiceText by VoiceSessionManager.statusText.collectAsState()
    var onlyFailed by remember { mutableStateOf(false) }
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val running = entries.firstOrNull { it.running }
    val failures = ToolLog.failuresByTool(entries)
    val shown = if (onlyFailed) entries.filter { it.failed } else entries

    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.md, vertical = Spacing.sm).verticalScroll(rememberScrollState())) {
        LiaTopBar(title = "Lia Monitor", onBack = onBack)

        LiaGlassCard(Modifier.fillMaxWidth()) {
            Column {
                Text("Voice: $voice · $voiceText", style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    if (running == null) "Running now: nothing"
                    else "Running now: ${running.name} ${running.args}",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.accent,
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        LiaGlassCard(Modifier.fillMaxWidth()) {
            Column {
                Text("Not working", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                if (failures.isEmpty()) {
                    Text("No failures yet.", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                } else {
                    failures.forEach { (name, count, last) ->
                        Text("$name: failed $count time(s). Last: $last", style = MaterialTheme.typography.bodySmall, color = c.error)
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            LiaButton(
                text = "All",
                style = if (onlyFailed) LiaButtonStyle.SECONDARY else LiaButtonStyle.PRIMARY,
                onClick = { onlyFailed = false },
                modifier = Modifier.weight(1f),
            )
            LiaButton(
                text = "Only failed",
                style = if (onlyFailed) LiaButtonStyle.PRIMARY else LiaButtonStyle.SECONDARY,
                onClick = { onlyFailed = true },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.sm))

        Text("Timeline (newest first)", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        if (shown.isEmpty()) {
            Text("Nothing yet. Give Lia a command.", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        shown.take(120).forEach { e ->
            val status = when {
                e.running -> "running"
                e.failed -> "FAILED"
                else -> "ok"
            }
            LiaGlassCard(Modifier.fillMaxWidth().padding(bottom = Spacing.xs)) {
                Column {
                    Text(
                        "${clock.format(Date(e.at))}  ${e.name}  [$status]  ${if (e.running) "" else "${e.ms} ms"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (e.failed) c.error else c.textPrimary,
                    )
                    Text("args: ${e.args}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    if (!e.running) {
                        Text(
                            "result: ${e.result}${if (e.message.isNotBlank()) " - ${e.message}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text("Agent steps (latest)", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        if (trace.isEmpty()) {
            Text("No screen task yet.", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        trace.takeLast(40).forEach { line ->
            Text(line, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}
