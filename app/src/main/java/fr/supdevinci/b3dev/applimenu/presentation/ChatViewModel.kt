// presentation/ChatViewModel.kt
package fr.supdevinci.b3dev.applimenu.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.supdevinci.b3dev.applimenu.data.repository.ChatRepositoryImpl
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import fr.supdevinci.b3dev.applimenu.datasource.remote.SocketDataSource
import fr.supdevinci.b3dev.applimenu.domain.Message
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
        // URL par défaut - 10.0.2.2 est localhost pour l'émulateur Android
        const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"
    }

    private val socketDataSource = SocketDataSource()
    private val repository = ChatRepositoryImpl(socketDataSource)

    val uiState: StateFlow<List<Message>> = repository.messages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val _currentUsername = MutableStateFlow<String?>(null)
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _serverUrl = MutableStateFlow(DEFAULT_SERVER_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    /**
     * Se connecter au serveur avec le username
     */
    fun connect(username: String, serverUrl: String = DEFAULT_SERVER_URL) {
        if (username.isBlank()) {
            Log.w(TAG, "Username vide, connexion annulée")
            return
        }

        Log.d(TAG, "Connexion avec username: $username à $serverUrl")
        _currentUsername.value = username
        _serverUrl.value = serverUrl

        viewModelScope.launch {
            repository.connect(serverUrl, username)
                .launchIn(viewModelScope)
        }
    }

    /**
     * Se déconnecter du serveur
     */
    fun disconnect() {
        Log.d(TAG, "Déconnexion...")
        repository.disconnect()
        _currentUsername.value = null
    }

    /**
     * Envoyer un message
     */
    fun onSendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(text)
        }
    }

    /**
     * Vérifier si connecté
     */
    fun isConnected(): Boolean = repository.isConnected()

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}