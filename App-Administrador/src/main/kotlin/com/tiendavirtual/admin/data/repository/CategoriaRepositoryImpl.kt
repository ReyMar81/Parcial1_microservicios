package com.tiendavirtual.admin.data.repository

import com.tiendavirtual.admin.data.remote.api.CategoriaApi
import com.tiendavirtual.admin.domain.model.Categoria
import com.tiendavirtual.admin.domain.repository.CategoriaRepository

class CategoriaRepositoryImpl(
    private val api: CategoriaApi = CategoriaApi()
) : CategoriaRepository {

    override suspend fun obtenerCategorias(): List<Categoria> {
        return api.obtenerCategorias()
    }

    override suspend fun crearCategoria(categoria: Categoria): Categoria? {
        return api.crearCategoria(categoria)
    }

    override suspend fun actualizarCategoria(id: Int, categoria: Categoria): Categoria? {
        return api.actualizarCategoria(id, categoria)
    }

    override suspend fun eliminarCategoria(id: Int): Boolean {
        return api.eliminarCategoria(id)
    }
}
