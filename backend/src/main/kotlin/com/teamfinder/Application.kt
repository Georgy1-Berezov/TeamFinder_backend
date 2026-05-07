package com.teamfinder

import com.teamfinder.config.*
import com.teamfinder.database.DatabaseFactory
import com.teamfinder.plugins.*
import com.teamfinder.security.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Application")

    try {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            module()
        }.start(wait = true)
    } catch (e: Exception) {
        logger.error("Failed to start server: ${e.message}", e)
        System.exit(1)
    }
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting TeamFinder application...")

    // Конфигурация из переменных окружения
    val config = AppConfig.load()

    // Инициализация БД
    DatabaseFactory.init(config.database)

    // Настройка JWT
    val jwtConfig = JwtConfig(config.security)

    // Плагины Ktor
    configureSerialization()
    configureSecurity(jwtConfig)
    configureSockets()              // 1. WebSockets ДО маршрутов
    configureRouting(jwtConfig)      // 2. Маршруты ПОСЛЕ WebSockets
    configureMonitoring()
    configureStatusPages()
    configureCompression()
    configureCORS()

    logger.info("Application started successfully on port 8080")
}