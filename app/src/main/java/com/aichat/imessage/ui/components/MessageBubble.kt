package com.aichat.imessage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aichat.imessage.data.Attachment
import com.aichat.imessage.data.ChatMessage
import com.aichat.imessage.data.MessageDisplayMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Igual que formatTime() de app.js */
fun formatTime(ts: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("es"))
    return sdf.format(Date(ts))
}

/** Un mensaje puede mezclar texto normal y bloques de código en la misma burbuja. */
sealed class ContentSegment {
    data class Text(val text: String) : ContentSegment()
    data class Code(val code: String, val language: String?) : ContentSegment()
}

private val CODE_FENCE_REGEX = Regex("```([a-zA-Z0-9_+-]*)\\n?([\\s\\S]*?)```")

fun splitContentSegments(content: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    var lastIndex = 0
    for (match in CODE_FENCE_REGEX.findAll(content)) {
        if (match.range.first > lastIndex) {
            val text = content.substring(lastIndex, match.range.first)
            if (text.isNotBlank()) segments.add(ContentSegment.Text(text.trim()))
        }
        val lang = match.groupValues[1].ifBlank { null }
        val code = match.groupValues[2].trimEnd('\n')
        segments.add(ContentSegment.Code(code, lang))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < content.length) {
        val text = content.substring(lastIndex)
        if (text.isNotBlank()) segments.add(ContentSegment.Text(text.trim()))
    }
    if (segments.isEmpty()) segments.add(ContentSegment.Text(content))
    return segments
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    bubbleColor: Color,
    textColor: Color,
    timeColor: Color,
    errorColor: Color,
    onOpenAttachment: (Attachment) -> Unit = {}
) {
    val sent = message.role == "user"
    val isPlain = message.displayMode == MessageDisplayMode.PLAIN

    val rawSegments = splitContentSegments(message.content)
    val segments = if (message.displayMode == MessageDisplayMode.CODE &&
        rawSegments.size == 1 && rawSegments[0] is ContentSegment.Text
    ) {
        listOf(ContentSegment.Code((rawSegments[0] as ContentSegment.Text).text, null))
    } else {
        rawSegments
    }

    Column(horizontalAlignment = if (sent) Alignment.End else Alignment.Start) {
        if (isPlain) {
            // "Texto suelto": mensajes de sistema / registro de acciones de la IA,
            // sin fondo de burbuja, para diferenciarlos de la conversación normal.
            Text(
                text = message.content,
                color = if (message.error) errorColor else timeColor,
                fontSize = 13.sp,
                modifier = Modifier.widthIn(max = 300.dp).padding(vertical = 2.dp, horizontal = 4.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (sent) 18.dp else 4.dp,
                            bottomEnd = if (sent) 4.dp else 18.dp
                        )
                    )
                    .background(if (message.error) errorColor.copy(alpha = 0.16f) else bubbleColor)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                segments.forEachIndexed { index, segment ->
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    when (segment) {
                        is ContentSegment.Text -> Text(
                            text = segment.text,
                            color = if (message.error) errorColor else textColor,
                            fontSize = 16.sp
                        )
                        is ContentSegment.Code -> CodeBlockView(segment.code, segment.language)
                    }
                }
            }
        }

        if (message.attachments.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            message.attachments.forEach { att ->
                AttachmentChip(att, onClick = { onOpenAttachment(att) })
                Spacer(Modifier.height(4.dp))
            }
        }

        Text(
            text = formatTime(message.time),
            color = timeColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
