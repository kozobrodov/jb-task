package ru.kozobrodov

import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.Test
import org.junit.Assert.*
import java.io.File

// Since there is no extra logic in server, we only need to
// check responses here (especially, statuses)
@Suppress("EXPERIMENTAL_API_USAGE")
class ServerTest {

    @Test
    fun `list root directory`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/")) {
            assertTrue(response.content?.isNotEmpty() ?: false)
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `list subdirectory`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/Inner directory")) {
            assertTrue(response.content?.isNotEmpty() ?: false)
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `list ZIP archive`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/test-zip.zip")) {
            assertTrue(response.content?.isNotEmpty() ?: false)
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `attempt to list non-expandable file`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/test-image.jpg")) {
            assertEquals(HttpStatusCode.BadRequest, response.status())
        }
    }

    @Test
    fun `attempt to list unsupported type of archive`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/rar-archive.rar")) {
            assertEquals(HttpStatusCode.NotImplemented, response.status())
        }
    }

    @Test
    fun `attempt to list inner ZIP archive`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/zip-with-inner-zip.zip/test-zip.zip")) {
            assertEquals(HttpStatusCode.NotImplemented, response.status())
        }
    }

    @Test
    fun `attempt to list non-existing file`() = withTestApplication({
        module()
    }) {
        config(this)
        with(handleRequest(HttpMethod.Get, "/doesnt-exist.jar")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    private fun config(engine: ApplicationEngine) {
        (engine.environment.config as MapApplicationConfig).apply {
            val baseDir = File(javaClass.getResource("/TestData").toURI())
            put("ktor.application.baseDir", baseDir.absolutePath)
        }
    }
}