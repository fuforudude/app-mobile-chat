package fr.supdevinci.b3dev.applimenu.domain.repository

import com.google.ai.edge.litertlm.Conversation
import fr.supdevinci.b3dev.applimenu.domain.Message
import kotlinx.coroutines.flow.Flow


interface ChatRepository {
    fun getMessages(conversationId: String): Flow<List<Message>>

    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>

    fun getConversations(): Flow<List<Conversation>>
}