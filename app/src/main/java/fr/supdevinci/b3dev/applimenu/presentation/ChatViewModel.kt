// presentation/ChatViewModel.kt
package fr.supdevinci.b3dev.applimenu.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.supdevinci.b3dev.applimenu.data.repository.ChatRepositoryImpl
import fr.supdevinci.b3dev.applimenu.datasource.remote.FakeSocketDataSource
import fr.supdevinci.b3dev.applimenu.domain.Message
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val socket = FakeSocketDataSource()
    private val repository = ChatRepositoryImpl(socket)

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