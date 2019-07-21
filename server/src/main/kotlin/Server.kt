package ru.kozobrodov

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import ru.kozobrodov.filetreedataprovider.FileDataProvider
import java.io.FileNotFoundException
import java.nio.file.NotDirectoryException

@Suppress("unused")
fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(CORS) {
        anyHost()
    }

    install(StatusPages) {
        exception<UnsupportedOperationException> {
            call.respond(HttpStatusCode.NotImplemented, it.message ?: "Not implemented")
        }
        exception<FileNotFoundException> {
            call.respond(HttpStatusCode.NotFound, it.message ?: "File was not found")
        }
        exception<NotDirectoryException> {
            call.respond(HttpStatusCode.BadRequest, it.message ?: "Not a directory")
        }
    }

    routing {
        get("/{path...}") {
            val path = call.parameters.getAll("path")
            if (path != null) {
                val baseDir = application
                        .environment
                        .config
                        .propertyOrNull("ktor.application.baseDir")
                        ?.getString() ?: "/"
                call.respond(FileDataProvider(baseDir).list(path))
            }
        }
    }
}