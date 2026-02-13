package fr.supdevinci.b3dev.applimenu.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// URL par défaut - 10.0.2.2 pour émulateur Android (localhost de la machine hôte)
// Si tu testes sur un appareil physique, utilise l'IP de ton PC (ex: 192.168.1.x)
private const val DEFAULT_SERVER_URL = "http://10.0.2.2:3000"

@Composable
fun AuthScreen(
    onLoginSuccess: (username: String, serverUrl: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(DEFAULT_SERVER_URL) }
    var showServerField by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Chat App",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(48.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            label = { Text("Nom d'utilisateur") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null
        )

        Spacer(Modifier.height(16.dp))

        // Champ URL du serveur (toujours visible pour debug)
        OutlinedTextField(
            value = serverUrl,
            onValueChange = {
                serverUrl = it
                errorMessage = null
            },
            label = { Text("URL du serveur") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                when {
                    username.isBlank() -> {
                        errorMessage = "Veuillez entrer un nom d'utilisateur"
                    }
                    serverUrl.isBlank() -> {
                        errorMessage = "Veuillez entrer l'URL du serveur"
                    }
                    else -> {
                        isLoading = true
                        errorMessage = null
                        onLoginSuccess(username.trim(), serverUrl.trim())
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && username.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Se connecter")
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "💡 Émulateur: 10.0.2.2 | Appareil: IP de votre PC",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}