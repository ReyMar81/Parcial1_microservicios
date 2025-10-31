package com.tiendavirtual.admin.domain.repository

import com.tiendavirtual.admin.domain.model.Venta

interface VentaRepository {
    suspend fun obtenerVentas(): List<Venta>
    suspend fun obtenerVenta(id: Int): Venta?
    suspend fun crearVenta(venta: Venta): Boolean
    suspend fun actualizarVenta(venta: Venta): Boolean
    suspend fun confirmarVenta(id: Int): Boolean
    suspend fun anularVenta(id: Int): Boolean
    suspend fun eliminarVenta(id: Int): Boolean
}
