package fr.supdevinci.b3dev.applimenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.supdevinci.b3dev.applimenu.screens.AuthScreen
import fr.supdevinci.b3dev.applimenu.screens.ChatScreen
import fr.supdevinci.b3dev.applimenu.ui.theme.AppliMenuTheme
import fr.supdevinci.b3dev.applimenu.presentation.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppliMenuTheme {

                val myChatViewModel: ChatViewModel = viewModel()

                var isLoggedIn by remember { mutableStateOf(false) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (!isLoggedIn) {
                            AuthScreen(
                                onLoginSuccess = { username, serverUrl ->
                                    // Connecter au serveur WebSocket avec le username
                                    myChatViewModel.connect(username, serverUrl)
                                    isLoggedIn = true
                                }
                            )
                        } else {
                            ChatScreen(
                                viewModel = myChatViewModel,
                                onDisconnect = {
                                    isLoggedIn = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}