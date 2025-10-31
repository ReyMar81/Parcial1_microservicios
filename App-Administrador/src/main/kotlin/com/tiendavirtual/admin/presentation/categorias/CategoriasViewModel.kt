package com.tiendavirtual.admin.presentation.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiendavirtual.admin.data.repository.CategoriaRepositoryImpl
import com.tiendavirtual.admin.domain.model.Categoria
import com.tiendavirtual.admin.domain.repository.CategoriaRepository
import com.tiendavirtual.admin.presentation.categorias.state.CategoriasUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoriasViewModel(
    private val repository: CategoriaRepository = CategoriaRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriasUiState())
    val uiState: StateFlow<CategoriasUiState> = _uiState.asStateFlow()

    init {
        cargarCategorias()
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val categorias = repository.obtenerCategorias()
            _uiState.update { it.copy(categorias = categorias, isLoading = false) }
        }
    }

    fun mostrarFormularioNuevo() {
        _uiState.update {
            it.copy(mostrarFormulario = true, categoriaEditando = null)
        }
    }

    fun mostrarFormularioEditar(categoria: Categoria) {
        _uiState.update {
            it.copy(mostrarFormulario = true, categoriaEditando = categoria)
        }
    }

    fun cerrarFormulario() {
        _uiState.update {
            it.copy(mostrarFormulario = false, categoriaEditando = null)
        }
    }

    fun guardarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val resultado = if (_uiState.value.categoriaEditando == null) {
                repository.crearCategoria(categoria)
            } else {
                repository.actualizarCategoria(_uiState.value.categoriaEditando!!.id!!, categoria)
            }
            
            if (resultado != null) {
                _uiState.update { state ->
                    val nuevaLista = if (state.categoriaEditando == null) {
                        state.categorias + resultado
                    } else {
                        state.categorias.map { if (it.id == resultado.id) resultado else it }
                    }
                    state.copy(
                        categorias = nuevaLista,
                        isLoading = false,
                        mostrarFormulario = false,
                        categoriaEditando = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun eliminarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (repository.eliminarCategoria(categoria.id!!)) {
                _uiState.update { state ->
                    state.copy(
                        categorias = state.categorias.filter { it.id != categoria.id },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
