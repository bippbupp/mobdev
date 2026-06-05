package ru.bippbupp.fv3chat.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val channel: String,
    val text: String,
    val from: String,
    val timestamp: Long = System.currentTimeMillis()
)