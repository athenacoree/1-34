package com.aichat.imessage.tools

import android.Manifest
import android.os.Build

/** Permisos del sistema que la IA puede solicitar a través de [[ACCION:PERMISO|...]]. */
enum class AiPermissionKey(val label: String, val androidPermission: String?) {
    CAMARA("Cámara", Manifest.permission.CAMERA),
    MICROFONO("Micrófono", Manifest.permission.RECORD_AUDIO),
    ARCHIVOS(
        "Archivos y medios",
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    ),
    NOTIFICACIONES(
        "Notificaciones",
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    )
}

fun resolvePermissionKey(raw: String): AiPermissionKey? = when (raw.trim().lowercase()) {
    "camara", "cámara", "camera" -> AiPermissionKey.CAMARA
    "microfono", "micrófono", "microphone" -> AiPermissionKey.MICROFONO
    "archivos", "storage", "files", "almacenamiento" -> AiPermissionKey.ARCHIVOS
    "notificaciones", "notifications" -> AiPermissionKey.NOTIFICACIONES
    else -> null
}

/** Pedido de permiso pendiente de que el usuario lo apruebe o rechace desde un diálogo. */
data class PermissionRequest(val conversationId: String, val key: AiPermissionKey, val reason: String)
