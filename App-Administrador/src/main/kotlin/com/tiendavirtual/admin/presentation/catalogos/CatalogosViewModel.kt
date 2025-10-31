package com.tiendavirtual.admin.presentation.catalogos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiendavirtual.admin.data.repository.CatalogoRepositoryImpl
import com.tiendavirtual.admin.data.repository.ProductoRepositoryImpl
import com.tiendavirtual.admin.domain.model.Catalogo
import com.tiendavirtual.admin.domain.repository.CatalogoRepository
import com.tiendavirtual.admin.domain.repository.ProductoRepository
import com.tiendavirtual.admin.presentation.catalogos.state.CatalogosUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CatalogosViewModel(
    private val catalogoRepository: CatalogoRepository = CatalogoRepositoryImpl(),
    private val productoRepository: ProductoRepository = ProductoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogosUiState())
    val uiState: StateFlow<CatalogosUiState> = _uiState.asStateFlow()

    init {
        cargarCatalogos()
        cargarProductos()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val catalogos = catalogoRepository.obtenerCatalogos()
            _uiState.update { it.copy(catalogos = catalogos, isLoading = false) }
        }
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProductos = true) }
            val productos = productoRepository.obtenerProductos()
            _uiState.update { it.copy(productos = productos, isLoadingProductos = false) }
        }
    }

    fun mostrarFormularioNuevo() {
        _uiState.update {
            it.copy(mostrarFormulario = true, catalogoEditando = null)
        }
    }

    fun mostrarFormularioEditar(catalogo: Catalogo) {
        _uiState.update {
            it.copy(mostrarFormulario = true, catalogoEditando = catalogo)
        }
    }

    fun cerrarFormulario() {
        _uiState.update {
            it.copy(mostrarFormulario = false, catalogoEditando = null)
        }
    }

    fun guardarCatalogo(catalogo: Catalogo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val resultado = if (_uiState.value.catalogoEditando == null) {
                catalogoRepository.crearCatalogo(catalogo)
            } else {
                catalogoRepository.actualizarCatalogo(_uiState.value.catalogoEditando!!.id!!, catalogo)
            }
            
            if (resultado != null) {
                _uiState.update { state ->
                    val nuevaLista = if (state.catalogoEditando == null) {
                        state.catalogos + resultado
                    } else {
                        state.catalogos.map { if (it.id == resultado.id) resultado else it }
                    }
                    state.copy(
                        catalogos = nuevaLista,
                        isLoading = false,
                        mostrarFormulario = false,
                        catalogoEditando = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun eliminarCatalogo(catalogo: Catalogo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (catalogoRepository.eliminarCatalogo(catalogo.id!!)) {
                _uiState.update { state ->
                    state.copy(
                        catalogos = state.catalogos.filter { it.id != catalogo.id },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun mostrarGestionProductos(catalogo: Catalogo) {
        _uiState.update {
            it.copy(mostrarGestionProductos = true, catalogoSeleccionado = catalogo)
        }
        cargarProductosDeCatalogo(catalogo.id!!)
    }

    fun cerrarGestionProductos() {
        _uiState.update {
            it.copy(mostrarGestionProductos = false, catalogoSeleccionado = null, productosDelCatalogo = emptyList())
        }
    }

    private fun cargarProductosDeCatalogo(catalogoId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProductos = true) }
            val productos = catalogoRepository.obtenerProductosDeCatalogo(catalogoId)
            _uiState.update { it.copy(productosDelCatalogo = productos, isLoadingProductos = false) }
        }
    }

    fun agregarProductos(productoIds: List<Int>) {
        viewModelScope.launch {
            val catalogoId = _uiState.value.catalogoSeleccionado?.id ?: return@launch
            _uiState.update { it.copy(isLoadingProductos = true) }
            
            if (catalogoRepository.agregarProductos(catalogoId, productoIds)) {
                cargarProductosDeCatalogo(catalogoId)
            } else {
                _uiState.update { it.copy(isLoadingProductos = false) }
            }
        }
    }

    fun eliminarProductoDeCatalogo(productoId: Int) {
        viewModelScope.launch {
            val catalogoId = _uiState.value.catalogoSeleccionado?.id ?: return@launch
            _uiState.update { it.copy(isLoadingProductos = true) }
            
            if (catalogoRepository.eliminarProductoDeCatalogo(catalogoId, productoId)) {
                cargarProductosDeCatalogo(catalogoId)
            } else {
                _uiState.update { it.copy(isLoadingProductos = false) }
            }
        }
    }
}
