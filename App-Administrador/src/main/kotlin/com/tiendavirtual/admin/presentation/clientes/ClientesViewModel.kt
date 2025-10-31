package com.tiendavirtual.admin.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiendavirtual.admin.data.repository.ClienteRepositoryImpl
import com.tiendavirtual.admin.domain.model.Cliente
import com.tiendavirtual.admin.domain.repository.ClienteRepository
import com.tiendavirtual.admin.presentation.clientes.state.ClientesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClientesViewModel(
    private val repository: ClienteRepository = ClienteRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    init {
        cargarClientes()
    }

    private fun cargarClientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val clientes = repository.obtenerClientes()
            _uiState.update { it.copy(clientes = clientes, isLoading = false) }
        }
    }

    fun mostrarFormularioNuevo() {
        _uiState.update {
            it.copy(mostrarFormulario = true, clienteEditando = null)
        }
    }

    fun mostrarFormularioEditar(cliente: Cliente) {
        _uiState.update {
            it.copy(mostrarFormulario = true, clienteEditando = cliente)
        }
    }

    fun cerrarFormulario() {
        _uiState.update {
            it.copy(mostrarFormulario = false, clienteEditando = null)
        }
    }

    fun guardarCliente(cliente: Cliente) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val resultado = if (_uiState.value.clienteEditando == null) {
                repository.crearCliente(cliente)
            } else {
                repository.actualizarCliente(_uiState.value.clienteEditando!!.id!!, cliente)
            }
            
            if (resultado != null) {
                // Actualizar lista localmente para mejor UX
                _uiState.update { state ->
                    val nuevaLista = if (state.clienteEditando == null) {
                        state.clientes + resultado
                    } else {
                        state.clientes.map { if (it.id == resultado.id) resultado else it }
                    }
                    state.copy(
                        clientes = nuevaLista,
                        isLoading = false,
                        mostrarFormulario = false,
                        clienteEditando = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun eliminarCliente(cliente: Cliente) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (repository.eliminarCliente(cliente.id!!)) {
                _uiState.update { state ->
                    state.copy(
                        clientes = state.clientes.filter { it.id != cliente.id },
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
