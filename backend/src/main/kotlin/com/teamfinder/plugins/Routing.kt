package com.teamfinder.plugins

import com.teamfinder.routes.*
import com.teamfinder.security.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.* 

fun Application.configureRouting(jwtConfig: JwtConfig) {
    routing {
        // Проверка работоспособности сервера
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // 2. РАЗДАЧА ФАЙЛОВ: Делаем папку "uploads" доступной по ссылке /static/
        // Теперь если файл лежит в uploads/abc.jpg, он будет доступен как http://localhost:8080/static/abc.jpg
        static("/static") {
            files("uploads")
        }
        
        // 3. ПУБЛИЧНЫЕ И СМЕШАННЫЕ МАРШРУТЫ
        authRouting(jwtConfig)
        projectRouting(jwtConfig)
        userRouting()
        notificationRouting() // Чтобы Ktor увидел эти пути
        responseRouting()
        uploadRouting()
    }
}