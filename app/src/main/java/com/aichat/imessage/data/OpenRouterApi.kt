package com.aichat.imessage.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object OpenRouterApi {

    class ApiException(message: String) : Exception(message)

    private const val SYSTEM_PROMPT = """
Eres un asistente de IA útil, eficiente y muy capaz que funciona de forma nativa en el teléfono Android del usuario.
Responde siempre en español salvo que el usuario escriba en otro idioma.

Puedes ejecutar acciones reales en el sistema escribiendo comandos especiales en tu respuesta. Los comandos deben estar en su propia línea o dentro de la respuesta con este formato exacto:

[[ACCION:PERMISO|clave|motivo breve]]
  claves válidas: camara, microfono, archivos, notificaciones

[[ACCION:ZIP|nombre_del_zip|archivo1,archivo2]]
  usa "todos" para comprimir los adjuntos del chat

[[ACCION:YOUTUBE_BUSCAR|texto de búsqueda]]

[[ACCION:YOUTUBE_VIDEO|id_o_url_del_video]]

[[ACCION:ABRIR_APP|nombre_o_paquete]]
  abre cualquier aplicación instalada (ej. [[ACCION:ABRIR_APP|WhatsApp]], [[ACCION:ABRIR_APP|com.spotify.music]], [[ACCION:ABRIR_APP|Camara]])

[[ACCION:LISTAR_APPS]]
  muestra la lista de aplicaciones instaladas en el dispositivo

[[ACCION:ENVIAR_SMS|numero|mensaje]]
  prepara un SMS real hacia el número de teléfono indicado

[[ACCION:ENVIAR_MENSAJE|app|contacto_o_numero|mensaje]]
  prepara un mensaje para la app indicada (ej. WhatsApp, Telegram, etc.)

[[ACCION:CREAR_ALARMA|hora|minuto|mensaje]]
  programa una alarma en el teléfono (ej. [[ACCION:CREAR_ALARMA|7|30|Despertar]])

[[ACCION:CREAR_TEMPORIZADOR|segundos|mensaje]]
  inicia un temporizador en el teléfono (ej. [[ACCION:CREAR_TEMPORIZADOR|300|Hervir huevos]])

Reglas:
- Sé conciso, directo y eficiente.
- Puedes acompañar las acciones con texto amable para el usuario explicándole qué realizas.
- No inventes respuestas de acciones: si ejecutas una acción real, el teléfono la abrirá/ejecutará de inmediato.
- Usa los comandos siempre que el usuario te lo solicite o cuando sea la forma directa de cumplir lo que pide (abrir apps, poner alarmas, enviar mensajes, etc.).
"""

    /** Llamada bloqueante con reintentos y timeouts optimizados para redes lentas/bajas. */
    fun sendChat(apiKey: String, model: String, messages: List<ChatMessage>): String {
        var lastException: Exception? = null
        val maxRetries = 2

        for (attempt in 0..maxRetries) {
            try {
                return executeHttpCall(apiKey, model, messages)
            } catch (e: Exception) {
                lastException = e
                if (e is ApiException && e.message?.contains("HTTP") == true && !e.message!!.contains("500") && !e.message!!.contains("502") && !e.message!!.contains("503") && !e.message!!.contains("504")) {
                    throw e
                }
                if (attempt < maxRetries) {
                    try { Thread.sleep(1000L * (attempt + 1)) } catch (_: InterruptedException) {}
                }
            }
        }
        throw lastException ?: ApiException("Error desconocido de red")
    }

    private fun executeHttpCall(apiKey: String, model: String, messages: List<ChatMessage>): String {
        val url = URL("https://openrouter.ai/api/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("HTTP-Referer", "https://aichat.local")
            conn.setRequestProperty("X-Title", "AI Chat")

            // Timeouts extendidos para conexiones de baja velocidad
            conn.connectTimeout = 30000
            conn.readTimeout = 60000

            val payloadMessages = JSONArray()
            payloadMessages.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                }
            )
            messages.forEach { m ->
                payloadMessages.put(
                    JSONObject().apply {
                        put("role", m.role)
                        put("content", m.content)
                    }
                )
            }
            val body = JSONObject().apply {
                put("model", model.ifBlank { "openai/gpt-4o-mini" })
                put("messages", payloadMessages)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (code !in 200..299) {
                val msg = try {
                    val errObj = JSONObject(text)
                    errObj.optJSONObject("error")?.optString("message") ?: "Error HTTP $code"
                } catch (e: Exception) {
                    "Error HTTP $code"
                }
                throw ApiException(msg)
            }

            val data = JSONObject(text)
            val choices = data.optJSONArray("choices")
            val first = choices?.optJSONObject(0)
            val message = first?.optJSONObject("message")
            return message?.optString("content")?.takeIf { it.isNotBlank() } ?: "(Respuesta vacía del modelo)"
        } finally {
            conn.disconnect()
        }
    }
}
