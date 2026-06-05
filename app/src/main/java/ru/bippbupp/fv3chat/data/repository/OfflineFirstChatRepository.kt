package ru.bippbupp.fv3chat.data.repository

import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response
import ru.bippbupp.fv3chat.data.database.AppDatabase
import ru.bippbupp.fv3chat.data.database.ChannelEntity
import ru.bippbupp.fv3chat.data.database.MessageEntity
import ru.bippbupp.fv3chat.data.database.PendingMessageEntity
import ru.bippbupp.fv3chat.data.database.toEntity
import ru.bippbupp.fv3chat.data.model.ChatMessage
import ru.bippbupp.fv3chat.data.model.LoginRequest
import ru.bippbupp.fv3chat.data.model.MessageDataDto
import ru.bippbupp.fv3chat.data.model.MessageDto
import ru.bippbupp.fv3chat.data.model.TextPayload
import ru.bippbupp.fv3chat.data.model.toDomain
import ru.bippbupp.fv3chat.data.network.AuthSession
import ru.bippbupp.fv3chat.data.network.ChatApi

private const val PAGE_SIZE = 20
private const val UNAUTHORIZED = 401

class OfflineFirstChatRepository(
    private val api: ChatApi,
    private val session: AuthSession,
    private val database: AppDatabase
) {
    private val dao = database.chatDao()

    suspend fun getCachedChannels(): List<String> = dao.getAllChannels()

    suspend fun refreshChannels(): List<String> {
        return try {
            val freshChannels = authorized { api.channels().sorted() }
            dao.clearChannels()
            dao.insertChannels(freshChannels.map { ChannelEntity(it) })
            freshChannels
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getCachedMessages(channel: String): List<ChatMessage> =
        dao.getMessagesForChannel(channel).map { it.toDomain() }

    suspend fun refreshMessages(channel: String): List<ChatMessage> {
        val fresh = authorized {
            api.channelMessages(channel, PAGE_SIZE, "0", false)
                .mapNotNull { it.toDomain() }
                .sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
        }
        val existing = dao.getMessagesForChannel(channel).map { it.toDomain() }
        val merged = (existing + fresh).distinctBy { it.id }
            .sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
        dao.clearMessagesForChannel(channel)
        dao.insertMessages(merged.map { it.toEntity() })
        return fresh
    }

    suspend fun loadOlderMessages(channel: String, oldestId: String): List<ChatMessage> {
        val older = authorized {
            api.channelMessages(channel, PAGE_SIZE, oldestId, true)
                .mapNotNull { it.toDomain() }
                .sortedBy { it.id.toLongOrNull() ?: Long.MAX_VALUE }
        }
        if (older.isNotEmpty()) {
            dao.insertMessages(older.map { it.toEntity() })
        }
        return older
    }

    suspend fun sendMessageOnline(username: String, channel: String, text: String): String {
        val response = authorizedResponse {
            api.sendMessage(
                MessageDto(
                    from = username,
                    to = channel,
                    data = MessageDataDto(text = TextPayload(text = text))
                )
            )
        }
        val id = response.body()?.string()?.trim().orEmpty()
        val message = ChatMessage(
            id = id.ifBlank { System.currentTimeMillis().toString() },
            from = username,
            to = channel,
            text = text,
            imagePath = null,
            time = (System.currentTimeMillis() / 1000L).toString()
        )
        dao.insertMessage(message.toEntity())
        return id
    }

    suspend fun savePendingMessage(channel: String, text: String, from: String) {
        dao.insertPendingMessage(PendingMessageEntity(channel = channel, text = text, from = from))
    }

    suspend fun getAllPendingMessages(): List<PendingMessageEntity> = dao.getAllPendingMessages()

    suspend fun deletePendingMessage(uid: Long) = dao.deletePendingMessage(uid)

    suspend fun clearPendingMessages() = dao.clearPendingMessages()

    suspend fun login(name: String, password: String): String {
        val response = wrapNetwork { api.login(LoginRequest(name, password)) }
        if (response.code() == UNAUTHORIZED) throw InvalidCredentialsException()
        ensureSuccess(response)
        val token = response.body()?.string()?.trim()?.trim('"').orEmpty()
        if (token.isBlank()) throw ChatServerException(response.code(), response.message())
        session.token = token
        return token
    }

    suspend fun logout() {
        runCatching { authorizedResponse { api.logout() } }
        session.token = null
        dao.clearChannels()
        dao.clearPendingMessages()
    }

    private suspend fun <T> authorized(block: suspend () -> T): T = try {
        wrapNetwork(block)
    } catch (e: HttpException) {
        if (e.code() == UNAUTHORIZED) {
            session.token = null
            throw AuthExpiredException()
        }
        throw ChatServerException(e.code(), e.message())
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

    private suspend fun <T> wrapNetwork(block: suspend () -> T): T = try {
        block()
    } catch (e: IOException) {
        throw ChatNetworkException(e)
    }

    private fun <T> ensureSuccess(response: Response<T>) {
        if (!response.isSuccessful) {
            throw ChatServerException(response.code(), response.message())
        }
    }
}