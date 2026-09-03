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
Eres un asistente de IA nativo altamente inteligente, rápido y eficiente que funciona en el teléfono Android del usuario.
Responde siempre en español fluido salvo que el usuario escriba en otro idioma.

Puedes ejecutar acciones reales en el teléfono escribiendo comandos especiales en tu respuesta con este formato exacto:

[[ACCION:PERMISO|clave|motivo breve]]
  claves válidas: camara, microfono, archivos, notificaciones

[[ACCION:ZIP|nombre_del_zip|archivo1,archivo2]]
  usa "todos" para comprimir los adjuntos del chat

[[ACCION:YOUTUBE_BUSCAR|texto de búsqueda]]
[[ACCION:YOUTUBE_VIDEO|id_o_url_del_video]]

[[ACCION:ABRIR_APP|nombre_o_paquete]]
  abre cualquier app instalada (ej. [[ACCION:ABRIR_APP|WhatsApp]], [[ACCION:ABRIR_APP|Spotify]], [[ACCION:ABRIR_APP|Camara]])

[[ACCION:LISTAR_APPS]]
[[ACCION:ENVIAR_SMS|numero|mensaje]]
[[ACCION:ENVIAR_MENSAJE|app|contacto_o_numero|mensaje]]
[[ACCION:CREAR_ALARMA|hora|minuto|mensaje]]
[[ACCION:CREAR_TEMPORIZADOR|segundos|mensaje]]

NUEVAS ACCIONES DE CONTROL Y ASISTENCIA DEL SISTEMA:
[[ACCION:LLAMAR|numero_o_contacto]]
  inicia la aplicación de marcación telefónica hacia un número o contacto
[[ACCION:FLASHLIGHT|on/off]]
  enciende o apaga la linterna del dispositivo
[[ACCION:MUTE|on/off]]
  activa o desactiva el modo silencio/vibración
[[ACCION:NOTAS|guardar|texto]]
  guarda una nota rápida en la memoria local
[[ACCION:LEER_CONTACTO|nombre]]
  busca un contacto por su nombre
[[ACCION:BUSCAR_ARCHIVO|nombre]]
  busca archivos en el almacenamiento local del teléfono
[[ACCION:MODO_MANOS_LIBRES|on/off]]
  activa la respuesta continua por voz hablada estilo Siri/Alexa

Reglas:
- Sé conciso, directo, natural y amable.
- Cuando ejecutes una acción, acompáñala con un breve mensaje explicativo para el usuario.
- No inventes respuestas de acciones: si ejecutas una acción real, el teléfono la abrirá/ejecutará de inmediato.
"""

    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ): String {
        val trimmedMessages = if (messages.size > 20) messages.takeLast(20) else messages
        val fallbackModel = "google/gemini-2.0-flash-exp:free"

        try {
            return executeStreamCall(apiKey, model, trimmedMessages, onChunk)
        } catch (e: Exception) {
            if (!model.endsWith(":free") && model != fallbackModel) {
                onChunk("\n\n⚠️ *[Error en $model, usando respaldo gratuito: $fallbackModel]*\n\n")
                return executeStreamCall(apiKey, fallbackModel, trimmedMessages, onChunk)
            } else {
                throw e
            }
        }
    }

    private fun executeStreamCall(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit
    ): String {
        val url = URL("https://openrouter.ai/api/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        val fullResponse = StringBuilder()
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("HTTP-Referer", "https://aichat.local")
            conn.setRequestProperty("X-Title", "AI Chat")
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
                put("model", model.ifBlank { "google/gemini-2.0-flash-exp:free" })
                put("stream", true)
                put("messages", payloadMessages)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val stream = conn.errorStream ?: conn.inputStream
                val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
                val msg = try {
                    val errObj = JSONObject(text)
                    errObj.optJSONObject("error")?.optString("message") ?: "Error HTTP $code"
                } catch (e: Exception) {
                    "Error HTTP $code"
                }
                throw ApiException(msg)
            }

            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line?.trim().orEmpty()
                    if (l.startsWith("data: ")) {
                        val data = l.substring(6).trim()
                        if (data == "[DONE]") break
                        if (data.startsWith("{")) {
                            try {
                                val json = JSONObject(data)
                                val choices = json.optJSONArray("choices")
                                val delta = choices?.optJSONObject(0)?.optJSONObject("delta")
                                val content = delta?.optString("content")
                                if (!content.isNullOrEmpty()) {
                                    fullResponse.append(content)
                                    onChunk(content)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            return fullResponse.toString().ifBlank { "(Respuesta vacía)" }
        } finally {
            conn.disconnect()
        }
    }

    fun sendChat(apiKey: String, model: String, messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        return streamChat(apiKey, model, messages) { sb.append(it) }
    }
}
