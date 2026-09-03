package com.aichat.imessage.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aichat.imessage.data.Storage
import com.aichat.imessage.tools.PremiumVoiceTtsHelper

class NotificationReaderService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val s = Storage(applicationContext).loadSettings()
        if (!s.readNotificationsEnabled) return

        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()

        if (title.isNotBlank() && text.isNotBlank()) {
            val appName = when (sbn.packageName) {
                "com.whatsapp" -> "WhatsApp"
                "org.telegram.messenger" -> "Telegram"
                else -> "Notificación"
            }
            PremiumVoiceTtsHelper.speak("Mensaje de $appName de $title: $text", pitch = s.voicePitch, speed = s.voiceSpeed)
        }
    }
}
