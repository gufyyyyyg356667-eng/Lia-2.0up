package com.Lia.assistant.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoGuardTest {
    @Test fun mutedWhilePlayingAndForTail() {
        var t = 0L
        val guard = EchoGuard { t }
        guard.notifyPlaybackStarted()
        assertTrue(guard.isMicMuted())
        guard.notifyPlaybackStopped()
        t = 100
        assertTrue("still muted during tail", guard.isMicMuted())
        t = 500
        assertFalse("unmuted after tail", guard.isMicMuted())
    }

    @Test fun nestedPlaybackStaysMutedUntilAllStop() {
        var t = 0L
        val guard = EchoGuard { t }
        guard.notifyPlaybackStarted()
        guard.notifyPlaybackStarted()
        guard.notifyPlaybackStopped()
        t = 10_000
        assertTrue(guard.isMicMuted())
    }
}
