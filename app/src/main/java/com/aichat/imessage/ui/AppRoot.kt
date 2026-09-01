package com.aichat.imessage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aichat.imessage.viewmodel.AppViewModel

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val activeId by viewModel.activeId.collectAsState()
    val permissionRequest by viewModel.permissionRequest.collectAsState()

    BackHandler(enabled = activeId != null) {
        viewModel.closeChatPanel()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(220)) { it } togetherWith slideOutHorizontally(tween(220)) { -it / 3 })
                } else {
                    (slideInHorizontally(tween(220)) { -it / 3 } togetherWith slideOutHorizontally(tween(220)) { it })
                }
            },
            label = "chatNav"
        ) { id ->
            if (id == null) {
                ChatListScreen(viewModel)
            } else {
                ChatScreen(viewModel, id)
            }
        }

        ToastHost(viewModel)

        permissionRequest?.let { req ->
            PermissionRequestDialog(request = req, onRespond = { granted -> viewModel.respondToPermissionRequest(granted) })
        }
    }
}
