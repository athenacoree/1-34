package com.aichat.imessage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Igual que la función initials() de app.js */
fun initials(name: String): String {
    if (name.isBlank()) return "IA"
    val parts = name.trim().split(Regex("\\s+"))
    val a = parts.getOrNull(0)?.firstOrNull()?.toString() ?: ""
    val b = parts.getOrNull(1)?.firstOrNull()?.toString() ?: ""
    val result = (a + b).uppercase()
    return result.ifBlank { "IA" }
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun AvatarView(
    name: String,
    colorHex: String,
    fallback: Color,
    size: Dp = 44.dp
) {
    Box(
        modifier = Modifier.size(size).background(parseHexColor(colorHex, fallback), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials(name),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.36f).sp
        )
    }
}
