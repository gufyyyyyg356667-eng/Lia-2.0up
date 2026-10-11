package com.Lia.assistant.voice

object PromptBuilder {

    /** System instruction for a live voice session. */
    fun build(
        personality: Personality,
        language: LanguagePreference,
        detected: DetectedLanguage,
        recap: String?,
        assistantName: String,
        toolInstruction: String = "",
    ): String = buildList {
        add("You are $assistantName, a persistent personal voice assistant living on the user's phone. " +
            "Keep replies short and conversational, like spoken conversation.")
        if (toolInstruction.isNotBlank()) add(toolInstruction)
        add(personality.instruction)
        add(languagePolicy(language, detected))
        add(SAFETY)
        if (recap != null) add("This is a continuing conversation — do not greet again. Recap:\n$recap")
    }.joinToString("\n\n")

    /** System instruction for typed chat. */
    fun buildChat(personality: Personality, language: LanguagePreference, assistantName: String): String = listOf(
        "You are $assistantName, a helpful AI assistant in a typed chat. Never mention tapping, listening or speaking.",
        personality.instruction,
        languagePolicy(language, DetectedLanguage.UNKNOWN),
        SAFETY,
    ).joinToString("\n\n")

    private fun languagePolicy(language: LanguagePreference, detected: DetectedLanguage): String = when (language) {
        LanguagePreference.AUTO ->
            "Language: reply in the same language the user just used (Hindi, Hinglish or English), turn by turn. " +
                "Never translate what the user said. Last detected language: ${detected.name}."
        LanguagePreference.HINDI -> "Language: always reply in Hindi."
        LanguagePreference.HINGLISH -> "Language: always reply in Hinglish (Hindi written in Roman script mixed with English)."
        LanguagePreference.ENGLISH -> "Language: always reply in English."
    }

    private const val SAFETY =
        "Safety: never claim to be human. No sexually explicit content. If the user asks to stop a topic, stop. " +
            "Vary your phrasing and do not repeat yourself."
}
