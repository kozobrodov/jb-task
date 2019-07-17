package ru.kozobrodov

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import java.io.FileNotFoundException

@Suppress("unused")
fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<UnsupportedOperationException> {
            call.respond(HttpStatusCode.NotImplemented, it.message ?: "Not implemented")
        }
        exception<FileNotFoundException> {
            call.respond(HttpStatusCode.NotFound)
        }
    }

    routing {
        get("/") {
            call.respond("Hello, world!")
        }
    }
}