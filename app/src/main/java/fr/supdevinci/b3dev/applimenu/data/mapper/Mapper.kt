package fr.supdevinci.b3dev.applimenu.data.mapper

import fr.supdevinci.b3dev.applimenu.data.local.entity.MessageEntity
import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.MessageStatus


fun MessageDto.toEntity(conversationId: String): MessageEntity {
    return MessageEntity(
        id = this.id ?: java.util.UUID.randomUUID().toString(),
        text = this.content,
        timestamp = this.sentAt,
        senderId = this.sender,
        status = "SENT"
    )
}

fun MessageDto.toDomain(currentUsername: String? = null): Message {
    return Message(
        id = this.id ?: java.util.UUID.randomUUID().toString(),
        text = this.content,
        senderId = this.sender,
        timestamp = this.sentAt,
        isFromMe = currentUsername != null && this.sender == currentUsername,
        status = MessageStatus.SENT
    )
}