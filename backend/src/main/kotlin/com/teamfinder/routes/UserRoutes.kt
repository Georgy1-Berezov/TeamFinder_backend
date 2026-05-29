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

// --- DTO ДЛЯ ДИЗАЙНА (Glassmorphism UI) ---

@Serializable
data class UserProfileResponse(
    val id: Int,
    val username: String,
    val fullName: String,
    val avatarUrl: String?,
    val universityHeader: String,
    val location: String,
    val ageDisplay: String,
    val schedule: String,
    val isOnline: Boolean,
    val isNew: Boolean,
    val skills: List<Skill>,
    val qualities: List<String>,
    val stats: UserStatsDto,
    val contacts: List<ContactMethod>, // Список контактов вместо одной строки
)

@Serializable
data class UserStatsDto(
    val views: Int,
    val saved: Int,
    val projects: Int
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

        // ВАЖНО: /search ДОЛЖЕН БЫТЬ ПЕРЕД /{id}!
        // Иначе Ktor cматчит "search" как {id}, не сможет распарсить в Int и вернёт 400.
        authenticate("auth-jwt") {
            // 1. ПОИСК ПОЛЬЗОВАТЕЛЕЙ
            get("/search") {
                val skill = call.request.queryParameters["skill"]
                val query = call.request.queryParameters["query"]

                val users = when {
                    !skill.isNullOrBlank() -> userRepository.searchBySkill(skill)
                    !query.isNullOrBlank() -> userRepository.search(query, 1, 20)
                    else -> userRepository.getAll(1, 20)
                }
                val total = userRepository.count(query ?: "")

                call.respond(
                    HttpStatusCode.OK, UsersListResponse(
                        data = users.map { it.toResponse() },
                        pagination = PaginationInfo(1, 20, total, (total + 19) / 20)
                    )
                )
            }
        }

        // 2. ПОЛУЧИТЬ ПРОФИЛЬ (С уникальными просмотрами)
        authenticate("auth-jwt", optional = true) {
            get("/{id}") {
                val targetId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)

                val principal = call.principal<JWTPrincipal>()
                val viewerId = principal?.payload?.subject?.toIntOrNull()

                val user = userRepository.getProfileWithView(targetId, viewerId)

                if (user != null) {
                    call.respond(HttpStatusCode.OK, user.toResponse())
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                }
            }
        }

        authenticate("auth-jwt") {
            // 3. ИЗБРАННОЕ
            post("/{id}/bookmark") {
                val targetId =
                    call.parameters["id"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val ownerId =
                    call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() ?: return@post call.respond(
                        HttpStatusCode.Unauthorized
                    )

                val isSaved = userRepository.toggleBookmark(ownerId, targetId)
                call.respond(HttpStatusCode.OK, mapOf("saved" to isSaved))
            }

            // 4. СПИСОК ВСЕХ ПОЛЬЗОВАТЕЛЕЙ
            get("/") {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                val users = userRepository.getAll(page, limit)
                val total = userRepository.count()

                call.respond(
                    HttpStatusCode.OK, UsersListResponse(
                        data = users.map { it.toResponse() },
                        pagination = PaginationInfo(page, limit, total, (total + limit - 1) / limit)
                    )
                )
            }

            // 5. ОБНОВЛЕНИЕ ПРОФИЛЯ
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()

                if (id == null || userId == null || id != userId) {
                    return@put call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                }

                try {
                    val updates = call.receive<User>()
                    val success = userRepository.updateProfile(id, updates)

                    if (success) {
                        call.respond(HttpStatusCode.OK, MessageResponse("Profile updated successfully"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Update failed"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid data format"))
                }
            }

            // 6. УДАЛЕНИЕ АККАУНТА
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
        }
    }
}

// --- МАППИНГ ПОД ДИЗАЙН ---
fun User.toResponse() = UserProfileResponse(
    id = this.id!!,
    username = this.username,
    fullName = "${this.lastName ?: ""} ${this.firstName ?: ""} ${this.username}".trim(),
    avatarUrl = this.avatarUrl,
    universityHeader = "🏛 ${this.university ?: "ВУЗ"} · ${this.faculty ?: "Кафедра"} · ${this.programCode ?: ""}",
    location = this.city ?: "Не указан",
    ageDisplay = "${this.age ?: "?"} лет",
    schedule = this.schedule ?: "Гибкий график",
    isOnline = this.isOnline,
    isNew = true, // Логику новизны можно добавить по дате регистрации
    skills = this.skills,
    qualities = this.qualities,
    contacts = this.contacts,
    stats = UserStatsDto(
        views = this.viewsCount,
        saved = this.bookmarksCount,
        projects = this.projectsCount
    )
)