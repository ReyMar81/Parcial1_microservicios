package com.tiendavirtual.productos.api

import com.sun.net.httpserver.HttpExchange
import com.tiendavirtual.productos.negocio.CategoriaNegocio
import com.tiendavirtual.productos.negocio.ProductoNegocio
import com.tiendavirtual.productos.negocio.CatalogoNegocio
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

private val json = Json { ignoreUnknownKeys = true }

// DTOs Categorías
@Serializable data class CrearCategoriaRequest(val nombre: String)
@Serializable data class ActualizarCategoriaRequest(val nombre: String? = null)
@Serializable data class CategoriaResponse(val id: Int, val nombre: String)

// DTOs Productos
@Serializable data class CrearProductoRequest(val nombre: String, val descripcion: String, val imagen: String, val precio: Double, val stock: Int, val categoriaId: Int)
@Serializable data class ActualizarProductoRequest(val nombre: String? = null, val descripcion: String? = null, val imagen: String? = null, val precio: Double? = null, val stock: Int? = null, val categoriaId: Int? = null)
@Serializable data class ProductoResponse(val codigo: Int, val nombre: String, val descripcion: String, val imagen: String, val precio: Double, val stock: Int, val categoriaId: Int)

// DTOs Catálogos
@Serializable data class CrearCatalogoRequest(val nombre: String, val descripcion: String)
@Serializable data class ActualizarCatalogoRequest(val nombre: String? = null, val descripcion: String? = null)
@Serializable data class CatalogoResponse(val id: Int, val nombre: String, val descripcion: String)

// Asociación Catálogo-Productos
@Serializable data class AsociarProductosRequest(val productoIds: List<Int>)
@Serializable data class AsociarProductosResponse(val catalogoId: Int, val productoIds: List<Int>, val agregados: Int? = null, val total: Int? = null)
@Serializable data class ErrorResponse(val error: String)

class ProductosApi(
    private val productoNegocio: ProductoNegocio,
    private val categoriaNegocio: CategoriaNegocio,
    private val catalogoNegocio: CatalogoNegocio
) {
    private val catalogoProductosRegex = Regex("/catalogos/\\d+/productos$")
    private val catalogoProductoItemRegex = Regex("/catalogos/\\d+/productos/\\d+$")

    fun manejarRequest(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod
            println("[DEBUG] $method $path") // Agregar logging
            val handled = when (method) {
                "GET" -> handleGet(path, exchange)
                "POST" -> handlePost(path, exchange)
                "PUT" -> handlePut(path, exchange)
                "PATCH" -> handlePatch(path, exchange)
                "DELETE" -> handleDelete(path, exchange)
                else -> { enviarError(exchange, 405, "Método no permitido"); true }
            }
            if (!handled) enviarError(exchange, 404, "No encontrado")
        } catch (e: SerializationException) {
            println("[ERROR] SerializationException: ${e.message}")
            enviarError(exchange, 400, "JSON inválido")
        } catch (e: NumberFormatException) {
            println("[ERROR] NumberFormatException: ${e.message}")
            enviarError(exchange, 400, "Parámetro inválido")
        } catch (e: IllegalArgumentException) {
            println("[ERROR] IllegalArgumentException: ${e.message}")
            enviarError(exchange, 400, e.message ?: "Datos inválidos")
        } catch (e: Exception) {
            println("[ERROR] Exception: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            enviarError(exchange, 500, "Error interno: ${e.message}")
        } finally {
            runCatching { exchange.responseBody.close() }
        }
    }

    private fun handleGet(path: String, ex: HttpExchange): Boolean = when {
        catalogoProductosRegex.matches(path) -> { listarProductosDeCatalogo(ex); true }
        path == "/categorias" -> { listarCategorias(ex); true }
        path.matches(Regex("/categorias/\\d+$")) -> { obtenerCategoria(ex); true }
        path == "/productos" -> { listarProductos(ex); true }
        path.matches(Regex("/productos/\\d+$")) -> { obtenerProducto(ex); true }
        path == "/catalogos" -> { listarCatalogos(ex); true }
        path.matches(Regex("/catalogos/\\d+$")) -> { obtenerCatalogo(ex); true }
        else -> false
    }
    private fun handlePost(path: String, ex: HttpExchange): Boolean = when {
        catalogoProductosRegex.matches(path) -> { agregarProductosACatalogo(ex); true }
        path == "/categorias" -> { crearCategoria(ex); true }
        path == "/productos" -> { crearProducto(ex); true }
        path == "/catalogos" -> { crearCatalogo(ex); true }
        else -> false
    }
    private fun handlePut(path: String, ex: HttpExchange): Boolean = when {
        catalogoProductosRegex.matches(path) -> { reemplazarProductosDeCatalogo(ex); true }
        path.matches(Regex("/categorias/\\d+$")) -> { actualizarCategoria(ex); true }
        path.matches(Regex("/productos/\\d+$")) -> { actualizarProducto(ex); true }
        path.matches(Regex("/catalogos/\\d+$")) -> { actualizarCatalogo(ex); true }
        else -> false
    }
    private fun handlePatch(path: String, ex: HttpExchange): Boolean = when {
        path.matches(Regex("/categorias/\\d+$")) -> { actualizarCategoria(ex); true }
        path.matches(Regex("/productos/\\d+$")) -> { actualizarProducto(ex); true }
        path.matches(Regex("/catalogos/\\d+$")) -> { actualizarCatalogo(ex); true }
        else -> false
    }
    private fun handleDelete(path: String, ex: HttpExchange): Boolean = when {
        catalogoProductoItemRegex.matches(path) -> { eliminarProductoDeCatalogo(ex); true }
        path.matches(Regex("/categorias/\\d+$")) -> { eliminarCategoria(ex); true }
        path.matches(Regex("/productos/\\d+$")) -> { eliminarProducto(ex); true }
        path.matches(Regex("/catalogos/\\d+$")) -> { eliminarCatalogo(ex); true }
        else -> false
    }

    // Helpers
    private fun pathId(exchange: HttpExchange): Int = exchange.requestURI.path.substringAfterLast('/').toInt()
    private fun extraerCatalogoId(path: String): Int = path.removePrefix("/catalogos/").substringBefore('/').toInt()
    private fun extraerProductoIdDesdeCatalogoItem(path: String): Pair<Int, Int> {
        val sinPref = path.removePrefix("/catalogos/")
        val catalogoId = sinPref.substringBefore('/').toInt()
        val productoId = sinPref.substringAfter("/productos/").toInt()
        return catalogoId to productoId
    }

    // Categorías
    private fun listarCategorias(exchange: HttpExchange) {
        val categorias = categoriaNegocio.listarCategorias().map { it.toResponse() }
        val body = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CategoriaResponse.serializer()), categorias)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun obtenerCategoria(exchange: HttpExchange) {
        val id = pathId(exchange)
        categoriaNegocio.listarCategorias().firstOrNull { it.id == id }?.let { categoria ->
            val body = json.encodeToString(CategoriaResponse.serializer(), categoria.toResponse())
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }
    private fun crearCategoria(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().decodeToString()
        val req = json.decodeFromString<CrearCategoriaRequest>(body)
        val creada = categoriaNegocio.crearCategoria(req.nombre)
        exchange.responseHeaders.add("Location", "/categorias/${creada.id}")
        val responseBody = json.encodeToString(CategoriaResponse.serializer(), creada.toResponse())
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = responseBody.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(201, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun actualizarCategoria(exchange: HttpExchange) {
        val id = pathId(exchange)
        val req = json.decodeFromString<ActualizarCategoriaRequest>(exchange.requestBody.readBytes().decodeToString())
        val actualizada = req.nombre?.let { categoriaNegocio.actualizarCategoria(id, it) }
        actualizada?.let { categoria ->
            val body = json.encodeToString(CategoriaResponse.serializer(), categoria.toResponse())
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }
    private fun eliminarCategoria(exchange: HttpExchange) {
        val id = pathId(exchange)
        if (categoriaNegocio.eliminarCategoria(id)) exchange.sendResponseHeaders(204, -1)
    }

    // Productos
    private fun listarProductos(exchange: HttpExchange) {
        val productos = productoNegocio.listarProductos().map { it.toResponse() }
        val body = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ProductoResponse.serializer()), productos)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun obtenerProducto(exchange: HttpExchange) {
        val codigo = pathId(exchange)
        productoNegocio.obtenerProducto(codigo)?.let { producto ->
            val body = json.encodeToString(ProductoResponse.serializer(), producto.toResponse())
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }
    private fun crearProducto(exchange: HttpExchange) {
        val req = json.decodeFromString<CrearProductoRequest>(exchange.requestBody.readBytes().decodeToString())
        val creado = productoNegocio.crearProducto(req.nombre, req.descripcion, req.imagen, req.precio, req.stock, req.categoriaId)
        exchange.responseHeaders.add("Location", "/productos/${creado.codigo}")
        val responseBody = json.encodeToString(ProductoResponse.serializer(), creado.toResponse())
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = responseBody.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(201, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun actualizarProducto(exchange: HttpExchange) {
        val codigo = pathId(exchange)
        val req = json.decodeFromString<ActualizarProductoRequest>(exchange.requestBody.readBytes().decodeToString())
        productoNegocio.actualizarProducto(codigo, req.nombre, req.descripcion, req.imagen, req.precio, req.stock, req.categoriaId)?.let { producto ->
            val body = json.encodeToString(ProductoResponse.serializer(), producto.toResponse())
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }
    private fun eliminarProducto(exchange: HttpExchange) {
        val codigo = pathId(exchange)
        if (productoNegocio.eliminarProducto(codigo)) exchange.sendResponseHeaders(204, -1)
    }

    // Catálogos
    private fun listarCatalogos(exchange: HttpExchange) {
        val catalogos = catalogoNegocio.listarCatalogos().map { it.toResponse() }
        val body = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(CatalogoResponse.serializer()), catalogos)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun obtenerCatalogo(exchange: HttpExchange) {
        val id = pathId(exchange)
        catalogoNegocio.listarCatalogos().firstOrNull { it.id == id }?.let { catalogo ->
            val body = json.encodeToString(CatalogoResponse.serializer(), catalogo.toResponse())
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }
    private fun crearCatalogo(exchange: HttpExchange) {
        val req = json.decodeFromString<CrearCatalogoRequest>(exchange.requestBody.readBytes().decodeToString())
        val creado = catalogoNegocio.crearCatalogo(req.nombre, req.descripcion)
        exchange.responseHeaders.add("Location", "/catalogos/${creado.id}")
        val responseBody = json.encodeToString(CatalogoResponse.serializer(), creado.toResponse())
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = responseBody.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(201, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun actualizarCatalogo(exchange: HttpExchange) {
        val id = pathId(exchange)
        val req = json.decodeFromString<ActualizarCatalogoRequest>(exchange.requestBody.readBytes().decodeToString())
        catalogoNegocio.actualizarCatalogo(id, req.nombre ?: return, req.descripcion ?: "")?.let { catalogo ->
            val body = json.encodeToString(CatalogoResponse.serializer(), catalogo.toResponse())
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
        }
    }
    private fun eliminarCatalogo(exchange: HttpExchange) {
        val id = pathId(exchange)
        if (catalogoNegocio.eliminarCatalogo(id)) exchange.sendResponseHeaders(204, -1)
    }

    // Asociación Catálogo-Productos
    private fun listarProductosDeCatalogo(exchange: HttpExchange) {
        val catalogoId = extraerCatalogoId(exchange.requestURI.path)
        if (!catalogoExiste(catalogoId)) { enviarError(exchange, 404, "Catálogo no encontrado"); return }
        val productos = catalogoNegocio.listarProductosDeCatalogo(catalogoId).map { it.toResponse() }
        val body = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ProductoResponse.serializer()), productos)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun agregarProductosACatalogo(exchange: HttpExchange) {
        val catalogoId = extraerCatalogoId(exchange.requestURI.path)
        if (!catalogoExiste(catalogoId)) { enviarError(exchange, 404, "Catálogo no encontrado"); return }
        val req = json.decodeFromString<AsociarProductosRequest>(exchange.requestBody.readBytes().decodeToString())
        if (req.productoIds.isEmpty()) { enviarError(exchange, 400, "Lista vacía"); return }
        val agregados = catalogoNegocio.agregarProductosACatalogo(catalogoId, req.productoIds)
        val responseBody = json.encodeToString(AsociarProductosResponse.serializer(), AsociarProductosResponse(catalogoId, req.productoIds, agregados = agregados))
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = responseBody.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun reemplazarProductosDeCatalogo(exchange: HttpExchange) {
        val catalogoId = extraerCatalogoId(exchange.requestURI.path)
        if (!catalogoExiste(catalogoId)) { enviarError(exchange, 404, "Catálogo no encontrado"); return }
        val req = json.decodeFromString<AsociarProductosRequest>(exchange.requestBody.readBytes().decodeToString())
        val total = catalogoNegocio.reemplazarProductosDeCatalogo(catalogoId, req.productoIds)
        val responseBody = json.encodeToString(AsociarProductosResponse.serializer(), AsociarProductosResponse(catalogoId, req.productoIds, total = total))
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = responseBody.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
    private fun eliminarProductoDeCatalogo(exchange: HttpExchange) {
        val (catalogoId, productoId) = extraerProductoIdDesdeCatalogoItem(exchange.requestURI.path)
        if (!catalogoExiste(catalogoId)) { enviarError(exchange, 404, "Catálogo no encontrado"); return }
        if (catalogoNegocio.eliminarProductoDeCatalogo(catalogoId, productoId)) exchange.sendResponseHeaders(204, -1)
        else enviarError(exchange, 404, "Vínculo no encontrado")
    }

    private fun catalogoExiste(id: Int) = catalogoNegocio.listarCatalogos().any { it.id == id }

    private fun enviarError(exchange: HttpExchange, codigo: Int, mensaje: String) {
        val body = json.encodeToString(ErrorResponse(mensaje))
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(codigo, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
}

// Mapeos
private fun com.tiendavirtual.productos.negocio.Categoria.toResponse() = CategoriaResponse(id, nombre)
private fun com.tiendavirtual.productos.negocio.Producto.toResponse() = ProductoResponse(codigo, nombre, descripcion, imagen, precio, stock, categoriaId)
private fun com.tiendavirtual.productos.negocio.Catalogo.toResponse() = CatalogoResponse(id, nombre, descripcion)
