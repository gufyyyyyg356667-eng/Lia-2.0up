package com.Lia.assistant.action

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Step-by-step log of the last screen-agent run, shown on the Debug screen so failures can be read. */
object AgentTrace {
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun start(goal: String) {
        _lines.value = listOf("Goal: $goal")
    }

    fun add(line: String) {
        _lines.value = (_lines.value + line).takeLast(60)
    }
}
