package com.aichat.imessage.assistant

import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Cada vez que el usuario invoca al asistente (gesto, botón de encendido, etc.)
 * el sistema llama a [onNewSession] para crear la burbuja flotante.
 */
class AiVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: android.os.Bundle?): VoiceInteractionSession {
        return AiVoiceInteractionSession(this)
    }
}
