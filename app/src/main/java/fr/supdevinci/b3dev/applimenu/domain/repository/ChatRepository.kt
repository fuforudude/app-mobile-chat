package fr.supdevinci.b3dev.applimenu.domain.repository

import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val messages: StateFlow<List<Message>>
    val connectionState: StateFlow<ConnectionState>

    fun connect(serverUrl: String, username: String): Flow<Unit>
    fun disconnect()
    suspend fun sendMessage(text: String)
    fun isConnected(): Boolean
    fun getCurrentUsername(): String?
}