package com.teamfinder.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.teamfinder.security.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity(jwtConfig: JwtConfig) {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.getSecret()))
                    .withIssuer(jwtConfig.getIssuer())
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject?.toIntOrNull()
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}