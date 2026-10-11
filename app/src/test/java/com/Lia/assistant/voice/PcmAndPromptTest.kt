package com.Lia.assistant.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmAndPromptTest {
    @Test fun silenceHasZeroRms() = assertEquals(0f, PcmMath.rms(ByteArray(64)), 0f)

    @Test fun fullScaleIsNearOne() {
        // 32767 little-endian = 0xFF, 0x7F
        val pcm = ByteArray(200) { if (it % 2 == 0) 0xFF.toByte() else 0x7F.toByte() }
        assertTrue(PcmMath.rms(pcm) > 0.99f)
    }

    @Test fun everyPersonalityAndLanguageBuilds() {
        for (p in Personality.entries) for (l in LanguagePreference.entries) {
            val text = PromptBuilder.build(p, l, DetectedLanguage.UNKNOWN, null, "Lia")
            assertTrue(text.contains(p.instruction))
        }
    }

    @Test fun recapAppearsOnlyWhenGiven() {
        val without = PromptBuilder.build(Personality.NORMAL, LanguagePreference.AUTO, DetectedLanguage.UNKNOWN, null, "Lia")
        val with = PromptBuilder.build(Personality.NORMAL, LanguagePreference.AUTO, DetectedLanguage.UNKNOWN, "User: hi", "Lia")
        assertFalse(without.contains("Recap"))
        assertTrue(with.contains("Recap:"))
    }
}
