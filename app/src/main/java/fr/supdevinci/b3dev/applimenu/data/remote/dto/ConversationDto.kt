package fr.supdevinci.b3dev.applimenu.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    val id: Int,
    val type: String,  // "private" ou "group"
    val name: String,
    val memberCount: Int = 0,
    val createdAt: String,
    val lastMessage: LastMessageDto? = null
)

@Serializable
data class LastMessageDto(
    val content: String,
    val sender: String,
    val sentAt: String
)

