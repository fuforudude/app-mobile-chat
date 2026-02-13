package fr.supdevinci.b3dev.applimenu.data.repository

import android.util.Log
import fr.supdevinci.b3dev.applimenu.data.mapper.toDomain
import fr.supdevinci.b3dev.applimenu.domain.model.Message
import fr.supdevinci.b3dev.applimenu.domain.model.Conversation
import fr.supdevinci.b3dev.applimenu.domain.model.User
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ChatRepositoryImpl(
    private val socketDataSource: SocketDataSource = SocketDataSource()
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Messages legacy
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // État de connexion
    override val connectionState: StateFlow<ConnectionState> = socketDataSource.connectionState

    // Conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    override val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Utilisateurs
    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: StateFlow<List<User>> = _users.asStateFlow()

    // Messages de la conversation actuelle
    private val _currentConversationMessages = MutableStateFlow<List<Message>>(emptyList())
    override val currentConversationMessages: StateFlow<List<Message>> = _currentConversationMessages.asStateFlow()

    // Dernier message reçu (pour les notifications) - avec ID unique
    data class ReceivedMessageInfo(
        val uniqueId: Long,
        val sender: String,
        val content: String,
        val conversationId: Int?,
        val isFromMe: Boolean
    )
    private val _lastReceivedMessage = MutableStateFlow<ReceivedMessageInfo?>(null)
    val lastReceivedMessage: StateFlow<ReceivedMessageInfo?> = _lastReceivedMessage.asStateFlow()

    init {
        observeIncomingMessages()
        observeConversations()
        observeUsers()
        observeConversationMessages()
        observeLastReceivedMessage()
    }

    private fun observeLastReceivedMessage() {
        scope.launch {
            socketDataSource.lastReceivedMessage.collect { messageWithId ->
                if (messageWithId != null) {
                    val messageDto = messageWithId.message
                    val currentUsername = socketDataSource.getCurrentUsername()
                    val isFromMe = messageDto.sender == currentUsername
                    _lastReceivedMessage.value = ReceivedMessageInfo(
                        uniqueId = messageWithId.id,
                        sender = messageDto.sender,
                        content = messageDto.content,
                        conversationId = messageDto.conversationId,
                        isFromMe = isFromMe
                    )
                    Log.d(TAG, "Message #${messageWithId.id} pour notif: sender=${messageDto.sender}, convId=${messageDto.conversationId}, isFromMe=$isFromMe")
                }
            }
        }
    }

    private fun observeIncomingMessages() {
        scope.launch {
            socketDataSource.incomingMessages.collect { dtoList ->
                val currentUsername = socketDataSource.getCurrentUsername()
                _messages.value = dtoList.map { it.toDomain(currentUsername) }
            }
        }
    }

    private fun observeConversations() {
        scope.launch {
            socketDataSource.conversations.collect { dtoList ->
                Log.d(TAG, "=== CONVERSATIONS MISES À JOUR ===")
                Log.d(TAG, "Nombre de conversations reçues: ${dtoList.size}")
                dtoList.forEach {
                    Log.d(TAG, "  - ${it.id}: ${it.name}")
                }
                _conversations.value = dtoList.map { it.toDomain() }
            }
        }
    }

    private fun observeUsers() {
        scope.launch {
            socketDataSource.users.collect { dtoList ->
                _users.value = dtoList.map { it.toDomain() }
            }
        }
    }

    private fun observeConversationMessages() {
        scope.launch {
            socketDataSource.currentConversationMessages.collect { dtoList ->
                val currentUsername = socketDataSource.getCurrentUsername()
                _currentConversationMessages.value = dtoList.map { it.toDomain(currentUsername) }
            }
        }
    }

    // ============== CONNEXION ==============

    override fun connect(serverUrl: String, username: String): Flow<Unit> = flow {
        Log.d(TAG, "Connexion au serveur: $serverUrl avec username: $username")

        socketDataSource.connect(serverUrl, username)
            .onEach { event ->
                when (event) {
                    is SocketEvent.Connected -> Log.d(TAG, "Événement: Connecté")
                    is SocketEvent.Disconnected -> Log.d(TAG, "Événement: Déconnecté")
                    is SocketEvent.JoinSuccess -> Log.d(TAG, "Événement: Join réussi")
                    is SocketEvent.Error -> Log.e(TAG, "Événement: Erreur - ${event.message}")
                    is SocketEvent.MessageHistory -> Log.d(TAG, "Événement: Historique (${event.messages.size})")
                    is SocketEvent.NewMessage -> Log.d(TAG, "Événement: Nouveau message")
                    is SocketEvent.NewConversationMessage -> Log.d(TAG, "Événement: Message conversation")
                    is SocketEvent.NewConversation -> Log.d(TAG, "Événement: Nouvelle conversation - ${event.conversation.name}")
                }
            }
            .launchIn(scope)

        emit(Unit)
    }

    override fun connectWithToken(serverUrl: String, token: String, username: String): Flow<Unit> = flow {
        Log.d(TAG, "Connexion JWT au serveur: $serverUrl avec username: $username")

        socketDataSource.connectWithToken(serverUrl, token, username)
            .onEach { event ->
                when (event) {
                    is SocketEvent.Connected -> Log.d(TAG, "Événement JWT: Connecté")
                    is SocketEvent.Disconnected -> Log.d(TAG, "Événement JWT: Déconnecté")
                    is SocketEvent.JoinSuccess -> Log.d(TAG, "Événement JWT: Authentifié")
                    is SocketEvent.Error -> Log.e(TAG, "Événement JWT: Erreur - ${event.message}")
                    is SocketEvent.MessageHistory -> Log.d(TAG, "Événement JWT: Historique (${event.messages.size})")
                    is SocketEvent.NewMessage -> Log.d(TAG, "Événement JWT: Nouveau message")
                    is SocketEvent.NewConversationMessage -> Log.d(TAG, "Événement JWT: Message conversation")
                    is SocketEvent.NewConversation -> Log.d(TAG, "Événement JWT: Nouvelle conversation - ${event.conversation.name}")
                }
            }
            .launchIn(scope)

        emit(Unit)
    }

    override fun disconnect() {
        Log.d(TAG, "Déconnexion...")
        socketDataSource.disconnect()
        _messages.value = emptyList()
        _conversations.value = emptyList()
        _users.value = emptyList()
        _currentConversationMessages.value = emptyList()
    }

    override fun isConnected(): Boolean = socketDataSource.isConnected()
    override fun getCurrentUsername(): String? = socketDataSource.getCurrentUsername()

    // ============== MESSAGES LEGACY ==============

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

    // ============== UTILISATEURS ==============

    override suspend fun getAllUsers(): List<User> = suspendCancellableCoroutine { continuation ->
        socketDataSource.getAllUsers { success, users ->
            if (success) {
                val domainUsers = users.map { it.toDomain() }
                _users.value = domainUsers
                continuation.resume(domainUsers)
            } else {
                continuation.resume(emptyList())
            }
        }
    }

    override suspend fun searchUsers(query: String): List<User> = suspendCancellableCoroutine { continuation ->
        socketDataSource.searchUsers(query) { success, users ->
            if (success) {
                val domainUsers = users.map { it.toDomain() }
                _users.value = domainUsers
                continuation.resume(domainUsers)
            } else {
                continuation.resume(emptyList())
            }
        }
    }

    // ============== CONVERSATIONS ==============

    override suspend fun getConversations(): List<Conversation> = suspendCancellableCoroutine { continuation ->
        socketDataSource.getConversations { success, conversations ->
            if (success) {
                val domainConversations = conversations.map { it.toDomain() }
                _conversations.value = domainConversations
                continuation.resume(domainConversations)
            } else {
                continuation.resume(emptyList())
            }
        }
    }

    override suspend fun createPrivateConversation(recipientUsername: String): Result<Conversation> =
        suspendCancellableCoroutine { continuation ->
            socketDataSource.createPrivateConversation(recipientUsername) { success, conversationDto, error ->
                if (success && conversationDto != null) {
                    val conversation = conversationDto.toDomain()
                    // Rafraîchir la liste des conversations
                    scope.launch { getConversations() }
                    continuation.resume(Result.success(conversation))
                } else {
                    continuation.resume(Result.failure(Exception(error ?: "Erreur inconnue")))
                }
            }
        }

    override suspend fun createGroupConversation(
        name: String,
        memberUsernames: List<String>
    ): Result<Conversation> = suspendCancellableCoroutine { continuation ->
        socketDataSource.createGroupConversation(name, memberUsernames) { success, conversationDto, error ->
            if (success && conversationDto != null) {
                val conversation = conversationDto.toDomain()
                // Rafraîchir la liste des conversations
                scope.launch { getConversations() }
                continuation.resume(Result.success(conversation))
            } else {
                continuation.resume(Result.failure(Exception(error ?: "Erreur inconnue")))
            }
        }
    }

    // ============== MESSAGES DE CONVERSATION ==============

    override suspend fun getConversationMessages(conversationId: Int): List<Message> =
        suspendCancellableCoroutine { continuation ->
            socketDataSource.getConversationMessages(conversationId) { success, messages ->
                if (success) {
                    val currentUsername = socketDataSource.getCurrentUsername()
                    val domainMessages = messages.map { it.toDomain(currentUsername) }
                    _currentConversationMessages.value = domainMessages
                    continuation.resume(domainMessages)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

    override suspend fun sendMessageToConversation(conversationId: Int, content: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            if (content.isBlank()) {
                continuation.resume(Result.failure(Exception("Message vide")))
                return@suspendCancellableCoroutine
            }

            socketDataSource.sendMessageToConversation(conversationId, content) { success, error ->
                if (success) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception(error ?: "Erreur d'envoi")))
                }
            }
        }
}