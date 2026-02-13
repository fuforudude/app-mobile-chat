package fr.supdevinci.b3dev.applimenu.data.repository

import fr.supdevinci.b3dev.applimenu.data.mapper.toDomain
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.repository.ChatRepository
import fr.supdevinci.b3dev.applimenu.datasource.remote.FakeSocketDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRepositoryImpl(
    private val socketDataSource: FakeSocketDataSource
) : ChatRepository {

    private var messageList = mutableListOf<Message>()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    // On utilise "override" pour lier à l'interface
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        observeIncomingMessages()
    }

    private fun observeIncomingMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            socketDataSource.incomingMessages.collect { dto ->
                val newMessage = dto.toDomain()
                messageList.add(newMessage)
                _messages.value = messageList.toList()
            }
        }
    }

    override suspend fun sendMessage(text: String) {
        socketDataSource.send(text)
    }
}