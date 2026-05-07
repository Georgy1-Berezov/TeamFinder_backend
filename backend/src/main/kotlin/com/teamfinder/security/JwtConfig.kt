package com.teamfinder.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.teamfinder.config.SecurityConfig
import com.teamfinder.models.User
import io.ktor.server.auth.jwt.*
import java.util.*

class JwtConfig(private val config: SecurityConfig) {

    private val algorithm = Algorithm.HMAC256(config.secret)
 fun getSecret(): String = config.secret
    fun getIssuer(): String = config.issuer
    fun generateAccessToken(user: User): String {
        return JWT.create()
            .withIssuer(config.issuer)
            .withSubject(user.id.toString())
            .withClaim("email", user.email)
            .withClaim("username", user.username)
            .withClaim("role", user.role?.name ?: "USER")
            .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpiry))
            .withIssuedAt(Date())
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: Int): String {
        return JWT.create()
            .withIssuer(config.issuer)
            .withSubject(userId.toString())
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + config.refreshTokenExpiry))
            .withIssuedAt(Date())
            .sign(algorithm)
    }

    fun verifyToken(token: String): JWTPrincipal? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(config.issuer)
                .build()

            val jwt = verifier.verify(token)

            JWTPrincipal(jwt)
        } catch (e: JWTVerificationException) {
            null
        }
    }

    fun extractUserId(token: String): Int? {
        return try {
            val verifier = JWT.require(algorithm).build()
            val jwt = verifier.verify(token)
            jwt.subject?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}