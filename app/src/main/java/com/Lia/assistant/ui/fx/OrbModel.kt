package com.Lia.assistant.ui.fx

import com.Lia.assistant.voice.VoiceState

/** Visual states of the orb. Maps from the session state in the voice layer. */
enum class OrbState { IDLE, LISTENING, THINKING, SPEAKING, CONNECTING, ERROR }

enum class NovaOrbStyle(val displayName: String) {
    NOVA("Nova"),
    AURORA("Aurora"),
    PLASMA("Plasma"),
    GLASS("Glass"),
    ENERGY("Energy"),
    MINIMAL("Minimal"),
    ARCHER("Archer");

    companion object {
        fun fromName(name: String?): NovaOrbStyle = entries.firstOrNull { it.name == name } ?: NOVA
    }
}

/** ARGB colours (Long) plus pace. [pulse] is the breathing amount for LISTENING. */
data class OrbPalette(val core: Long, val rim: Long, val halo: Long, val speed: Float, val pulse: Float)

fun paletteFor(state: OrbState): OrbPalette = when (state) {
    OrbState.IDLE -> OrbPalette(core = 0xFF7C5CFF, rim = 0xFFFF6FAE, halo = 0xFF8B5CF6, speed = 0.6f, pulse = 0f)
    OrbState.LISTENING -> OrbPalette(core = 0xFF3FE0D0, rim = 0xFFA7F3EE, halo = 0xFF1FA89A, speed = 0.9f, pulse = 0.03f)
    OrbState.THINKING -> OrbPalette(core = 0xFF8B5CF6, rim = 0xFFC4B5FD, halo = 0xFF6D28D9, speed = 2.0f, pulse = 0f)
    OrbState.SPEAKING -> OrbPalette(core = 0xFFFFB547, rim = 0xFFFF6FAE, halo = 0xFFFF8A3D, speed = 1.2f, pulse = 0f)
    OrbState.CONNECTING -> OrbPalette(core = 0xFFFFC36B, rim = 0xFFFFE3A8, halo = 0xFFB86A00, speed = 0.4f, pulse = 0f)
    OrbState.ERROR -> OrbPalette(core = 0xFFB3261E, rim = 0xFFFF7A85, halo = 0xFF7A1E1E, speed = 0.2f, pulse = 0f)
}

fun orbStateFor(state: VoiceState): OrbState = when (state) {
    VoiceState.IDLE -> OrbState.IDLE
    VoiceState.CONNECTING -> OrbState.CONNECTING
    VoiceState.LISTENING -> OrbState.LISTENING
    VoiceState.SPEAKING -> OrbState.SPEAKING
    VoiceState.ERROR -> OrbState.ERROR
}

/** Edge glow colour per session state. Null means no glow (session off). */
fun edgeGlowArgb(state: VoiceState): Long? = when (state) {
    VoiceState.IDLE -> null
    VoiceState.LISTENING, VoiceState.CONNECTING -> 0xFF8B5CF6
    VoiceState.SPEAKING -> 0xFFFF6FAE
    VoiceState.ERROR -> 0xFFFF7A85
}
