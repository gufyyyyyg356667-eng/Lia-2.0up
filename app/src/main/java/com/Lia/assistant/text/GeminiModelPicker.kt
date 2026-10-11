package com.Lia.assistant.text

/** Orders the text models the key can use, best first. Pure, so it is unit-tested. */
object GeminiModelPicker {
    private val plainFlash = Regex("^gemini-[0-9.]+-flash$")
    private val unusable = listOf("live", "image", "tts", "embedding", "aqa", "vision")
    private val unstable = listOf("preview", "exp", "lite", "thinking")

    fun candidates(names: List<String>): List<String> {
        val clean = names.map { it.removePrefix("models/") }.distinct()
        val usable = clean.filter { n -> unusable.none { n.contains(it) } && n.contains("gemini") }
        val plain = usable.filter { plainFlash.matches(it) }
        val stableFlash = usable.filter { it.contains("flash") && unstable.none { w -> it.contains(w) } && it !in plain }
        val otherFlash = usable.filter { it.contains("flash") && it !in plain && it !in stableFlash }
        val rest = usable.filter { it !in plain && it !in stableFlash && it !in otherFlash }
        return plain + stableFlash + otherFlash + rest
    }

    fun pick(names: List<String>): String? = candidates(names).firstOrNull()
}
