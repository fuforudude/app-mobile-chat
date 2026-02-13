package fr.supdevinci.b3dev.applimenu.domain.model

data class Conversation(
    val id: Int,
    val type: ConversationType,
    val name: String,
    val memberCount: Int = 0,
    val createdAt: String,
    val lastMessage: LastMessage? = null,
    val hasUnreadMessages: Boolean = false
)

enum class ConversationType {
    PRIVATE,
    GROUP;

    companion object {
        fun fromString(value: String): ConversationType {
            return when (value.lowercase()) {
                "private" -> PRIVATE
                "group" -> GROUP
                else -> PRIVATE
            }
        }
    }
}

data class LastMessage(
    val content: String,
    val sender: String,
    val sentAt: String
)

