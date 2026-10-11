package com.Lia.assistant.forge

/** Recognises a typed request to build a website. Questions do not count. */
object ForgeIntent {
    private val subject = Regex("\\b(website|webpage|web page|web site|landing page|vebsite|site banao)\\b")
    private val verb = Regex("\\b(build|create|make|generate|design|bana|banao|banaiye|bana do|bnao|banado)\\b")
    private val question = Regex("^(what|how|why|which|kya|kaise|kyun|kon)\\b")

    fun matches(text: String): Boolean {
        val t = text.trim().lowercase()
        if (t.isEmpty() || t.endsWith("?") || question.containsMatchIn(t)) return false
        return subject.containsMatchIn(t) && verb.containsMatchIn(t)
    }
}
