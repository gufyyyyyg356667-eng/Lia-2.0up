package com.Lia.assistant.access

/** Button names on call screens, per action. Pure, so it is unit-tested. */
object CallRules {
    private val table: Map<String, List<String>> = mapOf(
        "answer" to listOf("answer", "accept", "pick up"),
        "decline" to listOf("decline", "reject"),
        "end" to listOf("end call", "hang up", "end"),
        "speaker" to listOf("speaker", "loudspeaker"),
        "mute" to listOf("mute", "unmute"),
        "hold" to listOf("hold"),
    )

    fun labels(action: String): List<String>? = table[action.trim().lowercase()]

    val actions: Set<String> get() = table.keys

    /** Shutter and camera-flip button names used by take_photo. */
    val shutterLabels = listOf("shutter", "take photo", "take picture", "capture", "photo")
    val flipLabels = listOf("switch camera", "flip camera", "front camera", "selfie", "switch to front", "rotate camera")
}
