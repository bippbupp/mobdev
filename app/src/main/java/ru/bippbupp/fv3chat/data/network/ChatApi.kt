package ru.bippbupp.fv3chat.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import ru.bippbupp.fv3chat.data.model.LoginRequest
import ru.bippbupp.fv3chat.data.model.MessageDto

interface ChatApi {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    @GET("channels")
    suspend fun channels(): List<String>

    @GET("channel/{name}")
    suspend fun channelMessages(
        @Path("name") name: String,
        @Query("limit") limit: Int,
        @Query("lastKnownId") lastKnownId: String,
        @Query("reverse") reverse: Boolean,
    ): List<MessageDto>

    @POST("messages")
    suspend fun sendMessage(@Body message: MessageDto): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(): Response<Unit>
}
