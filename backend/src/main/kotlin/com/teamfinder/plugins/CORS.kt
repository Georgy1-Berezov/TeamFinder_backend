package com.teamfinder.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        // Разрешаем все локальные хосты
        allowHost("localhost:3000", schemes = listOf("http", "https"))
        allowHost("localhost:8080", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:3000", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:8080", schemes = listOf("http", "https"))
        
        // Разрешаем все методы
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        
        // Разрешаем все стандартные заголовки
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        
        // Разрешаем credentials
        allowCredentials = true
        
        // Опционально: разрешаем все хосты (для разработки)
        // anyHost()
    }
}