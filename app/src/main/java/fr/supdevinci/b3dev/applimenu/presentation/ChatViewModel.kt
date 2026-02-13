package fr.supdevinci.b3dev.applimenu.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.supdevinci.b3dev.applimenu.data.repository.ChatRepositoryImpl
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import fr.supdevinci.b3dev.applimenu.datasource.remote.SocketDataSource
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.model.Conversation
import fr.supdevinci.b3dev.applimenu.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"
    }

    private val socketDataSource = SocketDataSource()
    private val repository = ChatRepositoryImpl(socketDataSource)

    // États de l'UI
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    // Messages legacy (ancien système)
    val uiState: StateFlow<List<Message>> = repository.messages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Conversations
    val conversations: StateFlow<List<Conversation>> = repository.conversations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Utilisateurs
    val users: StateFlow<List<User>> = repository.users
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Messages de la conversation actuelle
    val currentConversationMessages: StateFlow<List<Message>> = repository.currentConversationMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _currentConversationId = MutableStateFlow<Int?>(null)
    val currentConversationId: StateFlow<Int?> = _currentConversationId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ============== CONNEXION ==============

    fun connect(username: String, serverUrl: String = DEFAULT_SERVER_URL) {
        if (username.isBlank()) {
            Log.w(TAG, "Username vide, connexion annulée")
            return
        }

        Log.d(TAG, "Connexion avec username: $username à $serverUrl")
        _currentUsername.value = username

        viewModelScope.launch {
            repository.connect(serverUrl, username)
                .launchIn(viewModelScope)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Déconnexion...")
        repository.disconnect()
        _currentUsername.value = null
        _currentConversationId.value = null
    }

    fun isConnected(): Boolean = repository.isConnected()

    fun clearError() {
        _error.value = null
    }

    // ============== UTILISATEURS ==============

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllUsers()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur loadAllUsers: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.searchUsers(query)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur searchUsers: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============== CONVERSATIONS ==============

    fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getConversations()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur loadConversations: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPrivateConversation(recipientUsername: String, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.createPrivateConversation(recipientUsername)
                result.onSuccess { conversation ->
                    Log.d(TAG, "Conversation privée créée: ${conversation.id}")
                    onSuccess(conversation.id)
                }.onFailure { error ->
                    Log.e(TAG, "Erreur création conversation: ${error.message}")
                    _error.value = error.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur createPrivateConversation: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createGroupConversation(name: String, memberUsernames: List<String>, onSuccess: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.createGroupConversation(name, memberUsernames)
                result.onSuccess { conversation ->
                    Log.d(TAG, "Groupe créé: ${conversation.id}")
                    onSuccess(conversation.id)
                }.onFailure { error ->
                    Log.e(TAG, "Erreur création groupe: ${error.message}")
                    _error.value = error.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur createGroupConversation: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============== MESSAGES DE CONVERSATION ==============

    fun openConversation(conversationId: Int) {
        _currentConversationId.value = conversationId
        loadConversationMessages(conversationId)
    }

    fun loadConversationMessages(conversationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getConversationMessages(conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur loadConversationMessages: ${e.message}")
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessageToConversation(content: String) {
        val conversationId = _currentConversationId.value ?: return

        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                val result = repository.sendMessageToConversation(conversationId, content)
                result.onFailure { error ->
                    Log.e(TAG, "Erreur envoi message: ${error.message}")
                    _error.value = error.message
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur sendMessageToConversation: ${e.message}")
                _error.value = e.message
            }
        }
    }

    // ============== LEGACY ==============

    fun onSendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(text)
        }
    }


    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}