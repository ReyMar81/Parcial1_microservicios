package com.tiendavirtual.gateway

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import com.tiendavirtual.gateway.config.GatewayConfig
import com.tiendavirtual.gateway.config.ServiceRegistry
import com.tiendavirtual.gateway.routing.Router
import com.tiendavirtual.gateway.proxy.HttpProxy
import com.tiendavirtual.gateway.health.HealthChecker
import com.tiendavirtual.gateway.exception.GatewayExceptionHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

private val logger = LoggerFactory.getLogger("APIGateway")

class GatewayService(
    private val config: GatewayConfig,
    private val serviceRegistry: ServiceRegistry,
    private val router: Router,
    private val httpProxy: HttpProxy,
    private val healthChecker: HealthChecker,
    private val exceptionHandler: GatewayExceptionHandler
) {

    fun handleRequest(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val method = exchange.requestMethod

        logger.info("Gateway: $method $path")

        try {
            when (path) {
                "/health" -> handleHealthCheck(exchange)
                else -> routeToMicroservice(exchange, path, method)
            }
        } catch (e: Exception) {
            exceptionHandler.handleInternalError(exchange, e, path)
        }
    }

    private fun handleHealthCheck(exchange: HttpExchange) {
        val healthResponse = healthChecker.checkAllServices()
        val jsonResponse = healthChecker.toJsonString(healthResponse)

        val statusCode = if (healthResponse.status == "UP") 200 else 503
        sendResponse(exchange, statusCode, jsonResponse)
    }

    private fun routeToMicroservice(exchange: HttpExchange, path: String, method: String) {
        // Validar que la ruta existe
        if (!router.isValidRoute(path)) {
            exceptionHandler.handleNotFound(exchange, path)
            return
        }

        // Resolver ruta al microservicio
        val routeResult = router.resolveRoute(path)
        if (routeResult == null) {
            exceptionHandler.handleNotFound(exchange, path)
            return
        }

        // Validar metodo HTTP permitido
        if (!isValidHttpMethod(method)) {
            exceptionHandler.handleMethodNotAllowed(exchange, method, path)
            return
        }

        logger.info("Enrutando a ${routeResult.serviceName}: $method ${routeResult.targetUrl}")

        // Reenviar petición al microservicio
        val proxyResponse = httpProxy.forwardRequest(exchange, routeResult.targetUrl, routeResult.serviceName)

        // Copiar headers importantes de la respuesta
        proxyResponse.headers.forEach { (key, values) ->
            if (key.lowercase() in listOf("content-type", "content-disposition")) {
                values.forEach { value ->
                    exchange.responseHeaders.add(key, value)
                }
            }
        }

        sendResponse(exchange, proxyResponse.statusCode, proxyResponse.body)
    }

    private fun isValidHttpMethod(method: String): Boolean {
        return method.uppercase() in listOf("GET", "POST", "PUT", "PATCH", "DELETE")
    }

    private fun sendResponse(exchange: HttpExchange, statusCode: Int, body: String) {
        val responseBytes = body.toByteArray()

        if (!exchange.responseHeaders.containsKey("Content-Type")) {
            exchange.responseHeaders.set("Content-Type", "application/json")
        }

        exchange.sendResponseHeaders(statusCode, responseBytes.size.toLong())
        exchange.responseBody.write(responseBytes)
        exchange.responseBody.close()
    }
}

fun main() {
    logger.info("Iniciando API Gateway...")

    // Inicializar componentes
    val config = GatewayConfig()
    val serviceRegistry = ServiceRegistry()
    val router = Router(serviceRegistry)
    val httpProxy = HttpProxy(config)
    val healthChecker = HealthChecker(serviceRegistry)
    val exceptionHandler = GatewayExceptionHandler()

    val gatewayService = GatewayService(
        config, serviceRegistry, router, httpProxy, healthChecker, exceptionHandler
    )

    // Configurar servidor HTTP
    val server = HttpServer.create(InetSocketAddress(config.port), 0)

    server.createContext("/") { exchange ->
        gatewayService.handleRequest(exchange)
    }

    server.executor = null
    server.start()

    logger.info("API Gateway iniciado exitosamente en puerto ${config.port}")
    logger.info("Funcionalidades mejoradas:")
    logger.info("Health checks detallados - GET /health")
    logger.info("Manejo de errores robusto")
    logger.info("Reintentos automáticos")
    logger.info("Timeouts configurables")
    logger.info("Arquitectura de capas")
    logger.info("Rutas disponibles:")
    serviceRegistry.getAllServices().forEach { (key, service) ->
        logger.info("   *    /api/$key/** → ${service.name} (${service.url})")
    }
    logger.info("Gateway listo para recibir peticiones...")
}
