package com.teamfinder.repositories

import com.teamfinder.database.DatabaseFactory.dbQuery
import com.teamfinder.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import java.time.LocalDateTime

class ProjectRepository {

    // 1. МАППИНГ: Чистое преобразование
    private fun rowToProject(row: ResultRow): Project = Project(
        id = row[Projects.id],
        authorId = row[Projects.authorId],
        title = row[Projects.title],
        description = row[Projects.description],
        status = row[Projects.status],
        industry = row[Projects.industry],
        createdAt = row[Projects.createdAt].toString(),
        isActive = row[Projects.isActive],
        likesCount = row[Projects.likesCount],
        viewsCount = row[Projects.viewsCount]
    )

    // 2. ПОИСК ПО ID
    suspend fun findById(id: Int): Project? = dbQuery {
        Projects.selectAll().where { Projects.id eq id }
            .map { row ->
                rowToProject(row).copy(
                    roles = getRolesSync(id),
                    tags = getTagsSync(id),
                    authorName = getUserNameSync(row[Projects.authorId])
                )
            }.singleOrNull()
    }

    // 3. СОЗДАНИЕ ПРОЕКТА
    suspend fun create(project: Project): Project? = dbQuery {
        try {
            // 1. Вставка проекта
            val newProjectId = Projects.insert {
                it[Projects.authorId] = project.authorId
                it[Projects.title] = project.title
                it[Projects.description] = project.description
                it[Projects.status] = project.status
                it[Projects.industry] = project.industry
                it[Projects.createdAt] = LocalDateTime.now()
                it[Projects.isActive] = true
            }[Projects.id]

            // 2. МАГИЯ: Увеличиваем счетчик проектов у автора в таблице Users
            Users.update({ Users.id eq project.authorId }) {
                it[projectsCount] = Users.projectsCount plus 1
            }
            println("📈 [STATS] Projects count incremented for user: ${project.authorId}")

            // 3. Вставка ролей
            project.roles.forEach { roleData ->
                ProjectRoles.insert {
                    it[ProjectRoles.projectId] = newProjectId
                    it[ProjectRoles.roleName] = roleData.roleName
                    it[ProjectRoles.requiredSkills] = roleData.requiredSkills
                    it[ProjectRoles.spotsTotal] = roleData.spotsTotal
                    it[ProjectRoles.spotsFilled] = 0
                }
            }

            // 4. Вставка тегов
            project.tags.forEach { tagName ->
                val tagId = Tags.selectAll().where { Tags.name eq tagName }
                    .map { row -> row[Tags.id] }
                    .singleOrNull() ?: Tags.insert { t -> t[Tags.name] = tagName }[Tags.id]

                ProjectTags.insert {
                    it[ProjectTags.projectId] = newProjectId
                    it[ProjectTags.tagId] = tagId
                }
            }

            // 5. Возврат собранного объекта
            Projects.selectAll().where { Projects.id eq newProjectId }
                .map { row ->
                    rowToProject(row).copy(
                        roles = getRolesSync(newProjectId),
                        tags = getTagsSync(newProjectId),
                        authorName = getUserNameSync(project.authorId)
                    )
                }.singleOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 4. ЛЕНТА
    suspend fun getFeed(page: Int, limit: Int): List<Project> = dbQuery {
        Projects.selectAll()
            .limit(limit, offset = ((page - 1).coerceAtLeast(0) * limit).toLong())
            .orderBy(Projects.createdAt to SortOrder.DESC)
            .map { row ->
                val id = row[Projects.id]
                rowToProject(row).copy(
                    roles = getRolesSync(id),
                    tags = getTagsSync(id),
                    authorName = getUserNameSync(row[Projects.authorId])
                )
            }
    }

    // 5. ПУБЛИЧНЫЕ МЕТОДЫ (Для Роутов)
    suspend fun getTagsForProject(projectId: Int): List<String> = dbQuery { getTagsSync(projectId) }
    suspend fun getRolesForProject(projectId: Int): List<Role> = dbQuery { getRolesSync(projectId) }

    suspend fun searchByTag(tagName: String): List<Project> = dbQuery {
        (Projects innerJoin ProjectTags innerJoin Tags)
            .select(Projects.columns)
            .where { Tags.name.lowerCase() like "%${tagName.lowercase()}%" }
            .map { row -> rowToProject(row) }
    }

    // 6. СОЦИАЛЬНЫЕ ФУНКЦИИ
    suspend fun toggleLike(projectId: Int, userId: Int): Boolean = dbQuery {
        val exists = ProjectLikes.selectAll().where { (ProjectLikes.projectId eq projectId) and (ProjectLikes.userId eq userId) }.count() > 0
        if (exists) {
            ProjectLikes.deleteWhere { (ProjectLikes.projectId eq projectId) and (ProjectLikes.userId eq userId) }
            Projects.update({ Projects.id eq projectId }) {
                it.update(Projects.likesCount, Projects.likesCount minus 1)
            }
            false
        } else {
            ProjectLikes.insert {
                it[ProjectLikes.projectId] = projectId
                it[ProjectLikes.userId] = userId
                it[ProjectLikes.createdAt] = LocalDateTime.now()
            }
            Projects.update({ Projects.id eq projectId }) {
                it.update(Projects.likesCount, Projects.likesCount plus 1)
            }
            true
        }
    }

    suspend fun addComment(comment: Comment): Comment = dbQuery {
        // 1. Сохраняем сам комментарий
        val id = Comments.insert {
            it[projectId] = comment.projectId
            it[userId] = comment.userId
            it[content] = comment.content
            it[createdAt] = LocalDateTime.now()
        }[Comments.id]

        // 2. МАГИЯ УВЕДОМЛЕНИЙ: Оповещаем автора проекта
        try {
            // Находим, кто автор этого проекта
            val projectAuthorId = Projects.select(Projects.authorId)
                .where { Projects.id eq comment.projectId }
                .map { it[Projects.authorId] }
                .singleOrNull()

            // Узнаем имя того, кто оставил комментарий
            val commenterName = Users.select(Users.username)
                .where { Users.id eq comment.userId }
                .map { it[Users.username] }
                .singleOrNull() ?: "Кто-то"

            // Если комментирует НЕ сам автор — создаем уведомление
            if (projectAuthorId != null && projectAuthorId != comment.userId) {
                Notifications.insert {
                    it[userId] = projectAuthorId
                    it[title] = "Новый комментарий"
                    it[content] = "$commenterName оставил комментарий под вашим проектом"
                    it[type] = "COMMENT"
                    it[createdAt] = LocalDateTime.now()
                }
                println("🔔 Уведомление о комментарии создано для юзера $projectAuthorId")
            }
        } catch (e: Exception) {
            println("⚠️ Не удалось создать уведомление: ${e.message}")
        }

        comment.copy(id = id, createdAt = LocalDateTime.now().toString())
    }

    suspend fun getComments(projectId: Int): List<Comment> = dbQuery {
        Comments.selectAll().where { Comments.projectId eq projectId }
            .orderBy(Comments.createdAt to SortOrder.DESC)
            .map { row ->
                Comment(
                    id = row[Comments.id],
                    projectId = row[Comments.projectId],
                    userId = row[Comments.userId],
                    content = row[Comments.content],
                    createdAt = row[Comments.createdAt].toString()
                )
            }
    }

    // 7. СЛУЖЕБНЫЕ МЕТОДЫ (ВНУТРЕННИЕ)
    private fun getRolesSync(projectId: Int): List<Role> {
        return ProjectRoles.selectAll().where { ProjectRoles.projectId eq projectId }.map { row ->
            Role(
                id = row[ProjectRoles.id],
                projectId = row[ProjectRoles.projectId],
                roleName = row[ProjectRoles.roleName],
                requiredSkills = row[ProjectRoles.requiredSkills],
                spotsTotal = row[ProjectRoles.spotsTotal],
                spotsFilled = row[ProjectRoles.spotsFilled]
            )
        }
    }

    private fun getTagsSync(projectId: Int): List<String> {
        return (ProjectTags innerJoin Tags).select(Tags.name)
            .where { ProjectTags.projectId eq projectId }.map { row -> row[Tags.name] }
    }

    private fun getUserNameSync(userId: Int): String? {
        return Users.select(Users.username)
            .where { Users.id eq userId }
            .map { row -> row[Users.username] }
            .singleOrNull()
    }

    suspend fun update(id: Int, p: Project): Boolean = dbQuery {
        val updated = Projects.update({ Projects.id eq id }) {
            it[Projects.title] = p.title
            it[Projects.description] = p.description
            it[Projects.industry] = p.industry
            it[Projects.status] = p.status
        } > 0

        if (p.roles.isNotEmpty()) {
            ProjectRoles.deleteWhere { ProjectRoles.projectId eq id }
            p.roles.forEach { roleData ->
                ProjectRoles.insert {
                    it[ProjectRoles.projectId] = id
                    it[ProjectRoles.roleName] = roleData.roleName
                    it[ProjectRoles.requiredSkills] = roleData.requiredSkills
                    it[ProjectRoles.spotsTotal] = roleData.spotsTotal
                    it[ProjectRoles.spotsFilled] = 0
                }
            }
        }
        updated
    }

    suspend fun delete(id: Int): Boolean = dbQuery {
        Projects.deleteWhere { Projects.id eq id } > 0
    }

    suspend fun incrementViews(projectId: Int) = dbQuery {
        Projects.update({ Projects.id eq projectId }) {
            it.update(Projects.viewsCount, Projects.viewsCount plus 1)
        }
    }
}