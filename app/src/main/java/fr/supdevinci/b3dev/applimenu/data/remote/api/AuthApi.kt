package fr.supdevinci.b3dev.applimenu.data.remote.api

import fr.supdevinci.b3dev.applimenu.data.remote.dto.AuthResponse
import fr.supdevinci.b3dev.applimenu.data.remote.dto.LoginRequest
import fr.supdevinci.b3dev.applimenu.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}

