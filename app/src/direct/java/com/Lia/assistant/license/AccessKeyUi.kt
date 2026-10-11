package com.Lia.assistant.license


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaGlassCard
import com.Lia.assistant.ui.components.LiaSectionHeader
import com.Lia.assistant.ui.components.LiaTextField
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Shown on Home while no active key is present. Hidden once the key is active. */
@Composable
fun AccessKeyBanner(onOpen: () -> Unit) {
    val info by AccessKeyManager.info.collectAsState()
    if (info.active) return
    val c = LocalLiaColors.current
    LiaGlassCard(Modifier.padding(bottom = Spacing.md)) {
        Column {
            Text(info.statusText, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
            Spacer(Modifier.height(Spacing.sm))
            LiaButton(text = "Add access key", onClick = onOpen, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Profile section to enter, replace or remove the access key. */
@Composable
fun AccessKeySection() {
    val ctx = LocalContext.current
    val info by AccessKeyManager.info.collectAsState()
    val scope = rememberCoroutineScope()
    val c = LocalLiaColors.current
    var key by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxWidth()) {
        LiaSectionHeader("Access key")
        if (com.Lia.assistant.BuildConfig.LICENSE_BASE_URL.isBlank()) {
            Text("Personal build: no key server, so no key is needed.", style = MaterialTheme.typography.bodyMedium, color = LocalLiaColors.current.textSecondary)
            return@Column
        }
        Text(info.statusText, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.sm))
        LiaTextField(value = key, onValueChange = { key = it }, label = "Access key", isPassword = true)
        Spacer(Modifier.height(Spacing.sm))
        LiaButton(
            text = "Activate",
            enabled = key.isNotBlank(),
            onClick = {
                scope.launch {
                    message = when (val r = AccessKeyManager.activate(ctx, key)) {
                        is AccessKeyManager.Activation.Ok -> {
                            key = ""
                            "Activated${if (r.plan.isNotBlank()) " (${r.plan})" else ""}."
                        }
                        AccessKeyManager.Activation.Invalid -> "That key is not valid."
                        AccessKeyManager.Activation.Blocked -> "This key was disabled."
                        AccessKeyManager.Activation.Unreachable -> "Couldn't reach the key server. Try again later."
                        AccessKeyManager.Activation.NotConfigured -> "Licensing is not set up in this build."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        message?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
        }
        if (info.hasRememberedKey) {
            Spacer(Modifier.height(Spacing.sm))
            LiaButton(
                text = "Remove key",
                style = LiaButtonStyle.TEXT,
                onClick = { AccessKeyManager.forget(ctx) },
            )
        }
    }
}
