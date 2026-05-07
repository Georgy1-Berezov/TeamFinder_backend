package com.teamfinder.routes

import com.teamfinder.models.*
import com.teamfinder.security.JwtConfig
import com.teamfinder.services.AuthService
import com.teamfinder.services.AuthResult
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

fun Route.authRouting(jwtConfig: JwtConfig) {
    val authService = AuthService(jwtConfig)

    route("/auth") {

        post("/register") {
            try {
                // Ktor сам распарсит JSON в объект благодаря ContentNegotiation
                val request = call.receive<RegisterRequest>()
                
                val result = authService.register(
                    username = request.username,
                    email = request.email,
                    password = request.password
                )
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.Created, result.toAuthResponse())
                    }
                    is AuthResult.Error -> {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse(result.message))
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Registration failed"))
            }
        }

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
                val result = authService.login(
                    email = request.email,
                    password = request.password
                )
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.OK, result.toAuthResponse())
                    }
                    is AuthResult.Error -> {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse(result.message))
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Login failed"))
            }
        }

        post("/refresh") {
            try {
                val request = call.receive<RefreshRequest>()
                val result = authService.refreshToken(request.refreshToken)
                
                when (result) {
                    is AuthResult.Success -> {
                        call.respond(HttpStatusCode.OK, result.toAuthResponse())
                    }
                    is AuthResult.Error -> {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse(result.message))
                    }
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Token refresh failed"))
            }
        }
    }
}

// Вспомогательная функция для маппинга данных (убирает дублирование кода)
private fun AuthResult.Success.toAuthResponse() = AuthResponse(
    accessToken = this.accessToken,
    refreshToken = this.refreshToken,
    user = UserResponse(
        id = this.user.id!!,
        username = this.user.username,
        email = this.user.email,
        firstName = this.user.firstName,
        lastName = this.user.lastName,
        avatarUrl = this.user.avatarUrl
    )
)