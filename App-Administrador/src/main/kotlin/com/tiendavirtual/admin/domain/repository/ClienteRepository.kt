package com.tiendavirtual.admin.domain.repository

import com.tiendavirtual.admin.domain.model.Cliente

interface ClienteRepository {
    suspend fun obtenerClientes(): List<Cliente>
    suspend fun crearCliente(cliente: Cliente): Cliente?
    suspend fun actualizarCliente(id: Int, cliente: Cliente): Cliente?
    suspend fun eliminarCliente(id: Int): Boolean
}
