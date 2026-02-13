package fr.supdevinci.b3dev.applimenu.domain.repository

import fr.supdevinci.b3dev.applimenu.domain.Message
import kotlinx.coroutines.flow.StateFlow

interface ChatRepository {
    val messages: StateFlow<List<Message>>

    suspend fun sendMessage(text: String)
}