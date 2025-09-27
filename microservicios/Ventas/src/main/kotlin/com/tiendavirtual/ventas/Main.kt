package com.tiendavirtual.ventas

import com.sun.net.httpserver.HttpServer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.tiendavirtual.ventas.api.VentasApi
import com.tiendavirtual.ventas.negocio.VentaNegocio
import com.tiendavirtual.ventas.data.VentasData
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

private val logger = LoggerFactory.getLogger("VentasMS")

/**
 * Main del microservicio Ventas
 * Responsabilidad: Configurar e inicializar las 3 capas
 */
fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8083
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5435/ventas"
    val dbUser = System.getenv("DB_USER") ?: "app"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "app"

    logger.info("🚀 Iniciando Microservicio Ventas en puerto $port")

    // Configurar DataSource
    val config = HikariConfig()
    config.jdbcUrl = dbUrl
    config.username = dbUser
    config.password = dbPassword
    config.maximumPoolSize = 10
    val dataSource = HikariDataSource(config)

    // Ejecutar migraciones
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .load()
    flyway.migrate()

    // INICIALIZAR LAS 3 CAPAS SEGÚN TU DIAGRAMA
    logger.info("📊 Inicializando capa de Datos...")
    val ventasData = VentasData(dataSource)

    logger.info("🧠 Inicializando capa de Negocio...")
    val ventaNegocio = VentaNegocio(ventasData)

    logger.info("📡 Inicializando capa API...")
    val ventasApi = VentasApi(ventaNegocio)

    // Crear servidor HTTP
    val server = HttpServer.create(InetSocketAddress(port), 0)

    // Health check
    server.createContext("/health") { exchange ->
        exchange.responseHeaders.set("Content-Type", "application/json")
        val response = """{"status":"ok","service":"ventas-ms","architecture":"3-layers"}"""
        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
        exchange.responseBody.write(response.toByteArray())
        exchange.responseBody.close()
    }

    // Delegar todas las peticiones a la capa API
    server.createContext("/ventas") { exchange ->
        // Llamar al método correcto definido en VentasApi
        ventasApi.manejarRequest(exchange)
    }

    server.executor = null
    server.start()
    logger.info("✅ Microservicio Ventas iniciado en http://localhost:$port")
    logger.info("📐 Arquitectura: API → Negocio → Datos → BD")
    logger.info("🔄 Flujo transaccional: Productos ↔ Clientes ↔ Ventas")
}
