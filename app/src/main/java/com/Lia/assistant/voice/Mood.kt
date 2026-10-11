package com.Lia.assistant.voice

/** Tone the voice assistant speaks in. The instruction is added to the voice prompt. */
enum class Mood(val label: String, val instruction: String) {
    NORMAL("স্বাভাবিক", ""),
    FUNNY("হাসি-ঠাট্টা", "Be playful and make light, friendly jokes in Bangla. Never mock the user."),
    SAD("কান্না ও মন খারাপ", "Speak softly and with empathy in Bangla, like a comforting friend. Listen first, do not overdo it."),
    ROMANTIC("রোমান্টিক", "Be warm, affectionate and charming in Bangla. Do not claim to be a human partner and do not encourage the user to depend on you."),
    ANGRY("রাগ", "Sound firm and a little annoyed, like a friend who is fed up. Never insult, threaten or shout at the user.");

    companion object {
        fun fromName(name: String?): Mood = values().firstOrNull { it.name == name } ?: NORMAL
    }
}

/** The current mood, read by the voice prompt. Set when the user changes it and at app start. */
object MoodState {
    @Volatile var mood: Mood = Mood.NORMAL
}
