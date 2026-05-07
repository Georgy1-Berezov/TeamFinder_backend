package com.teamfinder.config

import com.typesafe.config.ConfigFactory
import java.io.File

data class AppConfig(
    val environment: String,
    val database: DatabaseConfig,
    val security: SecurityConfig,
    val cors: CORSConfig,
    val upload: UploadConfig,
    val email: EmailConfig,
    val redis: RedisConfig,
    val elasticsearch: ElasticsearchConfig
) {
    companion object {
        fun load(): AppConfig {
            val config = ConfigFactory.load()
            val env = System.getenv("APP_ENV") ?: "development"

            return AppConfig(
                environment = env,
                database = DatabaseConfig(
                    url = System.getenv("DB_URL") ?: config.getString("database.url"),
                    user = System.getenv("DB_USER") ?: config.getString("database.user"),
                    password = System.getenv("DB_PASSWORD") ?: config.getString("database.password"),
                    maxPoolSize = config.getInt("database.maxPoolSize")
                ),
                security = SecurityConfig(
                    secret = System.getenv("JWT_SECRET") ?: config.getString("security.jwtSecret"),
                    issuer = config.getString("security.jwtIssuer"),
                    accessTokenExpiry = config.getLong("security.accessTokenExpiry"),
                    refreshTokenExpiry = config.getLong("security.refreshTokenExpiry")
                ),
                cors = CORSConfig(
                    allowedOrigins = config.getStringList("cors.allowedOrigins"),
                    allowCredentials = config.getBoolean("cors.allowCredentials")
                ),
                upload = UploadConfig(
                    uploadDir = System.getenv("UPLOAD_DIR") ?: config.getString("upload.directory"),
                    maxFileSize = config.getLong("upload.maxFileSize"),
                    allowedTypes = config.getStringList("upload.allowedTypes")
                ),
                email = EmailConfig(
                    host = System.getenv("SMTP_HOST") ?: config.getString("email.host"),
                    port = config.getInt("email.port"),
                    username = System.getenv("SMTP_USERNAME") ?: config.getString("email.username"),
                    password = System.getenv("SMTP_PASSWORD") ?: config.getString("email.password")
                ),
                redis = RedisConfig(
                    host = System.getenv("REDIS_HOST") ?: config.getString("redis.host"),
                    port = config.getInt("redis.port")
                ),
                elasticsearch = ElasticsearchConfig(
                    host = System.getenv("ES_HOST") ?: config.getString("elasticsearch.host"),
                    port = config.getInt("elasticsearch.port")
                )
            )
        }
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)

data class SecurityConfig(
    val secret: String,
    val issuer: String,
    val accessTokenExpiry: Long,
    val refreshTokenExpiry: Long
)

data class CORSConfig(
    val allowedOrigins: List<String>,
    val allowCredentials: Boolean
)

data class UploadConfig(
    val uploadDir: String,
    val maxFileSize: Long,
    val allowedTypes: List<String>
)

data class EmailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String
)

data class RedisConfig(
    val host: String,
    val port: Int
)

data class ElasticsearchConfig(
    val host: String,
    val port: Int
)