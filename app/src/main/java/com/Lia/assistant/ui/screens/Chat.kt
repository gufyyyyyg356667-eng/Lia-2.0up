package com.Lia.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import com.Lia.assistant.data.Conversation
import com.Lia.assistant.data.ConversationStore
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.data.StoredMessage
import com.Lia.assistant.text.GeminiTextClient
import com.Lia.assistant.ui.components.ChatMessage
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaEmptyState
import com.Lia.assistant.ui.components.LiaMessageBubble
import com.Lia.assistant.ui.components.LiaTextField
import com.Lia.assistant.ui.components.LiaTopBar
import com.Lia.assistant.ui.theme.LocalBottomBarInset
import com.Lia.assistant.ui.theme.Spacing
import com.Lia.assistant.ui.theme.LiaShapes
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.depthSurface
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.fx.OrbState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.Lia.assistant.voice.PromptBuilder
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.forge.ForgeIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Typed chat. [conversationId] null starts a new conversation; otherwise an earlier one is reopened. */
@Composable
fun ChatScreen(conversationId: String?, onBack: (() -> Unit)?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val chatId = remember(conversationId) { conversationId ?: UUID.randomUUID().toString() }
    val messages = remember(chatId) { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        if (conversationId != null && messages.isEmpty()) {
            val saved = withContext(Dispatchers.IO) { ConversationStore.get(ctx, conversationId) }
            saved?.messages?.forEachIndexed { i, m ->
                messages += ChatMessage(id = "$chatId-$i", text = m.text, isUser = m.isUser)
            }
        }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun persist() {
        val stored = messages.filter { !it.isStreaming && !it.isError }.map { StoredMessage(it.isUser, it.text) }
        if (stored.isEmpty()) return
        val convo = Conversation(
            id = chatId,
            title = stored.firstOrNull { it.isUser }?.text?.take(40) ?: "Chat",
            updatedAt = System.currentTimeMillis(),
            messages = stored,
        )
        scope.launch(Dispatchers.IO) { ConversationStore.upsert(ctx, convo) }
    }

    Column(Modifier.fillMaxSize().padding(Spacing.md).padding(bottom = LocalBottomBarInset.current)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Chat", style = MaterialTheme.typography.headlineMedium, color = LocalLiaColors.current.textPrimary)
            Spacer(Modifier.weight(1f))
            // Small live orb in the header. It breathes while Lia is replying.
            LiaOrb(state = if (busy) OrbState.THINKING else OrbState.IDLE, amplitude = 0f, diameter = 40.dp)
        }

        if (messages.isEmpty()) {
            LiaEmptyState(
                title = "Here to help",
                body = "Type anything. ${Prefs.assistantName(ctx)} answers in the language you use.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                items(messages, key = { it.id }) { m -> LiaMessageBubble(m) }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs)
                .depthSurface(LiaShapes.pill, elevation = 8.dp)
                .background(LocalLiaColors.current.surfaceGlass, LiaShapes.pill)
                .padding(Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            LiaTextField(
                value = input,
                onValueChange = { input = it },
                label = "Message",
                modifier = Modifier.weight(1f),
            )
            LiaButton(
                text = "Send",
                enabled = !busy && input.isNotBlank(),
                onClick = {
                    val text = input.trim()
                    input = ""
                    val base = System.nanoTime().toString()
                    messages += ChatMessage(id = base, text = text, isUser = true)
                    if (ForgeIntent.matches(text)) {
                        val started = ForgeController.start(ctx, text)
                        messages += ChatMessage(id = "$base-forge", text = started.message, isUser = false, isError = started !is ForgeController.StartResult.Started)
                        if (started is ForgeController.StartResult.Started) ForgeController.requestOpen()
                        return@LiaButton
                    }
                    val placeholder = messages.size
                    messages += ChatMessage(id = "$base-reply", text = "", isUser = false, isStreaming = true)
                    busy = true
                    scope.launch {
                        val history = messages.dropLast(1).map { GeminiTextClient.ChatTurn(it.isUser, it.text) }
                        val system = PromptBuilder.buildChat(
                            Prefs.personality(ctx),
                            Prefs.language(ctx),
                            Prefs.assistantName(ctx),
                        )
                        val reply = when (val r = GeminiTextClient.reply(Prefs.getApiKey(ctx), system, history)) {
                            is GeminiTextClient.Reply.Success -> ChatMessage("$base-reply", r.text, isUser = false)
                            is GeminiTextClient.Reply.Error ->
                                ChatMessage("$base-reply", r.message, isUser = false, isError = true)
                        }
                        messages[placeholder] = reply
                        busy = false
                        persist()
                    }
                },
            )
        }
    }
}
