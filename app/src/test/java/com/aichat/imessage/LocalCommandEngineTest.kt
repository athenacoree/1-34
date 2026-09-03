package com.aichat.imessage

import android.content.Context
import android.content.pm.PackageManager
import com.aichat.imessage.data.AppSettings
import com.aichat.imessage.tools.AiActionType
import com.aichat.imessage.tools.LocalCommandEngine
import com.aichat.imessage.tools.LocalCommandResult
import com.aichat.imessage.tools.parseAiActions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class LocalCommandEngineTest {

    private lateinit var mockContext: Context
    private lateinit var mockPackageManager: PackageManager
    private val defaultSettings = AppSettings()

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPackageManager = mock(PackageManager::class.java)
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockPackageManager.queryIntentActivities(any(), anyInt())).thenReturn(emptyList())
    }

    @Test
    fun testParseAiActions() {
        val raw = "Hola [[ACCION:CREAR_ALARMA|8|30|Despertar]] mundo"
        val parsed = parseAiActions(raw)
        assertEquals("Hola  mundo", parsed.displayText)
        assertEquals(1, parsed.actions.size)
        assertEquals(AiActionType.CREAR_ALARMA, parsed.actions[0].type)
        assertEquals(listOf("8", "30", "Despertar"), parsed.actions[0].args)
    }

    @Test
    fun testLocalCommandAlarm() = runBlocking {
        val result = LocalCommandEngine.process("pon una alarma a las 7:30", mockContext, defaultSettings)
        assertTrue(result is LocalCommandResult.Handled)
        val handled = result as LocalCommandResult.Handled
        assertEquals(1, handled.actions.size)
        assertEquals(AiActionType.CREAR_ALARMA, handled.actions[0].type)
    }

    @Test
    fun testLocalCommandTimer() = runBlocking {
        val result = LocalCommandEngine.process("temporizador de 5 minutos", mockContext, defaultSettings)
        assertTrue(result is LocalCommandResult.Handled)
        val handled = result as LocalCommandResult.Handled
        assertEquals(1, handled.actions.size)
        assertEquals(AiActionType.CREAR_TEMPORIZADOR, handled.actions[0].type)
        assertEquals("300", handled.actions[0].args[0])
    }

    @Test
    fun testLocalCommandFlashlight() = runBlocking {
        val result = LocalCommandEngine.process("enciende la linterna", mockContext, defaultSettings)
        assertTrue(result is LocalCommandResult.Handled)
        val handled = result as LocalCommandResult.Handled
        assertEquals(1, handled.actions.size)
        assertEquals(AiActionType.FLASHLIGHT, handled.actions[0].type)
        assertEquals("on", handled.actions[0].args[0])
    }

    @Test
    fun testLocalCommandMute() = runBlocking {
        val result = LocalCommandEngine.process("modo silencio", mockContext, defaultSettings)
        assertTrue(result is LocalCommandResult.Handled)
        val handled = result as LocalCommandResult.Handled
        assertEquals(1, handled.actions.size)
        assertEquals(AiActionType.MUTE, handled.actions[0].type)
    }

    @Test
    fun testLocalCommandOpenApp() = runBlocking {
        val result = LocalCommandEngine.process("abre WhatsApp", mockContext, defaultSettings)
        assertTrue(result is LocalCommandResult.Handled)
        val handled = result as LocalCommandResult.Handled
        assertEquals(1, handled.actions.size)
        assertEquals(AiActionType.ABRIR_APP, handled.actions[0].type)
        assertEquals("WhatsApp", handled.actions[0].args[0])
    }

    @Test
    fun testLocalCommandNotHandled() = runBlocking {
        val result = LocalCommandEngine.process("¿Cuál es el significado de la vida?", mockContext, defaultSettings)
        assertTrue(result is LocalCommandResult.NotHandled)
    }
}
