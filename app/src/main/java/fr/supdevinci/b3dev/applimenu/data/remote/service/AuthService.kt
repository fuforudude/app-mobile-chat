package fr.supdevinci.b3dev.applimenu.data.remote.service

import android.util.Log
import com.google.gson.GsonBuilder
import fr.supdevinci.b3dev.applimenu.data.remote.api.AuthApi
import fr.supdevinci.b3dev.applimenu.data.remote.dto.AuthResponse
import fr.supdevinci.b3dev.applimenu.data.remote.dto.LoginRequest
import fr.supdevinci.b3dev.applimenu.data.remote.dto.RegisterRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Service d'authentification REST
 */
class AuthService(baseUrl: String) {

    companion object {
        private const val TAG = "AuthService"
    }

    private val authApi: AuthApi

    init {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        authApi = retrofit.create(AuthApi::class.java)
    }

    /**
     * Inscription d'un nouvel utilisateur
     */
    suspend fun register(username: String, password: String): Result<AuthResponse> {
        return try {
            Log.d(TAG, "Tentative d'inscription pour: $username")
            val response = authApi.register(RegisterRequest(username, password))
            handleResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inscription: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Connexion d'un utilisateur existant
     */
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return try {
            Log.d(TAG, "Tentative de connexion pour: $username")
            val response = authApi.login(LoginRequest(username, password))
            handleResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur connexion: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun handleResponse(response: Response<AuthResponse>): Result<AuthResponse> {
        return if (response.isSuccessful && response.body() != null) {
            Log.d(TAG, "Authentification réussie")
            Result.success(response.body()!!)
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = when (response.code()) {
                401 -> "Identifiants incorrects"
                409 -> "Ce nom d'utilisateur existe déjà"
                400 -> "Données invalides"
                else -> "Erreur serveur: ${response.code()}"
            }
            Log.e(TAG, "Erreur HTTP ${response.code()}: $errorBody")
            Result.failure(Exception(errorMessage))
        }
    }
}

