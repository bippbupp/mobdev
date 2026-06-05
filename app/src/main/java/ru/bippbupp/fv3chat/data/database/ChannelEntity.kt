package ru.bippbupp.fv3chat.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val name: String,
    val lastUpdated: Long = System.currentTimeMillis()
)