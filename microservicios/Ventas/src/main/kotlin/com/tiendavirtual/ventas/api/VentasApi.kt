package com.tiendavirtual.ventas.api

import com.sun.net.httpserver.HttpExchange
import com.tiendavirtual.ventas.negocio.VentaNegocio
import com.tiendavirtual.ventas.negocio.EstadoVenta
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.time.format.DateTimeFormatter
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import kotlin.math.abs

private val json = Json { ignoreUnknownKeys = true }
private val httpClient = HttpClient.newBuilder().build()

// URL del microservicio de productos (solo para confirmar venta y descontar stock)
private const val PRODUCTOS_SERVICE_URL = "http://localhost:8081"

@Serializable
data class CrearVentaRequest(
    val clienteId: Int,
    val fecha: String,
    val estado: String,
    val total: Double,
    val detalles: List<DetalleVentaRequest>
)

@Serializable
data class DetalleVentaRequest(
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)

@Serializable
data class ProductoResponse(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val stock: Int
)

@Serializable
data class VentaResponse(
    val id: Int,
    val fecha: String,
    val estado: String,
    val total: Double,
    val clienteId: Int,
    val detalles: List<DetalleVentaResponse>
)

@Serializable
data class DetalleVentaResponse(
    val id: Int,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)

@Serializable
data class ErrorResponse(val error: String, val details: String? = null)

@Serializable
data class ConfirmarAnularResponse(val mensaje: String, val venta: VentaResponse)

class VentasApi(private val ventaNegocio: VentaNegocio) {
    fun manejarRequest(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod
            when (method) {
                "GET" -> {
                    if (path == "/ventas") atenderListarVentas(exchange)
                    else if (path.matches(Regex("/ventas/\\d+$"))) atenderObtenerVenta(exchange)
                    else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                "POST" -> {
                    if (path == "/ventas") atenderCrearVenta(exchange)
                    else if (path.matches(Regex("/ventas/\\d+/confirmar$"))) atenderConfirmarVenta(exchange)
                    else if (path.matches(Regex("/ventas/\\d+/anular$"))) atenderAnularVenta(exchange)
                    else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                "PUT" -> {
                    if (path.matches(Regex("/ventas/\\d+$"))) atenderActualizarVenta(exchange)
                    else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                "DELETE" -> {
                    if (path.matches(Regex("/ventas/\\d+$"))) atenderEliminarVenta(exchange)
                    else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                else -> enviarError(exchange, 405, "Método no permitido")
            }
        } catch (e: SerializationException) {
            enviarError(exchange, 400, "JSON inválido")
        } catch (e: Exception) {
            enviarError(exchange, 500, "Error interno", e.message)
        }
    }

    private fun atenderCrearVenta(exchange: HttpExchange) {
        val body = exchange.requestBody.bufferedReader().readText()
        val req = json.decodeFromString(CrearVentaRequest.serializer(), body)

        // Validar datos básicos
        if (req.clienteId <= 0) {
            enviarError(exchange, 400, "Cliente inválido")
            return
        }

        if (req.detalles.isEmpty()) {
            enviarError(exchange, 400, "La venta debe tener al menos un producto")
            return
        }

        // Validar detalles
        for (detalle in req.detalles) {
            if (detalle.productoId <= 0) {
                enviarError(exchange, 400, "Producto inválido: ${detalle.productoId}")
                return
            }
            if (detalle.cantidad <= 0) {
                enviarError(exchange, 400, "Cantidad debe ser mayor a 0 para producto ${detalle.productoId}")
                return
            }
            if (detalle.precioUnitario < 0) {
                enviarError(exchange, 400, "Precio no puede ser negativo para producto ${detalle.productoId}")
                return
            }
        }

        // Verificar que el total calculado coincida con el total enviado
        val totalCalculado = req.detalles.sumOf { it.cantidad * it.precioUnitario }
        if (abs(req.total - totalCalculado) > 0.01) {
            enviarError(exchange, 400, "El total no coincide con la suma de los detalles. Enviado: ${req.total}, Calculado: $totalCalculado")
            return
        }

        // Crear mapas para el negocio
        val productosConCantidades = req.detalles.map { it.productoId to it.cantidad }
        val precios = req.detalles.associate { it.productoId to it.precioUnitario }

        val venta = ventaNegocio.crearVenta(req.clienteId, productosConCantidades, precios)
        enviarJson(exchange, 201, venta.toResponse())
    }

    private fun atenderListarVentas(exchange: HttpExchange) {
        val ventas = ventaNegocio.listarVentas().map { it.toResponse() }
        enviarJson(exchange, 200, ventas)
    }

    private fun atenderObtenerVenta(exchange: HttpExchange) {
        val id = exchange.requestURI.path.substringAfterLast("/").toIntOrNull()
        if (id == null) { enviarError(exchange, 400, "ID inválido"); return }
        val venta = ventaNegocio.obtenerVenta(id)
        if (venta == null) enviarError(exchange, 404, "Venta no encontrada")
        else enviarJson(exchange, 200, venta.toResponse())
    }

    private fun atenderEliminarVenta(exchange: HttpExchange) {
        val id = exchange.requestURI.path.substringAfterLast("/").toIntOrNull()
        if (id == null) { enviarError(exchange, 400, "ID inválido"); return }
        val ok = ventaNegocio.eliminarVenta(id)
        if (ok) enviarJson(exchange, 200, mapOf("ok" to true))
        else enviarError(exchange, 404, "Venta no encontrada")
    }

    private fun atenderConfirmarVenta(exchange: HttpExchange) {
        val id = exchange.requestURI.path.split("/").dropLast(1).last().toIntOrNull()
        if (id == null) {
            enviarError(exchange, 400, "ID inválido")
            return
        }

        // Obtener la venta actual
        val ventaActual = ventaNegocio.obtenerVenta(id)
        if (ventaActual == null) {
            enviarError(exchange, 404, "Venta no encontrada")
            return
        }

        if (ventaActual.estado != EstadoVenta.CREADA) {
            enviarError(exchange, 400, "Solo se pueden confirmar ventas en estado CREADA")
            return
        }

        val venta = ventaNegocio.confirmarVenta(id)
        if (venta == null) {
            enviarError(exchange, 500, "Error al confirmar venta")
        } else {
            enviarJson(exchange, 200, ConfirmarAnularResponse(
                mensaje = "Venta confirmada exitosamente",
                venta = venta.toResponse()
            ))
        }
    }

    private fun atenderAnularVenta(exchange: HttpExchange) {
        val id = exchange.requestURI.path.split("/").dropLast(1).last().toIntOrNull()
        if (id == null) {
            enviarError(exchange, 400, "ID inválido")
            return
        }

        // Obtener la venta actual
        val venta = ventaNegocio.obtenerVenta(id)
        if (venta == null) {
            enviarError(exchange, 404, "Venta no encontrada")
            return
        }

        if (venta.estado != EstadoVenta.CREADA) {
            enviarError(exchange, 400, "Solo se pueden anular ventas en estado CREADA")
            return
        }

        val ventaAnulada = ventaNegocio.anularVenta(id)
        if (ventaAnulada != null) {
            enviarJson(exchange, 200, ConfirmarAnularResponse(
                mensaje = "Venta anulada exitosamente",
                venta = ventaAnulada.toResponse()
            ))
        } else {
            enviarError(exchange, 500, "Error al anular venta")
        }
    }

    private fun atenderActualizarVenta(exchange: HttpExchange) {
        val id = exchange.requestURI.path.substringAfterLast("/").toIntOrNull()
        if (id == null) { enviarError(exchange, 400, "ID inválido"); return }

        val body = exchange.requestBody.bufferedReader().readText()
        val req = json.decodeFromString(CrearVentaRequest.serializer(), body)

        // Validar estado de la venta
        val ventaExistente = ventaNegocio.obtenerVenta(id)
        if (ventaExistente == null) {
            enviarError(exchange, 404, "Venta no encontrada")
            return
        }

        if (ventaExistente.estado != EstadoVenta.CREADA) {
            enviarError(exchange, 400, "Solo se pueden modificar ventas en estado CREADA")
            return
        }

        // Validar datos básicos
        if (req.clienteId <= 0) {
            enviarError(exchange, 400, "Cliente inválido")
            return
        }

        if (req.detalles.isEmpty()) {
            enviarError(exchange, 400, "La venta debe tener al menos un producto")
            return
        }

        // Validar detalles
        for (detalle in req.detalles) {
            if (detalle.productoId <= 0) {
                enviarError(exchange, 400, "Producto inválido: ${detalle.productoId}")
                return
            }
            if (detalle.cantidad <= 0) {
                enviarError(exchange, 400, "Cantidad debe ser mayor a 0 para producto ${detalle.productoId}")
                return
            }
            if (detalle.precioUnitario < 0) {
                enviarError(exchange, 400, "Precio no puede ser negativo para producto ${detalle.productoId}")
                return
            }
        }

        // Verificar que el total calculado coincida con el total enviado
        val totalCalculado = req.detalles.sumOf { it.cantidad * it.precioUnitario }
        if (abs(req.total - totalCalculado) > 0.01) {
            enviarError(exchange, 400, "El total no coincide con la suma de los detalles. Enviado: ${req.total}, Calculado: $totalCalculado")
            return
        }

        // Crear mapas para el negocio
        val productosConCantidades = req.detalles.map { it.productoId to it.cantidad }
        val precios = req.detalles.associate { it.productoId to it.precioUnitario }

        // Actualizar venta
        val ventaActualizada = ventaNegocio.actualizarVenta(id, req.clienteId, productosConCantidades, precios)
        if (ventaActualizada != null) enviarJson(exchange, 200, ventaActualizada.toResponse())
        else enviarError(exchange, 500, "Error al actualizar venta")
    }

    // Funciones de integración con microservicio de productos (solo para stock)
    private fun obtenerProducto(productoId: Int): ProductoResponse? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$PRODUCTOS_SERVICE_URL/productos/$productoId"))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                json.decodeFromString(ProductoResponse.serializer(), response.body())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun descontarStockProductos(productos: List<Pair<Int, Int>>): Boolean {
        return try {
            productos.all { (productoId, cantidad) ->
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$PRODUCTOS_SERVICE_URL/productos/$productoId/stock"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString("""{"cantidad":$cantidad,"operacion":"DESCONTAR"}"""))
                    .build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                response.statusCode() == 200
            }
        } catch (e: Exception) {
            false
        }
    }

    // Helpers para transformar modelos
    private fun com.tiendavirtual.ventas.negocio.Venta.toResponse(): VentaResponse = VentaResponse(
        id = id,
        fecha = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        estado = estado.name,
        total = total,
        clienteId = clienteId,
        detalles = detalles.map { it.toResponse() }
    )

    private fun com.tiendavirtual.ventas.negocio.DetalleVenta.toResponse(): DetalleVentaResponse = DetalleVentaResponse(
        id = id,
        productoId = productoId,
        cantidad = cantidad,
        precioUnitario = precioUnitario
    )

    private inline fun <reified T> enviarJson(exchange: HttpExchange, status: Int, obj: T) {
        val response = json.encodeToString(obj)
        exchange.responseHeaders.set("Content-Type", "application/json")
        val bytes = response.toByteArray()
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
        exchange.responseBody.flush()
        exchange.responseBody.close()
    }

    private fun enviarError(exchange: HttpExchange, status: Int, error: String, details: String? = null) {
        enviarJson(exchange, status, ErrorResponse(error, details))
    }
}