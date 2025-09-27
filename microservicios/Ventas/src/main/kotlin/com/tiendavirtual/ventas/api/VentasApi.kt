package com.tiendavirtual.ventas.api

import com.sun.net.httpserver.HttpExchange
import com.tiendavirtual.ventas.negocio.VentaNegocio
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("VentasAPI")
private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class CrearVentaRequest(
    val clienteId: Int,
    val items: List<ItemVentaRequest>
)

@Serializable
data class ItemVentaRequest(
    val productoId: String,
    val cantidad: Int
)

@Serializable
data class VentaResponse(
    val id: Int,
    val fecha: String,
    val clienteId: Int,
    val estado: String,
    val total: Double,
    val detalles: List<DetalleVentaResponse>
)

@Serializable
data class DetalleVentaResponse(
    val id: Int,
    val productoId: String,
    val cantidad: Int,
    val precioUnitario: Double
)

/**
 * Capa API del microservicio Ventas
 * Responsabilidad: Solo manejo de HTTP requests/responses
 * Delega toda la lógica a la capa de negocio
 */
class VentasApi(private val ventaNegocio: VentaNegocio) {

    /**
     * Manejar todas las rutas de ventas
     */
    fun manejarRequest(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod

            logger.info("🌐 API: $method $path")

            when {
                // POST /ventas - Crear venta
                path == "/ventas" && method == "POST" -> crearVenta(exchange)

                // GET /ventas - Listar ventas
                path == "/ventas" && method == "GET" -> listarVentas(exchange)

                // GET /ventas/{id} - Obtener venta por ID
                path.matches(Regex("/ventas/\\d+$")) && method == "GET" -> obtenerVenta(exchange)

                // POST /ventas/{id}/confirmar - Confirmar venta
                path.matches(Regex("/ventas/\\d+/confirmar$")) && method == "POST" -> confirmarVenta(exchange)

                // POST /ventas/{id}/anular - Anular venta
                path.matches(Regex("/ventas/\\d+/anular$")) && method == "POST" -> anularVenta(exchange)

                // POST /ventas/{id}/nota-pdf - Generar nota PDF
                path.matches(Regex("/ventas/\\d+/nota-pdf$")) && method == "POST" -> generarNotaPdf(exchange)

                // GET /ventas/{id}/nota-pdf - Descargar nota PDF
                path.matches(Regex("/ventas/\\d+/nota-pdf$")) && method == "GET" -> descargarNotaPdf(exchange)

                else -> {
                    enviarError(exchange, 404, "Endpoint no encontrado")
                }
            }

        } catch (e: Exception) {
            logger.error("Error en VentasAPI: ${e.message}", e)
            enviarError(exchange, 500, "Error interno del servidor: ${e.message}")
        }
    }

    private fun crearVenta(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        val request = json.decodeFromString<CrearVentaRequest>(body)

        val items = request.items.map { it.productoId to it.cantidad }
        val venta = ventaNegocio.crearVenta(request.clienteId, items)

        val response = VentaResponse(
            id = venta.id,
            fecha = venta.fecha.toString(),
            clienteId = venta.clienteId,
            estado = venta.estado.name,
            total = venta.total,
            detalles = venta.detalles.map {
                DetalleVentaResponse(it.id, it.productoId, it.cantidad, it.precioUnitario)
            }
        )

        enviarRespuestaExitosa(exchange, 201, response)
    }

    private fun listarVentas(exchange: HttpExchange) {
        val ventas = ventaNegocio.obtenerVentas()
        val response = ventas.map { venta ->
            VentaResponse(
                id = venta.id,
                fecha = venta.fecha.toString(),
                clienteId = venta.clienteId,
                estado = venta.estado.name,
                total = venta.total,
                detalles = venta.detalles.map {
                    DetalleVentaResponse(it.id, it.productoId, it.cantidad, it.precioUnitario)
                }
            )
        }

        enviarRespuestaExitosa(exchange, 200, response)
    }

    private fun obtenerVenta(exchange: HttpExchange) {
        val id = extraerIdDeRuta(exchange.requestURI.path)
        val venta = ventaNegocio.obtenerVenta(id)

        if (venta != null) {
            val response = VentaResponse(
                id = venta.id,
                fecha = venta.fecha.toString(),
                clienteId = venta.clienteId,
                estado = venta.estado.name,
                total = venta.total,
                detalles = venta.detalles.map {
                    DetalleVentaResponse(it.id, it.productoId, it.cantidad, it.precioUnitario)
                }
            )
            enviarRespuestaExitosa(exchange, 200, response)
        } else {
            enviarError(exchange, 404, "Venta no encontrada")
        }
    }

    private fun confirmarVenta(exchange: HttpExchange) {
        val id = extraerIdDeRuta(exchange.requestURI.path)
        val resultado = ventaNegocio.confirmarVenta(id)

        if (resultado.exitoso && resultado.venta != null) {
            val response = VentaResponse(
                id = resultado.venta.id,
                fecha = resultado.venta.fecha.toString(),
                clienteId = resultado.venta.clienteId,
                estado = resultado.venta.estado.name,
                total = resultado.venta.total,
                detalles = resultado.venta.detalles.map {
                    DetalleVentaResponse(it.id, it.productoId, it.cantidad, it.precioUnitario)
                }
            )
            enviarRespuestaExitosa(exchange, 200, mapOf("mensaje" to resultado.mensaje, "venta" to response))
        } else {
            enviarError(exchange, 400, resultado.mensaje)
        }
    }

    private fun anularVenta(exchange: HttpExchange) {
        val id = extraerIdDeRuta(exchange.requestURI.path)
        val anulada = ventaNegocio.anularVenta(id)

        if (anulada) {
            enviarRespuestaExitosa(exchange, 200, mapOf("mensaje" to "Venta anulada exitosamente"))
        } else {
            enviarError(exchange, 400, "No se pudo anular la venta")
        }
    }

    private fun generarNotaPdf(exchange: HttpExchange) {
        val id = extraerIdDeRuta(exchange.requestURI.path)
        val pdfBytes = ventaNegocio.generarNotaVenta(id)

        exchange.responseHeaders.set("Content-Type", "application/pdf")
        exchange.responseHeaders.set("Content-Disposition", "attachment; filename=nota_venta_$id.pdf")
        exchange.sendResponseHeaders(200, pdfBytes.size.toLong())
        exchange.responseBody.write(pdfBytes)
        exchange.responseBody.close()
    }

    private fun descargarNotaPdf(exchange: HttpExchange) {
        generarNotaPdf(exchange) // Mismo comportamiento
    }

    // Métodos utilitarios
    private fun extraerIdDeRuta(path: String): Int {
        return path.split("/")[2].toInt()
    }

    private fun enviarRespuestaExitosa(exchange: HttpExchange, codigo: Int, data: Any) {
        val jsonResponse = json.encodeToString(mapOf("success" to true, "data" to data))
        val responseBytes = jsonResponse.toByteArray()

        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(codigo, responseBytes.size.toLong())
        exchange.responseBody.write(responseBytes)
        exchange.responseBody.close()
    }

    private fun enviarError(exchange: HttpExchange, codigo: Int, mensaje: String) {
        val jsonResponse = json.encodeToString(mapOf("success" to false, "error" to mensaje))
        val responseBytes = jsonResponse.toByteArray()

        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(codigo, responseBytes.size.toLong())
        exchange.responseBody.write(responseBytes)
        exchange.responseBody.close()
    }
}
