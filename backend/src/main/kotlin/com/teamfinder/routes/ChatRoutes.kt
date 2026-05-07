package com.teamfinder.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRouting() {
    route("/chats") {
        get {
            call.respond(mapOf("message" to "Chats list"))
        }
        
        get("/{id}/messages") {
            call.respond(mapOf("message" to "Chat messages"))
        }
    }
}