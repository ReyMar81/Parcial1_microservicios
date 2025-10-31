package com.tiendavirtual.admin.data.repository

import com.tiendavirtual.admin.data.remote.api.CatalogoApi
import com.tiendavirtual.admin.domain.model.Catalogo
import com.tiendavirtual.admin.domain.model.Producto
import com.tiendavirtual.admin.domain.repository.CatalogoRepository

class CatalogoRepositoryImpl(
    private val api: CatalogoApi = CatalogoApi()
) : CatalogoRepository {

    override suspend fun obtenerCatalogos(): List<Catalogo> {
        return api.obtenerCatalogos()
    }

    override suspend fun crearCatalogo(catalogo: Catalogo): Catalogo? {
        return api.crearCatalogo(catalogo)
    }

    override suspend fun actualizarCatalogo(id: Int, catalogo: Catalogo): Catalogo? {
        return api.actualizarCatalogo(id, catalogo)
    }

    override suspend fun eliminarCatalogo(id: Int): Boolean {
        return api.eliminarCatalogo(id)
    }

    override suspend fun agregarProductos(catalogoId: Int, productoIds: List<Int>): Boolean {
        return api.agregarProductos(catalogoId, productoIds)
    }

    override suspend fun obtenerProductosDeCatalogo(catalogoId: Int): List<Producto> {
        return api.obtenerProductosDeCatalogo(catalogoId)
    }

    override suspend fun eliminarProductoDeCatalogo(catalogoId: Int, productoId: Int): Boolean {
        return api.eliminarProductoDeCatalogo(catalogoId, productoId)
    }
}
