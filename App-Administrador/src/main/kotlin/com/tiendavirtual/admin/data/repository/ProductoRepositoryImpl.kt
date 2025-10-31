package com.tiendavirtual.admin.data.repository

import com.tiendavirtual.admin.data.remote.api.ProductoApi
import com.tiendavirtual.admin.domain.model.Producto
import com.tiendavirtual.admin.domain.repository.ProductoRepository

class ProductoRepositoryImpl(
    private val api: ProductoApi = ProductoApi()
) : ProductoRepository {

    override suspend fun obtenerProductos(): List<Producto> {
        return api.obtenerProductos()
    }

    override suspend fun crearProducto(producto: Producto): Producto? {
        return api.crearProducto(producto)
    }

    override suspend fun actualizarProducto(codigo: Int, producto: Producto): Producto? {
        return api.actualizarProducto(codigo, producto)
    }

    override suspend fun eliminarProducto(codigo: Int): Boolean {
        return api.eliminarProducto(codigo)
    }
}
