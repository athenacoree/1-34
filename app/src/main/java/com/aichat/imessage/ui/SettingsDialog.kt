package com.aichat.imessage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aichat.imessage.data.AppSettings
import com.aichat.imessage.data.ThemeMode
import com.aichat.imessage.ui.theme.AIChatColors
import com.aichat.imessage.ui.theme.LocalAIChatColors
import com.aichat.imessage.viewmodel.AppViewModel

@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalAIChatColors.current
    var apiKey by remember { mutableStateOf(currentSettings.apiKey) }
    var model by remember { mutableStateOf(currentSettings.model) }
    var theme by remember { mutableStateOf(currentSettings.theme) }

    var pitch by remember { mutableFloatStateOf(viewModel.voicePitch) }
    var speed by remember { mutableFloatStateOf(viewModel.voiceSpeed) }

    var youtubeApiKey by remember { mutableStateOf(currentSettings.youtubeApiKey) }
    var googleApiKey by remember { mutableStateOf(currentSettings.googleApiKey) }
    var googleCseId by remember { mutableStateOf(currentSettings.googleCseId) }
    var nasaApiKey by remember { mutableStateOf(currentSettings.nasaApiKey) }

    var biometricLockEnabled by remember { mutableStateOf(currentSettings.biometricLockEnabled) }
    var readNotificationsEnabled by remember { mutableStateOf(currentSettings.readNotificationsEnabled) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgCard)
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Configuración de IA y App", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.text)
            Spacer(Modifier.height(14.dp))

            ApiKeyField(
                label = "Clave de API OpenRouter",
                value = apiKey,
                onValueChange = { apiKey = it },
                colors = colors
            )
            Spacer(Modifier.height(10.dp))

            Text("Modelo de IA", fontSize = 12.sp, color = colors.textSecondary)
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "💡 Nota: Los modelos con el sufijo :free (ej. google/gemini-2.0-flash-exp:free) no consumen créditos ni tokens de pago.",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(14.dp))

            Text("Claves de API Opcionales (Editables / Borrables)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.text)
            Spacer(Modifier.height(6.dp))

            ApiKeyField(
                label = "YouTube Data API Key",
                value = youtubeApiKey,
                onValueChange = { youtubeApiKey = it },
                colors = colors
            )

            ApiKeyField(
                label = "Google Custom Search API Key",
                value = googleApiKey,
                onValueChange = { googleApiKey = it },
                colors = colors
            )

            Text("Google Search Engine ID / CX", fontSize = 12.sp, color = colors.textSecondary)
            OutlinedTextField(
                value = googleCseId,
                onValueChange = { googleCseId = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (googleCseId.isNotEmpty()) {
                        IconButton(onClick = { googleCseId = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Borrar", tint = colors.textSecondary)
                        }
                    }
                }
            )

            ApiKeyField(
                label = "NASA APOD API Key (Opcional)",
                value = nasaApiKey,
                onValueChange = { nasaApiKey = it },
                colors = colors
            )

            Spacer(Modifier.height(14.dp))
            Text("Seguridad y Notificaciones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.text)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bloqueo de App (PIN / Huella)", fontSize = 13.sp, color = colors.text, fontWeight = FontWeight.Medium)
                    Text("Protege el acceso a la app", fontSize = 11.sp, color = colors.textSecondary)
                }
                Switch(
                    checked = biometricLockEnabled,
                    onCheckedChange = { biometricLockEnabled = it }
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Leer Notificaciones en Voz Alta", fontSize = 13.sp, color = colors.text, fontWeight = FontWeight.Medium)
                    Text("Lee WhatsApp / SMS en modo manos libres", fontSize = 11.sp, color = colors.textSecondary)
                }
                Switch(
                    checked = readNotificationsEnabled,
                    onCheckedChange = { readNotificationsEnabled = it }
                )
            }

            Spacer(Modifier.height(14.dp))
            Text("Respaldo de Datos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.text)
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { viewModel.exportAllData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📦 Exportar Todo (Conversaciones y Configuración .zip)", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(14.dp))
            Text("Ajustes de Voz Femenina Siri/Alexa", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.text)
            Spacer(Modifier.height(4.dp))
            Text("Tono de voz: ${String.format("%.2f", pitch)}", fontSize = 12.sp, color = colors.textSecondary)
            Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.8f..1.5f)

            Text("Velocidad de voz: ${String.format("%.2f", speed)}", fontSize = 12.sp, color = colors.textSecondary)
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.7f..1.4f)

            Spacer(Modifier.height(14.dp))
            Text("Tema", fontSize = 12.sp, color = colors.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = theme == ThemeMode.SYSTEM, onClick = { theme = ThemeMode.SYSTEM })
                Text("Sistema", color = colors.text, modifier = Modifier.clickable { theme = ThemeMode.SYSTEM })
                Spacer(Modifier.width(12.dp))
                RadioButton(selected = theme == ThemeMode.LIGHT, onClick = { theme = ThemeMode.LIGHT })
                Text("Claro", color = colors.text, modifier = Modifier.clickable { theme = ThemeMode.LIGHT })
                Spacer(Modifier.width(12.dp))
                RadioButton(selected = theme == ThemeMode.DARK, onClick = { theme = ThemeMode.DARK })
                Text("Oscuro", color = colors.text, modifier = Modifier.clickable { theme = ThemeMode.DARK })
            }

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = colors.textSecondary)
                }
                TextButton(
                    onClick = {
                        viewModel.voicePitch = pitch
                        viewModel.voiceSpeed = speed
                        viewModel.updateSettings(
                            apiKey = apiKey,
                            model = model,
                            theme = theme,
                            pitch = pitch,
                            speed = speed,
                            youtubeApiKey = youtubeApiKey,
                            googleApiKey = googleApiKey,
                            googleCseId = googleCseId,
                            nasaApiKey = nasaApiKey,
                            biometricLockEnabled = biometricLockEnabled,
                            readNotificationsEnabled = readNotificationsEnabled
                        )
                    }
                ) {
                    Text("Guardar", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    colors: AIChatColors
) {
    var visible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = colors.textSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Borrar clave", tint = colors.textSecondary)
                        }
                    }
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (visible) "Ocultar" else "Mostrar",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        )
    }
}
