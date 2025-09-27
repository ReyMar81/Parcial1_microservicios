package com.tiendavirtual.clientes

import com.sun.net.httpserver.HttpServer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.tiendavirtual.clientes.api.ClientesApi
import com.tiendavirtual.clientes.negocio.ClienteNegocio
import com.tiendavirtual.clientes.data.ClientesData
import java.net.InetSocketAddress

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8082
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5434/Clientes"
    val dbUser = System.getenv("DB_USER") ?: "app"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "app"

    // Configurar DataSource
    val config = HikariConfig()
    config.jdbcUrl = dbUrl
    config.username = dbUser
    config.password = dbPassword
    config.maximumPoolSize = 10
    val dataSource = HikariDataSource(config)

    // Crear tabla clientes si no existe
    try {
        dataSource.connection.use { connection ->
            val createTableSql = """
                CREATE TABLE IF NOT EXISTS clientes (
                  id SERIAL PRIMARY KEY,
                  nombres VARCHAR(120) NOT NULL,
                  docIdentidad VARCHAR(30) NOT NULL,
                  whatsapp VARCHAR(30) NOT NULL,
                  direccion VARCHAR(200) NOT NULL
                )
            """.trimIndent()

            connection.prepareStatement(createTableSql).use { statement ->
                statement.execute()
                println("[DB-DEBUG] Tabla 'clientes' creada exitosamente")
            }
        }
    } catch (e: Exception) {
        println("[DB-ERROR] Error creando tabla: ${e.message}")
    }

    // INICIALIZAR LAS 3 CAPAS
    val clientesData = ClientesData(dataSource)
    val clienteNegocio = ClienteNegocio(clientesData)
    val clientesApi = ClientesApi(clienteNegocio)

    // Crear servidor HTTP
    val server = HttpServer.create(InetSocketAddress(port), 0)

    server.createContext("/health") { exchange ->
        exchange.responseHeaders.set("Content-Type", "application/json")
        val response = """{"status":"ok","service":"clientes-ms"}"""
        exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
        exchange.responseBody.write(response.toByteArray())
        exchange.responseBody.close()
    }

    // Solo delegar a la capa API
    server.createContext("/clientes") { exchange ->
        clientesApi.manejarRequest(exchange)
    }

    server.executor = null
    server.start()
}
