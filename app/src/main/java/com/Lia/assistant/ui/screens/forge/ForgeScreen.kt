package com.Lia.assistant.ui.screens.forge

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.width
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.forge.ForgeStage
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaTextField
import com.Lia.assistant.ui.components.LiaTopBar
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.ui.theme.depthSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun shareWebsite(ctx: android.content.Context, fileName: String) {
    val file = File(File(ctx.filesDir, "Websites"), fileName)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.forgefiles", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/html")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, "Share website").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** The Forge: describe a website, watch it build, then view, change or delete it. */
@Composable
fun ForgeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val state by ForgeController.state.collectAsState()
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var change by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var html by remember { mutableStateOf<String?>(null) }

    // Save writes the page to a file the user picks. Nothing is written without that choice.
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        val text = html
        if (uri != null && text != null) {
            scope.launch(Dispatchers.IO) {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        }
    }

    LaunchedEffect(state.phase, state.file) {
        if (state.phase == ForgeController.Phase.DONE && state.file != null) {
            html = withContext(Dispatchers.IO) { ForgeController.readSaved(ctx, state.file!!) }
        }
    }

    Column(Modifier.fillMaxSize().padding(Spacing.md)) {
        LiaTopBar(title = "Forge", onBack = onBack)
        when (state.phase) {
            ForgeController.Phase.IDLE, ForgeController.Phase.FAILED, ForgeController.Phase.CANCELLED -> {
                if (state.phase != ForgeController.Phase.IDLE) {
                    Text(
                        state.error ?: "Stopped.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textSecondary,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                }
                Text("What should the website be?", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
                Spacer(Modifier.height(Spacing.sm))
                LiaTextField(value = prompt, onValueChange = { prompt = it }, label = "Describe it, e.g. a cafe in Jaipur")
                Spacer(Modifier.height(Spacing.sm))
                LiaButton(
                    text = "Build website",
                    enabled = prompt.isNotBlank(),
                    onClick = { message = ForgeController.start(ctx, prompt).message },
                    modifier = Modifier.fillMaxWidth(),
                )
                message?.let {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                }
            }

            ForgeController.Phase.BUILDING -> Building(state)

            ForgeController.Phase.DONE -> {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LiaButton(text = "New", style = LiaButtonStyle.TEXT, onClick = { ForgeController.reset() })
                    Spacer(Modifier.weight(1f))
                    LiaButton(
                        text = "Save",
                        style = LiaButtonStyle.SECONDARY,
                        enabled = html != null,
                        onClick = { saveLauncher.launch(state.file ?: "website.html") },
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    LiaButton(
                        text = "Share",
                        style = LiaButtonStyle.SECONDARY,
                        enabled = state.file != null,
                        onClick = { state.file?.let { shareWebsite(ctx, it) } },
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    LiaButton(
                        text = "Delete",
                        style = LiaButtonStyle.DANGER,
                        onClick = { state.file?.let { ForgeController.delete(ctx, it) } },
                    )
                }
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    LiaTextField(
                        value = change,
                        onValueChange = { change = it },
                        label = "Change it, e.g. make the hero darker",
                        modifier = Modifier.weight(1f),
                    )
                    LiaButton(
                        text = "Change",
                        enabled = change.isNotBlank(),
                        onClick = {
                            message = ForgeController.edit(ctx, change).message
                            change = ""
                        },
                        modifier = Modifier.padding(start = Spacing.xs),
                    )
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
                Spacer(Modifier.height(Spacing.xs))
                html?.let { GeneratedSiteView(it, Modifier.fillMaxSize().weight(1f)) }
            }
        }
    }
}

@Composable
private fun Building(state: ForgeController.UiState) {
    val c = LocalLiaColors.current
    val m = state.metrics
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        // Top: the prompt as a chip.
        Box(
            Modifier
                .fillMaxWidth()
                .depthSurface(LiaShapes.pill, elevation = 6.dp)
                .background(c.surfaceGlass, LiaShapes.pill)
                .border(1.dp, c.surfaceBorder, LiaShapes.pill)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Text(
                if (state.isEdit) "Updating: ${state.prompt}" else "Building: ${state.prompt}",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textPrimary,
            )
        }

        // Bottom: the status card.
        Column(
            Modifier
                .fillMaxWidth()
                .depthSurface(LiaShapes.medium, elevation = 10.dp)
                .background(c.surfaceGlass, LiaShapes.medium)
                .border(1.dp, c.surfaceBorder, LiaShapes.medium)
                .padding(Spacing.md)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(state.stage.label, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(
                "${(state.progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                color = c.accent,
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 6) {
                    val filled = state.progress * 6f > i
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .background(if (filled) c.accent else c.surfaceRaised, LiaShapes.pill)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "${m.chars / 1024} KB · ${m.sections} sections · ${m.tokensPerSec} tok/s · ${m.elapsedSec}s",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(Spacing.md))
            LiaButton(
                text = "Cancel",
                style = LiaButtonStyle.DANGER,
                onClick = { ForgeController.cancel() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
