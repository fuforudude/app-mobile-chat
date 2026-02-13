package fr.supdevinci.b3dev.applimenu.domain.model

data class User(
    val id: Int,
    val username: String,
    val isOnline: Boolean = false
)

