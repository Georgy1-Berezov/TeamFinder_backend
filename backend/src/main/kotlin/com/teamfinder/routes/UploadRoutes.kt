package com.teamfinder.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.*

fun Route.uploadRouting() {
    // 1. SECURITY: Only authenticated users can upload files
    authenticate("auth-jwt") {
        route("/uploads") {
            post {
                try {
                    val multipart = call.receiveMultipart()
                    var fileName: String? = null

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            // 2. FILE EXTENSION VALIDATION
                            val originalName = part.originalFileName ?: "file"
                            val extension = originalName.substringAfterLast('.', "bin").lowercase()
                            
                            // Добавь doc, docx и pdf в список разрешенных
val allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "doc", "docx", "pdf", "txt")
                            
                            if (extension !in allowedExtensions) {
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Unsupported file format: .$extension"))
                                return@forEachPart
                            }

                            // 3. SECURE FILENAME: Prevents overwriting and directory traversal attacks
                            fileName = "${UUID.randomUUID()}.$extension"
                            
                            val folder = File("uploads")
                            if (!folder.exists()) folder.mkdirs()
                            
                            // 4. PERFORMANCE: Streaming the file to disk (Efficient Memory Usage)
                            val file = File(folder, fileName!!)
                            part.streamProvider().use { input ->
                                file.outputStream().buffered().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        part.dispose()
                    }

                    if (fileName != null) {
                        call.respond(HttpStatusCode.Created, mapOf(
                            "message" to "File uploaded successfully",
                            "url" to "/static/$fileName"
                        ))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file found in request"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to upload file"))
                }
            }
        }
    }
}