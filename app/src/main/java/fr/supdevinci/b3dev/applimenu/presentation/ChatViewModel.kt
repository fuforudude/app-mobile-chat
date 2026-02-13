package fr.supdevinci.b3dev.applimenu.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.supdevinci.b3dev.applimenu.domain.Message
import fr.supdevinci.b3dev.applimenu.domain.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    val uiState: StateFlow<List<Message>> = repository.messages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSendMessage(text: String) {
        viewModelScope.launch {
            repository.sendMessage(text)
        }
    }
}