package ru.bippbupp.fv3chat.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("SELECT name FROM channels ORDER BY name ASC")
    suspend fun getAllChannels(): List<String>

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE channel = :channel ORDER BY CAST(id AS INTEGER) ASC")
    suspend fun getMessagesForChannel(channel: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE channel = :channel")
    suspend fun clearMessagesForChannel(channel: String)

    @Insert
    suspend fun insertPendingMessage(pending: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY timestamp ASC")
    suspend fun getAllPendingMessages(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE uid = :uid")
    suspend fun deletePendingMessage(uid: Long)

    @Query("DELETE FROM pending_messages")
    suspend fun clearPendingMessages()
}