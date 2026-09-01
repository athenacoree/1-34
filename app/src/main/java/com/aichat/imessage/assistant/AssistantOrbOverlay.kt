package com.aichat.imessage.assistant

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Burbuja flotante estilo Siri/Alexa: núcleo con gradiente que gira y pulsa,
 * más un halo exterior. Toque corto = abrir el chat. Toque largo o el sistema
 * la puede descartar con [onDismiss].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistantOrbOverlay(
    onTap: () -> Unit,
    onDismiss: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "orb")

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "rotation"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val haloAlpha by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "halo"
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onDismiss
            ),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // Halo exterior difuso
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF7C4DFF).copy(alpha = haloAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Núcleo del orbe: gradiente cónico girando + pulso de escala
        Canvas(
            modifier = Modifier
                .size((64f * pulse).dp)
        ) {
            rotate(rotation) {
                val colors = listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF7C4DFF),
                    Color(0xFFFF4DD8),
                    Color(0xFF00E5FF)
                )
                drawCircle(
                    brush = Brush.sweepGradient(colors),
                    radius = size.minDimension / 2f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            // Brillo interior tipo glassmorphism
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.35f, size.height * 0.3f),
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * 0.35f, size.height * 0.3f)
            )
        }

        // Tres puntitos orbitando, como "escuchando"
        OrbitingDots(rotation)
    }
}

@Composable
private fun OrbitingDots(rotationDeg: Float) {
    Canvas(modifier = Modifier.size(90.dp)) {
        val radius = size.minDimension / 2f - 4.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        repeat(3) { i ->
            val angle = Math.toRadians((rotationDeg * 1.5f + i * 120f).toDouble())
            val dotCenter = Offset(
                x = center.x + radius * cos(angle).toFloat(),
                y = center.y + radius * sin(angle).toFloat()
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 2.5.dp.toPx(),
                center = dotCenter
            )
        }
    }
}
