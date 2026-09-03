package com.aichat.imessage

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.aichat.imessage.ui.AppRoot
import com.aichat.imessage.ui.theme.AIChatTheme
import com.aichat.imessage.ui.theme.LocalAIChatColors
import com.aichat.imessage.viewmodel.AppViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: AppViewModel by viewModels()
    private val isAuthenticated = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(settings.biometricLockEnabled) {
                if (settings.biometricLockEnabled && !isAuthenticated.value) {
                    showBiometricPrompt()
                } else {
                    isAuthenticated.value = true
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> viewModel.onSystemPermissionResult(granted) }

            val systemPermissionToLaunch by viewModel.systemPermissionToLaunch.collectAsState()
            LaunchedEffect(systemPermissionToLaunch) {
                systemPermissionToLaunch?.let { permissionLauncher.launch(it) }
            }

            val filePickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri -> uri?.let { viewModel.onFilePicked(it) } }

            val requestFilePicker by viewModel.requestFilePicker.collectAsState()
            LaunchedEffect(requestFilePicker) {
                if (requestFilePicker) {
                    filePickerLauncher.launch("*/*")
                    viewModel.onFilePickerConsumed()
                }
            }

            val shareFile by viewModel.shareFile.collectAsState()
            LaunchedEffect(shareFile) {
                shareFile?.let { file ->
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir"))
                    viewModel.onShareFileConsumed()
                }
            }

            AIChatTheme(themeMode = settings.theme) {
                Surface(modifier = Modifier.fillMaxSize(), color = LocalAIChatColors.current.bg) {
                    if (settings.biometricLockEnabled && !isAuthenticated.value) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(LocalAIChatColors.current.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔒 AI Chat está bloqueada", color = LocalAIChatColors.current.text, fontSize = 18.sp)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { showBiometricPrompt() }) {
                                    Text("Desbloquear con PIN / Huella")
                                }
                            }
                        }
                    } else {
                        AppRoot(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOfBlank()) {
                    viewModel.handleSharedContent(sharedText, null)
                }
            } else if (type.startsWith("image/")) {
                val imageUri = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                if (imageUri != null) {
                    viewModel.handleSharedContent(null, imageUri)
                }
            }
        } else if (intent.getStringExtra("action") == "new_chat") {
            viewModel.createConversation("Nuevo chat")
        } else if (intent.getStringExtra("action") == "voice_chat") {
            val convs = viewModel.conversations.value
            if (convs.isEmpty()) {
                viewModel.createConversation("Chat de voz")
            } else {
                viewModel.openConversation(convs.first().id)
            }
            viewModel.startVoiceRecognition()
        }
    }

    private fun showBiometricPrompt() {
        try {
            val executor = ContextCompat.getMainExecutor(this)
            val biometricPrompt = BiometricPrompt(this, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        isAuthenticated.value = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("AI Chat Bloqueada")
                .setSubtitle("Autentícate para acceder a tus chats")
                .setNegativeButtonText("Cancelar")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            isAuthenticated.value = true
        }
    }
}

private fun String?.isNullOfBlank(): Boolean = this == null || this.trim().isEmpty()
