package fr.supdevinci.b3dev.applimenu.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.supdevinci.b3dev.applimenu.datasource.remote.ConnectionState
import fr.supdevinci.b3dev.applimenu.presentation.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onDisconnect: () -> Unit = {}
) {
    val messages by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()

    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll vers le bas quand nouveaux messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar avec statut de connexion
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Indicateur de connexion
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .padding(end = 4.dp)
                    ) {
                        val statusColor = when (connectionState) {
                            is ConnectionState.Connected -> Color.Green
                            is ConnectionState.Connecting -> Color.Yellow
                            is ConnectionState.Disconnected -> Color.Red
                            is ConnectionState.Error -> Color.Red
                        }
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = statusColor)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = currentUsername ?: "Chat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Connecté"
                                is ConnectionState.Connecting -> "Connexion..."
                                is ConnectionState.Disconnected -> "Déconnecté"
                                is ConnectionState.Error -> "Erreur: ${(connectionState as ConnectionState.Error).message}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = {
                    viewModel.disconnect()
                    onDisconnect()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Déconnexion"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // Contenu principal
        when (connectionState) {
            is ConnectionState.Connecting -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Connexion au serveur...")
                    }
                }
            }

            is ConnectionState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "❌ Erreur de connexion",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (connectionState as ConnectionState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Vérifiez que:\n• Le serveur est démarré\n• L'URL est correcte\n• Le téléphone est sur le même réseau",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.disconnect()
                                onDisconnect()
                            }
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
            }

            else -> {
                // Liste des messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    state = listState,
                    reverseLayout = false,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (messages.isEmpty() && connectionState is ConnectionState.Connected) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Aucun message pour le moment.\nCommencez la conversation !",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                }

                // Barre d'envoi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Écrire un message...") },
                        enabled = connectionState is ConnectionState.Connected,
                        singleLine = false,
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                viewModel.onSendMessage(textState)
                                textState = ""
                            }
                        },
                        enabled = connectionState is ConnectionState.Connected && textState.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = if (connectionState is ConnectionState.Connected && textState.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}