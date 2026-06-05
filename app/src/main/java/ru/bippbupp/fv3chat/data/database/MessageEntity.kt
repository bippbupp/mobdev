package ru.bippbupp.fv3chat.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.bippbupp.fv3chat.data.model.ChatMessage

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val channel: String,
    val from: String,
    val text: String?,
    val imagePath: String?,
    val time: String?
) {
    fun toDomain() = ChatMessage(id, from, channel, text, imagePath, time)
}

fun ChatMessage.toEntity() = MessageEntity(id, to, from, text, imagePath, time)