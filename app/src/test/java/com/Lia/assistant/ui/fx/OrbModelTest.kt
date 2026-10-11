package com.Lia.assistant.ui.fx

import com.Lia.assistant.voice.VoiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbModelTest {
    @Test fun everyOrbStateHasAPalette() {
        for (s in OrbState.entries) assertNotNull(paletteFor(s))
    }

    @Test fun onlyListeningBreathes() {
        assertTrue(paletteFor(OrbState.LISTENING).pulse > 0f)
        assertEquals(0f, paletteFor(OrbState.SPEAKING).pulse, 0f)
    }

    @Test fun voiceStatesMapToOrbStates() {
        assertEquals(OrbState.LISTENING, orbStateFor(VoiceState.LISTENING))
        assertEquals(OrbState.ERROR, orbStateFor(VoiceState.ERROR))
        assertEquals(OrbState.IDLE, orbStateFor(VoiceState.IDLE))
    }

    @Test fun edgeGlowHiddenWhenIdle() {
        assertNull(edgeGlowArgb(VoiceState.IDLE))
        assertNotNull(edgeGlowArgb(VoiceState.LISTENING))
        assertEquals(0xFFFF6FAEL, edgeGlowArgb(VoiceState.SPEAKING))
    }

    @Test fun unknownStyleFallsBackToNova() {
        assertEquals(NovaOrbStyle.NOVA, NovaOrbStyle.fromName("nope"))
        assertEquals(NovaOrbStyle.ARCHER, NovaOrbStyle.fromName("ARCHER"))
    }

    @Test fun argbSplitsIntoUnitFloats() {
        val rgba = argbToRgba(0xFFFF8000L)
        assertEquals(1f, rgba[0], 0.001f)
        assertEquals(0.5f, rgba[1], 0.01f)
        assertEquals(0f, rgba[2], 0.001f)
        assertEquals(1f, rgba[3], 0.001f)
    }
}
