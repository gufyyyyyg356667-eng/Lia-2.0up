package com.Lia.assistant.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelPickerTest {
    @Test fun prefersPlainStableFlash() {
        val names = listOf("models/gemini-2.5-flash-preview-tts", "models/gemini-2.0-flash-exp", "models/gemini-2.5-flash", "models/gemini-2.0-flash-lite")
        assertEquals("gemini-2.5-flash", GeminiModelPicker.pick(names))
    }

    @Test fun ordersStableBeforeExperimentalAndSkipsUnusable() {
        val names = listOf("models/gemini-live-2.5", "models/gemini-2.0-flash-exp", "models/gemini-2.0-flash", "models/gemini-pro")
        val ordered = GeminiModelPicker.candidates(names)
        assertEquals("gemini-2.0-flash", ordered[0])
        assertTrue(ordered.indexOf("gemini-2.0-flash") < ordered.indexOf("gemini-2.0-flash-exp"))
        assertTrue(ordered.none { it.contains("live") })
    }

    @Test fun fallsBackWhenNoPlainFlash() {
        assertEquals("gemini-pro", GeminiModelPicker.pick(listOf("models/gemini-pro", "models/gemini-pro-vision")))
    }

    @Test fun emptyListGivesNull() = assertNull(GeminiModelPicker.pick(emptyList()))
}
