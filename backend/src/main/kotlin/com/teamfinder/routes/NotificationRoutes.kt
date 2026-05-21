package com.teamfinder.routes

import com.teamfinder.models.ErrorResponse
import com.teamfinder.models.MessageResponse
import com.teamfinder.repositories.NotificationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.notificationRouting() {
    val notifRepo = NotificationRepository()

    authenticate("auth-jwt") {
        route("/notifications") {

            // 1. ПОЛУЧИТЬ ВСЕ УВЕДОМЛЕНИЯ (Для экрана 03)
            get {
                val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val list = notifRepo.getForUser(userId)
                call.respond(HttpStatusCode.OK, list)
            }

            // 2. ПОМЕТИТЬ КАК ПРОЧИТАННОЕ (Когда юзер нажал на уведомление)
            patch("/{id}/read") {
                val notifId = call.parameters["id"]?.toIntOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest)

                val success = notifRepo.markAsRead(notifId)
                if (success) {
                    call.respond(HttpStatusCode.OK, MessageResponse("Notification marked as read"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Notification not found"))
                }
            }
        }
    }
}