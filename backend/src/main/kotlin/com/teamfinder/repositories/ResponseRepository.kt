package com.teamfinder.repositories

import com.teamfinder.database.DatabaseFactory.dbQuery
import com.teamfinder.models.*
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime

class ResponseRepository {

    // 1. Отправить заявку на участие в проекте
    suspend fun createResponse(projectId: Int, userId: Int, roleId: Int?, message: String?): Boolean = dbQuery {
        Responses.insert {
            it[Responses.projectId] = projectId
            it[Responses.userId] = userId
            it[Responses.roleId] = roleId
            it[Responses.message] = message
            it[status] = "рассматривается"
            it[createdAt] = LocalDateTime.now()
        }.insertedCount > 0
    }

    // 2. Получить все отклики для конкретного проекта (для автора)
    suspend fun getResponsesForProject(projectId: Int): List<UserResponseData> = dbQuery {
        (Responses innerJoin Users)
            .select { Responses.projectId eq projectId }
            .map { row ->
                UserResponseData(
                    responseId = row[Responses.id],
                    userId = row[Users.id],
                    username = row[Users.username],
                    message = row[Responses.message],
                    status = row[Responses.status]
                )
            }
    }

    // 2b. Получить список участников проекта (принято/активно)
    suspend fun getProjectMembers(projectId: Int): List<ProjectMemberData> = dbQuery {
        (Responses innerJoin Users)
            .select { 
                (Responses.projectId eq projectId) and 
                (Responses.status.inList(listOf("принято", "активно")))
            }
            .map { row ->
                ProjectMemberData(
                    userId = row[Users.id],
                    username = row[Users.username],
                    firstName = row[Users.firstName],
                    avatarUrl = row[Users.avatarUrl],
                    roleName = null // можно добавить если нужно
                )
            }
    }

    // 2c. Получить список проектов, в которых участвует пользователь
    suspend fun getUserParticipationProjects(userId: Int): List<Int> = dbQuery {
        Responses
            .select { 
                (Responses.userId eq userId) and 
                (Responses.status.inList(listOf("принято", "активно")))
            }
            .map { row -> row[Responses.projectId] }
            .distinct()
    }

    // 3. Изменить статус отклика (Принять/Отклонить)
    suspend fun updateStatus(responseId: Int, newStatus: String): Boolean = dbQuery {
        Responses.update({ Responses.id eq responseId }) {
            it[status] = newStatus
        } > 0
    }

    // 4. Найти отклик по ID (чтобы проверить, кто автор проекта)
    suspend fun findResponseById(id: Int) = dbQuery {
        Responses.select { Responses.id eq id }.singleOrNull()
    }
}

// DTO для списка откликов
@kotlinx.serialization.Serializable
data class UserResponseData(
    val responseId: Int,
    val userId: Int,
    val username: String,
    val message: String?,
    val status: String
)

// DTO для участников проекта
@kotlinx.serialization.Serializable
data class ProjectMemberData(
    val userId: Int,
    val username: String,
    val firstName: String,
    val avatarUrl: String?,
    val roleName: String?
)