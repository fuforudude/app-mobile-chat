package fr.supdevinci.b3dev.applimenu.data.repository

import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import fr.supdevinci.b3dev.applimenu.datasource.local.MessageDao
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

// data/repository/ChatRepositoryImpl.kt
class ChatRepositoryImpl(
    private val socketDataSource: ChatSocketDataSource // Ton interface avec le WebSocket
) : ChatRepository {

    // On stocke la liste en mémoire pour la durée de la session
    private var messageList = mutableListOf<Message>()

    // Le StateFlow qui sera observé par l'UI (ViewModel)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        // Dès que le Repo est créé, on écoute le flux entrant
        observeIncomingMessages()
    }

    private fun observeIncomingMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            socketDataSource.incomingMessages.collect { dto ->
                // 1. On transforme le DTO en modèle Domain
                val newMessage = dto.toDomain()

                // 2. On met à jour notre liste en mémoire
                messageList.add(newMessage)

                // 3. On émet la nouvelle liste pour l'UI
                _messages.value = messageList.toList() // .toList() pour créer une nouvelle instance
            }
        }
    }

    override suspend fun sendMessage(text: String) {
        // On envoie au socket (logique "Fire and Forget" ou avec retour)
        socketDataSource.send(text)
    }
}