package com.Lia.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StoredMessage(val isUser: Boolean, val text: String)

data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<StoredMessage>,
)

/**
 * Typed chat history in a private file. Keeps the newest [MAX] conversations.
 * Never logs content. Call from a background dispatcher.
 */
object ConversationStore {
    private const val FILE = "conversations.json"
    private const val MAX = 50
    private val lock = Any()

    private fun file(ctx: Context) = File(ctx.applicationContext.filesDir, FILE)

    fun load(ctx: Context): List<Conversation> = synchronized(lock) {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val msgs = o.getJSONArray("messages")
                Conversation(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    updatedAt = o.getLong("updatedAt"),
                    messages = (0 until msgs.length()).map { j ->
                        val m = msgs.getJSONObject(j)
                        StoredMessage(m.getBoolean("isUser"), m.getString("text"))
                    },
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun get(ctx: Context, id: String): Conversation? = load(ctx).firstOrNull { it.id == id }

    fun upsert(ctx: Context, convo: Conversation) = synchronized(lock) {
        val list = load(ctx).filterNot { it.id == convo.id }.toMutableList()
        list.add(0, convo)
        save(ctx, list.sortedByDescending { it.updatedAt }.take(MAX))
    }

    fun delete(ctx: Context, id: String) = synchronized(lock) {
        save(ctx, load(ctx).filterNot { it.id == id })
    }

    fun clear(ctx: Context) = synchronized(lock) {
        file(ctx).delete()
    }

    private fun save(ctx: Context, list: List<Conversation>) {
        val arr = JSONArray()
        list.forEach { c ->
            val msgs = JSONArray()
            c.messages.forEach { m ->
                msgs.put(JSONObject().put("isUser", m.isUser).put("text", m.text))
            }
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("title", c.title)
                    .put("updatedAt", c.updatedAt)
                    .put("messages", msgs)
            )
        }
        val target = file(ctx)
        val tmp = File(target.parentFile, "$FILE.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(target)) {
            target.writeText(arr.toString())
            tmp.delete()
        }
    }
}
