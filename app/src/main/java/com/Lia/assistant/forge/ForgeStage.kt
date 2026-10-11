package com.Lia.assistant.forge

enum class ForgeStage(val label: String, val rank: Int) {
    READING("Reading your idea", 0),
    STRUCTURE("Laying out structure", 1),
    STYLING("Styling sections", 2),
    VISUALS("Building 3D visuals", 3),
    MOTION("Injecting motion", 4),
    FINALIZING("Finalizing", 5),
    READY("Ready", 6);
}

/** Stages come from what has actually streamed so far, not from timers. Furthest stage wins. */
object ForgeStageDetector {
    private val photo = Regex("https?://[^\"' )]+\\.(jpg|jpeg|png|webp)|picsum\\.photos|pravatar")
    private val container = Regex("<(header|section|footer)\\b", RegexOption.IGNORE_CASE)
    private val sectionName = Regex("<section\\b[^>]*?(?:id|aria-label)\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

    fun detect(html: String): ForgeStage {
        val lower = html.lowercase()
        val body = lower.substringAfter("<body", lower)
        return when {
            lower.contains("</html>") -> ForgeStage.FINALIZING
            lower.contains("gsap.") || lower.contains("scrolltrigger") -> ForgeStage.MOTION
            photo.containsMatchIn(body) || body.contains("<canvas") || body.contains("perspective(") -> ForgeStage.VISUALS
            container.findAll(html).count() >= 2 -> ForgeStage.STYLING
            lower.contains("<body") || lower.contains("<section") -> ForgeStage.STRUCTURE
            else -> ForgeStage.READING
        }
    }

    fun sectionNames(html: String): List<String> =
        sectionName.findAll(html).map { it.groupValues[1].replace('-', ' ').replace('_', ' ').trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

    /** Progress never goes backwards and stays below 98 % until the page is saved. */
    fun progress(stage: ForgeStage, chars: Int): Float =
        maxOf(stage.rank / 6f, chars / 60_000f).coerceAtMost(0.98f)
}
