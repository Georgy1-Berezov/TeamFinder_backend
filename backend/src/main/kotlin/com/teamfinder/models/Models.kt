package com.teamfinder.models

import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.date
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.ReferenceOption
import java.time.LocalDateTime

// ============================================
// 1. ENUMS (ПЕРЕЧИСЛЕНИЯ)
// ============================================

@Serializable
enum class UserRole { 
    USER, ADMIN, MODERATOR 
}

@Serializable
enum class ProjectStage { 
    IDEA, DEVELOPMENT, TESTING, COMPLETED 
}

// ============================================
// 2. DATA CLASSES (ОБЪЕКТЫ ДЛЯ КОДА И API)
// ============================================

@Serializable
data class User(
    val id: Int? = null,
    val username: String="",
    val email: String="",
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,

    // --- БЛОК: ОБРАЗОВАНИЕ (из дизайна) ---
    val age: Int? = null,
    val university: String? = null,    // "ЮФУ"
    val faculty: String? = null,       // "САПР"
    val programCode: String? = null,   // "09.03.02"
    val city: String? = null,          // "Таганрог"
    val studyMode: String? = null,
    // --- БЛОК: ДОСТУПНОСТЬ И СВЯЗЬ ---
    val schedule: String? = null,      // "2–3 ч/день"
    val contacts: List<ContactMethod> = emptyList(), // ЗАМЕНИЛИ telegram
    val portfolioUrl: String? = null,

    // --- СПИСКИ (JSONB) ---
    val skills: List<Skill> = emptyList(),
    val qualities: List<String> = emptyList(), // "Ответственность, честность"
    val interests: List<String> = emptyList(),
    val goals: String? = null,

    // --- СТАТИСТИКА (Блок как в Авито) ---
    val viewsCount: Int = 0,
    val bookmarksCount: Int = 0,
    val projectsCount: Int = 0,

    // --- СЛУЖЕБНОЕ ---
    val isOnline: Boolean = false,     // Зеленая точка в дизайне
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val createdAt: String? = null
)
@Serializable
data class ContactMethod(
    val type: String,  // "Telegram", "Phone", "VK", "WhatsApp"
    val value: String  // "@username" или "+7..."
)
@Serializable
data class Skill(
    val name: String,
    val level: String = "Beginner"
)

@Serializable
data class Project(
    val id: Int? = null,
    val authorId: Int,
    val title: String,
    val description: String? = null,
    val status: String = "идея",
    val deadline: String? = null,
    val industry: String? = null,
    val createdAt: String? = null,
    val isActive: Boolean = true,
    val roles: List<Role> = emptyList(),
    val tags: List<String> = emptyList(),
    val authorName: String? = null, 
    val likesCount: Int = 0 
)
// Data class для API
@Serializable
data class Notification(
    val id: Int? = null,
    val title: String,
    val content: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAt: String? = null
)
@Serializable
data class Role(
    val id: Int? = null,
    val projectId: Int? = null,
    val roleName: String,
    val requiredSkills: String = "[]",
    val spotsTotal: Int,
    val spotsFilled: Int = 0
)
@Serializable
data class Comment(
    val id: Int? = null,
    val projectId: Int = 0,
    val userId: Int = 0,
    val content: String,
    val createdAt: String? = null
)
@Serializable
data class MessageResponse(val message: String)

@Serializable
data class ErrorResponse(val error: String)

// ============================================
// 3. EXPOSED TABLES (СТРУКТУРА ТАБЛИЦ БД)
// ============================================

object Users : Table("users") {
    // --- БАЗОВЫЕ ДАННЫЕ ---
    val id = integer("user_id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex().nullable()
    val passwordHash = varchar("password_hash", 255).nullable()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100).nullable()
    val avatarUrl = text("avatar_url").nullable()

    // --- ОБРАЗОВАНИЕ И ЛОКАЦИЯ (из карточки дизайна) ---
    val age = integer("age").nullable()                     // "19 лет"
    val university = varchar("university", 255).nullable()  // "ЮФУ"
    val faculty = varchar("faculty", 100).nullable()        // "САПР"
    val programCode = varchar("program_code", 50).nullable() // "09.03.02"
    val studyMode = varchar("study_mode", 50).nullable()    // "очная"
    val city = varchar("city", 100).nullable()              // "Таганрог"

    // --- ДОСТУПНОСТЬ И КОНТАКТЫ ---
    val schedule = varchar("schedule", 100).nullable()      // "2–3 ч/день"
    val contacts = text("contacts").default("[]") // Храним список контактов как JSON
    val portfolioUrl = text("portfolio_url").nullable()
    val goals = text("goals").nullable()                    // "Цели и пожелания"

    // --- СПИСКИ (храним как JSON строку) ---
    val skills = text("skills").default("[]")               // "Photoshop, React"
    val qualities = text("qualities").default("[]")         // "Честность, Порядочность"
    val interests = text("interests").default("[]")

    // --- СТАТИСТИКА (цифры в профиле) ---
    val viewsCount = integer("views_count").default(0)      // "47 просмотров"
    val bookmarksCount = integer("bookmarks_count").default(0) // "12 в избранном"
    val projectsCount = integer("projects_count").default(0)   // "3 проекта"

    // --- СТАТУСЫ ---
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.USER)
    val isActive = bool("is_active").default(true)
    val isOnline = bool("is_online").default(false)         // Зеленая точка в дизайне

    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val lastActive = datetime("last_active").nullable()

    override val primaryKey = PrimaryKey(id)
}
object UserAuth : Table("user_auth") {
    val id = integer("auth_id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val provider = varchar("provider", 20) // 'google', 'telegram'
    val providerId = varchar("provider_id", 255)
    override val primaryKey = PrimaryKey(id)
}

object Tags : Table("tags") {
    val id = integer("tag_id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    val category = varchar("category", 50).nullable()
    override val primaryKey = PrimaryKey(id)
}
object ProfileViews : Table("profile_views") {
    val viewerId = integer("viewer_id").references(Users.id) // Кто смотрел
    val targetId = integer("target_id").references(Users.id) // Кого смотрели
    val viewedAt = datetime("viewed_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(viewerId, targetId) // Уникальность связки
}

object Projects : Table("projects") {
    val id = integer("project_id").autoIncrement()
    val authorId = integer("author_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 200)
    val description = text("description").nullable()
    val status = varchar("status", 50).default("идея")
    val deadline = date("deadline").nullable()
    val industry = varchar("industry", 100).nullable()
    val likesCount = integer("likes_count").default(0) 
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val isActive = bool("is_active").default(true)
    
    override val primaryKey = PrimaryKey(id)
}

object ProjectTags : Table("project_tags") {
    val projectId = integer("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val tagId = integer("tag_id").references(Tags.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(projectId, tagId)
}

object ProjectRoles : Table("project_roles") {
    val id = integer("role_id").autoIncrement()
    val projectId = integer("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val roleName = varchar("role_name", 100)
    val requiredSkills = text("required_skills").default("[]")
    val spotsTotal = integer("spots_total")
    val spotsFilled = integer("spots_filled").default(0)
    override val primaryKey = PrimaryKey(id)
}

object Files : Table("files") {
    val id = integer("file_id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val entityType = varchar("entity_type", 20) // 'project', 'message'
    val entityId = integer("entity_id")
    val fileName = varchar("file_name", 255)
    val filePath = text("file_path")
    val uploadedAt = datetime("uploaded_at").clientDefault { LocalDateTime.now() }
    override val primaryKey = PrimaryKey(id)
}

object Responses : Table("responses") {
    val id = integer("response_id").autoIncrement()
    val projectId = integer("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val roleId = integer("role_id").references(ProjectRoles.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val message = text("message").nullable()
    val status = varchar("status", 50).default("рассматривается")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    override val primaryKey = PrimaryKey(id)
}

object Invitations : Table("invitations") {
    val id = integer("invitation_id").autoIncrement()
    val projectId = integer("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val fromUser = integer("from_user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val toUser = integer("to_user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val roleId = integer("role_id").references(ProjectRoles.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val status = varchar("status", 50).default("отправлено")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    override val primaryKey = PrimaryKey(id)
}
object Notifications : Table("notifications") {
    val id = integer("notification_id").autoIncrement()
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)      // "Новое сообщение", "Вас добавили в избранное"
    val content = text("content")          // "Кириллов Артём сохранил ваш профиль"
    val type = varchar("type", 50)         // "LIKE", "BOOKMARK", "RESPONSE", "MESSAGE"
    val isRead = bool("is_read").default(false)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)
}


object Messages : Table("messages") {
    val id = integer("message_id").autoIncrement()
    val senderId = integer("sender_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val chatType = varchar("chat_type", 20) // 'team', 'response'
    val chatId = integer("chat_id")
    val content = text("content")
    val timestamp = datetime("timestamp").clientDefault { LocalDateTime.now() }
    val isRead = bool("is_read").default(false)
    override val primaryKey = PrimaryKey(id)
}
object UserBookmarks : Table("user_bookmarks") {
    val ownerId = integer("owner_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val targetId = integer("target_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(ownerId, targetId)
}
object Comments : Table("comments") {
    val id = integer("comment_id").autoIncrement()
    val projectId = integer("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val content = text("content")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    
    override val primaryKey = PrimaryKey(id)
}

object ProjectLikes : Table("project_likes") {
    val projectId = integer("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    
    override val primaryKey = PrimaryKey(projectId, userId) // Ограничение: 1 лайк от 1 юзера
}