package com.Lia.assistant.access

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.Lia.assistant.ui.components.LiaSettingsRow

/** Permissions row that shows whether screen control is on and opens its settings page. */
@Composable
fun ScreenControlRow(refreshKey: Int) {
    val ctx = LocalContext.current
    val enabled = remember(refreshKey) { NovaAccessibilityService.isEnabled(ctx) }
    LiaSettingsRow(
        title = "Screen control",
        subtitle = "Enabled: ${if (enabled) "yes" else "no"} · Connected: ${if (NovaAccessibilityService.instance != null) "yes" else "no"}",
        value = if (enabled) "On" else "Off",
        onClick = {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    )
}
