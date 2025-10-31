package com.tiendavirtual.admin.presentation.catalogos.state

import com.tiendavirtual.admin.domain.model.Catalogo
import com.tiendavirtual.admin.domain.model.Producto

data class CatalogosUiState(
    val catalogos: List<Catalogo> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val productosDelCatalogo: List<Producto> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingProductos: Boolean = false,
    val mostrarFormulario: Boolean = false,
    val mostrarGestionProductos: Boolean = false,
    val catalogoEditando: Catalogo? = null,
    val catalogoSeleccionado: Catalogo? = null
)
