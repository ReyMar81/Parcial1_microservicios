package com.tiendavirtual.admin.data.repository

import com.tiendavirtual.admin.data.remote.api.ClienteApi
import com.tiendavirtual.admin.domain.model.Cliente
import com.tiendavirtual.admin.domain.repository.ClienteRepository

class ClienteRepositoryImpl(
    private val api: ClienteApi = ClienteApi()
) : ClienteRepository {

    override suspend fun obtenerClientes(): List<Cliente> {
        return api.obtenerClientes()
    }

    override suspend fun crearCliente(cliente: Cliente): Cliente? {
        return api.crearCliente(cliente)
    }

    override suspend fun actualizarCliente(id: Int, cliente: Cliente): Cliente? {
        return api.actualizarCliente(id, cliente)
    }

    override suspend fun eliminarCliente(id: Int): Boolean {
        return api.eliminarCliente(id)
    }
}
