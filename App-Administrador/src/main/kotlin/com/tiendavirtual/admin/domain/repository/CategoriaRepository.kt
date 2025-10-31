package com.tiendavirtual.admin.domain.repository

import com.tiendavirtual.admin.domain.model.Categoria

interface CategoriaRepository {
    suspend fun obtenerCategorias(): List<Categoria>
    suspend fun crearCategoria(categoria: Categoria): Categoria?
    suspend fun actualizarCategoria(id: Int, categoria: Categoria): Categoria?
    suspend fun eliminarCategoria(id: Int): Boolean
}
