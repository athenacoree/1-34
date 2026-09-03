package com.aichat.imessage.data

/** Cómo se debe pintar un mensaje en pantalla. AUTO detecta bloques de código
 * (```) dentro del texto y los muestra con formato especial automáticamente. */
enum class MessageDisplayMode { AUTO, BUBBLE, PLAIN, CODE }

/** Metadatos de un archivo guardado en el almacenamiento local de la app.
 * El contenido real vive en filesDir/attachments; aquí solo guardamos la ruta. */
data class Attachment(
    val id: Long = 0,
    val fileName: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long
)

data class ChatMessage(
    val id: Long = 0,
    val role: String, // "user" | "assistant"
    val content: String,
    val time: Long,
    val error: Boolean = false,
    val displayMode: MessageDisplayMode = MessageDisplayMode.AUTO,
    val attachments: List<Attachment> = emptyList()
)

data class Conversation(
    val id: String,
    var name: String,
    var avatarColor: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    var updatedAt: Long = System.currentTimeMillis(),
    var pending: Boolean = false
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val apiKey: String = "",
    val model: String = "google/gemini-2.0-flash-exp:free",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val voicePitch: Float = 1.15f,
    val voiceSpeed: Float = 0.98f,
    val youtubeApiKey: String = "",
    val googleApiKey: String = "",
    val googleCseId: String = "",
    val nasaApiKey: String = "",
    val biometricLockEnabled: Boolean = false,
    val readNotificationsEnabled: Boolean = false
)

val AVATAR_PALETTE = listOf("#0B6CFF", "#34C759", "#FF9F0A", "#AF52DE", "#FF3B30", "#5AC8FA", "#FF2D55")
