package com.aichat.imessage.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aichat.imessage.MainActivity

/**
 * Esta clase es "la burbuja". No es una Activity ni abre ningún panel:
 * el sistema la muestra como una ventana pequeña y transparente flotando
 * sobre lo que sea que el usuario esté usando, igual que Siri o el
 * asistente de Google cuando se invoca con el gesto.
 */
class AiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private var lifecycleOwner: SessionLifecycleOwner? = null

    override fun onCreate() {
        super.onCreate()
        setUiEnabled(true)

        // Ventana chica, transparente y sin dim, anclada abajo al centro (como el orbe de Siri)
        window?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            )
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            val size = dp(110)
            setLayout(size, size)
            attributes = attributes.apply {
                y = dp(90)
            }
        }
    }

    override fun onCreateContentView(): View {
        val owner = SessionLifecycleOwner().also { lifecycleOwner = it }
        owner.performCreate()

        return ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                AssistantOrbOverlay(
                    onTap = { openChat() },
                    onDismiss = { hide() }
                )
            }
        }
    }

    override fun onHandleAssist(data: Bundle?, structure: android.app.assist.AssistStructure?, content: android.app.assist.AssistContent?) {
        // Aquí en el futuro se podría leer structure/content para "ver" la app abierta.
        // Por ahora la burbuja no necesita este contexto para mostrarse.
    }

    override fun onDestroy() {
        lifecycleOwner?.performDestroy()
        lifecycleOwner = null
        super.onDestroy()
    }

    private fun openChat() {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startAssistantActivity(intent)
        hide()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics
    ).toInt()
}
