package com.aichat.imessage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aichat.imessage.data.AppSettings
import com.aichat.imessage.data.ThemeMode
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

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bgCard)
                .padding(20.dp)
        ) {
            Text("Configuración de IA y Voz", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.text)
            Spacer(Modifier.height(14.dp))

            Text("Clave de API OpenRouter", fontSize = 12.sp, color = colors.textSecondary)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            Text("Modelo de IA", fontSize = 12.sp, color = colors.textSecondary)
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = colors.textSecondary)
                }
                TextButton(
                    onClick = {
                        viewModel.voicePitch = pitch
                        viewModel.voiceSpeed = speed
                        viewModel.updateSettings(apiKey, model, theme)
                    }
                ) {
                    Text("Guardar", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
