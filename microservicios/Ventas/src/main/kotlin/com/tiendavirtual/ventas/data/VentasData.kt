package com.tiendavirtual.ventas.data

// Eliminado @Serializable porque no serializamos directamente estas clases con kotlinx

data class VentaData(
    val id: Int,
    val fecha: java.time.LocalDateTime,
    val estado: String,
    val total: Double,
    val clienteId: Int
)

data class DetalleVentaData(
    val id: Int,
    val cantidad: Int,
    val precioUnitario: Double,
    val productoId: Int,
    val ventaId: Int
)

class VentasData(private val dataSource: javax.sql.DataSource) {

    // ================== PRIMITIVAS DE VENTAS (CABECERA) ==================

    fun crearVenta(conn: java.sql.Connection, fecha: java.time.LocalDateTime, estado: String, total: Double, clienteId: Int): VentaData {
        val sql = "INSERT INTO ventas (fecha, estado, total, cliente_id) VALUES (?, ?, ?, ?) RETURNING id"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(fecha))
            stmt.setString(2, estado)
            stmt.setDouble(3, total)
            stmt.setInt(4, clienteId)

            val rs = stmt.executeQuery()
            if (rs.next()) {
                val ventaId = rs.getInt("id")
                return VentaData(ventaId, fecha, estado, total, clienteId)
            } else {
                throw RuntimeException("No se pudo crear la venta")
            }
        }
    }

    fun obtenerVenta(ventaId: Int): VentaData? {
        dataSource.connection.use { conn ->
            val sql = "SELECT id, fecha, estado, total, cliente_id FROM ventas WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, ventaId)
                val rs = stmt.executeQuery()
                return if (rs.next()) {
                    VentaData(
                        id = rs.getInt("id"),
                        fecha = rs.getTimestamp("fecha").toLocalDateTime(),
                        estado = rs.getString("estado"),
                        total = rs.getDouble("total"),
                        clienteId = rs.getInt("cliente_id")
                    )
                } else null
            }
        }
    }

    fun listarVentas(): List<VentaData> {
        dataSource.connection.use { conn ->
            val sql = "SELECT id, fecha, estado, total, cliente_id FROM ventas ORDER BY fecha DESC"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val ventas = mutableListOf<VentaData>()
                while (rs.next()) {
                    ventas.add(VentaData(
                        id = rs.getInt("id"),
                        fecha = rs.getTimestamp("fecha").toLocalDateTime(),
                        estado = rs.getString("estado"),
                        total = rs.getDouble("total"),
                        clienteId = rs.getInt("cliente_id")
                    ))
                }
                return ventas
            }
        }
    }

    fun actualizarEstadoVenta(conn: java.sql.Connection, ventaId: Int, nuevoEstado: String): Boolean {
        val sql = "UPDATE ventas SET estado = ? WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nuevoEstado)
            stmt.setInt(2, ventaId)
            return stmt.executeUpdate() > 0
        }
    }

    fun eliminarVenta(conn: java.sql.Connection, ventaId: Int): Boolean {
        val sql = "DELETE FROM ventas WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, ventaId)
            return stmt.executeUpdate() > 0
        }
    }

    // ================== PRIMITIVAS DE DETALLES DE VENTA ==================

    fun insertarDetalleVenta(conn: java.sql.Connection, cantidad: Int, precioUnitario: Double, productoId: Int, ventaId: Int): DetalleVentaData {
        val sql = "INSERT INTO detalle_venta (cantidad, precio_unitario, producto_id, venta_id) VALUES (?, ?, ?, ?) RETURNING id"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, cantidad)
            stmt.setDouble(2, precioUnitario)
            stmt.setInt(3, productoId)
            stmt.setInt(4, ventaId)

            val rs = stmt.executeQuery()
            if (rs.next()) {
                val detalleId = rs.getInt("id")
                return DetalleVentaData(detalleId, cantidad, precioUnitario, productoId, ventaId)
            } else {
                throw RuntimeException("No se pudo crear el detalle de venta")
            }
        }
    }

    fun obtenerDetallesPorVenta(ventaId: Int): List<DetalleVentaData> {
        dataSource.connection.use { conn ->
            val sql = "SELECT id, cantidad, precio_unitario, producto_id, venta_id FROM detalle_venta WHERE venta_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, ventaId)
                val rs = stmt.executeQuery()
                val detalles = mutableListOf<DetalleVentaData>()
                while (rs.next()) {
                    detalles.add(DetalleVentaData(
                        id = rs.getInt("id"),
                        cantidad = rs.getInt("cantidad"),
                        precioUnitario = rs.getDouble("precio_unitario"),
                        productoId = rs.getInt("producto_id"),
                        ventaId = rs.getInt("venta_id")
                    ))
                }
                return detalles
            }
        }
    }

    fun eliminarDetallesPorVenta(conn: java.sql.Connection, ventaId: Int) {
        val sql = "DELETE FROM detalle_venta WHERE venta_id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, ventaId)
            stmt.executeUpdate()
        }
    }

    // ================== NUEVAS UTILIDADES DE ACTUALIZACIÓN ==================
    fun actualizarVentaCabecera(conn: java.sql.Connection, ventaId: Int, nuevoClienteId: Int, nuevoTotal: Double): Boolean {
        val sql = "UPDATE ventas SET cliente_id = ?, total = ? WHERE id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, nuevoClienteId)
            stmt.setDouble(2, nuevoTotal)
            stmt.setInt(3, ventaId)
            return stmt.executeUpdate() > 0
        }
    }

    // ================== UTILIDADES ==================

    fun obtenerDataSource(): javax.sql.DataSource = dataSource
}
