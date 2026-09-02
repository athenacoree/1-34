package com.aichat.imessage.tools

/**
 * La IA puede pedir acciones en el dispositivo escribiendo, en cualquier
 * parte de su respuesta, comandos con este formato exacto:
 *
 *   [[ACCION:PERMISO|clave|motivo]]
 *   [[ACCION:ZIP|nombre|archivo1,archivo2]]
 *   [[ACCION:YOUTUBE_BUSCAR|texto de búsqueda]]
 *   [[ACCION:YOUTUBE_VIDEO|id_o_url]]
 *   [[ACCION:ABRIR_APP|nombre_o_paquete]]
 *   [[ACCION:LISTAR_APPS]]
 *   [[ACCION:ENVIAR_SMS|numero|mensaje]]
 *   [[ACCION:ENVIAR_MENSAJE|app|contacto_o_numero|mensaje]]
 *   [[ACCION:CREAR_ALARMA|hora|minuto|mensaje]]
 *   [[ACCION:CREAR_TEMPORIZADOR|segundos|mensaje]]
 *   [[ACCION:LLAMAR|numero_o_contacto]]
 *   [[ACCION:FLASHLIGHT|on_o_off]]
 *   [[ACCION:MUTE|on_o_off]]
 *   [[ACCION:NOTAS|guardar|texto]]
 *   [[ACCION:LEER_CONTACTO|nombre]]
 *   [[ACCION:BUSCAR_ARCHIVO|nombre_de_archivo]]
 *   [[ACCION:MODO_MANOS_LIBRES|on_o_off]]
 */
enum class AiActionType {
    PERMISO,
    ZIP,
    YOUTUBE_BUSCAR,
    YOUTUBE_VIDEO,
    ABRIR_APP,
    LISTAR_APPS,
    ENVIAR_SMS,
    ENVIAR_MENSAJE,
    CREAR_ALARMA,
    CREAR_TEMPORIZADOR,
    LLAMAR,
    FLASHLIGHT,
    MUTE,
    NOTAS,
    LEER_CONTACTO,
    BUSCAR_ARCHIVO,
    MODO_MANOS_LIBRES
}

data class AiAction(val type: AiActionType, val args: List<String>)

data class ParsedAiMessage(val displayText: String, val actions: List<AiAction>)

private val ACTION_REGEX = Regex("""\[\[ACCION:([A-Z_]+)(?:\|([^\]]*))?\]\]""")

fun parseAiActions(raw: String): ParsedAiMessage {
    val actions = mutableListOf<AiAction>()
    val cleaned = ACTION_REGEX.replace(raw) { match ->
        val typeStr = match.groupValues[1]
        val argsStr = match.groupValues.getOrNull(2) ?: ""
        val type = runCatching { AiActionType.valueOf(typeStr) }.getOrNull()
        if (type != null) {
            val argsList = if (argsStr.isBlank()) emptyList() else argsStr.split("|").map { it.trim() }
            actions.add(AiAction(type, argsList))
        }
        ""
    }.trim()
    return ParsedAiMessage(cleaned, actions)
}
