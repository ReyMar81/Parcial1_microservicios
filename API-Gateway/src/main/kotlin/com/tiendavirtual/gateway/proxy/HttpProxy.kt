package com.tiendavirtual.gateway.proxy

import com.tiendavirtual.gateway.config.GatewayConfig
import com.sun.net.httpserver.HttpExchange
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class HttpProxy(private val config: GatewayConfig) {
    private val logger = LoggerFactory.getLogger(HttpProxy::class.java)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds))
        .build()

    data class ProxyResponse(
        val statusCode: Int,
        val body: String,
        val headers: Map<String, List<String>>
    )

    fun forwardRequest(
        exchange: HttpExchange,
        targetUrl: String,
        serviceName: String
    ): ProxyResponse {
        val method = exchange.requestMethod
        val body = exchange.requestBody.readBytes()

        logger.info("Reenviando a $serviceName: $method $targetUrl")

        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))

            // Copiar headers importantes
            copyImportantHeaders(exchange, requestBuilder)

            // Configurar metodo HTTP y body
            configureHttpMethod(method, body, requestBuilder)

            // Enviar petición con reintentos
            val response = sendWithRetry(requestBuilder.build(), serviceName)

            logger.info("Respuesta de $serviceName: ${response.statusCode()}")

            ProxyResponse(
                statusCode = response.statusCode(),
                body = response.body(),
                headers = response.headers().map()
            )

        } catch (e: Exception) {
            logger.error("Error reenviando a $serviceName: ${e.message}")
            ProxyResponse(
                statusCode = 500,
                body = """{"success":false,"error":"Error comunicándose con $serviceName: ${e.message}"}""",
                headers = emptyMap()
            )
        }
    }

    private fun copyImportantHeaders(exchange: HttpExchange, requestBuilder: HttpRequest.Builder) {
        exchange.requestHeaders.forEach { (key, values) ->
            if (key.lowercase() in listOf("content-type", "authorization", "accept", "user-agent")) {
                values.forEach { value ->
                    requestBuilder.header(key, value)
                }
            }
        }
    }

    private fun configureHttpMethod(
        method: String,
        body: ByteArray,
        requestBuilder: HttpRequest.Builder
    ) {
        when (method.uppercase()) {
            "GET" -> requestBuilder.GET()
            "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(body))
            "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(body))
            "PATCH" -> requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofByteArray(body))
            "DELETE" -> requestBuilder.DELETE()
            else -> throw IllegalArgumentException("Método no soportado: $method")
        }
    }

    private fun sendWithRetry(request: HttpRequest, serviceName: String): HttpResponse<String> {
        var lastException: Exception? = null

        repeat(config.retryAttempts) { attempt ->
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (e: Exception) {
                lastException = e
                if (attempt < config.retryAttempts - 1) {
                    logger.warn("Reintento ${attempt + 1}/${config.retryAttempts} para $serviceName")
                    Thread.sleep(config.retryDelayMs * (attempt + 1))
                }
            }
        }

        throw lastException ?: Exception("Error desconocido")
    }
}
