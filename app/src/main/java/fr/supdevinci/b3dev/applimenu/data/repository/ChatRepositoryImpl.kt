package fr.supdevinci.b3dev.applimenu.data.repository

import android.util.Log
import fr.supdevinci.b3dev.applimenu.data.mapper.toDomain
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.repository.ChatRepository
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import fr.supdevinci.b3dev.applimenu.datasource.remote.SocketDataSource
import fr.supdevinci.b3dev.applimenu.datasource.remote.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatRepositoryImpl(
    private val socketDataSource: SocketDataSource = SocketDataSource()
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    override val connectionState: StateFlow<ConnectionState> = socketDataSource.connectionState

    init {
        // Observer les messages entrants du SocketDataSource
        observeIncomingMessages()
    }

    private fun observeIncomingMessages() {
        scope.launch {
            socketDataSource.incomingMessages.collect { dtoList ->
                val currentUsername = socketDataSource.getCurrentUsername()
                _messages.value = dtoList.map { it.toDomain(currentUsername) }
            }
        }
    }

    override fun connect(serverUrl: String, username: String): Flow<Unit> = flow {
        Log.d(TAG, "Connexion au serveur: $serverUrl avec username: $username")

        socketDataSource.connect(serverUrl, username)
            .onEach { event ->
                when (event) {
                    is SocketEvent.Connected -> {
                        Log.d(TAG, "Événement: Connecté")
                    }
                    is SocketEvent.Disconnected -> {
                        Log.d(TAG, "Événement: Déconnecté")
                    }
                    is SocketEvent.Error -> {
                        Log.e(TAG, "Événement: Erreur - ${event.message}")
                    }
                    is SocketEvent.MessageHistory -> {
                        Log.d(TAG, "Événement: Historique reçu (${event.messages.size} messages)")
                    }
                    is SocketEvent.NewMessage -> {
                        Log.d(TAG, "Événement: Nouveau message de ${event.message.sender}")
                    }
                }
            }
            .launchIn(scope)

        emit(Unit)
    }

    override fun disconnect() {
        Log.d(TAG, "Déconnexion...")
        socketDataSource.disconnect()
        _messages.value = emptyList()
    }

    override suspend fun sendMessage(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "Message vide ignoré")
            return
        }

        Log.d(TAG, "Envoi du message: $text")
        socketDataSource.sendMessage(text) { success, error ->
            if (success) {
                Log.d(TAG, "Message envoyé avec succès")
            } else {
                Log.e(TAG, "Erreur d'envoi: $error")
            }
        }
    }

    override fun isConnected(): Boolean = socketDataSource.isConnected()

    override fun getCurrentUsername(): String? = socketDataSource.getCurrentUsername()
}