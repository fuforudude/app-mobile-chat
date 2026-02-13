package fr.supdevinci.b3dev.applimenu.datasource.remote

import fr.supdevinci.b3dev.applimenu.data.remote.dto.MessageDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

class FakeSocketDataSource {
    val incomingMessages = flow {
        emit(
            MessageDto(
                id = "1",
                content = "Salut ! Bienvenue sur le chat",
                senderId = "123",
                sentAt = 25
            )
        )
        delay(5000)
        emit(
            MessageDto(
                id = "2",
                content = "N'oublie pas de commander ton plat !",
                senderId = "456",
                sentAt = 41
            )
        )
    }
    suspend fun send(text: String) {
        delay(500)
        println("Message envoyé : $text")
    }
}