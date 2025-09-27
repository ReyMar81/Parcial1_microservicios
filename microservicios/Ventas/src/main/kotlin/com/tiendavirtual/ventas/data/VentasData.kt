package com.tiendavirtual.ventas.data

import com.tiendavirtual.ventas.negocio.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("VentasData")

/**
 * Capa de Datos del microservicio Ventas
 * Responsabilidad: Acceso a base de datos y persistencia
 */
class VentasData(private val dataSource: DataSource) {

    /**
     * Crear nueva venta en estado CREADA
     */
    fun crearVenta(clienteId: Int, detalles: List<Pair<String, Int>>): Venta {
        logger.info("💾 Data: Creando venta para cliente: $clienteId")

        dataSource.connection.use { conn ->
            conn.autoCommit = false

            try {
                // Crear venta
                val ventaId = conn.prepareStatement("""
                    INSERT INTO ventas (fecha, cliente_id, estado, total) 
                    VALUES (?, ?, ?, ?) 
                    RETURNING id
                """).use { stmt ->
                    stmt.setObject(1, LocalDateTime.now())
                    stmt.setInt(2, clienteId)
                    stmt.setString(3, EstadoVenta.CREADA.name)
                    stmt.setDouble(4, 0.0) // Total se calculará al confirmar

                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt("id") else throw RuntimeException("Error al crear venta")
                }

                // Crear detalles de venta (precios se obtendrán de productos)
                for ((productoId, cantidad) in detalles) {
                    conn.prepareStatement("""
                        INSERT INTO detalleventa (venta_id, producto_id, cantidad, precio_unitario) 
                        VALUES (?, ?, ?, ?)
                    """).use { stmt ->
                        stmt.setInt(1, ventaId)
                        stmt.setString(2, productoId)
                        stmt.setInt(3, cantidad)
                        stmt.setDouble(4, 0.0) // Precio se actualizará al confirmar
                        stmt.executeUpdate()
                    }
                }

                conn.commit()
                return obtenerVenta(ventaId)!!

            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    /**
     * Obtener venta por ID con sus detalles
     */
    fun obtenerVenta(id: Int): Venta? {
        logger.info("💾 Data: Consultando venta ID: $id")

        dataSource.connection.use { conn ->
            // Obtener venta
            val venta = conn.prepareStatement("""
                SELECT id, fecha, cliente_id, estado, total 
                FROM ventas WHERE id = ?
            """).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    Venta(
                        id = rs.getInt("id"),
                        fecha = rs.getTimestamp("fecha").toLocalDateTime(),
                        clienteId = rs.getInt("cliente_id"),
                        estado = EstadoVenta.valueOf(rs.getString("estado")),
                        total = rs.getDouble("total"),
                        detalles = emptyList() // Se llenarán después
                    )
                } else null
            } ?: return null

            // Obtener detalles
            val detalles = conn.prepareStatement("""
                SELECT id, venta_id, producto_id, cantidad, precio_unitario 
                FROM detalleventa WHERE venta_id = ?
            """).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                val detalles = mutableListOf<DetalleVenta>()

                while (rs.next()) {
                    detalles.add(
                        DetalleVenta(
                            id = rs.getInt("id"),
                            ventaId = rs.getInt("venta_id"),
                            productoId = rs.getString("producto_id"),
                            cantidad = rs.getInt("cantidad"),
                            precioUnitario = rs.getDouble("precio_unitario")
                        )
                    )
                }
                detalles
            }

            return venta.copy(detalles = detalles)
        }
    }

    /**
     * Obtener todas las ventas
     */
    fun obtenerVentas(): List<Venta> {
        logger.info("💾 Data: Consultando todas las ventas")

        val sql = """
            SELECT id, fecha, cliente_id, estado, total 
            FROM ventas ORDER BY fecha DESC
        """
        val ventas = mutableListOf<Venta>()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val ventaId = rs.getInt("id")
                    val detalles = obtenerDetallesVenta(ventaId)

                    ventas.add(
                        Venta(
                            id = ventaId,
                            fecha = rs.getTimestamp("fecha").toLocalDateTime(),
                            clienteId = rs.getInt("cliente_id"),
                            estado = EstadoVenta.valueOf(rs.getString("estado")),
                            total = rs.getDouble("total"),
                            detalles = detalles
                        )
                    )
                }
            }
        }

        return ventas
    }

    /**
     * Confirmar venta - actualizar estado y total
     */
    fun confirmarVenta(ventaId: Int, total: Double): Venta? {
        logger.info("💾 Data: Confirmando venta ID: $ventaId con total: $total")

        val sql = """
            UPDATE ventas 
            SET estado = ?, total = ? 
            WHERE id = ? AND estado = ?
            RETURNING id
        """

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, EstadoVenta.CONFIRMADA.name)
                stmt.setDouble(2, total)
                stmt.setInt(3, ventaId)
                stmt.setString(4, EstadoVenta.CREADA.name)

                val rs = stmt.executeQuery()
                return if (rs.next()) {
                    obtenerVenta(ventaId)
                } else null
            }
        }
    }

    /**
     * Anular venta
     */
    fun anularVenta(ventaId: Int): Boolean {
        logger.info("💾 Data: Anulando venta ID: $ventaId")

        val sql = "UPDATE ventas SET estado = ? WHERE id = ?"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, EstadoVenta.ANULADA.name)
                stmt.setInt(2, ventaId)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * Actualizar precios unitarios en los detalles
     */
    fun actualizarPreciosDetalles(ventaId: Int, precios: Map<String, Double>) {
        logger.info("💾 Data: Actualizando precios para venta ID: $ventaId")

        dataSource.connection.use { conn ->
            for ((productoId, precio) in precios) {
                conn.prepareStatement("""
                    UPDATE detalleventa 
                    SET precio_unitario = ? 
                    WHERE venta_id = ? AND producto_id = ?
                """).use { stmt ->
                    stmt.setDouble(1, precio)
                    stmt.setInt(2, ventaId)
                    stmt.setString(3, productoId)
                    stmt.executeUpdate()
                }
            }
        }
    }

    /**
     * Generar PDF de nota de venta
     */
    fun generarNotaVentaPdf(venta: Venta): ByteArray {
        logger.info("💾 Data: Generando PDF nota de venta ID: ${venta.id}")

        val contenido = buildString {
            append("NOTA DE VENTA\n")
            append("=".repeat(40) + "\n")
            append("Número: ${venta.id}\n")
            append("Fecha: ${venta.fecha}\n")
            append("Cliente ID: ${venta.clienteId}\n")
            append("Estado: ${venta.estado}\n\n")

            append("DETALLE DE PRODUCTOS:\n")
            append("-".repeat(40) + "\n")

            for (detalle in venta.detalles) {
                val subtotal = detalle.cantidad * detalle.precioUnitario
                append("Producto: ${detalle.productoId}\n")
                append("Cantidad: ${detalle.cantidad}\n")
                append("Precio Unit.: $${detalle.precioUnitario}\n")
                append("Subtotal: $${subtotal}\n")
                append("-".repeat(20) + "\n")
            }

            append("\nTOTAL: $${venta.total}\n")
            append("=".repeat(40) + "\n")
        }

        return contenido.toByteArray()
    }

    /**
     * Obtener detalles de una venta específica
     */
    private fun obtenerDetallesVenta(ventaId: Int): List<DetalleVenta> {
        val sql = """
            SELECT id, venta_id, producto_id, cantidad, precio_unitario 
            FROM detalleventa WHERE venta_id = ?
        """
        val detalles = mutableListOf<DetalleVenta>()

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, ventaId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    detalles.add(
                        DetalleVenta(
                            id = rs.getInt("id"),
                            ventaId = rs.getInt("venta_id"),
                            productoId = rs.getString("producto_id"),
                            cantidad = rs.getInt("cantidad"),
                            precioUnitario = rs.getDouble("precio_unitario")
                        )
                    )
                }
            }
        }

        return detalles
    }
}
