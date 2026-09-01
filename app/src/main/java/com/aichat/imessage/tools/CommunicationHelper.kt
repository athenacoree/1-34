package com.aichat.imessage.tools

import android.content.Context
import android.content.Intent
import android.net.Uri

object CommunicationHelper {

    fun sendSms(context: Context, phoneNumber: String, messageText: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phoneNumber.trim()}")
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendMessageViaApp(context: Context, targetApp: String, contactOrPhone: String, messageText: String): Boolean {
        val appLower = targetApp.lowercase()
        val isWhatsApp = appLower.contains("whatsapp")

        return try {
            if (isWhatsApp) {
                val cleanPhone = contactOrPhone.replace(Regex("[^0-9]"), "")
                val uri = if (cleanPhone.isNotEmpty()) {
                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                } else {
                    Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(messageText)}")
                }
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } else {
                // Compartido genérico para la app destino o elección del usuario
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (targetApp.isNotBlank()) {
                    val pkg = when {
                        appLower.contains("telegram") -> "org.telegram.messenger"
                        appLower.contains("signal") -> "org.thoughtcrime.securesms"
                        appLower.contains("messenger") -> "com.facebook.orca"
                        else -> null
                    }
                    if (pkg != null) {
                        sendIntent.setPackage(pkg)
                    }
                }
                context.startActivity(Intent.createChooser(sendIntent, "Enviar mensaje con...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            }
        } catch (e: Exception) {
            // Intent con fallback si la app no está instalada
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Enviar mensaje...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }
    }
}
