package com.aichat.imessage.assistant

import android.content.Intent
import android.speech.RecognitionService

/**
 * Android exige declarar un RecognitionService para poder registrar la app
 * como Asistente predeterminado. Nuestra burbuja no usa reconocimiento de voz
 * real (todavía), así que este stub simplemente no hace nada: existe solo
 * para cumplir el requisito del sistema.
 */
class AiRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(android.speech.SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onCancel(listener: Callback?) {}

    override fun onStopListening(listener: Callback?) {}
}
