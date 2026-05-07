package com.teamfinder.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText("Internal Server Error: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}