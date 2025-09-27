package com.tiendavirtual.productos.api

import com.sun.net.httpserver.HttpExchange
import com.tiendavirtual.productos.negocio.CategoriaNegocio
import com.tiendavirtual.productos.negocio.ProductoNegocio
import com.tiendavirtual.productos.negocio.CatalogoNegocio
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

// ================== DTOs Categorías ==================
@Serializable
data class CrearCategoriaRequest(val nombre: String)
@Serializable
data class ActualizarCategoriaRequest(val nombre: String? = null)
@Serializable
data class CategoriaResponse(val id: Int, val nombre: String)

// ================== DTOs Productos ==================
@Serializable
data class CrearProductoRequest(
    val nombre: String,
    val descripcion: String,
    val imagen: String,
    val precio: Double,
    val stock: Int,
    val categoriaId: Int
)
@Serializable
data class ActualizarProductoRequest(
    val nombre: String? = null,
    val descripcion: String? = null,
    val imagen: String? = null,
    val precio: Double? = null,
    val stock: Int? = null,
    val categoriaId: Int? = null
)
@Serializable
data class ProductoResponse(
    val codigo: Int,
    val nombre: String,
    val descripcion: String,
    val imagen: String,
    val precio: Double,
    val stock: Int,
    val categoriaId: Int
)

// ================== DTOs Catálogos ==================
@Serializable
data class CrearCatalogoRequest(val nombre: String, val descripcion: String)
@Serializable
data class ActualizarCatalogoRequest(val nombre: String? = null, val descripcion: String? = null)
@Serializable
data class CatalogoResponse(val id: Int, val nombre: String, val descripcion: String)

// ================== Errores ==================
@Serializable
data class ErrorResponse(val error: String, val details: String? = null)

class ProductosApi(
    private val productoNegocio: ProductoNegocio,
    private val categoriaNegocio: CategoriaNegocio,
    private val catalogoNegocio: CatalogoNegocio
) {
    fun manejarRequest(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod
            when (method) {
                // ======= GET =======
                "GET" -> when {
                    path == "/categorias" -> listarCategorias(exchange)
                    path.matches(Regex("/categorias/\\d+$")) -> obtenerCategoria(exchange)
                    path == "/productos" -> listarProductos(exchange)
                    path.matches(Regex("/productos/\\d+$")) -> obtenerProducto(exchange)
                    path == "/catalogos" -> listarCatalogos(exchange)
                    path.matches(Regex("/catalogos/\\d+$")) -> obtenerCatalogo(exchange)
                    else -> enviarError(exchange, 404, "Endpoint no encontrado")
                }
                // ======= POST (crear) =======
                "POST" -> when (path) {
                    "/categorias" -> crearCategoria(exchange)
                    "/productos" -> crearProducto(exchange)
                    "/catalogos" -> crearCatalogo(exchange)
                    else -> enviarError(exchange, 404, "Endpoint no encontrado")
                }
                // ======= PUT (full update, se trata igual que PATCH aquí) =======
                "PUT" -> when {
                    path.matches(Regex("/categorias/\\d+$")) -> actualizarCategoria(exchange)
                    path.matches(Regex("/productos/\\d+$")) -> actualizarProducto(exchange)
                    path.matches(Regex("/catalogos/\\d+$")) -> actualizarCatalogo(exchange)
                    else -> enviarError(exchange, 404, "Endpoint no encontrado")
                }
                // ======= PATCH (partial update) =======
                "PATCH" -> when {
                    path.matches(Regex("/categorias/\\d+$")) -> actualizarCategoria(exchange)
                    path.matches(Regex("/productos/\\d+$")) -> actualizarProducto(exchange)
                    path.matches(Regex("/catalogos/\\d+$")) -> actualizarCatalogo(exchange)
                    else -> enviarError(exchange, 404, "Endpoint no encontrado")
                }
                // ======= DELETE =======
                "DELETE" -> when {
                    path.matches(Regex("/categorias/\\d+$")) -> eliminarCategoria(exchange)
                    path.matches(Regex("/productos/\\d+$")) -> eliminarProducto(exchange)
                    path.matches(Regex("/catalogos/\\d+$")) -> eliminarCatalogo(exchange)
                    else -> enviarError(exchange, 404, "Endpoint no encontrado")
                }
                else -> {
                    exchange.responseHeaders.set("Allow", "GET,POST,PUT,PATCH,DELETE")
                    enviarError(exchange, 405, "Método no permitido")
                }
            }
        } catch (_: SerializationException) {
            enviarError(exchange, 400, "JSON inválido")
        } catch (_: NumberFormatException) {
            enviarError(exchange, 400, "Id inválido")
        } catch (e: IllegalArgumentException) {
            enviarError(exchange, 400, e.message ?: "Datos inválidos")
        } catch (e: IllegalStateException) {
            enviarError(exchange, 409, e.message ?: "Conflicto de estado")
        } catch (e: Exception) {
            enviarError(exchange, 500, "Error interno del servidor")
        } finally {
            runCatching { exchange.responseBody.close() }
        }
    }

    // ================== Categorías ==================
    private fun listarCategorias(exchange: HttpExchange) {
        val lista = categoriaNegocio.listarCategorias().map { it.toResponse() }
        enviarRespuesta(exchange, 200, json.encodeToString(lista))
    }
    private fun obtenerCategoria(exchange: HttpExchange) {
        val id = pathId(exchange)
        val categoria = categoriaNegocio.listarCategorias().firstOrNull { it.id == id }
        if (categoria != null) enviarRespuesta(exchange, 200, json.encodeToString(categoria.toResponse()))
        else enviarError(exchange, 404, "Categoría no encontrada")
    }
    private fun crearCategoria(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<CrearCategoriaRequest>(body)
        val creada = categoriaNegocio.crearCategoria(req.nombre)
        exchange.responseHeaders.add("Location", "/categorias/${creada.id}")
        enviarRespuesta(exchange, 201, json.encodeToString(creada.toResponse()))
    }
    private fun actualizarCategoria(exchange: HttpExchange) {
        val id = pathId(exchange)
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<ActualizarCategoriaRequest>(body)
        val actualizada = req.nombre?.let { categoriaNegocio.actualizarCategoria(id, it) }
        if (actualizada != null) enviarRespuesta(exchange, 200, json.encodeToString(actualizada.toResponse()))
        else enviarError(exchange, 404, "Categoría no encontrada")
    }
    private fun eliminarCategoria(exchange: HttpExchange) {
        val id = pathId(exchange)
        if (categoriaNegocio.eliminarCategoria(id)) exchange.sendResponseHeaders(204, -1)
        else enviarError(exchange, 404, "Categoría no encontrada")
    }

    // ================== Productos ==================
    private fun listarProductos(exchange: HttpExchange) {
        val lista = productoNegocio.listarProductos().map { it.toResponse() }
        enviarRespuesta(exchange, 200, json.encodeToString(lista))
    }
    private fun obtenerProducto(exchange: HttpExchange) {
        val codigo = pathId(exchange)
        val producto = productoNegocio.obtenerProducto(codigo)
        if (producto != null) enviarRespuesta(exchange, 200, json.encodeToString(producto.toResponse()))
        else enviarError(exchange, 404, "Producto no encontrado")
    }
    private fun crearProducto(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<CrearProductoRequest>(body)
        val creado = productoNegocio.crearProducto(
            req.nombre, req.descripcion, req.imagen, req.precio, req.stock, req.categoriaId
        )
        exchange.responseHeaders.add("Location", "/productos/${creado.codigo}")
        enviarRespuesta(exchange, 201, json.encodeToString(creado.toResponse()))
    }
    private fun actualizarProducto(exchange: HttpExchange) {
        val codigo = pathId(exchange)
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<ActualizarProductoRequest>(body)
        val actualizado = productoNegocio.actualizarProducto(
            codigo, req.nombre, req.descripcion, req.imagen, req.precio, req.stock, req.categoriaId
        )
        if (actualizado != null) enviarRespuesta(exchange, 200, json.encodeToString(actualizado.toResponse()))
        else enviarError(exchange, 404, "Producto no encontrado")
    }
    private fun eliminarProducto(exchange: HttpExchange) {
        val codigo = pathId(exchange)
        if (productoNegocio.eliminarProducto(codigo)) exchange.sendResponseHeaders(204, -1)
        else enviarError(exchange, 404, "Producto no encontrado")
    }

    // ================== Catálogos ==================
    private fun listarCatalogos(exchange: HttpExchange) {
        val lista = catalogoNegocio.listarCatalogos().map { it.toResponse() }
        enviarRespuesta(exchange, 200, json.encodeToString(lista))
    }
    private fun obtenerCatalogo(exchange: HttpExchange) {
        val id = pathId(exchange)
        val catalogo = catalogoNegocio.listarCatalogos().firstOrNull { it.id == id }
        if (catalogo != null) enviarRespuesta(exchange, 200, json.encodeToString(catalogo.toResponse()))
        else enviarError(exchange, 404, "Catálogo no encontrado")
    }
    private fun crearCatalogo(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<CrearCatalogoRequest>(body)
        val creado = catalogoNegocio.crearCatalogo(req.nombre, req.descripcion)
        exchange.responseHeaders.add("Location", "/catalogos/${creado.id}")
        enviarRespuesta(exchange, 201, json.encodeToString(creado.toResponse()))
    }
    private fun actualizarCatalogo(exchange: HttpExchange) {
        val id = pathId(exchange)
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<ActualizarCatalogoRequest>(body)
        val actualizado = catalogoNegocio.actualizarCatalogo(id, req.nombre ?: return enviarError(exchange, 400, "nombre requerido"), req.descripcion ?: "")
        if (actualizado != null) enviarRespuesta(exchange, 200, json.encodeToString(actualizado.toResponse()))
        else enviarError(exchange, 404, "Catálogo no encontrado")
    }
    private fun eliminarCatalogo(exchange: HttpExchange) {
        val id = pathId(exchange)
        if (catalogoNegocio.eliminarCatalogo(id)) exchange.sendResponseHeaders(204, -1)
        else enviarError(exchange, 404, "Catálogo no encontrado")
    }

    // ================== Util ==================
    private fun pathId(exchange: HttpExchange): Int = exchange.requestURI.path.substringAfterLast('/').toInt()

    private fun enviarRespuesta(exchange: HttpExchange, codigo: Int, jsonBody: String) {
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = jsonBody.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(codigo, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun enviarError(exchange: HttpExchange, codigo: Int, mensaje: String) {
        val body = json.encodeToString(ErrorResponse(mensaje))
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(codigo, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
}

// ================== Mapeos ==================
private fun com.tiendavirtual.productos.negocio.Categoria.toResponse() = CategoriaResponse(id, nombre)
private fun com.tiendavirtual.productos.negocio.Producto.toResponse() = ProductoResponse(codigo, nombre, descripcion, imagen, precio, stock, categoriaId)
private fun com.tiendavirtual.productos.negocio.Catalogo.toResponse() = CatalogoResponse(id, nombre, descripcion)
