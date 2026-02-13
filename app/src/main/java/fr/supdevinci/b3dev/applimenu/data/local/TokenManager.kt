package fr.supdevinci.b3dev.applimenu.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Gestionnaire de token JWT avec stockage persistant
 * Utilise SharedPreferences pour stocker le token
 * Note: Pour la production, utilisez EncryptedSharedPreferences
 */
class TokenManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Sauvegarder le token JWT
     */
    fun saveToken(token: String) {
        sharedPrefs.edit { putString(KEY_JWT_TOKEN, token) }
    }

    /**
     * Récupérer le token JWT
     */
    fun getToken(): String? {
        return sharedPrefs.getString(KEY_JWT_TOKEN, null)
    }

    /**
     * Sauvegarder les informations utilisateur
     */
    fun saveUserInfo(userId: Int, username: String) {
        sharedPrefs.edit {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
        }
    }

    /**
     * Récupérer le nom d'utilisateur
     */
    fun getUsername(): String? {
        return sharedPrefs.getString(KEY_USERNAME, null)
    }

    /**
     * Récupérer l'ID utilisateur
     */
    fun getUserId(): Int {
        return sharedPrefs.getInt(KEY_USER_ID, -1)
    }

    /**
     * Vérifier si l'utilisateur est connecté
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    /**
     * Effacer toutes les données d'authentification
     */
    fun clearAll() {
        sharedPrefs.edit {
            remove(KEY_JWT_TOKEN)
            remove(KEY_USERNAME)
            remove(KEY_USER_ID)
        }
    }
}


