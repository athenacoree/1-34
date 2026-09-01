package com.aichat.imessage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aichat.imessage.data.ThemeMode

data class AIChatColors(
    val bg: Color,
    val bgSidebar: Color,
    val bgCard: Color,
    val bubbleSent: Color,
    val bubbleSentText: Color,
    val bubbleReceived: Color,
    val bubbleReceivedText: Color,
    val text: Color,
    val textSecondary: Color,
    val divider: Color,
    val inputBg: Color,
    val inputBorder: Color,
    val placeholder: Color,
    val avatarBg: Color,
    val modalBg: Color,
    val danger: Color,
    val accent: Color,
    val accentWash: Color
)

// Misma paleta que --bg, --bubble-received, --accent, etc. en style.css (tema claro)
val LightAIChatColors = AIChatColors(
    bg = Color(0xFFFFFFFF),
    bgSidebar = Color(0xFFF6F6F8),
    bgCard = Color(0xFFFFFFFF),
    bubbleSent = Color(0xFF0B6CFF),
    bubbleSentText = Color(0xFFFFFFFF),
    bubbleReceived = Color(0xFFE9E9EB),
    bubbleReceivedText = Color(0xFF111114),
    text = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),
    divider = Color(red = 60, green = 60, blue = 67, alpha = 56),
    inputBg = Color(0xFFFFFFFF),
    inputBorder = Color(0xFFD5D5DA),
    placeholder = Color(0xFFA9A9AE),
    avatarBg = Color(0xFFC7C7CC),
    modalBg = Color(0xFFFFFFFF),
    danger = Color(0xFFFF3B30),
    accent = Color(0xFF0B6CFF),
    accentWash = Color(red = 11, green = 108, blue = 255, alpha = 31)
)

// Misma paleta que el bloque [data-theme="dark"] en style.css
val DarkAIChatColors = AIChatColors(
    bg = Color(0xFF000000),
    bgSidebar = Color(0xFF0A0A0C),
    bgCard = Color(0xFF1C1C1E),
    bubbleSent = Color(0xFF2F8CFF),
    bubbleSentText = Color(0xFFFFFFFF),
    bubbleReceived = Color(0xFF26262A),
    bubbleReceivedText = Color(0xFFF2F2F3),
    text = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF8E8E93),
    divider = Color(red = 84, green = 84, blue = 88, alpha = 107),
    inputBg = Color(0xFF1C1C1E),
    inputBorder = Color(0xFF3A3A3C),
    placeholder = Color(0xFF6D6D72),
    avatarBg = Color(0xFF48484A),
    modalBg = Color(0xFF1C1C1E),
    danger = Color(0xFFFF453A),
    accent = Color(0xFF2F8CFF),
    accentWash = Color(red = 47, green = 140, blue = 255, alpha = 46)
)

val LocalAIChatColors = staticCompositionLocalOf { LightAIChatColors }

@Composable
fun AIChatTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colors = if (isDark) DarkAIChatColors else LightAIChatColors

    CompositionLocalProvider(LocalAIChatColors provides colors) {
        MaterialTheme(content = content)
    }
}
