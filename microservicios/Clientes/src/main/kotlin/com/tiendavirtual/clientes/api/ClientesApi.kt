package com.tiendavirtual.clientes.api

import com.sun.net.httpserver.HttpExchange
import com.tiendavirtual.clientes.negocio.ClienteNegocio
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class CrearClienteRequest(
    val nombres: String,
    val docIdentidad: String,
    val whatsapp: String,
    val direccion: String
)

@Serializable
data class ActualizarClienteRequest(
    val nombres: String? = null,
    val docIdentidad: String? = null,
    val whatsapp: String? = null,
    val direccion: String? = null
)

@Serializable
data class ClienteResponse(
    val id: Int,
    val nombres: String,
    val docIdentidad: String,
    val whatsapp: String,
    val direccion: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null
)

class ClientesApi(private val clienteNegocio: ClienteNegocio) {

    fun manejarRequest(exchange: HttpExchange) {
        try {
            val path = exchange.requestURI.path
            val method = exchange.requestMethod

            when (method) {
                "GET" -> {
                    when {
                        path == "/clientes" -> atenderObtenerClientes(exchange)
                        path.matches(Regex("/clientes/[^/]+$")) -> {
                            val idStr = path.substringAfterLast("/")
                            idStr.toIntOrNull() ?: throw NumberFormatException("id no numérico")
                            atenderObtenerClientePorId(exchange, idStr)
                        }
                        else -> enviarError(exchange, 404, "Endpoint no encontrado")
                    }
                }
                "POST" -> {
                    if (path == "/clientes") atenderCrearCliente(exchange)
                    else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                "PATCH" -> {
                    if (path.matches(Regex("/clientes/[^/]+$"))) {
                        val idStr = path.substringAfterLast("/")
                        idStr.toIntOrNull() ?: throw NumberFormatException("id no numérico")
                        atenderActualizarClienteParcial(exchange, idStr)
                    } else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                "DELETE" -> {
                    if (path.matches(Regex("/clientes/[^/]+$"))) {
                        val idStr = path.substringAfterLast("/")
                        idStr.toIntOrNull() ?: throw NumberFormatException("id no numérico")
                        atenderEliminarCliente(exchange, idStr)
                    } else enviarError(exchange, 404, "Endpoint no encontrado")
                }
                else -> {
                    exchange.responseHeaders.set("Allow", "GET,POST,PATCH,DELETE")
                    enviarError(exchange, 405, "Método no permitido")
                }
            }
        } catch (_: SerializationException) {
            enviarError(exchange, 400, "JSON inválido")
        } catch (_: NumberFormatException) {
            enviarError(exchange, 400, "Id inválido")
        } catch (e: IllegalArgumentException) {
            enviarError(exchange, 400, e.message ?: "Datos inválidos")
        } catch (e: IllegalStateException) {
            enviarError(exchange, 409, e.message ?: "Conflicto de estado")
        } catch (_: Exception) {
            enviarError(exchange, 500, "Error interno del servidor")
        } finally {
            runCatching { exchange.responseBody.close() }
        }
    }

    private fun atenderObtenerClientes(exchange: HttpExchange) {
        val clientes = clienteNegocio.listarClientes()
        val response = clientes.map { it.toResponse() }
        enviarRespuestaExitosa(exchange, 200, response)
    }

    private fun atenderObtenerClientePorId(exchange: HttpExchange, idStr: String) {
        val cliente = clienteNegocio.obtenerCliente(idStr)
        if (cliente != null) {
            enviarRespuestaExitosa(exchange, 200, cliente.toResponse())
        } else {
            enviarError(exchange, 404, "Cliente no encontrado")
        }
    }

    private fun atenderCrearCliente(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        val request = json.decodeFromString<CrearClienteRequest>(body)
        val cliente = clienteNegocio.crearCliente(
            nombres = request.nombres,
            docIdentidad = request.docIdentidad,
            whatsapp = request.whatsapp,
            direccion = request.direccion
        )
        exchange.responseHeaders.set("Location", "/clientes/${cliente.id}")
        enviarRespuestaExitosa(exchange, 201, cliente.toResponse())
    }

    private fun atenderActualizarClienteParcial(exchange: HttpExchange, idStr: String) {
        val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        val request = json.decodeFromString<ActualizarClienteRequest>(body)
        val cliente = clienteNegocio.actualizarCliente(
            id = idStr,
            nombres = request.nombres,
            docIdentidad = request.docIdentidad,
            whatsapp = request.whatsapp,
            direccion = request.direccion
        )
        if (cliente != null) {
            enviarRespuestaExitosa(exchange, 200, cliente.toResponse())
        } else {
            enviarError(exchange, 404, "Cliente no encontrado")
        }
    }

    private fun atenderEliminarCliente(exchange: HttpExchange, idStr: String) {
        val eliminado = clienteNegocio.eliminarCliente(idStr)
        if (eliminado) {
            exchange.sendResponseHeaders(204, -1)
        } else {
            enviarError(exchange, 404, "Cliente no encontrado")
        }
    }

    private fun enviarRespuestaExitosa(exchange: HttpExchange, codigo: Int, data: Any) {
        val response = when (data) {
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                json.encodeToString(data as List<ClienteResponse>)
            }
            is ClienteResponse -> json.encodeToString(data)
            else -> json.encodeToString(data.toString())
        }
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = response.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(codigo, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    private fun enviarError(exchange: HttpExchange, codigo: Int, mensaje: String) {
        val errorResponse = ErrorResponse(mensaje)
        val response = json.encodeToString(errorResponse)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        val bytes = response.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(codigo, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }
}

private fun com.tiendavirtual.clientes.negocio.Cliente.toResponse(): ClienteResponse {
    return ClienteResponse(
        id = this.id,
        nombres = this.nombres,
        docIdentidad = this.docIdentidad,
        whatsapp = this.whatsapp,
        direccion = this.direccion
    )
}
