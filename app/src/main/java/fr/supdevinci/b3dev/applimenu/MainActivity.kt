package fr.supdevinci.b3dev.applimenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.supdevinci.b3dev.applimenu.screens.ConversationChatScreen
import fr.supdevinci.b3dev.applimenu.screens.ConversationsScreen
import fr.supdevinci.b3dev.applimenu.screens.LoginScreen
import fr.supdevinci.b3dev.applimenu.screens.SearchUsersScreen
import fr.supdevinci.b3dev.applimenu.ui.theme.AppliMenuTheme
import fr.supdevinci.b3dev.applimenu.presentation.AuthViewModel

// États de navigation
sealed class Screen {
    data object Login : Screen()
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
                val authViewModel: AuthViewModel = viewModel()

                MainNavigation(viewModel = authViewModel)
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: AuthViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val conversations by viewModel.conversations.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    // Mettre à jour l'écran en fonction de l'authentification
    if (!isAuthenticated && currentScreen !is Screen.Login) {
        currentScreen = Screen.Login
    } else if (isAuthenticated && currentScreen is Screen.Login) {
        currentScreen = Screen.Conversations
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Afficher un loader pendant la vérification de session initiale
            if (authLoading && currentScreen is Screen.Login) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (val screen = currentScreen) {
                    is Screen.Login -> {
                        LoginScreen(viewModel = viewModel)
                    }

                    is Screen.Conversations -> {
                        ConversationsScreen(
                            viewModel = viewModel,
                            onConversationClick = { conversationId ->
                                val conversation = conversations.find { it.id == conversationId }
                                val name = conversation?.name ?: "Chat"
                                currentScreen = Screen.Chat(conversationId, name)
                            },
                            onSearchUsersClick = {
                                currentScreen = Screen.SearchUsers
                            },
                            onDisconnect = {
                                viewModel.logout()
                                currentScreen = Screen.Login
                            }
                        )
                    }

                    is Screen.SearchUsers -> {
                        SearchUsersScreen(
                            viewModel = viewModel,
                            onBack = {
                                currentScreen = Screen.Conversations
                            },
                            onUserClick = { _ -> },
                            onConversationCreated = { conversationId ->
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
                                viewModel.loadConversations()
                                currentScreen = Screen.Conversations
                            }
                        )
                    }
                }
            }
        }
    }
}