package com.aichat.imessage.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

private const val PREFS_NAME = "aichat_prefs"
private const val SETTINGS_KEY = "aichat_settings"

/**
 * Guarda la configuración (clave de API, modelo, tema) en SharedPreferences.
 * El historial de chats, mensajes y adjuntos vive en la base de datos local
 * SQLite (ver data/local/ChatRepository.kt).
 */
class Storage(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        val raw = prefs.getString(SETTINGS_KEY, null) ?: return AppSettings()
        return try {
            val o = JSONObject(raw)
            AppSettings(
                apiKey = o.optString("apiKey", ""),
                model = o.optString("model", "openai/gpt-4o-mini"),
                theme = when (o.optString("theme", "system")) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
                voicePitch = o.optDouble("voicePitch", 1.15).toFloat(),
                voiceSpeed = o.optDouble("voiceSpeed", 0.98).toFloat(),
                youtubeApiKey = o.optString("youtubeApiKey", ""),
                googleApiKey = o.optString("googleApiKey", ""),
                googleCseId = o.optString("googleCseId", "")
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(s: AppSettings) {
        val o = JSONObject()
        o.put("apiKey", s.apiKey)
        o.put("model", s.model)
        o.put(
            "theme",
            when (s.theme) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        )
        o.put("voicePitch", s.voicePitch.toDouble())
        o.put("voiceSpeed", s.voiceSpeed.toDouble())
        o.put("youtubeApiKey", s.youtubeApiKey)
        o.put("googleApiKey", s.googleApiKey)
        o.put("googleCseId", s.googleCseId)
        prefs.edit { putString(SETTINGS_KEY, o.toString()) }
    }
}
