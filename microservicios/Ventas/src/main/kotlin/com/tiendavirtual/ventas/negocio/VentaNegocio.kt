package com.tiendavirtual.ventas.negocio

import com.tiendavirtual.ventas.data.VentasData
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

private val logger = LoggerFactory.getLogger("VentaNegocio")

/**
 * Entidades de la capa de negocio
 */
data class Venta(
    val id: Int,
    val fecha: LocalDateTime,
    val clienteId: Int,
    val estado: EstadoVenta,
    val total: Double,
    val detalles: List<DetalleVenta> = emptyList()
)

data class DetalleVenta(
    val id: Int,
    val ventaId: Int,
    val productoId: String,
    val cantidad: Int,
    val precioUnitario: Double
)

enum class EstadoVenta {
    CREADA, CONFIRMADA, ANULADA
}

/**
 * Capa de Negocio del microservicio Ventas
 * Responsabilidad: Lógica de negocio, validaciones y orquestación de transacciones
 */
class VentaNegocio(private val ventasData: VentasData) {

    /**
     * Crear nueva venta en estado CREADA
     */
    fun crearVenta(clienteId: Int, detalles: List<Pair<String, Int>>): Venta {
        logger.info("🔧 Negocio: Creando venta para cliente: $clienteId")

        // Validaciones de negocio
        if (detalles.isEmpty()) {
            throw IllegalArgumentException("La venta debe tener al menos un producto")
        }

        for ((productoId, cantidad) in detalles) {
            if (cantidad <= 0) {
                throw IllegalArgumentException("La cantidad debe ser mayor a 0 para producto: $productoId")
            }
        }

        // Crear venta en estado CREADA
        return ventasData.crearVenta(clienteId, detalles)
    }

    /**
     * Obtener venta por ID
     */
    fun obtenerVenta(id: Int): Venta? {
        logger.info("🔧 Negocio: Obteniendo venta ID: $id")
        return ventasData.obtenerVenta(id)
    }

    /**
     * Obtener todas las ventas
     */
    fun obtenerVentas(): List<Venta> {
        logger.info("🔧 Negocio: Obteniendo todas las ventas")
        return ventasData.obtenerVentas()
    }

    /**
     * Confirmar venta - Flujo transaccional completo
     * 1. Validar cliente existe
     * 2. Reservar stock de productos
     * 3. Confirmar stock
     * 4. Calcular total
     * 5. Cambiar estado a CONFIRMADA
     */
    fun confirmarVenta(ventaId: Int): ConfirmacionResult {
        logger.info("🔧 Negocio: Confirmando venta ID: $ventaId")

        val venta = ventasData.obtenerVenta(ventaId)
            ?: throw IllegalArgumentException("Venta no encontrada: $ventaId")

        if (venta.estado != EstadoVenta.CREADA) {
            throw IllegalArgumentException("Solo se pueden confirmar ventas en estado CREADA")
        }

        try {
            // 1. Validar cliente (llamada a microservicio Clientes)
            val clienteValido = validarCliente(venta.clienteId)
            if (!clienteValido) {
                return ConfirmacionResult(false, "Cliente no válido", null)
            }

            // 2. Reservar stock (llamada a microservicio Productos)
            val itemsReserva = venta.detalles.map { it.productoId to it.cantidad }
            val reservaResult = reservarStock(itemsReserva)

            if (!reservaResult.todosReservados) {
                return ConfirmacionResult(false, "Stock insuficiente para algunos productos", null)
            }

            // 3. Confirmar stock
            val stockConfirmado = confirmarStock(reservaResult.reservaId)
            if (!stockConfirmado) {
                liberarStock(reservaResult.reservaId)
                return ConfirmacionResult(false, "Error al confirmar stock", null)
            }

            // 4. Calcular total y confirmar venta
            val total = calcularTotal(venta.detalles)
            val ventaConfirmada = ventasData.confirmarVenta(ventaId, total)

            return if (ventaConfirmada != null) {
                ConfirmacionResult(true, "Venta confirmada exitosamente", ventaConfirmada)
            } else {
                // Si falla, liberar stock
                liberarStock(reservaResult.reservaId)
                ConfirmacionResult(false, "Error al confirmar venta", null)
            }

        } catch (e: Exception) {
            logger.error("Error al confirmar venta: ${e.message}", e)
            return ConfirmacionResult(false, "Error interno: ${e.message}", null)
        }
    }

    /**
     * Anular venta - Libera reservas de stock
     */
    fun anularVenta(ventaId: Int): Boolean {
        logger.info("🔧 Negocio: Anulando venta ID: $ventaId")

        val venta = ventasData.obtenerVenta(ventaId)
            ?: throw IllegalArgumentException("Venta no encontrada: $ventaId")

        if (venta.estado == EstadoVenta.ANULADA) {
            throw IllegalArgumentException("La venta ya está anulada")
        }

        // Si la venta estaba confirmada, liberar stock
        if (venta.estado == EstadoVenta.CONFIRMADA) {
            val itemsLiberar = venta.detalles.map { it.productoId to it.cantidad }
            liberarStockConfirmado(itemsLiberar)
        }

        return ventasData.anularVenta(ventaId)
    }

    /**
     * Generar nota de venta en PDF
     */
    fun generarNotaVenta(ventaId: Int): ByteArray {
        logger.info("🔧 Negocio: Generando nota de venta ID: $ventaId")

        val venta = ventasData.obtenerVenta(ventaId)
            ?: throw IllegalArgumentException("Venta no encontrada: $ventaId")

        if (venta.estado != EstadoVenta.CONFIRMADA) {
            throw IllegalArgumentException("Solo se puede generar nota de ventas confirmadas")
        }

        return ventasData.generarNotaVentaPdf(venta)
    }

    // ==================== MÉTODOS PRIVADOS DE INTEGRACIÓN ====================

    /**
     * Validar cliente - Integración con microservicio Clientes
     */
    private fun validarCliente(clienteId: Int): Boolean {
        // TODO: Implementar llamada HTTP al microservicio Clientes
        // Por ahora, simulación
        logger.info("🔧 Validando cliente ID: $clienteId en microservicio Clientes")
        return true
    }

    /**
     * Reservar stock - Integración con microservicio Productos
     */
    private fun reservarStock(items: List<Pair<String, Int>>): ReservaStockResult {
        // TODO: Implementar llamada HTTP al microservicio Productos
        // Por ahora, simulación
        logger.info("🔧 Reservando stock en microservicio Productos")
        return ReservaStockResult(UUID.randomUUID().toString(), true)
    }

    /**
     * Confirmar stock - Integración con microservicio Productos
     */
    private fun confirmarStock(reservaId: String): Boolean {
        // TODO: Implementar llamada HTTP al microservicio Productos
        logger.info("🔧 Confirmando stock reserva: $reservaId")
        return true
    }

    /**
     * Liberar stock - Integración con microservicio Productos
     */
    private fun liberarStock(reservaId: String): Boolean {
        // TODO: Implementar llamada HTTP al microservicio Productos
        logger.info("🔧 Liberando stock reserva: $reservaId")
        return true
    }

    /**
     * Liberar stock confirmado - Para ventas anuladas
     */
    private fun liberarStockConfirmado(items: List<Pair<String, Int>>): Boolean {
        // TODO: Implementar llamada HTTP al microservicio Productos
        logger.info("🔧 Liberando stock confirmado")
        return true
    }

    /**
     * Calcular total de la venta
     */
    private fun calcularTotal(detalles: List<DetalleVenta>): Double {
        return detalles.sumOf { it.cantidad * it.precioUnitario }
    }
}

/**
 * Clases de respuesta para operaciones complejas
 */
data class ConfirmacionResult(
    val exitoso: Boolean,
    val mensaje: String,
    val venta: Venta?
)

data class ReservaStockResult(
    val reservaId: String,
    val todosReservados: Boolean
)
