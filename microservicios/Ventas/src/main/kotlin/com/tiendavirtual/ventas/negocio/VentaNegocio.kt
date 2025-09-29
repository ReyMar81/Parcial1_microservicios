package com.tiendavirtual.ventas.negocio

import com.tiendavirtual.ventas.data.VentasData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import kotlinx.serialization.json.Json

// URLs de microservicios
private const val CLIENTES_SERVICE_URL = "http://localhost:8082"
private const val PRODUCTOS_SERVICE_URL = "http://localhost:8081"

private val httpClient = HttpClient.newBuilder().build()
private val json = Json { ignoreUnknownKeys = true }

// Estados de venta
enum class EstadoVenta { CREADA, CONFIRMADA, ANULADA }

// Modelos de dominio (no serializables directamente porque contienen LocalDateTime)
data class Venta(
    val id: Int,
    val fecha: LocalDateTime,
    val estado: EstadoVenta,
    val total: Double,
    val clienteId: Int,
    val detalles: List<DetalleVenta> = emptyList()
)

data class DetalleVenta(
    val id: Int,
    val cantidad: Int,
    val precioUnitario: Double,
    val productoId: Int,
    val ventaId: Int
)

// DTOs para comunicación con otros microservicios
@Serializable
data class ClienteDto(
    val id: Int,
    @SerialName("nombres") val nombres: String,
    val docIdentidad: String,
    val whatsapp: String,
    val direccion: String
)

@Serializable
data class ProductoDto(
    @SerialName("codigo") val id: Int,
    val nombre: String,
    val precio: Double,
    val stock: Int
)

class VentaNegocio(private val ventasData: VentasData) {

    // ================== CREAR VENTA (TRANSACCIÓN COMPLETA) ==================

    fun crearVenta(clienteId: Int, detalles: List<Pair<Int, Int>>, precios: Map<Int, Double>): Venta {
        // 1. VALIDAR CLIENTE EXISTE (consulta a microservicio Clientes)
        val cliente = obtenerCliente(clienteId)
            ?: throw IllegalArgumentException("Cliente con ID $clienteId no existe")

        // 2. VALIDAR PRODUCTOS EXISTEN Y TIENEN STOCK (consulta a microservicio Productos)
        val productosValidados = mutableMapOf<Int, ProductoDto>()
        for ((productoId, cantidad) in detalles) {
            val producto = obtenerProducto(productoId)
                ?: throw IllegalArgumentException("Producto con ID $productoId no existe")

            if (producto.stock < cantidad) {
                throw IllegalArgumentException("Stock insuficiente para producto ${producto.nombre}. Disponible: ${producto.stock}, Requerido: $cantidad")
            }

            productosValidados[productoId] = producto
        }

        // 3. CALCULAR TOTAL
        var total = 0.0
        for ((productoId, cantidad) in detalles) {
            val precioUnitario = precios[productoId] ?: productosValidados[productoId]!!.precio
            total += cantidad * precioUnitario
        }

        // 4. CREAR VENTA EN TRANSACCIÓN (cabecera + detalles)
        val dataSource = ventasData.obtenerDataSource()
        dataSource.connection.use { conn ->
            try {
                conn.autoCommit = false // INICIAR TRANSACCIÓN

                // 4.1 Crear cabecera de venta
                val fecha = LocalDateTime.now()
                val estado = EstadoVenta.CREADA
                val ventaData = ventasData.crearVenta(conn, fecha, estado.name, total, clienteId)

                // 4.2 Crear detalles de venta
                val detallesCreados = mutableListOf<DetalleVenta>()
                for ((productoId, cantidad) in detalles) {
                    val precioUnitario = precios[productoId] ?: productosValidados[productoId]!!.precio
                    val detalleData = ventasData.insertarDetalleVenta(conn, cantidad, precioUnitario, productoId, ventaData.id)

                    detallesCreados.add(DetalleVenta(
                        id = detalleData.id,
                        cantidad = detalleData.cantidad,
                        precioUnitario = detalleData.precioUnitario,
                        productoId = detalleData.productoId,
                        ventaId = detalleData.ventaId
                    ))
                }

                conn.commit() // CONFIRMAR TRANSACCIÓN

                return Venta(
                    id = ventaData.id,
                    fecha = ventaData.fecha,
                    estado = EstadoVenta.valueOf(ventaData.estado),
                    total = ventaData.total,
                    clienteId = ventaData.clienteId,
                    detalles = detallesCreados
                )

            } catch (e: Exception) {
                conn.rollback() // REVERTIR TRANSACCIÓN EN CASO DE ERROR
                throw RuntimeException("Error creando venta: ${e.message}", e)
            } finally {
                conn.autoCommit = true
            }
        }
    }

    // ================== CONFIRMAR VENTA ==================

    fun confirmarVenta(ventaId: Int): Venta? {
        val dataSource = ventasData.obtenerDataSource()
        dataSource.connection.use { conn ->
            try {
                conn.autoCommit = false

                // Validar que la venta existe y está en estado CREADA
                val ventaActual = obtenerVenta(ventaId)
                if (ventaActual == null || ventaActual.estado != EstadoVenta.CREADA) {
                    return null
                }

                // Cambiar estado a CONFIRMADA
                val actualizado = ventasData.actualizarEstadoVenta(conn, ventaId, EstadoVenta.CONFIRMADA.name)

                conn.commit()

                return if (actualizado) {
                    ventaActual.copy(estado = EstadoVenta.CONFIRMADA)
                } else null

            } catch (e: Exception) {
                conn.rollback()
                throw RuntimeException("Error confirmando venta: ${e.message}", e)
            } finally {
                conn.autoCommit = true
            }
        }
    }

    // ================== ANULAR VENTA ==================

    fun anularVenta(ventaId: Int): Venta? {
        val dataSource = ventasData.obtenerDataSource()
        dataSource.connection.use { conn ->
            try {
                conn.autoCommit = false

                // Validar que la venta existe y está en estado CREADA
                val ventaActual = obtenerVenta(ventaId)
                if (ventaActual == null || ventaActual.estado != EstadoVenta.CREADA) {
                    return null
                }

                // Cambiar estado a ANULADA
                val actualizado = ventasData.actualizarEstadoVenta(conn, ventaId, EstadoVenta.ANULADA.name)

                conn.commit()

                return if (actualizado) {
                    ventaActual.copy(estado = EstadoVenta.ANULADA)
                } else null

            } catch (e: Exception) {
                conn.rollback()
                throw RuntimeException("Error anulando venta: ${e.message}", e)
            } finally {
                conn.autoCommit = true
            }
        }
    }

    // ================== OBTENER VENTA COMPLETA ==================

    fun obtenerVenta(ventaId: Int): Venta? {
        val ventaData = ventasData.obtenerVenta(ventaId) ?: return null
        val detallesData = ventasData.obtenerDetallesPorVenta(ventaId)

        val detalles = detallesData.map { detalle ->
            DetalleVenta(
                id = detalle.id,
                cantidad = detalle.cantidad,
                precioUnitario = detalle.precioUnitario,
                productoId = detalle.productoId,
                ventaId = detalle.ventaId
            )
        }

        return Venta(
            id = ventaData.id,
            fecha = ventaData.fecha,
            estado = EstadoVenta.valueOf(ventaData.estado),
            total = ventaData.total,
            clienteId = ventaData.clienteId,
            detalles = detalles
        )
    }

    // ================== LISTAR VENTAS ==================

    fun listarVentas(): List<Venta> {
        val ventasLista = ventasData.listarVentas()
        return ventasLista.map { ventaData ->
            val detalles = this.ventasData.obtenerDetallesPorVenta(ventaData.id).map { detalle ->
                DetalleVenta(
                    id = detalle.id,
                    cantidad = detalle.cantidad,
                    precioUnitario = detalle.precioUnitario,
                    productoId = detalle.productoId,
                    ventaId = detalle.ventaId
                )
            }

            Venta(
                id = ventaData.id,
                fecha = ventaData.fecha,
                estado = EstadoVenta.valueOf(ventaData.estado),
                total = ventaData.total,
                clienteId = ventaData.clienteId,
                detalles = detalles
            )
        }
    }

    // ================== ACTUALIZAR VENTA ==================

    fun actualizarVenta(ventaId: Int, clienteId: Int, detalles: List<Pair<Int, Int>>, precios: Map<Int, Double>): Venta? {
        // Validar que la venta existe y está en estado CREADA
        val ventaActual = obtenerVenta(ventaId)
        if (ventaActual == null || ventaActual.estado != EstadoVenta.CREADA) {
            return null
        }

        // Validar cliente y productos igual que en crear venta
        val cliente = obtenerCliente(clienteId) ?: return null

        val productosValidados = mutableMapOf<Int, ProductoDto>()
        for ((productoId, cantidad) in detalles) {
            val producto = obtenerProducto(productoId) ?: return null
            if (producto.stock < cantidad) return null
            productosValidados[productoId] = producto
        }

        // Calcular nuevo total
        var total = 0.0
        for ((productoId, cantidad) in detalles) {
            val precioUnitario = precios[productoId] ?: productosValidados[productoId]!!.precio
            total += cantidad * precioUnitario
        }

        // Actualizar en transacción
        val dataSource = ventasData.obtenerDataSource()
        dataSource.connection.use { conn ->
            try {
                conn.autoCommit = false

                // Eliminar detalles existentes
                ventasData.eliminarDetallesPorVenta(conn, ventaId)

                // Actualizar cabecera (cliente y total) manteniendo estado CREADA
                ventasData.actualizarVentaCabecera(conn, ventaId, clienteId, total)

                // Crear nuevos detalles
                val detallesCreados = mutableListOf<DetalleVenta>()
                for ((productoId, cantidad) in detalles) {
                    val precioUnitario = precios[productoId] ?: productosValidados[productoId]!!.precio
                    val detalleData = ventasData.insertarDetalleVenta(conn, cantidad, precioUnitario, productoId, ventaId)

                    detallesCreados.add(DetalleVenta(
                        id = detalleData.id,
                        cantidad = detalleData.cantidad,
                        precioUnitario = detalleData.precioUnitario,
                        productoId = detalleData.productoId,
                        ventaId = detalleData.ventaId
                    ))
                }

                conn.commit()

                return Venta(
                    id = ventaId,
                    fecha = ventaActual.fecha,
                    estado = EstadoVenta.CREADA,
                    total = total,
                    clienteId = clienteId,
                    detalles = detallesCreados
                )

            } catch (e: Exception) {
                conn.rollback()
                throw RuntimeException("Error actualizando venta: ${e.message}", e)
            } finally {
                conn.autoCommit = true
            }
        }
    }

    // ================== ELIMINAR VENTA ==================

    fun eliminarVenta(ventaId: Int): Boolean {
        val dataSource = ventasData.obtenerDataSource()
        dataSource.connection.use { conn ->
            try {
                conn.autoCommit = false

                // Eliminar detalles primero (por restricción de clave foránea)
                ventasData.eliminarDetallesPorVenta(conn, ventaId)

                // Eliminar cabecera
                val eliminado = ventasData.eliminarVenta(conn, ventaId)

                conn.commit()
                return eliminado

            } catch (e: Exception) {
                conn.rollback()
                throw RuntimeException("Error eliminando venta: ${e.message}", e)
            } finally {
                conn.autoCommit = true
            }
        }
    }

    // ================== COMUNICACIÓN CON OTROS MICROSERVICIOS ==================

    private fun obtenerCliente(clienteId: Int): ClienteDto? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$CLIENTES_SERVICE_URL/clientes/$clienteId"))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                json.decodeFromString(ClienteDto.serializer(), response.body())
            } else null

        } catch (e: Exception) {
            null
        }
    }

    private fun obtenerProducto(productoId: Int): ProductoDto? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$PRODUCTOS_SERVICE_URL/productos/$productoId"))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                json.decodeFromString(ProductoDto.serializer(), response.body())
            } else null

        } catch (e: Exception) {
            null
        }
    }
}
