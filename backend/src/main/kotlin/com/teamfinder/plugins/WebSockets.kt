package com.teamfinder.plugins

import com.teamfinder.services.ChatService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

fun Application.configureSockets() {
    val chatService = ChatService()
    val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    install(WebSockets) {
        pingPeriod = java.time.Duration.ofSeconds(15)
        timeout = java.time.Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        webSocket("/ws/chat") {
            val userId = call.parameters["userId"]?.toIntOrNull()
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User ID required"))

            // Добавляем сессию
            sessions.getOrPut(userId.toString()) { mutableSetOf() }.add(this)

            try {
                for (frame in incoming) {
    when (frame) {
        is Frame.Text -> {
            val text = frame.readText()
            println("Message from user $userId: $text")
        }
        // Добавляем обработку всех остальных типов фреймов
        is Frame.Binary, is Frame.Close, is Frame.Ping, is Frame.Pong -> {
            // Просто игнорируем
        }
        else -> {
            // На всякий случай
        }
    }
}
            } finally {
                // Удаляем сессию
                sessions[userId.toString()]?.remove(this)
            }
        }
    }
}