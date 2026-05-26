package com.teamfinder.plugins

// ЭТИ ИМПОРТЫ ГОВОРЯТ КОТЛИНУ, ГДЕ ИСКАТЬ ТВОИ ФУНКЦИИ
import com.teamfinder.routes.*
import com.teamfinder.security.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import io.ktor.http.*
import java.util.Properties
import java.io.File

fun Application.configureRouting(jwtConfig: JwtConfig) {
    routing {
        // 1. Проверка жизни сервера
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // 2. ИНФОРМАЦИЯ О БИЛДЕ
        get("/info") {
            val props = Properties()
            // Пытаемся найти файл build.info в ресурсах
            val buildInfoStream = Application::class.java.classLoader.getResourceAsStream("build.info")

            val buildTime = if (buildInfoStream != null) {
                props.load(buildInfoStream)
                props.getProperty("build_time")
            } else {
                "Unknown (Run ./gradlew build first)"
            }

            call.respond(mapOf(
                "app" to "TeamFinder Backend",
                "status" to "running",
                "last_build_at" to buildTime
            ))
        }

        // 3. РАЗДАЧА СТАТИКИ (Картинки)
        staticFiles("/static", File("uploads"))

        // 4. ПОДКЛЮЧЕНИЕ ВСЕХ МАРШРУТОВ ИЗ ПАПКИ ROUTES
        authRouting(jwtConfig)
        projectRouting(jwtConfig)
        userRouting()
        notificationRouting()
        responseRouting()
        uploadRouting()
    }
}