package com.Lia.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Times one conversation turn. Marks must arrive in order; a mark that is not later than the last
 * accepted one is ignored, so duplicates and stray events cannot corrupt the timings.
 */
class VoiceLatencyTracker(private val now: () -> Long = { System.currentTimeMillis() }) {

    enum class Mark { SPEECH_SEEN, FIRST_AUDIO, TURN_DONE }

    private val marks = linkedMapOf<Mark, Long>()

    private val _latest = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** Timings of the last finished turn, in milliseconds. */
    val latest: StateFlow<Map<String, Long>> = _latest

    private val _reconnects = MutableStateFlow(0)
    val reconnects: StateFlow<Int> = _reconnects

    @Synchronized
    fun mark(mark: Mark) {
        val last = marks.keys.maxOfOrNull { it.ordinal } ?: -1
        if (mark.ordinal <= last) return
        marks[mark] = now()
    }

    /** Computes the timings for the turn, publishes them and starts a new turn. */
    @Synchronized
    fun finishTurn() {
        _latest.value = deltas(marks)
        marks.clear()
    }

    fun onReconnect() {
        _reconnects.value = _reconnects.value + 1
    }

    companion object {
        /** Pure timing rules, so they can be tested directly. */
        fun deltas(marks: Map<Mark, Long>): Map<String, Long> {
            val out = linkedMapOf<String, Long>()
            val speech = marks[Mark.SPEECH_SEEN]
            marks[Mark.FIRST_AUDIO]?.let { first -> speech?.let { out["firstAudioMs"] = first - it } }
            marks[Mark.TURN_DONE]?.let { done -> speech?.let { out["turnMs"] = done - it } }
            return out
        }
    }
}
