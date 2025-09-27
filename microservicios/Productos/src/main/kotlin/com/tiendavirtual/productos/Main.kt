package com.tiendavirtual.productos

import com.sun.net.httpserver.HttpServer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.tiendavirtual.productos.api.ProductosApi
import com.tiendavirtual.productos.negocio.CategoriaNegocio
import com.tiendavirtual.productos.negocio.ProductoNegocio
import com.tiendavirtual.productos.negocio.CatalogoNegocio
import com.tiendavirtual.productos.data.CategoriasData
import com.tiendavirtual.productos.data.ProductosData
import com.tiendavirtual.productos.data.CatalogosData
import java.net.InetSocketAddress

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5434/productos"
    val dbUser = System.getenv("DB_USER") ?: "app"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "app"

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
            connection.prepareStatement("""
                CREATE TABLE IF NOT EXISTS categorias (
                  id SERIAL PRIMARY KEY,
                  nombre VARCHAR(100) NOT NULL
                );
                CREATE TABLE IF NOT EXISTS catalogos (
                  id SERIAL PRIMARY KEY,
                  nombre VARCHAR(100) NOT NULL,
                  descripcion VARCHAR(255)
                );
                CREATE TABLE IF NOT EXISTS productos (
                  codigo SERIAL PRIMARY KEY,
                  nombre VARCHAR(120) NOT NULL,
                  descripcion VARCHAR(255) NOT NULL,
                  imagen TEXT NOT NULL,
                  precio NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
                  stock INTEGER NOT NULL CHECK (stock >= 0),
                  categoria_id INTEGER NOT NULL REFERENCES categorias(id)
                );
                CREATE TABLE IF NOT EXISTS catalogo_producto (
                  producto_id INTEGER NOT NULL REFERENCES productos(codigo) ON DELETE CASCADE,
                  catalogo_id INTEGER NOT NULL REFERENCES catalogos(id) ON DELETE CASCADE,
                  PRIMARY KEY (producto_id, catalogo_id)
                );
            """).use { statement ->
                statement.execute()
                println("[DB-DEBUG] Tablas de productos creadas exitosamente")
            }
        }
    } catch (e: Exception) {
        println("[DB-ERROR] Error creando tablas: ${e.message}")
    }

    // Inicializar las 3 capas
    val categoriasData = CategoriasData(dataSource)
    val productosData = ProductosData(dataSource)
    val catalogosData = CatalogosData(dataSource)

    val categoriaNegocio = CategoriaNegocio(categoriasData)
    val productoNegocio = ProductoNegocio(productosData)
    val catalogoNegocio = CatalogoNegocio(catalogosData)

    val productosApi = ProductosApi(productoNegocio, categoriaNegocio, catalogoNegocio)

    // Crear servidor HTTP
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/") { exchange ->
        productosApi.manejarRequest(exchange)
    }

    server.executor = null
    server.start()

    println("🚀 Microservicio Productos iniciado en puerto $port")
    println("📊 Conectado a base de datos: $dbUrl")
}
