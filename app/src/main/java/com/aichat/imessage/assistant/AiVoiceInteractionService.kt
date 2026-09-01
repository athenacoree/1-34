package com.aichat.imessage.assistant

import android.service.voice.VoiceInteractionService

/**
 * Servicio "raíz" del asistente. Android lo mantiene vivo cuando esta app
 * está seleccionada como Asistente predeterminado. No necesita lógica propia:
 * su única función es habilitar que el sistema pueda crear sesiones
 * (ver [AiVoiceInteractionSessionService]) cuando el usuario invoca al asistente
 * (gesto, botón de encendido largo, "Ok Google" reemplazado, etc.).
 */
class AiVoiceInteractionService : VoiceInteractionService()
