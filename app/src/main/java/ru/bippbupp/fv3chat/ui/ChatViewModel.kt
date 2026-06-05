package ru.bippbupp.fv3chat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.bippbupp.fv3chat.R
import ru.bippbupp.fv3chat.data.database.AppDatabase
import ru.bippbupp.fv3chat.data.model.ChatMessage
import ru.bippbupp.fv3chat.data.network.AuthSession
import ru.bippbupp.fv3chat.data.network.ChatApi
import ru.bippbupp.fv3chat.data.network.NetworkModule
import ru.bippbupp.fv3chat.data.network.NetworkMonitor
import ru.bippbupp.fv3chat.data.repository.AuthExpiredException
import ru.bippbupp.fv3chat.data.repository.ChatNetworkException
import ru.bippbupp.fv3chat.data.repository.ChatServerException
import ru.bippbupp.fv3chat.data.repository.InvalidCredentialsException
import ru.bippbupp.fv3chat.data.repository.OfflineFirstChatRepository
import ru.bippbupp.fv3chat.data.storage.CredentialsStore
import java.io.IOException

private const val PAGE_SIZE = 20

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val session = AuthSession()
    private val database = AppDatabase.getInstance(application)
    private val repository = OfflineFirstChatRepository(
        api = NetworkModule.createApi(session) as ChatApi,
        session = session,
        database = database
    )
    private val credentialsStore = CredentialsStore(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var isNetworkAvailable = false

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

        viewModelScope.launch {
            networkMonitor.isConnected().collect { connected ->
                val wasOffline = !isNetworkAvailable
                isNetworkAvailable = connected
                if (connected && wasOffline) {
                    syncAfterNetworkRestored()
                }
            }
        }
    }

    private suspend fun syncAfterNetworkRestored() {
        processPendingMessages()
        if (_uiState.value.isLoggedIn) {
            refreshChannelsSilently()
            _uiState.value.selectedChat?.let { channel ->
                refreshMessagesSilently(channel)
            }
        }
    }

    private suspend fun processPendingMessages() {
        val pending = repository.getAllPendingMessages()
        if (pending.isEmpty()) return
        val state = _uiState.value
        val username = state.username
        if (username.isBlank()) return

        for (msg in pending) {
            try {
                val newId = repository.sendMessageOnline(username, msg.channel, msg.text)
                repository.deletePendingMessage(msg.uid)
                _uiState.update { current ->
                    val messages = current.messagesByChat[msg.channel].orEmpty()
                    val updated = messages.map { message ->
                        if (message.id.startsWith("pending_") &&
                            message.text == msg.text &&
                            message.time?.toLongOrNull() == msg.timestamp
                        ) {
                            message.copy(id = newId.ifBlank { message.id })
                        } else message
                    }
                    current.copy(
                        messagesByChat = current.messagesByChat + (msg.channel to updated)
                    )
                }
            } catch (e: Exception) {
                if (e is AuthExpiredException) {
                    _uiState.update {
                        ChatUiState(loginInput = username, errorMessage = text(R.string.auth_expired))
                    }
                    return
                }
            }
        }
    }

    private suspend fun refreshChannelsSilently() {
        runCatching { repository.refreshChannels() }
            .onSuccess { channels ->
                _uiState.update { it.copy(channels = channels) }
            }
            .onFailure { }
    }

    private suspend fun refreshMessagesSilently(channel: String) {
        runCatching { repository.refreshMessages(channel) }
            .onSuccess { fresh ->
                _uiState.update { current ->
                    current.copy(
                        messagesByChat = current.messagesByChat.withMergedMessages(channel, fresh),
                        canLoadMore = fresh.size == PAGE_SIZE
                    )
                }
            }
            .onFailure { }
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
        _uiState.update {
            it.copy(
                selectedChat = channel,
                fullImagePath = null,
            )
        }
        refreshCachedMessages(channel)
        if (isNetworkAvailable) {
            viewModelScope.launch { refreshMessagesSilently(channel) }
        }
    }

    private fun refreshCachedMessages(channel: String) {
        viewModelScope.launch {
            val cached = repository.getCachedMessages(channel)
            if (cached.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        messagesByChat = it.messagesByChat + (channel to cached),
                        canLoadMore = cached.size == PAGE_SIZE
                    )
                }
            }
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
        val oldestId = state.messagesByChat[channel].orEmpty().firstOrNull()?.id ?: return
        if (state.isMoreLoading || !state.canLoadMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMoreLoading = true) }
            runCatching { repository.loadOlderMessages(channel, oldestId) }
                .onSuccess { older ->
                    _uiState.update { current ->
                        val existing = current.messagesByChat[channel].orEmpty()
                        val merged = (older + existing).distinctBy { it.id }
                            .sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
                        current.copy(
                            messagesByChat = current.messagesByChat + (channel to merged),
                            canLoadMore = older.size == PAGE_SIZE,
                            isMoreLoading = false
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
            runCatching { repository.sendMessageOnline(state.username, channel, text) }
                .onSuccess { id ->
                    val message = ChatMessage(
                        id = id.ifBlank { System.currentTimeMillis().toString() },
                        from = state.username,
                        to = channel,
                        text = text,
                        imagePath = null,
                        time = (System.currentTimeMillis() / 1000L).toString()
                    )
                    _uiState.update { current ->
                        current.copy(
                            messagesByChat = current.messagesByChat.withMergedMessages(channel, listOf(message)),
                            messageInput = "",
                            isSending = false
                        )
                    }
                }
                .onFailure { error ->
                    if (error is ChatNetworkException || error is IOException) {
                        val tempId = "pending_${System.currentTimeMillis()}"
                        val tempMessage = ChatMessage(
                            id = tempId,
                            from = state.username,
                            to = channel,
                            text = text,
                            imagePath = null,
                            time = (System.currentTimeMillis() / 1000L).toString()
                        )
                        _uiState.update { current ->
                            current.copy(
                                messagesByChat = current.messagesByChat.withMergedMessages(channel, listOf(tempMessage)),
                                messageInput = "",
                                isSending = false,
                                errorMessage = text(R.string.message_saved_offline)
                            )
                        }
                        repository.savePendingMessage(channel, text, state.username)
                    } else {
                        handleFailure(error, isSending = true)
                    }
                }
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
            val cached = repository.getCachedChannels()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(channels = cached, isChannelsLoading = false) }
            }
            runCatching { repository.refreshChannels() }
                .onSuccess { fresh ->
                    _uiState.update { it.copy(channels = fresh, isChannelsLoading = false) }
                }
                .onFailure { error ->
                    if (cached.isEmpty()) {
                        handleFailure(error, isChannelsLoading = true)
                    } else {
                        _uiState.update { it.copy(errorMessage = text(R.string.network_error)) }
                    }
                }
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
    this + (channel to (get(channel).orEmpty() + messages).distinctBy { it.id }
        .sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE })