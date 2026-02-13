package fr.supdevinci.b3dev.applimenu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String? = null,
    val user: UserInfoDto? = null
)

data class UserInfoDto(
    val id: Int = 0,
    val username: String? = null
)

