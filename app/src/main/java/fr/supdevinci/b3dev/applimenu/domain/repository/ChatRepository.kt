package fr.supdevinci.b3dev.applimenu.domain.repository

import fr.supdevinci.b3dev.applimenu.domain.model.Message
import fr.supdevinci.b3dev.applimenu.domain.model.Conversation
import fr.supdevinci.b3dev.applimenu.domain.model.User
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val messages: StateFlow<List<Message>>
    val connectionState: StateFlow<ConnectionState>
    val conversations: StateFlow<List<Conversation>>
    val users: StateFlow<List<User>>
    val currentConversationMessages: StateFlow<List<Message>>

    // Connexion
    fun connect(serverUrl: String, username: String): Flow<Unit>
    fun connectWithToken(serverUrl: String, token: String, username: String): Flow<Unit>
    fun disconnect()
    fun isConnected(): Boolean
    fun getCurrentUsername(): String?

    // Messages (legacy)
    suspend fun sendMessage(text: String)

    // Utilisateurs
    suspend fun getAllUsers(): List<User>
    suspend fun searchUsers(query: String): List<User>

    // Conversations
    suspend fun getConversations(): List<Conversation>
    suspend fun createPrivateConversation(recipientUsername: String): Result<Conversation>
    suspend fun createGroupConversation(name: String, memberUsernames: List<String>): Result<Conversation>

    // Messages de conversation
    suspend fun getConversationMessages(conversationId: Int): List<Message>
    suspend fun sendMessageToConversation(conversationId: Int, content: String): Result<Unit>
}