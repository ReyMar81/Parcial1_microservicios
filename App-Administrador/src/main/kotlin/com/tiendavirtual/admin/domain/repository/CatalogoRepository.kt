package com.tiendavirtual.admin.domain.repository

import com.tiendavirtual.admin.domain.model.Catalogo
import com.tiendavirtual.admin.domain.model.Producto

interface CatalogoRepository {
    suspend fun obtenerCatalogos(): List<Catalogo>
    suspend fun crearCatalogo(catalogo: Catalogo): Catalogo?
    suspend fun actualizarCatalogo(id: Int, catalogo: Catalogo): Catalogo?
    suspend fun eliminarCatalogo(id: Int): Boolean
    suspend fun agregarProductos(catalogoId: Int, productoIds: List<Int>): Boolean
    suspend fun obtenerProductosDeCatalogo(catalogoId: Int): List<Producto>
    suspend fun eliminarProductoDeCatalogo(catalogoId: Int, productoId: Int): Boolean
}
