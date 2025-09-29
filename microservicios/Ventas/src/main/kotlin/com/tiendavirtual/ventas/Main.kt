package com.tiendavirtual.ventas

import com.sun.net.httpserver.HttpServer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.tiendavirtual.ventas.api.VentasApi
import com.tiendavirtual.ventas.negocio.VentaNegocio
import com.tiendavirtual.ventas.data.VentasData
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

    // Crear tablas si no existen
    try {
        dataSource.connection.use { connection ->
            val createTableSql = """
                CREATE TABLE IF NOT EXISTS ventas (
                  id SERIAL PRIMARY KEY,
                  fecha TIMESTAMP NOT NULL,
                  estado VARCHAR(30) NOT NULL,
                  total NUMERIC(12,2) NOT NULL,
                  cliente_id INTEGER NOT NULL
                );
                CREATE TABLE IF NOT EXISTS detalle_venta (
                  id SERIAL PRIMARY KEY,
                  cantidad INTEGER NOT NULL,
                  precio_unitario NUMERIC(12,2) NOT NULL,
                  producto_id INTEGER NOT NULL,
                  venta_id INTEGER NOT NULL REFERENCES ventas(id) ON DELETE CASCADE
                );
            """.trimIndent()
            connection.prepareStatement(createTableSql).use { statement ->
                statement.execute()
                logger.info("[DB-DEBUG] Tablas de ventas creadas exitosamente")
            }
        }
    } catch (e: Exception) {
        logger.error("[DB-ERROR] Error creando tablas: ", e)
    }

    // INICIALIZAR LAS 3 CAPAS SEGÚN TU DIAGRAMA
    logger.info("📊 Inicializando capa de Datos...")
    val ventasData = VentasData(dataSource)

    logger.info("🧠 Inicializando capa de Negocio...")
    val ventaNegocio = VentaNegocio(ventasData)

    logger.info("📡 Inicializando capa API...")
    val ventasApi = VentasApi(ventaNegocio)

    // Crear servidor HTTP nativo (como Productos)
    val server = HttpServer.create(InetSocketAddress(port), 0)

    // Configurar endpoints
    server.createContext("/ventas") { exchange ->
        // Agregar headers CORS
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type, Authorization")

        if (exchange.requestMethod == "OPTIONS") {
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.close()
        } else {
            ventasApi.manejarRequest(exchange)
        }
    }

    server.createContext("/health") { exchange ->
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.add("Content-Type", "application/json")
        val response = """{"status":"UP","service":"ventas"}"""
        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
        exchange.responseBody.write(response.toByteArray())
        exchange.responseBody.close()
    }

    server.executor = null
    server.start()

    logger.info("✅ Microservicio Ventas iniciado en http://localhost:$port")
    logger.info("📐 Arquitectura: API → Negocio → Datos → BD")
    logger.info("🔄 Endpoints:")
    logger.info("   POST /ventas/{id}/confirmar - Solo cambia estado a CONFIRMADA")
    logger.info("   POST /ventas/{id}/anular - Solo cambia estado a ANULADA")
}
