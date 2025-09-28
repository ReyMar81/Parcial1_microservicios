package com.tiendavirtual.gateway.routing

import com.tiendavirtual.gateway.config.ServiceRegistry
import org.slf4j.LoggerFactory

class Router(private val serviceRegistry: ServiceRegistry) {
    private val logger = LoggerFactory.getLogger(Router::class.java)

    data class RouteResult(val targetUrl: String, val serviceName: String)

    data class PublicRouteInfo(
        val apiPattern: String,
        val forwardsToService: String,
        val serviceRelativeBase: String,
        val description: String
    )

    fun listSupportedRoutes(): List<PublicRouteInfo> = listOf(
        PublicRouteInfo(
            apiPattern = "/api/productos/productos[/{id}]",
            forwardsToService = "productos",
            serviceRelativeBase = "/productos",
            description = "CRUD de productos (canónico)"
        ),
        PublicRouteInfo(
            apiPattern = "/api/productos/categorias[/{id}]",
            forwardsToService = "productos",
            serviceRelativeBase = "/categorias",
            description = "CRUD de categorías (canónico bajo MS productos)"
        ),
        PublicRouteInfo(
            apiPattern = "/api/productos/catalogos[/{id}]",
            forwardsToService = "productos",
            serviceRelativeBase = "/catalogos",
            description = "CRUD de catálogos (canónico bajo MS productos)"
        ),
        PublicRouteInfo(
            apiPattern = "/api/clientes[/{id}]",
            forwardsToService = "clientes",
            serviceRelativeBase = "/clientes",
            description = "CRUD de clientes"
        ),
        PublicRouteInfo(
            apiPattern = "/api/ventas[/{id}]",
            forwardsToService = "ventas",
            serviceRelativeBase = "/ventas",
            description = "CRUD de ventas (cuando esté habilitado)"
        )
    )

    fun resolveRoute(path: String): RouteResult? {
        logger.debug("Resolviendo ruta: $path")

        return when {
            path.startsWith("/api/productos") -> {
                val rest = path.removePrefix("/api/productos")
                val service = serviceRegistry.getService("productos") ?: return null
                val finalPath = if (rest.isEmpty()) "/productos" else rest
                RouteResult("${service.url}$finalPath", service.name)
            }
            path.startsWith("/api/clientes") -> {
                val rest = path.removePrefix("/api/clientes")
                val service = serviceRegistry.getService("clientes") ?: return null
                val finalPath = if (rest.isEmpty()) "/clientes" else "/clientes$rest"
                RouteResult("${service.url}$finalPath", service.name)
            }
            path.startsWith("/api/ventas") -> {
                val rest = path.removePrefix("/api/ventas")
                val service = serviceRegistry.getService("ventas") ?: return null
                val finalPath = if (rest.isEmpty()) "/ventas" else "/ventas$rest"
                RouteResult("${service.url}$finalPath", service.name)
            }
            else -> null
        }
    }

    fun isValidRoute(path: String): Boolean = resolveRoute(path) != null || path == "/health"
}
