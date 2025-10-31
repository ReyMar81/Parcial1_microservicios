package com.tiendavirtual.admin.data.repository

import com.tiendavirtual.admin.data.remote.api.VentaApi
import com.tiendavirtual.admin.domain.model.Venta
import com.tiendavirtual.admin.domain.repository.VentaRepository

class VentaRepositoryImpl(
    private val api: VentaApi = VentaApi()
) : VentaRepository {
    
    override suspend fun obtenerVentas(): List<Venta> {
        return api.obtenerVentas()
    }

    override suspend fun obtenerVenta(id: Int): Venta? {
        return api.obtenerVenta(id)
    }

    override suspend fun crearVenta(venta: Venta): Boolean {
        return api.crearVenta(venta)
    }

    override suspend fun actualizarVenta(venta: Venta): Boolean {
        return api.actualizarVenta(venta)
    }

    override suspend fun confirmarVenta(id: Int): Boolean {
        return api.confirmarVenta(id)
    }

    override suspend fun anularVenta(id: Int): Boolean {
        return api.anularVenta(id)
    }

    override suspend fun eliminarVenta(id: Int): Boolean {
        return api.eliminarVenta(id)
    }
}
