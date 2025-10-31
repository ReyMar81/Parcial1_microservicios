package com.tiendavirtual.admin.presentation.productos.state

import com.tiendavirtual.admin.domain.model.Categoria
import com.tiendavirtual.admin.domain.model.Producto

data class ProductosUiState(
    val productos: List<Producto> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingCategorias: Boolean = false,
    val mostrarFormulario: Boolean = false,
    val productoEditando: Producto? = null
)
