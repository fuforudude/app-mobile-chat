package fr.supdevinci.b3dev.applimenu.data.mapper

import fr.supdevinci.b3dev.applimenu.data.local.entity.MessageEntity
import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto

fun MessageDto.toEntity(conversationId: String, senderId: String): MessageEntity {
    return MessageEntity(
        id = this.id,
        text = this.content,
        timestamp = this.sentAt,
        senderId = senderId,
        status = "SENT"
    )
}