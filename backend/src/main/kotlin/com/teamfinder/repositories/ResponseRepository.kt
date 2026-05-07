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