package fr.supdevinci.b3dev.applimenu.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val isOnline: Boolean = false
)

