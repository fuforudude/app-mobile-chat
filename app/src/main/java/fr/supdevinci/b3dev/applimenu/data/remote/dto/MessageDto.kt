package fr.supdevinci.b3dev.applimenu.data.remote.dto

import kotlinx.serialization.Serializable


@Serializable
data class MessageDto(
    val id: String? = null,
    val content: String,
    val sender: String,
    val sentAt: Long
)