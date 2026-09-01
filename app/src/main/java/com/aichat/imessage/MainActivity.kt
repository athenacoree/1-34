package com.aichat.imessage

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.aichat.imessage.ui.AppRoot
import com.aichat.imessage.ui.theme.AIChatTheme
import com.aichat.imessage.ui.theme.LocalAIChatColors
import com.aichat.imessage.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val context = LocalContext.current

            // Permiso de sistema pedido por la IA (cámara, micrófono, archivos, notificaciones)
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> viewModel.onSystemPermissionResult(granted) }

            val systemPermissionToLaunch by viewModel.systemPermissionToLaunch.collectAsState()
            LaunchedEffect(systemPermissionToLaunch) {
                systemPermissionToLaunch?.let { permissionLauncher.launch(it) }
            }

            // Selector de archivos para adjuntar al chat (se guardan en la base de datos local)
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

            // Compartir un .zip generado (ya sea por la IA o por "Exportar chat")
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
                    AppRoot(viewModel)
                }
            }
        }
    }
}
