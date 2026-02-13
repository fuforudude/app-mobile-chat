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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.supdevinci.b3dev.applimenu.presentation.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.authLoading.collectAsState()
    val error by viewModel.authError.collectAsState()

    // Erreur combinée (locale ou du serveur)
    val displayError = passwordError ?: error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Titre
        Text(
            text = "Chat App",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isRegisterMode) "Créer un compte" else "Connexion",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        // Champ username
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                passwordError = null
                viewModel.clearAuthError()
            },
            label = { Text("Nom d'utilisateur") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(Modifier.height(16.dp))

        // Champ mot de passe
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
                viewModel.clearAuthError()
            },
            label = { Text("Mot de passe") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Masquer" else "Afficher"
                    )
                }
            }
        )

        // Confirmation mot de passe (mode inscription)
        if (isRegisterMode) {
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    viewModel.clearAuthError()
                },
                label = { Text("Confirmer le mot de passe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        // Message d'erreur
        if (displayError != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = displayError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(32.dp))

        // Bouton principal
        Button(
            onClick = {
                if (isRegisterMode) {
                    if (password == confirmPassword) {
                        viewModel.register(username.trim(), password)
                    } else {
                        // Afficher erreur mots de passe différents via un state local
                        passwordError = "Les mots de passe ne correspondent pas"
                    }
                } else {
                    viewModel.login(username.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank() &&
                    (!isRegisterMode || confirmPassword.isNotBlank())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isRegisterMode) "S'inscrire" else "Se connecter")
        }

        Spacer(Modifier.height(16.dp))

        // Lien pour basculer entre login et register
        TextButton(
            onClick = {
                isRegisterMode = !isRegisterMode
                viewModel.clearAuthError()
                confirmPassword = ""
            },
            enabled = !isLoading
        ) {
            Text(
                if (isRegisterMode)
                    "Déjà un compte ? Se connecter"
                else
                    "Pas de compte ? S'inscrire"
            )
        }
    }
}


