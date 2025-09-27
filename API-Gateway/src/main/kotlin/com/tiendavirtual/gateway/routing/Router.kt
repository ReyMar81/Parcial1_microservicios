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
            path.startsWith("/api/productos") -> routeProductos(path)
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

    private fun routeProductos(fullPath: String): RouteResult? {
        // Formatos aceptados:
        // /api/productos/productos
        // /api/productos/productos/{id}
        // /api/productos/categorias
        // /api/productos/categorias/{id}
        // /api/productos/catalogos
        // /api/productos/catalogos/{id}
        val service = serviceRegistry.getService("productos") ?: return null
        val withoutPrefix = fullPath.removePrefix("/api/productos") // e.g. "", "/productos", "/categorias/3"
        val segments = withoutPrefix.split('/').filter { it.isNotBlank() } // lista limpia

        if (segments.isEmpty()) {
            // Si no se especifica recurso, devolvemos listado de productos por convención
            return RouteResult("${service.url}/productos", service.name)
        }
        val recurso = segments[0]
        if (recurso !in setOf("productos", "categorias", "catalogos")) {
            logger.warn("Recurso desconocido bajo /api/productos: $recurso")
            return null
        }
        val tail = if (segments.size > 1) "/" + segments.drop(1).joinToString("/") else ""
        val target = "${service.url}/$recurso$tail"
        return RouteResult(target, service.name)
    }

    fun isValidRoute(path: String): Boolean = resolveRoute(path) != null || path == "/health"
}
