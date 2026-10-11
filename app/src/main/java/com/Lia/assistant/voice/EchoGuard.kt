package com.Lia.assistant.voice

/**
 * While Lia is speaking through the loudspeaker, the mic is muted so Lia does not hear itself.
 * Unmutes a short tail after playback stops. A watchdog force-unmutes if playback never reports stopped.
 */
class EchoGuard(private val now: () -> Long = { System.currentTimeMillis() }) {

    companion object {
        const val TAIL_MS = 450L
        const val WATCHDOG_MS = 60_000L
    }

    private var active = 0
    private var startedAt = 0L
    private var unmuteAt = 0L

    @Synchronized
    fun notifyPlaybackStarted() {
        if (active == 0) startedAt = now()
        active++
    }

    @Synchronized
    fun notifyPlaybackStopped() {
        if (active > 0) active--
        if (active == 0) unmuteAt = now() + TAIL_MS
    }

    @Synchronized
    fun isMicMuted(): Boolean {
        if (active > 0 && now() - startedAt > WATCHDOG_MS) {
            active = 0
            unmuteAt = 0
        }
        return active > 0 || now() < unmuteAt
    }

    @Synchronized
    fun reset() {
        active = 0
        startedAt = 0
        unmuteAt = 0
    }
}
