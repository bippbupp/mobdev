package ru.bippbupp.fv3chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.bippbupp.fv3chat.R
import ru.bippbupp.fv3chat.data.model.ChatMessage
import ru.bippbupp.fv3chat.data.network.AuthSession
import ru.bippbupp.fv3chat.data.network.NetworkModule
import ru.bippbupp.fv3chat.data.repository.AuthExpiredException
import ru.bippbupp.fv3chat.data.repository.ChatNetworkException
import ru.bippbupp.fv3chat.data.repository.ChatRepository
import ru.bippbupp.fv3chat.data.repository.ChatServerException
import ru.bippbupp.fv3chat.data.repository.InvalidCredentialsException
import ru.bippbupp.fv3chat.data.storage.CredentialsStore

private const val PAGE_SIZE = 20

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val session = AuthSession()
    private val repository = ChatRepository(NetworkModule.createApi(session), session)
    private val credentialsStore = CredentialsStore(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        credentialsStore.read()?.let { credentials ->
            _uiState.update {
                it.copy(
                    loginInput = credentials.username,
                    passwordInput = credentials.password,
                    isAutoLogin = true,
                )
            }
            login(credentials.username, credentials.password, isAutomatic = true)
        }
    }

    fun onLoginChanged(value: String) {
        _uiState.update { it.copy(loginInput = value) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(passwordInput = value) }
    }

    fun onMessageChanged(value: String) {
        _uiState.update { it.copy(messageInput = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loginFromInput() {
        val state = _uiState.value
        login(state.loginInput.trim(), state.passwordInput, isAutomatic = false)
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            credentialsStore.clear()
            _uiState.value = ChatUiState()
        }
    }

    fun openChat(channel: String) {
        val alreadyLoaded = _uiState.value.messagesByChat.containsKey(channel)
        _uiState.update {
            it.copy(
                selectedChat = channel,
                fullImagePath = null,
                canLoadMore = it.messagesByChat[channel]?.size == PAGE_SIZE,
            )
        }
        if (!alreadyLoaded) {
            loadMessages(channel)
        }
    }

    fun closeChat() {
        _uiState.update { it.copy(selectedChat = null, fullImagePath = null) }
    }

    fun openImage(path: String) {
        _uiState.update { it.copy(fullImagePath = path) }
    }

    fun closeImage() {
        _uiState.update { it.copy(fullImagePath = null) }
    }

    fun loadOlderMessages() {
        val state = _uiState.value
        val channel = state.selectedChat ?: return
        val oldestId = state.messagesByChat[channel]
            .orEmpty()
            .firstOrNull()
            ?.id
            ?: return
        if (state.isMoreLoading || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMoreLoading = true) }
            runCatching { repository.messages(channel, lastKnownId = oldestId, reverse = true) }
                .onSuccess { older ->
                    _uiState.update { current ->
                        current.copy(
                            messagesByChat = current.messagesByChat.withMergedMessages(
                                channel = channel,
                                messages = older,
                            ),
                            canLoadMore = older.size == PAGE_SIZE,
                            isMoreLoading = false,
                        )
                    }
                }
                .onFailure { handleFailure(it, isMoreLoading = true) }
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val channel = state.selectedChat ?: return
        val text = state.messageInput.trim()
        if (text.isEmpty() || state.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            runCatching { repository.sendText(state.username, channel, text) }
                .onSuccess { newId ->
                    val id = newId.ifBlank { System.currentTimeMillis().toString() }
                    val message = ChatMessage(
                        id = id,
                        from = state.username,
                        to = channel,
                        text = text,
                        imagePath = null,
                        time = (System.currentTimeMillis() / 1000L).toString(),
                    )
                    _uiState.update { current ->
                        current.copy(
                            messagesByChat = current.messagesByChat.withMergedMessages(
                                channel = channel,
                                messages = listOf(message),
                            ),
                            messageInput = "",
                            isSending = false,
                        )
                    }
                }
                .onFailure { handleFailure(it, isSending = true) }
        }
    }

    fun handleBack(): Boolean {
        val state = _uiState.value
        return when {
            state.fullImagePath != null -> {
                closeImage()
                true
            }
            state.selectedChat != null -> {
                closeChat()
                true
            }
            else -> false
        }
    }

    private fun login(username: String, password: String, isAutomatic: Boolean) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(
                    isAutoLogin = false,
                    errorMessage = text(R.string.empty_login_password),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoginLoading = true,
                    isAutoLogin = isAutomatic,
                    errorMessage = null,
                )
            }
            runCatching { repository.login(username, password) }
                .onSuccess {
                    credentialsStore.save(username, password)
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            username = username,
                            loginInput = username,
                            passwordInput = password,
                            isLoginLoading = false,
                            isAutoLogin = false,
                        )
                    }
                    loadChannels()
                }
                .onFailure { handleFailure(it, isLoginLoading = true) }
        }
    }

    private fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChannelsLoading = true) }
            runCatching { repository.channels() }
                .onSuccess { channels ->
                    _uiState.update {
                        it.copy(
                            channels = channels,
                            isChannelsLoading = false,
                        )
                    }
                }
                .onFailure { handleFailure(it, isChannelsLoading = true) }
        }
    }

    private fun loadMessages(channel: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMessagesLoading = true, canLoadMore = true) }
            runCatching { repository.messages(channel) }
                .onSuccess { messages ->
                    _uiState.update {
                        it.copy(
                            messagesByChat = it.messagesByChat + (channel to messages.sortedById()),
                            isMessagesLoading = false,
                            canLoadMore = messages.size == PAGE_SIZE,
                        )
                    }
                }
                .onFailure { handleFailure(it, isMessagesLoading = true) }
        }
    }

    private fun handleFailure(
        throwable: Throwable,
        isLoginLoading: Boolean = false,
        isChannelsLoading: Boolean = false,
        isMessagesLoading: Boolean = false,
        isMoreLoading: Boolean = false,
        isSending: Boolean = false,
    ) {
        val message = when (throwable) {
            is InvalidCredentialsException -> text(R.string.wrong_credentials)
            is AuthExpiredException -> text(R.string.auth_expired)
            is ChatNetworkException -> text(R.string.network_error)
            is ChatServerException -> text(R.string.server_error)
            else -> text(R.string.unknown_error)
        }

        if (throwable is AuthExpiredException) {
            credentialsStore.clear()
            session.token = null
            _uiState.update { current ->
                ChatUiState(
                    loginInput = current.username.ifBlank { current.loginInput },
                    errorMessage = message,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoginLoading = if (isLoginLoading) false else it.isLoginLoading,
                isChannelsLoading = if (isChannelsLoading) false else it.isChannelsLoading,
                isMessagesLoading = if (isMessagesLoading) false else it.isMessagesLoading,
                isMoreLoading = if (isMoreLoading) false else it.isMoreLoading,
                isSending = if (isSending) false else it.isSending,
                isAutoLogin = false,
                errorMessage = message,
            )
        }
    }

    private fun text(id: Int): String = getApplication<Application>().getString(id)
}

private fun Map<String, List<ChatMessage>>.withMergedMessages(
    channel: String,
    messages: List<ChatMessage>,
): Map<String, List<ChatMessage>> =
    this + (channel to (get(channel).orEmpty() + messages).distinctBy { it.id }.sortedById())

private fun List<ChatMessage>.sortedById(): List<ChatMessage> =
    sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
