package com.teamfinder.repositories

import com.teamfinder.database.DatabaseFactory.dbQuery
import com.teamfinder.models.*
import com.teamfinder.utils.PasswordUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class UserRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val notifRepo = NotificationRepository()
    // 1. МАППИНГ
    private fun toUser(row: ResultRow): User = User(
        id = row[Users.id],
        username = row[Users.username],
        email = row[Users.email] ?: "",
        firstName = row[Users.firstName],
        lastName = row[Users.lastName],
        avatarUrl = row[Users.avatarUrl],
        age = row[Users.age],
        university = row[Users.university],
        faculty = row[Users.faculty],
        programCode = row[Users.programCode],
        city = row[Users.city],
        schedule = row[Users.schedule],
        contacts = parseContacts(row[Users.contacts]),
        portfolioUrl = row[Users.portfolioUrl],
        goals = row[Users.goals],
        skills = parseSkills(row[Users.skills]),
        qualities = parseList(row[Users.qualities]),
        interests = parseList(row[Users.interests]),
        viewsCount = row[Users.viewsCount],
        bookmarksCount = row[Users.bookmarksCount],
        projectsCount = row[Users.projectsCount],
        isOnline = row[Users.isOnline],
        role = row[Users.role],
        isActive = row[Users.isActive],
        createdAt = row[Users.createdAt].toString()
    )

    // 2. УНИКАЛЬНЫЕ ПРОСМОТРЫ


    // 3. ПОИСК ПО НАВЫКАМ (Исправлено через lowerCase)
    suspend fun searchBySkill(skillName: String): List<User> = dbQuery {
        Users.selectAll()
            .where { Users.skills.lowerCase() like "%${skillName.lowercase()}%" }
            .map { toUser(it) }
    }

    // 4. ОБНОВЛЕНИЕ ПРОФИЛЯ
    suspend fun updateProfile(id: Int, user: User): Boolean = dbQuery {
        Users.update({ Users.id eq id }) {
            it[firstName] = user.firstName ?: ""
            it[lastName] = user.lastName
            it[age] = user.age
            it[university] = user.university
            it[faculty] = user.faculty
            it[programCode] = user.programCode
            it[city] = user.city
            it[studyMode] = user.studyMode
            it[schedule] = user.schedule
            it[contacts] = json.encodeToString(user.contacts)
            it[portfolioUrl] = user.portfolioUrl
            it[goals] = user.goals
            it[qualities] = json.encodeToString(user.qualities)
            it[skills] = json.encodeToString(user.skills)
            it[interests] = json.encodeToString(user.interests)
        } > 0
    }

    // 5. ПОИСК И СЧЕТЧИК (Исправлено через lowerCase)
    suspend fun count(query: String = ""): Int = dbQuery {
        if (query.isBlank()) {
            Users.selectAll().count().toInt()
        } else {
            val q = query.lowercase()
            Users.selectAll()
                .where { (Users.username.lowerCase() like "%$q%") or (Users.firstName.lowerCase() like "%$q%") }
                .count().toInt()
        }
    }
    suspend fun toggleBookmark(ownerId: Int, targetId: Int): Boolean = dbQuery {
        val exists = UserBookmarks.selectAll().where {
            (UserBookmarks.ownerId eq ownerId) and (UserBookmarks.targetId eq targetId)
        }.count() > 0

        if (exists) {
            // 1. УДАЛЯЕМ ИЗБРАННОЕ
            UserBookmarks.deleteWhere { (UserBookmarks.ownerId eq ownerId) and (UserBookmarks.targetId eq targetId) }
            Users.update({ Users.id eq targetId }) { with(SqlExpressionBuilder) { it[bookmarksCount] = bookmarksCount - 1 } }
            false
        } else {
            // 2. ДОБАВЛЯЕМ В ИЗБРАННОЕ
            UserBookmarks.insert {
                it[UserBookmarks.ownerId] = ownerId
                it[UserBookmarks.targetId] = targetId
            }
            Users.update({ Users.id eq targetId }) { with(SqlExpressionBuilder) { it[bookmarksCount] = bookmarksCount + 1 } }

            // 3. МАГИЯ: СОЗДАЕМ УВЕДОМЛЕНИЕ ДЛЯ ТАКРГЕТ-ЮЗЕРА
            // Сначала узнаем имя того, кто добавил в избранное
            val senderName = Users.selectAll().where { Users.id eq ownerId }
                .map { it[Users.username] }
                .singleOrNull() ?: "Кто-то"

            // Вставляем запись в таблицу уведомлений (убедись, что Notifications объект создан в Models.kt)
            Notifications.insert {
                it[userId] = targetId // Кому придет (владельцу профиля)
                it[title] = "Вас добавили в избранное"
                it[content] = "$senderName сохранил ваш профиль"
                it[type] = "BOOKMARK"
                it[createdAt] = LocalDateTime.now()
            }

            true
        }
    }
    suspend fun search(query: String, page: Int, limit: Int): List<User> = dbQuery {
        val offset = (page - 1).coerceAtLeast(0) * limit
        val q = query.lowercase()
        Users.selectAll()
            .where { (Users.username.lowerCase() like "%$q%") or (Users.firstName.lowerCase() like "%$q%") }
            .limit(limit, offset.toLong())
            .map { toUser(it) }
    }

    // --- СТАНДАРТНЫЕ МЕТОДЫ ---
    // Стандартный поиск юзера (нужен для AuthService и внутренней логики)
    suspend fun findById(id: Int): User? = dbQuery {
        Users.selectAll().where { Users.id eq id }
            .map { toUser(it) }
            .singleOrNull()
    }
    // Объединенный метод: увеличивает счетчик и сразу возвращает юзера
    suspend fun getProfileWithView(targetId: Int, viewerId: Int?): User? = dbQuery {
        println("🔎 [DEBUG] Запрос профиля $targetId. Кто смотрит: $viewerId")

        if (viewerId != null && viewerId != targetId) {
            println("🚀 [DEBUG] Пробуем засчитать просмотр...")

            val alreadyViewed = ProfileViews.selectAll().where {
                (ProfileViews.viewerId eq viewerId) and (ProfileViews.targetId eq targetId)
            }.count() > 0

            if (!alreadyViewed) {
                ProfileViews.insert {
                    it[ProfileViews.viewerId] = viewerId
                    it[ProfileViews.targetId] = targetId
                    it[viewedAt] = LocalDateTime.now()
                }

                Users.update({ Users.id eq targetId }) {
                    it[viewsCount] = Users.viewsCount plus 1
                }
                println("✅ [DEBUG] Счетчик в базе успешно обновлен!")
            } else {
                println("ℹ️ [DEBUG] Этот юзер уже смотрел этот профиль сегодня.")
            }
        } else {
            println("⚠️ [DEBUG] Просмотр НЕ засчитан (viewerId null или это владелец)")
        }

        Users.selectAll().where { Users.id eq targetId }
            .map { toUser(it) }
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        Users.selectAll().where { Users.email eq email }.map { toUser(it) }.singleOrNull()
    }

    suspend fun findByUsername(username: String): User? = dbQuery {
        Users.selectAll().where { Users.username eq username }.map { toUser(it) }.singleOrNull()
    }

    suspend fun validateCredentials(email: String, password: String): User? = dbQuery {
        val row = Users.selectAll().where { Users.email eq email }.singleOrNull()
        if (row != null && row[Users.passwordHash]?.let { PasswordUtils.verify(password, it) } == true) toUser(row) else null
    }

    suspend fun create(user: User, password: String): User? = dbQuery {
        val passwordHash = PasswordUtils.hash(password)

        // Используем insert и сразу получаем результат
        val insertStatement = Users.insert {
            it[username] = user.username
            it[email] = user.email
            it[Users.passwordHash] = passwordHash
            it[firstName] = user.firstName ?: user.username
            it[lastName] = user.lastName
            it[role] = user.role
            it[isActive] = user.isActive
            it[createdAt] = LocalDateTime.now()

            // Инициализируем новые колонки пустыми значениями
            it[skills] = "[]"
            it[qualities] = "[]"
            it[interests] = "[]"
            it[contacts] = "[]"
        }

        // Сразу возвращаем созданного пользователя
        insertStatement.resultedValues?.firstOrNull()?.let { toUser(it) }
    }

    suspend fun getAll(page: Int, limit: Int) = dbQuery {
        val offset = (page - 1).coerceAtLeast(0) * limit
        Users.selectAll().limit(limit, offset.toLong()).map { toUser(it) }
    }

    private fun parseSkills(jsonStr: String?): List<Skill> = try {
        if (jsonStr.isNullOrBlank()) emptyList() else json.decodeFromString(jsonStr)
    } catch (e: Exception) { emptyList() }

    private fun parseList(jsonStr: String?): List<String> = try {
        if (jsonStr.isNullOrBlank()) emptyList() else json.decodeFromString(jsonStr)
    } catch (e: Exception) { emptyList() }

    private fun parseContacts(jsonStr: String?): List<ContactMethod> = try {
        if (jsonStr.isNullOrBlank()) emptyList() else json.decodeFromString(jsonStr)
    } catch (e: Exception) { emptyList() }

    suspend fun updateLastLogin(id: Int) = dbQuery {
        Users.update({ Users.id eq id }) { it[lastActive] = LocalDateTime.now() } > 0
    }
    suspend fun delete(id: Int) = dbQuery { Users.deleteWhere { Users.id eq id } > 0 }
}