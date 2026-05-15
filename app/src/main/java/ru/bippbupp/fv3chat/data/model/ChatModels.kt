package ru.bippbupp.fv3chat.data.model

import com.squareup.moshi.Json

data class LoginRequest(
    val name: String,
    val pwd: String,
)

data class MessageDto(
    val id: String? = null,
    val from: String,
    val to: String? = null,
    val data: MessageDataDto,
    val time: String? = null,
)

data class MessageDataDto(
    @Json(name = "Text")
    val text: TextPayload? = null,
    @Json(name = "Image")
    val image: ImagePayload? = null,
)

data class TextPayload(
    val text: String,
)

data class ImagePayload(
    val link: String? = null,
)

data class ChatMessage(
    val id: String,
    val from: String,
    val to: String,
    val text: String?,
    val imagePath: String?,
    val time: String?,
)

fun MessageDto.toDomain(): ChatMessage? {
    val messageId = id ?: return null
    return ChatMessage(
        id = messageId,
        from = from,
        to = to.orEmpty(),
        text = data.text?.text,
        imagePath = data.image?.link,
        time = time,
    )
}
