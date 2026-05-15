package ru.bippbupp.fv3chat.ui

import ru.bippbupp.fv3chat.data.model.ChatMessage

data class ChatUiState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val loginInput: String = "",
    val passwordInput: String = "",
    val channels: List<String> = emptyList(),
    val selectedChat: String? = null,
    val messagesByChat: Map<String, List<ChatMessage>> = emptyMap(),
    val messageInput: String = "",
    val fullImagePath: String? = null,
    val canLoadMore: Boolean = true,
    val isAutoLogin: Boolean = false,
    val isLoginLoading: Boolean = false,
    val isChannelsLoading: Boolean = false,
    val isMessagesLoading: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
) {
    val visibleMessages: List<ChatMessage>
        get() = selectedChat?.let { messagesByChat[it] }.orEmpty()
}
