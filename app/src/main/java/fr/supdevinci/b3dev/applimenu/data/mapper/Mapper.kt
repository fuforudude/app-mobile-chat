package fr.supdevinci.b3dev.applimenu.data.mapper

import fr.supdevinci.b3dev.applimenu.data.local.entity.MessageEntity
import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.MessageStatus


fun MessageDto.toEntity(conversationId: String, senderId: String): MessageEntity {
    return MessageEntity(
        id = this.id,
        text = this.content,
        timestamp = this.sentAt,
        senderId = senderId,
        status = "SENT"
    )
}
fun MessageDto.toDomain(): Message {
    return Message(
        id = this.id ?: "",
        text = this.content ?: "",
        senderId = this.senderId ?: "Inconnu",
        timestamp = this.sentAt?.toLong() ?: System.currentTimeMillis(),
        isFromMe = false,
        status = MessageStatus.SENT
    )
}