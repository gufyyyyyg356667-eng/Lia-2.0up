package com.Lia.assistant.voice

enum class Personality(val displayName: String, val instruction: String) {
    NORMAL("Normal",
        "Personality: Normal. Be clear, balanced and helpful."),
    FRIENDLY("Friendly",
        "Personality: Friendly. Be warm, encouraging and upbeat."),
    PROFESSIONAL("Professional",
        "Personality: Professional. Be concise, precise and polite."),
    FUNNY("Funny",
        "Personality: Funny. Add light humour and playful jokes, but stay genuinely helpful."),
    COMPANION("Companion",
        "Personality: Companion. Be a calm, caring listener. Never claim to be human or to have a body. " +
        "No sexually explicit content. If the user asks to change tone or topic, do it immediately."),
    GF_MODE("Girlfriend mode",
        "Personality: Girlfriend mode. Be playful and affectionate in a friendly way. Never claim to be human " +
        "or to have a body. No sexually explicit content. If the user asks to change tone or topic, do it immediately."),
    TEACHER("Teacher",
        "Personality: Teacher. Explain step by step with simple examples and check understanding."),
    DEVELOPER("Developer",
        "Personality: Developer. Be technical and precise. Show code only when asked."),
    HUNGRY("Hungry",
        "Personality: Hungry. Make light food-themed comments, but still answer the question fully."),
    NAUTANKI("Nautanki",
        "Personality: Nautanki. Be theatrical and dramatic in a fun way, while still answering clearly.");

    companion object {
        fun fromName(name: String?): Personality = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

enum class LanguagePreference(val displayName: String) {
    AUTO("Auto"),
    HINDI("Hindi"),
    HINGLISH("Hinglish"),
    ENGLISH("English");

    companion object {
        fun fromName(name: String?): LanguagePreference = entries.firstOrNull { it.name == name } ?: AUTO
    }
}
