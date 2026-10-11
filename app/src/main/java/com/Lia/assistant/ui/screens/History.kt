package com.Lia.assistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Lia.assistant.ui.fx.LiaOrb
import com.Lia.assistant.ui.fx.OrbState
import androidx.compose.ui.platform.LocalContext
import com.Lia.assistant.data.Conversation
import com.Lia.assistant.data.ConversationStore
import com.Lia.assistant.ui.components.LiaButton
import com.Lia.assistant.ui.components.LiaButtonStyle
import com.Lia.assistant.ui.components.LiaEmptyState
import com.Lia.assistant.ui.components.LiaGlassCard
import com.Lia.assistant.ui.components.LiaTopBar
import com.Lia.assistant.ui.theme.LocalBottomBarInset
import com.Lia.assistant.ui.theme.LocalLiaColors
import com.Lia.assistant.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(onOpen: (String) -> Unit, onNewChat: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalLiaColors.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        items = withContext(Dispatchers.IO) { ConversationStore.load(ctx) }
        loaded = true
    }

    Column(Modifier.fillMaxSize().padding(Spacing.md)) {
        LiaTopBar(title = "History")
        LiaButton(text = "New chat", onClick = onNewChat, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))

        if (loaded && items.isEmpty()) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LiaOrb(state = OrbState.IDLE, amplitude = 0f, diameter = 120.dp)
                Text("No conversations yet", style = MaterialTheme.typography.headlineMedium, color = LocalLiaColors.current.textPrimary)
                Text("Your typed chats show up here.", style = MaterialTheme.typography.bodyLarge, color = LocalLiaColors.current.textSecondary)
                LiaButton(text = "Start a chat", onClick = onNewChat, modifier = Modifier.fillMaxWidth())
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = LocalBottomBarInset.current + Spacing.md),
            ) {
                items(items, key = { it.id }) { convo ->
                    LiaGlassCard(Modifier.padding(bottom = Spacing.xs).clickable { onOpen(convo.id) }) {
                        Column {
                            Text(convo.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                            Text(
                                "${convo.messages.size} messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                            Row {
                                LiaButton(
                                    text = "Delete",
                                    style = LiaButtonStyle.TEXT,
                                    onClick = {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { ConversationStore.delete(ctx, convo.id) }
                                            items = items.filterNot { it.id == convo.id }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
