package fr.supdevinci.b3dev.applimenu.data.remote.dto

import kotlinx.serialization.Serializable


@Serializable
data class MessageDto(
    val id: String? = null,
    val content: String,
    val sender: String,  // Le serveur renvoie "sender" (string), pas "senderId"
    val sentAt: Long     // Le serveur renvoie "sentAt", pas "timestamp"
)