package com.tiendavirtual.admin.domain.repository

import com.tiendavirtual.admin.domain.model.Producto

interface ProductoRepository {
    suspend fun obtenerProductos(): List<Producto>
    suspend fun crearProducto(producto: Producto): Producto?
    suspend fun actualizarProducto(codigo: Int, producto: Producto): Producto?
    suspend fun eliminarProducto(codigo: Int): Boolean
}
