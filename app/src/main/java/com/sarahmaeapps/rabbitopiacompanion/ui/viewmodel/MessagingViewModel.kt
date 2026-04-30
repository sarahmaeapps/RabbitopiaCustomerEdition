package com.sarahmaeapps.rabbitopiacompanion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarahmaeapps.rabbitopiacompanion.data.model.Message
import com.sarahmaeapps.rabbitopiacompanion.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessagingViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError

    fun getCurrentUserEmail(): String? {
        return repository.getCurrentUserEmail()
    }

    fun startListening() {
        val email = repository.getCurrentUserEmail() ?: return
        viewModelScope.launch {
            repository.getMessages(email).collectLatest {
                _messages.value = it
            }
        }
    }

    fun sendMessage(text: String, imageUri: android.net.Uri? = null) {
        val email = repository.getCurrentUserEmail() ?: return
        val userName = repository.getCurrentUserName() ?: "Customer"
        
        viewModelScope.launch {
            var imageUrl: String? = null
            
            if (imageUri != null) {
                if (repository.canUploadImage(email)) {
                    imageUrl = repository.uploadMessageImage(email, imageUri)
                } else {
                    _uploadError.value = "Image limit reached: One per hour."
                    return@launch
                }
            }

            val message = Message(
                senderId = email,
                senderName = userName,
                text = text,
                imageUrl = imageUrl
            )
            repository.sendMessage(email, message)
            _uploadError.value = null
        }
    }

    fun clearError() {
        _uploadError.value = null
    }
}
