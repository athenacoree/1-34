package com.aichat.imessage.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aichat.imessage.data.Conversation
import com.aichat.imessage.data.ThemeMode
import com.aichat.imessage.ui.components.AvatarView
import com.aichat.imessage.ui.components.formatTime
import com.aichat.imessage.ui.theme.AIChatColors
import com.aichat.imessage.ui.theme.LocalAIChatColors
import com.aichat.imessage.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ChatListScreen(viewModel: AppViewModel) {
    val colors = LocalAIChatColors.current
    val conversations by viewModel.conversations.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val showNewChat by viewModel.showNewChat.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(conversations, query) {
        conversations
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedByDescending { it.updatedAt }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgSidebar)) {

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chats", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = colors.text)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoundIconButton(
                        icon = if (settings.theme == ThemeMode.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        bg = colors.bgCard,
                        tint = colors.text
                    ) {
                        val nextTheme = if (settings.theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                        viewModel.updateSettings(settings.apiKey, settings.model, nextTheme)
                    }
                    RoundIconButton(icon = Icons.Filled.Edit, bg = colors.accent, tint = Color.White) {
                        viewModel.openNewChatDialog()
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SearchField(query = query, onQueryChange = { query = it }, colors = colors)
        }

        if (conversations.isEmpty()) {
            EmptyState(colors = colors, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { conv ->
                    SwipeableChatRow(
                        conversation = conv,
                        colors = colors,
                        onOpen = { viewModel.openConversation(conv.id) },
                        onDelete = { viewModel.deleteConversation(conv.id) }
                    )
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 78.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
        TextButton(
            onClick = { viewModel.openSettingsDialog() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = colors.text)
            Spacer(Modifier.width(8.dp))
            Text("Configuración", color = colors.text)
        }
    }

    if (showNewChat) {
        NewChatDialog(
            onDismiss = { viewModel.closeNewChatDialog() },
            onCreate = { name -> viewModel.createConversation(name) }
        )
    }
    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { viewModel.closeSettingsDialog() },
            onSave = { key, model, theme -> viewModel.updateSettings(key, model, theme) }
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, colors: AIChatColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.inputBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (query.isEmpty()) {
                Text("Buscar", color = colors.placeholder, fontSize = 16.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyState(colors: AIChatColors, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.ChatBubbleOutline,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Sin conversaciones", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.text)
        Spacer(Modifier.height(4.dp))
        Text(
            "Toca el botón + para iniciar un chat nuevo con la IA.",
            color = colors.textSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SwipeableChatRow(
    conversation: Conversation,
    colors: AIChatColors,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 84.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        // Fondo de acción "Eliminar", igual que .swipe-action en el CSS original
        Row(
            modifier = Modifier.matchParentSize().background(colors.danger),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .fillMaxHeight()
                    .clickable { onDelete() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(20.dp))
                Text("Eliminar", color = Color.White, fontSize = 11.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bgSidebar)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(conversation.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val target = if (offsetX.value < -maxSwipePx / 2) -maxSwipePx else 0f
                                offsetX.animateTo(target)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newVal = (offsetX.value + dragAmount).coerceIn(-maxSwipePx, 0f)
                                offsetX.snapTo(newVal)
                            }
                        }
                    )
                }
                .clickable {
                    if (offsetX.value == 0f) onOpen() else scope.launch { offsetX.animateTo(0f) }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(name = conversation.name, colorHex = conversation.avatarColor, fallback = colors.avatarBg)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        conversation.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val lastTime = conversation.messages.lastOrNull()?.time
                    if (lastTime != null) {
                        Text(formatTime(lastTime), fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(2.dp))
                val snippet = if (conversation.pending) {
                    "escribiendo…"
                } else {
                    conversation.messages.lastOrNull()?.content ?: "Nueva conversación"
                }
                Text(
                    snippet,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
