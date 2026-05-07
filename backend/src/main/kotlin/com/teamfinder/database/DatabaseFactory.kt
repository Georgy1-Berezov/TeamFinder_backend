package com.teamfinder.database

import com.teamfinder.models.*
import com.teamfinder.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    fun init(config: DatabaseConfig) {
        logger.info("Initializing database connection to: ${config.url}")
        
        Database.connect(hikari(config))
        
        transaction {
            logger.info("Verifying 10-table architecture...")
            
            // СООТВЕТСТВИЕ ТВОЕМУ SQL (10 ТАБЛИЦ)
            SchemaUtils.createMissingTablesAndColumns(
                Users,           // 1. Пользователи
                UserAuth,        // 2. Авторизация (соцсети)
                Tags,            // 3. Справочник тегов
                Projects,        // 4. Проекты
                ProjectTags,     // 5. Связи проектов и тегов
                ProjectRoles,    // 6. Роли (вместо Vacancies)
                Files,           // 7. Файлы
                Responses,       // 8. Отклики
                Invitations,     // 9. Приглашения
                Messages,         // 10. Сообщения (чаты)
                ProjectLikes,
                Comments
            )
            
            logger.info("Database synchronized with new schema successfully")
        }
    }

    private fun hikari(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T = 
        newSuspendedTransaction(Dispatchers.IO) { block() }
}