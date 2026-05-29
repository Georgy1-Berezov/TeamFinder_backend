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
            if (config.migrate) {
                logger.warn("MIGRATION MODE: dropping all tables and recreating schema...")
                exec("DROP SCHEMA public CASCADE")
                exec("CREATE SCHEMA public")
                logger.warn("All tables dropped, recreating from scratch...")
            }

            logger.info("Verifying 15-table architecture...")

            SchemaUtils.createMissingTablesAndColumns(
                Users,
                UserAuth,
                Tags,
                Projects,
                ProjectTags,
                ProjectRoles,
                Files,
                Responses,
                Invitations,
                Messages,
                ProjectLikes,
                Comments,
                ProfileViews,
                Notifications,
                UserBookmarks
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