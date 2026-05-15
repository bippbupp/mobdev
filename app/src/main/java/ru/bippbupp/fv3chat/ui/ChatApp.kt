package ru.bippbupp.fv3chat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.bippbupp.fv3chat.R
import ru.bippbupp.fv3chat.data.model.ChatMessage
import ru.bippbupp.fv3chat.data.network.NetworkModule

@Composable
fun ChatApp(
    state: ChatUiState,
    isLandscape: Boolean,
    onLoginChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCloseChat: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenImage: (String) -> Unit,
    onCloseImage: () -> Unit,
    onDismissError: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when {
            !state.isLoggedIn -> LoginScreen(
                state = state,
                onLoginChanged = onLoginChanged,
                onPasswordChanged = onPasswordChanged,
                onLogin = onLogin,
            )
            isLandscape -> LandscapeChatScreen(
                state = state,
                onLogout = onLogout,
                onOpenChat = onOpenChat,
                onMessageChanged = onMessageChanged,
                onSend = onSend,
                onLoadMore = onLoadMore,
                onOpenImage = onOpenImage,
            )
            state.selectedChat == null -> ChatsScreen(
                state = state,
                onLogout = onLogout,
                onOpenChat = onOpenChat,
            )
            else -> MessagesScreen(
                state = state,
                showBackButton = true,
                onCloseChat = onCloseChat,
                onMessageChanged = onMessageChanged,
                onSend = onSend,
                onLoadMore = onLoadMore,
                onOpenImage = onOpenImage,
            )
        }

        state.fullImagePath?.let { imagePath ->
            FullImageScreen(imagePath = imagePath, onClose = onCloseImage)
        }

        state.errorMessage?.let { message ->
            ErrorDialog(message = message, onDismiss = onDismissError)
        }
    }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.ok))
            }
        },
        title = { Text(text = stringResource(R.string.error_title)) },
        text = { Text(text = message) },
    )
}

@Composable
private fun LoginScreen(
    state: ChatUiState,
    onLoginChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = state.loginInput,
            onValueChange = onLoginChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(text = stringResource(R.string.login)) },
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.passwordInput,
            onValueChange = onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text(text = stringResource(R.string.password)) },
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoginLoading && !state.isAutoLogin,
        ) {
            if (state.isLoginLoading || state.isAutoLogin) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.sign_in))
            }
        }
    }
}

@Composable
private fun LandscapeChatScreen(
    state: ChatUiState,
    onLogout: () -> Unit,
    onOpenChat: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenImage: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(320.dp)) {
            ChatsScreen(
                state = state,
                onLogout = onLogout,
                onOpenChat = onOpenChat,
            )
        }
        if (state.selectedChat == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.choose_chat),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                MessagesScreen(
                    state = state,
                    showBackButton = false,
                    onCloseChat = {},
                    onMessageChanged = onMessageChanged,
                    onSend = onSend,
                    onLoadMore = onLoadMore,
                    onOpenImage = onOpenImage,
                )
            }
        }
    }
}

@Composable
private fun ChatsScreen(
    state: ChatUiState,
    onLogout: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.channels),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(onClick = onLogout) {
                Text(text = stringResource(R.string.logout))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when {
            state.isChannelsLoading -> LoadingContent()
            state.channels.isEmpty() -> EmptyContent(text = stringResource(R.string.empty_channels))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.channels, key = { it }) { channel ->
                    ChannelRow(
                        channel = channel,
                        selected = channel == state.selectedChat,
                        onClick = { onOpenChat(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: String, selected: Boolean, onClick: () -> Unit) {
    val colors = if (selected) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.elevatedCardColors()
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = colors,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = channel,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Text(
                    text = stringResource(R.string.selected_chat),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MessagesScreen(
    state: ChatUiState,
    showBackButton: Boolean,
    onCloseChat: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onSend: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenImage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackButton) {
                OutlinedButton(onClick = onCloseChat) {
                    Text(text = stringResource(R.string.back_to_chats))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = state.selectedChat.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        MessageList(
            state = state,
            modifier = Modifier.weight(1f),
            onLoadMore = onLoadMore,
            onOpenImage = onOpenImage,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MessageInput(
            value = state.messageInput,
            isSending = state.isSending,
            onValueChange = onMessageChanged,
            onSend = onSend,
        )
    }
}

@Composable
private fun MessageInput(
    value: String,
    isSending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(text = stringResource(R.string.message_hint)) },
            maxLines = 4,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = value.isNotBlank() && !isSending,
        ) {
            Text(text = stringResource(R.string.send))
        }
    }
}

@Composable
private fun MessageList(
    state: ChatUiState,
    modifier: Modifier,
    onLoadMore: () -> Unit,
    onOpenImage: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    when {
        state.isMessagesLoading -> Box(modifier = modifier.fillMaxWidth()) {
            LoadingContent()
        }
        state.visibleMessages.isEmpty() -> Box(modifier = modifier.fillMaxWidth()) {
            EmptyContent(text = stringResource(R.string.empty_messages))
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                LoadMoreRow(
                    canLoadMore = state.canLoadMore,
                    isMoreLoading = state.isMoreLoading,
                    onLoadMore = onLoadMore,
                )
            }
            items(state.visibleMessages, key = { it.id }) { message ->
                MessageRow(message = message, onOpenImage = onOpenImage)
            }
        }
    }
}

@Composable
private fun LoadMoreRow(
    canLoadMore: Boolean,
    isMoreLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    if (canLoadMore) {
        OutlinedButton(
            onClick = onLoadMore,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isMoreLoading,
        ) {
            if (isMoreLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.load_more))
            }
        }
    } else {
        Text(
            text = stringResource(R.string.no_more_messages),
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MessageRow(message: ChatMessage, onOpenImage: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = message.from,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            message.text?.let { text ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
            }
            message.imagePath?.let { path ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = NetworkModule.thumbUrl(path),
                    contentDescription = stringResource(R.string.image_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { onOpenImage(path) },
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun FullImageScreen(imagePath: String, onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            AsyncImage(
                model = NetworkModule.imageUrl(imagePath),
                contentDescription = stringResource(R.string.image_message),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(text = stringResource(R.string.close_image))
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.loading))
        }
    }
}

@Composable
private fun EmptyContent(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
