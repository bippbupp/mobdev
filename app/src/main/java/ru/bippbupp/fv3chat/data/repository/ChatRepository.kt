package ru.bippbupp.fv3chat.data.repository

import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response
import ru.bippbupp.fv3chat.data.model.ChatMessage
import ru.bippbupp.fv3chat.data.model.LoginRequest
import ru.bippbupp.fv3chat.data.model.MessageDataDto
import ru.bippbupp.fv3chat.data.model.MessageDto
import ru.bippbupp.fv3chat.data.model.TextPayload
import ru.bippbupp.fv3chat.data.model.toDomain
import ru.bippbupp.fv3chat.data.network.AuthSession
import ru.bippbupp.fv3chat.data.network.ChatApi

private const val UNAUTHORIZED = 401
private const val PAGE_SIZE = 20

class InvalidCredentialsException : Exception()
class AuthExpiredException : Exception()
class ChatNetworkException(cause: Throwable) : Exception(cause)
class ChatServerException(val code: Int, message: String?) : Exception(message)

class ChatRepository(
    private val api: ChatApi,
    private val session: AuthSession,
) {
    suspend fun login(name: String, password: String): String {
        val response = wrapNetwork {
            api.login(LoginRequest(name = name, pwd = password))
        }
        if (response.code() == UNAUTHORIZED) {
            throw InvalidCredentialsException()
        }
        ensureSuccess(response)
        val token = response.body()?.string()?.trim()?.trim('"').orEmpty()
        if (token.isBlank()) {
            throw ChatServerException(response.code(), response.message())
        }
        session.token = token
        return token
    }

    suspend fun channels(): List<String> = authorized {
        api.channels().sorted()
    }

    suspend fun messages(
        channel: String,
        lastKnownId: String = "0",
        reverse: Boolean = false,
    ): List<ChatMessage> = authorized {
        api.channelMessages(
            name = channel,
            limit = PAGE_SIZE,
            lastKnownId = lastKnownId,
            reverse = reverse,
        )
            .mapNotNull { it.toDomain() }
            .sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
    }

    suspend fun sendText(username: String, channel: String, text: String): String {
        val response = authorizedResponse {
            api.sendMessage(
                MessageDto(
                    from = username,
                    to = channel,
                    data = MessageDataDto(text = TextPayload(text = text)),
                ),
            )
        }
        return response.body()?.string()?.trim().orEmpty()
    }

    suspend fun logout() {
        runCatching {
            authorizedResponse { api.logout() }
        }
        session.token = null
    }

    private suspend fun <T> authorized(block: suspend () -> T): T =
        try {
            wrapNetwork(block)
        } catch (exception: HttpException) {
            if (exception.code() == UNAUTHORIZED) {
                session.token = null
                throw AuthExpiredException()
            }
            throw ChatServerException(exception.code(), exception.message())
        }

    private suspend fun <T> authorizedResponse(block: suspend () -> Response<T>): Response<T> {
        val response = wrapNetwork(block)
        if (response.code() == UNAUTHORIZED) {
            session.token = null
            throw AuthExpiredException()
        }
        ensureSuccess(response)
        return response
    }

    private suspend fun <T> wrapNetwork(block: suspend () -> T): T =
        try {
            block()
        } catch (exception: IOException) {
            throw ChatNetworkException(exception)
        }

    private fun <T> ensureSuccess(response: Response<T>) {
        if (!response.isSuccessful) {
            throw ChatServerException(response.code(), response.message())
        }
    }
}
