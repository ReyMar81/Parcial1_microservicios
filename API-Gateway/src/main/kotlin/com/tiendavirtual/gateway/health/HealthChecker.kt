package com.tiendavirtual.gateway.health

import com.tiendavirtual.gateway.config.ServiceRegistry
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class HealthChecker(private val serviceRegistry: ServiceRegistry) {
    private val logger = LoggerFactory.getLogger(HealthChecker::class.java)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    @Serializable
    data class ServiceHealth(
        val name: String,
        val status: String,
        val url: String,
        val responseTime: Long? = null,
        val error: String? = null
    )

    @Serializable
    data class GatewayHealthResponse(
        val status: String,
        val service: String = "api-gateway",
        val timestamp: Long = System.currentTimeMillis(),
        val services: List<ServiceHealth>
    )

    fun checkAllServices(): GatewayHealthResponse {
        val serviceHealths = serviceRegistry.getAllServices().map { (_, service) ->
            checkServiceHealth(service)
        }

        val overallStatus = if (serviceHealths.all { it.status == "UP" }) "UP" else "DEGRADED"

        return GatewayHealthResponse(
            status = overallStatus,
            services = serviceHealths
        )
    }

    private fun checkServiceHealth(service: com.tiendavirtual.gateway.config.ServiceEndpoint): ServiceHealth {
        val startTime = System.currentTimeMillis()

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("${service.url}${service.healthPath}"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val responseTime = System.currentTimeMillis() - startTime

            if (response.statusCode() == 200) {
                ServiceHealth(
                    name = service.name,
                    status = "UP",
                    url = service.url,
                    responseTime = responseTime
                )
            } else {
                ServiceHealth(
                    name = service.name,
                    status = "DOWN",
                    url = service.url,
                    responseTime = responseTime,
                    error = "HTTP ${response.statusCode()}"
                )
            }

        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            logger.warn("Health check falló para ${service.name}: ${e.message}")

            ServiceHealth(
                name = service.name,
                status = "DOWN",
                url = service.url,
                responseTime = responseTime,
                error = e.message
            )
        }
    }

    fun toJsonString(healthResponse: GatewayHealthResponse): String {
        return Json.encodeToString(healthResponse)
    }
}
