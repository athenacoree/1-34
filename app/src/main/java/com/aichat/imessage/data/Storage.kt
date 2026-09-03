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
                model = o.optString("model", "google/gemini-2.0-flash-exp:free"),
                theme = when (o.optString("theme", "system")) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
                voicePitch = o.optDouble("voicePitch", 1.15).toFloat(),
                voiceSpeed = o.optDouble("voiceSpeed", 0.98).toFloat(),
                youtubeApiKey = o.optString("youtubeApiKey", ""),
                googleApiKey = o.optString("googleApiKey", ""),
                googleCseId = o.optString("googleCseId", ""),
                nasaApiKey = o.optString("nasaApiKey", ""),
                biometricLockEnabled = o.optBoolean("biometricLockEnabled", false),
                readNotificationsEnabled = o.optBoolean("readNotificationsEnabled", false)
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
        o.put("nasaApiKey", s.nasaApiKey)
        o.put("biometricLockEnabled", s.biometricLockEnabled)
        o.put("readNotificationsEnabled", s.readNotificationsEnabled)
        prefs.edit { putString(SETTINGS_KEY, o.toString()) }
    }

    fun getDailyUsage(): Pair<Int, Int> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val savedDate = prefs.getString("usage_date", "")
        if (savedDate != today) {
            prefs.edit {
                putString("usage_date", today)
                putInt("paid_msgs", 0)
                putInt("paid_tokens", 0)
            }
            return Pair(0, 0)
        }
        return Pair(prefs.getInt("paid_msgs", 0), prefs.getInt("paid_tokens", 0))
    }

    fun addPaidUsage(tokensEst: Int) {
        val (msgs, tokens) = getDailyUsage()
        prefs.edit {
            putInt("paid_msgs", msgs + 1)
            putInt("paid_tokens", tokens + tokensEst)
        }
    }
}
