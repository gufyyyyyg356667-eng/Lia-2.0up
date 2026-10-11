package com.Lia.assistant.voice

enum class DetectedLanguage { HINDI, HINGLISH, ENGLISH, UNKNOWN }

object LanguageDetector {
    private val markers = setOf(
        "hai", "hain", "ho", "kya", "kyun", "kaise", "nahi", "haan", "mujhe", "tumhe", "aap",
        "mera", "yaar", "bhai", "accha", "theek", "matlab", "karna", "karo", "raha", "chahiye",
        "batao", "suno", "dekho", "abhi", "phir", "lekin", "bahut", "thoda", "kuch", "sab", "wala"
    )

    fun detect(text: String): DetectedLanguage {
        val t = text.trim()
        if (t.isEmpty()) return DetectedLanguage.UNKNOWN
        if (t.any { it in '\u0900'..'\u097F' }) return DetectedLanguage.HINDI

        val words = t.lowercase().split(Regex("[^a-z']+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return DetectedLanguage.ENGLISH
        val hits = words.count { it in markers }

        if (words.size <= 4) {
            return if (hits >= 1) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
        }
        val ratio = hits.toDouble() / words.size
        return if (hits >= 2 || ratio >= 0.15) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
    }
}
