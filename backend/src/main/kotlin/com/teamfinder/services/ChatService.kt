package com.teamfinder.services

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ChatService {
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()
    
    private val _typing = MutableSharedFlow<String>()
    val typing = _typing.asSharedFlow()
    
    suspend fun getChatParticipants(chatId: Int): List<Int> {
        // Временная заглушка
        return emptyList()
    }
}