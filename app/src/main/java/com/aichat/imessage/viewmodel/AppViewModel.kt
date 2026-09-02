package com.aichat.imessage.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aichat.imessage.data.AVATAR_PALETTE
import com.aichat.imessage.data.AppSettings
import com.aichat.imessage.data.Attachment
import com.aichat.imessage.data.ChatMessage
import com.aichat.imessage.data.Conversation
import com.aichat.imessage.data.MessageDisplayMode
import com.aichat.imessage.data.OpenRouterApi
import com.aichat.imessage.data.Storage
import com.aichat.imessage.data.ThemeMode
import com.aichat.imessage.data.local.ChatRepository
import com.aichat.imessage.tools.AiAction
import com.aichat.imessage.tools.AiActionType
import com.aichat.imessage.tools.AiPermissionKey
import com.aichat.imessage.tools.AppLauncherHelper
import com.aichat.imessage.tools.CommunicationHelper
import com.aichat.imessage.tools.PermissionRequest
import com.aichat.imessage.tools.PremiumVoiceTtsHelper
import com.aichat.imessage.tools.SystemTaskHelper
import com.aichat.imessage.tools.VoskVoiceHelper
import com.aichat.imessage.tools.YouTubeHelper
import com.aichat.imessage.tools.ZipUtil
import com.aichat.imessage.tools.parseAiActions
import com.aichat.imessage.tools.resolvePermissionKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class ToastMessage(val text: String, val success: Boolean, val nonce: Long = System.currentTimeMillis())

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = Storage(application)
    private val repository = ChatRepository(application)

    private val _conversations = MutableStateFlow<MutableList<Conversation>>(mutableListOf())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _settings = MutableStateFlow(storage.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private val _showNewChat = MutableStateFlow(false)
    val showNewChat: StateFlow<Boolean> = _showNewChat.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // ---- Control de generación de IA y cancelación ("Detener Búsqueda") ----
    private var aiJob: Job? = null

    // ---- Control de voz y manos libres (STT + TTS Premium Femenina) ----
    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _handsFreeMode = MutableStateFlow(false)
    val handsFreeMode: StateFlow<Boolean> = _handsFreeMode.asStateFlow()

    // Configuración de la voz femenina premium
    var voicePitch = 1.15f
    var voiceSpeed = 0.98f

    // ---- Herramientas de la IA: permisos, adjuntos, zip, YouTube ----

    private val _permissionRequest = MutableStateFlow<PermissionRequest?>(null)
    val permissionRequest: StateFlow<PermissionRequest?> = _permissionRequest.asStateFlow()

    private val _systemPermissionToLaunch = MutableStateFlow<String?>(null)
    val systemPermissionToLaunch: StateFlow<String?> = _systemPermissionToLaunch.asStateFlow()

    private val _requestFilePicker = MutableStateFlow(false)
    val requestFilePicker: StateFlow<Boolean> = _requestFilePicker.asStateFlow()

    private val _shareFile = MutableStateFlow<File?>(null)
    val shareFile: StateFlow<File?> = _shareFile.asStateFlow()

    private var pendingPermissionConversationId: String? = null
    private var pendingPermissionLabel: String? = null

    init {
        PremiumVoiceTtsHelper.init(application)
        if (_settings.value.apiKey.isBlank()) {
            showToast("Configura tu clave de OpenRouter para empezar", true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.migrateLegacyJsonIfNeeded()
            val loaded = repository.loadAll()
            withContext(Dispatchers.Main) { _conversations.value = loaded }
        }
    }

    fun showToast(text: String, success: Boolean = true) {
        _toast.value = ToastMessage(text, success)
    }

    fun clearToast() {
        _toast.value = null
    }

    fun openNewChatDialog() { _showNewChat.value = true }
    fun closeNewChatDialog() { _showNewChat.value = false }

    fun openSettingsDialog() { _showSettings.value = true }
    fun closeSettingsDialog() { _showSettings.value = false }

    fun createConversation(name: String) {
        val finalName = name.trim().ifBlank { "Nuevo chat" }
        val conv = Conversation(
            id = "c" + System.currentTimeMillis().toString(36) + (100000..999999).random().toString(36),
            name = finalName,
            avatarColor = AVATAR_PALETTE[_conversations.value.size % AVATAR_PALETTE.size]
        )
        _conversations.value = (_conversations.value + conv).toMutableList()
        viewModelScope.launch(Dispatchers.IO) { repository.upsertConversation(conv) }
        _showNewChat.value = false
        _activeId.value = conv.id
    }

    fun deleteConversation(id: String) {
        _conversations.value = _conversations.value.filter { it.id != id }.toMutableList()
        viewModelScope.launch(Dispatchers.IO) { repository.deleteConversation(id) }
        if (_activeId.value == id) _activeId.value = null
    }

    fun openConversation(id: String) { _activeId.value = id }
    fun closeChatPanel() { _activeId.value = null }

    fun updateSettings(apiKey: String, model: String, theme: ThemeMode) {
        _settings.value = AppSettings(apiKey.trim(), model.trim().ifBlank { "openai/gpt-4o-mini" }, theme)
        storage.saveSettings(_settings.value)
        _showSettings.value = false
        showToast("Configuración guardada", true)
    }

    // ---- Detener Búsqueda / Cancelar Generación ----
    fun cancelAiRequest() {
        aiJob?.cancel()
        aiJob = null
        PremiumVoiceTtsHelper.stop()
        val id = _activeId.value ?: return
        updateConversation(id) { c -> c.pending = false }
        showToast("Búsqueda/Respuesta detenida", true)
    }

    // ---- Dictado por Voz ----
    fun startVoiceRecognition() {
        val context = getApplication<Application>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val currentId = _activeId.value ?: return
            _permissionRequest.value = PermissionRequest(
                conversationId = currentId,
                key = AiPermissionKey.MICROFONO,
                reason = "Se necesita permiso del micrófono para dictado por voz."
            )
            return
        }

        stopVoiceRecognition()

        VoskVoiceHelper.startListening(
            context = context,
            onResult = { text ->
                if (text.isNotBlank()) {
                    _spokenText.value = text
                }
            },
            onError = { _ ->
                startNativeSpeechRecognizer(context)
            }
        )
    }

    private fun startNativeSpeechRecognizer(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            showToast("Reconocimiento de voz no disponible", false)
            return
        }
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { _isListening.value = false }
                    override fun onError(error: Int) {
                        _isListening.value = false
                    }
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            _spokenText.value = text
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            _spokenText.value = text
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopVoiceRecognition() {
        VoskVoiceHelper.stopListening()
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
        _isListening.value = false
    }

    fun consumeSpokenText() {
        _spokenText.value = ""
    }

    fun toggleHandsFreeMode() {
        _handsFreeMode.value = !_handsFreeMode.value
        showToast(if (_handsFreeMode.value) "Modo manos libres activado" else "Modo manos libres desactivado", true)
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val id = _activeId.value ?: return
        if (trimmed.isEmpty()) return

        if (_settings.value.apiKey.isBlank()) {
            showToast("Primero configura tu clave de OpenRouter", false)
            _showSettings.value = true
            return
        }

        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                repository.insertMessage(id, ChatMessage(role = "user", content = trimmed, time = System.currentTimeMillis()))
            }
            updateConversation(id) { c -> c.messages.add(saved); c.updatedAt = System.currentTimeMillis() }
            persistMeta(id)
            requestAiReply(id)
        }
    }

    private fun requestAiReply(id: String) {
        updateConversation(id) { it.pending = true }

        aiJob = viewModelScope.launch {
            val conv = _conversations.value.find { it.id == id } ?: return@launch
            val apiKey = _settings.value.apiKey
            val model = _settings.value.model
            val messagesSnapshot = conv.messages.toList()

            try {
                val reply = withContext(Dispatchers.IO) {
                    OpenRouterApi.sendChat(apiKey, model, messagesSnapshot)
                }
                val parsed = parseAiActions(reply)
                if (parsed.displayText.isNotBlank()) {
                    val saved = withContext(Dispatchers.IO) {
                        repository.insertMessage(
                            id,
                            ChatMessage(role = "assistant", content = parsed.displayText, time = System.currentTimeMillis())
                        )
                    }
                    updateConversation(id) { c -> c.messages.add(saved) }
                    PremiumVoiceTtsHelper.speak(parsed.displayText, pitch = voicePitch, speed = voiceSpeed)
                }
                if (parsed.actions.isNotEmpty()) {
                    executeAiActions(id, parsed.actions)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    addToolLogMessage(id, "⏹️ Búsqueda/Respuesta detenida.")
                } else {
                    val saved = withContext(Dispatchers.IO) {
                        repository.insertMessage(
                            id,
                            ChatMessage(
                                role = "assistant",
                                content = "No se pudo obtener respuesta: ${e.message}",
                                time = System.currentTimeMillis(),
                                error = true
                            )
                        )
                    }
                    updateConversation(id) { c -> c.messages.add(saved) }
                    showToast("Error al contactar la IA", false)
                }
            } finally {
                updateConversation(id) { c ->
                    c.pending = false
                    c.updatedAt = System.currentTimeMillis()
                }
                persistMeta(id)
            }
        }
    }

    // ---- Ejecución de las acciones que la IA pide en su respuesta ----

    private fun executeAiActions(conversationId: String, actions: List<AiAction>) {
        val context = getApplication<Application>()
        actions.forEach { action ->
            when (action.type) {
                AiActionType.PERMISO -> {
                    val key = action.args.getOrNull(0)?.let { resolvePermissionKey(it) }
                    val reason = action.args.getOrNull(1) ?: "La IA necesita este permiso para ayudarte mejor."
                    if (key != null) {
                        _permissionRequest.value = PermissionRequest(conversationId, key, reason)
                    } else {
                        addToolLogMessage(conversationId, "⚠️ La IA pidió un permiso que no reconozco.")
                    }
                }

                AiActionType.ZIP -> {
                    val requestedName = action.args.getOrNull(0)?.trim()?.ifBlank { null } ?: "adjuntos"
                    val zipName = if (requestedName.endsWith(".zip")) requestedName else "$requestedName.zip"
                    val requestedFiles = action.args.getOrNull(1)?.split(",")?.map { it.trim() } ?: listOf("todos")
                    val conv = _conversations.value.find { it.id == conversationId }
                    val allAttachments = conv?.messages?.flatMap { it.attachments } ?: emptyList()
                    val toZip = if (requestedFiles.any { it.equals("todos", ignoreCase = true) }) {
                        allAttachments
                    } else {
                        allAttachments.filter { att -> requestedFiles.any { it.equals(att.fileName, ignoreCase = true) } }
                    }

                    if (toZip.isEmpty()) {
                        addToolLogMessage(conversationId, "⚠️ No encontré archivos adjuntos para comprimir en \"$zipName\".")
                    } else {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val outDir = File(context.filesDir, "exports").apply { mkdirs() }
                                val outFile = File(outDir, zipName)
                                ZipUtil.createZip(outFile, toZip.map { File(it.localPath) })
                                withContext(Dispatchers.Main) {
                                    addToolLogMessage(conversationId, "📦 Creé el archivo \"$zipName\" con ${toZip.size} archivo(s).")
                                    _shareFile.value = outFile
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    addToolLogMessage(conversationId, "⚠️ No pude crear el zip: ${e.message}")
                                }
                            }
                        }
                    }
                }

                AiActionType.YOUTUBE_BUSCAR -> {
                    val query = action.args.getOrNull(0).orEmpty()
                    if (query.isNotBlank()) {
                        YouTubeHelper.search(context, query)
                        addToolLogMessage(conversationId, "▶️ Abrí YouTube buscando: \"$query\".")
                    }
                }

                AiActionType.YOUTUBE_VIDEO -> {
                    val idOrUrl = action.args.getOrNull(0).orEmpty()
                    if (idOrUrl.isNotBlank()) {
                        YouTubeHelper.playVideo(context, idOrUrl)
                        addToolLogMessage(conversationId, "▶️ Reproduciendo video de YouTube.")
                    }
                }

                AiActionType.ABRIR_APP -> {
                    val target = action.args.getOrNull(0).orEmpty()
                    if (target.isNotBlank()) {
                        val success = AppLauncherHelper.openAppByNameOrPackage(context, target)
                        if (success) {
                            addToolLogMessage(conversationId, "📱 Abrí la aplicación: \"$target\".")
                        } else {
                            addToolLogMessage(conversationId, "⚠️ No pude encontrar ni abrir la aplicación: \"$target\".")
                        }
                    }
                }

                AiActionType.LISTAR_APPS -> {
                    val installed = AppLauncherHelper.getInstalledApps(context)
                    val appListText = installed.take(30).joinToString("\n") { "• ${it.name} (${it.packageName})" }
                    addToolLogMessage(conversationId, "📱 Aplicaciones instaladas encontradas:\n$appListText")
                }

                AiActionType.ENVIAR_SMS -> {
                    val phone = action.args.getOrNull(0).orEmpty()
                    val msg = action.args.getOrNull(1).orEmpty()
                    val success = CommunicationHelper.sendSms(context, phone, msg)
                    if (success) {
                        addToolLogMessage(conversationId, "💬 Abrí la app de SMS para enviar a $phone.")
                    } else {
                        addToolLogMessage(conversationId, "⚠️ No pude abrir la app de SMS.")
                    }
                }

                AiActionType.ENVIAR_MENSAJE -> {
                    val app = action.args.getOrNull(0).orEmpty()
                    val contact = action.args.getOrNull(1).orEmpty()
                    val msg = action.args.getOrNull(2).orEmpty()
                    val success = CommunicationHelper.sendMessageViaApp(context, app, contact, msg)
                    if (success) {
                        addToolLogMessage(conversationId, "📩 Preparando mensaje en $app.")
                    } else {
                        addToolLogMessage(conversationId, "⚠️ No se pudo abrir la app para enviar el mensaje.")
                    }
                }

                AiActionType.CREAR_ALARMA -> {
                    val h = action.args.getOrNull(0)?.toIntOrNull() ?: 8
                    val m = action.args.getOrNull(1)?.toIntOrNull() ?: 0
                    val msg = action.args.getOrNull(2).orEmpty()
                    val success = SystemTaskHelper.createAlarm(context, h, m, msg)
                    if (success) {
                        addToolLogMessage(conversationId, "⏰ Programé una alarma para las ${String.format("%02d:%02d", h, m)}.")
                    } else {
                        addToolLogMessage(conversationId, "⚠️ No pude programar la alarma.")
                    }
                }

                AiActionType.CREAR_TEMPORIZADOR -> {
                    val secs = action.args.getOrNull(0)?.toIntOrNull() ?: 60
                    val msg = action.args.getOrNull(1).orEmpty()
                    val success = SystemTaskHelper.createTimer(context, secs, msg)
                    if (success) {
                        addToolLogMessage(conversationId, "⏱️ Inicié un temporizador de $secs segundos.")
                    } else {
                        addToolLogMessage(conversationId, "⚠️ No pude iniciar el temporizador.")
                    }
                }

                AiActionType.LLAMAR -> {
                    val number = action.args.getOrNull(0).orEmpty()
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.trim()}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching {
                        context.startActivity(intent)
                        addToolLogMessage(conversationId, "📞 Abrí el marcador de llamadas para: $number.")
                    }.onFailure {
                        addToolLogMessage(conversationId, "⚠️ No se pudo abrir el marcador de llamadas.")
                    }
                }

                AiActionType.FLASHLIGHT -> {
                    val state = action.args.getOrNull(0)?.lowercase() ?: "on"
                    val turnOn = state == "on" || state == "encender"
                    try {
                        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val cameraId = cameraManager.cameraIdList.firstOrNull()
                        if (cameraId != null) {
                            cameraManager.setTorchMode(cameraId, turnOn)
                            addToolLogMessage(conversationId, if (turnOn) "🔦 Linterna encendida." else "🔦 Linterna apagada.")
                        }
                    } catch (e: Exception) {
                        addToolLogMessage(conversationId, "⚠️ No se pudo controlar la linterna.")
                    }
                }

                AiActionType.MUTE -> {
                    val state = action.args.getOrNull(0)?.lowercase() ?: "on"
                    val turnOn = state == "on" || state == "silencio"
                    try {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audioManager.ringerMode = if (turnOn) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
                        addToolLogMessage(conversationId, if (turnOn) "🔇 Modo silencio activado." else "🔊 Modo sonido normal activado.")
                    } catch (e: SecurityException) {
                        addToolLogMessage(conversationId, "⚠️ Se requiere permiso de No Molestar del sistema.")
                    } catch (e: Exception) {
                        addToolLogMessage(conversationId, "⚠️ No se pudo cambiar el modo de sonido.")
                    }
                }

                AiActionType.NOTAS -> {
                    val noteText = action.args.getOrNull(1) ?: action.args.getOrNull(0).orEmpty()
                    if (noteText.isNotBlank()) {
                        val notesDir = File(context.filesDir, "notes").apply { mkdirs() }
                        val noteFile = File(notesDir, "nota_${System.currentTimeMillis()}.txt")
                        noteFile.writeText(noteText)
                        addToolLogMessage(conversationId, "📝 Nota guardada en memoria local: \"${noteText.take(30)}...\"")
                    }
                }

                AiActionType.LEER_CONTACTO -> {
                    val searchName = action.args.getOrNull(0).orEmpty()
                    addToolLogMessage(conversationId, "📞 Búsqueda de contacto \"$searchName\" solicitada.")
                }

                AiActionType.BUSCAR_ARCHIVO -> {
                    val fileName = action.args.getOrNull(0).orEmpty()
                    val filesDir = context.filesDir
                    val matches = filesDir.walkTopDown().filter { it.isFile && it.name.contains(fileName, ignoreCase = true) }.take(5).toList()
                    if (matches.isNotEmpty()) {
                        val fileList = matches.joinToString("\n") { "• ${it.name}" }
                        addToolLogMessage(conversationId, "📁 Archivos encontrados:\n$fileList")
                    } else {
                        addToolLogMessage(conversationId, "📁 No se encontraron archivos locales con \"$fileName\".")
                    }
                }

                AiActionType.MODO_MANOS_LIBRES -> {
                    val state = action.args.getOrNull(0)?.lowercase() ?: "on"
                    _handsFreeMode.value = state == "on" || state == "activar"
                    addToolLogMessage(conversationId, if (_handsFreeMode.value) "🗣️ Modo manos libres activo." else "🗣️ Modo manos libres desactivado.")
                }
            }
        }
    }

    private fun addToolLogMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                repository.insertMessage(
                    conversationId,
                    ChatMessage(
                        role = "assistant",
                        content = text,
                        time = System.currentTimeMillis(),
                        displayMode = MessageDisplayMode.PLAIN
                    )
                )
            }
            updateConversation(conversationId) { c -> c.messages.add(saved); c.updatedAt = System.currentTimeMillis() }
            persistMeta(conversationId)
        }
    }

    // ---- Permisos ----

    fun respondToPermissionRequest(granted: Boolean) {
        val req = _permissionRequest.value ?: return
        _permissionRequest.value = null
        if (!granted) {
            addToolLogMessage(req.conversationId, "🔒 Denegaste el permiso de ${req.key.label}.")
            return
        }
        val androidPermission = req.key.androidPermission
        if (androidPermission == null) {
            addToolLogMessage(req.conversationId, "✅ Permiso de ${req.key.label} concedido.")
            return
        }
        pendingPermissionConversationId = req.conversationId
        pendingPermissionLabel = req.key.label
        _systemPermissionToLaunch.value = androidPermission
    }

    fun onSystemPermissionResult(granted: Boolean) {
        val convId = pendingPermissionConversationId
        val label = pendingPermissionLabel
        _systemPermissionToLaunch.value = null
        pendingPermissionConversationId = null
        pendingPermissionLabel = null
        if (convId != null && label != null) {
            addToolLogMessage(
                convId,
                if (granted) "✅ Permiso de $label concedido." else "🔒 El sistema denegó el permiso de $label."
            )
            if (granted && label == "Micrófono") {
                startVoiceRecognition()
            }
        }
    }

    // ---- Adjuntar archivos (guardado local) ----

    fun requestAttachFile() { _requestFilePicker.value = true }
    fun onFilePickerConsumed() { _requestFilePicker.value = false }

    fun onFilePicked(uri: Uri) {
        val id = _activeId.value ?: return
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                var fileName = "archivo_${System.currentTimeMillis()}"
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) fileName = cursor.getString(nameIdx)
                }
                val destDir = File(context.filesDir, "attachments").apply { mkdirs() }
                val destFile = File(destDir, "${System.currentTimeMillis()}_$fileName")
                resolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                val attachment = Attachment(
                    fileName = fileName,
                    localPath = destFile.absolutePath,
                    mimeType = mime,
                    sizeBytes = destFile.length()
                )
                val message = ChatMessage(
                    role = "user",
                    content = "📎 Adjunté \"$fileName\"",
                    time = System.currentTimeMillis(),
                    displayMode = MessageDisplayMode.PLAIN,
                    attachments = listOf(attachment)
                )
                val saved = repository.insertMessage(id, message)
                withContext(Dispatchers.Main) {
                    updateConversation(id) { c -> c.messages.add(saved); c.updatedAt = System.currentTimeMillis() }
                    persistMeta(id)
                    showToast("Archivo adjuntado: $fileName", true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showToast("No se pudo adjuntar el archivo", false) }
            }
        }
    }

    // ---- Exportar / compartir chat como zip ----

    fun exportConversation(id: String) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val conv = _conversations.value.find { it.id == id } ?: return@launch
            try {
                val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
                val sdf = SimpleDateFormat("HH:mm", Locale("es"))
                val transcript = File(exportDir, "transcripcion_${conv.id}.txt")
                transcript.writeText(
                    conv.messages.joinToString("\n\n") { m ->
                        val who = if (m.role == "user") "Tú" else "IA"
                        "$who (${sdf.format(java.util.Date(m.time))}): ${m.content}"
                    }
                )
                val attachmentFiles = conv.messages.flatMap { it.attachments }.map { File(it.localPath) }
                val safeName = conv.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val zipFile = File(exportDir, "chat_$safeName.zip")
                ZipUtil.createZip(zipFile, listOf(transcript) + attachmentFiles)
                withContext(Dispatchers.Main) { _shareFile.value = zipFile }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { showToast("No se pudo exportar el chat", false) }
            }
        }
    }

    fun onShareFileConsumed() { _shareFile.value = null }

    override fun onCleared() {
        PremiumVoiceTtsHelper.shutdown()
        super.onCleared()
    }

    // ---- Utilidades internas ----

    private fun updateConversation(id: String, block: (Conversation) -> Unit) {
        val list = _conversations.value.map { c ->
            if (c.id == id) {
                val copy = c.copy(messages = c.messages.toMutableList())
                block(copy)
                copy
            } else c
        }.toMutableList()
        _conversations.value = list
    }

    private fun persistMeta(id: String) {
        val conv = _conversations.value.find { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) { repository.upsertConversation(conv) }
    }
}
