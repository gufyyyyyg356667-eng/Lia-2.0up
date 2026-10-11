package com.Lia.assistant.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.LocalReducedMotion
import com.Lia.assistant.ui.theme.Motion
import com.Lia.assistant.ui.theme.Spacing

enum class LiaButtonStyle { PRIMARY, SECONDARY, TEXT, DANGER }

/** Pill button with a press scale of 0.96. Minimum touch target 48 dp. */
@Composable
fun LiaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: LiaButtonStyle = LiaButtonStyle.PRIMARY,
    enabled: Boolean = true,
) {
    val c = LocalLiaColors.current
    val reduced = LocalReducedMotion.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.96f else 1f,
        animationSpec = tween(if (reduced) 0 else Motion.FAST_MS),
        label = "buttonScale",
    )

    val (container, content, borderColor) = when (style) {
        LiaButtonStyle.PRIMARY -> Triple(c.accent, c.onAccent, Color.Transparent)
        LiaButtonStyle.SECONDARY -> Triple(Color.Transparent, c.textPrimary, c.surfaceBorder)
        LiaButtonStyle.TEXT -> Triple(Color.Transparent, c.accent, Color.Transparent)
        LiaButtonStyle.DANGER -> Triple(c.error.copy(alpha = 0.28f), c.textPrimary, c.error.copy(alpha = 0.6f))
    }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) {
        if (pressed && enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .background(container, LiaShapes.pill)
            .border(1.dp, borderColor, LiaShapes.pill)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
) {
    val c = LocalLiaColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = LiaShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.surfaceBorder,
            focusedLabelColor = c.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        ),
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
    )
}

@Composable
fun LiaTopBar(title: String, onBack: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Back", style = MaterialTheme.typography.labelLarge, color = LocalLiaColors.current.accent)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = LocalLiaColors.current.textPrimary)
    }
}

@Composable
fun LiaSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = LocalLiaColors.current.textTertiary,
        modifier = modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
    )
}

/** One settings line. Pass [switchChecked] for a toggle, or [value] for a current-value label. */
@Composable
fun LiaSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    switchChecked: Boolean? = null,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalLiaColors.current
    val toggle: (() -> Unit)? =
        if (switchChecked != null && onSwitchChange != null) ({ onSwitchChange(!switchChecked) }) else null
    val rowClick: (() -> Unit)? = onClick ?: toggle

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (rowClick != null) Modifier.clickable(role = Role.Button, onClick = rowClick) else Modifier)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(Spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyLarge, color = c.accent, modifier = Modifier.padding(start = Spacing.sm))
        }
        if (switchChecked != null) {
            Switch(
                checked = switchChecked,
                onCheckedChange = onSwitchChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.onAccent,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.textSecondary,
                    uncheckedTrackColor = c.surfaceRaised,
                ),
                modifier = Modifier.semantics { contentDescription = title },
            )
        }
    }
}

/** Choose one option from a list, laid out in rows of three. */
@Composable
fun <T> LiaPickerRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLiaColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Spacer(Modifier.height(Spacing.xs))
        options.chunked(3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                rowOptions.forEach { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .background(if (isSelected) c.accent else c.surfaceRaised, LiaShapes.pill)
                            .clickable(role = Role.RadioButton, onClick = { onSelect(option) })
                            .padding(horizontal = Spacing.xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label(option),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) c.onAccent else c.textPrimary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                repeat(3 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
