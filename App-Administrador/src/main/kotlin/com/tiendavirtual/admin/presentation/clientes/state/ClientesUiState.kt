package com.tiendavirtual.admin.presentation.clientes.state

import com.tiendavirtual.admin.domain.model.Cliente

data class ClientesUiState(
    val clientes: List<Cliente> = emptyList(),
    val isLoading: Boolean = false,
    val mostrarFormulario: Boolean = false,
    val clienteEditando: Cliente? = null
)
