package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceLatencyTrackerTest {

    @Test fun inOrderMarksGiveTheRightDeltas() {
        var t = 1_000L
        val tracker = VoiceLatencyTracker { t }
        tracker.mark(VoiceLatencyTracker.Mark.SPEECH_SEEN)
        t = 1_400L
        tracker.mark(VoiceLatencyTracker.Mark.FIRST_AUDIO)
        t = 2_500L
        tracker.mark(VoiceLatencyTracker.Mark.TURN_DONE)
        tracker.finishTurn()
        assertEquals(400L, tracker.latest.value["firstAudioMs"])
        assertEquals(1_500L, tracker.latest.value["turnMs"])
    }

    @Test fun outOfOrderAndDuplicateMarksAreIgnored() {
        var t = 0L
        val tracker = VoiceLatencyTracker { t }
        t = 100; tracker.mark(VoiceLatencyTracker.Mark.FIRST_AUDIO) // arrives before speech: kept as first
        t = 200; tracker.mark(VoiceLatencyTracker.Mark.SPEECH_SEEN) // out of order: ignored
        t = 300; tracker.mark(VoiceLatencyTracker.Mark.FIRST_AUDIO) // duplicate: ignored
        tracker.finishTurn()
        assertNull(tracker.latest.value["firstAudioMs"])
    }

    @Test fun finishingAClearsTheTurn() {
        var t = 0L
        val tracker = VoiceLatencyTracker { t }
        t = 10; tracker.mark(VoiceLatencyTracker.Mark.SPEECH_SEEN)
        t = 20; tracker.mark(VoiceLatencyTracker.Mark.TURN_DONE)
        tracker.finishTurn()
        tracker.finishTurn()
        assertEquals(emptyMap<String, Long>(), tracker.latest.value)
    }

    @Test fun reconnectsAreCounted() {
        val tracker = VoiceLatencyTracker()
        tracker.onReconnect()
        tracker.onReconnect()
        assertEquals(2, tracker.reconnects.value)
    }
}
