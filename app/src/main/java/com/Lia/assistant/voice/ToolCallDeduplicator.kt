package com.Lia.assistant.voice

/** Remembers recent tool call ids so a repeated call from the server is run only once. */
class ToolCallDeduplicator(private val capacity: Int = 256) {
    private val seen = LinkedHashSet<String>()

    /** Returns true the first time an id is seen, false for repeats. */
    @Synchronized
    fun firstTime(id: String): Boolean {
        if (!seen.add(id)) return false
        if (seen.size > capacity) {
            val oldest = seen.iterator()
            oldest.next()
            oldest.remove()
        }
        return true
    }
}
