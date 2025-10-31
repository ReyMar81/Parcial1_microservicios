package com.tiendavirtual.admin.domain.model

data class Venta(
    val id: Int,
    val fecha: String,
    val estado: String,
    val total: Double,
    val clienteId: Int,
    val detalles: List<DetalleVenta> = emptyList()
)

data class DetalleVenta(
    val id: Int,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)

data class CrearVentaRequest(
    val clienteId: Int,
    val fecha: String,
    val estado: String,
    val total: Double,
    val detalles: List<DetalleVentaRequest>
)

data class DetalleVentaRequest(
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double
)
