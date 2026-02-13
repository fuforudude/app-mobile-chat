package fr.supdevinci.b3dev.applimenu.datasource.remote

import android.util.Log
import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

/**
 * DataSource pour la connexion WebSocket avec Socket.IO
 *
 * Points importants basés sur le retour d'expérience :
 * - Le serveur renvoie "sender" (string) et "sentAt" (timestamp)
 * - Pour sendMessage, utiliser { content: string } comme payload
 * - Écouter "messageHistory" pour l'historique après join
 * - Écouter "newMessage" pour les nouveaux messages
 */
class SocketDataSource {

    companion object {
        private const val TAG = "SocketDataSource"
        private const val DEFAULT_URL = "http://10.0.2.2:3000" // localhost pour l'émulateur Android
    }

    private var socket: Socket? = null
    private var currentUsername: String? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableStateFlow<List<MessageDto>>(emptyList())
    val incomingMessages: StateFlow<List<MessageDto>> = _incomingMessages.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Se connecter au serveur Socket.IO et rejoindre le chat
     */
    fun connect(serverUrl: String = DEFAULT_URL, username: String): Flow<SocketEvent> = callbackFlow {
        try {
            Log.d(TAG, "Tentative de connexion à $serverUrl avec username: $username")
            _connectionState.value = ConnectionState.Connecting

            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 10000
            }

            socket = IO.socket(serverUrl, options)
            currentUsername = username

            socket?.apply {
                // Debug: Logger tous les événements reçus
                onAnyIncoming { args ->
                    Log.d(TAG, "Événement reçu: ${args.contentToString()}")
                }

                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Connecté au serveur")
                    _connectionState.value = ConnectionState.Connected
                    trySend(SocketEvent.Connected)

                    // Rejoindre le chat avec le username
                    val joinPayload = JSONObject().apply {
                        put("username", username)
                    }
                    emit("join", joinPayload)
                    Log.d(TAG, "Envoi de join avec: $joinPayload")
                }

                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Déconnecté du serveur")
                    _connectionState.value = ConnectionState.Disconnected
                    trySend(SocketEvent.Disconnected)
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()?.toString() ?: "Erreur inconnue"
                    Log.e(TAG, "Erreur de connexion: $error")
                    _connectionState.value = ConnectionState.Error(error)
                    trySend(SocketEvent.Error(error))
                }

                // Réception de l'historique des messages après join
                on("messageHistory") { args ->
                    Log.d(TAG, "Historique reçu: ${args.contentToString()}")
                    try {
                        val messagesArray = args.firstOrNull() as? JSONArray
                        if (messagesArray != null) {
                            val messages = parseMessageArray(messagesArray)
                            Log.d(TAG, "Historique parsé: ${messages.size} messages")
                            _incomingMessages.value = messages
                            trySend(SocketEvent.MessageHistory(messages))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing historique: ${e.message}", e)
                    }
                }

                // Réception des nouveaux messages
                on("newMessage") { args ->
                    Log.d(TAG, "Nouveau message reçu: ${args.contentToString()}")
                    try {
                        val messageObj = args.firstOrNull() as? JSONObject
                        if (messageObj != null) {
                            val message = parseMessage(messageObj)
                            Log.d(TAG, "Message parsé: $message")
                            _incomingMessages.value = _incomingMessages.value + message
                            trySend(SocketEvent.NewMessage(message))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing message: ${e.message}", e)
                    }
                }

                // Écouter les erreurs du serveur
                on("error") { args ->
                    val errorMsg = (args.firstOrNull() as? JSONObject)?.optString("message")
                        ?: args.firstOrNull()?.toString()
                        ?: "Erreur serveur"
                    Log.e(TAG, "Erreur serveur: $errorMsg")
                    trySend(SocketEvent.Error(errorMsg))
                }

                connect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la connexion: ${e.message}", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Erreur")
            trySend(SocketEvent.Error(e.message ?: "Erreur de connexion"))
        }

        awaitClose {
            Log.d(TAG, "Fermeture du flow, déconnexion...")
            disconnect()
        }
    }

    /**
     * Envoyer un message
     * IMPORTANT: Le payload doit être { content: string }
     */
    fun sendMessage(content: String, callback: ((Boolean, String?) -> Unit)? = null) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            Log.e(TAG, "Impossible d'envoyer: socket non connecté")
            callback?.invoke(false, "Non connecté")
            return
        }

        val payload = JSONObject().apply {
            put("content", content)
        }

        Log.d(TAG, "Envoi du message: $payload")

        // Utiliser la version avec acknowledgment si disponible
        currentSocket.emit("sendMessage", arrayOf(payload)) { response ->
            Log.d(TAG, "Réponse du serveur après envoi: ${response.contentToString()}")
            val responseObj = response.firstOrNull() as? JSONObject
            val success = responseObj?.optBoolean("success", true) ?: true
            val error = responseObj?.optString("error")
            callback?.invoke(success, error)
        }
    }

    /**
     * Se déconnecter proprement
     */
    fun disconnect() {
        Log.d(TAG, "Déconnexion...")
        socket?.disconnect()
        socket?.off()
        socket = null
        currentUsername = null
        _connectionState.value = ConnectionState.Disconnected
        _incomingMessages.value = emptyList()
    }

    fun isConnected(): Boolean = socket?.connected() == true

    fun getCurrentUsername(): String? = currentUsername

    /**
     * Parser un tableau de messages JSON
     */
    private fun parseMessageArray(array: JSONArray): List<MessageDto> {
        val messages = mutableListOf<MessageDto>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                messages.add(parseMessage(obj))
            } catch (e: Exception) {
                Log.e(TAG, "Erreur parsing message index $i: ${e.message}")
            }
        }
        return messages
    }

    /**
     * Parser un message JSON
     * Format attendu: { sender: string, content: string, sentAt: number/string }
     */
    private fun parseMessage(obj: JSONObject): MessageDto {
        return MessageDto(
            id = obj.optString("id", null),
            content = obj.optString("content", ""),
            sender = obj.optString("sender", "Inconnu"),
            sentAt = parseSentAt(obj)
        )
    }

    /**
     * Parser le timestamp sentAt (peut être string ISO ou number)
     */
    private fun parseSentAt(obj: JSONObject): Long {
        return try {
            // Essayer d'abord comme Long
            obj.optLong("sentAt", 0).takeIf { it != 0L }
                ?: // Sinon essayer de parser une date ISO
                try {
                    val dateStr = obj.optString("sentAt", "")
                    if (dateStr.isNotEmpty()) {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                            .parse(dateStr)?.time ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

/**
 * États de la connexion WebSocket
 */
sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * Événements Socket.IO
 */
sealed class SocketEvent {
    data object Connected : SocketEvent()
    data object Disconnected : SocketEvent()
    data class Error(val message: String) : SocketEvent()
    data class MessageHistory(val messages: List<MessageDto>) : SocketEvent()
    data class NewMessage(val message: MessageDto) : SocketEvent()
}

