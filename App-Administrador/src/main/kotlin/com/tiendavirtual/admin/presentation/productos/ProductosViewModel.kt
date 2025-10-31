package com.tiendavirtual.admin.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiendavirtual.admin.data.repository.CategoriaRepositoryImpl
import com.tiendavirtual.admin.data.repository.ProductoRepositoryImpl
import com.tiendavirtual.admin.domain.model.Producto
import com.tiendavirtual.admin.domain.repository.CategoriaRepository
import com.tiendavirtual.admin.domain.repository.ProductoRepository
import com.tiendavirtual.admin.presentation.productos.state.ProductosUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductosViewModel(
    private val productoRepository: ProductoRepository = ProductoRepositoryImpl(),
    private val categoriaRepository: CategoriaRepository = CategoriaRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductosUiState())
    val uiState: StateFlow<ProductosUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
        cargarCategorias()
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val productos = productoRepository.obtenerProductos()
            _uiState.update { it.copy(productos = productos, isLoading = false) }
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategorias = true) }
            val categorias = categoriaRepository.obtenerCategorias()
            _uiState.update { it.copy(categorias = categorias, isLoadingCategorias = false) }
        }
    }

    fun mostrarFormularioNuevo() {
        _uiState.update {
            it.copy(mostrarFormulario = true, productoEditando = null)
        }
        cargarCategorias()
    }

    fun mostrarFormularioEditar(producto: Producto) {
        _uiState.update {
            it.copy(mostrarFormulario = true, productoEditando = producto)
        }
        cargarCategorias()
    }

    fun cerrarFormulario() {
        _uiState.update {
            it.copy(mostrarFormulario = false, productoEditando = null)
        }
    }

    fun guardarProducto(producto: Producto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val resultado = if (_uiState.value.productoEditando == null) {
                productoRepository.crearProducto(producto)
            } else {
                productoRepository.actualizarProducto(_uiState.value.productoEditando!!.codigo, producto)
            }
            
            if (resultado != null) {
                _uiState.update { state ->
                    val nuevaLista = if (state.productoEditando == null) {
                        state.productos + resultado
                    } else {
                        state.productos.map { if (it.codigo == resultado.codigo) resultado else it }
                    }
                    state.copy(
                        productos = nuevaLista,
                        isLoading = false,
                        mostrarFormulario = false,
                        productoEditando = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun eliminarProducto(producto: Producto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (productoRepository.eliminarProducto(producto.codigo)) {
                _uiState.update { state ->
                    state.copy(
                        productos = state.productos.filter { it.codigo != producto.codigo },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
