package com.Lia.assistant.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaTextField
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val name = Prefs.assistantName(ctx)
    val pages = listOf(
        "Hi, I'm $name" to "Your voice assistant, living on your phone.",
        "Talk, don't type" to "Speak in Hindi, Hinglish or English. Interrupt any time.",
        "I can help on the phone" to "I open apps, call people and prepare messages. You always tap Send yourself.",
        "Your key stays here" to "Your Gemini key is stored only on this phone.",
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().padding(Spacing.lg)) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(pages[page].first, style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
                Spacer(Modifier.height(Spacing.md))
                Text(pages[page].second, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = Spacing.md),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .background(if (i == pagerState.currentPage) c.accent else c.surfaceBorder, CircleShape)
                )
            }
        }
        LiaButton(
            text = if (last) "Get started" else "Next",
            onClick = {
                if (last) onDone() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SetupScreen(onSaved: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    var key by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(Spacing.lg), verticalArrangement = Arrangement.Center) {
        Text("Add your key", style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Paste your Gemini API key. It is stored only on this phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))
        LiaTextField(value = key, onValueChange = { key = it }, label = "Gemini API key", isPassword = true)
        Spacer(Modifier.height(Spacing.md))
        LiaButton(
            text = "Save and continue",
            onClick = {
                Prefs.saveApiKey(ctx, key)
                onSaved()
            },
            enabled = key.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
