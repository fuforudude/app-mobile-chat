package fr.supdevinci.b3dev.applimenu.data.remote.dto

import kotlinx.serialization.Serializable


@Serializable
data class MessageDto(
    val id: String,
    val content: String,
    val senderId: String,
    val sentAt: Long
)