package com.aichat.imessage.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.aichat.imessage.tools.PermissionRequest

@Composable
fun PermissionRequestDialog(
    request: PermissionRequest,
    onRespond: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onRespond(false) },
        title = { Text("La IA quiere usar: ${request.key.label}") },
        text = { Text(request.reason.ifBlank { "La IA necesita este permiso para ayudarte mejor." }) },
        confirmButton = {
            TextButton(onClick = { onRespond(true) }) { Text("Permitir") }
        },
        dismissButton = {
            TextButton(onClick = { onRespond(false) }) { Text("Denegar") }
        }
    )
}
