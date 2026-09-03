package com.aichat.imessage.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.aichat.imessage.data.Attachment
import com.aichat.imessage.data.ChatMessage
import com.aichat.imessage.data.Conversation
import com.aichat.imessage.data.MessageDisplayMode
import org.json.JSONArray

/**
 * Repositorio sobre la base de datos SQLite local del teléfono. Toda la
 * información del chat (conversaciones, mensajes y adjuntos) vive aquí.
 */
class ChatRepository(context: Context) {
    private val appContext = context.applicationContext
    private val helper = ChatDatabaseHelper(appContext)

    fun loadAll(): MutableList<Conversation> {
        val db = helper.readableDatabase
        val conversations = mutableListOf<Conversation>()
        db.rawQuery(
            "SELECT id, name, avatarColor, updatedAt FROM conversations ORDER BY updatedAt DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                conversations.add(
                    Conversation(
                        id = id,
                        name = c.getString(1),
                        avatarColor = c.getString(2),
                        messages = loadMessages(id),
                        updatedAt = c.getLong(3)
                    )
                )
            }
        }
        return conversations
    }

    private fun loadMessages(conversationId: String): MutableList<ChatMessage> {
        val db = helper.readableDatabase
        val messages = mutableListOf<ChatMessage>()
        db.rawQuery(
            "SELECT id, role, content, time, error, displayMode FROM messages WHERE conversationId = ? ORDER BY time ASC, id ASC",
            arrayOf(conversationId)
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                messages.add(
                    ChatMessage(
                        id = id,
                        role = c.getString(1),
                        content = c.getString(2),
                        time = c.getLong(3),
                        error = c.getInt(4) != 0,
                        displayMode = runCatching { MessageDisplayMode.valueOf(c.getString(5)) }
                            .getOrDefault(MessageDisplayMode.AUTO),
                        attachments = loadAttachments(id)
                    )
                )
            }
        }
        return messages
    }

    private fun loadAttachments(messageId: Long): List<Attachment> {
        val db = helper.readableDatabase
        val list = mutableListOf<Attachment>()
        db.rawQuery(
            "SELECT id, fileName, localPath, mimeType, sizeBytes FROM attachments WHERE messageId = ?",
            arrayOf(messageId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(Attachment(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4)))
            }
        }
        return list
    }

    fun upsertConversation(conversation: Conversation) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("id", conversation.id)
            put("name", conversation.name)
            put("avatarColor", conversation.avatarColor)
            put("updatedAt", conversation.updatedAt)
        }
        db.insertWithOnConflict("conversations", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteConversation(id: String) {
        val db = helper.writableDatabase
        db.rawQuery("SELECT id FROM messages WHERE conversationId = ?", arrayOf(id)).use { c ->
            while (c.moveToNext()) {
                db.delete("attachments", "messageId = ?", arrayOf(c.getLong(0).toString()))
            }
        }
        db.delete("messages", "conversationId = ?", arrayOf(id))
        db.delete("conversations", "id = ?", arrayOf(id))
    }

    /** Inserta un mensaje (y sus adjuntos) y devuelve una copia con el id real de la base de datos. */
    fun insertMessage(conversationId: String, message: ChatMessage): ChatMessage {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("conversationId", conversationId)
            put("role", message.role)
            put("content", message.content)
            put("time", message.time)
            put("error", if (message.error) 1 else 0)
            put("displayMode", message.displayMode.name)
        }
        val newId = db.insert("messages", null, values)
        val savedAttachments = message.attachments.map { insertAttachment(newId, it) }
        return message.copy(id = newId, attachments = savedAttachments)
    }

    fun insertAttachment(messageId: Long, attachment: Attachment): Attachment {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("messageId", messageId)
            put("fileName", attachment.fileName)
            put("localPath", attachment.localPath)
            put("mimeType", attachment.mimeType)
            put("sizeBytes", attachment.sizeBytes)
        }
        val newId = db.insert("attachments", null, values)
        return attachment.copy(id = newId)
    }

    fun updateMessage(message: ChatMessage) {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("content", message.content)
            put("error", if (message.error) 1 else 0)
            put("displayMode", message.displayMode.name)
        }
        db.update("messages", values, "id = ?", arrayOf(message.id.toString()))
    }

    /** Importa una sola vez las conversaciones del guardado antiguo (SharedPreferences en JSON),
     * si existían, y luego limpia esa clave para no duplicar en el futuro. */
    fun migrateLegacyJsonIfNeeded() {
        val prefs = appContext.getSharedPreferences("aichat_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("aichat_conversations", null) ?: return

        val db = helper.readableDatabase
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM conversations", null)
        countCursor.moveToFirst()
        val alreadyMigrated = countCursor.getInt(0) > 0
        countCursor.close()

        if (!alreadyMigrated) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val conv = Conversation(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        avatarColor = o.optString("avatarColor", "#0B6CFF"),
                        updatedAt = o.optLong("updatedAt")
                    )
                    upsertConversation(conv)
                    val msgsArr = o.optJSONArray("messages") ?: JSONArray()
                    for (j in 0 until msgsArr.length()) {
                        val mo = msgsArr.getJSONObject(j)
                        insertMessage(
                            conv.id,
                            ChatMessage(
                                role = mo.optString("role"),
                                content = mo.optString("content"),
                                time = mo.optLong("time"),
                                error = mo.optBoolean("error", false)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Si la migración falla no rompemos la app; simplemente se pierde el historial viejo.
            }
        }
        prefs.edit().remove("aichat_conversations").apply()
    }
}
