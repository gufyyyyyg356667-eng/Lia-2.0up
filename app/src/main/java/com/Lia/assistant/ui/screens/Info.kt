package com.Lia.assistant.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.theme.LocalReducedMotion
import com.Lia.assistant.ui.theme.Motion
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.data.ConversationStore
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaGlassCard
import com.Lia.assistant.ui.components.LiaSettingsRow
import com.Lia.assistant.ui.components.LiaTopBar
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.voice.VoiceSessionManager
import com.Lia.assistant.access.ScreenControlRow
import com.Lia.assistant.alerts.BatteryGuide
import com.Lia.assistant.voice.VoiceLatencyTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row

private data class PermissionRow(val title: String, val permission: String, val minSdk: Int = 0)

private val permissionRows = listOf(
    PermissionRow("Microphone", Manifest.permission.RECORD_AUDIO),
    PermissionRow("Notifications", Manifest.permission.POST_NOTIFICATIONS, minSdk = 33),
    PermissionRow("Contacts", Manifest.permission.READ_CONTACTS),
    PermissionRow("Calls", Manifest.permission.CALL_PHONE),
    PermissionRow("Location", Manifest.permission.ACCESS_COARSE_LOCATION),
    PermissionRow("Calendar", Manifest.permission.READ_CALENDAR),
)

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    // Bumped on resume so the status rows refresh after the user changes a setting.
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val rows = permissionRows.filter { Build.VERSION.SDK_INT >= it.minSdk }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }

    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        LiaTopBar(title = "Permissions", onBack = onBack)
        rows.forEach { row ->
            val granted = run {
                refresh
                ContextCompat.checkSelfPermission(ctx, row.permission) == PackageManager.PERMISSION_GRANTED
            }
            LiaSettingsRow(
                title = row.title,
                value = if (granted) "Allowed" else "Not allowed",
                icon = { StatusLight(granted) },
                onClick = {
                    if (!granted) launcher.launch(arrayOf(row.permission))
                },
            )
        }
        ScreenControlRow(refreshKey = refresh)
        LiaSettingsRow(
            title = "Keep Lia running",
            subtitle = BatteryGuide.message(android.os.Build.MANUFACTURER),
            onClick = {
                ctx.startActivity(
                    Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
        )
        Spacer(Modifier.height(Spacing.md))
        LiaButton(
            text = "Open app settings",
            style = LiaButtonStyle.SECONDARY,
            onClick = {
                ctx.startActivity(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val scope = rememberCoroutineScope()
    var cleared by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        LiaTopBar(title = "Privacy", onBack = onBack)
        Text(
            "Your Gemini key, chat history and settings are stored only on this phone. " +
                "While voice is on, the microphone is used and audio is sent to Google's Gemini service to get replies. " +
                "Lia never sends a message without you tapping Send.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(Spacing.lg))
        LiaButton(
            text = if (cleared == 0) "Delete all chat history" else "Chat history deleted",
            style = LiaButtonStyle.SECONDARY,
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { ConversationStore.clear(ctx) }
                    cleared++
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = LocalLiaColors.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        LiaTopBar(title = "About", onBack = onBack)
        Text(AssistantBrand.FULL_NAME, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
        Text(AssistantBrand.TAGLINE, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.md))
        LiaGlassCard {
            Text(
                "Version ${versionName(ctx)}",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textPrimary,
            )
        }
    }
}

@Composable
fun DebugScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val active by VoiceSessionManager.isActive.collectAsState()
    val state by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val muted by VoiceSessionManager.muted.collectAsState()
    val error by VoiceSessionManager.lastError.collectAsState()

    val detected by VoiceSessionManager.detectedLanguage.collectAsState()
    val latency by VoiceSessionManager.latency.latest.collectAsState()
    val reconnects by VoiceSessionManager.latency.reconnects.collectAsState()

    val wake by com.Lia.assistant.alerts.WakeStatus.text.collectAsState()
    val lines = listOf(
        "Wake listener" to wake,
        "Session" to if (active) "live" else "off",
        "Detected language" to detected.name,
        "Configured language" to Prefs.language(ctx).name,
        "Reconnects" to reconnects.toString(),
        "First audio (last turn)" to (latency["firstAudioMs"]?.let { "$it ms" } ?: "n/a"),
        "Turn length (last turn)" to (latency["turnMs"]?.let { "$it ms" } ?: "n/a"),
        "State" to state.name,
        "Mic level" to "%.2f".format(amplitude),
        "Muted" to muted.toString(),
        "Personality" to Prefs.personality(ctx).name,
        "Language" to Prefs.language(ctx).name,
        "Orb style" to Prefs.orbStyle(ctx).name,
        "Last error" to (error ?: "none"),
    )

    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        LiaTopBar(title = "Voice debug", onBack = onBack)
        val trace by com.Lia.assistant.action.AgentTrace.lines.collectAsState()
        val logText = buildString {
            appendLine("Lia debug log")
            lines.forEach { (k, v) -> appendLine("$k: $v") }
            appendLine("--- Last screen task ---")
            trace.forEach { appendLine(it) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            LiaButton(
                text = "Copy log",
                style = LiaButtonStyle.SECONDARY,
                onClick = {
                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Lia log", logText))
                },
                modifier = Modifier.weight(1f),
            )
            LiaButton(
                text = "Share log",
                onClick = {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_TEXT, logText)
                    ctx.startActivity(
                        android.content.Intent.createChooser(send, "Share Lia log")
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        LiaGlassCard {
            Column {
                Text("Last screen task", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Spacer(Modifier.height(Spacing.xs))
                (if (trace.isEmpty()) listOf("(no task run yet)") else trace).forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    Spacer(Modifier.height(Spacing.xxs))
                }
                Spacer(Modifier.height(Spacing.md))
                lines.forEach { (k, v) ->
                    Text("$k: $v", style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                    Spacer(Modifier.height(Spacing.xxs))
                }
            }
        }
    }
}

private fun versionName(ctx: android.content.Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "unknown"

/** A small lagoon (allowed) or amber (not allowed) dot that pulses once when its state changes. */
@Composable
private fun StatusLight(granted: Boolean) {
    val c = LocalLiaColors.current
    val reduced = LocalReducedMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(granted) {
        if (!reduced) {
            scale.snapTo(1.6f)
            scale.animateTo(1f, tween(Motion.MEDIUM_MS))
        }
    }
    Box(
        Modifier
            .size(12.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(if (granted) c.lagoon else c.warning, CircleShape)
    )
}
