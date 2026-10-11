package com.Lia.assistant.voice

/** Short in-memory window of the current session. Cleared when the session stops. */
class ConversationMemory(private val maxTurns: Int = 12, private val maxChars: Int = 1600) {

    enum class Role { USER, ASSISTANT }
    data class Turn(val role: Role, val text: String)

    private val turns = ArrayDeque<Turn>()

    @Synchronized
    fun add(role: Role, text: String) {
        if (text.isBlank()) return
        turns.addLast(Turn(role, text.trim()))
        while (turns.size > maxTurns) turns.removeFirst()
    }

    @Synchronized
    fun recap(): String? {
        if (turns.isEmpty()) return null
        val text = turns.joinToString("\n") { if (it.role == Role.USER) "User: ${it.text}" else "You: ${it.text}" }
        return if (text.length > maxChars) text.takeLast(maxChars) else text
    }

    @Synchronized
    fun clear() = turns.clear()
}
