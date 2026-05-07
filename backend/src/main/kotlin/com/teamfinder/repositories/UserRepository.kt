package com.teamfinder.repositories

import com.teamfinder.database.DatabaseFactory.dbQuery
import com.teamfinder.models.*
import com.teamfinder.utils.PasswordUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class UserRepository {
    
    private fun toUser(row: ResultRow): User = User(
        id = row[Users.id],
        username = row[Users.username],
        email = row[Users.email] ?: "",
        firstName = row[Users.firstName],
        lastName = row[Users.lastName],
        avatarUrl = row[Users.avatarUrl],
        role = row[Users.role],
        isActive = row[Users.isActive],
        createdAt = row[Users.createdAt].toString()
    )

    suspend fun findById(id: Int): User? = dbQuery {
        Users.select { Users.id eq id }.map { toUser(it) }.singleOrNull()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }.map { toUser(it) }.singleOrNull()
    }

    suspend fun findByUsername(username: String): User? = dbQuery {
        Users.select { Users.username eq username }.map { toUser(it) }.singleOrNull()
    }

    suspend fun create(user: User, password: String): User? = dbQuery {
        val passwordHash = PasswordUtils.hash(password)
        
        // Вставляем и сразу получаем объект, не выходя из транзакции
        val insertStatement = Users.insert {
            it[username] = user.username
            it[email] = user.email
            it[Users.passwordHash] = passwordHash
            it[firstName] = user.firstName ?: user.username
            it[role] = user.role
            it[isActive] = user.isActive
            it[createdAt] = LocalDateTime.now()
        }
        
        insertStatement.resultedValues?.firstOrNull()?.let { toUser(it) }
    }

    suspend fun validateCredentials(email: String, password: String): User? = dbQuery {
        val row = Users.select { Users.email eq email }.singleOrNull()
        if (row != null && row[Users.passwordHash]?.let { PasswordUtils.verify(password, it) } == true) {
            toUser(row)
        } else null
    }

    suspend fun updateLastLogin(id: Int) = dbQuery {
        Users.update({ Users.id eq id }) { it[lastActive] = LocalDateTime.now() } > 0
    }

    suspend fun getAll(page: Int, limit: Int) = dbQuery {
        Users.selectAll().limit(limit, ((page - 1) * limit).toLong()).map { toUser(it) }
    }

    suspend fun count(q: String = "") = dbQuery { Users.selectAll().count().toInt() }
    suspend fun search(q: String, p: Int, l: Int) = getAll(p, l)
    suspend fun delete(id: Int) = dbQuery { Users.deleteWhere { Users.id eq id } > 0 }
    suspend fun update(id: Int, u: User) = dbQuery {
        Users.update({ Users.id eq id }) { it[firstName] = u.firstName ?: "" } > 0
    }
}