package com.Lia.assistant.alerts

/** Composes the morning briefing text. Pure, so it is unit-tested. */
object Briefing {
    fun morning(weather: String?, eventTitles: List<String>): String {
        val w = weather ?: "আবহাওয়ার তথ্য পাওয়া যায়নি।"
        val events = if (eventTitles.isEmpty()) "আজ কোনো ক্যালেন্ডার ইভেন্ট নেই।"
        else "আজকের ইভেন্ট: ${eventTitles.take(5).joinToString(", ")}।"
        return "সুপ্রভাত! $w $events"
    }
}
