package com.teamfinder.routes

import com.teamfinder.models.*
import com.teamfinder.repositories.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val skills: List<Skill> = emptyList(),
    val interests: List<String> = emptyList(),
    val goals: String? = null,
    val portfolioUrl: String? = null,
    val createdAt: String? = null
)

@Serializable
data class UsersListResponse(
    val data: List<UserProfileResponse>,
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val currentPage: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int
)

fun Route.userRouting() {
    val userRepository = UserRepository()

    route("/users") {

        // 1. Получить пользователя по ID (Теперь возвращает ПОЛНЫЙ профиль)
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid ID"))
            
            val user = userRepository.findById(id)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user.toResponse())
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
            }
        }

        // 2. Список всех пользователей с пагинацией
        get("/") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            
            val users = userRepository.getAll(page, limit)
            val total = userRepository.count()
            
            val response = UsersListResponse(
                data = users.map { it.toResponse() },
                pagination = PaginationInfo(
                    currentPage = page,
                    pageSize = limit,
                    totalItems = total,
                    totalPages = (total + limit - 1) / limit
                )
            )
            call.respond(HttpStatusCode.OK, response)
        }

        authenticate("auth-jwt") {

            // 3. Обновление профиля
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                
                if (id == null || userId == null || id != userId) {
                    return@put call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                }
                
                val updates = call.receive<User>() // Принимаем объект User с обновленными полями
                val success = userRepository.update(id, updates)
                
                if (success) {
                    call.respond(HttpStatusCode.OK, MessageResponse("Profile updated successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Update failed"))
                }
            }

            // 4. Удаление аккаунта
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                
                if (id == null || userId == null || id != userId) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                }
                
                if (userRepository.delete(id)) {
                    call.respond(HttpStatusCode.OK, MessageResponse("Account deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                }
            }

            // 5. Поиск пользователей
            get("/search") {
                val query = call.request.queryParameters["query"] ?: ""
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                
                val users = userRepository.search(query, page, limit)
                val total = userRepository.count(query)
                
                call.respond(HttpStatusCode.OK, UsersListResponse(
                    data = users.map { it.toResponse() },
                    pagination = PaginationInfo(page, limit, total, (total + limit - 1) / limit)
                ))
            }
        }
    }
}

// Функция-помощник для превращения модели БД в ответ API (без пароля!)
fun User.toResponse() = UserProfileResponse(
    id = this.id!!,
    username = this.username,
    email = this.email,
    firstName = this.firstName,
    lastName = this.lastName,
    avatarUrl = this.avatarUrl,
    skills = this.skills ?: emptyList(),
    interests = this.interests ?: emptyList(),
    goals = this.goals,
    portfolioUrl = this.portfolioUrl,
    createdAt = this.createdAt
)