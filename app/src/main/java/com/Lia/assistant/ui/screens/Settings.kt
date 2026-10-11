package com.Lia.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.depthSurface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import com.Lia.assistant.data.Prefs
import com.Lia.assistant.license.AccessKeySection
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaPickerRow
import com.Lia.assistant.ui.components.LiaSectionHeader
import com.Lia.assistant.ui.components.LiaSettingsRow
import com.Lia.assistant.ui.components.LiaTextField
import com.Lia.assistant.ui.components.LiaTopBar
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.fx.NovaOrb
import com.Lia.assistant.ui.fx.NovaOrbStyle
import com.Lia.assistant.ui.fx.OrbState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import com.Lia.assistant.ui.nav.AppSettings
import com.Lia.assistant.ui.nav.Routes
import com.Lia.assistant.ui.theme.LocalBottomBarInset
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.ThemeMode
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.voice.LanguagePreference
import com.Lia.assistant.voice.Personality
import com.Lia.assistant.voice.VoiceSessionManager

@Composable
fun SettingsScreen(onOpen: (String) -> Unit, onChangeKey: () -> Unit) {
    val ctx = LocalContext.current
    AppSettings.prefsVersion // subscribe to preference changes

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .padding(bottom = LocalBottomBarInset.current)
            .verticalScroll(rememberScrollState())
    ) {
        LiaTopBar(title = "Settings")

        LiaSectionHeader("Voice")
        LiaSettingsRow(
            title = "Personality",
            value = Prefs.personality(ctx).displayName,
            onClick = { onOpen(Routes.PERSONALITY) },
        )
        LiaSettingsRow(
            title = "Orb style",
            value = Prefs.orbStyle(ctx).displayName,
            onClick = { onOpen(Routes.ORB_STYLE) },
        )
        Spacer(Modifier.height(Spacing.sm))
        LiaSettingsRow(
            title = "Listen for my name",
            subtitle = "Lia wakes when you call its name, even with the screen off (direct build)",
            switchChecked = Prefs.wakeOn(ctx),
            onSwitchChange = { wantOn ->
                val started = if (wantOn) com.Lia.assistant.wake.WakeControl.start(ctx) else {
                    com.Lia.assistant.wake.WakeControl.stop(ctx)
                    true
                }
                Prefs.saveWakeOn(ctx, wantOn && started)
                AppSettings.notifyPrefsChanged()
            },
        )
        LiaPickerRow(
            title = "Mood",
            options = com.Lia.assistant.voice.Mood.values().toList(),
            selected = Prefs.mood(ctx),
            label = { it.label },
            onSelect = {
                Prefs.saveMood(ctx, it)
                AppSettings.notifyPrefsChanged()
                VoiceSessionManager.refreshInstructions()
            },
        )
        LiaSettingsRow(
            title = "Phone alerts",
            subtitle = "Lia tells you about charging",
            switchChecked = Prefs.alertsOn(ctx),
            onSwitchChange = {
                Prefs.saveAlertsOn(ctx, it)
                if (it) com.Lia.assistant.alerts.PhoneMonitorService.start(ctx)
                else com.Lia.assistant.alerts.PhoneMonitorService.stop(ctx)
                AppSettings.notifyPrefsChanged()
            },
        )
        LanguageRow()

        LiaSectionHeader("Appearance")
        LiaPickerRow(
            title = "Theme",
            options = ThemeMode.entries,
            selected = AppSettings.themeMode,
            label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            onSelect = { AppSettings.setTheme(ctx, it) },
        )
        Spacer(Modifier.height(Spacing.sm))
        LiaSettingsRow(
            title = "Reduce motion",
            subtitle = "Stops looping and springy animations",
            switchChecked = AppSettings.reducedMotion,
            onSwitchChange = { AppSettings.setReducedMotion(ctx, it) },
        )

        LiaSectionHeader("Account")
        LiaSettingsRow(
            title = "Profile",
            value = Prefs.userName(ctx).ifBlank { null },
            onClick = { onOpen(Routes.PROFILE) },
        )
        LiaSettingsRow(title = "Permissions", onClick = { onOpen(Routes.PERMISSIONS) })
        LiaSettingsRow(title = "Privacy", onClick = { onOpen(Routes.PRIVACY) })
        LiaSettingsRow(title = "About", onClick = { onOpen(Routes.ABOUT) })
        LiaSettingsRow(title = "Voice debug", onClick = { onOpen(Routes.DEBUG) })

        Spacer(Modifier.height(Spacing.lg))
        LiaButton(
            text = "Change API key",
            onClick = {
                Prefs.clearApiKey(ctx)
                onChangeKey()
            },
            style = LiaButtonStyle.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LanguageRow() {
    val ctx = LocalContext.current
    val language = Prefs.language(ctx)
    LiaPickerRow(
        title = "Language",
        options = LanguagePreference.entries,
        selected = language,
        label = { it.displayName },
        onSelect = {
            Prefs.saveLanguage(ctx, it)
            AppSettings.notifyPrefsChanged()
            VoiceSessionManager.refreshInstructions()
        },
    )
}

private fun blurbFor(p: Personality): String = when (p) {
    Personality.NORMAL -> "Clear and balanced"
    Personality.FRIENDLY -> "Warm and upbeat"
    Personality.PROFESSIONAL -> "Short and precise"
    Personality.FUNNY -> "Light jokes, still helpful"
    Personality.COMPANION -> "A calm, caring listener"
    Personality.GF_MODE -> "Playful and affectionate"
    Personality.TEACHER -> "Step by step"
    Personality.DEVELOPER -> "Technical and exact"
    Personality.HUNGRY -> "Food on the mind"
    Personality.NAUTANKI -> "Dramatic storyteller"
}

@Composable
fun PersonalityScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    AppSettings.prefsVersion
    val selected = Prefs.personality(ctx)
    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = Spacing.md).verticalScroll(rememberScrollState())) {
        LiaTopBar(title = "Personality", onBack = onBack)
        Text("Changes take effect immediately", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.md))
        Personality.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { p ->
                    val isSelected = p == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                val lift = if (isSelected) 1.04f else 1f
                                scaleX = lift
                                scaleY = lift
                                alpha = if (isSelected) 1f else 0.6f
                            }
                            .depthSurface(LiaShapes.medium, elevation = if (isSelected) 14.dp else 4.dp)
                            .background(c.surfaceGlass, LiaShapes.medium)
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) c.accent else c.surfaceBorder, LiaShapes.medium)
                            .clickable(role = Role.RadioButton) {
                                Prefs.savePersonality(ctx, p)
                                AppSettings.notifyPrefsChanged()
                                VoiceSessionManager.refreshInstructions()
                            }
                            .padding(Spacing.md)
                            .heightIn(min = 96.dp),
                    ) {
                        Column {
                            Text(p.displayName, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                            Spacer(Modifier.height(Spacing.xxs))
                            Text(blurbFor(p), style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrbStyleScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    AppSettings.prefsVersion
    val styles = NovaOrbStyle.entries
    val current = Prefs.orbStyle(ctx)
    val pager = rememberPagerState(initialPage = styles.indexOf(current), pageCount = { styles.size })
    var previewState by remember { mutableStateOf(OrbState.IDLE) }

    // Saving happens when a swipe settles, so the choice follows the centred orb.
    LaunchedEffect(pager.settledPage) {
        val picked = styles[pager.settledPage]
        if (picked != Prefs.orbStyle(ctx)) {
            Prefs.saveOrbStyle(ctx, picked)
            AppSettings.notifyPrefsChanged()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        LiaTopBar(title = "Orb style", onBack = onBack)

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            LiaOrb(
                state = previewState,
                amplitude = if (previewState == OrbState.SPEAKING) 0.6f else 0.2f,
                diameter = 200.dp,
                style = styles[pager.currentPage],
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOf(OrbState.IDLE, OrbState.LISTENING, OrbState.SPEAKING).forEach { s ->
                LiaButton(
                    text = s.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = if (s == previewState) LiaButtonStyle.PRIMARY else LiaButtonStyle.SECONDARY,
                    onClick = { previewState = s },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))

        HorizontalPager(
            state = pager,
            pageSize = PageSize.Fixed(180.dp),
            contentPadding = PaddingValues(horizontal = 110.dp),
            modifier = Modifier.fillMaxWidth().height(230.dp),
        ) { page ->
            val offset = (page - pager.currentPage - pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
            val centred = 1f - abs(offset)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = -35f * offset
                        cameraDistance = 12f * density
                        val s = 0.8f + 0.2f * centred
                        scaleX = s
                        scaleY = s
                        alpha = 0.5f + 0.5f * centred
                    },
                contentAlignment = Alignment.Center,
            ) {
                NovaOrb(
                    state = OrbState.IDLE,
                    amplitude = 0f,
                    diameter = 140.dp,
                    style = styles[page],
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            styles[pager.currentPage].displayName,
            style = MaterialTheme.typography.titleLarge,
            color = c.textPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            "Swipe to choose. The orb saves when you stop.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var userName by remember { mutableStateOf(Prefs.userName(ctx)) }
    var assistantName by remember { mutableStateOf(Prefs.assistantName(ctx)) }
    var emergency by remember { mutableStateOf(Prefs.emergencyNumber(ctx)) }
    var wakeName by remember { mutableStateOf(Prefs.wakeName(ctx)) }
    var briefingHour by remember { mutableStateOf(Prefs.briefingHour(ctx).toString()) }

    Column(Modifier.fillMaxSize().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        LiaTopBar(title = "Profile", onBack = onBack)
        LiaTextField(value = userName, onValueChange = { userName = it }, label = "Your name")
        Spacer(Modifier.height(Spacing.md))
        LiaTextField(value = assistantName, onValueChange = { assistantName = it }, label = "Assistant name")
        Spacer(Modifier.height(Spacing.md))
        LiaTextField(value = emergency, onValueChange = { emergency = it }, label = "Emergency number")
        Spacer(Modifier.height(Spacing.md))
        LiaTextField(value = wakeName, onValueChange = { wakeName = it }, label = "Wake name (the name you call Lia by)")
        Spacer(Modifier.height(Spacing.md))
        LiaTextField(value = briefingHour, onValueChange = { briefingHour = it }, label = "Morning briefing hour (0-23)")
        Spacer(Modifier.height(Spacing.lg))
        AccessKeySection()
        Spacer(Modifier.height(Spacing.lg))
        LiaButton(
            text = "Save",
            onClick = {
                Prefs.saveUserName(ctx, userName)
                Prefs.saveAssistantName(ctx, assistantName.ifBlank { com.Lia.assistant.data.AssistantBrand.NAME })
                Prefs.saveEmergencyNumber(ctx, emergency)
                Prefs.saveWakeName(ctx, wakeName)
                Prefs.saveBriefingHour(ctx, briefingHour.toIntOrNull() ?: 8)
                AppSettings.notifyPrefsChanged()
                VoiceSessionManager.refreshInstructions()
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
