package ru.bippbupp.fv3chat

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.bippbupp.fv3chat.ui.ChatApp
import ru.bippbupp.fv3chat.ui.ChatViewModel
import ru.bippbupp.fv3chat.ui.theme.FV3ChatTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FV3ChatTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val isLandscape = LocalConfiguration.current.orientation ==
                    Configuration.ORIENTATION_LANDSCAPE

                BackHandler {
                    if (!viewModel.handleBack()) {
                        finish()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatApp(
                        state = state,
                        isLandscape = isLandscape,
                        onLoginChanged = viewModel::onLoginChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onLogin = viewModel::loginFromInput,
                        onLogout = viewModel::logout,
                        onOpenChat = viewModel::openChat,
                        onCloseChat = viewModel::closeChat,
                        onMessageChanged = viewModel::onMessageChanged,
                        onSend = viewModel::sendMessage,
                        onLoadMore = viewModel::loadOlderMessages,
                        onOpenImage = viewModel::openImage,
                        onCloseImage = viewModel::closeImage,
                        onDismissError = viewModel::dismissError,
                    )
                }
            }
        }
    }
}
