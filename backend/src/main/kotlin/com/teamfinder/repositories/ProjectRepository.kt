package com.teamfinder.repositories

import com.teamfinder.database.DatabaseFactory.dbQuery
import com.teamfinder.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import java.time.LocalDateTime

class ProjectRepository {
    
    private fun toProject(row: ResultRow): Project = Project(
        id = row[Projects.id],
        authorId = row[Projects.authorId],
        title = row[Projects.title],
        description = row[Projects.description],
        status = row[Projects.status],
        likesCount = row[Projects.likesCount], // Читаем лайки
        createdAt = row[Projects.createdAt].toString()
    )

    suspend fun findById(id: Int): Project? = dbQuery {
        Projects.select { Projects.id eq id }.map { toProject(it) }.singleOrNull()
    }

    suspend fun create(project: Project): Project? = dbQuery {
        val insertStatement = Projects.insert {
            it[authorId] = project.authorId
            it[title] = project.title
            it[status] = project.status
            it[createdAt] = LocalDateTime.now()
        }
        insertStatement.resultedValues?.firstOrNull()?.let { toProject(it) }
    }

    suspend fun getFeed(page: Int, limit: Int) = dbQuery {
        Projects.selectAll()
            .limit(limit, ((page - 1) * limit).toLong())
            .orderBy(Projects.createdAt to SortOrder.DESC)
            .map { toProject(it) }
    }

    suspend fun update(id: Int, p: Project) = dbQuery {
        Projects.update({ Projects.id eq id }) { it[Projects.title] = p.title } > 0
    }

    suspend fun delete(id: Int) = dbQuery {
        Projects.deleteWhere { Projects.id eq id } > 0
    }

    // --- РЕАЛЬНАЯ ЛОГИКА ЛАЙКОВ ---
    suspend fun toggleLike(projectId: Int, userId: Int): Boolean = dbQuery {
        // 1. Проверяем, есть ли уже лайк
        val existingLike = ProjectLikes.select { 
            (ProjectLikes.projectId eq projectId) and (ProjectLikes.userId eq userId) 
        }.singleOrNull()

        if (existingLike != null) {
            // 2. Если лайк есть — удаляем
            ProjectLikes.deleteWhere { 
                (ProjectLikes.projectId eq projectId) and (ProjectLikes.userId eq userId) 
            }
            // Уменьшаем счетчик
            Projects.update({ Projects.id eq projectId }) {
                with(SqlExpressionBuilder) {
                    it[likesCount] = Projects.likesCount - 1
                }
            }
            false
        } else {
            // 3. Если лайка нет — ставим
            ProjectLikes.insert {
                it[ProjectLikes.projectId] = projectId
                it[ProjectLikes.userId] = userId
                it[createdAt] = LocalDateTime.now()
            }
            // Увеличиваем счетчик
            Projects.update({ Projects.id eq projectId }) {
                with(SqlExpressionBuilder) {
                    it[likesCount] = Projects.likesCount + 1
                }
            }
            true
        }
    }

    suspend fun incrementViews(id: Int) = true
   // 1. Добавить комментарий
    suspend fun addComment(comment: Comment): Comment? = dbQuery {
        val insertStatement = Comments.insert {
            it[projectId] = comment.projectId
            it[userId] = comment.userId
            it[content] = comment.content
            it[createdAt] = LocalDateTime.now()
        }
        
        insertStatement.resultedValues?.firstOrNull()?.let {
            Comment(
                id = it[Comments.id],
                projectId = it[Comments.projectId],
                userId = it[Comments.userId],
                content = it[Comments.content],
                createdAt = it[Comments.createdAt].toString()
            )
        }
    }

    // 2. Получить список комментариев
    suspend fun getComments(projectId: Int): List<Comment> = dbQuery {
        Comments.select { Comments.projectId eq projectId }
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
    // Поиск проектов по названию тега
    suspend fun searchByTag(tagName: String): List<Project> = dbQuery {
        (Projects innerJoin ProjectTags innerJoin Tags)
            .select { Tags.name like "%$tagName%" } // Используем нечувствительный к регистру поиск
            .orderBy(Projects.createdAt to SortOrder.DESC)
            .map { toProject(it) }
    }

    // Вспомогательный метод для получения всех тегов проекта (чтобы видеть их в выдаче)
    suspend fun getTagsForProject(projectId: Int): List<String> = dbQuery {
        (ProjectTags innerJoin Tags)
            .slice(Tags.name)
            .select { ProjectTags.projectId eq projectId }
            .map { it[Tags.name] }
    }
}