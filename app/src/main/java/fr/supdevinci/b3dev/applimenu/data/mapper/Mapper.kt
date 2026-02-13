package fr.supdevinci.b3dev.applimenu.data.mapper

import fr.supdevinci.b3dev.applimenu.data.local.entity.MessageEntity
import fr.supdevinci.b3dev.applimenu.data.remote.dto.ConversationDto
import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import fr.supdevinci.b3dev.applimenu.data.remote.dto.UserDto
import fr.supdevinci.b3dev.applimenu.domain.model.Message
import fr.supdevinci.b3dev.applimenu.domain.model.MessageStatus
import fr.supdevinci.b3dev.applimenu.domain.model.Conversation
import fr.supdevinci.b3dev.applimenu.domain.model.ConversationType
import fr.supdevinci.b3dev.applimenu.domain.model.LastMessage
import fr.supdevinci.b3dev.applimenu.domain.model.User


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

fun UserDto.toDomain(): User {
    return User(
        id = this.id,
        username = this.username,
        isOnline = this.isOnline
    )
}

fun ConversationDto.toDomain(): Conversation {
    return Conversation(
        id = this.id,
        type = ConversationType.fromString(this.type),
        name = this.name,
        memberCount = this.memberCount,
        createdAt = this.createdAt,
        lastMessage = this.lastMessage?.let {
            LastMessage(
                content = it.content,
                sender = it.sender,
                sentAt = it.sentAt
            )
        }
    )
}
