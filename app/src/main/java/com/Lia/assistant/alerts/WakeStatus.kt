package com.Lia.assistant.alerts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the wake-word listener is doing, shown on the Debug screen. */
object WakeStatus {
    private val _text = MutableStateFlow("off")
    val text: StateFlow<String> = _text

    fun set(value: String) {
        _text.value = value
    }
}
