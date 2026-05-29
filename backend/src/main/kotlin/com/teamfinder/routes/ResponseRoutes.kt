package com.teamfinder.routes

import com.teamfinder.models.*
import com.teamfinder.repositories.ResponseRepository
import com.teamfinder.repositories.ProjectRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

// 1. ПЕРЕНОСИМ DTO НАВЕРХ (ЧТОБЫ ОНИ БЫЛИ ВИДНЫ ВЕЗДЕ)
@Serializable
data class ResponseRequest(
    val projectId: Int, 
    val roleId: Int? = null, 
    val message: String? = null
)

@Serializable
data class StatusUpdateRequest(
    val status: String
)

fun Route.responseRouting() {
    val responseRepo = ResponseRepository()
    val projectRepo = ProjectRepository()

    authenticate("auth-jwt") {
        route("/responses") {

            // ПОЛУЧИТЬ СВОИ ЗАЯВКИ (ПРОЕКТЫ, В КОТОРЫХ УЧАСТВУЮ)
            get("/my-projects") {
                try {
                    val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() 
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    
                    val projectIds = responseRepo.getUserParticipationProjects(userId)
                    call.respond(HttpStatusCode.OK, mapOf("projectIds" to projectIds))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error"))
                }
            }

            // ОТПРАВИТЬ ОТКЛИК
            post {
                try {
                    val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() 
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    
                    val req = call.receive<ResponseRequest>()
                    
                    val success = responseRepo.createResponse(req.projectId, userId, req.roleId, req.message)
                    if (success) {
                        call.respond(HttpStatusCode.Created, MessageResponse("Application sent"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Failed to send application"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid format"))
                }
            }

            // ПОЛУЧИТЬ ОТКЛИКИ ДЛЯ ПРОЕКТА
            get("/project/{projectId}") {
                val projectId = call.parameters["projectId"]?.toIntOrNull() 
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                val currentUserId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() 
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val project = projectRepo.findById(projectId) 
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Project not found"))

                if (project.authorId != currentUserId) {
                    return@get call.respond(
                        HttpStatusCode.Forbidden, 
                        ErrorResponse("Access denied: Only project author can see applicants")
                    )
                }

                val responses = responseRepo.getResponsesForProject(projectId)
                call.respond(HttpStatusCode.OK, responses)
            }

            // ПОЛУЧИТЬ УЧАСТНИКОВ ПРОЕКТА
            get("/project/{projectId}/members") {
                val projectId = call.parameters["projectId"]?.toIntOrNull() 
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                
                try {
                    val members = responseRepo.getProjectMembers(projectId)
                    call.respond(HttpStatusCode.OK, members)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error"))
                }
            }

            // ПРИНЯТЬ ИЛИ ОТКЛОНИТЬ
            patch("/{id}") {
                try {
                    val userId = call.principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() 
                        ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                    
                    val responseId = call.parameters["id"]?.toIntOrNull() 
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)
                    
                    val req = call.receive<StatusUpdateRequest>()

                    val responseRow = responseRepo.findResponseById(responseId) 
                        ?: return@patch call.respond(HttpStatusCode.NotFound)
                    
                    val projectIdFromResponse = responseRow[Responses.projectId]
                    val project = projectRepo.findById(projectIdFromResponse)
                    
                    if (project?.authorId != userId) {
                        return@patch call.respond(HttpStatusCode.Forbidden, ErrorResponse("Only author can change status"))
                    }

                    if (responseRepo.updateStatus(responseId, req.status)) {
                        call.respond(HttpStatusCode.OK, MessageResponse("Status updated to ${req.status}"))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Update failed"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Format error"))
                }
            }
        }
    }
}