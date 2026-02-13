package fr.supdevinci.b3dev.applimenu.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "menuItems")
@Serializable
data class Message (
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: MessageStatus
)

enum class MessageStatus { SENT, DELIVERED, READ, ERROR }