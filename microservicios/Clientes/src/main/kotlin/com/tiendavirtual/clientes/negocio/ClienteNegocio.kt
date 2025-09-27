package com.tiendavirtual.clientes.negocio

import com.tiendavirtual.clientes.data.ClientesData

class ClienteNegocio(private val clientesData: ClientesData) {

    fun crearCliente(nombres: String, docIdentidad: String, whatsapp: String, direccion: String): Cliente {
        validarDatosCliente(nombres, docIdentidad, whatsapp, direccion)
        val clienteExistente = clientesData.obtenerClientePorDoc(docIdentidad)
        if (clienteExistente != null) {
            throw IllegalArgumentException("Ya existe un cliente con documento: $docIdentidad")
        }
        return clientesData.crearCliente(nombres, docIdentidad, whatsapp, direccion)
    }

    fun listarClientes(): List<Cliente> {
        return clientesData.obtenerTodosLosClientes()
    }

    fun obtenerCliente(id: String): Cliente? {
        val clienteId = id.toIntOrNull() ?: throw IllegalArgumentException("ID debe ser un número")
        return clientesData.obtenerCliente(clienteId)
    }

    fun actualizarCliente(id: String, nombres: String?, docIdentidad: String?, whatsapp: String?, direccion: String?): Cliente? {
        val clienteId = id.toIntOrNull() ?: throw IllegalArgumentException("ID debe ser un número")
        val clienteExistente = clientesData.obtenerCliente(clienteId)
            ?: throw IllegalArgumentException("Cliente no encontrado: $id")
        if (docIdentidad != null && docIdentidad != clienteExistente.docIdentidad) {
            val otroCliente = clientesData.obtenerClientePorDoc(docIdentidad)
            if (otroCliente != null && otroCliente.id != clienteId) {
                throw IllegalArgumentException("Ya existe otro cliente con documento: $docIdentidad")
            }
        }
        return clientesData.actualizarCliente(clienteId, nombres, docIdentidad, whatsapp, direccion)
    }

    fun eliminarCliente(id: String): Boolean {
        val clienteId = id.toIntOrNull() ?: throw IllegalArgumentException("ID debe ser un número")
        if (clientesData.obtenerCliente(clienteId) == null) {
            throw IllegalArgumentException("Cliente no encontrado: $id")
        }
        return clientesData.eliminarCliente(clienteId)
    }

    private fun validarDatosCliente(nombres: String, docIdentidad: String, whatsapp: String, direccion: String) {
        if (nombres.isBlank()) {
            throw IllegalArgumentException("El nombre no puede estar vacío")
        }
        if (docIdentidad.isBlank()) {
            throw IllegalArgumentException("El documento de identidad no puede estar vacío")
        }
        if (whatsapp.isBlank()) {
            throw IllegalArgumentException("El WhatsApp no puede estar vacío")
        }
        if (direccion.isBlank()) {
            throw IllegalArgumentException("La dirección no puede estar vacía")
        }
        if (docIdentidad.length > 30) {
            throw IllegalArgumentException("El documento no puede tener más de 30 caracteres")
        }
        if (!docIdentidad.matches(Regex("^[0-9]+$"))) {
            throw IllegalArgumentException("El documento debe contener solo números")
        }
        if (!whatsapp.matches(Regex("^[+]?[0-9]{8,15}$"))) {
            throw IllegalArgumentException("Formato de WhatsApp inválido")
        }
    }
}

data class Cliente(
    val id: Int,
    val nombres: String,
    val docIdentidad: String,
    val whatsapp: String,
    val direccion: String
)
