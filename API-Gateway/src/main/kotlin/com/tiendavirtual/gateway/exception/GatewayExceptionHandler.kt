package com.tiendavirtual.gateway.exception

import com.sun.net.httpserver.HttpExchange
import org.slf4j.LoggerFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class GatewayExceptionHandler {
    private val logger = LoggerFactory.getLogger(GatewayExceptionHandler::class.java)

    @Serializable
    data class ErrorResponse(
        val success: Boolean = false,
        val error: String,
        val code: String,
        val timestamp: Long = System.currentTimeMillis(),
        val path: String? = null
    )

    fun handleNotFound(exchange: HttpExchange, path: String) {
        val errorResponse = ErrorResponse(
            error = "Ruta no encontrada: $path",
            code = "ROUTE_NOT_FOUND",
            path = path
        )

        logger.warn("Ruta no encontrada: $path")
        sendErrorResponse(exchange, 404, errorResponse)
    }

    fun handleMethodNotAllowed(exchange: HttpExchange, method: String, path: String) {
        val errorResponse = ErrorResponse(
            error = "Método no permitido: $method",
            code = "METHOD_NOT_ALLOWED",
            path = path
        )

        logger.warn("Método no permitido: $method en $path")
        sendErrorResponse(exchange, 405, errorResponse)
    }

    fun handleInternalError(exchange: HttpExchange, exception: Exception, path: String) {
        val errorResponse = ErrorResponse(
            error = "Error interno del gateway: ${exception.message}",
            code = "INTERNAL_ERROR",
            path = path
        )

        logger.error("Error interno en $path: ${exception.message}", exception)
        sendErrorResponse(exchange, 500, errorResponse)
    }

    fun handleTimeout(exchange: HttpExchange, serviceName: String, path: String) {
        val errorResponse = ErrorResponse(
            error = "Timeout comunicándose con $serviceName",
            code = "SERVICE_TIMEOUT",
            path = path
        )

        logger.error("Timeout con $serviceName en $path")
        sendErrorResponse(exchange, 504, errorResponse)
    }

    fun handleServiceUnavailable(exchange: HttpExchange, serviceName: String, path: String) {
        val errorResponse = ErrorResponse(
            error = "Servicio $serviceName no disponible",
            code = "SERVICE_UNAVAILABLE",
            path = path
        )

        logger.error("Servicio $serviceName no disponible en $path")
        sendErrorResponse(exchange, 503, errorResponse)
    }

    private fun sendErrorResponse(exchange: HttpExchange, statusCode: Int, errorResponse: ErrorResponse) {
        try {
            val jsonResponse = Json.encodeToString(errorResponse)
            val responseBytes = jsonResponse.toByteArray()

            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(statusCode, responseBytes.size.toLong())
            exchange.responseBody.write(responseBytes)
            exchange.responseBody.close()
        } catch (e: Exception) {
            logger.error("Error enviando respuesta de error: ${e.message}")
        }
    }
}
