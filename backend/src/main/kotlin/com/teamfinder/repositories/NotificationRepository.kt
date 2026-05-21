package com.teamfinder.repositories

import com.teamfinder.database.DatabaseFactory.dbQuery
import com.teamfinder.models.*
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime

class NotificationRepository {

    // 1. Создать уведомление (будет вызываться автоматически при действиях)
    suspend fun create(userId: Int, title: String, content: String, type: String) = dbQuery {
        Notifications.insert {
            it[Notifications.userId] = userId
            it[Notifications.title] = title
            it[Notifications.content] = content
            it[Notifications.type] = type
            it[createdAt] = LocalDateTime.now()
        }
    }

    // 2. Получить список уведомлений пользователя
    suspend fun getForUser(userId: Int): List<Notification> = dbQuery {
        Notifications.select { Notifications.userId eq userId }
            .orderBy(Notifications.createdAt to SortOrder.DESC)
            .map { row ->
                Notification(
                    id = row[Notifications.id],
                    title = row[Notifications.title],
                    content = row[Notifications.content],
                    type = row[Notifications.type],
                    isRead = row[Notifications.isRead],
                    createdAt = row[Notifications.createdAt].toString()
                )
            }
    }

    // 3. Пометить как прочитанное
    suspend fun markAsRead(notificationId: Int) = dbQuery {
        Notifications.update({ Notifications.id eq notificationId }) {
            it[isRead] = true
        }>0
    }
}