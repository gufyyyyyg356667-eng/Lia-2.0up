package com.Lia.assistant.action

/** Turns common Bangla app names into the English names apps are installed under. Pure, so it is unit-tested. */
object BanglaNames {
    private val map = listOf(
        "ইউটিউব" to "YouTube",
        "ইউ টিউব" to "YouTube",
        "ইনস্টাগ্রাম" to "Instagram",
        "ফেসবুক" to "Facebook",
        "মেসেঞ্জার" to "Messenger",
        "হোয়াটসঅ্যাপ" to "WhatsApp",
        "হোয়াটস অ্যাপ" to "WhatsApp",
        "টেলিগ্রাম" to "Telegram",
        "গুগল ম্যাপ" to "Google Maps",
        "ম্যাপ" to "Maps",
        "গুগল" to "Google",
        "ক্রোম" to "Chrome",
        "জিমেইল" to "Gmail",
        "ক্যালকুলেটর" to "Calculator",
        "ক্যামেরা" to "Camera",
        "সেটিংস" to "Settings",
        "স্পটিফাই" to "Spotify",
        "ক্যালেন্ডার" to "Calendar",
        "গ্যালারি" to "Gallery",
        "ঘড়ি" to "Clock",
    )

    fun toEnglish(name: String): String {
        val trimmed = name.trim()
        return map.firstOrNull { (bangla, _) -> trimmed.contains(bangla) }?.second ?: trimmed
    }
}
