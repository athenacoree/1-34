package com.aichat.imessage.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aichat.imessage.data.AppSettings
import com.aichat.imessage.data.ThemeMode

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (String, String, ThemeMode) -> Unit
) {
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var model by remember { mutableStateOf(settings.model) }
    var theme by remember { mutableStateOf(settings.theme) }
    var showKey by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Clave de API de OpenRouter", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("sk-or-v1-...") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Mostrar/ocultar clave"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Consíguela gratis en openrouter.ai/keys. Se guarda solo en este dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text("Modelo", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    placeholder = { Text("openai/gpt-4o-mini") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Ejemplos: openai/gpt-4o-mini · anthropic/claude-3.5-sonnet · meta-llama/llama-3.1-70b-instruct",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text("Tema", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                ThemeOptionRow("Automático (según el sistema)", theme == ThemeMode.SYSTEM) { theme = ThemeMode.SYSTEM }
                ThemeOptionRow("Claro", theme == ThemeMode.LIGHT) { theme = ThemeMode.LIGHT }
                ThemeOptionRow("Oscuro", theme == ThemeMode.DARK) { theme = ThemeMode.DARK }

                Spacer(Modifier.height(16.dp))
                Text("Asistente del sistema", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Reemplaza al asistente predeterminado del teléfono (Gemini/Google) por una " +
                        "burbuja flotante de esta app cuando lo invoques con el gesto o botón de encendido.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextButton(onClick = { requestAssistantRole(context) }) {
                    Text("Usar como asistente predeterminado")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(apiKey, model, theme) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

/**
 * Abre el selector del sistema para que el usuario elija esta app como
 * Asistente predeterminado. Requiere Android 10+ (API 29). En versiones
 * anteriores no existe este rol y solo se avisa por Toast.
 *
 * Nota: en teléfonos con MIUI/HyperOS (como los Redmi), Xiaomi a veces
 * restringe u oculta esta opción incluso cuando la app la solicita
 * correctamente. Si el selector no aparece, hay que revisar manualmente
 * en Ajustes > Apps > Apps predeterminadas > App de asistencia.
 */
private fun requestAssistantRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
            if (roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                Toast.makeText(context, "Ya eres el asistente predeterminado", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
            context.startActivity(intent)
            return
        }
    }
    Toast.makeText(
        context,
        "Este teléfono no permite cambiar el asistente predeterminado desde la app",
        Toast.LENGTH_LONG
    ).show()
}

@Composable
private fun ThemeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
