package fr.supdevinci.b3dev.applimenu.datasource.remote

import android.util.Log
import fr.supdevinci.b3dev.applimenu.data.remote.dto.ConversationDto
import fr.supdevinci.b3dev.applimenu.data.remote.dto.LastMessageDto
import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import fr.supdevinci.b3dev.applimenu.data.remote.dto.UserDto
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * DataSource pour la connexion WebSocket avec Socket.IO
 * Gère toutes les interactions avec le serveur WebSocket
 */
class SocketDataSource {

    companion object {
        private const val TAG = "SocketDataSource"
        private const val DEFAULT_URL = "http://10.0.2.2:3000"
    }

    private var socket: Socket? = null
    private var currentUsername: String? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableStateFlow<List<MessageDto>>(emptyList())
    val incomingMessages: StateFlow<List<MessageDto>> = _incomingMessages.asStateFlow()

    private val _conversations = MutableStateFlow<List<ConversationDto>>(emptyList())
    val conversations: StateFlow<List<ConversationDto>> = _conversations.asStateFlow()

    private val _users = MutableStateFlow<List<UserDto>>(emptyList())
    val users: StateFlow<List<UserDto>> = _users.asStateFlow()

    private val _currentConversationMessages = MutableStateFlow<List<MessageDto>>(emptyList())
    val currentConversationMessages: StateFlow<List<MessageDto>> = _currentConversationMessages.asStateFlow()

    // ============== CONNEXION ==============

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
                    emit("join", arrayOf(joinPayload)) { response ->
                        val responseObj = response.firstOrNull() as? JSONObject
                        val success = responseObj?.optBoolean("success", false) ?: false
                        Log.d(TAG, "Join response: success=$success")
                        if (success) {
                            trySend(SocketEvent.JoinSuccess)
                        }
                    }
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

                // Réception de nouveaux messages dans une conversation
                on("newConversationMessage") { args ->
                    Log.d(TAG, "Nouveau message conversation: ${args.contentToString()}")
                    try {
                        val messageObj = args.firstOrNull() as? JSONObject
                        if (messageObj != null) {
                            val message = parseConversationMessage(messageObj)
                            _currentConversationMessages.value = _currentConversationMessages.value + message
                            trySend(SocketEvent.NewConversationMessage(message))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing message: ${e.message}", e)
                    }
                }

                // Legacy: écouter les anciens événements pour compatibilité
                on("messageHistory") { args ->
                    Log.d(TAG, "Historique reçu: ${args.contentToString()}")
                    try {
                        val messagesArray = args.firstOrNull() as? JSONArray
                        if (messagesArray != null) {
                            val messages = parseMessageArray(messagesArray)
                            _incomingMessages.value = messages
                            trySend(SocketEvent.MessageHistory(messages))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing historique: ${e.message}", e)
                    }
                }

                on("newMessage") { args ->
                    Log.d(TAG, "Nouveau message: ${args.contentToString()}")
                    try {
                        val messageObj = args.firstOrNull() as? JSONObject
                        if (messageObj != null) {
                            val message = parseMessage(messageObj)
                            _incomingMessages.value = _incomingMessages.value + message
                            trySend(SocketEvent.NewMessage(message))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing message: ${e.message}", e)
                    }
                }

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

    fun disconnect() {
        Log.d(TAG, "Déconnexion...")
        socket?.disconnect()
        socket?.off()
        socket = null
        currentUsername = null
        _connectionState.value = ConnectionState.Disconnected
        _incomingMessages.value = emptyList()
        _conversations.value = emptyList()
        _users.value = emptyList()
        _currentConversationMessages.value = emptyList()
    }

    fun isConnected(): Boolean = socket?.connected() == true
    fun getCurrentUsername(): String? = currentUsername

    // ============== UTILISATEURS ==============

    fun getAllUsers(callback: (Boolean, List<UserDto>) -> Unit) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, emptyList())
            return
        }

        currentSocket.emit("getAllUsers", arrayOf(JSONObject())) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                if (success) {
                    val usersArray = responseObj?.getJSONArray("users")
                    val users = parseUsersArray(usersArray)
                    _users.value = users
                    callback(true, users)
                } else {
                    callback(false, emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur getAllUsers: ${e.message}")
                callback(false, emptyList())
            }
        }
    }

    fun searchUsers(query: String, callback: (Boolean, List<UserDto>) -> Unit) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, emptyList())
            return
        }

        val data = JSONObject().put("query", query)
        currentSocket.emit("searchUsers", arrayOf(data)) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                if (success) {
                    val usersArray = responseObj?.getJSONArray("users")
                    val users = parseUsersArray(usersArray)
                    _users.value = users
                    callback(true, users)
                } else {
                    callback(false, emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur searchUsers: ${e.message}")
                callback(false, emptyList())
            }
        }
    }

    // ============== CONVERSATIONS ==============

    fun getConversations(callback: (Boolean, List<ConversationDto>) -> Unit) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, emptyList())
            return
        }

        currentSocket.emit("getConversations", arrayOf(JSONObject())) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                if (success) {
                    val convsArray = responseObj?.getJSONArray("conversations")
                    val conversations = parseConversationsArray(convsArray)
                    _conversations.value = conversations
                    callback(true, conversations)
                } else {
                    callback(false, emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur getConversations: ${e.message}")
                callback(false, emptyList())
            }
        }
    }

    fun createPrivateConversation(
        recipientUsername: String,
        callback: (Boolean, ConversationDto?, String?) -> Unit
    ) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, null, "Non connecté")
            return
        }

        val data = JSONObject().put("recipientUsername", recipientUsername)
        currentSocket.emit("createPrivateConversation", arrayOf(data)) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                if (success) {
                    val convJson = responseObj?.getJSONObject("conversation")
                    val conversation = convJson?.let { parseConversation(it) }
                    callback(true, conversation, null)
                } else {
                    val error = responseObj?.optString("error", "Erreur inconnue")
                    callback(false, null, error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur createPrivateConversation: ${e.message}")
                callback(false, null, e.message)
            }
        }
    }

    fun createGroupConversation(
        name: String,
        memberUsernames: List<String>,
        callback: (Boolean, ConversationDto?, String?) -> Unit
    ) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, null, "Non connecté")
            return
        }

        val membersArray = JSONArray(memberUsernames)
        val data = JSONObject()
            .put("name", name)
            .put("memberUsernames", membersArray)

        currentSocket.emit("createGroupConversation", arrayOf(data)) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                if (success) {
                    val convJson = responseObj?.getJSONObject("conversation")
                    val conversation = convJson?.let { parseConversation(it) }
                    callback(true, conversation, null)
                } else {
                    val error = responseObj?.optString("error", "Erreur inconnue")
                    callback(false, null, error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur createGroupConversation: ${e.message}")
                callback(false, null, e.message)
            }
        }
    }

    // ============== MESSAGES ==============

    fun getConversationMessages(conversationId: Int, callback: (Boolean, List<MessageDto>) -> Unit) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, emptyList())
            return
        }

        val data = JSONObject().put("conversationId", conversationId)
        currentSocket.emit("getConversationMessages", arrayOf(data)) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                if (success) {
                    val messagesArray = responseObj?.getJSONArray("messages")
                    val messages = parseConversationMessagesArray(messagesArray, conversationId)
                    _currentConversationMessages.value = messages
                    callback(true, messages)
                } else {
                    callback(false, emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur getConversationMessages: ${e.message}")
                callback(false, emptyList())
            }
        }
    }

    fun sendMessageToConversation(
        conversationId: Int,
        content: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback(false, "Non connecté")
            return
        }

        val data = JSONObject()
            .put("conversationId", conversationId)
            .put("content", content)

        currentSocket.emit("sendMessageToConversation", arrayOf(data)) { response ->
            try {
                val responseObj = response.firstOrNull() as? JSONObject
                val success = responseObj?.optBoolean("success", false) ?: false
                val error = responseObj?.optString("error")
                callback(success, error)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur sendMessageToConversation: ${e.message}")
                callback(false, e.message)
            }
        }
    }

    // Legacy: pour compatibilité avec l'ancien système
    fun sendMessage(content: String, callback: ((Boolean, String?) -> Unit)? = null) {
        val currentSocket = socket
        if (currentSocket == null || !currentSocket.connected()) {
            callback?.invoke(false, "Non connecté")
            return
        }

        val payload = JSONObject().apply {
            put("content", content)
        }

        currentSocket.emit("sendMessage", arrayOf(payload)) { response ->
            val responseObj = response.firstOrNull() as? JSONObject
            val success = responseObj?.optBoolean("success", true) ?: true
            val error = responseObj?.optString("error")
            callback?.invoke(success, error)
        }
    }

    // ============== PARSERS ==============

    private fun parseUsersArray(array: JSONArray?): List<UserDto> {
        if (array == null) return emptyList()
        val users = mutableListOf<UserDto>()
        for (i in 0 until array.length()) {
            try {
                val userJson = array.getJSONObject(i)
                users.add(
                    UserDto(
                        id = userJson.getInt("id"),
                        username = userJson.getString("username"),
                        isOnline = userJson.optBoolean("isOnline", false)
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur parsing user index $i: ${e.message}")
            }
        }
        return users
    }

    private fun parseConversationsArray(array: JSONArray?): List<ConversationDto> {
        if (array == null) return emptyList()
        val conversations = mutableListOf<ConversationDto>()
        for (i in 0 until array.length()) {
            try {
                val convJson = array.getJSONObject(i)
                conversations.add(parseConversation(convJson))
            } catch (e: Exception) {
                Log.e(TAG, "Erreur parsing conversation index $i: ${e.message}")
            }
        }
        return conversations
    }

    private fun parseConversation(json: JSONObject): ConversationDto {
        val lastMessageJson = json.optJSONObject("lastMessage")
        val lastMessage = lastMessageJson?.let {
            LastMessageDto(
                content = it.getString("content"),
                sender = it.getString("sender"),
                sentAt = it.getString("sentAt")
            )
        }

        return ConversationDto(
            id = json.getInt("id"),
            type = json.getString("type"),
            name = json.getString("name"),
            memberCount = json.optInt("memberCount", 0),
            createdAt = json.getString("createdAt"),
            lastMessage = lastMessage
        )
    }

    private fun parseConversationMessagesArray(array: JSONArray?, conversationId: Int): List<MessageDto> {
        if (array == null) return emptyList()
        val messages = mutableListOf<MessageDto>()
        for (i in 0 until array.length()) {
            try {
                val msgJson = array.getJSONObject(i)
                messages.add(
                    MessageDto(
                        id = msgJson.optString("id", msgJson.optInt("id").toString()),
                        content = msgJson.getString("content"),
                        sender = msgJson.getString("sender"),
                        sentAt = parseSentAt(msgJson)
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur parsing message index $i: ${e.message}")
            }
        }
        return messages
    }

    private fun parseConversationMessage(obj: JSONObject): MessageDto {
        return MessageDto(
            id = obj.optString("id", obj.optInt("id").toString()),
            content = obj.getString("content"),
            sender = obj.getString("sender"),
            sentAt = parseSentAt(obj)
        )
    }

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

    private fun parseMessage(obj: JSONObject): MessageDto {
        return MessageDto(
            id = obj.optString("id", null),
            content = obj.optString("content", ""),
            sender = obj.optString("sender", "Inconnu"),
            sentAt = parseSentAt(obj)
        )
    }

    private fun parseSentAt(obj: JSONObject): Long {
        return try {
            obj.optLong("sentAt", 0).takeIf { it != 0L }
                ?: try {
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
    data object JoinSuccess : SocketEvent()
    data class Error(val message: String) : SocketEvent()
    data class MessageHistory(val messages: List<MessageDto>) : SocketEvent()
    data class NewMessage(val message: MessageDto) : SocketEvent()
    data class NewConversationMessage(val message: MessageDto) : SocketEvent()
}

