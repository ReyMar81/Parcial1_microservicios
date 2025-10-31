package com.tiendavirtual.admin.presentation.categorias.state

import com.tiendavirtual.admin.domain.model.Categoria

data class CategoriasUiState(
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val mostrarFormulario: Boolean = false,
    val categoriaEditando: Categoria? = null
)
