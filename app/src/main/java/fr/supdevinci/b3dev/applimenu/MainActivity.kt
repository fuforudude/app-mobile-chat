package fr.supdevinci.b3dev.applimenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.supdevinci.b3dev.applimenu.screens.AuthScreen
import fr.supdevinci.b3dev.applimenu.screens.ConversationChatScreen
import fr.supdevinci.b3dev.applimenu.screens.ConversationsScreen
import fr.supdevinci.b3dev.applimenu.screens.SearchUsersScreen
import fr.supdevinci.b3dev.applimenu.ui.theme.AppliMenuTheme
import fr.supdevinci.b3dev.applimenu.presentation.ChatViewModel

// États de navigation
sealed class Screen {
    data object Auth : Screen()
    data object Conversations : Screen()
    data object SearchUsers : Screen()
    data class Chat(val conversationId: Int, val conversationName: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppliMenuTheme {
                val myChatViewModel: ChatViewModel = viewModel()

                MainNavigation(viewModel = myChatViewModel)
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: ChatViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Auth) }
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val screen = currentScreen) {
                is Screen.Auth -> {
                    AuthScreen(
                        onLoginSuccess = { username, serverUrl ->
                            viewModel.connect(username, serverUrl)
                            currentScreen = Screen.Conversations
                        }
                    )
                }

                is Screen.Conversations -> {
                    ConversationsScreen(
                        viewModel = viewModel,
                        onConversationClick = { conversationId ->
                            // Trouver le nom de la conversation
                            val conversation = conversations.find { it.id == conversationId }
                            val name = conversation?.name ?: "Chat"
                            currentScreen = Screen.Chat(conversationId, name)
                        },
                        onSearchUsersClick = {
                            currentScreen = Screen.SearchUsers
                        },
                        onDisconnect = {
                            currentScreen = Screen.Auth
                        }
                    )
                }

                is Screen.SearchUsers -> {
                    SearchUsersScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = Screen.Conversations
                        },
                        onUserClick = { user ->
                            // La création de conversation est gérée dans SearchUsersScreen
                        },
                        onConversationCreated = { conversationId ->
                            // Trouver le nom de la conversation
                            val conversation = conversations.find { it.id == conversationId }
                            val name = conversation?.name ?: "Chat"
                            currentScreen = Screen.Chat(conversationId, name)
                        }
                    )
                }

                is Screen.Chat -> {
                    ConversationChatScreen(
                        viewModel = viewModel,
                        conversationId = screen.conversationId,
                        conversationName = screen.conversationName,
                        onBack = {
                            // Rafraîchir les conversations avant de revenir
                            viewModel.loadConversations()
                            currentScreen = Screen.Conversations
                        }
                    )
                }
            }
        }
    }
}