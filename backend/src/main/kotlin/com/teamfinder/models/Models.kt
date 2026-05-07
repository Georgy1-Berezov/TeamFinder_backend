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
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val skills: List<Skill> = emptyList(),
    val interests: List<String> = emptyList(),
    val goals: String? = null,
    val portfolioUrl: String? = null,
    val role: UserRole = UserRole.USER, // КРИТИЧНО ДЛЯ AUTH
    val isActive: Boolean = true,       // КРИТИЧНО ДЛЯ AUTH
    val createdAt: String? = null
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
    val id = integer("user_id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex().nullable()
    val passwordHash = varchar("password_hash", 255).nullable()
    val username = varchar("username", 50).uniqueIndex()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100).nullable()
    val avatarUrl = text("avatar_url").nullable()
    val skills = text("skills").default("[]")
    val interests = text("interests").default("[]")
    val goals = text("goals").nullable()
    val rating = decimal("rating", 3, 2).nullable()
    val responseTimeAvg = integer("response_time_avg").nullable()
    val portfolioUrl = text("portfolio_url").nullable()
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.USER)
    val isActive = bool("is_active").default(true)
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