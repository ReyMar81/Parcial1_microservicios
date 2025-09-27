package com.tiendavirtual.gateway.config

data class GatewayConfig(
    val port: Int = System.getenv("PORT")?.toInt() ?: 8080,
    val connectTimeoutSeconds: Long = 10,
    val requestTimeoutSeconds: Long = 30,
    val retryAttempts: Int = 3,
    val retryDelayMs: Long = 1000
)

data class ServiceEndpoint(
    val name: String,
    val url: String,
    val healthPath: String = "/health"
)

class ServiceRegistry {
    private val services = mapOf(
        "productos" to ServiceEndpoint(
            name = "Productos",
            url = System.getenv("PRODUCTOS_MS_URL") ?: "http://localhost:8081"
        ),
        "clientes" to ServiceEndpoint(
            name = "Clientes",
            url = System.getenv("CLIENTES_MS_URL") ?: "http://localhost:8082"
        ),
        "ventas" to ServiceEndpoint(
            name = "Ventas",
            url = System.getenv("VENTAS_MS_URL") ?: "http://localhost:8083"
        )
    )

    fun getService(name: String): ServiceEndpoint? = services[name]

    fun getAllServices(): Map<String, ServiceEndpoint> = services
}
