package com.aichat.imessage.tools

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

object PremiumVoiceTtsHelper {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale("es", "ES")
                selectBestFemaleVoice()
            }
        }
    }

    private fun selectBestFemaleVoice() {
        val currentTts = tts ?: return
        try {
            val voices = currentTts.voices ?: return
            // Buscar voces neurales/premium en español femenino estilo Siri/Alexa
            val femaleVoice = voices.find { voice ->
                val name = voice.name.lowercase()
                val isSpanish = voice.locale.language == "es"
                isSpanish && (name.contains("female") || name.contains("siri") || name.contains("es-es-x") || name.contains("network") || name.contains("neural"))
            } ?: voices.find { it.locale.language == "es" && !it.isNetworkConnectionRequired }

            if (femaleVoice != null) {
                currentTts.voice = femaleVoice
            }
            // Modulación de tono y velocidad para lograr voz clara, natural y elegante estilo Siri
            currentTts.setPitch(1.15f)
            currentTts.setSpeechRate(0.98f)
        } catch (e: Exception) {
            // Configuración segura por defecto
            currentTts.setPitch(1.1f)
            currentTts.setSpeechRate(1.0f)
        }
    }

    fun speak(text: String, pitch: Float = 1.15f, speed: Float = 0.98f) {
        val currentTts = tts ?: return
        if (!isInitialized) return
        try {
            currentTts.setPitch(pitch)
            currentTts.setSpeechRate(speed)
            // Limpieza ligera del texto para que no lea comandos de formato en voz alta
            val cleanText = text.replace(Regex("""\[\[ACCION:[^\]]+\]\]"""), "").trim()
            if (cleanText.isNotBlank()) {
                currentTts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_AI_REPLY")
            }
        } catch (e: Exception) {
            // Silencioso
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isInitialized = false
    }
}
