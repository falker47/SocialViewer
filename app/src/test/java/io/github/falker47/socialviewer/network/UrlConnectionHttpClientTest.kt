package io.github.falker47.socialviewer.network

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.InetSocketAddress

class UrlConnectionHttpClientTest {
    @Test
    fun redirectResolutionStillReturnsSuccessfulFinalUrl() = withServer { baseUrl ->
        assertEquals(
            "$baseUrl/pin/123/",
            UrlConnectionHttpClient().resolveFinalUrl("$baseUrl/short"),
        )
    }

    @Test
    fun linkResolutionHttpFailureKeepsStatusWithoutTheSharedUrl() = withServer { baseUrl ->
        val error = assertThrows(LinkResolutionHttpException::class.java) {
            UrlConnectionHttpClient().resolveFinalUrl("$baseUrl/error?token=private-link-token")
        }

        assertEquals(503, error.statusCode)
        assertNull(error.cause)
        assertFalse(error.message.orEmpty().contains("private-link-token"))
        assertFalse(error.message.orEmpty().contains(baseUrl))
    }

    @Test
    fun getStillReturnsHttpErrorsForProviderClassification() = withServer { baseUrl ->
        val response = UrlConnectionHttpClient().get("$baseUrl/error")

        assertEquals(503, response.statusCode)
        assertEquals("$baseUrl/error", response.finalUrl)
        assertEquals("Service unavailable", response.body)
    }

    private fun withServer(block: (String) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val status = when (exchange.requestURI.path) {
                "/short" -> {
                    exchange.responseHeaders.add("Location", "/pin/123/")
                    302
                }
                "/error" -> 503
                else -> 200
            }
            val body = if (status == 503) "Service unavailable" else "OK"
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }
}
