package fr.supdevinci.b3dev.applimenu.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.supdevinci.b3dev.applimenu.data.local.TokenManager
import fr.supdevinci.b3dev.applimenu.data.remote.service.AuthService
import fr.supdevinci.b3dev.applimenu.data.repository.ChatRepositoryImpl
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import fr.supdevinci.b3dev.applimenu.datasource.remote.SocketDataSource
import fr.supdevinci.b3dev.applimenu.domain.model.Message
import fr.supdevinci.b3dev.applimenu.domain.model.Conversation
import fr.supdevinci.b3dev.applimenu.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel principal avec authentification JWT
 * Gère la connexion, l'inscription et la persistance du token
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
        const val DEFAULT_SERVER_URL = "https://websocketkotlin.onrender.com"
    }

    // Services
    private val tokenManager = TokenManager(application)
    private val authService = AuthService("$DEFAULT_SERVER_URL/")
    private val socketDataSource = SocketDataSource()
    private val repository = ChatRepositoryImpl(socketDataSource)

    // État d'authentification
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // États de l'UI
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

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

    init {
        // Vérifier si l'utilisateur est déjà connecté
        checkExistingSession()
    }

    // ============== AUTHENTIFICATION ==============

    /**
     * Vérifier si un token existe et se reconnecter automatiquement
     */
    private fun checkExistingSession() {
        val token = tokenManager.getToken()
        val username = tokenManager.getUsername()

        if (token != null && username != null) {
            Log.d(TAG, "Session existante trouvée pour: $username")
            _currentUsername.value = username
            connectWithToken(token, username)
        } else {
            Log.d(TAG, "Aucune session existante")
            _isAuthenticated.value = false
        }
    }

    /**
     * Inscription d'un nouvel utilisateur
     */
    fun register(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authError.value = "Veuillez remplir tous les champs"
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null

            try {
                Log.d(TAG, "Appel API register pour: $username")
                val result = authService.register(username, password)

                result.fold(
                    onSuccess = { authResponse ->
                        Log.d(TAG, "Inscription réussie pour: $username")
                        Log.d(TAG, "Token reçu: ${authResponse.accessToken?.take(20)}...")

                        val token = authResponse.accessToken
                        val userInfo = authResponse.user

                        if (token.isNullOrBlank()) {
                            Log.e(TAG, "Token null ou vide")
                            _authError.value = "Réponse du serveur invalide (token manquant)"
                            _authLoading.value = false
                            return@fold
                        }

                        try {
                            // Sauvegarder le token et les infos utilisateur
                            tokenManager.saveToken(token)
                            val finalUsername = userInfo?.username ?: username
                            tokenManager.saveUserInfo(userInfo?.id ?: 0, finalUsername)
                            Log.d(TAG, "Token sauvegardé avec succès")

                            _currentUsername.value = finalUsername

                            // Connecter le WebSocket avec le token
                            connectWithToken(token, finalUsername)
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur sauvegarde token: ${e.message}", e)
                            _authError.value = "Erreur lors de la sauvegarde: ${e.message}"
                            _authLoading.value = false
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Erreur inscription: ${error.message}")
                        _authError.value = error.message ?: "Erreur lors de l'inscription"
                        _authLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception inscription: ${e.message}", e)
                _authError.value = e.message ?: "Erreur lors de l'inscription"
                _authLoading.value = false
            }
        }
    }

    /**
     * Connexion d'un utilisateur existant
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _authError.value = "Veuillez remplir tous les champs"
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null

            try {
                Log.d(TAG, "Appel API login pour: $username")
                val result = authService.login(username, password)

                result.fold(
                    onSuccess = { authResponse ->
                        Log.d(TAG, "Connexion réussie pour: $username")

                        val token = authResponse.accessToken
                        val userInfo = authResponse.user

                        if (token.isNullOrBlank()) {
                            Log.e(TAG, "Token null ou vide")
                            _authError.value = "Réponse du serveur invalide (token manquant)"
                            _authLoading.value = false
                            return@fold
                        }

                        try {
                            // Sauvegarder le token et les infos utilisateur
                            tokenManager.saveToken(token)
                            val finalUsername = userInfo?.username ?: username
                            tokenManager.saveUserInfo(userInfo?.id ?: 0, finalUsername)

                            _currentUsername.value = finalUsername

                            // Connecter le WebSocket avec le token
                            connectWithToken(token, finalUsername)
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur sauvegarde token: ${e.message}", e)
                            _authError.value = "Erreur lors de la sauvegarde: ${e.message}"
                            _authLoading.value = false
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Erreur connexion: ${error.message}")
                        _authError.value = error.message ?: "Erreur lors de la connexion"
                        _authLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception connexion: ${e.message}", e)
                _authError.value = e.message ?: "Erreur lors de la connexion"
                _authLoading.value = false
            }
        }
    }

    /**
     * Connecter le WebSocket avec le token JWT
     */
    private fun connectWithToken(token: String, username: String) {
        Log.d(TAG, "Connexion WebSocket avec token pour: $username")

        viewModelScope.launch {
            repository.connectWithToken(DEFAULT_SERVER_URL, token, username)
                .launchIn(viewModelScope)

            _isAuthenticated.value = true
            _authLoading.value = false
        }
    }

    /**
     * Déconnexion complète
     */
    fun logout() {
        Log.d(TAG, "Déconnexion...")

        // Déconnecter le WebSocket
        repository.disconnect()

        // Effacer les données sauvegardées
        tokenManager.clearAll()

        // Réinitialiser les états
        _currentUsername.value = null
        _currentConversationId.value = null
        _isAuthenticated.value = false
        _authError.value = null
    }

    fun clearAuthError() {
        _authError.value = null
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

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}

