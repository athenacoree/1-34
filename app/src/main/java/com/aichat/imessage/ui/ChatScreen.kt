package com.aichat.imessage.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.aichat.imessage.data.Attachment
import com.aichat.imessage.ui.components.AvatarView
import com.aichat.imessage.ui.components.MessageBubble
import com.aichat.imessage.ui.components.TypingIndicator
import com.aichat.imessage.ui.theme.LocalAIChatColors
import com.aichat.imessage.viewmodel.AppViewModel
import java.io.File

private fun triggerHapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            v.vibrate(35)
        }
    } catch (e: Exception) {}
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Mensaje IA", text)
    clipboard.setPrimaryClip(clip)
}

private fun openAttachment(context: Context, attachment: Attachment) {
    try {
        val file = File(attachment.localPath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {}
}

@Composable
fun ChatScreen(viewModel: AppViewModel, chatId: String) {
    val colors = LocalAIChatColors.current
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsState()
    val conversation = conversations.find { it.id == chatId }

    val isListening by viewModel.isListening.collectAsState()
    val spokenText by viewModel.spokenText.collectAsState()
    val handsFreeMode by viewModel.handsFreeMode.collectAsState()

    if (conversation == null) {
        LaunchedEffect(Unit) { viewModel.closeChatPanel() }
        return
    }

    var input by remember { mutableStateOf("") }

    LaunchedEffect(spokenText) {
        if (spokenText.isNotBlank()) {
            input = if (input.isBlank()) spokenText else "$input $spokenText"
            viewModel.consumeSpokenText()
        }
    }

    val listState = rememberLazyListState()
    val itemCount = conversation.messages.size + if (conversation.pending) 1 else 0

    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {

        // Barra superior
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.bgCard).padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.closeChatPanel() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = colors.accent)
            }
            AvatarView(name = conversation.name, colorHex = conversation.avatarColor, fallback = colors.avatarBg, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    conversation.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.text
                )
                Text(
                    if (handsFreeMode) "Manos libres activo" else "En línea",
                    fontSize = 11.sp,
                    color = if (handsFreeMode) colors.accent else colors.textSecondary
                )
            }
            IconButton(onClick = { viewModel.toggleHandsFreeMode() }) {
                Icon(
                    Icons.Filled.Headset,
                    contentDescription = "Manos libres",
                    tint = if (handsFreeMode) colors.accent else colors.textSecondary
                )
            }
            IconButton(onClick = { viewModel.exportConversation(conversation.id) }) {
                Icon(Icons.Filled.IosShare, contentDescription = "Exportar chat", tint = colors.accent)
            }
            IconButton(onClick = { viewModel.deleteConversation(conversation.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar chat", tint = colors.danger)
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

        // Lista de mensajes
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (conversation.messages.isEmpty() && !conversation.pending) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxWidth().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Escribe un mensaje o habla con tu voz", color = colors.textSecondary)
                    }
                }
            }

            itemsIndexed(conversation.messages) { _, m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.role == "user") Arrangement.End else Arrangement.Start
                ) {
                    if (m.role != "user") {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Column {
                        MessageBubble(
                            message = m,
                            bubbleColor = if (m.role == "user") colors.bubbleSent else colors.bubbleReceived,
                            textColor = if (m.role == "user") colors.bubbleSentText else colors.bubbleReceivedText,
                            timeColor = colors.textSecondary,
                            errorColor = colors.danger,
                            onOpenAttachment = { att -> openAttachment(context, att) }
                        )
                        Row(modifier = Modifier.padding(top = 2.dp)) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Copiar",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        copyToClipboard(context, m.content)
                                        viewModel.showToast("Mensaje copiado", true)
                                    }
                            )
                        }
                    }
                }
            }

            if (conversation.pending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(colors.avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp))
                                .background(colors.bubbleReceived)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TypingIndicator(dotColor = colors.textSecondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Pensando...", color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Barra de entrada y controles
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.bgCard).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { viewModel.requestAttachFile() }) {
                Icon(Icons.Filled.AttachFile, contentDescription = "Adjuntar archivo", tint = colors.accent)
            }

            // Botón de Voz / Micrófono
            IconButton(
                onClick = {
                    triggerHapticFeedback(context)
                    if (isListening) {
                        viewModel.stopVoiceRecognition()
                    } else {
                        viewModel.startVoiceRecognition()
                    }
                }
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = "Dictado por voz",
                    tint = if (isListening) colors.danger else colors.accent
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.inputBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (input.isEmpty()) {
                    Text(
                        if (isListening) "Escuchando voz..." else "Mensaje",
                        color = if (isListening) colors.danger else colors.placeholder,
                        fontSize = 16.sp
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))

            val isPending = conversation.pending
            val canSend = input.isNotBlank() && !isPending

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (canSend) colors.accent else colors.avatarBg)
                    .clickable(enabled = canSend) {
                        triggerHapticFeedback(context)
                        viewModel.sendMessage(input)
                        input = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
